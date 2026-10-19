package app.noor.islamic.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.noor.islamic.MainActivity
import app.noor.islamic.R
import app.noor.islamic.data.Prefs
import app.noor.islamic.logic.ArabicDates
import app.noor.islamic.logic.Prayer
import app.noor.islamic.logic.PrayerCalc
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/** A quiet notification (no adhan, no sound) when a prayer time starts. */
object Notifier {
    // Bump the suffix if the channel settings ever change: Android caches channels once created.
    const val CHANNEL = "prayer_silent_v1"

    fun createChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) != null) return
        val ch = NotificationChannel(CHANNEL, ctx.getString(R.string.channel_prayer), NotificationManager.IMPORTANCE_HIGH)
        ch.description = ctx.getString(R.string.channel_prayer_desc)
        ch.setSound(null, null)
        ch.enableVibration(true)
        ch.vibrationPattern = longArrayOf(0, 160, 110, 160)
        ch.setShowBadge(false)
        nm.createNotificationChannel(ch)
    }

    fun hasPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(ctx: Context, index: Int, at: Long) {
        if (!hasPermission(ctx)) return
        createChannel(ctx)
        val prefs = Prefs(ctx)
        val zone = ZoneId.systemDefault()
        val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(at), zone)
        var clock = "%02d:%02d".format(Locale.US, time.hour, time.minute)
        if (prefs.easternDigits) clock = ArabicDates.toEastern(clock)

        val open = PendingIntent.getActivity(
            ctx,
            0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val place = prefs.place?.name.orEmpty()
        val text = if (place.isEmpty()) clock else "$clock · $place"
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_noor)
            .setContentTitle("حان الآن وقت صلاة ${Prayer.names[index]}")
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(100 + index, n)
        } catch (e: SecurityException) {
            // permission revoked in the meantime: nothing to do
        }
    }
}

/** Keeps exactly one alarm pending: the next prayer (that the user wants to be told about). */
object PrayerScheduler {
    private const val REQUEST = 7001

    private fun pending(ctx: Context, index: Int, at: Long): PendingIntent {
        val i = Intent(ctx, PrayerReceiver::class.java).putExtra("idx", index).putExtra("at", at)
        return PendingIntent.getBroadcast(ctx, REQUEST, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun scheduleNext(context: Context) {
        val ctx = context.applicationContext
        val am = ctx.getSystemService(AlarmManager::class.java)
        val prefs = Prefs(ctx)
        val place = prefs.place
        val mask = prefs.notifyMask
        if (place == null || mask == 0) {
            am.cancel(pending(ctx, 0, 0L))
            return
        }
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone).plusSeconds(1)
        var index = -1
        var time: ZonedDateTime? = null
        for (dayOffset in 0..1) {
            val day = PrayerCalc.compute(
                now.toLocalDate().plusDays(dayOffset.toLong()),
                place.lat, place.lon, zone, prefs.methodEnum, prefs.hanafi,
            )
            for (i in 0..5) {
                if ((Prayer.bit(i) and mask) != 0 && day[i].isAfter(now)) {
                    index = i
                    time = day[i]
                    break
                }
            }
            if (time != null) break
        }
        val t = time ?: return
        val at = t.toInstant().toEpochMilli()
        val op = pending(ctx, index, at)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, op)
        }
    }
}

class PrayerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val index = intent.getIntExtra("idx", -1)
        val at = intent.getLongExtra("at", 0L)
        // Ignore an alarm that fired very late (for example after the phone was off).
        if (index in 0..5 && System.currentTimeMillis() - at < 10 * 60_000L) {
            Notifier.show(context, index, at)
        }
        PrayerScheduler.scheduleNext(context)
    }
}

/** Re-arms the alarm after a reboot, an app update, or a clock / time-zone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerScheduler.scheduleNext(context)
    }
}

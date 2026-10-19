package app.noor.islamic.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import app.noor.islamic.LocationHelper
import app.noor.islamic.NoorApp
import app.noor.islamic.data.Cities
import app.noor.islamic.data.Place
import app.noor.islamic.notify.PrayerScheduler
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Locale

val LocalApp = staticCompositionLocalOf<NoorApp> { error("NoorApp not provided") }

/** Very small navigation model: four tabs, plus a few full-screen pages on top. */
class Nav {
    var tab by mutableIntStateOf(0)
    var overlay by mutableIntStateOf(0) // 0 = none, 1 = settings, 2 = city picker
    var quranPage by mutableIntStateOf(0) // 0 = surah index, otherwise the reader is open on this page
    var adhkar by mutableIntStateOf(-1) // -1 = list of sections, otherwise the section being read
    private var citiesFromSettings by mutableStateOf(false)

    fun openQuran(page: Int) {
        overlay = 0
        tab = 1
        quranPage = page
    }

    fun openAdhkar(index: Int) {
        overlay = 0
        tab = 2
        adhkar = index
    }

    fun openSettings() {
        overlay = 1
    }

    fun openCities(fromSettings: Boolean) {
        citiesFromSettings = fromSettings
        overlay = 2
    }

    fun canGoBack(): Boolean = overlay != 0 || quranPage != 0 || adhkar != -1 || tab != 0

    fun back() {
        when {
            overlay == 2 -> overlay = if (citiesFromSettings) 1 else 0
            overlay != 0 -> overlay = 0
            tab == 1 && quranPage != 0 -> quranPage = 0
            tab == 2 && adhkar != -1 -> adhkar = -1
            tab != 0 -> tab = 0
        }
    }
}

/** The current time, refreshed at every second boundary. */
@Composable
fun rememberNow(): State<ZonedDateTime> = produceState(ZonedDateTime.now()) {
    while (true) {
        value = ZonedDateTime.now()
        delay(1000L - (System.currentTimeMillis() % 1000L) + 5L)
    }
}

/** "10:42" and the am/pm marker ("" in 24-hour mode). */
fun timeParts(t: ZonedDateTime, use24: Boolean): Pair<String, String> {
    val h = t.hour
    val m = t.minute
    return if (use24) {
        Pair("%02d:%02d".format(Locale.US, h, m), "")
    } else {
        val h12 = if (h % 12 == 0) 12 else h % 12
        Pair("%d:%02d".format(Locale.US, h12, m), if (h < 12) "ص" else "م")
    }
}

@Composable
fun countdownText(d: Duration): String {
    val s = d.seconds.coerceAtLeast(0L)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    val t = when {
        h > 0 -> "$h س $m د"
        m > 0 -> "$m د $sec ث"
        else -> "$sec ث"
    }
    return digits(t)
}

/**
 * Returns a function that finds the user's location, asking for the permission first,
 * stores it as the current place and re-arms the prayer notifications.
 */
@Composable
fun rememberLocateAction(onDone: (Boolean) -> Unit = {}): () -> Unit {
    val ctx = LocalContext.current
    val app = LocalApp.current

    fun finish(loc: android.location.Location?) {
        if (loc == null) {
            Toast.makeText(ctx, "تعذّر تحديد الموقع، اختر مدينتك يدوياً", Toast.LENGTH_LONG).show()
            onDone(false)
            return
        }
        val near = Cities.nearest(loc.latitude, loc.longitude)
        app.prefs.setPlace(Place(near?.name ?: "موقعي الحالي", loc.latitude, loc.longitude))
        PrayerScheduler.scheduleNext(ctx)
        onDone(true)
    }

    fun start() {
        Toast.makeText(ctx, "جارٍ تحديد الموقع…", Toast.LENGTH_SHORT).show()
        LocationHelper.locate(ctx) { finish(it) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) {
            start()
        } else {
            Toast.makeText(ctx, "لم يُمنح إذن الموقع، اختر مدينتك يدوياً", Toast.LENGTH_LONG).show()
            onDone(false)
        }
    }
    return {
        if (LocationHelper.hasPermission(ctx)) {
            start()
        } else {
            launcher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }
}

/**
 * Returns a function that runs an action only once the notification permission is available
 * (Android 13+ asks the user the first time).
 */
@Composable
fun rememberNotificationGate(): (() -> Unit) -> Unit {
    val ctx = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pending
        pending = null
        if (granted) action?.invoke()
    }
    return { action ->
        if (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            action()
        } else {
            pending = action
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

package app.noor.islamic.ui

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.noor.islamic.logic.ArabicDates
import app.noor.islamic.logic.Method
import app.noor.islamic.logic.Prayer
import app.noor.islamic.notify.PrayerScheduler
import java.time.LocalDate

@Composable
fun SettingsScreen(nav: Nav) {
    val app = LocalApp.current
    val prefs = app.prefs
    val ctx = LocalContext.current
    val c = Noor.colors
    val locate = rememberLocateAction()
    val gate = rememberNotificationGate()

    // System settings (exact alarms, notification permission) are changed outside the app: look again on return.
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        PrayerScheduler.scheduleNext(ctx)
        onPauseOrDispose { }
    }
    val needsExact = remember(tick) {
        Build.VERSION.SDK_INT >= 31 && !ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }
    val systemNotifOff = remember(tick) { !NotificationManagerCompat.from(ctx).areNotificationsEnabled() }
    val notifOn = prefs.notifyMask != 0

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        NoorTopBar("الإعدادات", onBack = { nav.back() })
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Group("المظهر") {
                Field("السمة") {
                    SegmentedControl(listOf("تلقائي", "نهاري", "ليلي"), prefs.themeMode, { prefs.setTheme(it) })
                }
                Field("الأرقام") {
                    SegmentedControl(listOf("1 2 3", "١ ٢ ٣"), if (prefs.easternDigits) 1 else 0, { prefs.setEastern(it == 1) })
                }
                Field("تنسيق الساعة") {
                    SegmentedControl(
                        listOf("تلقائي", digits("12 ساعة"), digits("24 ساعة")),
                        prefs.clockFormat,
                        { prefs.setClockFormat(it) },
                    )
                }
            }

            Group("قراءة القرآن") {
                Field("طريقة التصفح") {
                    SegmentedControl(
                        listOf("تقليب الصفحات", "تمرير عمودي"),
                        if (prefs.readerPaged) 0 else 1,
                        { prefs.setPaged(it == 0) },
                    )
                }
            }

            Group("الموقع") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NIcon(Ic.Pin, tint = c.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        prefs.place?.name ?: "لم يُحدَّد الموقع بعد",
                        style = NoorType.h3,
                        color = if (prefs.place == null) c.muted else c.text,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("موقعي الحالي", onClick = locate, modifier = Modifier.weight(1f))
                    SoftButton("اختيار مدينة", onClick = { nav.openCities(true) }, modifier = Modifier.weight(1f))
                }
            }

            Group("التنبيهات") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("تنبيه عند دخول وقت الصلاة", style = NoorType.h3, color = c.text)
                        Text("إشعار هادئ بلا أذان ولا صوت", style = NoorType.small, color = c.muted)
                    }
                    NoorSwitch(notifOn) { on ->
                        val apply = {
                            prefs.setNotifyMask(if (on) Prayer.ALL_MASK else 0)
                            PrayerScheduler.scheduleNext(ctx)
                        }
                        if (on) gate(apply) else apply()
                    }
                }
                if (notifOn && prefs.place == null) {
                    Text("حدّد موقعك ليبدأ التنبيه.", style = NoorType.small, color = c.accent)
                }
                if (notifOn) {
                    Text(
                        "لتفعيل صلوات معيّنة فقط، استخدم أيقونات الجرس في بطاقة المواقيت بالصفحة الرئيسية.",
                        style = NoorType.small,
                        color = c.muted,
                    )
                }
                if (notifOn && systemNotifOff) {
                    NoticeRow("الإشعارات متوقفة من إعدادات النظام لهذا التطبيق.", "فتح الإعدادات") {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }
                if (notifOn && needsExact) {
                    NoticeRow("للتنبيه في الدقيقة نفسها اسمح بالمنبّهات الدقيقة، وإلا قد يتأخر قليلاً.", "السماح") {
                        if (Build.VERSION.SDK_INT >= 31) {
                            try {
                                ctx.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "تعذّر فتح الإعدادات", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            Group("طريقة حساب المواقيت") {
                Column {
                    Method.entries.forEachIndexed { i, m ->
                        val sel = prefs.method == i
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    prefs.setMethod(i)
                                    PrayerScheduler.scheduleNext(ctx)
                                }
                                .padding(horizontal = 4.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                m.label,
                                style = NoorType.body,
                                color = if (sel) c.primary else c.text,
                                modifier = Modifier.weight(1f),
                            )
                            if (sel) NIcon(Ic.Check, tint = c.primary, size = 20.dp)
                        }
                    }
                }
                Field("وقت العصر") {
                    SegmentedControl(
                        listOf("جمهور العلماء", "الحنفي"),
                        if (prefs.hanafi) 1 else 0,
                        {
                            prefs.setHanafi(it == 1)
                            PrayerScheduler.scheduleNext(ctx)
                        },
                    )
                    Text(
                        if (prefs.hanafi) "يدخل وقت العصر عند بلوغ ظل الشيء مثليه." else "يدخل وقت العصر عند بلوغ ظل الشيء مثله.",
                        style = NoorType.small,
                        color = c.muted,
                    )
                }
            }

            Group("التاريخ الهجري") {
                val hijri = remember(prefs.hijriAdjust) { ArabicDates.hijri(LocalDate.now(), prefs.hijriAdjust) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("تعديل التاريخ", style = NoorType.h3, color = c.text)
                        Text(
                            when (prefs.hijriAdjust) {
                                0 -> "حسب تقويم أم القرى"
                                1 -> "زيادة يوم واحد"
                                2 -> "زيادة يومين"
                                -1 -> "إنقاص يوم واحد"
                                else -> "إنقاص يومين"
                            },
                            style = NoorType.small,
                            color = c.muted,
                        )
                        if (hijri != null) {
                            Text("اليوم: " + digits(hijri.toString()), style = NoorType.small, color = c.primary)
                        }
                    }
                    StepButton("−", enabled = prefs.hijriAdjust > -2) { prefs.setHijriAdjust(prefs.hijriAdjust - 1) }
                    Spacer(Modifier.width(8.dp))
                    StepButton("+", enabled = prefs.hijriAdjust < 2) { prefs.setHijriAdjust(prefs.hijriAdjust + 1) }
                }
            }

            Group("حول التطبيق") {
                val version = remember {
                    try {
                        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                }
                Text("نور" + if (version.isEmpty()) "" else " · " + digits(version), style = NoorType.h3, color = c.text)
                Text(
                    "يعمل بالكامل دون إنترنت، بلا إعلانات، ولا يجمع أي بيانات عنك. تُحسب المواقيت واتجاه القبلة على جهازك.",
                    style = NoorType.small,
                    color = c.muted,
                )
                Text(
                    "نص المصحف: مشروع تنزيل (tanzil.net) على تقسيم مصحف المدينة · الخطوط: الأميري قرآن وتجوّل (رخصة OFL) · " +
                        "الأذكار: حصن المسلم (CC BY 4.0).",
                    style = NoorType.small,
                    color = c.muted,
                )
            }
            Spacer(Modifier.navigationBarsPadding().height(16.dp))
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        SectionLabel(title)
        Column(
            Modifier
                .fillMaxWidth()
                .noorCard()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
private fun Field(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = NoorType.h3, color = Noor.colors.text)
        content()
    }
}

@Composable
private fun NoticeRow(text: String, button: String, onClick: () -> Unit) {
    val c = Noor.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surfaceAlt)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, style = NoorType.small, color = c.text)
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(c.primarySoft)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(button, style = NoorType.label, color = c.primary)
        }
    }
}

@Composable
private fun StepButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Noor.colors
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(c.surfaceAlt)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            style = NoorType.h2,
            color = if (enabled) c.primary else c.muted.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NoorSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Noor.colors
    val pad by animateDpAsState(if (checked) 23.dp else 3.dp, label = "thumb")
    Box(
        Modifier
            .size(width = 52.dp, height = 30.dp)
            .clip(CircleShape)
            .background(if (checked) c.primary else c.line)
            .clickable { onChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(start = pad)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) c.onPrimary else c.surface),
        )
    }
}

package app.noor.islamic.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noor.islamic.logic.ArabicDates
import app.noor.islamic.logic.Prayer
import app.noor.islamic.logic.PrayerCalc
import app.noor.islamic.logic.Qibla
import app.noor.islamic.notify.PrayerScheduler
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

@Composable
fun HomeScreen(nav: Nav) {
    val app = LocalApp.current
    val prefs = app.prefs
    val ctx = LocalContext.current
    val now by rememberNow()
    val today = now.toLocalDate()
    val place = prefs.place
    val method = prefs.methodEnum
    val hanafi = prefs.hanafi

    val days = remember(place, method, hanafi, today) {
        if (place == null) {
            null
        } else {
            val zone = ZoneId.systemDefault()
            Triple(
                PrayerCalc.compute(today.minusDays(1), place.lat, place.lon, zone, method, hanafi),
                PrayerCalc.compute(today, place.lat, place.lon, zone, method, hanafi),
                PrayerCalc.compute(today.plusDays(1), place.lat, place.lon, zone, method, hanafi),
            )
        }
    }
    val upcoming = days?.let { PrayerCalc.pickUpcoming(now, it.first, it.second, it.third) }
    val use24 = when (prefs.clockFormat) {
        1 -> false
        2 -> true
        else -> DateFormat.is24HourFormat(ctx)
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HomeHeader(place?.name, onSettings = { nav.openSettings() }) }
        item { HeroCard(now, use24, upcoming, prefs.hijriAdjust) }
        if (place == null) item { PlacePrompt(nav) }
        if (days != null && upcoming != null) {
            item {
                PrayerCard(
                    times = days.second,
                    nextIndex = if (upcoming.time.toLocalDate() == today) upcoming.index else -1,
                    use24 = use24,
                    mask = prefs.notifyMask,
                    onToggle = { index, gate ->
                        val bit = Prayer.bit(index)
                        val turningOn = (prefs.notifyMask and bit) == 0
                        val apply = {
                            prefs.setNotifyMask(prefs.notifyMask xor bit)
                            PrayerScheduler.scheduleNext(ctx)
                        }
                        if (turningOn) gate(apply) else apply()
                    },
                )
            }
        }
        item { QuickGrid(nav) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun HomeHeader(placeName: String?, onSettings: () -> Unit) {
    val c = Noor.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("نور", style = NoorType.title, color = c.primary)
            if (!placeName.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NIcon(Ic.Pin, tint = c.muted, size = 14.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(placeName, style = NoorType.small, color = c.muted)
                }
            }
        }
        IconBtn(Ic.Tune, onSettings, tint = c.text)
    }
}

@Composable
private fun HeroCard(now: ZonedDateTime, use24: Boolean, upcoming: app.noor.islamic.logic.Upcoming?, hijriAdjust: Int) {
    val c = Noor.colors
    val today = now.toLocalDate()
    val hijri = remember(today, hijriAdjust) { ArabicDates.hijri(today, hijriAdjust) }
    val (hm, suffix) = timeParts(now, use24)
    val white = Color.White

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(c.heroStart, c.heroEnd)))
            .drawBehind {
                // faint eight-pointed star in the corner
                val side = size.width * 0.5f
                val center = Offset(size.width * 0.08f, size.height * 0.05f)
                val topLeft = Offset(center.x - side / 2f, center.y - side / 2f)
                val style = Stroke(width = 1.5.dp.toPx())
                val tint = Color.White.copy(alpha = 0.07f)
                drawRect(tint, topLeft = topLeft, size = Size(side, side), style = style)
                rotate(45f, center) {
                    drawRect(tint, topLeft = topLeft, size = Size(side, side), style = style)
                }
            }
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                hijri?.let { digits(it.toString()) } ?: "",
                style = NoorType.h3,
                color = white,
            )
            Text(digits(ArabicDates.gregorian(today)), style = NoorType.small, color = white.copy(alpha = 0.72f))

            Spacer(Modifier.height(6.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(digits(hm), style = NoorType.clock, color = white)
                    Text(
                        ":" + digits("%02d".format(Locale.US, now.second)),
                        style = NoorType.clockSeconds,
                        color = white.copy(alpha = 0.62f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 14.dp),
                    )
                    if (suffix.isNotEmpty()) {
                        Text(
                            suffix,
                            style = NoorType.label,
                            color = white.copy(alpha = 0.8f),
                            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
                        )
                    }
                }
            }

            if (upcoming != null) {
                Spacer(Modifier.height(14.dp))
                val remaining = Duration.between(now, upcoming.time)
                val total = Duration.between(upcoming.previous, upcoming.time).seconds.coerceAtLeast(1L)
                val done = Duration.between(upcoming.previous, now).seconds.coerceAtLeast(0L)
                val progress = (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                val (nextTime, nextSuffix) = timeParts(upcoming.time, use24)
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (upcoming.index == Prayer.SUNRISE) "القادم" else "الصلاة القادمة",
                            style = NoorType.small,
                            color = white.copy(alpha = 0.7f),
                        )
                        Text(Prayer.names[upcoming.index], style = NoorType.h2.copy(fontSize = 24.sp), color = white)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            digits(if (nextSuffix.isEmpty()) nextTime else "$nextTime $nextSuffix"),
                            style = NoorType.h2,
                            color = white,
                        )
                        Text("بعد " + countdownText(remaining), style = NoorType.small, color = white.copy(alpha = 0.75f))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(white.copy(alpha = 0.18f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(c.accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlacePrompt(nav: Nav) {
    val c = Noor.colors
    val locate = rememberLocateAction()
    Column(
        Modifier
            .fillMaxWidth()
            .noorCard()
            .padding(18.dp),
    ) {
        Text("حدّد موقعك", style = NoorType.h2, color = c.text)
        Spacer(Modifier.height(4.dp))
        Text(
            "لعرض مواقيت الصلاة واتجاه القبلة في مكانك. يمكنك أيضاً اختيار مدينتك يدوياً بدون أي أذونات.",
            style = NoorType.body,
            color = c.muted,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("موقعي الحالي", onClick = locate, modifier = Modifier.weight(1f))
            SoftButton("اختيار مدينة", onClick = { nav.openCities(false) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PrayerCard(
    times: List<ZonedDateTime>,
    nextIndex: Int,
    use24: Boolean,
    mask: Int,
    onToggle: (Int, (() -> Unit) -> Unit) -> Unit,
) {
    val c = Noor.colors
    val gate = rememberNotificationGate()
    Column(
        Modifier
            .fillMaxWidth()
            .noorCard()
            .padding(vertical = 8.dp),
    ) {
        for (i in 0..5) {
            val next = i == nextIndex
            val (t, suffix) = timeParts(times[i], use24)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (next) c.primarySoft else Color.Transparent)
                    .padding(start = 14.dp, end = 4.dp)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    Prayer.names[i],
                    style = if (next) NoorType.h3 else NoorType.body,
                    color = if (i == Prayer.SUNRISE) c.muted else c.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    digits(if (suffix.isEmpty()) t else "$t $suffix"),
                    style = if (next) NoorType.h3 else NoorType.body,
                    color = if (next) c.primary else c.text,
                )
                if (i == Prayer.SUNRISE) {
                    Spacer(Modifier.width(44.dp))
                } else {
                    val on = (mask and Prayer.bit(i)) != 0
                    IconBtn(
                        if (on) Ic.Bell else Ic.BellOff,
                        onClick = { onToggle(i, gate) },
                        tint = if (on) c.primary else c.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickGrid(nav: Nav) {
    val app = LocalApp.current
    val prefs = app.prefs
    val place = prefs.place
    val last = prefs.lastPage
    val quran = app.data.quran
    val adhkar = app.data.adhkar

    fun adhkarIndex(keyword: String): Int = adhkar?.indexOfFirst { it.title.contains(keyword) } ?: -1

    val quranSub = if (last > 0 && quran != null) {
        "تابع من " + "صفحة " + quranDigits(last)
    } else {
        "ابدأ القراءة"
    }
    val qiblaSub = if (place != null) {
        digits("%d° من الشمال".format(Locale.US, Math.round(Qibla.bearing(place.lat, place.lon)).toInt()))
    } else {
        "حدّد موقعك أولاً"
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickCard(Ic.Quran, "القرآن الكريم", quranSub, Modifier.weight(1f)) {
                if (last > 0) nav.openQuran(last) else {
                    nav.tab = 1
                    nav.quranPage = 0
                }
            }
            QuickCard(Ic.Compass, "اتجاه القبلة", qiblaSub, Modifier.weight(1f)) { nav.tab = 3 }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickCard(Ic.Adhkar, "أذكار الصباح والمساء", "حصن المسلم", Modifier.weight(1f)) {
                val i = adhkarIndex("الصباح والمساء")
                if (i >= 0) nav.openAdhkar(i) else nav.tab = 2
            }
            QuickCard(Ic.Adhkar, "أذكار بعد الصلاة", "التسبيح والدعاء", Modifier.weight(1f)) {
                val i = adhkarIndex("بعد السلام من الصلاة")
                if (i >= 0) nav.openAdhkar(i) else nav.tab = 2
            }
        }
    }
}

@Composable
private fun QuickCard(ic: Ic, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    val c = Noor.colors
    Column(
        modifier
            .noorCard()
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(c.primarySoft),
            contentAlignment = Alignment.Center,
        ) { NIcon(ic, tint = c.primary) }
        Spacer(Modifier.height(12.dp))
        Text(title, style = NoorType.h3, color = c.text)
        Text(sub, style = NoorType.small, color = c.muted, textAlign = TextAlign.Start)
    }
}

package app.noor.islamic.ui

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import app.noor.islamic.logic.Qibla
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Live heading of the phone, in degrees clockwise from true north. */
class CompassState {
    var heading by mutableFloatStateOf(0f)
    var hasReading by mutableStateOf(false)
    var available by mutableStateOf(true)
    var accuracy by mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
}

/**
 * Listens to the rotation-vector sensor while the screen is visible. The phone may lie flat or be held
 * upright (then the back of the phone is the pointer). The heading is smoothed so the dial doesn't jitter,
 * and corrected from magnetic to true north for the given place.
 */
@Composable
fun rememberCompass(lat: Double, lon: Double): CompassState {
    val ctx = LocalContext.current
    val state = remember { CompassState() }
    LifecycleStartEffect(lat, lon) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) {
            state.available = false
            onStopOrDispose { }
        } else {
            val declination = Math.toRadians(
                GeomagneticField(lat.toFloat(), lon.toFloat(), 0f, System.currentTimeMillis()).declination.toDouble(),
            )
            val rot = FloatArray(9)
            var sx = 0.0
            var cx = 0.0
            var seeded = false
            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    val v = if (e.values.size > 4) e.values.copyOf(4) else e.values
                    SensorManager.getRotationMatrixFromVector(rot, v)
                    // rot[7] is how far the top edge of the phone points up. Held upright, use the back of
                    // the phone as the pointer; otherwise (flat or tilted) use the top edge.
                    val east: Float
                    val north: Float
                    if (rot[7] > 0.7f) {
                        east = -rot[2]
                        north = -rot[5]
                    } else {
                        east = rot[1]
                        north = rot[4]
                    }
                    val h = atan2(east.toDouble(), north.toDouble()) + declination
                    if (!seeded) {
                        sx = sin(h)
                        cx = cos(h)
                        seeded = true
                    } else {
                        sx += 0.25 * (sin(h) - sx)
                        cx += 0.25 * (cos(h) - cx)
                    }
                    var deg = Math.toDegrees(atan2(sx, cx))
                    if (deg < 0) deg += 360.0
                    state.heading = deg.toFloat()
                    state.hasReading = true
                }

                override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {
                    state.accuracy = accuracy
                }
            }
            state.available = true
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            onStopOrDispose { sm.unregisterListener(listener) }
        }
    }
    return state
}

@Composable
fun QiblaScreen(nav: Nav) {
    val c = Noor.colors
    val place = LocalApp.current.prefs.place
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        NoorTopBar(
            "القبلة",
            onBack = null,
            trailing = {
                if (place != null) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .clickable { nav.openCities(false) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NIcon(Ic.Pin, tint = c.muted, size = 18.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(place.name, style = NoorType.small, color = c.muted)
                    }
                }
            },
        )
        if (place == null) {
            NoPlace(nav)
        } else {
            QiblaBody(place.lat, place.lon)
        }
    }
}

@Composable
private fun NoPlace(nav: Nav) {
    val c = Noor.colors
    val locate = rememberLocateAction()
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NIcon(Ic.Compass, tint = c.primary, size = 56.dp)
        Spacer(Modifier.height(16.dp))
        Text("حدّد موقعك لمعرفة اتجاه القبلة", style = NoorType.h2, color = c.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            "يُحسب الاتجاه من مكانك إلى الكعبة المشرفة. يمكنك أيضاً اختيار مدينتك يدوياً.",
            style = NoorType.body,
            color = c.muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("موقعي الحالي", onClick = locate)
            SoftButton("اختيار مدينة", onClick = { nav.openCities(false) })
        }
    }
}

@Composable
private fun QiblaBody(lat: Double, lon: Double) {
    val c = Noor.colors
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val qibla = remember(lat, lon) { Qibla.bearing(lat, lon) }
    val km = remember(lat, lon) { Qibla.distanceKm(lat, lon).roundToInt() }
    val compass = rememberCompass(lat, lon)

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var aligned by remember { mutableStateOf(false) }
    LaunchedEffect(qibla, compass) {
        snapshotFlow { compass.heading to compass.hasReading }.collect { (h, ok) ->
            val d = abs(Qibla.delta(h.toDouble(), qibla))
            aligned = ok && (if (aligned) d <= 6.0 else d <= 3.0)
        }
    }
    LaunchedEffect(aligned) {
        if (aligned) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    val turn by remember(compass, qibla) {
        derivedStateOf { Qibla.delta(compass.heading.toDouble(), qibla).roundToInt() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        CompassDial(compass, qibla.toFloat(), aligned, Modifier.fillMaxWidth(0.88f))
        Spacer(Modifier.height(18.dp))

        val live = compass.available && compass.hasReading
        val headline = when {
            !compass.available -> "اتجاه القبلة " + digits(qibla.roundToInt()) + "° من الشمال"
            !compass.hasReading -> "جارٍ قراءة البوصلة…"
            aligned -> "أنت باتجاه القبلة"
            turn > 0 -> "أدِر الهاتف يميناً " + digits(turn) + "°"
            else -> "أدِر الهاتف يساراً " + digits(-turn) + "°"
        }
        Text(
            headline,
            style = NoorType.h2,
            color = if (aligned) c.primary else c.text,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (live) "ضع الهاتف أفقياً أو أمسكه منتصباً" else if (!compass.available) "جهازك لا يحتوي على حساس بوصلة" else "",
            style = NoorType.small,
            color = c.muted,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("زاوية القبلة", digits(qibla.roundToInt()) + "°", Modifier.weight(1f))
            Stat("المسافة إلى الكعبة", digits(groupKm(km)) + " كم", Modifier.weight(1f))
        }

        if (live && compass.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            Spacer(Modifier.height(12.dp))
            Text(
                "البوصلة تحتاج إلى معايرة: حرّك الهاتف في الهواء على شكل الرقم ٨ عدة مرات.",
                style = NoorType.small,
                color = c.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .noorCard(14.dp)
                    .padding(12.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun groupKm(km: Int): String = NumberFormat.getIntegerInstance(Locale.US).format(km).replace(',', ' ')

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    val c = Noor.colors
    Column(
        modifier
            .noorCard(16.dp)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = NoorType.h2, color = c.primary, textAlign = TextAlign.Center)
        Text(label, style = NoorType.small, color = c.muted, textAlign = TextAlign.Center)
    }
}

/**
 * The dial turns against the phone, so north on the dial always points to real north.
 * The Kaaba badge sits at the qibla bearing; when it reaches the fixed marker at the top, the phone faces the qibla.
 */
@Composable
private fun CompassDial(state: CompassState, qibla: Float, aligned: Boolean, modifier: Modifier) {
    val c = Noor.colors
    Box(modifier.aspectRatio(1f)) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f - 16.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(c.surface, r, center)
            drawCircle(c.line, r, center, style = Stroke(1.dp.toPx()))
            drawCircle(c.line, r * 0.6f, center, style = Stroke(1.dp.toPx()))
            val p = Path().apply {
                moveTo(center.x - 8.dp.toPx(), 1.dp.toPx())
                lineTo(center.x + 8.dp.toPx(), 1.dp.toPx())
                lineTo(center.x, 17.dp.toPx())
                close()
            }
            drawPath(p, if (aligned) c.accent else c.primary)
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .graphicsLayer { rotationZ = -state.heading },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                for (deg in 0 until 360 step 5) {
                    val major = deg % 30 == 0
                    val len = (if (major) 10.dp else 5.dp).toPx()
                    val a = Math.toRadians(deg.toDouble())
                    val dx = sin(a).toFloat()
                    val dy = -cos(a).toFloat()
                    drawLine(
                        color = if (major) c.text.copy(alpha = 0.7f) else c.muted.copy(alpha = 0.45f),
                        start = Offset(center.x + dx * (r - 2.dp.toPx()), center.y + dy * (r - 2.dp.toPx())),
                        end = Offset(center.x + dx * (r - 2.dp.toPx() - len), center.y + dy * (r - 2.dp.toPx() - len)),
                        strokeWidth = (if (major) 1.6.dp else 1.dp).toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            val cardinals = listOf(0f to "ش", 90f to "ق", 180f to "ج", 270f to "غ")
            for ((angle, label) in cardinals) {
                Box(Modifier.fillMaxSize().rotate(angle)) {
                    Text(
                        label,
                        style = NoorType.h2,
                        color = if (angle == 0f) c.primary else c.muted,
                        fontWeight = if (angle == 0f) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 26.dp)
                            .rotate(-angle),
                    )
                }
            }

            Box(Modifier.fillMaxSize().rotate(qibla)) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawLine(
                        color = c.accent.copy(alpha = 0.55f),
                        start = center,
                        end = Offset(center.x, 40.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (aligned) c.primary else c.surface)
                        .border(1.5.dp, c.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    KaabaMark()
                }
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            drawCircle(c.primary, 5.dp.toPx(), Offset(size.width / 2f, size.height / 2f))
        }
    }
}

@Composable
private fun KaabaMark() {
    val gold = Noor.colors.accent
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = Color(0xFF151515),
            topLeft = Offset(w * 0.08f, h * 0.1f),
            size = Size(w * 0.84f, h * 0.8f),
            cornerRadius = CornerRadius(w * 0.1f),
        )
        drawRect(gold, Offset(w * 0.08f, h * 0.3f), Size(w * 0.84f, h * 0.11f))
    }
}

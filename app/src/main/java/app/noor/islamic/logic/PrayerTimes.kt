package app.noor.islamic.logic

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** Calculation conventions. Angles are the sun's depression below the horizon at dawn / nightfall. */
enum class Method(val label: String, val fajrAngle: Double, val ishaAngle: Double, val ishaMinutes: Int) {
    MWL("رابطة العالم الإسلامي", 18.0, 17.0, 0),
    EGYPT("الهيئة المصرية العامة للمساحة", 19.5, 17.5, 0),
    UMM_AL_QURA("أم القرى — مكة المكرمة", 18.5, 0.0, 90),
    KARACHI("جامعة العلوم الإسلامية — كراتشي", 18.0, 18.0, 0),
    ISNA("أمريكا الشمالية (ISNA)", 15.0, 15.0, 0),
    KUWAIT("الكويت", 18.0, 17.5, 0),
    QATAR("قطر", 18.0, 0.0, 90),
    TURKEY("رئاسة الشؤون الدينية — تركيا", 18.0, 17.0, 0),
}

/** Index order of the six daily events returned by [PrayerCalc.compute]. */
object Prayer {
    const val FAJR = 0
    const val SUNRISE = 1
    const val DHUHR = 2
    const val ASR = 3
    const val MAGHRIB = 4
    const val ISHA = 5
    val names = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")

    /** Bit used in the notification mask (sunrise has none). */
    fun bit(index: Int): Int = if (index == SUNRISE) 0 else 1 shl index

    /** Mask with all five daily prayers switched on (sunrise has no notification). */
    const val ALL_MASK = (1 shl FAJR) or (1 shl DHUHR) or (1 shl ASR) or (1 shl MAGHRIB) or (1 shl ISHA)
}

class Upcoming(val index: Int, val time: ZonedDateTime, val previous: ZonedDateTime)

/**
 * Prayer times from the sun's position (the well-known PrayTimes.org algorithm), no network needed.
 * Pure Kotlin: no Android dependencies, so it can be unit-tested on a plain JVM.
 */
object PrayerCalc {
    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)

    private fun fixAngle(a: Double): Double {
        var x = a % 360.0
        if (x < 0) x += 360.0
        return x
    }

    private fun fixHour(h: Double): Double {
        var x = h % 24.0
        if (x < 0) x += 24.0
        return x
    }

    private fun julian(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private class Sun(val declination: Double, val equation: Double)

    private fun sunPosition(jd: Double): Sun {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(rad(g)) + 0.020 * sin(rad(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val ra = deg(atan2(cos(rad(e)) * sin(rad(l)), cos(rad(l)))) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = deg(asin(sin(rad(e)) * sin(rad(l))))
        return Sun(decl, eqt)
    }

    /** The six events of [date] at the given place, in order fajr, sunrise, dhuhr, asr, maghrib, isha. */
    fun compute(
        date: LocalDate,
        lat: Double,
        lon: Double,
        zone: ZoneId,
        method: Method,
        hanafi: Boolean,
    ): List<ZonedDateTime> {
        val noon = date.atTime(12, 0).atZone(zone)
        val offsetHours = zone.rules.getOffset(noon.toInstant()).totalSeconds / 3600.0
        val jd = julian(date.year, date.monthValue, date.dayOfMonth) - lon / (15.0 * 24.0)

        fun midDay(t: Double): Double = fixHour(12.0 - sunPosition(jd + t).equation)

        fun sunAngleTime(angle: Double, t: Double, ccw: Boolean): Double {
            val decl = sunPosition(jd + t).declination
            val x = (-sin(rad(angle)) - sin(rad(decl)) * sin(rad(lat))) / (cos(rad(decl)) * cos(rad(lat)))
            if (x > 1.0 || x < -1.0) return Double.NaN
            val dt = deg(acos(x)) / 15.0
            return midDay(t) + if (ccw) -dt else dt
        }

        fun asrTime(factor: Int, t: Double): Double {
            val decl = sunPosition(jd + t).declination
            val angle = -deg(atan(1.0 / (factor + tan(rad(abs(lat - decl))))))
            return sunAngleTime(angle, t, false)
        }

        // Start from rough guesses (as fractions of a day) and refine once.
        var tFajr = 5.0 / 24
        var tSunrise = 6.0 / 24
        var tDhuhr = 12.0 / 24
        var tAsr = 13.0 / 24
        var tSunset = 18.0 / 24
        var tIsha = 18.0 / 24

        val riseSet = 0.833
        var fajr = Double.NaN
        var sunrise = Double.NaN
        var dhuhr = Double.NaN
        var asr = Double.NaN
        var sunset = Double.NaN
        var isha = Double.NaN
        for (pass in 0 until 2) {
            fajr = sunAngleTime(method.fajrAngle, tFajr, true)
            sunrise = sunAngleTime(riseSet, tSunrise, true)
            dhuhr = midDay(tDhuhr)
            asr = asrTime(if (hanafi) 2 else 1, tAsr)
            sunset = sunAngleTime(riseSet, tSunset, false)
            isha = if (method.ishaMinutes == 0) sunAngleTime(method.ishaAngle, tIsha, false) else Double.NaN
            if (pass == 0) {
                if (!fajr.isNaN()) tFajr = fajr / 24
                if (!sunrise.isNaN()) tSunrise = sunrise / 24
                if (!dhuhr.isNaN()) tDhuhr = dhuhr / 24
                if (!asr.isNaN()) tAsr = asr / 24
                if (!sunset.isNaN()) tSunset = sunset / 24
                if (!isha.isNaN()) tIsha = isha / 24
            }
        }

        // Local clock time.
        val adj = offsetHours - lon / 15.0
        fajr += adj
        sunrise += adj
        dhuhr += adj
        asr += adj
        sunset += adj
        isha += adj
        var maghrib = sunset

        // Very high latitudes: the sun may never reach the angle, so limit to a share of the night.
        val night = fixHour(sunrise - sunset)
        fun limit(time: Double, base: Double, angle: Double, ccw: Boolean): Double {
            val portion = angle / 60.0 * night
            val diff = if (ccw) fixHour(base - time) else fixHour(time - base)
            return if (time.isNaN() || diff > portion) base + (if (ccw) -portion else portion) else time
        }
        if (!sunrise.isNaN() && !sunset.isNaN()) {
            fajr = limit(fajr, sunrise, method.fajrAngle, true)
            if (method.ishaMinutes == 0) isha = limit(isha, sunset, method.ishaAngle, false)
        }
        if (method.ishaMinutes != 0) isha = maghrib + method.ishaMinutes / 60.0

        val hours = doubleArrayOf(fajr, sunrise, dhuhr, asr, maghrib, isha)
        return hours.map { toTime(date, it, zone) }
    }

    private fun toTime(date: LocalDate, hours: Double, zone: ZoneId): ZonedDateTime {
        val h = if (hours.isNaN()) 12.0 else hours
        val minutes = Math.round(h * 60.0)
        return date.atStartOfDay().plusMinutes(minutes).atZone(zone)
    }

    /** Which event comes next after [now]; the three lists are yesterday's, today's and tomorrow's events. */
    fun pickUpcoming(
        now: ZonedDateTime,
        yesterday: List<ZonedDateTime>,
        today: List<ZonedDateTime>,
        tomorrow: List<ZonedDateTime>,
    ): Upcoming {
        val idx = today.indexOfFirst { it.isAfter(now) }
        if (idx >= 0) {
            val prev = if (idx > 0) today[idx - 1] else yesterday.last()
            return Upcoming(idx, today[idx], prev)
        }
        return Upcoming(0, tomorrow[0], today.last())
    }
}

package app.noor.islamic.logic

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object Qibla {
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LON = 39.826206

    /** Initial great-circle bearing from the given point to the Kaaba, degrees clockwise from true north. */
    fun bearing(lat: Double, lon: Double): Double {
        val p1 = Math.toRadians(lat)
        val p2 = Math.toRadians(KAABA_LAT)
        val dl = Math.toRadians(KAABA_LON - lon)
        val y = sin(dl)
        val x = cos(p1) * tan(p2) - sin(p1) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun distanceKm(lat: Double, lon: Double): Double {
        val p1 = Math.toRadians(lat)
        val p2 = Math.toRadians(KAABA_LAT)
        val dp = p2 - p1
        val dl = Math.toRadians(KAABA_LON - lon)
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * 6371.0 * asin(sqrt(a))
    }

    /** Smallest signed angle from [from] to [to], in (-180, 180]. */
    fun delta(from: Double, to: Double): Double {
        var d = (to - from) % 360.0
        if (d > 180) d -= 360.0
        if (d <= -180) d += 360.0
        return d
    }
}

package app.noor.islamic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/** One-shot location without Google Play services (works on every device that has a location provider). */
object LocationHelper {
    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Calls [onResult] on the main thread with the location, or null if none could be found. */
    fun locate(ctx: Context, onResult: (Location?) -> Unit) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers: List<String> = try {
            lm.getProviders(true)
        } catch (e: SecurityException) {
            emptyList()
        }

        var best: Location? = null
        for (p in providers) {
            val l: Location? = try {
                lm.getLastKnownLocation(p)
            } catch (e: SecurityException) {
                null
            }
            val current = best
            if (l != null && (current == null || l.time > current.time)) best = l
        }

        val last = best
        // A fix from the last few hours is plenty for prayer times and the qibla.
        if (last != null && System.currentTimeMillis() - last.time < 6 * 3600_000L) {
            onResult(last)
            return
        }

        val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .firstOrNull { it in providers }
        if (provider == null) {
            onResult(last)
            return
        }
        try {
            LocationManagerCompat.getCurrentLocation(
                lm,
                provider,
                CancellationSignal(),
                ContextCompat.getMainExecutor(ctx),
            ) { loc: Location? -> onResult(loc ?: last) }
        } catch (e: SecurityException) {
            onResult(last)
        }
    }
}

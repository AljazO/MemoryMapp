package si.uni_lj.fe.tnuv.memorymapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*

class LocationTracker(
    private val context: Context,
    private val onLocationUpdate: (Location) -> Unit
) {
    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null
    private var lastSavedLocation: Location? = null

    private val MIN_DISTANCE_METERS = 11.0f
    private val MAX_ACCURACY_METERS = 50.0f
    private val MAX_SPEED_MS = 30.0f

    fun start() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.lastOrNull()?.let { location ->
                    if (isSignificantChange(location)) {
                        lastSavedLocation = location
                        onLocationUpdate(location)
                    }
                }
            }
        }

        requestUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun requestUpdates() {
        locationCallback?.let {
            fusedLocationClient.requestLocationUpdates(
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMinUpdateIntervalMillis(2000)
                    .build(),
                it,
                Looper.getMainLooper()
            )
        }
    }

    fun stop() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    private fun isSignificantChange(location: Location): Boolean {
        // 1. Accuracy check: ignore points with poor accuracy
        if (location.accuracy > MAX_ACCURACY_METERS) return false

        lastSavedLocation?.let { last ->
            // 2. Distance threshold check: only trigger if moved more than threshold
            val distance = last.distanceTo(location)
            if (distance < MIN_DISTANCE_METERS) return false

            // 3. Speed check: ignore unrealistic jumps
            val timeDelta = (location.time - last.time) / 1000.0
            if (timeDelta > 0) {
                val speed = distance / timeDelta
                if (speed > MAX_SPEED_MS) return false
            }
        }

        return true
    }
}

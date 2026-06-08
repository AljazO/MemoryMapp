package si.uni_lj.fe.tnuv.memorymapp.utils

import android.location.Location
import si.uni_lj.fe.tnuv.memorymapp.data.LocationPoint

data class AdvancedStats(
    val distanceKm: Double = 0.0,
    val durationMillis: Long = 0,
    val calories: Int = 0,
    val steps: Int = 0,
    val avgPaceKmh: Double = 0.0,
    val elevationGainM: Double = 0.0
)

object StatisticsCalculator {

    fun calculateStats(points: List<LocationPoint>, startTime: Long, endTime: Long): AdvancedStats {
        val duration = endTime - startTime
        
        if (points.isEmpty()) {
            return AdvancedStats(durationMillis = duration)
        }

        var totalDistance = 0.0
        var elevationGain = 0.0
        var totalSteps = 0

        // Iterate through points to sum up deltas
        for (i in 0 until points.size - 1) {
            val current = points[i]
            val next = points[i + 1]

            // Distance delta
            val results = FloatArray(1)
            Location.distanceBetween(
                current.latitude, current.longitude,
                next.latitude, next.longitude,
                results
            )
            totalDistance += results[0]

            // Elevation Gain (sum of positive altitude changes)
            val altDelta = next.altitude - current.altitude
            if (altDelta > 0) {
                elevationGain += altDelta
            }

            // Steps (handling potential reboots where counter resets to 0)
            if (current.totalStepsAtTimestamp > 0 && next.totalStepsAtTimestamp > 0) {
                val stepDelta = if (next.totalStepsAtTimestamp >= current.totalStepsAtTimestamp) {
                    next.totalStepsAtTimestamp - current.totalStepsAtTimestamp
                } else {
                    next.totalStepsAtTimestamp
                }
                totalSteps += stepDelta
            }
        }

        val distanceKm = totalDistance / 1000.0
        val durationHours = duration / (1000.0 * 60 * 60)
        val avgPaceKmh = if (durationHours > 0) distanceKm / durationHours else 0.0
        val calories = (totalSteps * 0.04).toInt()

        return AdvancedStats(
            distanceKm = distanceKm,
            durationMillis = duration,
            calories = calories,
            steps = totalSteps,
            avgPaceKmh = avgPaceKmh,
            elevationGainM = elevationGain
        )
    }
    
    fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}

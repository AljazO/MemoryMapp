package si.uni_lj.fe.tnuv.memorymapp.utils

import android.location.Location
import si.uni_lj.fe.tnuv.memorymapp.data.LocationDao
import si.uni_lj.fe.tnuv.memorymapp.data.LocationPoint

object TrackSmoother {

    /**
     * Smoothens a track by deleting points that represent movement below the threshold.
     * This follows the "save then delete" strategy to keep the path clean.
     */
    suspend fun smoothTrack(locationDao: LocationDao, userId: String?, startTime: Long, endTime: Long, thresholdMeters: Float = 5f) {
        if (userId == null) return
        val points = locationDao.getPointsInRangeSync(userId, startTime, endTime)
        if (points.size < 3) return

        val idsToDelete = mutableListOf<Long>()
        var lastKeptPoint = points.first()

        for (i in 1 until points.size - 1) {
            val currentPoint = points[i]
            
            val distance = FloatArray(1)
            Location.distanceBetween(
                lastKeptPoint.latitude, lastKeptPoint.longitude,
                currentPoint.latitude, currentPoint.longitude,
                distance
            )

            if (distance[0] < thresholdMeters) {
                // Movement is too small, mark for deletion
                idsToDelete.add(currentPoint.id)
            } else {
                // Significant movement, keep this point and use it as the new reference
                lastKeptPoint = currentPoint
            }
        }

        // We usually keep the very last point to ensure the track ends correctly
        // unless it's identical to the last kept point.
        val lastPoint = points.last()
        val finalDistance = FloatArray(1)
        Location.distanceBetween(
            lastKeptPoint.latitude, lastKeptPoint.longitude,
            lastPoint.latitude, lastPoint.longitude,
            finalDistance
        )
        if (finalDistance[0] < thresholdMeters && lastPoint.id != lastKeptPoint.id) {
            idsToDelete.add(lastPoint.id)
        }

        if (idsToDelete.isNotEmpty()) {
            locationDao.deletePointsByIds(idsToDelete)
        }
    }
}

package si.uni_lj.fe.tnuv.memorymapp.utils

import android.location.Location
import si.uni_lj.fe.tnuv.memorymapp.data.LocationDao

object TrackSmoother {

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
                idsToDelete.add(currentPoint.id)
            } else {
                lastKeptPoint = currentPoint
            }
        }

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

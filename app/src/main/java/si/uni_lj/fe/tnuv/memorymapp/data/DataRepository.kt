package si.uni_lj.fe.tnuv.memorymapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await

class DataRepository(
    private val locationDao: LocationDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun syncTripToFirebase(trip: Trip): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            
            // 1. Save Trip Metadata
            firestore.collection("users")
                .document(userId)
                .collection("trips")
                .document(trip.id.toString())
                .set(trip)
                .await()

            // 2. Sync Location Points (the path) for this trip
            val points = locationDao.getPointsInRangeSync(userId, trip.startTime, trip.endTime)
            
            // Sync in batches (Firestore limit is 500)
            if (points.isNotEmpty()) {
                points.chunked(400).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { point ->
                        val pointDoc = firestore.collection("users")
                            .document(userId)
                            .collection("trips")
                            .document(trip.id.toString())
                            .collection("points")
                            .document(point.id.toString())
                        batch.set(pointDoc, point)
                    }
                    batch.commit().await()
                }
            }

            // 3. Sync Media Points (photos/videos) for this trip
            val media = locationDao.getMediaInRangeSync(userId, trip.startTime, trip.endTime)
            if (media.isNotEmpty()) {
                media.chunked(400).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { item ->
                        val mediaDoc = firestore.collection("users")
                            .document(userId)
                            .collection("trips")
                            .document(trip.id.toString())
                            .collection("media")
                            .document(item.id)
                        batch.set(mediaDoc, item)
                    }
                    batch.commit().await()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncAllTrips() {
        val userId = auth.currentUser?.uid ?: return
        val trips = locationDao.getAllTripsSync(userId)
        trips.forEach { syncTripToFirebase(it) }
    }
}

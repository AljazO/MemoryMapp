package si.uni_lj.fe.tnuv.memorymapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert
    suspend fun insert(point: LocationPoint)

    @Query("SELECT * FROM location_points WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getPointsInRange(startTime: Long, endTime: Long): Flow<List<LocationPoint>>

    @Query("SELECT * FROM location_points WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    suspend fun getPointsInRangeSync(startTime: Long, endTime: Long): List<LocationPoint>

    @Query("DELETE FROM location_points")
    suspend fun deleteAll()

    // Media Methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(mediaPoint: MediaPoint)

    @Query("SELECT * FROM media_points WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getMediaInRange(startTime: Long, endTime: Long): Flow<List<MediaPoint>>

    @Query("SELECT * FROM media_points")
    suspend fun getAllMediaSync(): List<MediaPoint>

    @Query("SELECT id FROM media_points")
    suspend fun getAllMediaIds(): List<String>

    @Query("DELETE FROM media_points WHERE id = :id")
    suspend fun deleteMediaById(id: String)

    @Query("UPDATE media_points SET isLiked = :isLiked WHERE id = :id")
    suspend fun updateMediaLikeStatus(id: String, isLiked: Boolean)

    // Trip Methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip)

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTripById(id: Long)
}

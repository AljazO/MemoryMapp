package si.uni_lj.fe.tnuv.memorymapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert
    suspend fun insert(point: LocationPoint)

    @Query("SELECT * FROM location_points WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getPointsInRange(userId: String, startTime: Long, endTime: Long): Flow<List<LocationPoint>>

    @Query("SELECT * FROM location_points WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    suspend fun getPointsInRangeSync(userId: String, startTime: Long, endTime: Long): List<LocationPoint>

    @Query("DELETE FROM location_points WHERE id IN (:ids)")
    suspend fun deletePointsByIds(ids: List<Long>)

    @Query("DELETE FROM location_points")
    suspend fun deleteAll()

    // Media Methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(mediaPoint: MediaPoint)

    @Query("SELECT * FROM media_points WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getMediaInRange(userId: String, startTime: Long, endTime: Long): Flow<List<MediaPoint>>

    @Query("SELECT * FROM media_points WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    suspend fun getMediaInRangeSync(userId: String, startTime: Long, endTime: Long): List<MediaPoint>

    @Query("SELECT * FROM media_points WHERE userId = :userId")
    suspend fun getAllMediaSync(userId: String): List<MediaPoint>

    @Query("SELECT id FROM media_points WHERE userId = :userId")
    suspend fun getAllMediaIds(userId: String): List<String>

    @Query("DELETE FROM media_points WHERE id = :id AND userId = :userId")
    suspend fun deleteMediaById(id: String, userId: String)

    @Query("UPDATE media_points SET isLiked = :isLiked WHERE id = :id AND userId = :userId")
    suspend fun updateMediaLikeStatus(id: String, userId: String, isLiked: Boolean)

    // Trip Methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("SELECT * FROM trips WHERE userId = :userId ORDER BY startTime DESC")
    fun getAllTrips(userId: String): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE userId = :userId ORDER BY startTime DESC")
    suspend fun getAllTripsSync(userId: String): List<Trip>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: Long): Trip?

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTripById(id: Long)
}

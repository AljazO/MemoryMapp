package si.uni_lj.fe.tnuv.memorymapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert
    suspend fun insert(point: LocationPoint)

    @Query("SELECT * FROM location_points WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getPointsInRange(startTime: Long, endTime: Long): Flow<List<LocationPoint>>

    @Query("DELETE FROM location_points")
    suspend fun deleteAll()

    // Media Methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(mediaPoint: MediaPoint)

    @Query("SELECT * FROM media_points WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getMediaInRange(startTime: Long, endTime: Long): Flow<List<MediaPoint>>

    @Query("SELECT id FROM media_points")
    suspend fun getAllMediaIds(): List<Long>

    @Query("DELETE FROM media_points WHERE id = :id")
    suspend fun deleteMediaById(id: Long)

    @Query("UPDATE media_points SET isLiked = :isLiked WHERE id = :id")
    suspend fun updateMediaLikeStatus(id: Long, isLiked: Boolean)
}

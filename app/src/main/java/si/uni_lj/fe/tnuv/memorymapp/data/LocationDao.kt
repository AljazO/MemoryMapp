package si.uni_lj.fe.tnuv.memorymapp.data

import androidx.room.Dao
import androidx.room.Insert
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
}

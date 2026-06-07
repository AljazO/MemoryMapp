package si.uni_lj.fe.tnuv.memorymapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "location_points")
data class LocationPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String? = null,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val altitude: Double = 0.0,
    val totalStepsAtTimestamp: Int = 0
)

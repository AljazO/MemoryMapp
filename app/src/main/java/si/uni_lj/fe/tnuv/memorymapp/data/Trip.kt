package si.uni_lj.fe.tnuv.memorymapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val startTime: Long,
    val endTime: Long,
    val coverPhotoUri: String? = null
)

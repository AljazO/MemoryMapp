package si.uni_lj.fe.tnuv.memorymapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType {
    IMAGE, VIDEO
}

@Entity(tableName = "media_points", primaryKeys = ["id", "userId"])
data class MediaPoint(
    val id: String, 
    val userId: String,
    val uri: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val type: MediaType,
    val isLiked: Boolean = false
)

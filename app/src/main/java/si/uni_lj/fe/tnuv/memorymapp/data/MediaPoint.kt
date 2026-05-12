package si.uni_lj.fe.tnuv.memorymapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType {
    IMAGE, VIDEO
}

@Entity(tableName = "media_points")
data class MediaPoint(
    @PrimaryKey val id: String, // Changed to String to avoid collisions
    val uri: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val type: MediaType,
    val isLiked: Boolean = false // Added isLiked field
)

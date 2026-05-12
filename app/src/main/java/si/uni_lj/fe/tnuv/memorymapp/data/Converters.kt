package si.uni_lj.fe.tnuv.memorymapp.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMediaType(value: MediaType): String {
        return value.name
    }

    @TypeConverter
    fun toMediaType(value: String): MediaType {
        return MediaType.valueOf(value)
    }
}

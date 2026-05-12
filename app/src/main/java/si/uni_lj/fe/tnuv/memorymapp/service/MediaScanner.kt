package si.uni_lj.fe.tnuv.memorymapp.service

import android.content.ContentUris
import android.content.Context
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import si.uni_lj.fe.tnuv.memorymapp.data.AppDatabase
import si.uni_lj.fe.tnuv.memorymapp.data.MediaPoint
import si.uni_lj.fe.tnuv.memorymapp.data.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaScanner(private val context: Context) {

    suspend fun scanGallery() = withContext(Dispatchers.IO) {
        // 1. Fetch images
        scanImages()

        // 2. Fetch videos
        scanVideos()
    }

    private suspend fun scanImages() {
        val database = AppDatabase.getDatabase(context)
        val dao = database.locationDao()

        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN
        )

        val cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val date = it.getLong(dateColumn)
                val contentUri = ContentUris.withAppendedId(uri, id)

                // For Android 10+, we need setRequireOriginal to get location from EXIF
                val photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.setRequireOriginal(contentUri)
                } else {
                    contentUri
                }

                try {
                    context.contentResolver.openInputStream(photoUri)?.use { stream ->
                        val exif = ExifInterface(stream)
                        val latLong = FloatArray(2)
                        if (exif.getLatLong(latLong)) {
                            dao.insertMedia(
                                MediaPoint(
                                    id = id,
                                    uri = contentUri.toString(),
                                    latitude = latLong[0].toDouble(),
                                    longitude = latLong[1].toDouble(),
                                    timestamp = date,
                                    type = MediaType.IMAGE
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun scanVideos() {
        val database = AppDatabase.getDatabase(context)
        val dao = database.locationDao()

        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_TAKEN
        )

        val cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_TAKEN} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val date = it.getLong(dateColumn)
                val contentUri = ContentUris.withAppendedId(uri, id)

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, contentUri)
                    val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                    if (location != null) {
                        // Location format: "+46.0569+014.5058/"
                        parseLocation(location)?.let { (lat, lon) ->
                            dao.insertMedia(
                                MediaPoint(
                                    id = id,
                                    uri = contentUri.toString(),
                                    latitude = lat,
                                    longitude = lon,
                                    timestamp = date,
                                    type = MediaType.VIDEO
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.release()
                }
            }
        }
    }

    private fun parseLocation(location: String): Pair<Double, Double>? {
        return try {
            // Very basic parser for ISO 6709 format like "+46.0569+014.5058/"
            val regex = Regex("([+-][0-9.]+)([+-][0-9.]+)")
            val match = regex.find(location)
            if (match != null) {
                val lat = match.groupValues[1].toDouble()
                val lon = match.groupValues[2].toDouble()
                Pair(lat, lon)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

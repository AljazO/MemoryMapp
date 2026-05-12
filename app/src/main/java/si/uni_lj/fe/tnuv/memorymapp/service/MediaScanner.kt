package si.uni_lj.fe.tnuv.memorymapp.service

import android.content.ContentUris
import android.content.Context
import androidx.exifinterface.media.ExifInterface
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
        val database = AppDatabase.getDatabase(context)
        val dao = database.locationDao()
        val existingIds = dao.getAllMediaIds().toSet()

        // 1. Fetch images
        val foundImages = scanImages(existingIds)

        // 2. Fetch videos
        val foundVideos = scanVideos(existingIds)

        val allFoundIds = foundImages + foundVideos

        // 3. Cleanup: Remove media from DB that no longer exists in MediaStore or was filtered out
        val idsToRemove = existingIds.filter { !allFoundIds.contains(it) }
        idsToRemove.forEach { id ->
            dao.deleteMediaById(id)
        }
    }

    private suspend fun scanImages(existingIds: Set<String>): Set<String> {
        val foundIds = mutableSetOf<String>()
        val database = AppDatabase.getDatabase(context)
        val dao = database.locationDao()

        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.RELATIVE_PATH
        } else {
            MediaStore.Images.Media.DATA
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            pathColumn
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
            val dateTakenColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val pathColIndex = it.getColumnIndexOrThrow(pathColumn)

            while (it.moveToNext()) {
                // Filter out Screenshots and Downloads
                val path = it.getString(pathColIndex) ?: ""
                if (path.contains("Screenshots", ignoreCase = true) ||
                    path.contains("Download", ignoreCase = true)) {
                    continue
                }

                val mediaId = it.getLong(idColumn)
                val stringId = "img_$mediaId"
                foundIds.add(stringId)

                if (existingIds.contains(stringId)) continue

                var date = it.getLong(dateTakenColumn)
                if (date == 0L) {
                    date = it.getLong(dateAddedColumn) * 1000 // DATE_ADDED is in seconds
                }

                val contentUri = ContentUris.withAppendedId(uri, mediaId)

                // For Android 10+, we need setRequireOriginal to get location from EXIF
                val photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        MediaStore.setRequireOriginal(contentUri)
                    } catch (e: SecurityException) {
                        contentUri
                    }
                } else {
                    contentUri
                }

                try {
                    var lat: Double? = null
                    var lon: Double? = null

                    context.contentResolver.openInputStream(photoUri)?.use { stream ->
                        val exif = ExifInterface(stream)
                        val latLong = exif.latLong
                        if (latLong != null) {
                            lat = latLong[0]
                            lon = latLong[1]
                        }
                    }

                    // Fallback to app location if EXIF is missing
                    if (lat == null || lon == null) {
                        val range = 300000L // 5 minutes
                        val closest = dao.getPointsInRangeSync(date - range, date + range)
                            .minByOrNull { Math.abs(it.timestamp - date) }
                        if (closest != null) {
                            lat = closest.latitude
                            lon = closest.longitude
                        }
                    }

                    if (lat != null && lon != null) {
                        dao.insertMedia(
                            MediaPoint(
                                id = stringId,
                                uri = contentUri.toString(),
                                latitude = lat!!,
                                longitude = lon!!,
                                timestamp = date,
                                type = MediaType.IMAGE
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return foundIds
    }

    private suspend fun scanVideos(existingIds: Set<String>): Set<String> {
        val foundIds = mutableSetOf<String>()
        val database = AppDatabase.getDatabase(context)
        val dao = database.locationDao()

        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.RELATIVE_PATH
        } else {
            MediaStore.Video.Media.DATA
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            pathColumn
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
            val dateTakenColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val pathColIndex = it.getColumnIndexOrThrow(pathColumn)

            while (it.moveToNext()) {
                // Filter out Screenshots and Downloads
                val path = it.getString(pathColIndex) ?: ""
                if (path.contains("Screenshots", ignoreCase = true) ||
                    path.contains("Download", ignoreCase = true)) {
                    continue
                }

                val mediaId = it.getLong(idColumn)
                val stringId = "vid_$mediaId"
                foundIds.add(stringId)

                if (existingIds.contains(stringId)) continue

                var date = it.getLong(dateTakenColumn)
                if (date == 0L) {
                    date = it.getLong(dateAddedColumn) * 1000
                }

                val contentUri = ContentUris.withAppendedId(uri, mediaId)

                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, contentUri)
                    val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                    
                    var lat: Double? = null
                    var lon: Double? = null

                    if (location != null) {
                        parseLocation(location)?.let { (l1, l2) ->
                            lat = l1
                            lon = l2
                        }
                    }

                    // Fallback to app location
                    if (lat == null || lon == null) {
                        val range = 300000L // 5 minutes
                        val closest = dao.getPointsInRangeSync(date - range, date + range)
                            .minByOrNull { Math.abs(it.timestamp - date) }
                        if (closest != null) {
                            lat = closest.latitude
                            lon = closest.longitude
                        }
                    }

                    if (lat != null && lon != null) {
                        dao.insertMedia(
                            MediaPoint(
                                id = stringId,
                                uri = contentUri.toString(),
                                latitude = lat!!,
                                longitude = lon!!,
                                timestamp = date,
                                type = MediaType.VIDEO
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.release()
                }
            }
        }
        return foundIds
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

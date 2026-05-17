package si.uni_lj.fe.tnuv.memorymapp.service

import android.content.ContentUris
import android.content.Context
import androidx.exifinterface.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import si.uni_lj.fe.tnuv.memorymapp.data.AppDatabase
import si.uni_lj.fe.tnuv.memorymapp.data.MediaPoint
import si.uni_lj.fe.tnuv.memorymapp.data.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

class MediaScanner(private val context: Context) {

    suspend fun scanGallery() = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            val dao = database.locationDao()
            val existingIds = dao.getAllMediaIds().toSet()

            // 1. Fetch images
            val foundImages = scanImages(existingIds)

            // 2. Fetch videos
            val foundVideos = scanVideos(existingIds)

            val allFoundIds = foundImages + foundVideos

            // 3. Cleanup: Remove media from DB that no longer exists in MediaStore or is inaccessible
            val idsToRemove = existingIds.filter { !allFoundIds.contains(it) }
            if (idsToRemove.isNotEmpty()) {
                Log.d("MediaScanner", "Removing ${idsToRemove.size} stale media items from database")
                idsToRemove.forEach { id ->
                    dao.deleteMediaById(id)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("MediaScanner", "Error scanning gallery", e)
        }
    }

    private fun isUriAccessible(uri: Uri): Boolean {
        return try {
            // Verifies the file actually exists and is readable.
            // This catches cases where MediaStore index is stale after a deletion.
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun scanImages(existingIds: Set<String>): Set<String> {
        val foundIds = mutableSetOf<String>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.RELATIVE_PATH
        } else {
            MediaStore.Images.Media.DATA
        }

        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            pathColumn
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projection.add(MediaStore.Images.Media.IS_TRASHED)
        }

        val cursor = context.contentResolver.query(
            uri,
            projection.toTypedArray(),
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateTakenColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val pathColIndex = it.getColumnIndexOrThrow(pathColumn)
            val trashedColIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                it.getColumnIndex(MediaStore.Images.Media.IS_TRASHED)
            } else -1

            while (it.moveToNext()) {
                // Ignore trashed items
                if (trashedColIndex != -1 && it.getInt(trashedColIndex) != 0) continue

                val mediaId = it.getLong(idColumn)
                val stringId = "img_$mediaId"
                val contentUri = ContentUris.withAppendedId(uri, mediaId)

                // Verify file actually exists and is accessible
                if (!isUriAccessible(contentUri)) continue

                // Filter out Screenshots and Downloads
                val path = it.getString(pathColIndex) ?: ""
                if (path.contains("Screenshots", ignoreCase = true) ||
                    path.contains("Download", ignoreCase = true)) {
                    continue
                }

                foundIds.add(stringId)

                if (existingIds.contains(stringId)) continue

                var date = it.getLong(dateTakenColumn)
                if (date == 0L) {
                    date = it.getLong(dateAddedColumn) * 1000 // DATE_ADDED is in seconds
                }

                processImageInsertion(stringId, contentUri, date)
            }
        }
        return foundIds
    }

    private suspend fun scanVideos(existingIds: Set<String>): Set<String> {
        val foundIds = mutableSetOf<String>()
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.RELATIVE_PATH
        } else {
            MediaStore.Video.Media.DATA
        }

        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            pathColumn
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projection.add(MediaStore.Video.Media.IS_TRASHED)
        }

        val cursor = context.contentResolver.query(
            uri,
            projection.toTypedArray(),
            null,
            null,
            "${MediaStore.Video.Media.DATE_TAKEN} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateTakenColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val pathColIndex = it.getColumnIndexOrThrow(pathColumn)
            val trashedColIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                it.getColumnIndex(MediaStore.Video.Media.IS_TRASHED)
            } else -1

            while (it.moveToNext()) {
                if (trashedColIndex != -1 && it.getInt(trashedColIndex) != 0) continue

                val mediaId = it.getLong(idColumn)
                val stringId = "vid_$mediaId"
                val contentUri = ContentUris.withAppendedId(uri, mediaId)

                if (!isUriAccessible(contentUri)) continue

                val path = it.getString(pathColIndex) ?: ""
                if (path.contains("Screenshots", ignoreCase = true) ||
                    path.contains("Download", ignoreCase = true)) {
                    continue
                }

                foundIds.add(stringId)

                if (existingIds.contains(stringId)) continue

                var date = it.getLong(dateTakenColumn)
                if (date == 0L) {
                    date = it.getLong(dateAddedColumn) * 1000
                }

                processVideoInsertion(stringId, contentUri, date)
            }
        }
        return foundIds
    }

    private suspend fun processImageInsertion(id: String, contentUri: Uri, date: Long) {
        val database = AppDatabase.getDatabase(context)
        val dao = database.locationDao()
        
        val photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                MediaStore.setRequireOriginal(contentUri)
            } catch (e: Exception) {
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
                        id = id,
                        uri = contentUri.toString(),
                        latitude = lat!!,
                        longitude = lon!!,
                        timestamp = date,
                        type = MediaType.IMAGE
                    )
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w("MediaScanner", "Failed to process image $id", e)
        }
    }

    private suspend fun processVideoInsertion(id: String, contentUri: Uri, date: Long) {
        val database = AppDatabase.getDatabase(context)
        val dao = database.locationDao()
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
                        id = id,
                        uri = contentUri.toString(),
                        latitude = lat!!,
                        longitude = lon!!,
                        timestamp = date,
                        type = MediaType.VIDEO
                    )
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w("MediaScanner", "Failed to process video $id", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
    }

    private fun parseLocation(location: String): Pair<Double, Double>? {
        return try {
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

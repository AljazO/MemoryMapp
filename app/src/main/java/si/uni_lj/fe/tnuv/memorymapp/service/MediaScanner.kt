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

    suspend fun scanGallery(userId: String) = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            val dao = database.locationDao()
            
            // 1. Fetch ALL images and videos from MediaStore
            val foundImages = scanImages(userId)
            val foundVideos = scanVideos(userId)
            val allFoundIds = foundImages + foundVideos

            // 2. Cleanup: Remove media from DB that:
            // - No longer exists in MediaStore
            // - OR belongs to this user but shouldn't (no location points)
            val existingIdsInDb = dao.getAllMediaIds(userId).toSet()
            val idsToRemove = existingIdsInDb.filter { !allFoundIds.contains(it) }
            
            if (idsToRemove.isNotEmpty()) {
                Log.d("MediaScanner", "Cleaning up ${idsToRemove.size} media items for user $userId")
                idsToRemove.forEach { id ->
                    dao.deleteMediaById(id, userId)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("MediaScanner", "Error scanning gallery for user $userId", e)
        }
    }

    private fun isUriAccessible(uri: Uri): Boolean {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun scanImages(userId: String): Set<String> {
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
                if (trashedColIndex != -1 && it.getInt(trashedColIndex) != 0) continue

                val mediaId = it.getLong(idColumn)
                val stringId = "img_$mediaId"
                val contentUri = ContentUris.withAppendedId(uri, mediaId)

                if (!isUriAccessible(contentUri)) continue

                val path = it.getString(pathColIndex) ?: ""
                if (path.contains("Screenshots", ignoreCase = true) ||
                    path.contains("Download", ignoreCase = true)) {
                    continue
                }

                var date = it.getLong(dateTakenColumn)
                if (date == 0L) {
                    date = it.getLong(dateAddedColumn) * 1000
                }

                // VALIDATION: Only add to foundIds if user was active
                if (wasUserActive(userId, date)) {
                    foundIds.add(stringId)
                    processImageInsertion(userId, stringId, contentUri, date)
                }
            }
        }
        return foundIds
    }

    private suspend fun scanVideos(userId: String): Set<String> {
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

                var date = it.getLong(dateTakenColumn)
                if (date == 0L) {
                    date = it.getLong(dateAddedColumn) * 1000
                }

                // VALIDATION: Only add to foundIds if user was active
                if (wasUserActive(userId, date)) {
                    foundIds.add(stringId)
                    processVideoInsertion(userId, stringId, contentUri, date)
                }
            }
        }
        return foundIds
    }

    private suspend fun wasUserActive(userId: String, timestamp: Long): Boolean {
        val database = AppDatabase.getDatabase(context)
        val dao = database.locationDao()
        val range = 300000L // 5 minutes
        val points = dao.getPointsInRangeSync(userId, timestamp - range, timestamp + range)
        return points.isNotEmpty()
    }

    private suspend fun processImageInsertion(userId: String, id: String, contentUri: Uri, date: Long) {
        val database = AppDatabase.getDatabase(context)
        val dao = database.locationDao()
        
        // Skip if already in DB to avoid unnecessary processing
        val existingIds = dao.getAllMediaIds(userId)
        if (existingIds.contains(id)) return

        val photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try { MediaStore.setRequireOriginal(contentUri) } catch (e: Exception) { contentUri }
        } else { contentUri }

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
                val range = 300000L
                val closest = dao.getPointsInRangeSync(userId, date - range, date + range)
                    .minByOrNull { Math.abs(it.timestamp - date) }
                if (closest != null) {
                    lat = closest.latitude
                    lon = closest.longitude
                }
            }

            if (lat != null && lon != null) {
                dao.insertMedia(MediaPoint(id, userId, contentUri.toString(), lat!!, lon!!, date, MediaType.IMAGE))
            }
        } catch (e: Exception) {
            Log.w("MediaScanner", "Failed to process image $id", e)
        }
    }

    private suspend fun processVideoInsertion(userId: String, id: String, contentUri: Uri, date: Long) {
        val database = AppDatabase.getDatabase(context)
        val dao = database.locationDao()
        
        val existingIds = dao.getAllMediaIds(userId)
        if (existingIds.contains(id)) return

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
                val range = 300000L
                val closest = dao.getPointsInRangeSync(userId, date - range, date + range)
                    .minByOrNull { Math.abs(it.timestamp - date) }
                if (closest != null) {
                    lat = closest.latitude
                    lon = closest.longitude
                }
            }

            if (lat != null && lon != null) {
                dao.insertMedia(MediaPoint(id, userId, contentUri.toString(), lat!!, lon!!, date, MediaType.VIDEO))
            }
        } catch (e: Exception) {
            Log.w("MediaScanner", "Failed to process video $id", e)
        } finally {
            try { retriever.release() } catch (e: Exception) {}
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
        } catch (e: Exception) { null }
    }
}

package si.uni_lj.fe.tnuv.memorymapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.res.ResourcesCompat

object MapUtils {
    fun createMediaBitmap(context: Context, isVideo: Boolean): Bitmap {
        val size = 100
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.color = android.graphics.Color.BLACK
        canvas.drawCircle(size / 2f, size / 2f, size / 2.3f, paint)

        val iconRes = if (isVideo) android.R.drawable.presence_video_online else android.R.drawable.ic_menu_camera
        val drawable = ResourcesCompat.getDrawable(context.resources, iconRes, null)
        drawable?.let {
            it.setBounds(size / 4, size / 4, (size * 3) / 4, (size * 3) / 4)
            it.setTint(android.graphics.Color.WHITE)
            it.draw(canvas)
        }

        return bitmap
    }

    fun createHistoryDotBitmap(context: Context): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (16 * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
        }
        canvas.drawCircle(center, center, size / 2.1f, paint)

        paint.color = android.graphics.Color.RED
        canvas.drawCircle(center, center, size / 2.8f, paint)

        return bitmap
    }
}

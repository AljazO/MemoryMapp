package si.uni_lj.fe.tnuv.memorymapp

import android.app.Application
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

class MemoryMappApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // Early initialization can help reduce Flogger configuration warnings
        // though Flogger is usually internal to Google libraries.
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}

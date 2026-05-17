package si.uni_lj.fe.tnuv.memorymapp

import android.app.Application
import android.util.Log

class MemoryMappApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Early initialization can help reduce Flogger configuration warnings
        // though Flogger is usually internal to Google libraries.
    }
}

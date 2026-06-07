package si.uni_lj.fe.tnuv.memorymapp.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import si.uni_lj.fe.tnuv.memorymapp.MainActivity
import si.uni_lj.fe.tnuv.memorymapp.data.AppDatabase
import si.uni_lj.fe.tnuv.memorymapp.data.LocationPoint
import si.uni_lj.fe.tnuv.memorymapp.utils.LocationTracker
import si.uni_lj.fe.tnuv.memorymapp.utils.TrackSmoother

class LocationService : Service(), SensorEventListener {

    private lateinit var tracker: LocationTracker
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastSavedLocation: Location? = null
    private var serviceStartTime: Long = 0
    private var currentUserId: String? = null
    
    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var currentTotalSteps: Int = 0
    private var lastSavedStepCount: Int = 0

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        serviceStartTime = System.currentTimeMillis()

        // Initialize the new tracker with a callback to save significant locations
        tracker = LocationTracker(this) { location ->
            saveLocation(location)
        }
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        val hasActivityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasActivityRecognition) {
            stepCounterSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
        
        startStationaryStepTracking()
    }

    private fun startStationaryStepTracking() {
        serviceScope.launch {
            while (isRunning) {
                delay(60000)
                // If we take steps while stationary, save a point to record the step count update
                if (currentTotalSteps > lastSavedStepCount) {
                    lastSavedLocation?.let { saveLocation(it) }
                }
                
                // Periodic background smoothing (every 5 minutes)
                if (System.currentTimeMillis() % 300000 < 60000) {
                    val db = AppDatabase.getDatabase(applicationContext)
                    TrackSmoother.smoothTrack(db.locationDao(), currentUserId, serviceStartTime, System.currentTimeMillis())
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            currentTotalSteps = event.values[0].toInt()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveLocation(location: Location) {
        lastSavedLocation = location
        lastSavedStepCount = currentTotalSteps
        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.locationDao().insert(
                LocationPoint(
                    userId = currentUserId, // Save point with current userId
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis(),
                    altitude = if (location.hasAltitude()) location.altitude else 0.0,
                    totalStepsAtTimestamp = currentTotalSteps
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentUserId = intent?.getStringExtra("USER_ID")
        
        createNotificationChannel()
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Memory Mapp")
            .setContentText("Recording your journey smoothly...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        tracker.start()
        
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        tracker.stop()
        sensorManager?.unregisterListener(this)
        
        // Final smoothing on stop, using GlobalScope to ensure it finishes after service is destroyed
        val finalStartTime = serviceStartTime
        val finalEndTime = System.currentTimeMillis()
        val context = applicationContext
        val userId = currentUserId
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            TrackSmoother.smoothTrack(db.locationDao(), userId, finalStartTime, finalEndTime)
            serviceScope.cancel()
        }
        
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1
        var isRunning = false
            private set
    }
}

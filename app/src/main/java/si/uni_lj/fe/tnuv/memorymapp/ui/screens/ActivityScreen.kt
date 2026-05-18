package si.uni_lj.fe.tnuv.memorymapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.collectAsState
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.DarkBg
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientEnd
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientStart
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import android.os.Build
import si.uni_lj.fe.tnuv.memorymapp.data.AppDatabase
import si.uni_lj.fe.tnuv.memorymapp.data.LocationPoint
import si.uni_lj.fe.tnuv.memorymapp.data.MediaPoint
import si.uni_lj.fe.tnuv.memorymapp.data.MediaType
import si.uni_lj.fe.tnuv.memorymapp.service.MediaScanner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.res.ResourcesCompat
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import android.net.Uri
import android.content.Context
import androidx.compose.ui.graphics.asImageBitmap
import si.uni_lj.fe.tnuv.memorymapp.ui.components.CalendarWindow
import si.uni_lj.fe.tnuv.memorymapp.ui.components.SelectionInfoBar
import si.uni_lj.fe.tnuv.memorymapp.ui.components.StatisticsWindow
import si.uni_lj.fe.tnuv.memorymapp.utils.MapUtils
import si.uni_lj.fe.tnuv.memorymapp.utils.StatisticsCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onMenuClick: () -> Unit,
    isTracking: Boolean,
    onToggleTracking: (Boolean) -> Unit,
    startDate: Calendar,
    endDate: Calendar,
    onPeriodChange: (Calendar, Calendar) -> Unit,
    onAddTrip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val isSingleDay = remember(startDate, endDate) {
        startDate.get(Calendar.YEAR) == endDate.get(Calendar.YEAR) &&
                startDate.get(Calendar.DAY_OF_YEAR) == endDate.get(Calendar.DAY_OF_YEAR)
    }

    val isToday = remember(startDate, endDate) {
        val today = Calendar.getInstance()
        isSingleDay &&
                startDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                startDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    // Database and DAO
    val database = remember { AppDatabase.getDatabase(context) }
    val locationDao = database.locationDao()

    val startTimeMillis = remember(startDate) {
        val cal = startDate.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val endTimeMillis = remember(endDate) {
        val cal = endDate.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        cal.timeInMillis
    }

    val allPointsForPeriod by locationDao.getPointsInRange(startTimeMillis, endTimeMillis)
        .collectAsState(initial = emptyList())

    val mediaPointsForPeriod by locationDao.getMediaInRange(startTimeMillis, endTimeMillis)
        .collectAsState(initial = emptyList())

    // Media Scanning logic
    val mediaScanner = remember { MediaScanner(context) }
    
    var hasMediaPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasActivityPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val combinedPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val mediaGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true &&
            permissions[Manifest.permission.READ_MEDIA_VIDEO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        val activityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.ACTIVITY_RECOGNITION] == true
        } else {
            true
        }
        
        hasMediaPermission = mediaGranted
        hasLocationPermission = locationGranted
        hasActivityPermission = activityGranted
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf<String>()
        
        // Media permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.READ_MEDIA_IMAGES)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.ACCESS_MEDIA_LOCATION)
        }

        // Location permissions
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Activity recognition permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        if (perms.isNotEmpty()) {
            combinedPermissionLauncher.launch(perms.toTypedArray())
        }
    }

    LaunchedEffect(hasMediaPermission) {
        if (hasMediaPermission) {
            mediaScanner.scanGallery()
        }
    }

    // Slider state
    val daysCountTotal = remember(startDate, endDate) {
        val s = startDate.clone() as Calendar
        s.set(Calendar.HOUR_OF_DAY, 0)
        s.set(Calendar.MINUTE, 0)
        s.set(Calendar.SECOND, 0)
        s.set(Calendar.MILLISECOND, 0)
        
        val e = endDate.clone() as Calendar
        e.set(Calendar.HOUR_OF_DAY, 0)
        e.set(Calendar.MINUTE, 0)
        e.set(Calendar.SECOND, 0)
        e.set(Calendar.MILLISECOND, 0)
        
        val diff = e.timeInMillis - s.timeInMillis
        (diff / (24 * 60 * 60 * 1000)).toInt() + 1
    }
    val totalMinutesTotal = daysCountTotal * 1440f

    val calendarNow = Calendar.getInstance()
    var currentMinutesOfToday by remember { mutableFloatStateOf((calendarNow.get(Calendar.HOUR_OF_DAY) * 60 + calendarNow.get(Calendar.MINUTE)).toFloat()) }
    
    val initialMinutes = if (isToday) {
        ((daysCountTotal - 1) * 1440f) + currentMinutesOfToday
    } else {
        totalMinutesTotal - 1f // 23:59 of the last day
    }
    
    var sliderValue by remember { mutableFloatStateOf(initialMinutes) }

    // Reset slider when date changes
    LaunchedEffect(startDate, endDate) {
        if (isToday) {
            sliderValue = ((daysCountTotal - 1) * 1440f) + currentMinutesOfToday
        } else {
            sliderValue = totalMinutesTotal - 1f
        }
    }

    // Filter points based on slider
    val pastPathPoints = remember(allPointsForPeriod, sliderValue) {
        val maxTimestamp = startTimeMillis + (sliderValue * 60 * 1000).toLong()
        allPointsForPeriod.filter { it.timestamp <= maxTimestamp }.map { LatLng(it.latitude, it.longitude) }
    }

    val filteredMediaPoints = remember(mediaPointsForPeriod, sliderValue) {
        val maxTimestamp = startTimeMillis + (sliderValue * 60 * 1000).toLong()
        mediaPointsForPeriod.filter { it.timestamp <= maxTimestamp }
    }

    val futurePathPoints = remember(allPointsForPeriod, sliderValue) {
        val maxTimestamp = startTimeMillis + (sliderValue * 60 * 1000).toLong()
        allPointsForPeriod.filter { it.timestamp > maxTimestamp }.map { LatLng(it.latitude, it.longitude) }
    }

    val historyIndicatorPosition = remember(pastPathPoints) {
        pastPathPoints.lastOrNull()
    }

    val isLiveTime = remember(sliderValue, currentMinutesOfToday, isToday) {
        isToday && kotlin.math.abs(sliderValue - currentMinutesOfToday) < 1.0f
    }

    // Auto-advance slider if at live time
    LaunchedEffect(currentMinutesOfToday) {
        if (isLiveTime) {
            sliderValue = currentMinutesOfToday
        }
    }

    // Handle resume safely using LifecycleResumeEffect with a key
    LifecycleResumeEffect(Unit) {
        val c = Calendar.getInstance()
        currentMinutesOfToday = (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)).toFloat()
        if (isTracking && isToday) {
            sliderValue = currentMinutesOfToday
        }
        
        if (hasMediaPermission) {
            scope.launch {
                mediaScanner.scanGallery()
            }
        }
        
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        while(true) {
            val c = Calendar.getInstance()
            currentMinutesOfToday = (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)).toFloat()
            kotlinx.coroutines.delay(60000)
        }
    }

    var showCalendar by remember { mutableStateOf(false) }
    var showStatistics by remember { mutableStateOf(false) }
    var showTimeEntry by remember { mutableStateOf(false) }
    var selectedMediaPoint by remember { mutableStateOf<MediaPoint?>(null) }
    
    var hourInput by remember { mutableStateOf("") }
    var minuteInput by remember { mutableStateOf("") }

    var isUserInteracting by remember { mutableStateOf(false) }
    var hasCentredOnce by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(46.0569, 14.5058), 12f)
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            isUserInteracting = true
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Initial center on current location and auto-start tracking
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && !hasCentredOnce) {
            // Auto-start tracking if not already tracking
            if (!isTracking) {
                onToggleTracking(true)
            }
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val startLatLng = LatLng(it.latitude, it.longitude)
                    scope.launch {
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(startLatLng, 15f))
                        hasCentredOnce = true
                    }
                }
            }
        } else if (!hasLocationPermission) {
            combinedPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Marker state for the history red dot
    val historyMarkerState = rememberMarkerState()

    // Unified logic to follow history dot and update marker
    LaunchedEffect(historyIndicatorPosition, isUserInteracting, isLiveTime, isTracking, isToday) {
        historyIndicatorPosition?.let { pos ->
            historyMarkerState.position = pos
            
            if (!isUserInteracting) {
                if (isToday && isLiveTime && isTracking) {
                    // Smoothly follow active tracking at live time
                    cameraPositionState.animate(CameraUpdateFactory.newLatLng(pos))
                } else {
                    // Instantly snap map to location when scrubbing through history
                    cameraPositionState.move(CameraUpdateFactory.newLatLng(pos))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            ActivityTopBar(
                startDate = startDate,
                endDate = endDate,
                onMenuClick = onMenuClick,
                onCalendarClick = { showCalendar = true },
                onPrevDay = {
                    val newStart = startDate.clone() as Calendar
                    newStart.add(Calendar.DAY_OF_YEAR, -1)
                    val newEnd = endDate.clone() as Calendar
                    newEnd.add(Calendar.DAY_OF_YEAR, -1)
                    onPeriodChange(newStart, newEnd)
                },
                onNextDay = {
                    if (!isToday) {
                        val newStart = startDate.clone() as Calendar
                        newStart.add(Calendar.DAY_OF_YEAR, 1)
                        val newEnd = endDate.clone() as Calendar
                        newEnd.add(Calendar.DAY_OF_YEAR, 1)
                        onPeriodChange(newStart, newEnd)
                    }
                }
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
        ) {
            // Map Container
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.LightGray)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMyLocationButtonClick = {
                            isUserInteracting = false
                            scope.launch {
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    location?.let {
                                        scope.launch {
                                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f))
                                        }
                                    }
                                }
                            }
                            true
                        },
                        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                        properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
                    ) {
                        // Future/Ghost Path (Lighter)
                        if (futurePathPoints.isNotEmpty()) {
                            Polyline(
                                points = futurePathPoints,
                                color = Color(0xFF4A90E2).copy(alpha = 0.3f),
                                width = 10f
                            )
                        }

                        // Past Path (Solid)
                        if (pastPathPoints.isNotEmpty()) {
                            Polyline(
                                points = pastPathPoints,
                                color = Color(0xFF4A90E2),
                                width = 10f
                            )
                        }

                        // History Indicator (Red Dot)
                        if (historyIndicatorPosition != null) {
                            val historyIcon = remember {
                                BitmapDescriptorFactory.fromBitmap(MapUtils.createHistoryDotBitmap(context))
                            }
                            Marker(
                                state = historyMarkerState,
                                icon = historyIcon,
                                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                                flat = true
                            )
                        }

                        // Media Pins
                        filteredMediaPoints.forEach { media ->
                            key(media.id) {
                                val markerIcon = remember(media.type) {
                                    BitmapDescriptorFactory.fromBitmap(MapUtils.createMediaBitmap(context, media.type == MediaType.VIDEO))
                                }
                                Marker(
                                    state = rememberMarkerState(position = LatLng(media.latitude, media.longitude)),
                                    icon = markerIcon,
                                    onClick = {
                                        selectedMediaPoint = media
                                        true
                                    }
                                )
                            }
                        }
                    }

            // Selection Info Bar - Shown when not "today"
            if (!isToday) {
                SelectionInfoBar(
                    onClear = {
                        val now = Calendar.getInstance()
                        val start = now.clone() as Calendar
                        start.set(Calendar.HOUR_OF_DAY, 0)
                        start.set(Calendar.MINUTE, 0)
                        start.set(Calendar.SECOND, 0)
                        start.set(Calendar.MILLISECOND, 0)
                        
                        val end = now.clone() as Calendar
                        end.set(Calendar.HOUR_OF_DAY, 23)
                        end.set(Calendar.MINUTE, 59)
                        end.set(Calendar.SECOND, 59)
                        end.set(Calendar.MILLISECOND, 999)
                        
                        onPeriodChange(start, end)
                    },
                    onAddTrip = onAddTrip,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }

            // Custom Zoom and Recenter Controls
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp, top = 60.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Recenter Button
                        ZoomButton(
                            icon = if (!isUserInteracting && isTracking && isToday) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                            iconTint = if (!isUserInteracting && isTracking && isToday) Color(0xFF6E6EF7) else Color.Black
                        ) {
                            isUserInteracting = false
                            scope.launch {
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    location?.let {
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(it.latitude, it.longitude),
                                                    15f
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        ZoomButton(icon = Icons.Default.Add) {
                            scope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                            }
                        }
                        ZoomButton(icon = Icons.Default.Remove) {
                            scope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                            }
                        }
                    }
                }
            }

            // Floating "Advanced statistics" button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp) // Lowered more
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable { showStatistics = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Advanced statistics",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Time slider bar
            if (true) { // Show slider always now, or keep some logic
                val daysCount = remember(startDate, endDate) {
                    val s = startDate.clone() as Calendar
                    s.set(Calendar.HOUR_OF_DAY, 0)
                    s.set(Calendar.MINUTE, 0)
                    s.set(Calendar.SECOND, 0)
                    s.set(Calendar.MILLISECOND, 0)
                    
                    val e = endDate.clone() as Calendar
                    e.set(Calendar.HOUR_OF_DAY, 0)
                    e.set(Calendar.MINUTE, 0)
                    e.set(Calendar.SECOND, 0)
                    e.set(Calendar.MILLISECOND, 0)
                    
                    val diff = e.timeInMillis - s.timeInMillis
                    (diff / (24 * 60 * 60 * 1000)).toInt() + 1
                }
                
                val sliderPadding = 60.dp
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = sliderPadding)
                        .fillMaxWidth()
                        .height(86.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val currentTotalMinutes = sliderValue.toInt()
                        val dayIndex = (currentTotalMinutes / 1440).coerceIn(0, daysCountTotal - 1)
                        val minutesInDay = (currentTotalMinutes % 1440).coerceIn(0, 1439)
                        val hours = minutesInDay / 60
                        val minutes = minutesInDay % 60
                        
                        val displayCalendar = (startDate.clone() as Calendar).apply {
                            add(Calendar.DAY_OF_YEAR, dayIndex)
                        }
                        val dateString = SimpleDateFormat("MMM d", Locale.ENGLISH).format(displayCalendar.time)
                        val timeString = if (daysCount > 1) {
                            "%s, %02d:%02d".format(dateString, hours, minutes)
                        } else {
                            "%02d:%02d".format(hours, minutes)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                timeString,
                                color = Color(0xFF6E6EF7),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    hourInput = "%02d".format(hours)
                                    minuteInput = "%02d".format(minutes)
                                    showTimeEntry = true
                                }
                            )
                            if (isToday) {
                                IconButton(
                                    onClick = { 
                                        sliderValue = currentMinutesOfToday
                                        isUserInteracting = false
                                    },
                                    modifier = Modifier.size(24.dp).padding(start = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = "Reset to current time",
                                        tint = Color(0xFF6E6EF7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Direct Time Entry Dialog
                        if (showTimeEntry) {
                            val focusRequesterHours = remember { FocusRequester() }
                            val focusRequesterMinutes = remember { FocusRequester() }

                            Dialog(
                                onDismissRequest = { showTimeEntry = false },
                                properties = DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().clickable { showTimeEntry = false }.padding(bottom = 260.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Card(
                                        modifier = Modifier.width(180.dp).clickable(enabled = false) { },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                                        elevation = CardDefaults.cardElevation(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("Set Time", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(16.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                // HOURS
                                                OutlinedTextField(
                                                    value = hourInput,
                                                    onValueChange = {
                                                        val digits = it.filter { c -> c.isDigit() }.take(2)
                                                        val value = digits.toIntOrNull()

                                                        if (value == null || value <= 23) {
                                                            hourInput = digits
                                                        }

                                                        if (digits.length == 2) {
                                                            focusRequesterMinutes.requestFocus()
                                                        }
                                                    },
                                                    placeholder = { Text("_ _", color = Color.Gray) },
                                                    textStyle = TextStyle(
                                                        color = Color.White,
                                                        textAlign = TextAlign.Center,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color(0xFF6E6EF7),
                                                        unfocusedBorderColor = Color.Gray,
                                                        cursorColor = Color(0xFF6E6EF7)
                                                    ),
                                                    singleLine = true,
                                                    modifier = Modifier
                                                        .width(65.dp)
                                                        .focusRequester(focusRequesterHours),
                                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                                    )
                                                )

                                                Text(
                                                    ":",
                                                    color = Color.White,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )

                                                // MINUTES
                                                OutlinedTextField(
                                                    value = minuteInput,
                                                    onValueChange = {
                                                        val digits = it.filter { c -> c.isDigit() }.take(2)
                                                        val value = digits.toIntOrNull()

                                                        if (value == null || value <= 59) {
                                                            minuteInput = digits
                                                        }
                                                    },
                                                    placeholder = { Text("_ _", color = Color.Gray) },
                                                    textStyle = TextStyle(
                                                        color = Color.White,
                                                        textAlign = TextAlign.Center,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color(0xFF6E6EF7),
                                                        unfocusedBorderColor = Color.Gray,
                                                        cursorColor = Color(0xFF6E6EF7)
                                                    ),
                                                    singleLine = true,
                                                    modifier = Modifier
                                                        .width(65.dp)
                                                        .focusRequester(focusRequesterMinutes)
                                                        .onKeyEvent { event ->
                                                            if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace && minuteInput.isEmpty()) {
                                                                focusRequesterHours.requestFocus()
                                                                if (hourInput.isNotEmpty()) {
                                                                    hourInput = hourInput.dropLast(1)
                                                                }
                                                                true
                                                            } else {
                                                                false
                                                            }
                                                        },
                                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                                    )
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Button(
                                                onClick = {
                                                    val h = hourInput.toIntOrNull() ?: 0
                                                    val m = minuteInput.toIntOrNull() ?: 0

                                                    if (h < 24 && m < 60) {
                                                        val requestedMinutes = (h * 60 + m).toFloat()
                                                        sliderValue = if (isToday) {
                                                            requestedMinutes.coerceIn(0f, currentMinutesOfToday)
                                                        } else {
                                                            requestedMinutes.coerceIn(0f, 1440f)
                                                        }
                                                        isUserInteracting = false
                                                    }
                                                    showTimeEntry = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd))),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("Confirm", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                            Slider(
                                value = sliderValue,
                                onValueChange = { newValue ->
                                    val maxAllowed = if (isToday) {
                                        ((daysCountTotal - 1) * 1440f) + currentMinutesOfToday
                                    } else {
                                        totalMinutesTotal - 1f
                                    }
                                    sliderValue = newValue.coerceIn(0f, maxAllowed)
                                    isUserInteracting = false // Force map to follow history dot during scrolling
                                },
                                valueRange = 0f..(totalMinutesTotal - 1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF6E6EF7),
                                    activeTrackColor = Color(0xFF6E6EF7),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // White dots for day separators
                            if (daysCount > 1) {
                                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    for (i in 1 until daysCount) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Box(modifier = Modifier.size(4.dp).background(Color.White, CircleShape))
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("00:00", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            Text("23:59", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    }
                }
            }

            // Media Preview Popup
            if (selectedMediaPoint != null) {
                Dialog(
                    onDismissRequest = { selectedMediaPoint = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.8f))
                            .clickable { selectedMediaPoint = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable(enabled = false) { },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = rememberAsyncImagePainter(selectedMediaPoint!!.uri),
                                    contentDescription = "Media Preview",
                                    modifier = Modifier.fillMaxSize()
                                )
                                
                                IconButton(
                                    onClick = { selectedMediaPoint = null },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Statistics Overlay
            if (showStatistics) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { showStatistics = false }
                ) {
                    val statsEndTime = if (isToday) System.currentTimeMillis() else endTimeMillis
                    StatisticsWindow(
                        points = allPointsForPeriod,
                        startTime = startTimeMillis,
                        endTime = statsEndTime,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 0.dp, start = 16.dp, end = 16.dp)
                            .clickable(enabled = false) { }, // Prevent clicks inside from closing
                        onClose = { showStatistics = false }
                    )
                }
            }

            // Calendar Dialog
            if (showCalendar) {
                Dialog(
                    onDismissRequest = { showCalendar = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showCalendar = false },
                        contentAlignment = Alignment.Center
                    ) {
                        CalendarWindow(
                            initialStartDate = startDate,
                            initialEndDate = endDate,
                            modifier = Modifier
                                .padding(24.dp)
                                .clickable(enabled = false) { },
                            onClose = { showCalendar = false },
                            onPeriodSelected = { start, end ->
                                onPeriodChange(start, end)
                                showCalendar = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomButton(icon: ImageVector, iconTint: Color = Color.Black, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.9f),
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ActivityTopBar(
    startDate: Calendar,
    endDate: Calendar,
    onMenuClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit
) {
    val isSingleDay = remember(startDate, endDate) {
        startDate.get(Calendar.YEAR) == endDate.get(Calendar.YEAR) &&
                startDate.get(Calendar.DAY_OF_YEAR) == endDate.get(Calendar.DAY_OF_YEAR)
    }

    val displayString = remember(startDate, endDate, isSingleDay) {
        if (isSingleDay) {
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val target = startDate.clone() as Calendar
            target.set(Calendar.HOUR_OF_DAY, 0)
            target.set(Calendar.MINUTE, 0)
            target.set(Calendar.SECOND, 0)
            target.set(Calendar.MILLISECOND, 0)

            val diffMillis = today.timeInMillis - target.timeInMillis
            val diffDays = (diffMillis / (24 * 60 * 60 * 1000)).toInt()

            when (diffDays) {
                0 -> "Today"
                1 -> "Yesterday"
                else -> SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).format(startDate.time)
            }
        } else {
            val sdf = SimpleDateFormat("MMM d", Locale.ENGLISH)
            "${sdf.format(startDate.time)} - ${sdf.format(endDate.time)}"
        }
    }

    val dateDetails = remember(startDate, endDate, isSingleDay) {
        if (isSingleDay) {
            SimpleDateFormat("MMMM d, yyyy | EEEE", Locale.ENGLISH).format(startDate.time)
        } else {
            SimpleDateFormat("yyyy", Locale.ENGLISH).format(startDate.time)
        }
    }

    val isToday = remember(startDate, endDate) {
        val today = Calendar.getInstance()
        startDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                startDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(32.dp)
            ) {
                IconButton(onClick = onPrevDay) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous", tint = Color.White)
                }
                Text(
                    text = displayString,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = onNextDay,
                    enabled = !isToday
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = if (isToday) Color.White.copy(alpha = 0.3f) else Color.White
                    )
                }
            }
            Text(
                text = dateDetails,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.offset(y = (-4).dp)
            )
        }

        IconButton(onClick = onCalendarClick) {
            Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription = "Calendar", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun SidebarContent(
    isTracking: Boolean,
    onToggleTracking: () -> Unit,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp)) // Content moved further down
        Box(modifier = Modifier.height(48.dp), contentAlignment = Alignment.CenterStart) {
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.offset(x = (-12).dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Close Menu",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "MEMORY",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 44.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "MAPP",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 44.sp
            )
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp).padding(start = 4.dp)
            )
        }

        Text(
            text = "Map your moves.\nRemember your moments.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        val trackingButtonModifier = if (isTracking) {
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                    shape = RoundedCornerShape(28.dp)
                )
        } else {
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                    shape = RoundedCornerShape(28.dp)
                )
        }

        Box(
            modifier = trackingButtonModifier.clickable { onToggleTracking() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isTracking) "Stop tracking" else "Start tracking",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        SidebarItem("Settings", Icons.Outlined.Settings)
        SidebarItem("Permissions", Icons.Outlined.Security)
        SidebarItem("About", Icons.Outlined.Info)

        Spacer(modifier = Modifier.weight(1f))

        Row(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(text = "Having issues? ", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Text(
                text = "Force quit app",
                color = Color(0xFF6E6EF7),
                fontSize = 12.sp,
                modifier = Modifier.clickable { /* Force quit */ }
            )
        }
    }
}

@Composable
fun SidebarItem(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clickable { /* Navigate */ },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

package si.uni_lj.fe.tnuv.memorymapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
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
import si.uni_lj.fe.tnuv.memorymapp.data.AppDatabase
import si.uni_lj.fe.tnuv.memorymapp.data.LocationPoint
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(onMenuClick: () -> Unit, isTracking: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Database and DAO
    val database = remember { AppDatabase.getDatabase(context) }
    val locationDao = database.locationDao()
    
    // Path points from database
    val calendarToday = Calendar.getInstance()
    calendarToday.set(Calendar.HOUR_OF_DAY, 0)
    calendarToday.set(Calendar.MINUTE, 0)
    calendarToday.set(Calendar.SECOND, 0)
    calendarToday.set(Calendar.MILLISECOND, 0)
    val startOfDay = calendarToday.timeInMillis
    
    val allTodayPoints by locationDao.getPointsInRange(startOfDay, startOfDay + 86400000)
        .collectAsState(initial = emptyList())
    
    // Slider state
    val calendarNow = Calendar.getInstance()
    val initialMinutes = (calendarNow.get(Calendar.HOUR_OF_DAY) * 60 + calendarNow.get(Calendar.MINUTE)).toFloat()
    var currentMinutesOfDay by remember { mutableFloatStateOf(initialMinutes) }
    var sliderValue by remember { mutableFloatStateOf(initialMinutes) }
    
    // Filter points based on slider
    val filteredPathPoints = remember(allTodayPoints, sliderValue) {
        val maxTimestamp = startOfDay + (sliderValue * 60 * 1000).toLong()
        allTodayPoints.filter { it.timestamp <= maxTimestamp }.map { LatLng(it.latitude, it.longitude) }
    }
    
    LaunchedEffect(Unit) {
        while(true) {
            val c = Calendar.getInstance()
            currentMinutesOfDay = (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)).toFloat()
            kotlinx.coroutines.delay(60000)
        }
    }

    var showCalendar by remember { mutableStateOf(false) }
    var showStatistics by remember { mutableStateOf(false) }
    
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

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Initial center on current location
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && !hasCentredOnce) {
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
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Auto-follow current location if tracking and not interacting
    LaunchedEffect(allTodayPoints, isUserInteracting, isTracking) {
        if (isTracking && !isUserInteracting && allTodayPoints.isNotEmpty()) {
            val lastPoint = allTodayPoints.last()
            val lastLatLng = LatLng(lastPoint.latitude, lastPoint.longitude)
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(lastLatLng))
        }
    }

    Scaffold(
        topBar = {
            ActivityTopBar(
                onMenuClick = onMenuClick,
                onCalendarClick = { showCalendar = true }
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
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
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
                        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true),
                        properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
                    ) {
                        if (filteredPathPoints.isNotEmpty()) {
                            Polyline(
                                points = filteredPathPoints,
                                color = Color(0xFF4A90E2),
                                width = 10f
                            )
                        }
                    }

                    // Custom Zoom Controls
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp, top = 60.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                    .padding(bottom = 100.dp)
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
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(84.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    val hours = (sliderValue / 60).toInt()
                    val minutes = (sliderValue % 60).toInt()
                    val timeString = String.format(LocalLocale.current.platformLocale, "%02d:%02d", hours, minutes)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("00:00", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(timeString, color = Color(0xFF6E6EF7), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { sliderValue = initialMinutes },
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
                        
                        Text("24:00", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                    
                    Slider(
                        value = sliderValue,
                        onValueChange = { newValue ->
                            if (newValue <= currentMinutesOfDay) {
                                sliderValue = newValue
                            }
                        },
                        valueRange = 0f..1440f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF6E6EF7),
                            activeTrackColor = Color(0xFF6E6EF7),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        )
                    )
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
                    StatisticsWindow(
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
                            modifier = Modifier
                                .padding(24.dp)
                                .clickable(enabled = false) { },
                            onClose = { showCalendar = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarWindow(modifier: Modifier = Modifier, onClose: () -> Unit) {
    val calendar = remember { Calendar.getInstance() }
    val monthName = remember { calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) }
    val year = remember { calendar.get(Calendar.YEAR) }
    val today = remember { calendar.get(Calendar.DAY_OF_MONTH) }
    
    val daysInMonth = remember { calendar.getActualMaximum(Calendar.DAY_OF_MONTH) }
    
    val firstDayOfWeekOffset = remember {
        val c = calendar.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        val dayOfWeek = c.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
    }
    
    val prevMonthDays = remember {
        val c = calendar.clone() as Calendar
        c.add(Calendar.MONTH, -1)
        c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    
    val dates = remember {
        val list = mutableListOf<Pair<Int, Boolean>>()
        for (i in (prevMonthDays - firstDayOfWeekOffset + 1)..prevMonthDays) {
            list.add(i to false)
        }
        for (i in 1..daysInMonth) {
            list.add(i to true)
        }
        val remaining = 42 - list.size
        for (i in 1..remaining) {
            list.add(i to false)
        }
        list
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "$monthName, $year",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                days.forEach { day ->
                    Text(
                        text = day,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(240.dp),
                userScrollEnabled = false
            ) {
                items(dates) { (day, isCurrentMonth) ->
                    val isToday = isCurrentMonth && day == today
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .aspectRatio(1f)
                            .background(
                                color = if (isToday) Color(0xFF6E6EF7) else Color(0xFF2C2C2E),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            color = if (isCurrentMonth) Color.White else Color.Gray.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Insert date", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    CalendarInputField(placeholder = "dd.mm.yyyy")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Insert period", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CalendarInputField(placeholder = "dd.mm.yyyy", modifier = Modifier.weight(1f))
                        Text(" Start", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CalendarInputField(placeholder = "dd.mm.yyyy", modifier = Modifier.weight(1f))
                        Text(" End", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp).width(30.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarInputField(placeholder: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = placeholder, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun StatisticsWindow(modifier: Modifier = Modifier, onClose: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1115))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(label = "Distance", value = "0,00 km", icon = null)
                StatItem(label = "Duration", value = "00:00:00", icon = null)
                StatItem(label = "Calories", value = "0 kcal", icon = null)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(label = "Steps", value = "0", icon = Icons.AutoMirrored.Filled.DirectionsRun)
                StatItem(label = "Avg Pace", value = "0.00 km/h", icon = Icons.Default.Timer, iconTint = Color(0xFF007AFF))
                StatItem(label = "Elevation", value = "0 m", icon = Icons.AutoMirrored.Filled.TrendingUp, iconTint = Color(0xFF81D4FA))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: ImageVector?, iconTint: Color = Color(0xFF6E6EF7)) {
    Column(
        modifier = Modifier.width(90.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(text = label, color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
        if (icon != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun ZoomButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.9f),
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ActivityTopBar(onMenuClick: () -> Unit, onCalendarClick: () -> Unit) {
    val currentDate = remember {
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(32.dp))
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White)
                Text(text = "Today", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
            }
            Text(text = currentDate, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
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
        Spacer(modifier = Modifier.height(8.dp))
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

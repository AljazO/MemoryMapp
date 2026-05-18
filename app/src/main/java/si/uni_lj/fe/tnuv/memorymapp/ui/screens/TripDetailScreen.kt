package si.uni_lj.fe.tnuv.memorymapp.ui.screens

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import si.uni_lj.fe.tnuv.memorymapp.ui.components.MediaPreviewDialog
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientEnd
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientStart
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import si.uni_lj.fe.tnuv.memorymapp.data.AppDatabase
import si.uni_lj.fe.tnuv.memorymapp.data.MediaPoint
import si.uni_lj.fe.tnuv.memorymapp.data.MediaType
import si.uni_lj.fe.tnuv.memorymapp.ui.components.MapZoomButton
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.DarkBg
import si.uni_lj.fe.tnuv.memorymapp.utils.MapUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TripDetailScreen(
    tripId: Long,
    onBackClick: () -> Unit,
    onViewPicturesClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val locationDao = database.locationDao()

    val trips by locationDao.getAllTrips().collectAsState(initial = emptyList())
    val trip = trips.find { it.id == tripId }

    if (trip == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val pathPoints by locationDao.getPointsInRange(trip.startTime, trip.endTime).collectAsState(initial = emptyList())
    val mediaPoints by locationDao.getMediaInRange(trip.startTime, trip.endTime).collectAsState(initial = emptyList())

    // Slider State
    var sliderValue by remember { mutableFloatStateOf(1f) }
    var isUserInteracting by remember { mutableStateOf(false) }
    var selectedMediaPoint by remember { mutableStateOf<MediaPoint?>(null) }

    val totalDurationMillis = trip.endTime - trip.startTime
    val currentTimestamp = trip.startTime + (sliderValue * totalDurationMillis).toLong()

    val pastPath = remember(pathPoints, currentTimestamp) {
        pathPoints.filter { it.timestamp <= currentTimestamp }.map { LatLng(it.latitude, it.longitude) }
    }
    
    val futurePath = remember(pathPoints, currentTimestamp) {
        pathPoints.filter { it.timestamp > currentTimestamp }.map { LatLng(it.latitude, it.longitude) }
    }

    val filteredMedia = remember(mediaPoints, currentTimestamp) {
        mediaPoints.filter { it.timestamp <= currentTimestamp }
    }

    val historyIndicatorPosition = pastPath.lastOrNull()
    val historyMarkerState = rememberMarkerState()

    val cameraPositionState = rememberCameraPositionState()

    // Detect user interaction
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            isUserInteracting = true
        }
    }

    // Initial camera zoom to fit whole trip
    var hasInitializedCamera by remember { mutableStateOf(false) }
    LaunchedEffect(pathPoints, mediaPoints) {
        if (!hasInitializedCamera && (pathPoints.isNotEmpty() || mediaPoints.isNotEmpty())) {
            val boundsBuilder = LatLngBounds.Builder()
            var hasAnyPoint = false
            pathPoints.forEach { 
                boundsBuilder.include(LatLng(it.latitude, it.longitude))
                hasAnyPoint = true
            }
            mediaPoints.forEach { 
                boundsBuilder.include(LatLng(it.latitude, it.longitude))
                hasAnyPoint = true
            }
            
            if (hasAnyPoint) {
                try {
                    val bounds = boundsBuilder.build()
                    cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                    hasInitializedCamera = true
                } catch (e: Exception) {
                    if (pathPoints.isNotEmpty()) {
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(LatLng(pathPoints[0].latitude, pathPoints[0].longitude), 15f))
                        hasInitializedCamera = true
                    }
                }
            }
        }
    }

    // Follow history dot and update marker position
    LaunchedEffect(historyIndicatorPosition, isUserInteracting) {
        historyIndicatorPosition?.let { pos ->
            historyMarkerState.position = pos
            if (!isUserInteracting) {
                cameraPositionState.move(CameraUpdateFactory.newLatLng(pos))
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trip.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
                properties = MapProperties(isMyLocationEnabled = false)
            ) {
                // Future Path (Ghost)
                if (futurePath.isNotEmpty()) {
                    Polyline(
                        points = futurePath,
                        color = Color(0xFF4A90E2).copy(alpha = 0.3f),
                        width = 10f
                    )
                }

                // Past Path (Solid)
                if (pastPath.isNotEmpty()) {
                    Polyline(
                        points = pastPath,
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
                filteredMedia.forEach { media ->
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

            // Recenter/Zoom Controls
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapZoomButton(
                    icon = if (!isUserInteracting) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                    iconTint = if (!isUserInteracting) Color(0xFF6E6EF7) else Color.Black
                ) {
                    isUserInteracting = false
                    historyIndicatorPosition?.let { pos ->
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLng(pos))
                        }
                    }
                }
                MapZoomButton(icon = Icons.Default.Add) {
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                    }
                }
                MapZoomButton(icon = Icons.Default.Remove) {
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                    }
                }
            }

            // Time Slider Bar
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 100.dp)
                    .fillMaxWidth()
                    .height(96.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    val sdfTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                    val sdfDate = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
                    
                    val daysCount = remember(trip) {
                        val s = Calendar.getInstance().apply { timeInMillis = trip.startTime }
                        s.set(Calendar.HOUR_OF_DAY, 0)
                        s.set(Calendar.MINUTE, 0)
                        s.set(Calendar.SECOND, 0)
                        s.set(Calendar.MILLISECOND, 0)
                        
                        val e = Calendar.getInstance().apply { timeInMillis = trip.endTime }
                        e.set(Calendar.HOUR_OF_DAY, 0)
                        e.set(Calendar.MINUTE, 0)
                        e.set(Calendar.SECOND, 0)
                        e.set(Calendar.MILLISECOND, 0)
                        
                        val diff = e.timeInMillis - s.timeInMillis
                        (diff / (24 * 60 * 60 * 1000)).toInt() + 1
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            sdfTime.format(Date(currentTimestamp)),
                            color = Color(0xFF6E6EF7),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            sdfDate.format(Date(currentTimestamp)),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                        Slider(
                            value = sliderValue,
                            onValueChange = { 
                                sliderValue = it
                                isUserInteracting = false 
                            },
                            valueRange = 0f..1f,
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
                        Text(sdfDate.format(Date(trip.startTime)), color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        Text(sdfDate.format(Date(trip.endTime)), color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }
            }

            // View Media Button
            Button(
                onClick = { onViewPicturesClick(tripId) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Media", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Media Preview Popup
            selectedMediaPoint?.let { media ->
                MediaPreviewDialog(
                    mediaPoint = media,
                    onDismissRequest = { selectedMediaPoint = null }
                )
            }
        }
    }
}

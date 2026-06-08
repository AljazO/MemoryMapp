package si.uni_lj.fe.tnuv.memorymapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import si.uni_lj.fe.tnuv.memorymapp.data.AppDatabase
import si.uni_lj.fe.tnuv.memorymapp.data.Trip
import si.uni_lj.fe.tnuv.memorymapp.data.DataRepository
import si.uni_lj.fe.tnuv.memorymapp.ui.components.verticalScrollbar
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientEnd
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientStart
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import si.uni_lj.fe.tnuv.memorymapp.ui.components.CalendarWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    userId: String, // Added userId
    onMenuClick: () -> Unit,
    onTripClick: (Long) -> Unit,
    initialStartDate: Calendar? = null,
    initialEndDate: Calendar? = null,
    showAddInitially: Boolean = false
) {
    var showEditor by remember { mutableStateOf(showAddInitially) }
    var editingTrip by remember { mutableStateOf<Trip?>(null) }
    var showCalendarByFab by remember { mutableStateOf(false) }
    var pendingStartDate by remember { mutableStateOf<Calendar?>(initialStartDate) }
    var pendingEndDate by remember { mutableStateOf<Calendar?>(initialEndDate) }
    
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val locationDao = database.locationDao()
    val repository = remember { DataRepository(locationDao) }
    
    // Filter trips by userId
    val trips by locationDao.getAllTrips(userId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("My trips", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.size(32.dp))
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (trips.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No trips saved yet", color = Color.Gray)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(scrollState)
                        .verticalScroll(scrollState)
                ) {
                    trips.forEach { trip ->
                        TripItem(
                            trip = trip,
                            onClick = { onTripClick(trip.id) },
                            onEdit = {
                                editingTrip = trip
                                showEditor = true
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Add Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(56.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                        shape = CircleShape
                    )
                    .clickable { showCalendarByFab = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Trip", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            if (showCalendarByFab) {
                val today = Calendar.getInstance()
                Dialog(
                    onDismissRequest = { showCalendarByFab = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showCalendarByFab = false },
                        contentAlignment = Alignment.Center
                    ) {
                        CalendarWindow(
                            initialStartDate = today,
                            initialEndDate = today,
                            modifier = Modifier
                                .padding(24.dp)
                                .clickable(enabled = false) { },
                            onClose = { showCalendarByFab = false },
                            onPeriodSelected = { start, end ->
                                pendingStartDate = start
                                pendingEndDate = end
                                editingTrip = null
                                showCalendarByFab = false
                                showEditor = true
                            }
                        )
                    }
                }
            }

            if (showEditor) {
                TripEditorPanel(
                    trip = editingTrip,
                    onClose = { 
                        showEditor = false
                        editingTrip = null
                    },
                    onSave = { title, description, start, end ->
                        scope.launch {
                            val tripToSync = if (editingTrip != null) {
                                val updatedTrip = editingTrip!!.copy(
                                    title = title,
                                    description = description,
                                    startTime = start.timeInMillis,
                                    endTime = end.timeInMillis
                                )
                                locationDao.updateTrip(updatedTrip)
                                updatedTrip
                            } else {
                                val newTrip = Trip(
                                    userId = userId,
                                    title = title,
                                    description = description,
                                    startTime = start.timeInMillis,
                                    endTime = end.timeInMillis
                                )
                                val newId = locationDao.insertTrip(newTrip)
                                newTrip.copy(id = newId)
                            }
                            
                            // Synchronize to Firebase
                            repository.syncTripToFirebase(tripToSync)

                            showEditor = false
                            editingTrip = null
                        }
                    },
                    onDelete = {
                        editingTrip?.let {
                            scope.launch {
                                locationDao.deleteTrip(it)
                                showEditor = false
                                editingTrip = null
                            }
                        }
                    },
                    initialStartDate = if (editingTrip != null) {
                        Calendar.getInstance().apply { timeInMillis = editingTrip!!.startTime }
                    } else pendingStartDate,
                    initialEndDate = if (editingTrip != null) {
                        Calendar.getInstance().apply { timeInMillis = editingTrip!!.endTime }
                    } else pendingEndDate
                )
            }
        }
    }
}

@Composable
fun TripItem(trip: Trip, onClick: () -> Unit, onEdit: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val sdf = remember(locale) { SimpleDateFormat("dd.MM.yyyy", locale) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Map, contentDescription = null, tint = Color.Gray)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(trip.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${sdf.format(Date(trip.startTime))} - ${sdf.format(Date(trip.endTime))}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.MoreVert, contentDescription = "Edit Trip", tint = Color.White)
            }
        }
    }
}

@Composable
fun TripEditorPanel(
    trip: Trip? = null,
    onClose: () -> Unit,
    onSave: (String, String, Calendar, Calendar) -> Unit,
    onDelete: () -> Unit,
    initialStartDate: Calendar? = null,
    initialEndDate: Calendar? = null
) {
    val locale = LocalConfiguration.current.locales[0]
    val sdf = remember(locale) { SimpleDateFormat("dd.MM.yyyy", locale) }
    
    var title by remember { mutableStateOf(trip?.title ?: "") }
    var description by remember { mutableStateOf(trip?.description ?: "") }
    val startDate = remember { 
        mutableStateOf(initialStartDate ?: Calendar.getInstance().apply { 
            trip?.startTime?.let { timeInMillis = it }
        }) 
    }
    val endDate = remember { 
        mutableStateOf(initialEndDate ?: Calendar.getInstance().apply { 
            trip?.endTime?.let { timeInMillis = it }
        }) 
    }
    var showCalendarInPanel by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (trip == null) "Add your trip" else "Edit your trip", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Insert name", color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Trip name", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Insert description", color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Custom text", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Date", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                            .clickable { showCalendarInPanel = true }
                            .padding(12.dp)
                        ) {
                            Text(sdf.format(startDate.value.time), color = Color.White, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Date", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                            .clickable { showCalendarInPanel = true }
                            .padding(12.dp)
                        ) {
                            Text(sdf.format(endDate.value.time), color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                if (showCalendarInPanel) {
                    Dialog(
                        onDismissRequest = { showCalendarInPanel = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { showCalendarInPanel = false },
                            contentAlignment = Alignment.Center
                        ) {
                            CalendarWindow(
                                initialStartDate = startDate.value,
                                initialEndDate = endDate.value,
                                modifier = Modifier
                                    .padding(24.dp)
                                    .clickable(enabled = false) { },
                                onClose = { showCalendarInPanel = false },
                                onPeriodSelected = { start, end ->
                                    startDate.value = start
                                    endDate.value = end
                                    showCalendarInPanel = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { onSave(title, description, startDate.value, endDate.value) },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (trip == null) "Add trip" else "Save trip", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (trip != null) {
                        Button(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
                        ) {
                            Text("Delete trip", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

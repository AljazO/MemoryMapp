package si.uni_lj.fe.tnuv.memorymapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import si.uni_lj.fe.tnuv.memorymapp.data.AppDatabase
import si.uni_lj.fe.tnuv.memorymapp.data.DataRepository
import si.uni_lj.fe.tnuv.memorymapp.ui.components.verticalScrollbar
import si.uni_lj.fe.tnuv.memorymapp.ui.viewmodels.AuthViewModel
import si.uni_lj.fe.tnuv.memorymapp.utils.StatisticsCalculator
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccountSettingsScreen(
    onMenuClick: () -> Unit,
    onLogoutClick: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    
    val database = remember { AppDatabase.getDatabase(context) }
    val locationDao = database.locationDao()
    val repository = remember { DataRepository(locationDao) }
    
    var isEditing by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    // Statistics state
    var totalDistance by remember { mutableStateOf(0.0) }
    var totalMediaCount by remember { mutableStateOf(0) }

    // Update local state when currentUser changes (e.g., on first load)
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            if (!isEditing) {
                name = user.fullName
                username = user.username
                bio = user.bio
            }

            // Calculate Statistics
            scope.launch {
                // 1. Get Media Count
                val mediaIds = locationDao.getAllMediaIds(user.uid)
                totalMediaCount = mediaIds.size

                // 2. Get Total Distance
                val trips = locationDao.getAllTripsSync(user.uid)
                var distanceSum = 0.0
                for (trip in trips) {
                    val points = locationDao.getPointsInRangeSync(user.uid, trip.startTime, trip.endTime)
                    if (points.isNotEmpty()) {
                        val stats = StatisticsCalculator.calculateStats(points, trip.startTime, trip.endTime)
                        distanceSum += stats.distanceKm
                    }
                }
                totalDistance = distanceSum
            }
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing) {
                    IconButton(onClick = { 
                        currentUser?.let {
                            name = it.fullName
                            username = it.username
                            bio = it.bio
                        }
                        isEditing = false 
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                } else {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                Text("My account", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                
                if (isEditing) {
                    IconButton(onClick = { 
                        viewModel.updateProfile(name, username, bio)
                        isEditing = false
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color(0xFF6E6EF7), modifier = Modifier.size(32.dp))
                    }
                } else {
                    IconButton(onClick = { isEditing = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScrollbar(scrollState)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Profile Image
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp))
                }
            }
            if (isEditing) {
                TextButton(onClick = { /* Change photo */ }) {
                    Text("Change photo", color = Color(0xFF6E6EF7), fontSize = 12.sp)
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fields
            AccountField("Name and surname", name, "Name and surname", isEditing) { name = it }
            Spacer(modifier = Modifier.height(16.dp))
            AccountField("Username", username, "@username", isEditing) { 
                if (it.startsWith("@") || it.isEmpty()) {
                    username = it
                } else {
                    username = "@$it"
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (!isEditing) {
                StatRow("Email address", currentUser?.email ?: "")
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text("Bio", color = Color.White, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isEditing) {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Insert text", color = Color.Gray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedBorderColor = Color.Gray,
                        focusedBorderColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            } else {
                Text(
                    text = bio.ifEmpty { "No bio added" },
                    color = if (bio.isEmpty()) Color.Gray else Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Stats
            val memberSince = remember(currentUser?.createdAt) {
                currentUser?.createdAt?.let {
                    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
                } ?: "N/A"
            }
            
            StatRow("Member since", memberSince)
            Spacer(modifier = Modifier.height(12.dp))
            StatRow("Total distance covered", "%.2f km".format(totalDistance)) 
            Spacer(modifier = Modifier.height(12.dp))
            StatRow("Total photos and videos taken", totalMediaCount.toString())

            Spacer(modifier = Modifier.height(32.dp))

            if (!isEditing) {
                // Sync Button
                Button(
                    onClick = {
                        scope.launch {
                            isSyncing = true
                            repository.syncAllTrips()
                            isSyncing = false
                            Toast.makeText(context, "All data synced to cloud!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSyncing) Color.Gray else Color(0xFF6E6EF7)
                    ),
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync data to Cloud", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Want to change your account? ", color = Color.Gray, fontSize = 14.sp)
                    TextButton(onClick = onLogoutClick, contentPadding = PaddingValues(0.dp)) {
                        Text("Log out", color = Color(0xFF6E6EF7), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AccountField(label: String, value: String, placeholder: String, isEditing: Boolean, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        if (isEditing) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, color = Color.Gray) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedBorderColor = Color.Gray,
                    focusedBorderColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        } else {
            Text(
                text = value.ifEmpty { placeholder },
                color = if (value.isEmpty()) Color.Gray else Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

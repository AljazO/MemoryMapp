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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import si.uni_lj.fe.tnuv.memorymapp.ui.components.verticalScrollbar
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientEnd
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientStart
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    onMenuClick: () -> Unit,
    initialStartDate: Calendar? = null,
    initialEndDate: Calendar? = null,
    showAddInitially: Boolean = false
) {
    var showAddTrip by remember { mutableStateOf(showAddInitially) }
    val scrollState = rememberScrollState()

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
                Box(modifier = Modifier.size(32.dp)) // Placeholder to balance
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // Placeholder Trip Card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScrollbar(scrollState)
                    .verticalScroll(scrollState)
            ) {
                TripItem()
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
                    .clickable { showAddTrip = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Trip", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            if (showAddTrip) {
                AddTripPanel(
                    onClose = { showAddTrip = false },
                    startDate = initialStartDate,
                    endDate = initialEndDate
                )
            }
        }
    }
}

@Composable
fun TripItem() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
                    .background(Color.DarkGray, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Dodano potovanje", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("24.04.2026 - 26.04.2026", color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun AddTripPanel(
    onClose: () -> Unit,
    startDate: Calendar? = null,
    endDate: Calendar? = null
) {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
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
                    Text("Add your trip", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Insert name", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp)).padding(12.dp)
                        ) {
                            Text("Trip name", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                        Text("Cover photo", color = Color.White, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Insert description", color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp).background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp)).padding(12.dp)
                ) {
                    Text("Custom text", color = Color.Gray, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        val dateText = if (startDate != null) sdf.format(startDate.time) else "dd.mm.yyyy"
                        Text("Insert date", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Text(dateText, color = if (startDate != null) Color.White else Color.Gray, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Insert period", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val startPeriodText = if (startDate != null) sdf.format(startDate.time) else "dd.mm.yyyy"
                        val endPeriodText = if (endDate != null) sdf.format(endDate.time) else "dd.mm.yyyy"
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f).background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp)).padding(8.dp)) {
                                Text(startPeriodText, color = if (startDate != null) Color.White else Color.Gray, fontSize = 10.sp)
                            }
                            Text(" Start", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f).background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp)).padding(8.dp)) {
                                Text(endPeriodText, color = if (endDate != null) Color.White else Color.Gray, fontSize = 10.sp)
                            }
                            Text(" End", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp).width(30.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
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
                        Text("Add trip", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

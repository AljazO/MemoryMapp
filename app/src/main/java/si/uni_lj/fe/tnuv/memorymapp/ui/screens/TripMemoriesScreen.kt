package si.uni_lj.fe.tnuv.memorymapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import si.uni_lj.fe.tnuv.memorymapp.data.AppDatabase
import si.uni_lj.fe.tnuv.memorymapp.data.MediaPoint
import si.uni_lj.fe.tnuv.memorymapp.ui.components.MediaPreviewDialog
import si.uni_lj.fe.tnuv.memorymapp.ui.components.verticalScrollbar
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.DarkBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripMemoriesScreen(
    tripId: Long,
    onBackClick: () -> Unit
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

    val filteredMedia by locationDao.getMediaInRange(trip.startTime, trip.endTime)
        .collectAsState(initial = emptyList())

    var selectedMedia by remember { mutableStateOf<MediaPoint?>(null) }
    val gridState = rememberLazyGridState()

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
                Column {
                    Text(
                        text = trip.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pictures from trip",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (filteredMedia.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No media captured during this trip", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(gridState)
                ) {
                    items(filteredMedia, key = { it.id }) { item ->
                        MediaItemView(
                            item = item,
                            onLikeToggle = {
                                scope.launch {
                                    locationDao.updateMediaLikeStatus(item.id, !item.isLiked)
                                }
                            },
                            onClick = {
                                selectedMedia = item
                            }
                        )
                    }
                }
            }

            selectedMedia?.let { media ->
                MediaPreviewDialog(
                    mediaPoint = media,
                    onDismissRequest = { selectedMedia = null }
                )
            }
        }
    }
}

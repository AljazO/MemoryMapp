package si.uni_lj.fe.tnuv.memorymapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import si.uni_lj.fe.tnuv.memorymapp.data.AppDatabase
import si.uni_lj.fe.tnuv.memorymapp.data.MediaPoint
import si.uni_lj.fe.tnuv.memorymapp.data.MediaType
import si.uni_lj.fe.tnuv.memorymapp.ui.components.verticalScrollbar
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientEnd
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientStart
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(onMenuClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Date state (Sync with multi-day navigation)
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    val isToday = remember(selectedDate) {
        val today = Calendar.getInstance()
        selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    var showSortMenu by remember { mutableStateOf(false) }
    val dateString = remember(selectedDate) {
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(selectedDate.time)
    }

    val gridState = rememberLazyGridState()

    // Database
    val database = remember { AppDatabase.getDatabase(context) }
    val locationDao = database.locationDao()

    val startOfDay = remember(selectedDate) {
        val cal = selectedDate.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val allMediaForDay by locationDao.getMediaInRange(startOfDay, startOfDay + 86400000)
        .collectAsState(initial = emptyList())

    // Filter State
    var showPhotos by remember { mutableStateOf(true) }
    var showVideos by remember { mutableStateOf(true) }
    var onlyLiked by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf("Newest to oldest") }

    val filteredMedia = remember(allMediaForDay, showPhotos, showVideos, onlyLiked, sortOrder) {
        allMediaForDay.filter {
            val typeMatch = (it.type == MediaType.IMAGE && showPhotos) || (it.type == MediaType.VIDEO && showVideos)
            val likeMatch = !onlyLiked || it.isLiked
            typeMatch && likeMatch
        }.sortedWith { a, b ->
            if (sortOrder == "Newest to oldest") b.timestamp.compareTo(a.timestamp)
            else a.timestamp.compareTo(b.timestamp)
        }
    }

    val relativeDay = remember(selectedDate) {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val target = selectedDate.clone() as Calendar
        target.set(Calendar.HOUR_OF_DAY, 0)
        target.set(Calendar.MINUTE, 0)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)

        val diffMillis = today.timeInMillis - target.timeInMillis
        val diffDays = (diffMillis / (24 * 60 * 60 * 1000)).toInt()

        when (diffDays) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> "$diffDays days ago"
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val newCal = selectedDate.clone() as Calendar
                            newCal.add(Calendar.DAY_OF_YEAR, -1)
                            selectedDate = newCal
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.White)
                        }
                        Text(
                            text = relativeDay,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = {
                                if (!isToday) {
                                    val newCal = selectedDate.clone() as Calendar
                                    newCal.add(Calendar.DAY_OF_YEAR, 1)
                                    selectedDate = newCal
                                }
                            },
                            enabled = !isToday
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Next",
                                tint = if (isToday) Color.White.copy(alpha = 0.3f) else Color.White
                            )
                        }
                    }
                    Text(dateString, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                IconButton(onClick = { /* Calendar */ }) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (filteredMedia.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No media captured", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
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
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(56.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                        shape = CircleShape
                    )
                    .clickable { showSortMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "Filter", tint = Color.White)
            }

            if (showSortMenu) {
                SortMediaPanel(
                    showPhotos = showPhotos,
                    onShowPhotosChange = { showPhotos = it },
                    showVideos = showVideos,
                    onShowVideosChange = { showVideos = it },
                    onlyLiked = onlyLiked,
                    onOnlyLikedChange = { onlyLiked = it },
                    sortOrder = sortOrder,
                    onSortOrderChange = { sortOrder = it },
                    onClose = { showSortMenu = false }
                )
            }
        }
    }
}

@Composable
fun MediaItemView(item: MediaPoint, onLikeToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.DarkGray)
    ) {
        Image(
            painter = rememberAsyncImagePainter(item.uri),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Heart icon at bottom right
        IconButton(
            onClick = onLikeToggle,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = if (item.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like",
                tint = if (item.isLiked) Color.Red else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        if (item.type == MediaType.VIDEO) {
            Icon(
                Icons.Default.PlayCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.Center).size(32.dp)
            )
        }
    }
}

@Composable
fun SortMediaPanel(
    showPhotos: Boolean,
    onShowPhotosChange: (Boolean) -> Unit,
    showVideos: Boolean,
    onShowVideosChange: (Boolean) -> Unit,
    onlyLiked: Boolean,
    onOnlyLikedChange: (Boolean) -> Unit,
    sortOrder: String,
    onSortOrderChange: (String) -> Unit,
    onClose: () -> Unit
) {
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
                    Text("Sort media", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show media:", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        FilterCheckbox("Photos", showPhotos, onShowPhotosChange)
                        FilterCheckbox("Videos", showVideos, onShowVideosChange)
                        FilterCheckbox("Only liked", onlyLiked, onOnlyLikedChange)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sort by:", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        FilterRadioButton("Newest to oldest", sortOrder == "Newest to oldest") {
                            onSortOrderChange("Newest to oldest")
                        }
                        FilterRadioButton("Oldest to newest", sortOrder == "Oldest to newest") {
                            onSortOrderChange("Oldest to newest")
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
                        Text("Sort", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterCheckbox(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.padding(vertical = 4.dp).clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF6E6EF7), checkmarkColor = Color.White)
        )
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun FilterRadioButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.padding(vertical = 4.dp).clickable { onClick() }
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6E6EF7))
        )
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}

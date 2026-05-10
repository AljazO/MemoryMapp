package si.uni_lj.fe.tnuv.memorymapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import si.uni_lj.fe.tnuv.memorymapp.ui.components.verticalScrollbar
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientEnd
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientStart
import java.text.SimpleDateFormat
import java.util.*

data class MediaItem(val id: Int, val isLiked: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(onMenuClick: () -> Unit) {
    var showSortMenu by remember { mutableStateOf(false) }
    val currentDate = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date()) }

    val gridState = rememberLazyGridState()

    // Media State
    var mediaItems by remember { 
        mutableStateOf((1..10).map { MediaItem(it) })
    }

    // Filter State
    var showPhotos by remember { mutableStateOf(true) }
    var showVideos by remember { mutableStateOf(true) }
    var onlyLiked by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf("Newest to oldest") }

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
                        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White)
                        Text("Today", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                    }
                    Text(currentDate, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                IconButton(onClick = { /* Calendar */ }) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
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
                itemsIndexed(mediaItems) { index, item ->
                    MediaItemView(
                        item = item,
                        onLikeToggle = {
                            val newList = mediaItems.toMutableList()
                            newList[index] = item.copy(isLiked = !item.isLiked)
                            mediaItems = newList
                        }
                    )
                }
            }

            FloatingActionButton(
                onClick = { showSortMenu = true },
                containerColor = Color(0xFF6E6EF7),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "Filter")
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
fun MediaItemView(item: MediaItem, onLikeToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.DarkGray)
    ) {
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

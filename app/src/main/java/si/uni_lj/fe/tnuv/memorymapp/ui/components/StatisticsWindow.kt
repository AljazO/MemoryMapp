package si.uni_lj.fe.tnuv.memorymapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import si.uni_lj.fe.tnuv.memorymapp.data.LocationPoint
import si.uni_lj.fe.tnuv.memorymapp.utils.StatisticsCalculator

@Composable
fun StatisticsWindow(
    points: List<LocationPoint>,
    startTime: Long,
    endTime: Long,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val stats = remember(points, startTime, endTime) {
        StatisticsCalculator.calculateStats(points, startTime, endTime)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1115))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Statistics",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(label = "Distance", value = "%.2f km".format(stats.distanceKm), icon = null)
                StatItem(label = "Duration", value = StatisticsCalculator.formatDuration(stats.durationMillis), icon = null)
                StatItem(label = "Calories", value = "%d kcal".format(stats.calories), icon = null)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(label = "Steps", value = "%d".format(stats.steps), icon = Icons.AutoMirrored.Filled.DirectionsRun)
                StatItem(label = "Avg Pace", value = "%.2f km/h".format(stats.avgPaceKmh), icon = Icons.Default.Timer, iconTint = Color(0xFF007AFF))
                StatItem(label = "Elevation", value = "%.0f m".format(stats.elevationGainM), icon = Icons.AutoMirrored.Filled.TrendingUp, iconTint = Color(0xFF81D4FA))
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

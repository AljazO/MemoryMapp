package si.uni_lj.fe.tnuv.memorymapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SelectionInfoBar(
    startDate: Calendar,
    endDate: Calendar,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSingleDay = remember(startDate, endDate) {
        startDate.get(Calendar.YEAR) == endDate.get(Calendar.YEAR) &&
                startDate.get(Calendar.DAY_OF_YEAR) == endDate.get(Calendar.DAY_OF_YEAR)
    }
    
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH)
    val periodText = if (isSingleDay) {
        sdf.format(startDate.time)
    } else {
        "${sdf.format(startDate.time)} - ${sdf.format(endDate.time)}"
    }

    Surface(
        color = Color.Black.copy(alpha = 0.8f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Selected period:", color = Color.Gray, fontSize = 10.sp)
                Text(periodText, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onClear,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6E6EF7)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Clear", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

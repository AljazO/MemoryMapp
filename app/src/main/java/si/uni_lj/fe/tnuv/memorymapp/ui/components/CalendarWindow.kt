package si.uni_lj.fe.tnuv.memorymapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientEnd
import si.uni_lj.fe.tnuv.memorymapp.ui.theme.GradientStart
import java.util.*

@Composable
fun CalendarWindow(
    initialStartDate: Calendar,
    initialEndDate: Calendar,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onPeriodSelected: (Calendar, Calendar) -> Unit
) {
    var calendar by remember { mutableStateOf(initialStartDate.clone() as Calendar) }
    var selectedStart by remember { mutableStateOf<Calendar?>(initialStartDate) }
    var selectedEnd by remember { mutableStateOf<Calendar?>(initialEndDate) }

    val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH)
    val year = calendar.get(Calendar.YEAR)
    val today = Calendar.getInstance()

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val firstDayOfWeekOffset = remember(calendar) {
        val c = calendar.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        val dayOfWeek = c.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
    }

    val prevMonthDays = remember(calendar) {
        val c = calendar.clone() as Calendar
        c.add(Calendar.MONTH, -1)
        c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val dates = remember(calendar) {
        val list = mutableListOf<Triple<Int, Int, Int>>() // day, monthOffset, year
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Prev month
        for (i in (prevMonthDays - firstDayOfWeekOffset + 1)..prevMonthDays) {
            list.add(Triple(i, -1, currentYear))
        }
        // Current month
        for (i in 1..daysInMonth) {
            list.add(Triple(i, 0, currentYear))
        }
        // Next month
        val remaining = 42 - list.size
        for (i in 1..remaining) {
            list.add(Triple(i, 1, currentYear))
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
                    IconButton(onClick = {
                        val c = calendar.clone() as Calendar
                        c.add(Calendar.MONTH, -1)
                        calendar = c
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$monthName, $year",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        val c = calendar.clone() as Calendar
                        c.add(Calendar.MONTH, 1)
                        calendar = c
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                    }
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
                items(dates) { (day, monthOffset, itemYear) ->
                    val itemDate = calendar.clone() as Calendar
                    itemDate.set(Calendar.YEAR, itemYear)
                    itemDate.add(Calendar.MONTH, monthOffset)
                    itemDate.set(Calendar.DAY_OF_MONTH, day)
                    itemDate.set(Calendar.HOUR_OF_DAY, 0)
                    itemDate.set(Calendar.MINUTE, 0)
                    itemDate.set(Calendar.SECOND, 0)
                    itemDate.set(Calendar.MILLISECOND, 0)

                    val isSelectedStart = selectedStart != null && itemDate.timeInMillis == selectedStart!!.timeInMillis
                    val isSelectedEnd = selectedEnd != null && itemDate.timeInMillis == selectedEnd!!.timeInMillis
                    val isInRange = selectedStart != null && selectedEnd != null && 
                                   itemDate.timeInMillis >= selectedStart!!.timeInMillis && 
                                   itemDate.timeInMillis <= selectedEnd!!.timeInMillis
                    
                    val isToday = itemDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                 itemDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .aspectRatio(1f)
                            .background(
                                color = when {
                                    isSelectedStart || isSelectedEnd -> Color(0xFF6E6EF7)
                                    isInRange -> Color(0xFF6E6EF7).copy(alpha = 0.3f)
                                    else -> Color(0xFF2C2C2E)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                val clickedDate = itemDate.clone() as Calendar
                                if (selectedStart == null || (selectedStart != null && selectedEnd != null)) {
                                    selectedStart = clickedDate
                                    selectedEnd = null
                                } else {
                                    if (clickedDate.before(selectedStart)) {
                                        selectedEnd = selectedStart
                                        selectedStart = clickedDate
                                    } else {
                                        selectedEnd = clickedDate
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day.toString(),
                                color = if (monthOffset == 0) Color.White else Color.Gray.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isToday) {
                                Box(modifier = Modifier.size(4.dp).background(Color(0xFF6E6EF7), CircleShape))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val finalStart = selectedStart ?: initialStartDate
                    val finalEnd = (selectedEnd ?: finalStart).clone() as Calendar
                    // Ensure end date is at the very end of the day
                    finalEnd.set(Calendar.HOUR_OF_DAY, 23)
                    finalEnd.set(Calendar.MINUTE, 59)
                    finalEnd.set(Calendar.SECOND, 59)
                    finalEnd.set(Calendar.MILLISECOND, 999)

                    onPeriodSelected(finalStart, finalEnd)
                },
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
                    Text("Confirm Period", color = Color.White, fontWeight = FontWeight.Bold)
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

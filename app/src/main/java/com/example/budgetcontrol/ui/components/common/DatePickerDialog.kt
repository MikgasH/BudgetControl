package com.example.budgetcontrol.ui.components.common

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.budgetcontrol.R
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DatePickerDialog(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var currentMonth by remember {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = selectedDate })
    }

    val monthYearFormatter = remember(Locale.getDefault().language) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    }

    // Prevent navigating beyond the current month
    val maxMonth = Calendar.getInstance()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.date_selection),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            currentMonth = Calendar.getInstance().apply {
                                timeInMillis = currentMonth.timeInMillis
                                add(Calendar.MONTH, -1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = stringResource(R.string.previous_month)
                        )
                    }

                    Text(
                        text = monthYearFormatter.format(currentMonth.time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    val canGoForward = currentMonth.get(Calendar.YEAR) < maxMonth.get(Calendar.YEAR) ||
                            (currentMonth.get(Calendar.YEAR) == maxMonth.get(Calendar.YEAR) &&
                                    currentMonth.get(Calendar.MONTH) < maxMonth.get(Calendar.MONTH))

                    if (canGoForward) {
                        IconButton(
                            onClick = {
                                currentMonth = Calendar.getInstance().apply {
                                    timeInMillis = currentMonth.timeInMillis
                                    add(Calendar.MONTH, 1)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = stringResource(R.string.next_month)
                            )
                        }
                    } else {
                        // Placeholder to keep the title centered
                        Box(modifier = Modifier.size(48.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(stringResource(R.string.day_mon), stringResource(R.string.day_tue), stringResource(R.string.day_wed), stringResource(R.string.day_thu), stringResource(R.string.day_fri), stringResource(R.string.day_sat), stringResource(R.string.day_sun)).forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                CalendarGrid(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    onDateSelected = { date ->
                        if (date <= System.currentTimeMillis()) {
                            onDateSelected(date)
                            onDismiss()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel_upper))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: Calendar,
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val daysInMonth = getDaysInMonth(currentMonth)

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(daysInMonth) { dayInfo ->
            DayItem(
                dayInfo = dayInfo,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
private fun DayItem(
    dayInfo: DayInfo,
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val isSelected = isSameDay(dayInfo.date, selectedDate)
    val isToday = isSameDay(dayInfo.date, System.currentTimeMillis())
    val isFuture = dayInfo.date > System.currentTimeMillis() && !isToday

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.tertiary
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = dayInfo.isCurrentMonth && !isFuture) {
                onDateSelected(dayInfo.date)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayInfo.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                !dayInfo.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isToday -> MaterialTheme.colorScheme.onTertiary
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private data class DayInfo(
    val date: Long,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean
)

private fun getDaysInMonth(currentMonth: Calendar): List<DayInfo> {
    val days = mutableListOf<DayInfo>()

    val firstDayOfMonth = Calendar.getInstance().apply {
        timeInMillis = currentMonth.timeInMillis
        set(Calendar.DAY_OF_MONTH, 1)
    }

    // Monday = Calendar.MONDAY (2), so offset accordingly
    val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)
    val daysFromPrevMonth = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

    val prevMonth = Calendar.getInstance().apply {
        timeInMillis = firstDayOfMonth.timeInMillis
        add(Calendar.MONTH, -1)
    }
    val daysInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

    for (i in daysFromPrevMonth downTo 1) {
        val day = daysInPrevMonth - i + 1
        val date = Calendar.getInstance().apply {
            timeInMillis = prevMonth.timeInMillis
            set(Calendar.DAY_OF_MONTH, day)
        }.timeInMillis

        days.add(DayInfo(date, day, false))
    }

    val daysInCurrentMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (day in 1..daysInCurrentMonth) {
        val date = Calendar.getInstance().apply {
            timeInMillis = currentMonth.timeInMillis
            set(Calendar.DAY_OF_MONTH, day)
        }.timeInMillis

        days.add(DayInfo(date, day, true))
    }

    // Fill remaining cells to complete a 6-week grid
    val totalCells = 42
    val remainingCells = totalCells - days.size

    for (day in 1..remainingCells) {
        val nextMonth = Calendar.getInstance().apply {
            timeInMillis = currentMonth.timeInMillis
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, day)
        }

        days.add(DayInfo(nextMonth.timeInMillis, day, false))
    }

    return days
}

private fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
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
fun PeriodRangePicker(
    onPeriodSelected: (Long, Long) -> Unit,
    onDismiss: () -> Unit,
    onAllTimeSelected: (() -> Unit)? = null
) {
    var currentMonth by remember {
        mutableStateOf(Calendar.getInstance())
    }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

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
                    text = stringResource(R.string.select_period),
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
                        text = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                            .format(currentMonth.time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

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

                PeriodCalendarGrid(
                    currentMonth = currentMonth,
                    startDate = startDate,
                    endDate = endDate,
                    onDateSelected = { date ->
                        when {
                            startDate == null || (endDate != null) -> {
                                startDate = date
                                endDate = null
                            }
                            startDate != null && endDate == null -> {
                                if (date >= startDate!!) {
                                    endDate = date
                                } else {
                                    // Selected date is before start, so swap them
                                    endDate = startDate
                                    startDate = date
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (startDate != null) {
                    Text(
                        text = when {
                            endDate != null -> stringResource(R.string.period_range_display, formatPeriod(startDate!!, endDate!!))
                            else -> stringResource(R.string.start_date_display, SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(startDate!!)))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            if (onAllTimeSelected != null) {
                                onAllTimeSelected()
                            } else {
                                val now = Calendar.getInstance()
                                val allTimeStart = Calendar.getInstance().apply {
                                    set(now.get(Calendar.YEAR), 0, 1, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                val allTimeEnd = Calendar.getInstance().apply {
                                    set(now.get(Calendar.YEAR), 11, 31, 23, 59, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }.timeInMillis
                                onPeriodSelected(allTimeStart, allTimeEnd)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.all_time),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel_upper))
                    }

                    if (startDate != null && endDate != null) {
                        TextButton(
                            onClick = {
                                onPeriodSelected(startDate!!, endDate!!)
                                onDismiss()
                            }
                        ) {
                            Text(stringResource(R.string.select_upper))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodCalendarGrid(
    currentMonth: Calendar,
    startDate: Long?,
    endDate: Long?,
    onDateSelected: (Long) -> Unit
) {
    val daysInMonth = getPeriodDaysInMonth(currentMonth)

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(240.dp)
    ) {
        items(daysInMonth) { dayInfo ->
            PeriodDayItem(
                dayInfo = dayInfo,
                startDate = startDate,
                endDate = endDate,
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
private fun PeriodDayItem(
    dayInfo: PeriodDayInfo,
    startDate: Long?,
    endDate: Long?,
    onDateSelected: (Long) -> Unit
) {
    val isStart = startDate != null && isSameDay(dayInfo.date, startDate)
    val isEnd = endDate != null && isSameDay(dayInfo.date, endDate)
    val isInRange = startDate != null && endDate != null &&
            dayInfo.date >= startDate && dayInfo.date <= endDate
    val isToday = isSameDay(dayInfo.date, System.currentTimeMillis())

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                when {
                    isStart || isEnd -> MaterialTheme.colorScheme.primary
                    isInRange -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    isToday -> MaterialTheme.colorScheme.tertiary
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = dayInfo.isCurrentMonth) {
                onDateSelected(dayInfo.date)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayInfo.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                !dayInfo.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                isStart || isEnd -> MaterialTheme.colorScheme.onPrimary
                isToday -> MaterialTheme.colorScheme.onTertiary
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isStart || isEnd || isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private data class PeriodDayInfo(
    val date: Long,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean
)

private fun getPeriodDaysInMonth(currentMonth: Calendar): List<PeriodDayInfo> {
    val days = mutableListOf<PeriodDayInfo>()

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

        days.add(PeriodDayInfo(date, day, false))
    }

    val daysInCurrentMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (day in 1..daysInCurrentMonth) {
        val date = Calendar.getInstance().apply {
            timeInMillis = currentMonth.timeInMillis
            set(Calendar.DAY_OF_MONTH, day)
        }.timeInMillis

        days.add(PeriodDayInfo(date, day, true))
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

        days.add(PeriodDayInfo(nextMonth.timeInMillis, day, false))
    }

    return days
}

private fun formatPeriod(startDate: Long, endDate: Long): String {
    val formatter = SimpleDateFormat("d MMM", Locale.getDefault())
    val startFormatted = formatter.format(Date(startDate))
    val endFormatted = formatter.format(Date(endDate))
    return "$startFormatted - $endFormatted"
}

private fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
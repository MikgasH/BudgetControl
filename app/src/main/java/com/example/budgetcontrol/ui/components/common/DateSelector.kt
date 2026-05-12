package com.example.budgetcontrol.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.util.DateRangeHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Shared component for date selection
 */
@Composable
fun DateSelector(
    selectedDate: Long,
    onDateSelect: (Long) -> Unit,
    onShowDatePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.date_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickDateOptions(
                selectedDate = selectedDate,
                onDateSelect = onDateSelect,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onShowDatePicker) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = stringResource(R.string.select_date),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun QuickDateOptions(
    selectedDate: Long,
    onDateSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    val today = calendar.timeInMillis

    calendar.add(Calendar.DAY_OF_MONTH, -1)
    val yesterday = calendar.timeInMillis

    calendar.add(Calendar.DAY_OF_MONTH, -1)
    val dayBeforeYesterday = calendar.timeInMillis

    val dateFormatter = remember(Locale.getDefault().language) {
        SimpleDateFormat("dd.MM", Locale.getDefault())
    }

    val quickDates = listOf(
        today to Pair(
            dateFormatter.format(Date(today)),
            stringResource(R.string.today)
        ),
        yesterday to Pair(
            dateFormatter.format(Date(yesterday)),
            stringResource(R.string.yesterday)
        ),
        dayBeforeYesterday to Pair(
            dateFormatter.format(Date(dayBeforeYesterday)),
            stringResource(R.string.day_before_yesterday)
        )
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        quickDates.forEach { (date, labelPair) ->
            val isSelected = DateRangeHelper.isSameDay(date, selectedDate)
            val (dateText, dayText) = labelPair

            FilterChip(
                onClick = { onDateSelect(date) },
                label = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .height(44.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = dayText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                },
                selected = isSelected,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
        }

        // Show the selected date chip when it's not one of the quick options
        if (!quickDates.any { DateRangeHelper.isSameDay(it.first, selectedDate) }) {
            FilterChip(
                onClick = { },
                label = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .height(40.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = dateFormatter.format(Date(selectedDate)),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = stringResource(R.string.selected),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                },
                selected = true,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
        }
    }
}
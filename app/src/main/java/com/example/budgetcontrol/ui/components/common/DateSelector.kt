package com.example.budgetcontrol.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
 * Общий компонент для выбора даты
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
            // Быстрые варианты дат
            QuickDateOptions(
                selectedDate = selectedDate,
                onDateSelect = onDateSelect,
                modifier = Modifier.weight(1f)
            )

            // Кнопка календаря
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

    val quickDates = listOf(
        today to Pair(
            SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(today)),
            stringResource(R.string.today)
        ),
        yesterday to Pair(
            SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(yesterday)),
            stringResource(R.string.yesterday)
        ),
        dayBeforeYesterday to Pair(
            SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(dayBeforeYesterday)),
            stringResource(R.string.day_before_yesterday)
        )
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp), // УМЕНЬШИЛИ отступы между кнопками
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
                            .height(44.dp) // УВЕЛИЧИЛИ высоту контейнера
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp) // УМЕНЬШИЛИ внутренние отступы
                    ) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp, // УВЕЛИЧИЛИ размер шрифта
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = dayText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp, // УВЕЛИЧИЛИ размер шрифта
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
                    .height(56.dp), // УВЕЛИЧИЛИ высоту чипа
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
        }

        // Показываем выбранную дату если она не среди быстрых вариантов
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
                            text = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(selectedDate)),
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
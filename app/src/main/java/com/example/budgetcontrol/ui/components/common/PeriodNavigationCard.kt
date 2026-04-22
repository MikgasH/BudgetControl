package com.example.budgetcontrol.ui.components.common

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.CategoryStatistic
import com.example.budgetcontrol.feature.main.MainScreenUiState
import com.example.budgetcontrol.feature.main.OperationType
import com.example.budgetcontrol.feature.main.PeriodType
import com.example.budgetcontrol.core.util.formatAmount
import com.example.budgetcontrol.core.util.getCurrencySymbol
import com.example.budgetcontrol.ui.components.charts.PieChart
import androidx.core.graphics.toColorInt
import java.util.Calendar

@Composable
internal fun PeriodNavigationCard(
    uiState: MainScreenUiState,
    periodDisplayText: String,
    baseCurrency: String,
    onNavigate: (Int) -> Unit,
    openingBalance: Double? = null,
    displayCurrency: String = baseCurrency,
    isOpeningBalanceApproximate: Boolean = false,
    collapseFraction: Float = 0f,
    chartHeight: Dp = 200.dp,
    barHeight: Dp = 44.dp
) {
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 50.dp.toPx() }
    val currentUiState by rememberUpdatedState(uiState)
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    // Track navigation direction for animation: true = forward, false = backward
    var isForward by remember { mutableStateOf(true) }

    val isCollapsed = collapseFraction > 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(uiState.isAllTimePeriod, uiState.selectedPeriodType) {
                if (currentUiState.isAllTimePeriod) return@pointerInput
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        if (totalDrag > swipeThresholdPx) {
                            isForward = false
                            currentOnNavigate(-1) // swipe right → previous
                        } else if (totalDrag < -swipeThresholdPx) {
                            if (canNavigateToFuture(currentUiState)) {
                                isForward = true
                                currentOnNavigate(1) // swipe left → next
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(if (isCollapsed) PaddingValues(horizontal = 16.dp, vertical = 8.dp) else PaddingValues(20.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Period navigation row — fixed height to prevent layout jumps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isAllTimePeriod) {
                    Spacer(modifier = Modifier.size(if (isCollapsed) 32.dp else 48.dp))
                } else {
                    IconButton(
                        onClick = {
                            isForward = false
                            onNavigate(-1)
                        },
                        modifier = Modifier.size(if (isCollapsed) 32.dp else 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = stringResource(R.string.previous_period),
                            modifier = if (isCollapsed) Modifier.size(20.dp) else Modifier
                        )
                    }
                }

                AnimatedContent(
                    targetState = periodDisplayText to uiState.totalAmount,
                    transitionSpec = {
                        if (isForward) {
                            (slideInHorizontally { it } + fadeIn(tween(300))) togetherWith
                                    (slideOutHorizontally { -it } + fadeOut(tween(300)))
                        } else {
                            (slideInHorizontally { -it } + fadeIn(tween(300))) togetherWith
                                    (slideOutHorizontally { it } + fadeOut(tween(300)))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "period_text"
                ) { (periodText, totalAmount) ->
                    if (isCollapsed) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = periodText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${formatAmount(totalAmount)} ${getCurrencySymbol(baseCurrency)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = periodText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (uiState.isAllTimePeriod || !canNavigateToFuture(uiState)) {
                    Spacer(modifier = Modifier.size(if (isCollapsed) 32.dp else 48.dp))
                } else {
                    IconButton(
                        onClick = {
                            isForward = true
                            onNavigate(1)
                        },
                        modifier = Modifier.size(if (isCollapsed) 32.dp else 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.next_period),
                            modifier = if (isCollapsed) Modifier.size(20.dp) else Modifier
                        )
                    }
                }
            }

            // Opening balance — shown in both expanded and collapsed states
            if (openingBalance != null) {
                val prefix = if (isOpeningBalanceApproximate) "~" else ""
                Text(
                    text = stringResource(
                        R.string.opening_balance,
                        "$prefix${formatAmount(openingBalance)} ${getCurrencySymbol(displayCurrency)}"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Animated spacer between nav row and chart area
            Spacer(modifier = Modifier.height(lerp(8.dp, 0.dp, collapseFraction)))

            // Chart area — fixed animated height prevents layout jumps
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
                contentAlignment = Alignment.Center
            ) {
                if (isCollapsed) {
                    // Segmented bar matching PieChart ring thickness
                    if (uiState.categoryStatistics.isNotEmpty()) {
                        CategorySegmentedBar(
                            statistics = uiState.categoryStatistics,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight)
                                .clip(RoundedCornerShape(barHeight / 2))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight)
                                .clip(RoundedCornerShape(barHeight / 2))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                } else {
                    if (uiState.categoryStatistics.isNotEmpty()) {
                        PieChart(
                            data = uiState.categoryStatistics,
                            totalAmount = uiState.totalAmount,
                            baseCurrency = baseCurrency,
                            modifier = Modifier.size(chartHeight)
                        )
                    } else {
                        // Empty state: gray circle with "No data" text
                        Box(
                            modifier = Modifier
                                .size(chartHeight)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val emptyText = when {
                                    uiState.isAllTimePeriod -> {
                                        if (uiState.selectedOperationType == OperationType.EXPENSES) {
                                            stringResource(R.string.no_expenses_all_time)
                                        } else {
                                            stringResource(R.string.no_incomes_all_time)
                                        }
                                    }
                                    uiState.selectedPeriodType == PeriodType.DAY && uiState.currentPeriodIndex == 0 -> {
                                        if (uiState.selectedOperationType == OperationType.EXPENSES) {
                                            stringResource(R.string.no_expenses_today)
                                        } else {
                                            stringResource(R.string.no_incomes_today)
                                        }
                                    }
                                    uiState.selectedPeriodType == PeriodType.DAY -> {
                                        stringResource(R.string.no_data_this_day)
                                    }
                                    else -> {
                                        stringResource(R.string.no_data_this_period)
                                    }
                                }
                                Text(
                                    text = emptyText,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySegmentedBar(
    statistics: List<CategoryStatistic>,
    modifier: Modifier = Modifier
) {
    val segments = remember(statistics) {
        val totalPercentage = statistics.sumOf { it.percentage.toDouble() }.toFloat()
        if (totalPercentage == 0f) return@remember emptyList()
        statistics.map { stat ->
            val color = try {
                Color(stat.category.color.toColorInt())
            } catch (_: Exception) {
                Color.Gray
            }
            val fraction = stat.percentage / totalPercentage
            color to fraction
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .drawBehind {
                var xOffset = 0f
                segments.forEach { (color, fraction) ->
                    val segmentWidth = size.width * fraction
                    drawRect(
                        color = color,
                        topLeft = Offset(xOffset, 0f),
                        size = Size(segmentWidth, size.height)
                    )
                    xOffset += segmentWidth
                }
            }
    )
}

/**
 * Checks whether forward navigation is allowed (prevents going into a future period)
 */
private fun canNavigateToFuture(uiState: MainScreenUiState): Boolean {
    if (uiState.isAllTimePeriod) return false

    val calendar = Calendar.getInstance()
    val today = calendar.timeInMillis

    val nextPeriodCalendar = Calendar.getInstance()

    return when (uiState.selectedPeriodType) {
        PeriodType.DAY -> {
            nextPeriodCalendar.add(Calendar.DAY_OF_MONTH, uiState.currentPeriodIndex + 1)
            nextPeriodCalendar.timeInMillis <= today
        }

        PeriodType.WEEK -> {
            nextPeriodCalendar.add(Calendar.WEEK_OF_YEAR, uiState.currentPeriodIndex + 1)
            nextPeriodCalendar.set(Calendar.DAY_OF_WEEK, nextPeriodCalendar.firstDayOfWeek)
            nextPeriodCalendar.timeInMillis <= today
        }

        PeriodType.MONTH -> {
            nextPeriodCalendar.add(Calendar.MONTH, uiState.currentPeriodIndex + 1)
            nextPeriodCalendar.set(Calendar.DAY_OF_MONTH, 1)
            nextPeriodCalendar.get(Calendar.YEAR) < Calendar.getInstance().get(Calendar.YEAR) ||
                    (nextPeriodCalendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) &&
                            nextPeriodCalendar.get(Calendar.MONTH) <= Calendar.getInstance().get(Calendar.MONTH))
        }

        PeriodType.YEAR -> {
            nextPeriodCalendar.add(Calendar.YEAR, uiState.currentPeriodIndex + 1)
            nextPeriodCalendar.get(Calendar.YEAR) <= Calendar.getInstance().get(Calendar.YEAR)
        }

        PeriodType.PERIOD -> false
    }
}

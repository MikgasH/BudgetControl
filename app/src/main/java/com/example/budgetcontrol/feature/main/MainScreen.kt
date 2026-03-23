package com.example.budgetcontrol.feature.main

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import com.example.budgetcontrol.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.core.domain.model.CategoryStatistic
import com.example.budgetcontrol.ui.components.common.PeriodNavigationCard
import com.example.budgetcontrol.ui.components.common.PeriodRangePicker
import com.example.budgetcontrol.ui.util.displayName
import androidx.core.graphics.toColorInt
import java.util.Locale
import com.example.budgetcontrol.ui.util.getCategoryIcon
import com.example.budgetcontrol.core.util.DateRangeHelper
import com.example.budgetcontrol.core.util.getCurrencySymbol

@Composable
fun MainScreen(
    onAddExpenseClick: (Long) -> Unit,
    onAddIncomeClick: (Long) -> Unit,
    onCategoryClick: (categoryId: String, operationType: OperationType, startDate: Long, endDate: Long, isAllTime: Boolean) -> Unit = { _, _, _, _, _ -> },
    onSettingsClick: () -> Unit = {},
    onRateHistoryClick: () -> Unit = {},
    viewModel: MainScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val baseCurrency by viewModel.baseCurrency.collectAsState()
    val currentContext = LocalContext.current
    val periodDisplayText = DateRangeHelper.getPeriodDisplayText(
        context = currentContext,
        periodType = uiState.selectedPeriodType,
        periodOffset = uiState.currentPeriodIndex,
        customStartDate = uiState.customStartDate,
        customEndDate = uiState.customEndDate,
        isAllTimePeriod = uiState.isAllTimePeriod
    )
    var showPeriodPicker by remember { mutableStateOf(false) }

    if (showPeriodPicker) {
        PeriodRangePicker(
            onPeriodSelected = { startDate, endDate ->
                viewModel.selectCustomPeriod(startDate, endDate)
                showPeriodPicker = false
            },
            onDismiss = { showPeriodPicker = false },
            onAllTimeSelected = {
                viewModel.selectAllTime()
                showPeriodPicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 60.dp,
                            bottom = 10.dp
                        )
                ) {
                    IconButton(
                        onClick = onRateHistoryClick,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = stringResource(R.string.rate_history),
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Text(
                        text = viewModel.formatBalance(balance),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val selectedDate = viewModel.getCurrentSelectedDate()
                    val operationType = viewModel.getCurrentSelectedOperationType()
                    when (operationType) {
                        OperationType.EXPENSES -> onAddExpenseClick(selectedDate)
                        OperationType.INCOMES -> onAddIncomeClick(selectedDate)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_operation),
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        val density = LocalDensity.current
        val fullHeight = 200.dp
        val ringFraction = 0.22f
        val collapsedBarHeight = fullHeight * ringFraction // 44dp — matches PieChart ring
        val maxCollapseOffsetPx = with(density) { (fullHeight - collapsedBarHeight).toPx() }
        var collapseOffsetPx by remember { mutableFloatStateOf(0f) }
        var snapAnimJob by remember { mutableStateOf<Job?>(null) }

        fun snapCollapseToNearest() {
            val fraction = if (maxCollapseOffsetPx > 0f) collapseOffsetPx / maxCollapseOffsetPx else 0f
            val target = if (fraction > 0.5f) maxCollapseOffsetPx else 0f
            if (collapseOffsetPx == target) return
            snapAnimJob?.cancel()
            snapAnimJob = scope.launch {
                animate(
                    initialValue = collapseOffsetPx,
                    targetValue = target,
                    animationSpec = tween(300)
                ) { value, _ ->
                    collapseOffsetPx = value
                }
            }
        }

        val nestedScrollConnection = remember(maxCollapseOffsetPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (available.y < 0f) {
                        snapAnimJob?.cancel()
                        // Scrolling down → collapse chart first
                        val oldOffset = collapseOffsetPx
                        collapseOffsetPx = (collapseOffsetPx - available.y)
                            .coerceIn(0f, maxCollapseOffsetPx)
                        return Offset(0f, -(collapseOffsetPx - oldOffset))
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (available.y > 0f) {
                        snapAnimJob?.cancel()
                        // Scrolling up + list at top → expand chart
                        val oldOffset = collapseOffsetPx
                        collapseOffsetPx = (collapseOffsetPx - available.y)
                            .coerceIn(0f, maxCollapseOffsetPx)
                        return Offset(0f, oldOffset - collapseOffsetPx)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    snapCollapseToNearest()
                    return Velocity.Zero
                }
            }
        }

        val collapseFraction = if (maxCollapseOffsetPx > 0f) {
            (collapseOffsetPx / maxCollapseOffsetPx).coerceIn(0f, 1f)
        } else 0f
        val chartHeight = lerp(fullHeight, collapsedBarHeight, collapseFraction)

        val isEmpty = uiState.categoryStatistics.isEmpty()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(isEmpty, maxCollapseOffsetPx) {
                    if (!isEmpty) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var lastX = down.position.x
                        var lastY = down.position.y
                        var directionDecided = false
                        var isVertical = false
                        do {
                            val event = awaitPointerEvent()
                            val drag = event.changes.firstOrNull() ?: break
                            val dx = drag.position.x - lastX
                            val dy = drag.position.y - lastY
                            if (!directionDecided) {
                                val totalMove = kotlin.math.abs(dx) + kotlin.math.abs(dy)
                                if (totalMove > 10f) {
                                    directionDecided = true
                                    isVertical = kotlin.math.abs(dy) > kotlin.math.abs(dx)
                                    if (!isVertical) break // horizontal — let it pass through
                                }
                            }
                            if (isVertical) {
                                lastX = drag.position.x
                                lastY = drag.position.y
                                collapseOffsetPx = (collapseOffsetPx - dy)
                                    .coerceIn(0f, maxCollapseOffsetPx)
                                drag.consume()
                            }
                        } while (event.changes.any { it.pressed })
                        if (isVertical) snapCollapseToNearest()
                    }
                }
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            // Fixed header: toggle, period selector, chart
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .pointerInput(maxCollapseOffsetPx) {
                        detectVerticalDragGestures(
                            onDragStart = { snapAnimJob?.cancel() },
                            onDragEnd = { snapCollapseToNearest() },
                            onDragCancel = { snapCollapseToNearest() }
                        ) { _, dragAmount ->
                            collapseOffsetPx = (collapseOffsetPx - dragAmount)
                                .coerceIn(0f, maxCollapseOffsetPx)
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExpenseIncomeToggle(
                    selectedType = uiState.selectedOperationType,
                    onTypeSelected = viewModel::selectOperationType
                )

                FixedPeriodTypeSelector(
                    selectedPeriod = uiState.selectedPeriodType,
                    onPeriodSelected = { period ->
                        if (period == PeriodType.PERIOD) {
                            showPeriodPicker = true
                        } else {
                            viewModel.selectPeriodType(period)
                        }
                    }
                )

                PeriodNavigationCard(
                    uiState = uiState,
                    periodDisplayText = periodDisplayText,
                    baseCurrency = baseCurrency,
                    onNavigate = viewModel::navigatePeriod,
                    collapseFraction = collapseFraction,
                    chartHeight = chartHeight,
                    barHeight = collapsedBarHeight
                )
            }

            if (uiState.categoryStatistics.isEmpty()) {
                // Empty state: gesture handled by outer Box
                Spacer(modifier = Modifier.weight(1f))
            } else {
                // Scrollable category list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(uiState.isAllTimePeriod, uiState.selectedPeriodType) {
                            if (uiState.isAllTimePeriod) return@pointerInput
                            val swipeThresholdPx = 50.dp.toPx()
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onDragEnd = {
                                    if (totalDrag > swipeThresholdPx) {
                                        viewModel.navigatePeriod(-1)
                                    } else if (totalDrag < -swipeThresholdPx) {
                                        viewModel.navigatePeriod(1)
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    totalDrag += dragAmount
                                }
                            )
                        },
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 16.dp, bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.categoryStatistics) { stat ->
                        CategoryStatisticItem(
                            statistic = stat,
                            baseCurrency = baseCurrency,
                            onClick = {
                                val (startDate, endDate) = viewModel.getCurrentPeriodDateRange()
                                onCategoryClick(
                                    stat.category.id,
                                    uiState.selectedOperationType,
                                    startDate,
                                    endDate,
                                    uiState.isAllTimePeriod
                                )
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ExpenseIncomeToggle(
    selectedType: OperationType,
    onTypeSelected: (OperationType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .clickable { onTypeSelected(OperationType.EXPENSES) },
                color = if (selectedType == OperationType.EXPENSES) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.expenses_upper),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (selectedType == OperationType.EXPENSES) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .clickable { onTypeSelected(OperationType.INCOMES) },
                color = if (selectedType == OperationType.INCOMES) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.incomes_upper),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (selectedType == OperationType.INCOMES) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FixedPeriodTypeSelector(
    selectedPeriod: PeriodType,
    onPeriodSelected: (PeriodType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PeriodType.entries.forEach { period ->
            val isSelected = selectedPeriod == period

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPeriodSelected(period) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = stringResource(period.displayNameRes),
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 8.dp
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}


@Composable
private fun CategoryStatisticItem(
    statistic: CategoryStatistic,
    baseCurrency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Color(statistic.category.color.toColorInt())
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(statistic.category.iconName),
                    contentDescription = statistic.category.displayName(),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statistic.category.displayName(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", statistic.percentage)}%",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${String.format(Locale.US, "%.2f", statistic.totalAmount)} ${getCurrencySymbol(baseCurrency)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

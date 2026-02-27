package com.example.budgetcontrol.feature.main

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.budgetcontrol.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.core.domain.model.CategoryStatistic
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.ui.components.charts.PieChart
import com.example.budgetcontrol.ui.components.common.PeriodRangePicker
import com.example.budgetcontrol.core.theme.AppBlue
import com.example.budgetcontrol.ui.util.displayName
import com.example.budgetcontrol.ui.util.getCategoryIcon
import java.util.Calendar

@Composable
fun MainScreen(
    onAddExpenseClick: (Long) -> Unit,
    onAddIncomeClick: (Long) -> Unit,
    onExpensesListClick: () -> Unit,
    onCategoryClick: (String, OperationType) -> Unit = { _, _ -> },
    onSettingsClick: () -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {},
    viewModel: MainScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPeriodPicker by remember { mutableStateOf(false) }

    // Показываем период пикер
    if (showPeriodPicker) {
        PeriodRangePicker(
            onPeriodSelected = { startDate, endDate ->
                viewModel.selectCustomPeriod(startDate, endDate)
                showPeriodPicker = false
            },
            onDismiss = { showPeriodPicker = false }
        )
    }

    Scaffold(
        topBar = {
            // Верхняя панель с балансом
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppBlue
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
                            top = 60.dp, // УВЕЛИЧИЛИ отступ сверху для статус бара
                            bottom = 10.dp // УВЕЛИЧИЛИ отступ снизу
                        )
                ) {
                    Text(
                        text = "${String.format("%.2f", calculateBalance(uiState))} €",
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
                containerColor = AppBlue
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
        val isEmpty = uiState.categoryStatistics.isEmpty()
        var swipeCollapsed by remember { mutableStateOf(false) }

        val density = LocalDensity.current
        val fullHeight = 200.dp
        val ringFraction = 0.22f
        val collapsedBarHeight = fullHeight * ringFraction // 44dp — matches PieChart ring
        val maxCollapseOffsetPx = with(density) { (fullHeight - collapsedBarHeight).toPx() }
        var collapseOffsetPx by remember { mutableStateOf(0f) }

        val nestedScrollConnection = remember(maxCollapseOffsetPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (available.y < 0f) {
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
                        // Scrolling up + list at top → expand chart
                        val oldOffset = collapseOffsetPx
                        collapseOffsetPx = (collapseOffsetPx - available.y)
                            .coerceIn(0f, maxCollapseOffsetPx)
                        return Offset(0f, oldOffset - collapseOffsetPx)
                    }
                    return Offset.Zero
                }
            }
        }

        val scrollFraction = if (maxCollapseOffsetPx > 0f) {
            (collapseOffsetPx / maxCollapseOffsetPx).coerceIn(0f, 1f)
        } else 0f

        // Swipe for empty state is animated; scroll-driven is instant (follows finger)
        val swipeTarget = if (swipeCollapsed) 1f else 0f
        val animatedSwipeFraction by animateFloatAsState(
            targetValue = swipeTarget,
            animationSpec = tween(300),
            label = "swipe_collapse"
        )

        // Sync scroll offset when swipe overrides
        LaunchedEffect(swipeCollapsed) {
            if (swipeCollapsed) {
                collapseOffsetPx = maxCollapseOffsetPx
            } else {
                collapseOffsetPx = 0f
            }
        }

        val collapseFraction = maxOf(animatedSwipeFraction, scrollFraction)
        val chartHeight = lerp(fullHeight, collapsedBarHeight, collapseFraction)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Fixed header: toggle, period selector, chart
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
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
                    onNavigate = viewModel::navigatePeriod,
                    collapseFraction = collapseFraction,
                    chartHeight = chartHeight,
                    barHeight = collapsedBarHeight,
                    isEmpty = isEmpty,
                    onSwipeCollapse = { collapse -> swipeCollapsed = collapse }
                )
            }

            // Scrollable category list
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 16.dp, bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.categoryStatistics.isNotEmpty()) {
                    items(uiState.categoryStatistics) { stat ->
                        CategoryStatisticItem(
                            statistic = stat,
                            onClick = {
                                onCategoryClick(stat.category.id, uiState.selectedOperationType)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
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
                color = if (selectedType == OperationType.EXPENSES) AppBlue else Color.Transparent,
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
                color = if (selectedType == OperationType.INCOMES) AppBlue else Color.Transparent,
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
        PeriodType.values().forEach { period ->
            val isSelected = selectedPeriod == period

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPeriodSelected(period) },
                color = if (isSelected) AppBlue else Color(0xFFE0E0E0),
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
                    color = if (isSelected) Color.White else Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PeriodNavigationCard(
    uiState: MainScreenUiState,
    onNavigate: (Int) -> Unit,
    collapseFraction: Float = 0f,
    chartHeight: androidx.compose.ui.unit.Dp = 200.dp,
    barHeight: androidx.compose.ui.unit.Dp = 44.dp,
    isEmpty: Boolean = false,
    onSwipeCollapse: (Boolean) -> Unit = {}
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
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -20f) {
                        onSwipeCollapse(true)
                    } else if (dragAmount > 20f) {
                        onSwipeCollapse(false)
                    }
                }
            }
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
                    targetState = uiState.periodDisplayText to uiState.totalAmount,
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
                                text = "${String.format("%.2f", totalAmount)} €",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AppBlue
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
        statistics.map { stat ->
            val color = try {
                Color(android.graphics.Color.parseColor(stat.category.color))
            } catch (_: Exception) {
                Color.Gray
            }
            // Normalize so segments always fill the full bar width
            val fraction = if (totalPercentage > 0f) stat.percentage / totalPercentage else 0f
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
 * Проверка можно ли навигировать в будущий период
 */
private fun canNavigateToFuture(uiState: MainScreenUiState): Boolean {
    if (uiState.isAllTimePeriod) return false

    val calendar = Calendar.getInstance()
    val today = calendar.timeInMillis

    // Вычисляем следующий период
    val nextPeriodCalendar = Calendar.getInstance()

    return when (uiState.selectedPeriodType) {
        PeriodType.DAY -> {
            nextPeriodCalendar.add(Calendar.DAY_OF_MONTH, uiState.currentPeriodIndex + 1)
            // Нельзя идти в завтрашний день
            nextPeriodCalendar.timeInMillis <= today
        }

        PeriodType.WEEK -> {
            nextPeriodCalendar.add(Calendar.WEEK_OF_YEAR, uiState.currentPeriodIndex + 1)
            nextPeriodCalendar.set(Calendar.DAY_OF_WEEK, nextPeriodCalendar.firstDayOfWeek)
            // Нельзя идти в будущую неделю
            nextPeriodCalendar.timeInMillis <= today
        }

        PeriodType.MONTH -> {
            nextPeriodCalendar.add(Calendar.MONTH, uiState.currentPeriodIndex + 1)
            nextPeriodCalendar.set(Calendar.DAY_OF_MONTH, 1)
            // Нельзя идти в будущий месяц
            nextPeriodCalendar.get(Calendar.YEAR) < Calendar.getInstance().get(Calendar.YEAR) ||
                    (nextPeriodCalendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) &&
                            nextPeriodCalendar.get(Calendar.MONTH) <= Calendar.getInstance().get(Calendar.MONTH))
        }

        PeriodType.YEAR -> {
            nextPeriodCalendar.add(Calendar.YEAR, uiState.currentPeriodIndex + 1)
            // Нельзя идти в будущий год
            nextPeriodCalendar.get(Calendar.YEAR) <= Calendar.getInstance().get(Calendar.YEAR)
        }

        PeriodType.PERIOD -> false // Для кастомного периода навигация отключена
    }
}

@Composable
private fun CategoryStatisticItem(
    statistic: CategoryStatistic,
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
            // Иконка категории
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Color(android.graphics.Color.parseColor(statistic.category.color))
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

            // Информация о категории
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statistic.category.displayName(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Статистика
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format("%.1f", statistic.percentage)}%",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = AppBlue
                )
                Text(
                    text = "${String.format("%.2f", statistic.totalAmount)} €",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


// Функция расчета баланса
private fun calculateBalance(uiState: MainScreenUiState): Double {
    val totalIncomes = uiState.incomes.sumOf { it.amount }
    val totalExpenses = uiState.expenses.sumOf { it.amount }
    return totalIncomes - totalExpenses
}
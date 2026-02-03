package com.example.budgetcontrol.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.core.domain.model.CategoryStatistic
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.ui.components.charts.PieChart
import com.example.budgetcontrol.ui.components.common.PeriodRangePicker
import com.example.budgetcontrol.core.theme.AppBlue
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
                            contentDescription = "Настройки",
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
                    contentDescription = "Добавить операцию",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок с переключением доходы/расходы
            item {
                ExpenseIncomeToggle(
                    selectedType = uiState.selectedOperationType,
                    onTypeSelected = viewModel::selectOperationType
                )
            }

            // Фильтр по периодам
            item {
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
            }

            // Навигация по конкретным периодам с диаграммой
            item {
                PeriodNavigationCard(
                    uiState = uiState,
                    onNavigate = viewModel::navigatePeriod
                )
            }

            // Список категорий со статистикой
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

            // Отступ для FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
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
                    text = "РАСХОДЫ",
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
                    text = "ДОХОДЫ",
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
                    text = period.displayName,
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

@Composable
private fun PeriodNavigationCard(
    uiState: MainScreenUiState,
    onNavigate: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Навигация по периодам
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка "назад" - всегда показываем если не "За все время"
                if (uiState.isAllTimePeriod) {
                    Spacer(modifier = Modifier.size(48.dp))
                } else {
                    IconButton(onClick = { onNavigate(-1) }) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Предыдущий период"
                        )
                    }
                }

                Text(
                    text = uiState.periodDisplayText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                // Кнопка "вперед" - показываем только если можем идти в будущее
                if (uiState.isAllTimePeriod) {
                    Spacer(modifier = Modifier.size(48.dp))
                } else {
                    val canGoForward = canNavigateToFuture(uiState)
                    if (canGoForward) {
                        IconButton(onClick = { onNavigate(1) }) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Следующий период"
                            )
                        }
                    } else {
                        // Пустое место для сохранения центрирования
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Диаграмма или заглушка
            if (uiState.categoryStatistics.isNotEmpty()) {
                PieChart(
                    data = uiState.categoryStatistics,
                    totalAmount = uiState.totalAmount,
                    modifier = Modifier.size(200.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when {
                            uiState.isAllTimePeriod -> {
                                Text(
                                    text = if (uiState.selectedOperationType == OperationType.EXPENSES) {
                                        "Нет расходов\nза все время"
                                    } else {
                                        "Нет доходов\nза все время"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            uiState.selectedPeriodType == PeriodType.DAY -> {
                                if (uiState.currentPeriodIndex == 0) {
                                    Text(
                                        text = if (uiState.selectedOperationType == OperationType.EXPENSES) {
                                            "Сегодня\nрасходов\nне было"
                                        } else {
                                            "Сегодня\nдоходов\nне было"
                                        },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "Нет данных\nза этот день",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = "Нет данных\nза этот период",
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
                    contentDescription = statistic.category.name,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Информация о категории
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statistic.category.name,
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

@Composable
private fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "work" -> Icons.Default.Work
        "computer" -> Icons.Default.Computer
        "trending_up" -> Icons.Default.TrendingUp
        "card_giftcard" -> Icons.Default.CardGiftcard
        "sell" -> Icons.Default.Sell
        "shopping_cart" -> Icons.Default.ShoppingCart
        "directions_car" -> Icons.Default.DirectionsCar
        "movie" -> Icons.Default.Movie
        "local_hospital" -> Icons.Default.LocalHospital
        "home" -> Icons.Default.Home
        "subscriptions" -> Icons.Default.Subscriptions
        else -> Icons.Default.Category
    }
}

// Функция расчета баланса
private fun calculateBalance(uiState: MainScreenUiState): Double {
    val totalIncomes = uiState.incomes.sumOf { it.amount }
    val totalExpenses = uiState.expenses.sumOf { it.amount }
    return totalIncomes - totalExpenses
}
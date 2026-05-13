package com.example.budgetcontrol.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetcontrol.R
import com.example.budgetcontrol.ui.components.charts.PieChart
import com.example.budgetcontrol.core.domain.model.CategoryStatistic
import com.example.budgetcontrol.ui.util.displayName
import com.example.budgetcontrol.ui.util.getCategoryIcon
import com.example.budgetcontrol.core.util.formatAmount
import com.example.budgetcontrol.core.util.getCurrencySymbol
import androidx.core.graphics.toColorInt
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBackClick: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val baseCurrency by viewModel.baseCurrency.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.statistics),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Expenses / Incomes tabs
            item {
                TabRow(
                    selectedTabIndex = if (uiState.selectedTab == StatisticsTab.EXPENSES) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = uiState.selectedTab == StatisticsTab.EXPENSES,
                        onClick = { viewModel.selectTab(StatisticsTab.EXPENSES) },
                        text = {
                            Text(
                                text = stringResource(R.string.expense_tab),
                                fontWeight = if (uiState.selectedTab == StatisticsTab.EXPENSES) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = uiState.selectedTab == StatisticsTab.INCOMES,
                        onClick = { viewModel.selectTab(StatisticsTab.INCOMES) },
                        text = {
                            Text(
                                text = stringResource(R.string.income_tab),
                                fontWeight = if (uiState.selectedTab == StatisticsTab.INCOMES) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            item {
                PeriodSelector(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = viewModel::selectPeriod
                )
            }

            item {
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
                        Text(
                            text = stringResource(uiState.selectedPeriod.displayNameRes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Percent-mode toggle — only for Expenses tab when income data is available
                        if (uiState.selectedTab == StatisticsTab.EXPENSES && uiState.totalIncome > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = !uiState.showPercentOfIncome,
                                    onClick = { if (uiState.showPercentOfIncome) viewModel.togglePercentOfIncomeMode() },
                                    label = { Text(stringResource(R.string.percent_of_expenses)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = uiState.showPercentOfIncome,
                                    onClick = { if (!uiState.showPercentOfIncome) viewModel.togglePercentOfIncomeMode() },
                                    label = { Text(stringResource(R.string.percent_of_income)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        if (uiState.categoryStatistics.isNotEmpty()) {
                            PieChart(
                                data = uiState.categoryStatistics,
                                totalAmount = uiState.totalAmount,
                                baseCurrency = baseCurrency,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.no_data_for_selected_period),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(40.dp)
                            )
                        }
                    }
                }
            }

            val showPercentOfIncome = uiState.showPercentOfIncome &&
                uiState.selectedTab == StatisticsTab.EXPENSES &&
                uiState.totalIncome > 0
            items(uiState.categoryStatistics) { stat ->
                CategoryStatisticItem(
                    statistic = stat,
                    baseCurrency = baseCurrency,
                    showPercentOfIncome = showPercentOfIncome,
                    totalIncome = uiState.totalIncome
                )
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TimePeriod.entries.forEach { period ->
                FilterChip(
                    onClick = { onPeriodSelected(period) },
                    label = { Text(stringResource(period.displayNameRes)) },
                    selected = selectedPeriod == period,
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun CategoryStatisticItem(
    statistic: CategoryStatistic,
    baseCurrency: String,
    modifier: Modifier = Modifier,
    showPercentOfIncome: Boolean = false,
    totalIncome: Double = 0.0
) {
    val displayedPercentage = if (showPercentOfIncome && totalIncome > 0) {
        (statistic.totalAmount / totalIncome * 100).toFloat()
    } else {
        statistic.percentage
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
            val categoryColor = try {
                Color(statistic.category.color.toColorInt())
            } catch (_: Exception) {
                Color.Gray
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(categoryColor),
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${statistic.transactionCount}${stringResource(R.string.transactions_count_suffix)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(Locale.US, "%.0f", displayedPercentage)}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${formatAmount(statistic.totalAmount)} ${getCurrencySymbol(baseCurrency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

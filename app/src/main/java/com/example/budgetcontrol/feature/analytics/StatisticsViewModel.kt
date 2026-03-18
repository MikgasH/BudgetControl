package com.example.budgetcontrol.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.Expense
import com.example.budgetcontrol.core.domain.model.Income
import com.example.budgetcontrol.core.domain.usecase.GetCategoriesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetIncomesUseCase
import com.example.budgetcontrol.core.domain.usecase.calculateCategoryStatistics
import com.example.budgetcontrol.core.domain.model.CategoryStatistic
import androidx.annotation.StringRes
import com.example.budgetcontrol.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StatisticsTab { EXPENSES, INCOMES }

data class StatisticsUiState(
    val expenses: List<Expense> = emptyList(),
    val incomes: List<Income> = emptyList(),
    val categories: List<Category> = emptyList(),
    val categoryStatistics: List<CategoryStatistic> = emptyList(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = true,
    val selectedPeriod: TimePeriod = TimePeriod.THIS_MONTH,
    val selectedTab: StatisticsTab = StatisticsTab.EXPENSES
)

enum class TimePeriod(@StringRes val displayNameRes: Int) {
    THIS_WEEK(R.string.time_period_this_week),
    THIS_MONTH(R.string.time_period_this_month),
    THIS_YEAR(R.string.time_period_this_year),
    ALL_TIME(R.string.time_period_all_time)
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getIncomesUseCase: GetIncomesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var loadDataJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch {
            val currentState = _uiState.value
            val startTime = getStartTimeForPeriod(currentState.selectedPeriod)

            val expensesFlow = if (startTime != null) {
                getExpensesUseCase.getByDateRange(startTime, Long.MAX_VALUE)
            } else {
                getExpensesUseCase()
            }
            val incomesFlow = if (startTime != null) {
                getIncomesUseCase.getByDateRange(startTime, Long.MAX_VALUE)
            } else {
                getIncomesUseCase()
            }

            combine(
                expensesFlow,
                incomesFlow,
                getCategoriesUseCase()
            ) { expenses, incomes, categories ->
                val (stats, total) = when (currentState.selectedTab) {
                    StatisticsTab.EXPENSES -> {
                        val totalAmount = expenses.sumOf { it.amount }
                        val categoryStats = calculateCategoryStatistics(
                            expenses, { it.amount }, { it.categoryId }, categories
                        )
                        categoryStats to totalAmount
                    }
                    StatisticsTab.INCOMES -> {
                        val totalAmount = incomes.sumOf { it.amount }
                        val categoryStats = calculateCategoryStatistics(
                            incomes, { it.amount }, { it.categoryId }, categories
                        )
                        categoryStats to totalAmount
                    }
                }

                StatisticsUiState(
                    expenses = expenses,
                    incomes = incomes,
                    categories = categories,
                    categoryStatistics = stats,
                    totalAmount = total,
                    isLoading = false,
                    selectedPeriod = currentState.selectedPeriod,
                    selectedTab = currentState.selectedTab
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun getStartTimeForPeriod(period: TimePeriod): Long? {
        val calendar = java.util.Calendar.getInstance()
        return when (period) {
            TimePeriod.THIS_WEEK -> {
                calendar.apply {
                    set(java.util.Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                calendar.timeInMillis
            }
            TimePeriod.THIS_MONTH -> {
                calendar.apply {
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                calendar.timeInMillis
            }
            TimePeriod.THIS_YEAR -> {
                calendar.apply {
                    set(java.util.Calendar.DAY_OF_YEAR, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                calendar.timeInMillis
            }
            TimePeriod.ALL_TIME -> null
        }
    }

    fun selectPeriod(period: TimePeriod) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        loadData()
    }

    fun selectTab(tab: StatisticsTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        loadData()
    }
}

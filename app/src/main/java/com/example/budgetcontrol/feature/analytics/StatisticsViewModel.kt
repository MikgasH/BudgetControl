package com.example.budgetcontrol.feature.analytics

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.Expense
import com.example.budgetcontrol.core.domain.model.Income
import com.example.budgetcontrol.core.domain.usecase.GetCategoriesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetIncomesUseCase
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.util.DateRangeHelper
import com.example.budgetcontrol.core.util.SUBSCRIPTION_TIMEOUT_MS
import com.example.budgetcontrol.core.domain.model.PeriodType
import com.example.budgetcontrol.core.domain.usecase.calculateCategoryStatistics
import com.example.budgetcontrol.core.domain.model.CategoryStatistic
import androidx.annotation.StringRes
import com.example.budgetcontrol.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StatisticsTab { EXPENSES, INCOMES }

@Immutable
data class StatisticsUiState(
    val expenses: List<Expense> = emptyList(),
    val incomes: List<Income> = emptyList(),
    val categories: List<Category> = emptyList(),
    val categoryStatistics: List<CategoryStatistic> = emptyList(),
    val totalAmount: Double = 0.0,
    val totalIncome: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedPeriod: TimePeriod = TimePeriod.THIS_MONTH,
    val selectedTab: StatisticsTab = StatisticsTab.EXPENSES,
    val showPercentOfIncome: Boolean = false
)

enum class TimePeriod(@param:StringRes val displayNameRes: Int) {
    THIS_WEEK(R.string.time_period_this_week),
    THIS_MONTH(R.string.time_period_this_month),
    THIS_YEAR(R.string.time_period_this_year),
    ALL_TIME(R.string.time_period_all_time)
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getIncomesUseCase: GetIncomesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    preferencesManager: PreferencesManager
) : ViewModel() {

    val baseCurrency: StateFlow<String> = preferencesManager.baseCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), DEFAULT_BASE_CURRENCY)

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var loadDataJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        loadDataJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadDataJob = viewModelScope.launch {
            try {
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
                    Triple(expenses, incomes, categories)
                }.collect { (expenses, incomes, categories) ->
                    val totalIncome = incomes.sumOf { it.amount }

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

                    _uiState.update {
                        it.copy(
                            expenses = expenses,
                            incomes = incomes,
                            categories = categories,
                            categoryStatistics = stats,
                            totalAmount = total,
                            totalIncome = totalIncome,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    private fun getStartTimeForPeriod(period: TimePeriod): Long? {
        val periodType = when (period) {
            TimePeriod.THIS_WEEK -> PeriodType.WEEK
            TimePeriod.THIS_MONTH -> PeriodType.MONTH
            TimePeriod.THIS_YEAR -> PeriodType.YEAR
            TimePeriod.ALL_TIME -> return null
        }
        return DateRangeHelper.getDateRange(periodType).first
    }

    fun selectPeriod(period: TimePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadData()
    }

    fun selectTab(tab: StatisticsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        loadData()
    }

    fun togglePercentOfIncomeMode() {
        _uiState.update { it.copy(showPercentOfIncome = !it.showPercentOfIncome) }
    }
}

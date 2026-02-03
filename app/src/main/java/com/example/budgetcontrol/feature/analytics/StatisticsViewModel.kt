package com.example.budgetcontrol.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.Expense
import com.example.budgetcontrol.core.domain.usecase.GetCategoriesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.domain.model.CategoryStatistic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val categoryStatistics: List<CategoryStatistic> = emptyList(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = true,
    val selectedPeriod: TimePeriod = TimePeriod.THIS_MONTH
)

enum class TimePeriod(val displayName: String) {
    THIS_WEEK("Эта неделя"),
    THIS_MONTH("Этот месяц"),
    THIS_YEAR("Этот год"),
    ALL_TIME("Все время")
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                getExpensesUseCase(),
                getCategoriesUseCase()
            ) { expenses, categories ->
                val filteredExpenses = filterExpensesByPeriod(expenses, _uiState.value.selectedPeriod)
                val totalAmount = filteredExpenses.sumOf { it.amount }
                val categoryStats = calculateCategoryStatistics(filteredExpenses, categories, totalAmount)

                StatisticsUiState(
                    expenses = filteredExpenses,
                    categories = categories,
                    categoryStatistics = categoryStats,
                    totalAmount = totalAmount,
                    isLoading = false,
                    selectedPeriod = _uiState.value.selectedPeriod
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun filterExpensesByPeriod(expenses: List<Expense>, period: TimePeriod): List<Expense> {
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
                val weekStart = calendar.timeInMillis
                expenses.filter { it.date >= weekStart }
            }
            TimePeriod.THIS_MONTH -> {
                calendar.apply {
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val monthStart = calendar.timeInMillis
                expenses.filter { it.date >= monthStart }
            }
            TimePeriod.THIS_YEAR -> {
                calendar.apply {
                    set(java.util.Calendar.DAY_OF_YEAR, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val yearStart = calendar.timeInMillis
                expenses.filter { it.date >= yearStart }
            }
            TimePeriod.ALL_TIME -> expenses
        }
    }

    private fun calculateCategoryStatistics(
        expenses: List<Expense>,
        categories: List<Category>,
        totalAmount: Double
    ): List<CategoryStatistic> {
        if (totalAmount == 0.0) return emptyList()

        val expensesByCategory = expenses.groupBy { it.categoryId }

        return categories.mapNotNull { category ->
            val categoryExpenses = expensesByCategory[category.id] ?: emptyList()
            if (categoryExpenses.isEmpty()) return@mapNotNull null

            val categoryTotal = categoryExpenses.sumOf { it.amount }
            val percentage = ((categoryTotal / totalAmount) * 100).toFloat()

            CategoryStatistic(
                category = category,
                totalAmount = categoryTotal,
                percentage = percentage,
                expenseCount = categoryExpenses.size
            )
        }.sortedByDescending { it.totalAmount }
    }

    fun selectPeriod(period: TimePeriod) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        loadData()
    }
}
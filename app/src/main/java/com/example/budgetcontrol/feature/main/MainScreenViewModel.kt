package com.example.budgetcontrol.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.CategoryType
import com.example.budgetcontrol.core.domain.model.Expense
import com.example.budgetcontrol.core.domain.model.Income
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.toExpense
import com.example.budgetcontrol.core.domain.model.toIncome
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.domain.usecase.DeleteExpenseUseCase
import com.example.budgetcontrol.core.domain.usecase.DeleteIncomeUseCase
import com.example.budgetcontrol.core.domain.usecase.GetCategoriesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetIncomesUseCase
import com.example.budgetcontrol.core.util.DateRangeHelper
import com.example.budgetcontrol.core.domain.model.CategoryStatistic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class MainScreenUiState(
    val expenses: List<Expense> = emptyList(),
    val incomes: List<Income> = emptyList(),
    val transactions: List<Transaction> = emptyList(), // ДОБАВИЛИ список транзакций
    val categories: List<Category> = emptyList(),
    val categoryStatistics: List<CategoryStatistic> = emptyList(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = true,
    val selectedPeriodType: PeriodType = PeriodType.DAY,
    val selectedOperationType: OperationType = OperationType.EXPENSES,
    val currentPeriodIndex: Int = 0,
    val periodDisplayText: String = "",
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val isAllTimePeriod: Boolean = false
)

enum class PeriodType(val displayName: String) {
    DAY("День"),
    WEEK("Неделя"),
    MONTH("Месяц"),
    YEAR("Год"),
    PERIOD("Период")
}

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getIncomesUseCase: GetIncomesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase, // ДОБАВИЛИ
    private val deleteIncomeUseCase: DeleteIncomeUseCase   // ДОБАВИЛИ
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                getExpensesUseCase(),
                getIncomesUseCase(),
                getCategoriesUseCase()
            ) { expenses, incomes, categories ->
                val currentState = _uiState.value

                val filteredExpenses = filterExpensesByCurrentPeriod(expenses)
                val filteredIncomes = filterIncomesByCurrentPeriod(incomes)

                // Создаем список транзакций для текущего периода
                val currentTransactions = when (currentState.selectedOperationType) {
                    OperationType.EXPENSES -> filteredExpenses.map { it.toTransaction() }
                    OperationType.INCOMES -> filteredIncomes.map { it.toTransaction() }
                }.sortedByDescending { it.date }

                // Для статистики используем данные за период
                val (totalAmount, categoryStats) = when (currentState.selectedOperationType) {
                    OperationType.EXPENSES -> {
                        val total = filteredExpenses.sumOf { it.amount }
                        val stats = calculateCategoryStatistics(filteredExpenses, categories, total)
                        Pair(total, stats)
                    }
                    OperationType.INCOMES -> {
                        val total = filteredIncomes.sumOf { it.amount }
                        val stats = calculateIncomeCategoryStatistics(filteredIncomes, categories, total)
                        Pair(total, stats)
                    }
                }

                val periodText = getPeriodDisplayText()

                currentState.copy(
                    expenses = expenses, // Все расходы для расчета баланса
                    incomes = incomes,   // Все доходы для расчета баланса
                    transactions = currentTransactions, // ДОБАВИЛИ транзакции за период
                    categories = categories,
                    categoryStatistics = categoryStats,
                    totalAmount = totalAmount,
                    isLoading = false,
                    periodDisplayText = periodText
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    // ДОБАВИЛИ функцию удаления транзакции
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            when (transaction) {
                is Transaction.ExpenseTransaction -> {
                    deleteExpenseUseCase(transaction.toExpense())
                }
                is Transaction.IncomeTransaction -> {
                    deleteIncomeUseCase(transaction.toIncome())
                }
            }
        }
    }

    fun selectPeriodType(periodType: PeriodType) {
        _uiState.value = _uiState.value.copy(
            selectedPeriodType = periodType,
            currentPeriodIndex = 0,
            customStartDate = null,
            customEndDate = null,
            isAllTimePeriod = false
        )
        loadData()
    }

    fun selectOperationType(operationType: OperationType) {
        _uiState.value = _uiState.value.copy(selectedOperationType = operationType)
        loadData()
    }

    fun navigatePeriod(direction: Int) {
        val currentIndex = _uiState.value.currentPeriodIndex
        val newIndex = currentIndex + direction

        _uiState.value = _uiState.value.copy(currentPeriodIndex = newIndex)
        loadData()
    }

    fun selectCustomPeriod(startDate: Long, endDate: Long) {
        val isAllTime = DateRangeHelper.isAllTimePeriod(startDate, endDate)

        _uiState.value = _uiState.value.copy(
            selectedPeriodType = PeriodType.PERIOD,
            currentPeriodIndex = 0,
            customStartDate = startDate,
            customEndDate = endDate,
            isAllTimePeriod = isAllTime
        )
        loadData()
    }

    private fun filterExpensesByCurrentPeriod(expenses: List<Expense>): List<Expense> {
        val currentState = _uiState.value
        val (startDate, endDate) = DateRangeHelper.getDateRange(
            periodType = currentState.selectedPeriodType,
            periodOffset = currentState.currentPeriodIndex,
            customStartDate = currentState.customStartDate,
            customEndDate = currentState.customEndDate
        )

        return if (currentState.isAllTimePeriod) {
            expenses
        } else {
            expenses.filter { it.date in startDate..endDate }
        }
    }

    private fun filterIncomesByCurrentPeriod(incomes: List<Income>): List<Income> {
        val currentState = _uiState.value
        val (startDate, endDate) = DateRangeHelper.getDateRange(
            periodType = currentState.selectedPeriodType,
            periodOffset = currentState.currentPeriodIndex,
            customStartDate = currentState.customStartDate,
            customEndDate = currentState.customEndDate
        )

        return if (currentState.isAllTimePeriod) {
            incomes
        } else {
            incomes.filter { it.date in startDate..endDate }
        }
    }

    private fun getPeriodDisplayText(): String {
        val currentState = _uiState.value
        return DateRangeHelper.getPeriodDisplayText(
            periodType = currentState.selectedPeriodType,
            periodOffset = currentState.currentPeriodIndex,
            customStartDate = currentState.customStartDate,
            customEndDate = currentState.customEndDate,
            isAllTimePeriod = currentState.isAllTimePeriod
        )
    }

    private fun calculateCategoryStatistics(
        expenses: List<Expense>,
        categories: List<Category>,
        totalAmount: Double
    ): List<CategoryStatistic> {
        if (totalAmount == 0.0) return emptyList()

        val expensesByCategory = expenses.groupBy { it.categoryId }
        val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }

        return expenseCategories.mapNotNull { category ->
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

    private fun calculateIncomeCategoryStatistics(
        incomes: List<Income>,
        categories: List<Category>,
        totalAmount: Double
    ): List<CategoryStatistic> {
        if (totalAmount == 0.0) return emptyList()

        val incomesByCategory = incomes.groupBy { it.categoryId }
        val incomeCategories = categories.filter { it.type == CategoryType.INCOME }

        return incomeCategories.mapNotNull { category ->
            val categoryIncomes = incomesByCategory[category.id] ?: emptyList()
            if (categoryIncomes.isEmpty()) return@mapNotNull null

            val categoryTotal = categoryIncomes.sumOf { it.amount }
            val percentage = ((categoryTotal / totalAmount) * 100).toFloat()

            CategoryStatistic(
                category = category,
                totalAmount = categoryTotal,
                percentage = percentage,
                expenseCount = categoryIncomes.size
            )
        }.sortedByDescending { it.totalAmount }
    }

    fun getCategoryById(categoryId: String): Category? {
        return _uiState.value.categories.find { it.id == categoryId }
    }

    fun getCurrentSelectedDate(): Long {
        val currentState = _uiState.value
        return when (currentState.selectedPeriodType) {
            PeriodType.DAY -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_MONTH, currentState.currentPeriodIndex)
                calendar.timeInMillis
            }
            PeriodType.WEEK -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.WEEK_OF_YEAR, currentState.currentPeriodIndex)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.timeInMillis
            }
            PeriodType.MONTH -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, currentState.currentPeriodIndex)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.timeInMillis
            }
            PeriodType.YEAR -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.YEAR, currentState.currentPeriodIndex)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.timeInMillis
            }
            else -> System.currentTimeMillis()
        }
    }

    fun getCurrentSelectedOperationType(): OperationType {
        return _uiState.value.selectedOperationType
    }
}
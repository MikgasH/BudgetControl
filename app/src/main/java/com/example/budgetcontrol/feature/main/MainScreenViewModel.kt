package com.example.budgetcontrol.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.CategoryType
import com.example.budgetcontrol.core.domain.model.findById
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
import com.example.budgetcontrol.core.domain.usecase.GetAccountsUseCase
import com.example.budgetcontrol.core.domain.usecase.AccountWithBalance
import com.example.budgetcontrol.core.domain.usecase.calculateCategoryStatistics
import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.util.formatAmount
import com.example.budgetcontrol.core.util.getCurrencySymbol
import com.example.budgetcontrol.core.util.DateRangeHelper
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class MainScreenUiState(
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val categoryStatistics: List<CategoryStatistic> = emptyList(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedPeriodType: PeriodType = PeriodType.DAY,
    val selectedOperationType: OperationType = OperationType.EXPENSES,
    val currentPeriodIndex: Int = 0,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val isAllTimePeriod: Boolean = false,
    val accounts: List<AccountWithBalance> = emptyList(),
    val selectedAccountId: String? = null,
    val showAccountsSheet: Boolean = false,
    val showCreateEditAccountSheet: Boolean = false,
    val editingAccountId: String? = null,
    val editingAccountTransactionCount: Int = 0
)

enum class PeriodType(@StringRes val displayNameRes: Int) {
    DAY(R.string.period_day),
    WEEK(R.string.period_week),
    MONTH(R.string.period_month),
    YEAR(R.string.period_year),
    PERIOD(R.string.period_custom)
}

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getIncomesUseCase: GetIncomesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val deleteIncomeUseCase: DeleteIncomeUseCase,
    private val preferencesManager: PreferencesManager,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private val _accountsWithBalances = MutableStateFlow<List<AccountWithBalance>>(emptyList())

    val balance: StateFlow<Double> = combine(
        _accountsWithBalances,
        _uiState.map { it.selectedAccountId }
    ) { accounts, selectedId ->
        if (selectedId == null) {
            accounts.sumOf { it.currentBalance }
        } else {
            accounts.find { it.account.id == selectedId }?.currentBalance ?: 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val baseCurrency: StateFlow<String> = preferencesManager.baseCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_BASE_CURRENCY)

    private var loadDataJob: Job? = null

    init {
        getAccountsUseCase.getAccountsWithBalances()
            .onEach { accounts ->
                _accountsWithBalances.value = accounts
                _uiState.value = _uiState.value.copy(accounts = accounts)
            }.launchIn(viewModelScope)

        loadData()
    }

    private fun loadData() {
        // Cancel the previous combine collector — period/tab change starts a new one
        loadDataJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        loadDataJob = viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val accountId = currentState.selectedAccountId

                val dateRange = if (!currentState.isAllTimePeriod) {
                    DateRangeHelper.getDateRange(
                        periodType = currentState.selectedPeriodType,
                        periodOffset = currentState.currentPeriodIndex,
                        customStartDate = currentState.customStartDate,
                        customEndDate = currentState.customEndDate
                    )
                } else null

                val expensesFlow = when {
                    accountId != null && dateRange != null ->
                        getExpensesUseCase.getByAccountAndDateRange(accountId, dateRange.first, dateRange.second)
                    accountId != null ->
                        getExpensesUseCase.getByAccount(accountId)
                    dateRange != null ->
                        getExpensesUseCase.getByDateRange(dateRange.first, dateRange.second)
                    else ->
                        getExpensesUseCase()
                }
                val incomesFlow = when {
                    accountId != null && dateRange != null ->
                        getIncomesUseCase.getByAccountAndDateRange(accountId, dateRange.first, dateRange.second)
                    accountId != null ->
                        getIncomesUseCase.getByAccount(accountId)
                    dateRange != null ->
                        getIncomesUseCase.getByDateRange(dateRange.first, dateRange.second)
                    else ->
                        getIncomesUseCase()
                }

                combine(
                    expensesFlow,
                    incomesFlow,
                    getCategoriesUseCase()
                ) { expenses, incomes, categories ->
                    val currentTransactions = when (currentState.selectedOperationType) {
                        OperationType.EXPENSES -> expenses.map { it.toTransaction() }
                        OperationType.INCOMES -> incomes.map { it.toTransaction() }
                    }.sortedByDescending { it.date }

                    val (totalAmount, categoryStats) = when (currentState.selectedOperationType) {
                        OperationType.EXPENSES -> {
                            val total = expenses.sumOf { it.amount }
                            val stats = calculateCategoryStatistics(
                                expenses, { it.amount }, { it.categoryId },
                                categories.filter { it.type == CategoryType.EXPENSE }
                            )
                            Pair(total, stats)
                        }
                        OperationType.INCOMES -> {
                            val total = incomes.sumOf { it.amount }
                            val stats = calculateCategoryStatistics(
                                incomes, { it.amount }, { it.categoryId },
                                categories.filter { it.type == CategoryType.INCOME }
                            )
                            Pair(total, stats)
                        }
                    }

                    currentState.copy(
                        transactions = currentTransactions,
                        categories = categories,
                        categoryStatistics = categoryStats,
                        totalAmount = totalAmount,
                        isLoading = false,
                        error = null
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

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

    fun selectAllTime() {
        viewModelScope.launch {
            val expenseMin = getExpensesUseCase.getMinDate()
            val expenseMax = getExpensesUseCase.getMaxDate()
            val incomeMin = getIncomesUseCase.getMinDate()
            val incomeMax = getIncomesUseCase.getMaxDate()

            val allDates = listOfNotNull(expenseMin, incomeMin)
            val allEndDates = listOfNotNull(expenseMax, incomeMax)

            val now = Calendar.getInstance()
            val start = if (allDates.isNotEmpty()) allDates.min() else {
                Calendar.getInstance().apply {
                    set(now.get(Calendar.YEAR), 0, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            val end = if (allEndDates.isNotEmpty()) allEndDates.max() else {
                Calendar.getInstance().apply {
                    set(now.get(Calendar.YEAR), 11, 31, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
            }

            _uiState.value = _uiState.value.copy(
                selectedPeriodType = PeriodType.PERIOD,
                currentPeriodIndex = 0,
                customStartDate = start,
                customEndDate = end,
                isAllTimePeriod = true
            )
            loadData()
        }
    }

    fun getCategoryById(categoryId: String): Category? {
        return _uiState.value.categories.findById(categoryId)
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

    fun getCurrentPeriodDateRange(): Pair<Long, Long> {
        val currentState = _uiState.value
        return DateRangeHelper.getDateRange(
            periodType = currentState.selectedPeriodType,
            periodOffset = currentState.currentPeriodIndex,
            customStartDate = currentState.customStartDate,
            customEndDate = currentState.customEndDate
        )
    }

    fun getTopExpenseCategories(limit: Int = 3): List<Category> {
        return _uiState.value.categories
            .filter { it.type == CategoryType.EXPENSE && it.usageCount > 0 }
            .sortedByDescending { it.usageCount }
            .take(limit)
    }

    // ── Account management ────────────────────────────────────────────

    fun selectAccount(accountId: String?) {
        _uiState.value = _uiState.value.copy(selectedAccountId = accountId)
        loadData()
    }

    fun toggleAccountsSheet() {
        _uiState.value = _uiState.value.copy(
            showAccountsSheet = !_uiState.value.showAccountsSheet
        )
    }

    fun dismissAccountsSheet() {
        _uiState.value = _uiState.value.copy(showAccountsSheet = false)
    }

    fun showCreateAccountSheet() {
        _uiState.value = _uiState.value.copy(
            showCreateEditAccountSheet = true,
            editingAccountId = null,
            editingAccountTransactionCount = 0
        )
    }

    fun showEditAccountSheet(accountId: String) {
        viewModelScope.launch {
            val expenseCount = getExpensesUseCase.getExpenseCountByAccount(accountId)
            val incomeCount = getIncomesUseCase.getIncomeCountByAccount(accountId)
            _uiState.value = _uiState.value.copy(
                showCreateEditAccountSheet = true,
                editingAccountId = accountId,
                editingAccountTransactionCount = expenseCount + incomeCount
            )
        }
    }

    fun dismissCreateEditAccountSheet() {
        _uiState.value = _uiState.value.copy(
            showCreateEditAccountSheet = false,
            editingAccountId = null,
            editingAccountTransactionCount = 0
        )
    }

    fun createAccount(name: String, iconName: String, color: String, initialBalance: Double, currency: String) {
        viewModelScope.launch {
            val account = Account(
                id = UUID.randomUUID().toString(),
                name = name,
                iconName = iconName,
                color = color,
                initialBalance = initialBalance,
                currency = currency,
                isDefault = false,
                createdAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis(),
                sortOrder = 0
            )
            accountRepository.insertAccount(account)
            dismissCreateEditAccountSheet()
        }
    }

    fun updateAccount(name: String, iconName: String, color: String, initialBalance: Double, currency: String) {
        val accountId = _uiState.value.editingAccountId ?: return
        viewModelScope.launch {
            val existing = accountRepository.getAccountById(accountId) ?: return@launch
            val updated = existing.copy(
                name = name,
                iconName = iconName,
                color = color,
                initialBalance = initialBalance,
                currency = currency
            )
            accountRepository.updateAccount(updated)
            dismissCreateEditAccountSheet()
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            val account = accountRepository.getAccountById(accountId) ?: return@launch
            if (account.isDefault) return@launch

            // Reassign transactions to default account
            expenseRepository.reassignExpenses(accountId, Account.DEFAULT_ACCOUNT_ID)
            incomeRepository.reassignIncomes(accountId, Account.DEFAULT_ACCOUNT_ID)
            accountRepository.deleteAccount(account)

            // If the deleted account was selected, reset to all accounts
            if (_uiState.value.selectedAccountId == accountId) {
                _uiState.value = _uiState.value.copy(selectedAccountId = null)
                loadData()
            }
            dismissCreateEditAccountSheet()
        }
    }

    fun getEditingAccount(): Account? {
        val id = _uiState.value.editingAccountId ?: return null
        return _uiState.value.accounts.find { it.account.id == id }?.account
    }

    fun getSelectedAccountName(): String? {
        val id = _uiState.value.selectedAccountId ?: return null
        return _uiState.value.accounts.find { it.account.id == id }?.account?.name
    }

    fun getTotalBalance(): Double {
        return _accountsWithBalances.value.sumOf { it.currentBalance }
    }

    fun formatBalance(amount: Double): String {
        val symbol = getCurrencySymbol(baseCurrency.value)
        return "${formatAmount(amount)} $symbol"
    }
}
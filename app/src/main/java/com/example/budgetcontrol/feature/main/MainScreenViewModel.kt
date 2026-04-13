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
import com.example.budgetcontrol.core.domain.usecase.AccountGroupWithBalance
import com.example.budgetcontrol.core.domain.usecase.AccountWithBalance
import com.example.budgetcontrol.core.domain.usecase.calculateCategoryStatistics
import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.model.AccountGroup
import com.example.budgetcontrol.core.domain.repository.AccountGroupRepository
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.di.ApplicationScope
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.util.formatAmount
import com.example.budgetcontrol.core.util.getCurrencySymbol
import com.example.budgetcontrol.core.util.DateRangeHelper
import com.example.budgetcontrol.core.domain.model.CategoryStatistic
import androidx.annotation.StringRes
import com.example.budgetcontrol.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
    val editingAccountTransactionCount: Int = 0,
    val accountGroups: List<AccountGroupWithBalance> = emptyList(),
    val selectedGroupId: String? = null,
    val showCreateEditGroupSheet: Boolean = false,
    val editingGroupId: String? = null,
    val availableCurrencies: List<String> = emptyList(),
    val favoriteCurrencies: Set<String> = emptySet(),
    val isCurrenciesLoading: Boolean = false
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
    private val incomeRepository: IncomeRepository,
    private val accountGroupRepository: AccountGroupRepository,
    private val cerpsRepository: CerpsRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private val _accountsWithBalances = MutableStateFlow<List<AccountWithBalance>>(emptyList())
    private val _groups = MutableStateFlow<List<AccountGroup>>(emptyList())

    val baseCurrency: StateFlow<String> = preferencesManager.baseCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_BASE_CURRENCY)

    private val _cachedRates = MutableStateFlow<Map<String, Double>>(emptyMap())

    /** Balance amount in the appropriate display currency. */
    val balance: StateFlow<Double> = combine(
        _accountsWithBalances,
        _groups,
        _uiState.map { Pair(it.selectedAccountId, it.selectedGroupId) }.distinctUntilChanged(),
        _cachedRates,
        baseCurrency
    ) { accounts, groups, (selectedId, selectedGroupId), rates, baseCur ->
        when {
            selectedGroupId != null -> {
                val group = groups.find { it.id == selectedGroupId }
                val memberIds = group?.memberAccountIds ?: emptyList()
                accounts.filter { it.account.id in memberIds }
                    .sumOf { convertToBaseCurrency(it, baseCur, rates) }
            }
            selectedId != null -> {
                // Single account: balance stays in account's own currency
                accounts.find { it.account.id == selectedId }?.currentBalance ?: 0.0
            }
            else -> accounts.sumOf { convertToBaseCurrency(it, baseCur, rates) }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    /** Currency code to display alongside the balance. */
    val displayCurrency: StateFlow<String> = combine(
        _accountsWithBalances,
        _uiState.map { it.selectedAccountId }.distinctUntilChanged(),
        baseCurrency
    ) { accounts, selectedId, baseCur ->
        if (selectedId != null) {
            accounts.find { it.account.id == selectedId }?.account?.currency ?: baseCur
        } else baseCur
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_BASE_CURRENCY)

    /** True when the total balance is approximate (mixed currencies among displayed accounts). */
    val isApproximateBalance: StateFlow<Boolean> = combine(
        _accountsWithBalances,
        _groups,
        _uiState.map { Pair(it.selectedAccountId, it.selectedGroupId) }.distinctUntilChanged(),
        baseCurrency
    ) { accounts, groups, (selectedId, selectedGroupId), baseCur ->
        when {
            selectedId != null -> false   // single account — exact
            selectedGroupId != null -> {
                val group = groups.find { it.id == selectedGroupId }
                val memberIds = group?.memberAccountIds ?: emptyList()
                accounts.filter { it.account.id in memberIds }
                    .any { it.account.currency != baseCur }
            }
            else -> accounts.any { it.account.currency != baseCur }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var loadDataJob: Job? = null

    init {
        viewModelScope.launch {
            val savedAccountId = preferencesManager.selectedAccountIdFlow.first()
            val savedGroupId = preferencesManager.selectedGroupIdFlow.first()
            _uiState.update {
                it.copy(
                    selectedAccountId = if (savedGroupId == null) savedAccountId else null,
                    selectedGroupId = savedGroupId
                )
            }

            getAccountsUseCase.getAccountsWithBalances()
                .onEach { accounts ->
                    _accountsWithBalances.value = accounts
                    _uiState.update { it.copy(accounts = accounts) }
                    updateGroupBalances()
                }.launchIn(viewModelScope)

            accountGroupRepository.getAllGroups()
                .onEach { groups ->
                    _groups.value = groups
                    updateGroupBalances()
                }.launchIn(viewModelScope)

            preferencesManager.getLastRates()
                .onEach { rates -> _cachedRates.value = rates }
                .launchIn(viewModelScope)

            preferencesManager.favoriteCurrenciesFlow
                .onEach { favs -> _uiState.update { it.copy(favoriteCurrencies = favs) } }
                .launchIn(viewModelScope)

            loadCurrencies()
            loadData()
        }
    }

    private fun updateGroupBalances() {
        val accounts = _accountsWithBalances.value
        val groups = _groups.value
        val baseCur = baseCurrency.value
        val rates = _cachedRates.value
        val groupsWithBalance = groups.map { group ->
            val memberBalances = accounts
                .filter { it.account.id in group.memberAccountIds }
            AccountGroupWithBalance(
                group = group,
                combinedBalance = memberBalances.sumOf { convertToBaseCurrency(it, baseCur, rates) },
                memberCount = group.memberAccountIds.size
            )
        }
        _uiState.update { it.copy(accountGroups = groupsWithBalance) }
    }

    private fun loadData() {
        // Cancel the previous combine collector — period/tab change starts a new one
        loadDataJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadDataJob = viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val accountId = currentState.selectedAccountId
                val groupId = currentState.selectedGroupId
                val groupMemberIds = if (groupId != null) {
                    currentState.accountGroups.find { it.group.id == groupId }
                        ?.group?.memberAccountIds ?: emptyList()
                } else emptyList()

                val dateRange = if (!currentState.isAllTimePeriod) {
                    DateRangeHelper.getDateRange(
                        periodType = currentState.selectedPeriodType,
                        periodOffset = currentState.currentPeriodIndex,
                        customStartDate = currentState.customStartDate,
                        customEndDate = currentState.customEndDate
                    )
                } else null

                val expensesFlow = when {
                    groupId != null && groupMemberIds.isNotEmpty() -> {
                        val flows = groupMemberIds.map { memberId ->
                            if (dateRange != null) getExpensesUseCase.getByAccountAndDateRange(memberId, dateRange.first, dateRange.second)
                            else getExpensesUseCase.getByAccount(memberId)
                        }
                        combine(flows) { arrays -> arrays.flatMap { it.toList() } }
                    }
                    groupId != null -> flowOf(emptyList())
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
                    groupId != null && groupMemberIds.isNotEmpty() -> {
                        val flows = groupMemberIds.map { memberId ->
                            if (dateRange != null) getIncomesUseCase.getByAccountAndDateRange(memberId, dateRange.first, dateRange.second)
                            else getIncomesUseCase.getByAccount(memberId)
                        }
                        combine(flows) { arrays -> arrays.flatMap { it.toList() } }
                    }
                    groupId != null -> flowOf(emptyList())
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

                    Triple(currentTransactions, categories, Pair(totalAmount, categoryStats))
                }.collect { (transactions, categories, totalAndStats) ->
                    _uiState.update {
                        it.copy(
                            transactions = transactions,
                            categories = categories,
                            categoryStatistics = totalAndStats.second,
                            totalAmount = totalAndStats.first,
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
        _uiState.update {
            it.copy(
                selectedPeriodType = periodType,
                currentPeriodIndex = 0,
                customStartDate = null,
                customEndDate = null,
                isAllTimePeriod = false
            )
        }
        loadData()
    }

    fun selectOperationType(operationType: OperationType) {
        _uiState.update { it.copy(selectedOperationType = operationType) }
        loadData()
    }

    fun navigatePeriod(direction: Int) {
        val currentIndex = _uiState.value.currentPeriodIndex
        val newIndex = currentIndex + direction

        _uiState.update { it.copy(currentPeriodIndex = newIndex) }
        loadData()
    }

    fun selectCustomPeriod(startDate: Long, endDate: Long) {
        val isAllTime = DateRangeHelper.isAllTimePeriod(startDate, endDate)

        _uiState.update {
            it.copy(
                selectedPeriodType = PeriodType.PERIOD,
                currentPeriodIndex = 0,
                customStartDate = startDate,
                customEndDate = endDate,
                isAllTimePeriod = isAllTime
            )
        }
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

            _uiState.update {
                it.copy(
                    selectedPeriodType = PeriodType.PERIOD,
                    currentPeriodIndex = 0,
                    customStartDate = start,
                    customEndDate = end,
                    isAllTimePeriod = true
                )
            }
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

    private fun loadCurrencies() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCurrenciesLoading = true) }
            when (val result = cerpsRepository.getCurrencies()) {
                is CerpsResult.Success -> {
                    _uiState.update {
                        it.copy(availableCurrencies = result.data, isCurrenciesLoading = false)
                    }
                }
                is CerpsResult.Error -> {
                    _uiState.update { it.copy(isCurrenciesLoading = false) }
                }
            }
        }
    }

    // ── Account management ────────────────────────────────────────────

    fun selectAccount(accountId: String?) {
        _uiState.update {
            it.copy(
                selectedAccountId = accountId,
                selectedGroupId = null
            )
        }
        // Persist on applicationScope so the write survives ViewModel cancellation when
        // the user exits the app right after tapping an account.
        applicationScope.launch {
            preferencesManager.setSelectedAccountId(accountId)
            preferencesManager.setSelectedGroupId(null)
        }
        loadData()
    }

    fun toggleAccountsSheet() {
        _uiState.update { it.copy(showAccountsSheet = !it.showAccountsSheet) }
    }

    fun dismissAccountsSheet() {
        _uiState.update { it.copy(showAccountsSheet = false) }
    }

    fun showCreateAccountSheet() {
        _uiState.update {
            it.copy(
                showCreateEditAccountSheet = true,
                editingAccountId = null,
                editingAccountTransactionCount = 0
            )
        }
    }

    fun showEditAccountSheet(accountId: String) {
        viewModelScope.launch {
            val expenseCount = getExpensesUseCase.getExpenseCountByAccount(accountId)
            val incomeCount = getIncomesUseCase.getIncomeCountByAccount(accountId)
            _uiState.update {
                it.copy(
                    showCreateEditAccountSheet = true,
                    editingAccountId = accountId,
                    editingAccountTransactionCount = expenseCount + incomeCount
                )
            }
        }
    }

    fun dismissCreateEditAccountSheet() {
        _uiState.update {
            it.copy(
                showCreateEditAccountSheet = false,
                editingAccountId = null,
                editingAccountTransactionCount = 0
            )
        }
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
                _uiState.update { it.copy(selectedAccountId = null) }
                applicationScope.launch { preferencesManager.setSelectedAccountId(null) }
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
        val state = _uiState.value
        if (state.selectedGroupId != null) {
            return state.accountGroups.find { it.group.id == state.selectedGroupId }?.group?.name
        }
        val id = state.selectedAccountId ?: return null
        return state.accounts.find { it.account.id == id }?.account?.name
    }

    fun getTotalBalance(): Double {
        val baseCur = baseCurrency.value
        val rates = _cachedRates.value
        return _accountsWithBalances.value.sumOf { convertToBaseCurrency(it, baseCur, rates) }
    }

    fun hasMixedCurrencies(): Boolean {
        val baseCur = baseCurrency.value
        return _accountsWithBalances.value.any { it.account.currency != baseCur }
    }

    fun formatBalance(amount: Double, currency: String, isApproximate: Boolean): String {
        val symbol = getCurrencySymbol(currency)
        val prefix = if (isApproximate) "~" else ""
        return "$prefix${formatAmount(amount)} $symbol"
    }

    private fun convertToBaseCurrency(
        accountWithBalance: AccountWithBalance,
        baseCur: String,
        rates: Map<String, Double>
    ): Double {
        val acctCurrency = accountWithBalance.account.currency
        if (acctCurrency == baseCur) return accountWithBalance.currentBalance
        // rates are EUR-based: rate[X] = how many X per 1 EUR
        val acctRate = rates[acctCurrency] ?: return accountWithBalance.currentBalance
        val baseRate = if (baseCur == "EUR") 1.0 else (rates[baseCur] ?: return accountWithBalance.currentBalance)
        return accountWithBalance.currentBalance * baseRate / acctRate
    }

    // ── Group management ───────────────────────────────────────────────

    fun selectGroup(groupId: String) {
        _uiState.update {
            it.copy(
                selectedGroupId = groupId,
                selectedAccountId = null
            )
        }
        // Persist on applicationScope so the write survives ViewModel cancellation when
        // the user exits the app right after tapping a group.
        applicationScope.launch {
            preferencesManager.setSelectedGroupId(groupId)
            preferencesManager.setSelectedAccountId(null)
        }
        loadData()
    }

    fun showCreateGroupSheet() {
        _uiState.update {
            it.copy(showCreateEditGroupSheet = true, editingGroupId = null)
        }
    }

    fun showEditGroupSheet(groupId: String) {
        _uiState.update {
            it.copy(showCreateEditGroupSheet = true, editingGroupId = groupId)
        }
    }

    fun dismissCreateEditGroupSheet() {
        _uiState.update {
            it.copy(showCreateEditGroupSheet = false, editingGroupId = null)
        }
    }

    fun createGroup(name: String, memberAccountIds: List<String>) {
        viewModelScope.launch {
            val group = AccountGroup(
                id = UUID.randomUUID().toString(),
                name = name,
                memberAccountIds = memberAccountIds,
                createdAt = System.currentTimeMillis()
            )
            accountGroupRepository.insertGroup(group)
            dismissCreateEditGroupSheet()
        }
    }

    fun updateGroup(name: String, memberAccountIds: List<String>) {
        val groupId = _uiState.value.editingGroupId ?: return
        viewModelScope.launch {
            val existing = accountGroupRepository.getGroupById(groupId) ?: return@launch
            val updated = existing.copy(
                name = name,
                memberAccountIds = memberAccountIds
            )
            accountGroupRepository.updateGroup(updated)
            dismissCreateEditGroupSheet()
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            val group = accountGroupRepository.getGroupById(groupId) ?: return@launch
            accountGroupRepository.deleteGroup(group)

            if (_uiState.value.selectedGroupId == groupId) {
                _uiState.update { it.copy(selectedGroupId = null) }
                applicationScope.launch { preferencesManager.setSelectedGroupId(null) }
                loadData()
            }
            dismissCreateEditGroupSheet()
        }
    }

    fun getEditingGroup(): AccountGroup? {
        val id = _uiState.value.editingGroupId ?: return null
        return _uiState.value.accountGroups.find { it.group.id == id }?.group
    }

    fun addAccountToGroup(accountId: String, groupId: String) {
        viewModelScope.launch {
            val group = accountGroupRepository.getGroupById(groupId) ?: return@launch
            if (accountId !in group.memberAccountIds) {
                val updated = group.copy(
                    memberAccountIds = group.memberAccountIds + accountId
                )
                accountGroupRepository.updateGroup(updated)
            }
        }
    }

    fun getSelectedGroupMemberIds(): List<String> {
        val groupId = _uiState.value.selectedGroupId ?: return emptyList()
        return _uiState.value.accountGroups
            .find { it.group.id == groupId }
            ?.group?.memberAccountIds ?: emptyList()
    }

    fun getGroupMemberAccounts(): List<AccountWithBalance> {
        val memberIds = getSelectedGroupMemberIds()
        return _uiState.value.accounts.filter { it.account.id in memberIds }
    }
}
package com.example.budgetcontrol.feature.transaction.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.model.AccountGroup
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.findById
import com.example.budgetcontrol.core.domain.model.toExpense
import com.example.budgetcontrol.core.domain.model.toIncome
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.domain.repository.AccountGroupRepository
import com.example.budgetcontrol.core.domain.usecase.AccountWithBalance
import com.example.budgetcontrol.core.domain.usecase.DeleteExpenseUseCase
import com.example.budgetcontrol.core.domain.usecase.DeleteIncomeUseCase
import com.example.budgetcontrol.core.domain.usecase.GetAccountsUseCase
import com.example.budgetcontrol.core.domain.usecase.GetCategoriesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetIncomesUseCase
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.util.SUBSCRIPTION_TIMEOUT_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TransactionTypeFilter { ALL, INCOME, EXPENSE }

@Immutable
data class UnifiedTransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val accountGroups: List<AccountGroup> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: String? = null,
    val selectedGroupId: String? = null,
    val selectedCategoryIds: Set<String> = emptySet(),
    val transactionTypeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isLoading: Boolean = true
)

@Immutable
private data class FilterParams(
    val accountId: String?,
    val groupId: String?,
    val categoryIds: Set<String>,
    val typeFilter: TransactionTypeFilter
)

@HiltViewModel
class UnifiedTransactionListViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getIncomesUseCase: GetIncomesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val accountGroupRepository: AccountGroupRepository,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val deleteIncomeUseCase: DeleteIncomeUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    private val _selectedGroupId = MutableStateFlow<String?>(null)
    private val _selectedCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    private val _transactionTypeFilter = MutableStateFlow(TransactionTypeFilter.ALL)
    private val _startDate = MutableStateFlow<Long?>(null)
    private val _endDate = MutableStateFlow<Long?>(null)

    val baseCurrency: StateFlow<String> = preferencesManager.baseCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), DEFAULT_BASE_CURRENCY)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UnifiedTransactionListUiState> = combine(
        _startDate,
        _endDate
    ) { start, end ->
        if (start != null && end != null) start to end else null
    }.distinctUntilChanged().flatMapLatest { dateRange ->
        val expensesFlow = if (dateRange != null) {
            getExpensesUseCase.getByDateRange(dateRange.first, dateRange.second)
        } else {
            getExpensesUseCase()
        }
        val incomesFlow = if (dateRange != null) {
            getIncomesUseCase.getByDateRange(dateRange.first, dateRange.second)
        } else {
            getIncomesUseCase()
        }
        combine(
            combine(expensesFlow, incomesFlow, getCategoriesUseCase()) { expenses, incomes, categories ->
                Triple(expenses, incomes, categories)
            },
            combine(
                _selectedAccountId,
                _selectedGroupId,
                _selectedCategoryIds,
                _transactionTypeFilter
            ) { accountId, groupId, categoryIds, typeFilter ->
                FilterParams(accountId, groupId, categoryIds, typeFilter)
            },
            getAccountsUseCase.getAccountsWithBalances(
                baseCurrencyFlow = preferencesManager.baseCurrencyFlow,
                ratesFlow = preferencesManager.getLastRates()
            ),
            accountGroupRepository.getAllGroups()
        ) { (expenses, incomes, categories), filters, accounts, groups ->
            val allTransactions = buildList {
                addAll(expenses.map { it.toTransaction() })
                addAll(incomes.map { it.toTransaction() })
            }

            // Resolve group member IDs once per recomputation. A missing group (e.g. deleted
            // while still selected) yields an empty set → no transactions match.
            val groupMemberIds: Set<String>? = filters.groupId?.let { gid ->
                groups.find { it.id == gid }?.memberAccountIds?.toSet() ?: emptySet()
            }

            val filtered = allTransactions
                .filter { tx ->
                    val txAccountId = when (tx) {
                        is Transaction.ExpenseTransaction -> tx.accountId
                        is Transaction.IncomeTransaction -> tx.accountId
                    }
                    val accountMatch = when {
                        groupMemberIds != null -> txAccountId != null && txAccountId in groupMemberIds
                        filters.accountId != null -> txAccountId == filters.accountId
                        else -> true
                    }
                    val categoryMatch = filters.categoryIds.isEmpty() || tx.categoryId in filters.categoryIds
                    val typeMatch = when (filters.typeFilter) {
                        TransactionTypeFilter.ALL -> true
                        TransactionTypeFilter.INCOME -> tx is Transaction.IncomeTransaction
                        TransactionTypeFilter.EXPENSE -> tx is Transaction.ExpenseTransaction
                    }
                    accountMatch && categoryMatch && typeMatch
                }
                .sortedByDescending { it.date }

            UnifiedTransactionListUiState(
                transactions = filtered,
                accounts = accounts,
                accountGroups = groups,
                categories = categories,
                selectedAccountId = filters.accountId,
                selectedGroupId = filters.groupId,
                selectedCategoryIds = filters.categoryIds,
                transactionTypeFilter = filters.typeFilter,
                startDate = dateRange?.first,
                endDate = dateRange?.second,
                isLoading = false
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        UnifiedTransactionListUiState()
    )

    fun setAccount(accountId: String?) {
        _selectedAccountId.value = accountId
        _selectedGroupId.value = null
    }

    fun setGroup(groupId: String?) {
        _selectedGroupId.value = groupId
        _selectedAccountId.value = null
    }

    fun clearAccountFilter() {
        _selectedAccountId.value = null
        _selectedGroupId.value = null
    }

    fun toggleCategory(categoryId: String) {
        val current = _selectedCategoryIds.value
        _selectedCategoryIds.value = if (categoryId in current) current - categoryId else current + categoryId
    }

    fun clearCategories() {
        _selectedCategoryIds.value = emptySet()
    }

    fun setTransactionTypeFilter(filter: TransactionTypeFilter) {
        _transactionTypeFilter.value = filter
    }

    fun setDateRange(startDate: Long, endDate: Long) {
        _startDate.value = startDate
        _endDate.value = endDate
    }

    fun clearDateRange() {
        _startDate.value = null
        _endDate.value = null
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            when (transaction) {
                is Transaction.ExpenseTransaction -> deleteExpenseUseCase(transaction.toExpense())
                is Transaction.IncomeTransaction -> deleteIncomeUseCase(transaction.toIncome())
            }
        }
    }

    fun getCategoryById(categoryId: String): Category? {
        return uiState.value.categories.findById(categoryId)
    }
}

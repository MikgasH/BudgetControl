package com.example.budgetcontrol.feature.transaction.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.findById
import com.example.budgetcontrol.core.domain.model.toExpense
import com.example.budgetcontrol.core.domain.model.toIncome
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.domain.usecase.AccountWithBalance
import com.example.budgetcontrol.core.domain.usecase.DeleteExpenseUseCase
import com.example.budgetcontrol.core.domain.usecase.DeleteIncomeUseCase
import com.example.budgetcontrol.core.domain.usecase.GetAccountsUseCase
import com.example.budgetcontrol.core.domain.usecase.GetCategoriesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetIncomesUseCase
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
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

data class UnifiedTransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: String? = null,
    val selectedCategoryIds: Set<String> = emptySet(),
    val transactionTypeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isLoading: Boolean = true
)

private data class FilterParams(
    val accountId: String?,
    val categoryIds: Set<String>,
    val typeFilter: TransactionTypeFilter
)

@HiltViewModel
class UnifiedTransactionListViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getIncomesUseCase: GetIncomesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val deleteIncomeUseCase: DeleteIncomeUseCase,
    preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    private val _selectedCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    private val _transactionTypeFilter = MutableStateFlow(TransactionTypeFilter.ALL)
    private val _startDate = MutableStateFlow<Long?>(null)
    private val _endDate = MutableStateFlow<Long?>(null)

    val baseCurrency: StateFlow<String> = preferencesManager.baseCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_BASE_CURRENCY)

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
                _selectedCategoryIds,
                _transactionTypeFilter
            ) { accountId, categoryIds, typeFilter ->
                FilterParams(accountId, categoryIds, typeFilter)
            },
            getAccountsUseCase.getAccountsWithBalances()
        ) { (expenses, incomes, categories), filters, accounts ->
            val allTransactions = buildList {
                addAll(expenses.map { it.toTransaction() })
                addAll(incomes.map { it.toTransaction() })
            }

            val filtered = allTransactions
                .filter { tx ->
                    val accountMatch = filters.accountId == null || when (tx) {
                        is Transaction.ExpenseTransaction -> tx.accountId == filters.accountId
                        is Transaction.IncomeTransaction -> tx.accountId == filters.accountId
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
                categories = categories,
                selectedAccountId = filters.accountId,
                selectedCategoryIds = filters.categoryIds,
                transactionTypeFilter = filters.typeFilter,
                startDate = dateRange?.first,
                endDate = dateRange?.second,
                isLoading = false
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UnifiedTransactionListUiState()
    )

    fun setAccount(accountId: String?) {
        _selectedAccountId.value = accountId
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

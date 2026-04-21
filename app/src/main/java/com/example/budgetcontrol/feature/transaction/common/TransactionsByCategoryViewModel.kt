package com.example.budgetcontrol.feature.transaction.common

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetIncomesUseCase
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.util.SUBSCRIPTION_TIMEOUT_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class TransactionsByCategoryUiState(
    val transactions: List<Transaction> = emptyList(),
    val category: Category? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = true,
    val showError: String? = null,
    val accountNames: Map<String, String> = emptyMap()
)

@HiltViewModel
class TransactionsByCategoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getIncomesUseCase: GetIncomesUseCase,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    preferencesManager: PreferencesManager
) : ViewModel() {

    val baseCurrency: StateFlow<String> = preferencesManager.baseCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), DEFAULT_BASE_CURRENCY)

    private val _uiState = MutableStateFlow(TransactionsByCategoryUiState())
    val uiState: StateFlow<TransactionsByCategoryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadTransactions(
        categoryId: String,
        transactionType: TransactionType,
        startDate: Long? = null,
        endDate: Long? = null,
        accountId: String? = null
    ) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                transactionType = transactionType,
                isLoading = true
            )

            try {
                val category = categoryRepository.getCategoryById(categoryId)

                // Load account names for display
                val accounts = accountRepository.getAllAccountsList()
                val accountNames = accounts.associate { it.id to it.name }

                val flow = when (transactionType) {
                    TransactionType.EXPENSE -> getExpensesUseCase.getByCategory(categoryId)
                        .mapTransactions(startDate, endDate, accountId) { it.toTransaction() }
                    TransactionType.INCOME -> getIncomesUseCase.getByCategory(categoryId)
                        .mapTransactions(startDate, endDate, accountId) { it.toTransaction() }
                }

                flow.collect { transactions ->
                    _uiState.value = TransactionsByCategoryUiState(
                        transactions = transactions,
                        category = category,
                        transactionType = transactionType,
                        totalAmount = transactions.sumOf { it.amount },
                        isLoading = false,
                        showError = if (category == null) context.getString(R.string.category_not_found) else null,
                        accountNames = accountNames
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = context.getString(R.string.error_loading, e.message ?: "")
                )
            }
        }
    }

    private fun <T> Flow<List<T>>.mapTransactions(
        startDate: Long?,
        endDate: Long?,
        accountId: String?,
        toTransaction: (T) -> Transaction
    ): Flow<List<Transaction>> = map { items ->
        var filtered = items.map(toTransaction)
        if (startDate != null && endDate != null) {
            filtered = filtered.filter { it.date in startDate..endDate }
        }
        if (accountId != null) {
            val accountIds = accountId.split(",").filter { it.isNotBlank() }.toSet()
            filtered = filtered.filter { txn ->
                val txnAccountId = when (txn) {
                    is Transaction.ExpenseTransaction -> txn.accountId ?: Account.DEFAULT_ACCOUNT_ID
                    is Transaction.IncomeTransaction -> txn.accountId ?: Account.DEFAULT_ACCOUNT_ID
                }
                txnAccountId in accountIds
            }
        }
        filtered.sortedByDescending { it.date }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(showError = null)
    }
}

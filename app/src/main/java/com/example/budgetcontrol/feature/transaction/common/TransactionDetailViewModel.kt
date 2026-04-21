package com.example.budgetcontrol.feature.transaction.common

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.domain.model.toExpense
import com.example.budgetcontrol.core.domain.model.toIncome
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val category: Category? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val showError: String? = null
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val categoryRepository: CategoryRepository,
    preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    val baseCurrency: StateFlow<String> = preferencesManager.baseCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_BASE_CURRENCY)

    fun loadTransaction(transactionId: String, transactionType: TransactionType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val transaction = when (transactionType) {
                    TransactionType.EXPENSE -> {
                        expenseRepository.getExpenseById(transactionId)?.toTransaction()
                    }
                    TransactionType.INCOME -> {
                        incomeRepository.getIncomeById(transactionId)?.toTransaction()
                    }
                }

                val category = transaction?.let {
                    categoryRepository.getCategoryById(it.categoryId)
                }

                _uiState.value = TransactionDetailUiState(
                    transaction = transaction,
                    category = category,
                    isLoading = false,
                    showError = if (transaction == null) context.getString(R.string.transaction_not_found) else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = context.getString(R.string.error_loading, e.message ?: "")
                )
            }
        }
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            val transaction = _uiState.value.transaction
            if (transaction == null) {
                _uiState.value = _uiState.value.copy(
                    showError = context.getString(R.string.transaction_not_found)
                )
                return@launch
            }

            try {
                when (transaction) {
                    is Transaction.ExpenseTransaction -> {
                        expenseRepository.deleteExpense(transaction.toExpense())
                    }
                    is Transaction.IncomeTransaction -> {
                        incomeRepository.deleteIncome(transaction.toIncome())
                    }
                }

                _uiState.value = _uiState.value.copy(isDeleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    showError = context.getString(R.string.error_deleting, e.message ?: "")
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(showError = null)
    }
}

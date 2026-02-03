package com.example.budgetcontrol.feature.transaction.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.domain.model.toExpense
import com.example.budgetcontrol.core.domain.model.toIncome
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val category: Category? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val showError: String? = null
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    /**
     * Загрузка транзакции
     */
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
                    showError = if (transaction == null) "Транзакция не найдена" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = "Ошибка загрузки: ${e.message}"
                )
            }
        }
    }

    /**
     * Удаление транзакции
     */
    fun deleteTransaction() {
        viewModelScope.launch {
            val transaction = _uiState.value.transaction
            if (transaction == null) {
                _uiState.value = _uiState.value.copy(
                    showError = "Транзакция не найдена"
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
                    showError = "Ошибка при удалении: ${e.message}"
                )
            }
        }
    }

    /**
     * Очистка ошибки
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(showError = null)
    }
}
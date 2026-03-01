package com.example.budgetcontrol.feature.transaction.common

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetIncomesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsByCategoryUiState(
    val transactions: List<Transaction> = emptyList(),
    val category: Category? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = true,
    val showError: String? = null
)

@HiltViewModel
class TransactionsByCategoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getIncomesUseCase: GetIncomesUseCase,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsByCategoryUiState())
    val uiState: StateFlow<TransactionsByCategoryUiState> = _uiState.asStateFlow()

    fun loadTransactions(
        categoryId: String,
        transactionType: TransactionType,
        startDate: Long? = null,
        endDate: Long? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                transactionType = transactionType,
                isLoading = true
            )

            try {
                val category = categoryRepository.getCategoryById(categoryId)

                when (transactionType) {
                    TransactionType.EXPENSE -> {
                        getExpensesUseCase.getByCategory(categoryId).collect { expenses ->
                            val filtered = if (startDate != null && endDate != null) {
                                expenses.filter { it.date in startDate..endDate }
                            } else {
                                expenses
                            }
                            val transactions = filtered.map { it.toTransaction() }
                                .sortedByDescending { it.date }

                            _uiState.value = TransactionsByCategoryUiState(
                                transactions = transactions,
                                category = category,
                                transactionType = transactionType,
                                totalAmount = transactions.sumOf { it.amount },
                                isLoading = false,
                                showError = if (category == null) context.getString(R.string.category_not_found) else null
                            )
                        }
                    }
                    TransactionType.INCOME -> {
                        getIncomesUseCase.getByCategory(categoryId).collect { incomes ->
                            val filtered = if (startDate != null && endDate != null) {
                                incomes.filter { it.date in startDate..endDate }
                            } else {
                                incomes
                            }
                            val transactions = filtered.map { it.toTransaction() }
                                .sortedByDescending { it.date }

                            _uiState.value = TransactionsByCategoryUiState(
                                transactions = transactions,
                                category = category,
                                transactionType = transactionType,
                                totalAmount = transactions.sumOf { it.amount },
                                isLoading = false,
                                showError = if (category == null) context.getString(R.string.category_not_found) else null
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = context.getString(R.string.error_loading, e.message ?: "")
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(showError = null)
    }
}

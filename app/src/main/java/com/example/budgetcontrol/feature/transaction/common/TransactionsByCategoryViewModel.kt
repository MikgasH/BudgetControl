package com.example.budgetcontrol.feature.transaction.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetIncomesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getIncomesUseCase: GetIncomesUseCase,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsByCategoryUiState())
    val uiState: StateFlow<TransactionsByCategoryUiState> = _uiState.asStateFlow()

    /**
     * Загрузка транзакций по категории
     */
    fun loadTransactions(categoryId: String, transactionType: TransactionType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                transactionType = transactionType,
                isLoading = true
            )

            try {
                // Загружаем категорию
                val category = categoryRepository.getCategoryById(categoryId)

                // Загружаем транзакции по категории в зависимости от типа
                when (transactionType) {
                    TransactionType.EXPENSE -> {
                        getExpensesUseCase.getByCategory(categoryId).collect { expenses ->
                            val transactions = expenses.map { it.toTransaction() }
                                .sortedByDescending { it.date }

                            _uiState.value = TransactionsByCategoryUiState(
                                transactions = transactions,
                                category = category,
                                transactionType = transactionType,
                                totalAmount = transactions.sumOf { it.amount },
                                isLoading = false,
                                showError = if (category == null) "Категория не найдена" else null
                            )
                        }
                    }
                    TransactionType.INCOME -> {
                        getIncomesUseCase.getByCategory(categoryId).collect { incomes ->
                            val transactions = incomes.map { it.toTransaction() }
                                .sortedByDescending { it.date }

                            _uiState.value = TransactionsByCategoryUiState(
                                transactions = transactions,
                                category = category,
                                transactionType = transactionType,
                                totalAmount = transactions.sumOf { it.amount },
                                isLoading = false,
                                showError = if (category == null) "Категория не найдена" else null
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = "Ошибка загрузки: ${e.message}"
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
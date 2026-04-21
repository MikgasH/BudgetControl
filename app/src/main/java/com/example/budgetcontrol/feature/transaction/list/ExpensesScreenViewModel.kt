package com.example.budgetcontrol.feature.transaction.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.findById
import com.example.budgetcontrol.core.domain.model.Expense
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.toExpense
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.usecase.DeleteExpenseUseCase
import com.example.budgetcontrol.core.domain.usecase.GetCategoriesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.util.SUBSCRIPTION_TIMEOUT_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ExpensesScreenUiState(
    val expenses: List<Expense> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val totalAmount: Double = 0.0
)

@HiltViewModel
class ExpensesScreenViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesScreenUiState())
    val uiState: StateFlow<ExpensesScreenUiState> = _uiState.asStateFlow()

    val baseCurrency: StateFlow<String> = preferencesManager.baseCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), DEFAULT_BASE_CURRENCY)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                combine(
                    getExpensesUseCase(),
                    getCategoriesUseCase()
                ) { expenses, categories ->
                    val transactions = expenses.map { it.toTransaction() }
                        .sortedByDescending { it.date }

                    ExpensesScreenUiState(
                        expenses = expenses,
                        transactions = transactions,
                        categories = categories,
                        isLoading = false,
                        totalAmount = expenses.sumOf { it.amount }
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
                else -> {
                    // Should not happen in ExpensesScreen, but handle as fallback
                }
            }
        }
    }

    fun getCategoryById(categoryId: String): Category? {
        return _uiState.value.categories.findById(categoryId)
    }
}
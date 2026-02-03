package com.example.budgetcontrol.feature.transaction.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.Expense
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.toExpense
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.domain.usecase.DeleteExpenseUseCase
import com.example.budgetcontrol.core.domain.usecase.GetCategoriesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpensesScreenUiState(
    val expenses: List<Expense> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val totalAmount: Double = 0.0
)

@HiltViewModel
class ExpensesScreenViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesScreenUiState())
    val uiState: StateFlow<ExpensesScreenUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
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
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            when (transaction) {
                is Transaction.ExpenseTransaction -> {
                    deleteExpenseUseCase(transaction.toExpense())
                }
                else -> {
                    // Это не должно происходить в ExpensesScreen, но на всякий случай
                }
            }
        }
    }

    fun getCategoryById(categoryId: String): Category? {
        return _uiState.value.categories.find { it.id == categoryId }
    }
}
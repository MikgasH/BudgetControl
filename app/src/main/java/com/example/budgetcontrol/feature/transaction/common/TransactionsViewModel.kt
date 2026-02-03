package com.example.budgetcontrol.feature.transaction.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.CategoryType
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.domain.model.toExpense
import com.example.budgetcontrol.core.domain.model.toIncome
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import com.example.budgetcontrol.core.domain.usecase.AddExpenseUseCase
import com.example.budgetcontrol.core.domain.usecase.AddExpenseResult
import com.example.budgetcontrol.core.domain.usecase.AddIncomeUseCase
import com.example.budgetcontrol.core.util.ValidationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionFormUiState(
    val mode: TransactionFormMode = TransactionFormMode.ADD,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val amount: String = "",
    val description: String = "",
    val selectedDate: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val showError: String? = null,
    val isSuccess: Boolean = false,
    val originalTransaction: Transaction? = null,
    val canChangeType: Boolean = false,
    val showTypeChangeDialog: Boolean = false,
    val pendingTypeChange: TransactionType? = null,
    val selectedExpenseCategory: Category? = null,
    val selectedIncomeCategory: Category? = null,
    // Мультивалютность
    val availableCurrencies: List<String> = listOf("EUR"),
    val selectedCurrency: String = "EUR",
    val isCurrenciesLoading: Boolean = false,
    val currenciesError: String? = null
)

enum class TransactionFormMode { ADD, EDIT }

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    private val addExpenseUseCase: AddExpenseUseCase,
    private val addIncomeUseCase: AddIncomeUseCase,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val categoryRepository: CategoryRepository,
    private val cerpsRepository: CerpsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionFormUiState())
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    private var allCategories: List<Category> = emptyList()

    /**
     * Инициализация ViewModel
     */
    fun initialize(
        mode: TransactionFormMode,
        type: TransactionType,
        transactionId: String? = null,
        initialDate: Long = System.currentTimeMillis()
    ) {
        _uiState.value = _uiState.value.copy(
            mode = mode,
            transactionType = type,
            selectedDate = initialDate,
            isLoading = true,
            canChangeType = mode == TransactionFormMode.EDIT
        )

        // Загружаем валюты для расходов
        if (type == TransactionType.EXPENSE) {
            loadCurrencies()
        }

        when (mode) {
            TransactionFormMode.ADD -> {
                loadCategoriesForAdd(type)
            }
            TransactionFormMode.EDIT -> {
                if (transactionId != null) {
                    loadTransactionForEdit(transactionId, type)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showError = "ID транзакции не указан"
                    )
                }
            }
        }
    }

    /**
     * Загрузка списка валют из CERPS
     */
    private fun loadCurrencies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCurrenciesLoading = true)

            when (val result = cerpsRepository.getCurrencies()) {
                is CerpsResult.Success -> {
                    val currencyCodes = result.data
                    _uiState.value = _uiState.value.copy(
                        availableCurrencies = currencyCodes.ifEmpty { listOf("EUR") },
                        isCurrenciesLoading = false,
                        currenciesError = null
                    )
                }
                is CerpsResult.Error -> {
                    // Если CERPS недоступен, оставляем только EUR
                    _uiState.value = _uiState.value.copy(
                        availableCurrencies = listOf("EUR"),
                        isCurrenciesLoading = false,
                        currenciesError = result.message
                    )
                }
            }
        }
    }

    /**
     * Выбор валюты
     */
    fun selectCurrency(currency: String) {
        _uiState.value = _uiState.value.copy(
            selectedCurrency = currency,
            showError = null
        )
    }

    /**
     * Смена типа транзакции
     */
    fun changeTransactionType(newType: TransactionType) {
        if (!_uiState.value.canChangeType || newType == _uiState.value.transactionType) return

        val currentState = _uiState.value

        val updatedState = when (currentState.transactionType) {
            TransactionType.EXPENSE -> currentState.copy(selectedExpenseCategory = currentState.selectedCategory)
            TransactionType.INCOME -> currentState.copy(selectedIncomeCategory = currentState.selectedCategory)
        }

        _uiState.value = updatedState.copy(
            transactionType = newType,
            isLoading = true,
            // Сбрасываем валюту на EUR при переключении на доходы
            selectedCurrency = if (newType == TransactionType.INCOME) "EUR" else updatedState.selectedCurrency
        )

        // Загружаем валюты если переключились на расходы
        if (newType == TransactionType.EXPENSE) {
            loadCurrencies()
        }

        loadCategoriesForType(newType)
    }

    fun requestTypeChange(newType: TransactionType) {
        changeTransactionType(newType)
    }

    fun confirmTypeChange() {}
    fun dismissTypeChangeDialog() {}

    private fun loadCategoriesForType(type: TransactionType) {
        viewModelScope.launch {
            try {
                if (allCategories.isEmpty()) {
                    categoryRepository.getAllCategories().collect { categories ->
                        allCategories = categories
                        updateCategoriesForType(type)
                    }
                } else {
                    updateCategoriesForType(type)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = "Ошибка загрузки категорий: ${e.message}"
                )
            }
        }
    }

    private fun updateCategoriesForType(type: TransactionType) {
        val filteredCategories = when (type) {
            TransactionType.EXPENSE -> allCategories.filter { it.type == CategoryType.EXPENSE }
            TransactionType.INCOME -> allCategories.filter { it.type == CategoryType.INCOME }
        }

        val previouslySelected = when (type) {
            TransactionType.EXPENSE -> _uiState.value.selectedExpenseCategory
            TransactionType.INCOME -> _uiState.value.selectedIncomeCategory
        }

        val categoryToSelect = if (previouslySelected != null &&
            filteredCategories.any { it.id == previouslySelected.id }) {
            filteredCategories.find { it.id == previouslySelected.id }
        } else {
            filteredCategories.firstOrNull()
        }

        _uiState.value = _uiState.value.copy(
            categories = filteredCategories,
            selectedCategory = categoryToSelect,
            isLoading = false
        )
    }

    private fun loadCategoriesForAdd(type: TransactionType) {
        loadCategoriesForType(type)
    }

    private fun loadTransactionForEdit(transactionId: String, type: TransactionType) {
        viewModelScope.launch {
            try {
                val transaction = when (type) {
                    TransactionType.EXPENSE -> {
                        expenseRepository.getExpenseById(transactionId)?.toTransaction()
                    }
                    TransactionType.INCOME -> {
                        incomeRepository.getIncomeById(transactionId)?.toTransaction()
                    }
                }

                if (transaction == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showError = "Транзакция не найдена"
                    )
                    return@launch
                }

                val selectedCategory = categoryRepository.getCategoryById(transaction.categoryId)

                categoryRepository.getAllCategories().collect { categories ->
                    allCategories = categories

                    val filteredCategories = when (type) {
                        TransactionType.EXPENSE -> categories.filter { it.type == CategoryType.EXPENSE }
                        TransactionType.INCOME -> categories.filter { it.type == CategoryType.INCOME }
                    }

                    val initialExpenseCategory = if (type == TransactionType.EXPENSE) {
                        selectedCategory
                    } else {
                        categories.filter { it.type == CategoryType.EXPENSE }.firstOrNull()
                    }

                    val initialIncomeCategory = if (type == TransactionType.INCOME) {
                        selectedCategory
                    } else {
                        categories.filter { it.type == CategoryType.INCOME }.firstOrNull()
                    }

                    _uiState.value = _uiState.value.copy(
                        categories = filteredCategories,
                        selectedCategory = selectedCategory,
                        amount = transaction.amount.toString(),
                        description = transaction.description ?: "",
                        selectedDate = transaction.date,
                        originalTransaction = transaction,
                        selectedExpenseCategory = initialExpenseCategory,
                        selectedIncomeCategory = initialIncomeCategory,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = "Ошибка загрузки транзакции: ${e.message}"
                )
            }
        }
    }

    fun updateAmount(amount: String) {
        val filteredAmount = ValidationHelper.filterAmountInput(amount)
        _uiState.value = _uiState.value.copy(
            amount = filteredAmount,
            showError = null
        )
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(
            description = description,
            showError = null
        )
    }

    fun selectCategory(category: Category) {
        val currentState = _uiState.value

        val updatedState = when (currentState.transactionType) {
            TransactionType.EXPENSE -> currentState.copy(selectedExpenseCategory = category)
            TransactionType.INCOME -> currentState.copy(selectedIncomeCategory = category)
        }

        _uiState.value = updatedState.copy(
            selectedCategory = category,
            showError = null
        )
    }

    fun updateDate(date: Long) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            showError = null
        )
    }

    /**
     * Сохранение транзакции
     */
    fun saveTransaction() {
        val currentState = _uiState.value

        val validationResult = ValidationHelper.validateTransaction(
            amount = currentState.amount,
            category = currentState.selectedCategory
        )

        if (validationResult is ValidationHelper.ValidationResult.Error) {
            _uiState.value = currentState.copy(showError = validationResult.message)
            return
        }

        val amountDouble = currentState.amount.toDouble()

        _uiState.value = currentState.copy(isLoading = true, showError = null)

        viewModelScope.launch {
            try {
                when (currentState.mode) {
                    TransactionFormMode.ADD -> {
                        when (currentState.transactionType) {
                            TransactionType.EXPENSE -> {
                                // Используем UseCase с валютой
                                val result = addExpenseUseCase(
                                    amount = amountDouble,
                                    currency = currentState.selectedCurrency,
                                    categoryId = currentState.selectedCategory!!.id,
                                    description = currentState.description.ifBlank { null },
                                    date = currentState.selectedDate
                                )

                                when (result) {
                                    is AddExpenseResult.Success -> {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            isSuccess = true
                                        )
                                    }
                                    is AddExpenseResult.Error -> {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            showError = result.message
                                        )
                                    }
                                }
                                return@launch
                            }
                            TransactionType.INCOME -> {
                                addIncomeUseCase(
                                    amount = amountDouble,
                                    categoryId = currentState.selectedCategory!!.id,
                                    description = currentState.description.ifBlank { null },
                                    date = currentState.selectedDate
                                )
                            }
                        }
                    }
                    TransactionFormMode.EDIT -> {
                        val originalTransaction = currentState.originalTransaction!!

                        if (currentState.transactionType == originalTransaction.type) {
                            updateExistingTransaction(
                                originalTransaction = originalTransaction,
                                amount = amountDouble,
                                categoryId = currentState.selectedCategory!!.id,
                                description = currentState.description.ifBlank { null },
                                date = currentState.selectedDate
                            )
                        } else {
                            replaceTransactionWithNewType(
                                originalTransaction = originalTransaction,
                                newType = currentState.transactionType,
                                amount = amountDouble,
                                categoryId = currentState.selectedCategory!!.id,
                                description = currentState.description.ifBlank { null },
                                date = currentState.selectedDate
                            )
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = "Ошибка при сохранении: ${e.message}"
                )
            }
        }
    }

    private suspend fun updateExistingTransaction(
        originalTransaction: Transaction,
        amount: Double,
        categoryId: String,
        description: String?,
        date: Long
    ) {
        when (originalTransaction) {
            is Transaction.ExpenseTransaction -> {
                val updatedExpense = originalTransaction.copy(
                    amount = amount,
                    categoryId = categoryId,
                    description = description,
                    date = date
                ).toExpense()

                expenseRepository.updateExpense(updatedExpense)
            }
            is Transaction.IncomeTransaction -> {
                val updatedIncome = originalTransaction.copy(
                    amount = amount,
                    categoryId = categoryId,
                    description = description,
                    date = date
                ).toIncome()

                incomeRepository.updateIncome(updatedIncome)
            }
        }
    }

    private suspend fun replaceTransactionWithNewType(
        originalTransaction: Transaction,
        newType: TransactionType,
        amount: Double,
        categoryId: String,
        description: String?,
        date: Long
    ) {
        when (originalTransaction) {
            is Transaction.ExpenseTransaction -> {
                expenseRepository.deleteExpense(originalTransaction.toExpense())
            }
            is Transaction.IncomeTransaction -> {
                incomeRepository.deleteIncome(originalTransaction.toIncome())
            }
        }

        when (newType) {
            TransactionType.EXPENSE -> {
                addExpenseUseCase.addInEur(
                    amount = amount,
                    categoryId = categoryId,
                    description = description,
                    date = date
                )
            }
            TransactionType.INCOME -> {
                addIncomeUseCase(
                    amount = amount,
                    categoryId = categoryId,
                    description = description,
                    date = date
                )
            }
        }
    }

    fun hasChanges(): Boolean {
        val currentState = _uiState.value
        val original = currentState.originalTransaction ?: return false

        return currentState.amount != original.amount.toString() ||
                currentState.description != (original.description ?: "") ||
                currentState.selectedCategory?.id != original.categoryId ||
                currentState.transactionType != original.type ||
                !isSameDay(currentState.selectedDate, original.date)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(showError = null)
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = date2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
}
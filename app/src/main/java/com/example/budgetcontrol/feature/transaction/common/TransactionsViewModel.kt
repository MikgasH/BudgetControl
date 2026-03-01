package com.example.budgetcontrol.feature.transaction.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.data.local.database.entities.BankEntity
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.repository.BankRepository
import com.example.budgetcontrol.core.data.repository.NetworkStatusRepository
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.CategoryType
import com.example.budgetcontrol.core.domain.model.CurrencyExchange
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.domain.model.toExpense
import com.example.budgetcontrol.core.domain.model.toIncome
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.CurrencyExchangeRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import com.example.budgetcontrol.core.domain.usecase.AddExpenseUseCase
import com.example.budgetcontrol.core.domain.usecase.AddExpenseResult
import com.example.budgetcontrol.core.domain.usecase.AddIncomeUseCase
import com.example.budgetcontrol.core.domain.usecase.AddIncomeResult
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.util.ValidationHelper
import java.util.UUID
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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
    val currenciesError: String? = null,
    // Банк и комиссия
    val availableBanks: List<BankEntity> = emptyList(),
    val selectedBank: BankEntity? = null,
    val convertedAmountPreview: String = "",
    // Уточнить сумму вручную
    val exactEurAmount: String = "",
    val isExactAmountEnabled: Boolean = false,
    // Оригинальная сумма в исходной валюте (заполняется только в EDIT mode)
    val originalAmount: Double = 0.0,
    val favoriteCurrencies: Set<String> = emptySet(),
    // Cash mode
    val paymentMethod: String = "CARD",
    val cashRate: String = "",
    val cashRatePlaceholder: String = "",
    val lastCashExchange: CurrencyExchange? = null,
    // Network status
    val networkStatus: NetworkStatus = NetworkStatus.ONLINE
)

enum class NetworkStatus {
    ONLINE,
    NO_INTERNET,
    SERVICE_UNAVAILABLE,
    OFFLINE_NO_CACHE
}

enum class TransactionFormMode { ADD, EDIT }

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val addIncomeUseCase: AddIncomeUseCase,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val categoryRepository: CategoryRepository,
    private val cerpsRepository: CerpsRepository,
    private val bankRepository: BankRepository,
    private val preferencesManager: PreferencesManager,
    private val currencyExchangeRepository: CurrencyExchangeRepository,
    private val networkStatusRepository: NetworkStatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionFormUiState())
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    private var allCategories: List<Category> = emptyList()

    /** In-memory cache so we don't re-fetch currencies on every screen open */
    private var cachedCurrencies: List<String>? = null

    /** Cache of the interbank exchange rate for the current currency */
    private var cachedInterBankRate: Double? = null
    private var cachedRateCurrency: String? = null

    init {
        loadBanks()
        observeFavoriteCurrencies()
    }

    private fun loadBanks() {
        viewModelScope.launch {
            bankRepository.getFavoriteBanks().collect { banks ->
                val current = _uiState.value
                _uiState.value = current.copy(availableBanks = banks)
            }
        }
    }

    private fun observeFavoriteCurrencies() {
        viewModelScope.launch {
            preferencesManager.favoriteCurrenciesFlow.collect { favorites ->
                _uiState.value = _uiState.value.copy(favoriteCurrencies = favorites)
            }
        }
    }

    private fun checkNetworkStatus() {
        viewModelScope.launch {
            val hasInternet = networkStatusRepository.isInternetAvailable()
            if (!hasInternet) {
                val cachedRates = preferencesManager.getLastRates().firstOrNull() ?: emptyMap()
                _uiState.value = _uiState.value.copy(
                    networkStatus = if (cachedRates.isNotEmpty()) {
                        NetworkStatus.NO_INTERNET
                    } else {
                        NetworkStatus.OFFLINE_NO_CACHE
                    }
                )
            } else {
                // Don't probe CERPS health here — just check internet connectivity.
                // The actual conversion call will handle CERPS errors gracefully.
                _uiState.value = _uiState.value.copy(networkStatus = NetworkStatus.ONLINE)
            }
        }
    }

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

        // Загружаем валюты для расходов и доходов
        loadCurrencies()
        checkNetworkStatus()

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
                        showError = context.getString(R.string.error_transaction_id_missing)
                    )
                }
            }
        }
    }

    /**
     * Загрузка списка валют из CERPS (с кэшированием в памяти)
     */
    private fun loadCurrencies() {
        // Serve from cache if already fetched
        val cached = cachedCurrencies
        if (cached != null) {
            _uiState.value = _uiState.value.copy(
                availableCurrencies = cached,
                isCurrenciesLoading = false,
                currenciesError = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCurrenciesLoading = true)

            when (val result = cerpsRepository.getCurrencies()) {
                is CerpsResult.Success -> {
                    val all = result.data.ifEmpty { listOf("EUR") }
                    cachedCurrencies = all
                    _uiState.value = _uiState.value.copy(
                        availableCurrencies = all,
                        isCurrenciesLoading = false,
                        currenciesError = null
                    )
                }
                is CerpsResult.Error -> {
                    // Если CERPS недоступен, оставляем только EUR
                    _uiState.value = _uiState.value.copy(
                        availableCurrencies = listOf("EUR"),
                        isCurrenciesLoading = false,
                        currenciesError = context.getString(R.string.error_currency_service_unavailable)
                    )
                }
            }
        }
    }

    /**
     * Выбор валюты
     */
    fun selectCurrency(currency: String) {
        val current = _uiState.value

        viewModelScope.launch {
            val lastMethod = if (currency != "EUR") {
                preferencesManager.lastPaymentMethodFlow.firstOrNull() ?: "CARD"
            } else "CARD"

            val defaultBank = if (currency != "EUR") {
                current.availableBanks.firstOrNull { it.isDefault }
                    ?: current.availableBanks.firstOrNull()
            } else null

            _uiState.value = current.copy(
                selectedCurrency = currency,
                selectedBank = defaultBank,
                convertedAmountPreview = "",
                // Reset exact amount when currency changes
                isExactAmountEnabled = if (currency == "EUR") false else current.isExactAmountEnabled,
                exactEurAmount = if (currency == "EUR") "" else current.exactEurAmount,
                // Restore last payment method
                paymentMethod = if (currency == "EUR") "CARD" else lastMethod,
                cashRate = "",
                cashRatePlaceholder = "",
                lastCashExchange = null,
                showError = null
            )

            if (currency != "EUR") {
                if (lastMethod == "CARD") {
                    fetchRateAndUpdatePreview(currency, current.amount, defaultBank)
                } else {
                    loadCashExchangeRate(currency)
                }
                checkNetworkStatus()
            } else {
                _uiState.value = _uiState.value.copy(networkStatus = NetworkStatus.ONLINE)
            }
        }
    }

    fun selectPaymentMethod(method: String) {
        val current = _uiState.value
        _uiState.value = current.copy(
            paymentMethod = method,
            showError = null
        )
        if (method == "CASH" && current.selectedCurrency != "EUR") {
            loadCashExchangeRate(current.selectedCurrency)
        }
        viewModelScope.launch {
            preferencesManager.setLastPaymentMethod(method)
        }
    }

    fun updateCashRate(rate: String) {
        _uiState.value = _uiState.value.copy(cashRate = rate, showError = null)
    }

    private fun loadCashExchangeRate(currency: String) {
        viewModelScope.launch {
            // Try to load last saved cash exchange rate
            val lastExchange = currencyExchangeRepository.getLatestExchangeForCurrency(currency, "EUR")
            val placeholder = if (lastExchange != null) {
                String.format("%.4f", lastExchange.exchangeRate)
            } else {
                // Fall back to interbank rate
                val rate = if (cachedRateCurrency == currency && cachedInterBankRate != null) {
                    cachedInterBankRate
                } else {
                    when (val result = cerpsRepository.convert("EUR", currency, 1.0)) {
                        is CerpsResult.Success -> {
                            val r = result.data.exchangeRate.toDouble()
                            cachedInterBankRate = r
                            cachedRateCurrency = currency
                            r
                        }
                        is CerpsResult.Error -> null
                    }
                }
                if (rate != null) String.format("%.4f", rate) else ""
            }

            _uiState.value = _uiState.value.copy(
                lastCashExchange = lastExchange,
                cashRatePlaceholder = placeholder,
                cashRate = if (lastExchange != null) String.format("%.4f", lastExchange.exchangeRate) else _uiState.value.cashRate
            )
        }
    }

    /**
     * Выбор банка
     */
    fun selectBank(bank: BankEntity) {
        val current = _uiState.value
        _uiState.value = current.copy(selectedBank = bank, convertedAmountPreview = "")
        if (current.selectedCurrency != "EUR") {
            fetchRateAndUpdatePreview(current.selectedCurrency, current.amount, bank)
        }
    }

    /**
     * Получить межбанковский курс и обновить превью конвертации.
     *
     * CERPS возвращает курс как "1 EUR = X единиц иностранной валюты".
     * Чтобы получить этот курс, конвертируем EUR→currency (1 EUR).
     * Формула: convertedAmount = originalAmount / realRate
     * Пример: 150 BYN / 3.48 ≈ 43 EUR ✓
     */
    private fun fetchRateAndUpdatePreview(currency: String, amount: String, bank: BankEntity?) {
        if (bank == null) return
        viewModelScope.launch {
            // Use cached rate if currency unchanged
            val interBankRate: Double? = if (cachedRateCurrency == currency && cachedInterBankRate != null) {
                cachedInterBankRate
            } else {
                // Convert EUR → currency to get "1 EUR = X foreign units" (e.g. 3.48 BYN)
                when (val result = cerpsRepository.convert("EUR", currency, 1.0)) {
                    is CerpsResult.Success -> {
                        val rate = result.data.exchangeRate.toDouble()
                        cachedInterBankRate = rate
                        cachedRateCurrency = currency
                        rate
                    }
                    is CerpsResult.Error -> null
                }
            }

            if (interBankRate != null) {
                _uiState.value = _uiState.value.copy(
                    convertedAmountPreview = buildPreview(amount, interBankRate, bank)
                )
            }
        }
    }

    /**
     * Строим строку превью конвертации
     * realRate = interBankRate * (1 - commission/100)
     * convertedAmount = originalAmount / realRate
     */
    private fun buildPreview(amountStr: String, interBankRate: Double, bank: BankEntity): String {
        val amount = amountStr.replace(',', '.').toDoubleOrNull() ?: return ""
        if (amount <= 0) return ""
        val realRate = interBankRate * (1.0 - bank.commissionPercent / 100.0)
        if (realRate <= 0) return ""
        val converted = amount / realRate
        val realRateFormatted = String.format("%.4f", realRate)
        val convertedFormatted = String.format("%.2f", converted)
        val commissionFormatted = bank.commissionPercent.let {
            if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
        }
        return context.getString(R.string.conversion_preview_format, convertedFormatted, realRateFormatted, commissionFormatted)
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
            isLoading = true
        )

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
                    showError = context.getString(R.string.error_loading_categories, e.message ?: "")
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
                // Fetch full entity to get originalCurrency/bankName
                val expense = if (type == TransactionType.EXPENSE) {
                    expenseRepository.getExpenseById(transactionId)
                } else null

                val income = if (type == TransactionType.INCOME) {
                    incomeRepository.getIncomeById(transactionId)
                } else null

                val transaction = when (type) {
                    TransactionType.EXPENSE -> expense?.toTransaction()
                    TransactionType.INCOME -> income?.toTransaction()
                }

                if (transaction == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showError = context.getString(R.string.transaction_not_found)
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

                    val current = _uiState.value

                    // Restore bank info for expense or income
                    val restoredCurrency = expense?.originalCurrency
                        ?: income?.originalCurrency
                        ?: "EUR"
                    val restoredBankName = expense?.bankName ?: income?.bankName
                    val restoredBank = if (restoredBankName != null) {
                        current.availableBanks.firstOrNull { it.name == restoredBankName }
                    } else null

                    _uiState.value = current.copy(
                        categories = filteredCategories,
                        selectedCategory = selectedCategory,
                        amount = transaction.amount.toString(),
                        description = transaction.description ?: "",
                        selectedDate = transaction.date,
                        originalTransaction = transaction,
                        selectedExpenseCategory = initialExpenseCategory,
                        selectedIncomeCategory = initialIncomeCategory,
                        selectedCurrency = restoredCurrency,
                        selectedBank = restoredBank,
                        originalAmount = expense?.originalAmount ?: income?.originalAmount ?: transaction.amount,
                        isLoading = false
                    )

                    // If non-EUR, fetch interbank rate for preview
                    if (restoredCurrency != "EUR" && restoredBank != null) {
                        val origAmount = expense?.originalAmount ?: income?.originalAmount ?: transaction.amount
                        fetchRateAndUpdatePreview(restoredCurrency, origAmount.toString(), restoredBank)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = context.getString(R.string.error_loading_transaction, e.message ?: "")
                )
            }
        }
    }

    fun updateAmount(amount: String) {
        val filteredAmount = ValidationHelper.filterAmountInput(amount)
        val current = _uiState.value
        val preview = if (current.selectedCurrency != "EUR" && current.selectedBank != null
            && cachedInterBankRate != null && cachedRateCurrency == current.selectedCurrency
        ) {
            buildPreview(filteredAmount, cachedInterBankRate!!, current.selectedBank)
        } else {
            current.convertedAmountPreview
        }
        _uiState.value = current.copy(
            amount = filteredAmount,
            convertedAmountPreview = preview,
            showError = null
        )
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(
            description = description,
            showError = null
        )
    }

    fun toggleExactAmount(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            isExactAmountEnabled = enabled,
            exactEurAmount = if (!enabled) "" else _uiState.value.exactEurAmount
        )
    }

    fun updateExactEurAmount(amount: String) {
        _uiState.value = _uiState.value.copy(
            exactEurAmount = amount,
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
            context = context,
            amount = currentState.amount,
            category = currentState.selectedCategory
        )

        if (validationResult is ValidationHelper.ValidationResult.Error) {
            _uiState.value = currentState.copy(showError = validationResult.message)
            return
        }

        val amountDouble = currentState.amount.replace(',', '.').toDouble()

        _uiState.value = currentState.copy(isLoading = true, showError = null)

        viewModelScope.launch {
            try {
                when (currentState.mode) {
                    TransactionFormMode.ADD -> {
                        val isCashMode = currentState.paymentMethod == "CASH" &&
                                currentState.selectedCurrency != "EUR"

                        when (currentState.transactionType) {
                            TransactionType.EXPENSE -> {
                                val result = if (isCashMode &&
                                    currentState.isExactAmountEnabled &&
                                    currentState.exactEurAmount.isNotBlank()
                                ) {
                                    // Cash mode with manual EUR amount
                                    val exactEurCash = currentState.exactEurAmount.replace(',', '.').toDoubleOrNull()
                                    if (exactEurCash == null || exactEurCash <= 0) {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            showError = context.getString(R.string.error_enter_valid_eur)
                                        )
                                        return@launch
                                    }
                                    addExpenseUseCase.addWithExactEurAmount(
                                        originalAmount = amountDouble,
                                        originalCurrency = currentState.selectedCurrency,
                                        exactEurAmount = exactEurCash,
                                        categoryId = currentState.selectedCategory!!.id,
                                        description = currentState.description.ifBlank { null },
                                        date = currentState.selectedDate,
                                        bankName = null,
                                        bankCommission = null,
                                        rateSource = "CASH_EXCHANGE"
                                    )
                                } else if (isCashMode) {
                                    // Cash mode: use the cash rate directly
                                    val cashRateValue = currentState.cashRate.replace(',', '.').toDoubleOrNull()
                                        ?: currentState.cashRatePlaceholder.replace(',', '.').toDoubleOrNull()
                                    if (cashRateValue == null || cashRateValue <= 0) {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            showError = context.getString(R.string.error_enter_valid_eur)
                                        )
                                        return@launch
                                    }
                                    val eurAmount = amountDouble / cashRateValue
                                    addExpenseUseCase.addWithExactEurAmount(
                                        originalAmount = amountDouble,
                                        originalCurrency = currentState.selectedCurrency,
                                        exactEurAmount = String.format("%.2f", eurAmount).toDouble(),
                                        categoryId = currentState.selectedCategory!!.id,
                                        description = currentState.description.ifBlank { null },
                                        date = currentState.selectedDate,
                                        bankName = null,
                                        bankCommission = null,
                                        rateSource = "CASH_EXCHANGE"
                                    )
                                } else if (
                                    currentState.isExactAmountEnabled &&
                                    currentState.exactEurAmount.isNotBlank() &&
                                    currentState.selectedCurrency != "EUR"
                                ) {
                                    val exactEur = currentState.exactEurAmount.replace(',', '.').toDoubleOrNull()
                                    if (exactEur == null || exactEur <= 0) {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            showError = context.getString(R.string.error_enter_valid_eur)
                                        )
                                        return@launch
                                    }
                                    addExpenseUseCase.addWithExactEurAmount(
                                        originalAmount = amountDouble,
                                        originalCurrency = currentState.selectedCurrency,
                                        exactEurAmount = exactEur,
                                        categoryId = currentState.selectedCategory!!.id,
                                        description = currentState.description.ifBlank { null },
                                        date = currentState.selectedDate,
                                        bankName = currentState.selectedBank?.name,
                                        bankCommission = currentState.selectedBank?.commissionPercent
                                    )
                                } else {
                                    addExpenseUseCase(
                                        amount = amountDouble,
                                        currency = currentState.selectedCurrency,
                                        categoryId = currentState.selectedCategory!!.id,
                                        description = currentState.description.ifBlank { null },
                                        date = currentState.selectedDate,
                                        bankName = currentState.selectedBank?.name,
                                        bankCommission = currentState.selectedBank?.commissionPercent
                                    )
                                }

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
                                val result = if (isCashMode &&
                                    currentState.isExactAmountEnabled &&
                                    currentState.exactEurAmount.isNotBlank()
                                ) {
                                    // Cash mode with manual EUR amount
                                    val exactEurCash = currentState.exactEurAmount.replace(',', '.').toDoubleOrNull()
                                    if (exactEurCash == null || exactEurCash <= 0) {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            showError = context.getString(R.string.error_enter_valid_eur)
                                        )
                                        return@launch
                                    }
                                    addIncomeUseCase.addWithExactEurAmount(
                                        originalAmount = amountDouble,
                                        originalCurrency = currentState.selectedCurrency,
                                        exactEurAmount = exactEurCash,
                                        categoryId = currentState.selectedCategory!!.id,
                                        description = currentState.description.ifBlank { null },
                                        date = currentState.selectedDate,
                                        bankName = null,
                                        bankCommission = null,
                                        rateSource = "CASH_EXCHANGE"
                                    )
                                } else if (isCashMode) {
                                    // Cash mode: use the cash rate directly
                                    val cashRateValue = currentState.cashRate.replace(',', '.').toDoubleOrNull()
                                        ?: currentState.cashRatePlaceholder.replace(',', '.').toDoubleOrNull()
                                    if (cashRateValue == null || cashRateValue <= 0) {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            showError = context.getString(R.string.error_enter_valid_eur)
                                        )
                                        return@launch
                                    }
                                    val eurAmount = amountDouble / cashRateValue
                                    addIncomeUseCase.addWithExactEurAmount(
                                        originalAmount = amountDouble,
                                        originalCurrency = currentState.selectedCurrency,
                                        exactEurAmount = String.format("%.2f", eurAmount).toDouble(),
                                        categoryId = currentState.selectedCategory!!.id,
                                        description = currentState.description.ifBlank { null },
                                        date = currentState.selectedDate,
                                        bankName = null,
                                        bankCommission = null,
                                        rateSource = "CASH_EXCHANGE"
                                    )
                                } else if (
                                    currentState.isExactAmountEnabled &&
                                    currentState.exactEurAmount.isNotBlank() &&
                                    currentState.selectedCurrency != "EUR"
                                ) {
                                    val exactEur = currentState.exactEurAmount.replace(',', '.').toDoubleOrNull()
                                    if (exactEur == null || exactEur <= 0) {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            showError = context.getString(R.string.error_enter_valid_eur)
                                        )
                                        return@launch
                                    }
                                    addIncomeUseCase.addWithExactEurAmount(
                                        originalAmount = amountDouble,
                                        originalCurrency = currentState.selectedCurrency,
                                        exactEurAmount = exactEur,
                                        categoryId = currentState.selectedCategory!!.id,
                                        description = currentState.description.ifBlank { null },
                                        date = currentState.selectedDate,
                                        bankName = currentState.selectedBank?.name,
                                        bankCommission = currentState.selectedBank?.commissionPercent
                                    )
                                } else {
                                    addIncomeUseCase(
                                        amount = amountDouble,
                                        currency = currentState.selectedCurrency,
                                        categoryId = currentState.selectedCategory!!.id,
                                        description = currentState.description.ifBlank { null },
                                        date = currentState.selectedDate,
                                        bankName = currentState.selectedBank?.name,
                                        bankCommission = currentState.selectedBank?.commissionPercent
                                    )
                                }

                                when (result) {
                                    is AddIncomeResult.Success -> {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            isSuccess = true
                                        )
                                    }
                                    is AddIncomeResult.Error -> {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            showError = result.message
                                        )
                                    }
                                }
                                return@launch
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
                                date = currentState.selectedDate,
                                selectedCurrency = currentState.selectedCurrency,
                                bankName = currentState.selectedBank?.name,
                                bankCommission = currentState.selectedBank?.commissionPercent
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
                    showError = context.getString(R.string.error_saving, e.message ?: "")
                )
            }
        }
    }

    private suspend fun updateExistingTransaction(
        originalTransaction: Transaction,
        amount: Double,
        categoryId: String,
        description: String?,
        date: Long,
        selectedCurrency: String = "EUR",
        bankName: String? = null,
        bankCommission: Double? = null
    ) {
        when (originalTransaction) {
            is Transaction.ExpenseTransaction -> {
                // Use AddExpenseUseCase to handle conversion (re-calculates via CERPS)
                addExpenseUseCase(
                    amount = amount,
                    currency = selectedCurrency,
                    categoryId = categoryId,
                    description = description,
                    date = date,
                    bankName = bankName,
                    bankCommission = bankCommission
                )
                // Delete the old record after inserting the new one
                expenseRepository.deleteExpenseById(originalTransaction.id)
            }
            is Transaction.IncomeTransaction -> {
                // Use AddIncomeUseCase to handle conversion (re-calculates via CERPS)
                addIncomeUseCase(
                    amount = amount,
                    currency = selectedCurrency,
                    categoryId = categoryId,
                    description = description,
                    date = date,
                    bankName = bankName,
                    bankCommission = bankCommission
                )
                // Delete the old record after inserting the new one
                incomeRepository.deleteIncomeById(originalTransaction.id)
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
                addIncomeUseCase.addInEur(
                    amount = amount,
                    categoryId = categoryId,
                    description = description,
                    date = date
                )
            }
        }
    }

    fun createCategory(name: String, iconName: String, color: String, type: CategoryType) {
        viewModelScope.launch {
            val newCategory = Category(
                id = UUID.randomUUID().toString(),
                name = name,
                iconName = iconName,
                color = color,
                isDefault = false,
                type = type,
                nameKey = null,
                isSystem = false,
                usageCount = 0
            )
            categoryRepository.insertCategory(newCategory)

            // Reload categories and auto-select the new one
            allCategories = emptyList()
            categoryRepository.getAllCategories().collect { categories ->
                allCategories = categories
                val currentType = _uiState.value.transactionType
                val filteredCategories = when (currentType) {
                    TransactionType.EXPENSE -> categories.filter { it.type == CategoryType.EXPENSE }
                    TransactionType.INCOME -> categories.filter { it.type == CategoryType.INCOME }
                }
                val created = filteredCategories.find { it.id == newCategory.id }
                _uiState.value = _uiState.value.copy(
                    categories = filteredCategories,
                    selectedCategory = created ?: _uiState.value.selectedCategory
                )
            }
        }
    }

    fun updateCategoryColor(category: Category, newColor: String) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category.copy(color = newColor))
            reloadCategories()
        }
    }

    fun updateCustomCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
            reloadCategories()
        }
    }

    fun deleteCustomCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategoryById(category.id)
            // If the deleted category was selected, clear selection
            if (_uiState.value.selectedCategory?.id == category.id) {
                _uiState.value = _uiState.value.copy(selectedCategory = null)
            }
            reloadCategories()
        }
    }

    private fun reloadCategories() {
        viewModelScope.launch {
            allCategories = emptyList()
            categoryRepository.getAllCategories().collect { categories ->
                allCategories = categories
                updateCategoriesForType(_uiState.value.transactionType)
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
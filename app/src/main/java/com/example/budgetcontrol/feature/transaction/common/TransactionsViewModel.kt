package com.example.budgetcontrol.feature.transaction.common

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.domain.model.Bank
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.repository.BankRepository
import com.example.budgetcontrol.core.data.repository.NetworkStatusRepository
import com.example.budgetcontrol.core.domain.model.CashRateMode
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.CategoryLimit
import com.example.budgetcontrol.core.domain.model.CategoryType
import com.example.budgetcontrol.core.domain.model.CurrencyExchange
import com.example.budgetcontrol.core.domain.model.NetworkStatus
import com.example.budgetcontrol.core.domain.model.RateSource
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.domain.model.toExpense
import com.example.budgetcontrol.core.domain.model.toIncome
import com.example.budgetcontrol.core.domain.model.toTransaction
import com.example.budgetcontrol.core.domain.repository.CategoryLimitRepository
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.CurrencyExchangeRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import com.example.budgetcontrol.core.domain.usecase.AddTransactionUseCase
import com.example.budgetcontrol.core.domain.usecase.AddTransactionResult
import com.example.budgetcontrol.core.domain.usecase.AddTransactionError
import com.example.budgetcontrol.core.domain.usecase.AccountWithBalance
import com.example.budgetcontrol.core.domain.usecase.GetAccountsUseCase
import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.util.DateRangeHelper
import com.example.budgetcontrol.core.util.HOUR_IN_MS
import com.example.budgetcontrol.core.util.ValidationHelper
import java.util.UUID
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class CashRateState(
    val cashRate: String = "",
    val cashRatePlaceholder: String = "",
    val cashRateMode: CashRateMode = CashRateMode.LAST_EXCHANGE,
    val isLastExchangeAvailable: Boolean = false,
    val isCurrentRateAvailable: Boolean = true,
    val isCurrentRateLoading: Boolean = false,
    val lastCashExchange: CurrencyExchange? = null,
    val availableCashExchanges: List<CurrencyExchange> = emptyList(),
    val selectedCashExchangeId: String? = null,
    val saveExchangeRecord: Boolean = false,
    val exchangeRecordDescription: String = ""
)

@Immutable
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
    val baseCurrency: String = DEFAULT_BASE_CURRENCY,
    val availableCurrencies: List<String> = listOf(DEFAULT_BASE_CURRENCY),
    val selectedCurrency: String = DEFAULT_BASE_CURRENCY,
    val isCurrenciesLoading: Boolean = false,
    val currenciesError: String? = null,
    val availableBanks: List<Bank> = emptyList(),
    val selectedBank: Bank? = null,
    val convertedAmountPreview: String = "",
    val exactEurAmount: String = "",
    val isExactAmountEnabled: Boolean = false,
    // Only populated in EDIT mode
    val originalAmount: Double = 0.0,
    val favoriteCurrencies: Set<String> = emptySet(),
    // Cash mode
    val paymentMethod: String = "CARD",
    val cashRateState: CashRateState = CashRateState(),
    // Network status
    val networkStatus: NetworkStatus = NetworkStatus.ONLINE,
    // True when foreign-currency selection is unavailable because no rates are cached
    // (fully offline on first run). UI should disable the picker and surface the reason.
    val foreignCurrencyDisabled: Boolean = false,
    val foreignCurrencyDisabledMessage: String? = null,
    // Stale rate warning (shown when cached rates are older than 8 hours)
    val staleRateWarning: String? = null,
    // Accounts
    val accounts: List<AccountWithBalance> = emptyList(),
    val selectedAccountId: String = Account.DEFAULT_ACCOUNT_ID,
    // Category spending limit awareness for the currently selected category
    val selectedCategoryLimit: CategoryLimit? = null,
    val selectedCategoryMonthSpend: Double = 0.0,
    val limitProgressMap: Map<String, com.example.budgetcontrol.core.domain.model.CategoryLimitProgress> = emptyMap(),
    val categoryLimits: Map<String, CategoryLimit> = emptyMap()
)

enum class TransactionFormMode { ADD, EDIT }

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val categoryRepository: CategoryRepository,
    private val cerpsRepository: CerpsRepository,
    bankRepository: BankRepository,
    private val preferencesManager: PreferencesManager,
    private val currencyExchangeRepository: CurrencyExchangeRepository,
    private val networkStatusRepository: NetworkStatusRepository,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val accountRepository: AccountRepository,
    private val categoryLimitRepository: CategoryLimitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionFormUiState())
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    private var allCategories: List<Category> = emptyList()

    /** Currency of the currently selected account; falls back to baseCurrency if unresolved. */
    private fun accountCurrency(): String {
        val current = _uiState.value
        return current.accounts.find { it.account.id == current.selectedAccountId }
            ?.account?.currency ?: current.baseCurrency
    }

    private fun accountCurrencyFor(state: TransactionFormUiState): String {
        return state.accounts.find { it.account.id == state.selectedAccountId }
            ?.account?.currency ?: state.baseCurrency
    }

    /** In-memory cache so we don't re-fetch currencies on every screen open */
    private var cachedCurrencies: List<String>? = null

    /** Cache of the interbank exchange rate for the current currency */
    private var cachedInterBankRate: Double? = null
    private var cachedRateCurrency: String? = null

    /** Tracks the active limit/spend observation so we can replace it on category change. */
    private var limitObservationJob: Job? = null

    /** Tracks the active cash-exchange history Flow so we can replace it on currency change. */
    private var cashHistoryJob: Job? = null

    init {
        // Load saved account first, then start collecting accounts to avoid race condition
        viewModelScope.launch {
            val savedAccountId = preferencesManager.selectedAccountIdFlow.firstOrNull()
            if (savedAccountId != null) {
                _uiState.value = _uiState.value.copy(selectedAccountId = savedAccountId)
            }

            // Now collect accounts — selectedAccountId is already set from prefs or navigation
            var initialCurrencyApplied = false
            getAccountsUseCase.getAccountsWithBalances(
                baseCurrencyFlow = preferencesManager.baseCurrencyFlow,
                ratesFlow = preferencesManager.getLastRates()
            )
                .collect { accountsWithBalances ->
                    val current = _uiState.value
                    val resolvedId = if (accountsWithBalances.any { it.account.id == current.selectedAccountId }) {
                        current.selectedAccountId
                    } else {
                        accountsWithBalances.firstOrNull { it.account.isDefault }?.account?.id
                            ?: Account.DEFAULT_ACCOUNT_ID
                    }
                    _uiState.value = current.copy(
                        accounts = accountsWithBalances,
                        selectedAccountId = resolvedId
                    )
                    // On first load, pre-select the account's currency
                    if (!initialCurrencyApplied) {
                        initialCurrencyApplied = true
                        val account = accountsWithBalances.find { it.account.id == resolvedId }?.account
                        if (account != null && account.currency != _uiState.value.baseCurrency
                            && _uiState.value.selectedCurrency == _uiState.value.baseCurrency) {
                            selectCurrency(account.currency)
                        }
                    }
                }
        }

        combine(
            bankRepository.getFavoriteBanks(),
            preferencesManager.favoriteCurrenciesFlow,
            preferencesManager.baseCurrencyFlow
        ) { banks, favorites, currency ->
            Triple(banks, favorites, currency)
        }.onEach { (banks, favorites, currency) ->
            val current = _uiState.value
            _uiState.value = current.copy(
                availableBanks = banks,
                favoriteCurrencies = favorites,
                baseCurrency = currency,
                selectedCurrency = if (current.selectedCurrency == current.baseCurrency) currency else current.selectedCurrency
            )
        }.launchIn(viewModelScope)

        // Maintain a category → limit progress map for the CategorySelector rings.
        viewModelScope.launch {
            val (start, end) = DateRangeHelper.getDateRange(
                com.example.budgetcontrol.core.domain.model.PeriodType.MONTH,
                periodOffset = 0
            )
            combine(
                categoryLimitRepository.getAllLimits(),
                expenseRepository.getSpentByCategoryInRange(start, end)
            ) { limits, spends ->
                val spentByCat = spends.associate { it.categoryId to it.spent }
                val progress = limits.associate { limit ->
                    val spent = spentByCat[limit.categoryId] ?: 0.0
                    val remaining = limit.amount - spent
                    val frac = if (limit.amount > 0.0) {
                        (spent / limit.amount).toFloat().coerceIn(0f, 2f)
                    } else 0f
                    limit.categoryId to com.example.budgetcontrol.core.domain.model.CategoryLimitProgress(
                        limit = limit.amount,
                        spent = spent,
                        remaining = remaining,
                        fraction = frac
                    )
                }
                val limitsByCat = limits.associateBy { it.categoryId }
                progress to limitsByCat
            }.collect { (progress, limitsByCat) ->
                _uiState.value = _uiState.value.copy(
                    limitProgressMap = progress,
                    categoryLimits = limitsByCat
                )
            }
        }
    }

    private fun checkNetworkStatus() {
        viewModelScope.launch {
            val hasInternet = networkStatusRepository.isInternetAvailable()
            val cachedRates = preferencesManager.getLastRates().firstOrNull() ?: emptyMap()
            // Foreign currency only makes sense when we have at least one rate to convert with.
            // Stale rates are still usable here — the user is warned separately via staleRateWarning.
            val foreignDisabled = !hasInternet && cachedRates.isEmpty()
            val disabledMessage = if (foreignDisabled) {
                context.getString(R.string.foreign_currency_requires_internet)
            } else null

            val status = if (!hasInternet) {
                if (cachedRates.isNotEmpty()) NetworkStatus.NO_INTERNET
                else NetworkStatus.OFFLINE_NO_CACHE
            } else {
                val cerpsUp = networkStatusRepository.isCerpsAvailable()
                if (cerpsUp) NetworkStatus.ONLINE else NetworkStatus.SERVICE_UNAVAILABLE
            }

            _uiState.value = _uiState.value.copy(
                networkStatus = status,
                foreignCurrencyDisabled = foreignDisabled,
                foreignCurrencyDisabledMessage = disabledMessage
            )
        }
    }

    /**
     * Initializes the ViewModel for add or edit mode
     */
    fun initialize(
        mode: TransactionFormMode,
        type: TransactionType,
        transactionId: String? = null,
        initialDate: Long = System.currentTimeMillis(),
        accountId: String? = null
    ) {
        _uiState.value = _uiState.value.copy(
            mode = mode,
            transactionType = type,
            selectedDate = minOf(initialDate, System.currentTimeMillis()),
            isLoading = true,
            canChangeType = mode == TransactionFormMode.EDIT,
            selectedAccountId = accountId ?: _uiState.value.selectedAccountId
        )

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
     * Loads available currencies from CERPS (with in-memory caching)
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
                    val all = result.data.ifEmpty { listOf(_uiState.value.baseCurrency) }
                    cachedCurrencies = all
                    _uiState.value = _uiState.value.copy(
                        availableCurrencies = all,
                        isCurrenciesLoading = false,
                        currenciesError = null
                    )
                }
                is CerpsResult.Error -> {
                    // Fall back to base currency only when CERPS is unavailable
                    _uiState.value = _uiState.value.copy(
                        availableCurrencies = listOf(_uiState.value.baseCurrency),
                        isCurrenciesLoading = false,
                        currenciesError = context.getString(R.string.error_currency_service_unavailable)
                    )
                }
            }
        }
    }

    fun selectCurrency(currency: String) {
        val current = _uiState.value
        val accountCurrency = accountCurrency()

        // Block picking a foreign currency when we can't convert (fully offline, no cache).
        // Fall back silently to the account's own currency so the form stays usable.
        if (currency != accountCurrency && current.foreignCurrencyDisabled) {
            _uiState.value = current.copy(selectedCurrency = accountCurrency)
            return
        }

        viewModelScope.launch {
            val lastMethod = if (currency != accountCurrency) {
                preferencesManager.lastPaymentMethodFlow.firstOrNull() ?: "CARD"
            } else "CARD"

            val defaultBank = if (currency != accountCurrency) {
                current.availableBanks.firstOrNull { it.isDefault }
                    ?: current.availableBanks.firstOrNull()
            } else null

            _uiState.value = current.copy(
                selectedCurrency = currency,
                selectedBank = defaultBank,
                convertedAmountPreview = "",
                isExactAmountEnabled = if (currency == accountCurrency) false else current.isExactAmountEnabled,
                exactEurAmount = if (currency == accountCurrency) "" else current.exactEurAmount,
                paymentMethod = if (currency == accountCurrency) "CARD" else lastMethod,
                cashRateState = CashRateState(),
                showError = null,
                staleRateWarning = null
            )

            if (currency != accountCurrency) {
                if (lastMethod == "CARD") {
                    fetchRateAndUpdatePreview(currency, current.amount, defaultBank)
                } else {
                    loadLastCashExchange(currency)
                    loadCurrentInterbankRate(currency)
                }
                checkNetworkStatus()
            } else {
                _uiState.value = _uiState.value.copy(networkStatus = NetworkStatus.ONLINE)
            }
        }
    }

    fun selectPaymentMethod(method: String) {
        val current = _uiState.value
        // Cash mode has no "exact amount" affordance — clear any leftover state from Card mode
        // so saveNewExpense / saveNewIncome don't take the manual-base-amount branch.
        _uiState.value = current.copy(
            paymentMethod = method,
            showError = null,
            isExactAmountEnabled = if (method == "CASH") false else current.isExactAmountEnabled,
            exactEurAmount = if (method == "CASH") "" else current.exactEurAmount
        )
        if (method == "CASH" && current.selectedCurrency != current.baseCurrency) {
            loadLastCashExchange(current.selectedCurrency)
            loadCurrentInterbankRate(current.selectedCurrency)
        }
        viewModelScope.launch {
            preferencesManager.setLastPaymentMethod(method)
        }
    }

    fun updateCashRate(rate: String) {
        _uiState.update {
            it.copy(
                cashRateState = it.cashRateState.copy(cashRate = rate),
                showError = null
            )
        }
    }

    fun selectCashRateMode(mode: CashRateMode) {
        _uiState.update {
            it.copy(cashRateState = it.cashRateState.copy(cashRateMode = mode))
        }
        resolveCashRateForMode()
    }

    fun toggleSaveExchangeRecord() {
        _uiState.update {
            it.copy(
                cashRateState = it.cashRateState.copy(
                    saveExchangeRecord = !it.cashRateState.saveExchangeRecord
                )
            )
        }
    }

    fun updateExchangeRecordDescription(text: String) {
        _uiState.update {
            it.copy(cashRateState = it.cashRateState.copy(exchangeRecordDescription = text))
        }
    }

    /**
     * Persists a cash-exchange record when the user opted in via the MANUAL-mode
     * "Save as exchange record" checkbox. Stored as 1 base → toAmount foreign so
     * the LAST_EXCHANGE lookup picks it up on the next transaction.
     */
    private suspend fun maybePersistExchangeRecord(state: TransactionFormUiState) {
        val cash = state.cashRateState
        if (cash.cashRateMode != CashRateMode.MANUAL) return
        if (!cash.saveExchangeRecord) return
        val rate = cash.cashRate.replace(',', '.').toDoubleOrNull() ?: return
        if (rate <= 0.0) return
        currencyExchangeRepository.insertExchange(
            CurrencyExchange(
                id = UUID.randomUUID().toString(),
                fromAmount = 1.0,
                fromCurrency = state.baseCurrency,
                toAmount = rate,
                toCurrency = state.selectedCurrency,
                exchangeRate = rate,
                location = cash.exchangeRecordDescription,
                date = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Subscribes to all stored cash exchanges for the (base → currency) pair.
     * The Flow updates `availableCashExchanges` and keeps the picker in sync
     * when the user records a new exchange (via MANUAL-mode save) or edits
     * records from Settings → Currency Exchange while the form is open.
     */
    private fun loadLastCashExchange(currency: String) {
        cashHistoryJob?.cancel()
        val baseCurrency = _uiState.value.baseCurrency
        cashHistoryJob = viewModelScope.launch {
            currencyExchangeRepository.getExchangesForCurrency(baseCurrency, currency)
                .collect { exchanges ->
                    _uiState.update { state ->
                        val cash = state.cashRateState
                        // Preserve the user's selection if it's still in the list; otherwise
                        // fall back to the most recent record.
                        val resolvedId = cash.selectedCashExchangeId
                            ?.takeIf { id -> exchanges.any { it.id == id } }
                            ?: exchanges.firstOrNull()?.id
                        val selected = exchanges.firstOrNull { it.id == resolvedId }
                        state.copy(
                            cashRateState = cash.copy(
                                availableCashExchanges = exchanges,
                                selectedCashExchangeId = resolvedId,
                                lastCashExchange = selected,
                                isLastExchangeAvailable = exchanges.isNotEmpty()
                            )
                        )
                    }
                    resolveCashRateForMode()
                }
        }
    }

    fun selectCashExchange(id: String) {
        val state = _uiState.value
        val exchange = state.cashRateState.availableCashExchanges.firstOrNull { it.id == id } ?: return
        _uiState.update {
            it.copy(
                cashRateState = it.cashRateState.copy(
                    selectedCashExchangeId = id,
                    lastCashExchange = exchange
                )
            )
        }
        resolveCashRateForMode()
    }

    /**
     * Resolves the live interbank rate for the (base → currency) pair.
     * Fast path: in-memory cache for the same currency. Otherwise CERPS.
     * Updates [CashRateState.isCurrentRateAvailable] and [CashRateState.isCurrentRateLoading].
     */
    private fun loadCurrentInterbankRate(currency: String) {
        viewModelScope.launch {
            val baseCurrency = _uiState.value.baseCurrency

            // Fast path: cached rate for this currency
            if (cachedRateCurrency == currency && cachedInterBankRate != null) {
                _uiState.update {
                    it.copy(
                        cashRateState = it.cashRateState.copy(
                            isCurrentRateAvailable = true,
                            isCurrentRateLoading = false
                        )
                    )
                }
                resolveCashRateForMode()
                return@launch
            }

            _uiState.update {
                it.copy(cashRateState = it.cashRateState.copy(isCurrentRateLoading = true))
            }

            when (val result = cerpsRepository.convert(baseCurrency, currency, 1.0)) {
                is CerpsResult.Success -> {
                    val rate = result.data.exchangeRate.toDouble()
                    cachedInterBankRate = rate
                    cachedRateCurrency = currency
                    _uiState.update {
                        it.copy(
                            cashRateState = it.cashRateState.copy(
                                isCurrentRateAvailable = true,
                                isCurrentRateLoading = false
                            )
                        )
                    }
                }
                is CerpsResult.Error -> {
                    _uiState.update {
                        it.copy(
                            cashRateState = it.cashRateState.copy(
                                isCurrentRateAvailable = false,
                                isCurrentRateLoading = false
                            )
                        )
                    }
                }
            }
            resolveCashRateForMode()
        }
    }

    /**
     * Applies the rate that corresponds to the active [CashRateMode] to the displayed
     * cashRate / cashRatePlaceholder. If the active mode is unavailable
     * (e.g. LAST_EXCHANGE chosen but no records exist), falls back per:
     *   LAST_EXCHANGE → CURRENT_RATE → MANUAL.
     */
    private fun resolveCashRateForMode() {
        val state = _uiState.value
        val cash = state.cashRateState

        val effectiveMode = when {
            cash.cashRateMode == CashRateMode.LAST_EXCHANGE && !cash.isLastExchangeAvailable ->
                if (cash.isCurrentRateAvailable) CashRateMode.CURRENT_RATE else CashRateMode.MANUAL
            cash.cashRateMode == CashRateMode.CURRENT_RATE && !cash.isCurrentRateAvailable ->
                if (cash.isLastExchangeAvailable) CashRateMode.LAST_EXCHANGE else CashRateMode.MANUAL
            else -> cash.cashRateMode
        }

        val rate: Double? = when (effectiveMode) {
            CashRateMode.LAST_EXCHANGE -> cash.lastCashExchange?.exchangeRate
            CashRateMode.CURRENT_RATE ->
                if (cachedRateCurrency == state.selectedCurrency) cachedInterBankRate else null
            CashRateMode.MANUAL -> null
        }

        val formatted = if (rate != null) String.format(java.util.Locale.US, "%.4f", rate) else ""

        _uiState.update {
            it.copy(
                cashRateState = it.cashRateState.copy(
                    cashRateMode = effectiveMode,
                    cashRate = if (effectiveMode == CashRateMode.MANUAL) "" else formatted,
                    cashRatePlaceholder = formatted
                )
            )
        }
    }

    fun selectBank(bank: Bank) {
        val current = _uiState.value
        _uiState.value = current.copy(selectedBank = bank, convertedAmountPreview = "")
        if (current.selectedCurrency != accountCurrency()) {
            fetchRateAndUpdatePreview(current.selectedCurrency, current.amount, bank)
        }
    }

    fun selectAccount(accountId: String) {
        val current = _uiState.value
        _uiState.value = current.copy(selectedAccountId = accountId)
        val account = current.accounts.find { it.account.id == accountId }?.account ?: return
        if (account.currency != current.selectedCurrency) {
            selectCurrency(account.currency)
        } else {
            // New account's currency matches the transaction currency → home currency, clear bank/preview.
            _uiState.value = _uiState.value.copy(
                selectedBank = null,
                convertedAmountPreview = "",
                staleRateWarning = null,
                isExactAmountEnabled = false,
                exactEurAmount = ""
            )
        }
    }

    /**
     * Fetches interbank rate and updates the conversion preview.
     *
     * CERPS returns rate as "1 base = X foreign units".
     * Formula: convertedAmount = originalAmount / realRate
     * Example: 150 BYN / 3.48 ~ 43 EUR
     */
    private fun fetchRateAndUpdatePreview(currency: String, amount: String, bank: Bank?) {
        if (bank == null) return
        if (currency == accountCurrency()) return
        viewModelScope.launch {
            // Use cached rate if currency unchanged
            val interBankRate: Double? = if (cachedRateCurrency == currency && cachedInterBankRate != null) {
                cachedInterBankRate
            } else {
                when (val result = cerpsRepository.ensureRatesLoaded()) {
                    is CerpsResult.Success -> {
                        val rate = result.data[currency]
                        if (rate != null) {
                            cachedInterBankRate = rate
                            cachedRateCurrency = currency
                        }
                        rate
                    }
                    is CerpsResult.Error -> null
                }
            }

            // Show stale rate warning if API failed and cached rates are older than 8 hours
            val staleWarning = if (interBankRate != null && cerpsRepository.areRatesStale()) {
                val ageMs = System.currentTimeMillis() - cerpsRepository.getRatesTimestamp()
                val ageHours = (ageMs / HOUR_IN_MS).toInt()
                context.getString(R.string.stale_rate_warning_improved, ageHours.toString())
            } else null

            if (interBankRate != null) {
                _uiState.value = _uiState.value.copy(
                    convertedAmountPreview = buildPreview(amount, interBankRate, bank),
                    staleRateWarning = staleWarning
                )
            }
        }
    }

    /**
     * Builds the conversion preview string.
     * realRate = interBankRate * (1 - commission/100)
     * convertedAmount = originalAmount / realRate
     */
    private fun buildPreview(amountStr: String, interBankRate: Double, bank: Bank): String {
        val amount = amountStr.replace(',', '.').toDoubleOrNull() ?: return ""
        if (amount <= 0) return ""
        val realRate = interBankRate * (1.0 - bank.commissionPercent / 100.0)
        if (realRate <= 0) return ""
        val converted = amount / realRate
        val realRateFormatted = String.format(java.util.Locale.US, "%.4f", realRate)
        val convertedFormatted = String.format(java.util.Locale.US, "%.2f", converted)
        val commissionFormatted = bank.commissionPercent.let {
            if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
        }
        return context.getString(R.string.conversion_preview_format, convertedFormatted, realRateFormatted, commissionFormatted)
    }

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
                        categories.firstOrNull { it.type == CategoryType.EXPENSE }
                    }

                    val initialIncomeCategory = if (type == TransactionType.INCOME) {
                        selectedCategory
                    } else {
                        categories.firstOrNull { it.type == CategoryType.INCOME }
                    }

                    val current = _uiState.value

                    // Restore bank info for expense or income
                    val restoredCurrency = expense?.originalCurrency
                        ?: income?.originalCurrency
                        ?: _uiState.value.baseCurrency
                    val restoredBankName = expense?.bankName ?: income?.bankName

                    val origAmount = expense?.originalAmount ?: income?.originalAmount ?: transaction.amount

                    val restoredAccountId = expense?.accountId
                        ?: income?.accountId
                        ?: Account.DEFAULT_ACCOUNT_ID

                    val restoredAccountCurrency = current.accounts.find { it.account.id == restoredAccountId }
                        ?.account?.currency ?: current.baseCurrency

                    val restoredBank = if (restoredBankName != null && restoredCurrency != restoredAccountCurrency) {
                        current.availableBanks.firstOrNull { it.name == restoredBankName }
                    } else null

                    _uiState.value = current.copy(
                        categories = filteredCategories,
                        selectedCategory = selectedCategory,
                        amount = origAmount.toString(),
                        description = transaction.description ?: "",
                        selectedDate = transaction.date,
                        originalTransaction = transaction,
                        selectedExpenseCategory = initialExpenseCategory,
                        selectedIncomeCategory = initialIncomeCategory,
                        selectedCurrency = restoredCurrency,
                        selectedBank = restoredBank,
                        originalAmount = origAmount,
                        selectedAccountId = restoredAccountId,
                        isLoading = false
                    )

                    // Only fetch interbank preview if the transaction currency is foreign to its account.
                    if (restoredCurrency != restoredAccountCurrency && restoredBank != null) {
                        val previewAmount = expense?.originalAmount ?: income?.originalAmount ?: transaction.amount
                        fetchRateAndUpdatePreview(restoredCurrency, previewAmount.toString(), restoredBank)
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
        if (current.selectedCurrency == accountCurrency()) {
            _uiState.value = current.copy(
                amount = filteredAmount,
                convertedAmountPreview = "",
                showError = null
            )
            return
        }
        val preview = if (current.selectedBank != null
            && cachedInterBankRate != null && cachedRateCurrency == current.selectedCurrency
        ) {
            buildPreview(filteredAmount, cachedInterBankRate ?: return, current.selectedBank)
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
            showError = null,
            selectedCategoryLimit = null,
            selectedCategoryMonthSpend = 0.0
        )

        observeLimitForCategory(category)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeLimitForCategory(category: Category) {
        limitObservationJob?.cancel()
        // Income categories don't carry spending limits.
        if (category.type != CategoryType.EXPENSE) return
        limitObservationJob = viewModelScope.launch {
            val (start, end) = DateRangeHelper.getDateRange(
                com.example.budgetcontrol.core.domain.model.PeriodType.MONTH,
                periodOffset = 0
            )
            categoryLimitRepository.getLimit(category.id)
                .flatMapLatest { limit ->
                    if (limit == null) {
                        // Explicit type pins the LUB at flatMapLatest's combiner site —
                        // without it, branch 1 infers Flow<Pair<Nothing?, Double>> and the
                        // union with branch 2 can become awkward depending on smart-casts.
                        flowOf<Pair<CategoryLimit?, Double>>(null to 0.0)
                    } else {
                        expenseRepository.getSpentByCategoryInRange(start, end).map { spends ->
                            val spent = spends.firstOrNull { it.categoryId == category.id }?.spent ?: 0.0
                            limit to spent
                        }
                    }
                }
                .collectLatest { (limit, spent) ->
                    _uiState.value = _uiState.value.copy(
                        selectedCategoryLimit = limit,
                        selectedCategoryMonthSpend = spent
                    )
                }
        }
    }

    fun updateDate(date: Long) {
        val clamped = minOf(date, System.currentTimeMillis())
        _uiState.value = _uiState.value.copy(
            selectedDate = clamped,
            showError = null
        )
    }

    fun saveTransaction() {
        val currentState = _uiState.value

        val validationResult = ValidationHelper.validateTransaction(
            context = context,
            amount = currentState.amount,
            category = currentState.selectedCategory,
            selectedDate = currentState.selectedDate
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
                                currentState.selectedCurrency != accountCurrencyFor(currentState)
                        when (currentState.transactionType) {
                            TransactionType.EXPENSE -> saveNewExpense(currentState, amountDouble, isCashMode)
                            TransactionType.INCOME -> saveNewIncome(currentState, amountDouble, isCashMode)
                        }
                        return@launch
                    }
                    TransactionFormMode.EDIT -> {
                        handleEditMode(currentState, amountDouble)
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

    /**
     * Handles saving a new expense in ADD mode.
     * Covers cash+exact, cash+rate, card+exact, and regular paths.
     */
    private suspend fun saveNewExpense(
        state: TransactionFormUiState,
        amountDouble: Double,
        isCashMode: Boolean
    ) {
        val baseCurrency = state.baseCurrency
        val accountCurrency = accountCurrencyFor(state)
        val isHomeCurrency = state.selectedCurrency == accountCurrency
        val result = if (isCashMode &&
            state.isExactAmountEnabled &&
            state.exactEurAmount.isNotBlank()
        ) {
            // Cash mode with manual base-currency amount
            val exactBaseCash = state.exactEurAmount.replace(',', '.').toDoubleOrNull()
            if (exactBaseCash == null || exactBaseCash <= 0) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = context.getString(R.string.error_enter_valid_eur)
                )
                return
            }
            addTransactionUseCase.addWithExactBaseAmount(
                type = TransactionType.EXPENSE,
                originalAmount = amountDouble,
                originalCurrency = state.selectedCurrency,
                exactBaseAmount = exactBaseCash,
                categoryId = state.selectedCategory?.id ?: return,
                description = state.description.ifBlank { null },
                date = state.selectedDate,
                bankName = null,
                bankCommission = null,
                rateSource = RateSource.CASH_EXCHANGE.name,
                accountId = state.selectedAccountId
            )
        } else if (isCashMode) {
            // Cash mode: use the cash rate directly
            val cashRateValue = state.cashRateState.cashRate.replace(',', '.').toDoubleOrNull()
                ?: state.cashRateState.cashRatePlaceholder.replace(',', '.').toDoubleOrNull()
            if (cashRateValue == null || cashRateValue <= 0) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = context.getString(R.string.error_enter_valid_eur)
                )
                return
            }
            val baseAmount = amountDouble / cashRateValue
            addTransactionUseCase.addWithExactBaseAmount(
                type = TransactionType.EXPENSE,
                originalAmount = amountDouble,
                originalCurrency = state.selectedCurrency,
                exactBaseAmount = String.format(java.util.Locale.US, "%.2f", baseAmount).toDouble(),
                categoryId = state.selectedCategory?.id ?: return,
                description = state.description.ifBlank { null },
                date = state.selectedDate,
                bankName = null,
                bankCommission = null,
                rateSource = RateSource.CASH_EXCHANGE.name,
                accountId = state.selectedAccountId
            )
        } else if (
            state.isExactAmountEnabled &&
            state.exactEurAmount.isNotBlank() &&
            state.selectedCurrency != baseCurrency
        ) {
            val exactBase = state.exactEurAmount.replace(',', '.').toDoubleOrNull()
            if (exactBase == null || exactBase <= 0) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = context.getString(R.string.error_enter_valid_eur)
                )
                return
            }
            addTransactionUseCase.addWithExactBaseAmount(
                type = TransactionType.EXPENSE,
                originalAmount = amountDouble,
                originalCurrency = state.selectedCurrency,
                exactBaseAmount = exactBase,
                categoryId = state.selectedCategory?.id ?: return,
                description = state.description.ifBlank { null },
                date = state.selectedDate,
                bankName = state.selectedBank?.name,
                bankCommission = state.selectedBank?.commissionPercent,
                accountId = state.selectedAccountId
            )
        } else {
            addTransactionUseCase(
                type = TransactionType.EXPENSE,
                amount = amountDouble,
                currency = state.selectedCurrency,
                categoryId = state.selectedCategory?.id ?: return,
                description = state.description.ifBlank { null },
                date = state.selectedDate,
                bankName = if (isHomeCurrency) null else state.selectedBank?.name,
                bankCommission = if (isHomeCurrency) null else state.selectedBank?.commissionPercent,
                baseCurrency = baseCurrency,
                accountCurrency = accountCurrency,
                accountId = state.selectedAccountId
            )
        }

        when (result) {
            is AddTransactionResult.Success -> {
                accountRepository.updateLastUsedAt(state.selectedAccountId)
                if (isCashMode) maybePersistExchangeRecord(state)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            }
            is AddTransactionResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = mapExpenseError(result.error)
                )
            }
        }
    }

    /**
     * Handles saving a new income in ADD mode.
     * Covers cash+exact, cash+rate, card+exact, and regular paths.
     */
    private suspend fun saveNewIncome(
        state: TransactionFormUiState,
        amountDouble: Double,
        isCashMode: Boolean
    ) {
        val baseCurrency = state.baseCurrency
        val accountCurrency = accountCurrencyFor(state)
        val isHomeCurrency = state.selectedCurrency == accountCurrency
        val result = if (isCashMode &&
            state.isExactAmountEnabled &&
            state.exactEurAmount.isNotBlank()
        ) {
            // Cash mode with manual base-currency amount
            val exactBaseCash = state.exactEurAmount.replace(',', '.').toDoubleOrNull()
            if (exactBaseCash == null || exactBaseCash <= 0) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = context.getString(R.string.error_enter_valid_eur)
                )
                return
            }
            addTransactionUseCase.addWithExactBaseAmount(
                type = TransactionType.INCOME,
                originalAmount = amountDouble,
                originalCurrency = state.selectedCurrency,
                exactBaseAmount = exactBaseCash,
                categoryId = state.selectedCategory?.id ?: return,
                description = state.description.ifBlank { null },
                date = state.selectedDate,
                bankName = null,
                bankCommission = null,
                rateSource = RateSource.CASH_EXCHANGE.name,
                accountId = state.selectedAccountId
            )
        } else if (isCashMode) {
            // Cash mode: use the cash rate directly
            val cashRateValue = state.cashRateState.cashRate.replace(',', '.').toDoubleOrNull()
                ?: state.cashRateState.cashRatePlaceholder.replace(',', '.').toDoubleOrNull()
            if (cashRateValue == null || cashRateValue <= 0) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = context.getString(R.string.error_enter_valid_eur)
                )
                return
            }
            val baseAmount = amountDouble / cashRateValue
            addTransactionUseCase.addWithExactBaseAmount(
                type = TransactionType.INCOME,
                originalAmount = amountDouble,
                originalCurrency = state.selectedCurrency,
                exactBaseAmount = String.format(java.util.Locale.US, "%.2f", baseAmount).toDouble(),
                categoryId = state.selectedCategory?.id ?: return,
                description = state.description.ifBlank { null },
                date = state.selectedDate,
                bankName = null,
                bankCommission = null,
                rateSource = RateSource.CASH_EXCHANGE.name,
                accountId = state.selectedAccountId
            )
        } else if (
            state.isExactAmountEnabled &&
            state.exactEurAmount.isNotBlank() &&
            state.selectedCurrency != baseCurrency
        ) {
            val exactBase = state.exactEurAmount.replace(',', '.').toDoubleOrNull()
            if (exactBase == null || exactBase <= 0) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = context.getString(R.string.error_enter_valid_eur)
                )
                return
            }
            addTransactionUseCase.addWithExactBaseAmount(
                type = TransactionType.INCOME,
                originalAmount = amountDouble,
                originalCurrency = state.selectedCurrency,
                exactBaseAmount = exactBase,
                categoryId = state.selectedCategory?.id ?: return,
                description = state.description.ifBlank { null },
                date = state.selectedDate,
                bankName = state.selectedBank?.name,
                bankCommission = state.selectedBank?.commissionPercent,
                accountId = state.selectedAccountId
            )
        } else {
            addTransactionUseCase(
                type = TransactionType.INCOME,
                amount = amountDouble,
                currency = state.selectedCurrency,
                categoryId = state.selectedCategory?.id ?: return,
                description = state.description.ifBlank { null },
                date = state.selectedDate,
                bankName = if (isHomeCurrency) null else state.selectedBank?.name,
                bankCommission = if (isHomeCurrency) null else state.selectedBank?.commissionPercent,
                baseCurrency = baseCurrency,
                accountCurrency = accountCurrency,
                accountId = state.selectedAccountId
            )
        }

        when (result) {
            is AddTransactionResult.Success -> {
                accountRepository.updateLastUsedAt(state.selectedAccountId)
                if (isCashMode) maybePersistExchangeRecord(state)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            }
            is AddTransactionResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = mapIncomeError(result.error)
                )
            }
        }
    }

    /**
     * Handles updating or replacing a transaction in EDIT mode.
     */
    private suspend fun handleEditMode(
        state: TransactionFormUiState,
        amountDouble: Double
    ) {
        val originalTransaction = state.originalTransaction ?: run {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showError = context.getString(R.string.error_loading, "Original transaction not found")
            )
            return
        }

        if (state.transactionType == originalTransaction.type) {
            updateExistingTransaction(
                originalTransaction = originalTransaction,
                amount = amountDouble,
                categoryId = state.selectedCategory?.id ?: return,
                description = state.description.ifBlank { null },
                date = state.selectedDate,
                selectedCurrency = state.selectedCurrency,
                bankName = state.selectedBank?.name,
                bankCommission = state.selectedBank?.commissionPercent,
                accountId = state.selectedAccountId
            )
        } else {
            replaceTransactionWithNewType(
                originalTransaction = originalTransaction,
                newType = state.transactionType,
                amount = amountDouble,
                categoryId = state.selectedCategory?.id ?: return,
                description = state.description.ifBlank { null },
                date = state.selectedDate,
                accountId = state.selectedAccountId
            )
        }
    }

    private suspend fun updateExistingTransaction(
        originalTransaction: Transaction,
        amount: Double,
        categoryId: String,
        description: String?,
        date: Long,
        selectedCurrency: String,
        bankName: String? = null,
        bankCommission: Double? = null,
        accountId: String = Account.DEFAULT_ACCOUNT_ID
    ) {
        val state = _uiState.value
        val baseCurrency = state.baseCurrency
        val accountCurrency = state.accounts.find { it.account.id == accountId }
            ?.account?.currency ?: baseCurrency
        val isHomeCurrency = selectedCurrency == accountCurrency
        addTransactionUseCase(
            type = originalTransaction.type,
            amount = amount,
            currency = selectedCurrency,
            categoryId = categoryId,
            description = description,
            date = date,
            bankName = if (isHomeCurrency) null else bankName,
            bankCommission = if (isHomeCurrency) null else bankCommission,
            baseCurrency = baseCurrency,
            accountCurrency = accountCurrency,
            accountId = accountId
        )
        when (originalTransaction) {
            is Transaction.ExpenseTransaction -> expenseRepository.deleteExpenseById(originalTransaction.id)
            is Transaction.IncomeTransaction -> incomeRepository.deleteIncomeById(originalTransaction.id)
        }
        accountRepository.updateLastUsedAt(accountId)
    }

    private suspend fun replaceTransactionWithNewType(
        originalTransaction: Transaction,
        newType: TransactionType,
        amount: Double,
        categoryId: String,
        description: String?,
        date: Long,
        accountId: String = Account.DEFAULT_ACCOUNT_ID
    ) {
        when (originalTransaction) {
            is Transaction.ExpenseTransaction -> {
                expenseRepository.deleteExpense(originalTransaction.toExpense())
            }
            is Transaction.IncomeTransaction -> {
                incomeRepository.deleteIncome(originalTransaction.toIncome())
            }
        }

        val state = _uiState.value
        val baseCurrency = state.baseCurrency
        val accountCurrency = state.accounts.find { it.account.id == accountId }
            ?.account?.currency ?: baseCurrency
        addTransactionUseCase.addInBaseCurrency(
            type = newType,
            amount = amount,
            baseCurrency = baseCurrency,
            categoryId = categoryId,
            description = description,
            date = date,
            accountCurrency = accountCurrency,
            accountId = accountId
        )
        accountRepository.updateLastUsedAt(accountId)
    }

    fun createCategory(
        name: String,
        iconName: String,
        color: String,
        type: CategoryType,
        limitAmount: Double? = null
    ) {
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
            // Persist optional limit before the categories collect blocks below — the repo
            // emission order doesn't matter for correctness, but doing it eagerly avoids a
            // perceptual gap between the new category appearing and its ring rendering.
            if (type == CategoryType.EXPENSE && limitAmount != null && limitAmount > 0.0) {
                categoryLimitRepository.setLimit(newCategory.id, limitAmount)
            }

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

    fun setCategoryLimit(categoryId: String, amount: Double) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            categoryLimitRepository.setLimit(categoryId, amount)
        }
    }

    fun clearCategoryLimit(categoryId: String) {
        viewModelScope.launch {
            categoryLimitRepository.clearLimit(categoryId)
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

        return currentState.amount != currentState.originalAmount.toString() ||
                currentState.description != (original.description ?: "") ||
                currentState.selectedCategory?.id != original.categoryId ||
                currentState.transactionType != original.type ||
                !DateRangeHelper.isSameDay(currentState.selectedDate, original.date)
    }

    private fun mapExpenseError(error: AddTransactionError): String {
        return when (error) {
            is AddTransactionError.SavingFailed ->
                context.getString(R.string.error_saving_expense, error.cause ?: "")
            is AddTransactionError.ConversionFailed -> error.message
            is AddTransactionError.NetworkUnavailable ->
                context.getString(R.string.error_currency_service_unavailable)
            is AddTransactionError.InvalidAmount ->
                context.getString(R.string.validation_amount_positive)
        }
    }

    private fun mapIncomeError(error: AddTransactionError): String {
        return when (error) {
            is AddTransactionError.SavingFailed ->
                context.getString(R.string.error_saving, error.cause ?: "")
            is AddTransactionError.ConversionFailed -> error.message
            is AddTransactionError.NetworkUnavailable ->
                context.getString(R.string.error_currency_service_unavailable)
            is AddTransactionError.InvalidAmount ->
                context.getString(R.string.validation_amount_positive)
        }
    }
}
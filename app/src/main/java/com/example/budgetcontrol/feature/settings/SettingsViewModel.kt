package com.example.budgetcontrol.feature.settings

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Immutable
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.domain.model.Bank
import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.model.CategoryLimit
import com.example.budgetcontrol.core.domain.model.CategoryLimitProgress
import com.example.budgetcontrol.core.domain.model.CategoryType
import com.example.budgetcontrol.core.domain.model.LookupState
import com.example.budgetcontrol.core.domain.model.PeriodType
import com.example.budgetcontrol.core.util.DateRangeHelper
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.util.SUBSCRIPTION_TIMEOUT_MS
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.data.remote.gemini.GeminiRepository
import com.example.budgetcontrol.core.data.remote.gemini.GeminiResult
import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.model.PendingCurrencyChange
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import com.example.budgetcontrol.core.domain.repository.BankRepository
import com.example.budgetcontrol.core.domain.repository.CategoryLimitRepository
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import com.example.budgetcontrol.core.domain.usecase.AccountWithBalance
import com.example.budgetcontrol.core.domain.usecase.ConvertCurrencyResult
import com.example.budgetcontrol.core.domain.usecase.GetAccountsUseCase
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetIncomesUseCase
import com.example.budgetcontrol.core.domain.usecase.UpdateAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@Immutable
data class SettingsUiState(
    val currentLanguage: String = "",
    val currentTheme: String = "light",
    val allCurrencies: List<String> = emptyList(),
    val isCurrenciesLoading: Boolean = false,
    val currenciesError: String? = null,
    val accounts: List<AccountWithBalance> = emptyList(),
    val showCreateEditAccountSheet: Boolean = false,
    val editingAccountId: String? = null,
    val editingAccountTransactionCount: Int = 0,
    val pendingCurrencyChange: PendingCurrencyChange? = null,
    val currencyChangeError: String? = null,
    val showCreateEditCategorySheet: Boolean = false,
    val editingCategoryId: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val bankRepository: BankRepository,
    private val cerpsRepository: CerpsRepository,
    private val geminiRepository: GeminiRepository,
    getAccountsUseCase: GetAccountsUseCase,
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val categoryRepository: CategoryRepository,
    private val categoryLimitRepository: CategoryLimitRepository,
    getExpensesUseCase: GetExpensesUseCase,
    getIncomesUseCase: GetIncomesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val banks: StateFlow<List<Bank>> = bankRepository.getAllBanks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptyList())

    val baseCurrency: StateFlow<String> = preferencesManager.baseCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), DEFAULT_BASE_CURRENCY)

    val favoriteCurrencies: StateFlow<Set<String>> = preferencesManager.favoriteCurrenciesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), PreferencesManager.DEFAULT_FAVORITE_CURRENCIES)

    private val totalExpenses: StateFlow<Double> = getExpensesUseCase()
        .map { expenses -> expenses.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), 0.0)

    private val totalIncomes: StateFlow<Double> = getIncomesUseCase()
        .map { incomes -> incomes.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), 0.0)

    val totalBalance: StateFlow<Double> = combine(
        preferencesManager.initialBalanceFlow,
        totalIncomes,
        totalExpenses
    ) { initial, incomes, expenses ->
        initial + incomes - expenses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), 0.0)

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptyList())

    val categoryLimits: StateFlow<Map<String, CategoryLimit>> = categoryLimitRepository.getAllLimits()
        .map { list -> list.associateBy { it.categoryId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryLimitProgress: StateFlow<Map<String, CategoryLimitProgress>> =
        categoryLimitRepository.getAllLimits()
            .flatMapLatest { limits ->
                val (start, end) = DateRangeHelper.getDateRange(PeriodType.MONTH, periodOffset = 0)
                expenseRepository.getSpentByCategoryInRange(start, end).map { spends ->
                    val spentByCategory = spends.associate { it.categoryId to it.spent }
                    limits.associate { limit ->
                        val spent = spentByCategory[limit.categoryId] ?: 0.0
                        val remaining = limit.amount - spent
                        val frac = if (limit.amount > 0.0) {
                            (spent / limit.amount).toFloat().coerceIn(0f, 2f)
                        } else 0f
                        limit.categoryId to CategoryLimitProgress(
                            limit = limit.amount,
                            spent = spent,
                            remaining = remaining,
                            fraction = frac
                        )
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptyMap())

    init {
        preferencesManager.languageFlow
            .onEach { tag -> _uiState.update { it.copy(currentLanguage = tag) } }
            .launchIn(viewModelScope)

        preferencesManager.themeFlow
            .onEach { theme -> _uiState.update { it.copy(currentTheme = theme) } }
            .launchIn(viewModelScope)

        getAccountsUseCase.getAccountsWithBalances(
            baseCurrencyFlow = preferencesManager.baseCurrencyFlow,
            ratesFlow = preferencesManager.getLastRates()
        )
            .onEach { accounts -> _uiState.update { it.copy(accounts = accounts) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val storedTheme = preferencesManager.themeFlow.first()
            if (storedTheme == "system") {
                val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                preferencesManager.setTheme(if (nightMode == Configuration.UI_MODE_NIGHT_YES) "dark" else "light")
            }
            val storedLang = preferencesManager.languageFlow.first()
            if (storedLang.isEmpty()) {
                val detectedLang = if (Locale.getDefault().language == "ru") "ru" else "en"
                preferencesManager.setLanguage(detectedLang)
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(detectedLang))
            }
        }

        loadCurrencies()
    }

    fun setTotalBalance(newTotal: Double) {
        viewModelScope.launch {
            val newInitialBalance = newTotal - totalIncomes.value + totalExpenses.value
            preferencesManager.setInitialBalance(newInitialBalance)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.setTheme(theme)
        }
    }

    fun setLanguage(tag: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(tag)
            val locales = if (tag.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    fun addBank(name: String, commission: Double) {
        viewModelScope.launch {
            bankRepository.insertBank(
                Bank(name = name, commissionPercent = commission)
            )
        }
    }

    fun updateBank(bank: Bank) {
        viewModelScope.launch {
            bankRepository.updateBank(bank)
        }
    }

    fun deleteBank(bank: Bank) {
        viewModelScope.launch {
            bankRepository.deleteBank(bank)
        }
    }

    fun toggleFavorite(bank: Bank) {
        viewModelScope.launch {
            if (bank.isFavorite) {
                // Prevent unchecking the last favorite
                if (banks.value.count { it.isFavorite } <= 1) return@launch
                if (bank.isDefault) {
                    val newDefault = banks.value.firstOrNull { it.isFavorite && it.id != bank.id }
                    bankRepository.updateBank(bank.copy(isFavorite = false, isDefault = false))
                    newDefault?.let { bankRepository.updateBank(it.copy(isDefault = true)) }
                } else {
                    bankRepository.updateBank(bank.copy(isFavorite = false))
                }
            } else {
                bankRepository.updateBank(bank.copy(isFavorite = true))
            }
        }
    }

    fun setDefaultBank(bank: Bank) {
        viewModelScope.launch {
            // Default must be a favorite — ensure it is
            banks.value.forEach { existing ->
                val newDefault = existing.id == bank.id
                bankRepository.updateBank(
                    existing.copy(
                        isDefault = newDefault,
                        isFavorite = if (newDefault) true else existing.isFavorite
                    )
                )
            }
        }
    }

    fun resetBanksToDefaults() {
        viewModelScope.launch {
            bankRepository.deleteAllBanks()
            bankRepository.insertDefaultBanks()
        }
    }

    private val _commissionLookupState = MutableStateFlow<LookupState?>(null)
    val commissionLookupState: StateFlow<LookupState?> = _commissionLookupState.asStateFlow()

    fun lookupBankCommission(bankName: String) {
        if (bankName.isBlank()) return
        viewModelScope.launch {
            _commissionLookupState.value = LookupState.Loading
            _commissionLookupState.value = when (val result = geminiRepository.getBankCommission(bankName)) {
                is GeminiResult.Success -> LookupState.Success(result.commission)
                is GeminiResult.NotFound -> LookupState.NotFound
                is GeminiResult.Error -> LookupState.Error(result.message)
            }
        }
    }

    fun resetCommissionLookup() {
        _commissionLookupState.value = null
    }

    private fun loadCurrencies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCurrenciesLoading = true)
            when (val result = cerpsRepository.getCurrencies()) {
                is CerpsResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        allCurrencies = result.data,
                        isCurrenciesLoading = false,
                        currenciesError = null
                    )
                }
                is CerpsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isCurrenciesLoading = false,
                        currenciesError = result.message
                    )
                }
            }
        }
    }

    fun toggleFavoriteCurrency(code: String) {
        viewModelScope.launch {
            val current = favoriteCurrencies.value
            if (current.contains(code)) {
                preferencesManager.setFavoriteCurrencies(current - code)
            } else {
                preferencesManager.setFavoriteCurrencies(current + code)
            }
        }
    }

    // ── Account management ────────────────────────────────────────────

    fun showCreateAccountSheet() {
        _uiState.update {
            it.copy(
                showCreateEditAccountSheet = true,
                editingAccountId = null,
                editingAccountTransactionCount = 0
            )
        }
    }

    fun showEditAccountSheet(accountId: String) {
        viewModelScope.launch {
            val expenseCount = expenseRepository.getExpenseCountByAccount(accountId)
            val incomeCount = incomeRepository.getIncomeCountByAccount(accountId)
            _uiState.update {
                it.copy(
                    showCreateEditAccountSheet = true,
                    editingAccountId = accountId,
                    editingAccountTransactionCount = expenseCount + incomeCount
                )
            }
        }
    }

    fun dismissCreateEditAccountSheet() {
        _uiState.update {
            it.copy(
                showCreateEditAccountSheet = false,
                editingAccountId = null,
                editingAccountTransactionCount = 0,
                pendingCurrencyChange = null,
                currencyChangeError = null
            )
        }
    }

    fun createAccount(name: String, iconName: String, color: String, initialBalance: Double, currency: String) {
        viewModelScope.launch {
            val account = Account(
                id = UUID.randomUUID().toString(),
                name = name,
                iconName = iconName,
                color = color,
                initialBalance = initialBalance,
                currency = currency,
                isDefault = false,
                createdAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis(),
                sortOrder = 0
            )
            accountRepository.insertAccount(account)
            dismissCreateEditAccountSheet()
        }
    }

    fun updateAccount(name: String, iconName: String, color: String, initialBalance: Double, currency: String) {
        val accountId = _uiState.value.editingAccountId ?: return
        viewModelScope.launch {
            val existing = accountRepository.getAccountById(accountId) ?: return@launch
            if (existing.currency != currency) {
                when (val preview = updateAccountUseCase.previewConversion(existing.currency, currency, initialBalance)) {
                    is ConvertCurrencyResult.Success -> {
                        _uiState.update {
                            it.copy(
                                pendingCurrencyChange = PendingCurrencyChange(
                                    accountId = accountId,
                                    name = name,
                                    iconName = iconName,
                                    color = color,
                                    fromCurrency = existing.currency,
                                    toCurrency = currency,
                                    oldInitialBalance = initialBalance,
                                    newInitialBalance = preview.conversion.convertedAmount,
                                    exchangeRate = preview.conversion.exchangeRate
                                ),
                                currencyChangeError = null
                            )
                        }
                    }
                    is ConvertCurrencyResult.Error -> {
                        _uiState.update { it.copy(currencyChangeError = preview.message) }
                    }
                }
                return@launch
            }
            commitAccountUpdate(existing, name, iconName, color, initialBalance, currency)
        }
    }

    fun confirmPendingCurrencyChange() {
        val pending = _uiState.value.pendingCurrencyChange ?: return
        viewModelScope.launch {
            val existing = accountRepository.getAccountById(pending.accountId) ?: return@launch
            commitAccountUpdate(
                existing = existing,
                name = pending.name,
                iconName = pending.iconName,
                color = pending.color,
                initialBalance = pending.oldInitialBalance,
                currency = pending.toCurrency
            )
            _uiState.update { it.copy(pendingCurrencyChange = null) }
        }
    }

    fun cancelPendingCurrencyChange() {
        _uiState.update { it.copy(pendingCurrencyChange = null) }
    }

    private suspend fun commitAccountUpdate(
        existing: Account,
        name: String,
        iconName: String,
        color: String,
        initialBalance: Double,
        currency: String
    ) {
        when (val result = updateAccountUseCase(existing, name, iconName, color, initialBalance, currency)) {
            is UpdateAccountUseCase.Result.Success -> dismissCreateEditAccountSheet()
            UpdateAccountUseCase.Result.CurrencyChangeBlocked -> {
                _uiState.update { it.copy(currencyChangeError = "Currency change is blocked by existing transactions") }
            }
            is UpdateAccountUseCase.Result.ConversionFailed -> {
                _uiState.update { it.copy(currencyChangeError = result.message) }
            }
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            val account = accountRepository.getAccountById(accountId) ?: return@launch
            if (account.isDefault) return@launch

            expenseRepository.reassignExpenses(accountId, Account.DEFAULT_ACCOUNT_ID)
            incomeRepository.reassignIncomes(accountId, Account.DEFAULT_ACCOUNT_ID)
            accountRepository.deleteAccount(account)
            dismissCreateEditAccountSheet()
        }
    }

    fun getEditingAccount(): Account? {
        val id = _uiState.value.editingAccountId ?: return null
        return _uiState.value.accounts.find { it.account.id == id }?.account
    }

    // ── Category management ───────────────────────────────────────────

    fun showCreateCategorySheet() {
        _uiState.update {
            it.copy(showCreateEditCategorySheet = true, editingCategoryId = null)
        }
    }

    fun showEditCategorySheet(categoryId: String) {
        _uiState.update {
            it.copy(showCreateEditCategorySheet = true, editingCategoryId = categoryId)
        }
    }

    fun dismissCategorySheet() {
        _uiState.update {
            it.copy(showCreateEditCategorySheet = false, editingCategoryId = null)
        }
    }

    fun createCategory(
        name: String,
        iconName: String,
        color: String,
        type: CategoryType,
        limitAmount: Double? = null
    ) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            categoryRepository.insertCategory(
                Category(
                    id = newId,
                    name = name,
                    iconName = iconName,
                    color = color,
                    type = type,
                    isSystem = false,
                    isDefault = false
                )
            )
            // Limit persistence is gated on type=EXPENSE inside the bottom sheet, but we
            // double-check here so a future caller can't smuggle a limit onto an income.
            if (type == CategoryType.EXPENSE && limitAmount != null && limitAmount > 0.0) {
                categoryLimitRepository.setLimit(newId, limitAmount)
            }
        }
    }

    fun updateCategory(
        name: String,
        iconName: String,
        color: String,
        limitAmount: Double? = null
    ) {
        val id = _uiState.value.editingCategoryId ?: return
        viewModelScope.launch {
            val existing = categoryRepository.getCategoryById(id) ?: return@launch
            categoryRepository.updateCategory(existing.copy(name = name, iconName = iconName, color = color))
            if (existing.type == CategoryType.EXPENSE) {
                if (limitAmount != null && limitAmount > 0.0) {
                    categoryLimitRepository.setLimit(id, limitAmount)
                } else {
                    // User cleared the field → drop any existing limit. Idempotent when none.
                    categoryLimitRepository.clearLimit(id)
                }
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategoryById(category.id)
        }
    }

    fun getEditingCategory(): Category? {
        val id = _uiState.value.editingCategoryId ?: return null
        return categories.value.find { it.id == id }
    }

    fun resetCategoriesToDefaults() {
        viewModelScope.launch {
            categories.value.forEach { categoryRepository.deleteCategory(it) }
            categoryRepository.initializeDefaultCategories()
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
}

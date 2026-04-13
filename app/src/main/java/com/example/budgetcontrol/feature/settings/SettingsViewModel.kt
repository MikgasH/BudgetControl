package com.example.budgetcontrol.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.domain.model.Bank
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.data.remote.gemini.GeminiRepository
import com.example.budgetcontrol.core.data.remote.gemini.GeminiResult
import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import com.example.budgetcontrol.core.domain.repository.BankRepository
import com.example.budgetcontrol.core.domain.repository.ExpenseRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import com.example.budgetcontrol.core.domain.usecase.AccountWithBalance
import com.example.budgetcontrol.core.domain.usecase.GetAccountsUseCase
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetIncomesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SettingsUiState(
    val currentLanguage: String = "",
    val currentTheme: String = "system",
    val allCurrencies: List<String> = emptyList(),
    val isCurrenciesLoading: Boolean = false,
    val currenciesError: String? = null,
    val accounts: List<AccountWithBalance> = emptyList(),
    val showCreateEditAccountSheet: Boolean = false,
    val editingAccountId: String? = null,
    val editingAccountTransactionCount: Int = 0
)

sealed class LookupState {
    object Idle : LookupState()
    object Loading : LookupState()
    data class Success(val value: Double) : LookupState()
    object NotFound : LookupState()
    data class Error(val message: String? = null) : LookupState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val bankRepository: BankRepository,
    private val cerpsRepository: CerpsRepository,
    private val geminiRepository: GeminiRepository,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    getExpensesUseCase: GetExpensesUseCase,
    getIncomesUseCase: GetIncomesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val banks: StateFlow<List<Bank>> = bankRepository.getAllBanks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val baseCurrency: StateFlow<String> = preferencesManager.baseCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_BASE_CURRENCY)

    val favoriteCurrencies: StateFlow<Set<String>> = preferencesManager.favoriteCurrenciesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferencesManager.DEFAULT_FAVORITE_CURRENCIES)

    private val totalExpenses: StateFlow<Double> = getExpensesUseCase()
        .map { expenses -> expenses.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val totalIncomes: StateFlow<Double> = getIncomesUseCase()
        .map { incomes -> incomes.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalBalance: StateFlow<Double> = combine(
        preferencesManager.initialBalanceFlow,
        totalIncomes,
        totalExpenses
    ) { initial, incomes, expenses ->
        initial + incomes - expenses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        preferencesManager.languageFlow
            .onEach { tag -> _uiState.update { it.copy(currentLanguage = tag) } }
            .launchIn(viewModelScope)

        preferencesManager.themeFlow
            .onEach { theme -> _uiState.update { it.copy(currentTheme = theme) } }
            .launchIn(viewModelScope)

        getAccountsUseCase.getAccountsWithBalances()
            .onEach { accounts -> _uiState.update { it.copy(accounts = accounts) } }
            .launchIn(viewModelScope)

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

    private val _commissionLookupState = MutableStateFlow<LookupState>(LookupState.Idle)
    val commissionLookupState: StateFlow<LookupState> = _commissionLookupState.asStateFlow()

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
        _commissionLookupState.value = LookupState.Idle
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
                editingAccountTransactionCount = 0
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
            val updated = existing.copy(
                name = name,
                iconName = iconName,
                color = color,
                initialBalance = initialBalance,
                currency = currency
            )
            accountRepository.updateAccount(updated)
            dismissCreateEditAccountSheet()
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
}

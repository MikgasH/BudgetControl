package com.example.budgetcontrol.feature.onboarding

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.model.Bank
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.data.remote.gemini.GeminiRepository
import com.example.budgetcontrol.core.data.remote.gemini.GeminiResult
import com.example.budgetcontrol.core.domain.repository.BankRepository
import com.example.budgetcontrol.feature.settings.LookupState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class OnboardingUiState(
    val selectedLanguage: String = "",
    val selectedTheme: String = "system",
    val selectedCurrency: String = DEFAULT_BASE_CURRENCY,
    val initialBalance: String = "",
    val currencies: List<String> = emptyList(),
    val currenciesLoading: Boolean = false,
    val favoriteCurrencies: Set<String> = PreferencesManager.DEFAULT_FAVORITE_CURRENCIES,
    val onboardingAccounts: List<Account> = emptyList()
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val bankRepository: BankRepository,
    private val cerpsRepository: CerpsRepository,
    private val geminiRepository: GeminiRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val banks: StateFlow<List<Bank>> = bankRepository.getAllBanks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        preferencesManager.languageFlow
            .onEach { tag -> _uiState.value = _uiState.value.copy(selectedLanguage = tag) }
            .launchIn(viewModelScope)
        preferencesManager.themeFlow
            .onEach { theme -> _uiState.value = _uiState.value.copy(selectedTheme = theme) }
            .launchIn(viewModelScope)
        loadCurrencies()
        _uiState.value = _uiState.value.copy(
            onboardingAccounts = listOf(
                Account(
                    id = Account.DEFAULT_ACCOUNT_ID,
                    name = context.getString(R.string.main_account),
                    iconName = "account_balance",
                    color = "#4CAF50",
                    isDefault = true,
                    createdAt = System.currentTimeMillis()
                )
            )
        )
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

    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.setTheme(theme)
        }
    }

    private fun loadCurrencies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(currenciesLoading = true)
            when (val result = cerpsRepository.getCurrencies()) {
                is CerpsResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        currencies = result.data,
                        currenciesLoading = false
                    )
                }
                is CerpsResult.Error -> {
                    _uiState.value = _uiState.value.copy(currenciesLoading = false)
                }
            }
        }
    }

    fun setCurrency(code: String) {
        val oldCurrency = _uiState.value.selectedCurrency
        // When the base currency changes, update any onboarding accounts
        // that still use the old base currency (i.e. haven't been customized).
        val updatedAccounts = _uiState.value.onboardingAccounts.map { account ->
            if (account.currency == oldCurrency || account.currency == DEFAULT_BASE_CURRENCY) {
                account.copy(currency = code)
            } else account
        }
        _uiState.value = _uiState.value.copy(
            selectedCurrency = code,
            onboardingAccounts = updatedAccounts
        )
    }

    fun toggleBankFavorite(bank: Bank) {
        viewModelScope.launch {
            if (bank.isFavorite) {
                if (banks.value.count { it.isFavorite } <= 1) return@launch
                bankRepository.updateBank(bank.copy(isFavorite = false))
            } else {
                bankRepository.updateBank(bank.copy(isFavorite = true))
            }
        }
    }

    fun setInitialBalance(amount: String) {
        val balance = amount.toDoubleOrNull() ?: 0.0
        val accounts = _uiState.value.onboardingAccounts.map { account ->
            if (account.isDefault) account.copy(initialBalance = balance) else account
        }
        _uiState.value = _uiState.value.copy(initialBalance = amount, onboardingAccounts = accounts)
    }

    fun toggleFavoriteCurrency(currency: String) {
        val current = _uiState.value.favoriteCurrencies
        val updated = if (current.contains(currency)) {
            if (current.size <= 1) return // Keep at least one
            current - currency
        } else {
            current + currency
        }
        _uiState.value = _uiState.value.copy(favoriteCurrencies = updated)
    }

    fun addBank(name: String, commission: Double) {
        viewModelScope.launch {
            bankRepository.insertBank(
                Bank(name = name, commissionPercent = commission)
            )
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

    fun addOnboardingAccount(name: String, iconName: String, color: String, initialBalance: Double, currency: String) {
        val newAccount = Account(
            id = UUID.randomUUID().toString(),
            name = name,
            iconName = iconName,
            color = color,
            initialBalance = initialBalance,
            currency = currency,
            isDefault = false,
            createdAt = System.currentTimeMillis(),
            sortOrder = _uiState.value.onboardingAccounts.size
        )
        _uiState.value = _uiState.value.copy(
            onboardingAccounts = _uiState.value.onboardingAccounts + newAccount
        )
    }

    fun updateOnboardingAccount(id: String, name: String, iconName: String, color: String, initialBalance: Double, currency: String) {
        val updated = _uiState.value.onboardingAccounts.map { account ->
            if (account.id == id) account.copy(
                name = name, iconName = iconName, color = color,
                initialBalance = initialBalance, currency = currency
            ) else account
        }
        _uiState.value = _uiState.value.copy(onboardingAccounts = updated)
        if (id == Account.DEFAULT_ACCOUNT_ID) {
            val balanceStr = if (initialBalance > 0) {
                if (initialBalance == initialBalance.toLong().toDouble()) initialBalance.toLong().toString()
                else initialBalance.toString()
            } else ""
            _uiState.value = _uiState.value.copy(initialBalance = balanceStr)
        }
    }

    fun removeOnboardingAccount(id: String) {
        if (id == Account.DEFAULT_ACCOUNT_ID) return
        _uiState.value = _uiState.value.copy(
            onboardingAccounts = _uiState.value.onboardingAccounts.filter { it.id != id }
        )
    }

    fun skipAccountSetup() {
        _uiState.value = _uiState.value.copy(
            onboardingAccounts = _uiState.value.onboardingAccounts.filter { it.isDefault }
        )
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val selectedCurrency = _uiState.value.selectedCurrency
            preferencesManager.setBaseCurrency(selectedCurrency)
            // Save selected favorite currencies, ensuring base currency is included
            val favorites = _uiState.value.favoriteCurrencies + selectedCurrency
            preferencesManager.setFavoriteCurrencies(favorites)
            val balance = _uiState.value.initialBalance.toDoubleOrNull() ?: 0.0
            if (balance > 0) {
                preferencesManager.setInitialBalance(balance)
            }
            // Save all accounts — each already carries the correct currency
            // (updated when the user changed base currency or set a custom one).
            for (account in _uiState.value.onboardingAccounts) {
                accountRepository.insertAccount(account)
            }
            preferencesManager.setOnboardingCompleted()
        }
    }
}

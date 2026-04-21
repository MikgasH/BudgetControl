package com.example.budgetcontrol.feature.onboarding

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Immutable
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.model.AccountGroup
import com.example.budgetcontrol.core.domain.model.Bank
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.repository.AccountGroupRepository
import com.example.budgetcontrol.core.domain.repository.AccountRepository
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.util.SUBSCRIPTION_TIMEOUT_MS
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.data.remote.gemini.GeminiRepository
import com.example.budgetcontrol.core.data.remote.gemini.GeminiResult
import com.example.budgetcontrol.core.domain.repository.BankRepository
import com.example.budgetcontrol.feature.settings.LookupState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@Immutable
data class OnboardingUiState(
    val selectedLanguage: String = "",
    val selectedTheme: String = "light",
    val selectedCurrency: String = DEFAULT_BASE_CURRENCY,
    val currencies: List<String> = emptyList(),
    val currenciesLoading: Boolean = false,
    val favoriteCurrencies: Set<String> = PreferencesManager.DEFAULT_FAVORITE_CURRENCIES,
    val onboardingAccounts: List<Account> = emptyList(),
    val onboardingGroups: List<AccountGroup> = emptyList()
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val bankRepository: BankRepository,
    private val cerpsRepository: CerpsRepository,
    private val geminiRepository: GeminiRepository,
    private val accountRepository: AccountRepository,
    private val accountGroupRepository: AccountGroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val banks: StateFlow<List<Bank>> = bankRepository.getAllBanks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptyList())

    init {
        viewModelScope.launch {
            val detectedLanguage = if (Locale.getDefault().language == "ru") "ru" else "en"
            preferencesManager.setLanguage(detectedLanguage)
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(detectedLanguage))

            val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val detectedTheme = if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
            preferencesManager.setTheme(detectedTheme)
        }
        preferencesManager.languageFlow
            .onEach { tag -> _uiState.value = _uiState.value.copy(selectedLanguage = tag) }
            .launchIn(viewModelScope)
        preferencesManager.themeFlow
            .onEach { theme -> _uiState.value = _uiState.value.copy(selectedTheme = theme) }
            .launchIn(viewModelScope)
        loadCurrencies()
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
                    // Offline fallback: let the user pick a base currency from the
                    // hardcoded defaults rather than getting stuck on onboarding.
                    _uiState.value = _uiState.value.copy(
                        currencies = PreferencesManager.DEFAULT_AVAILABLE_CURRENCIES,
                        currenciesLoading = false
                    )
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
        val isFirst = _uiState.value.onboardingAccounts.isEmpty()
        val newAccount = Account(
            id = if (isFirst) Account.DEFAULT_ACCOUNT_ID else UUID.randomUUID().toString(),
            name = name,
            iconName = iconName,
            color = color,
            initialBalance = initialBalance,
            currency = currency,
            isDefault = isFirst,
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
    }

    fun removeOnboardingAccount(id: String) {
        if (id == Account.DEFAULT_ACCOUNT_ID) return
        _uiState.value = _uiState.value.copy(
            onboardingAccounts = _uiState.value.onboardingAccounts.filter { it.id != id }
        )
    }

    fun addOnboardingGroup(name: String, memberAccountIds: List<String>) {
        val group = AccountGroup(
            id = UUID.randomUUID().toString(),
            name = name,
            memberAccountIds = memberAccountIds,
            createdAt = System.currentTimeMillis()
        )
        _uiState.value = _uiState.value.copy(
            onboardingGroups = _uiState.value.onboardingGroups + group
        )
    }

    fun updateOnboardingGroup(groupId: String, name: String, memberAccountIds: List<String>) {
        val updatedGroups = _uiState.value.onboardingGroups.map { group ->
            if (group.id == groupId) group.copy(name = name, memberAccountIds = memberAccountIds)
            else group
        }
        _uiState.value = _uiState.value.copy(onboardingGroups = updatedGroups)
    }

    fun removeOnboardingGroup(groupId: String) {
        _uiState.value = _uiState.value.copy(
            onboardingGroups = _uiState.value.onboardingGroups.filter { it.id != groupId }
        )
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val selectedCurrency = _uiState.value.selectedCurrency
            preferencesManager.setBaseCurrency(selectedCurrency)
            // Save selected favorite currencies, ensuring base currency is included
            val favorites = _uiState.value.favoriteCurrencies + selectedCurrency
            preferencesManager.setFavoriteCurrencies(favorites)
            // Balance is stored on the default account — no separate pref needed
            for (account in _uiState.value.onboardingAccounts) {
                accountRepository.insertAccount(account)
            }
            for (group in _uiState.value.onboardingGroups) {
                accountGroupRepository.insertGroup(group)
            }
            preferencesManager.setOnboardingCompleted()
        }
    }
}

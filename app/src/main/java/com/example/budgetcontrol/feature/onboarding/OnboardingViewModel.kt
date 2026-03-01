package com.example.budgetcontrol.feature.onboarding

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.data.local.database.entities.BankEntity
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.data.remote.gemini.GeminiRepository
import com.example.budgetcontrol.core.data.remote.gemini.GeminiResult
import com.example.budgetcontrol.core.data.repository.BankRepository
import com.example.budgetcontrol.feature.settings.LookupState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val selectedLanguage: String = "",
    val selectedCurrency: String = "EUR",
    val initialBalance: String = "",
    val currencies: List<String> = emptyList(),
    val currenciesLoading: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val bankRepository: BankRepository,
    private val cerpsRepository: CerpsRepository,
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val banks: StateFlow<List<BankEntity>> = bankRepository.getAllBanks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeLanguage()
        loadCurrencies()
    }

    private fun observeLanguage() {
        viewModelScope.launch {
            preferencesManager.languageFlow.collect { tag ->
                _uiState.value = _uiState.value.copy(selectedLanguage = tag)
            }
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
        _uiState.value = _uiState.value.copy(selectedCurrency = code)
    }

    fun toggleBankFavorite(bank: BankEntity) {
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
        _uiState.value = _uiState.value.copy(initialBalance = amount)
    }

    fun addBank(name: String, commission: Double) {
        viewModelScope.launch {
            bankRepository.insertBank(
                BankEntity(name = name, commissionPercent = commission)
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
                is GeminiResult.Error -> LookupState.Error
            }
        }
    }

    fun resetCommissionLookup() {
        _commissionLookupState.value = LookupState.Idle
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val selectedCurrency = _uiState.value.selectedCurrency
            preferencesManager.setBaseCurrency(selectedCurrency)
            // Ensure base currency is always in favorites
            val currentFavorites = preferencesManager.favoriteCurrenciesFlow.firstOrNull()
                ?: PreferencesManager.DEFAULT_FAVORITE_CURRENCIES
            if (!currentFavorites.contains(selectedCurrency)) {
                preferencesManager.setFavoriteCurrencies(currentFavorites + selectedCurrency)
            }
            val balance = _uiState.value.initialBalance.toDoubleOrNull() ?: 0.0
            if (balance > 0) {
                preferencesManager.setInitialBalance(balance)
            }
            preferencesManager.setOnboardingCompleted()
        }
    }
}

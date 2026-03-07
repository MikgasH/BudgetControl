package com.example.budgetcontrol.feature.settings

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
import com.example.budgetcontrol.core.domain.usecase.GetExpensesUseCase
import com.example.budgetcontrol.core.domain.usecase.GetIncomesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currentLanguage: String = "",
    val currentTheme: String = "system",
    val allCurrencies: List<String> = emptyList(),
    val isCurrenciesLoading: Boolean = false,
    val currenciesError: String? = null
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
    getExpensesUseCase: GetExpensesUseCase,
    getIncomesUseCase: GetIncomesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val banks: StateFlow<List<BankEntity>> = bankRepository.getAllBanks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteCurrencies: StateFlow<Set<String>> = preferencesManager.favoriteCurrenciesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferencesManager.DEFAULT_FAVORITE_CURRENCIES)

    private val totalExpenses: StateFlow<Double> = getExpensesUseCase().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    ).let { expensesFlow ->
        combine(expensesFlow) { _ -> expensesFlow.value.sumOf { it.amount } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }

    private val totalIncomes: StateFlow<Double> = getIncomesUseCase().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    ).let { incomesFlow ->
        combine(incomesFlow) { _ -> incomesFlow.value.sumOf { it.amount } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }

    val totalBalance: StateFlow<Double> = combine(
        preferencesManager.initialBalanceFlow,
        totalIncomes,
        totalExpenses
    ) { initial, incomes, expenses ->
        initial + incomes - expenses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        observeLanguage()
        observeTheme()
        loadCurrencies()
    }

    fun setTotalBalance(newTotal: Double) {
        viewModelScope.launch {
            val newInitialBalance = newTotal - totalIncomes.value + totalExpenses.value
            preferencesManager.setInitialBalance(newInitialBalance)
        }
    }

    private fun observeLanguage() {
        viewModelScope.launch {
            preferencesManager.languageFlow.collect { tag ->
                _uiState.value = _uiState.value.copy(currentLanguage = tag)
            }
        }
    }

    private fun observeTheme() {
        viewModelScope.launch {
            preferencesManager.themeFlow.collect { theme ->
                _uiState.value = _uiState.value.copy(currentTheme = theme)
            }
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
                BankEntity(name = name, commissionPercent = commission)
            )
        }
    }

    fun updateBank(bank: BankEntity) {
        viewModelScope.launch {
            bankRepository.updateBank(bank)
        }
    }

    fun deleteBank(bank: BankEntity) {
        viewModelScope.launch {
            bankRepository.deleteBank(bank)
        }
    }

    fun toggleFavorite(bank: BankEntity) {
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

    fun setDefaultBank(bank: BankEntity) {
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

}

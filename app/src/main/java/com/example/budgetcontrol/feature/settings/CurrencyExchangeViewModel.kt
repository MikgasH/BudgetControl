package com.example.budgetcontrol.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.domain.model.CurrencyExchange
import com.example.budgetcontrol.core.domain.repository.CurrencyExchangeRepository
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.util.SUBSCRIPTION_TIMEOUT_MS
import com.example.budgetcontrol.core.util.toDoubleLocale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@Immutable
data class ExchangeFormState(
    val fromAmount: String = "",
    val fromCurrency: String = DEFAULT_BASE_CURRENCY,
    val toAmount: String = "",
    val toCurrency: String = "USD",
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val error: String? = null
)

@HiltViewModel
class CurrencyExchangeViewModel @Inject constructor(
    private val currencyExchangeRepository: CurrencyExchangeRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val exchanges: StateFlow<List<CurrencyExchange>> = currencyExchangeRepository.getAllExchanges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptyList())

    val availableCurrencies: StateFlow<List<String>> = preferencesManager.getAvailableCurrencies()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            PreferencesManager.DEFAULT_AVAILABLE_CURRENCIES
        )

    val baseCurrency: StateFlow<String> = preferencesManager.baseCurrencyFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            DEFAULT_BASE_CURRENCY
        )

    val favoriteCurrencies: StateFlow<Set<String>> = preferencesManager.favoriteCurrenciesFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            emptySet()
        )

    private val _formState = MutableStateFlow(ExchangeFormState())
    val formState: StateFlow<ExchangeFormState> = _formState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.baseCurrencyFlow.collect { currency ->
                if (_formState.value.fromCurrency == DEFAULT_BASE_CURRENCY) {
                    _formState.value = _formState.value.copy(fromCurrency = currency)
                }
            }
        }
    }

    fun updateFromAmount(amount: String) {
        _formState.value = _formState.value.copy(fromAmount = amount, error = null)
    }

    fun updateFromCurrency(currency: String) {
        _formState.value = _formState.value.copy(fromCurrency = currency, error = null)
    }

    fun updateToAmount(amount: String) {
        _formState.value = _formState.value.copy(toAmount = amount, error = null)
    }

    fun updateToCurrency(currency: String) {
        _formState.value = _formState.value.copy(toCurrency = currency, error = null)
    }

    fun updateDescription(description: String) {
        _formState.value = _formState.value.copy(description = description)
    }

    fun updateDate(date: Long) {
        _formState.value = _formState.value.copy(date = date)
    }

    fun saveExchange() {
        val form = _formState.value
        val fromAmount = form.fromAmount.toDoubleLocale()
        val toAmount = form.toAmount.toDoubleLocale()

        if (fromAmount == null || fromAmount <= 0 || toAmount == null || toAmount <= 0) {
            _formState.value = form.copy(error = "enter_amounts")
            return
        }

        // Stored canonically as from=base ("you gave"), to=foreign ("you received").
        // exchangeRate = foreign per base = toAmount / fromAmount, matching the
        // foreign-per-base semantic used by CERPS rates and the cash-mode formula.
        val exchangeRate = toAmount / fromAmount

        viewModelScope.launch {
            currencyExchangeRepository.insertExchange(
                CurrencyExchange(
                    id = UUID.randomUUID().toString(),
                    fromAmount = fromAmount,
                    fromCurrency = form.fromCurrency,
                    toAmount = toAmount,
                    toCurrency = form.toCurrency,
                    exchangeRate = exchangeRate,
                    location = form.description.ifBlank { null },
                    date = form.date,
                    createdAt = System.currentTimeMillis()
                )
            )

            // Reset form after save
            _formState.value = ExchangeFormState(
                fromCurrency = form.fromCurrency,
                toCurrency = form.toCurrency
            )
        }
    }

    fun deleteExchange(id: String) {
        viewModelScope.launch {
            currencyExchangeRepository.deleteExchange(id)
        }
    }
}

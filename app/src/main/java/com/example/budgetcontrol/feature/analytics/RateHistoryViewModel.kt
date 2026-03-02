package com.example.budgetcontrol.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RateHistoryViewModel @Inject constructor(
    private val cerpsRepository: CerpsRepository
) : ViewModel() {

    private val _availableCurrencies = MutableStateFlow<List<String>>(emptyList())
    val availableCurrencies: StateFlow<List<String>> = _availableCurrencies.asStateFlow()

    private val _selectedFrom = MutableStateFlow("USD")
    val selectedFrom: StateFlow<String> = _selectedFrom.asStateFlow()

    private val _selectedTo = MutableStateFlow("EUR")
    val selectedTo: StateFlow<String> = _selectedTo.asStateFlow()

    private val _selectedPeriod = MutableStateFlow("30D")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _trendsData = MutableStateFlow<TrendsResponse?>(null)
    val trendsData: StateFlow<TrendsResponse?> = _trendsData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    companion object {
        val PERIODS = listOf("12H", "1D", "7D", "30D", "90D", "180D", "1Y")
    }

    init {
        loadCurrencies()
        loadTrends()
    }

    private fun loadCurrencies() {
        viewModelScope.launch {
            when (val result = cerpsRepository.getCurrencies()) {
                is CerpsResult.Success -> _availableCurrencies.value = result.data
                is CerpsResult.Error -> { /* currencies list will remain empty */ }
            }
        }
    }

    fun selectFrom(currency: String) {
        _selectedFrom.value = currency
        loadTrends()
    }

    fun selectTo(currency: String) {
        _selectedTo.value = currency
        loadTrends()
    }

    fun selectPeriod(period: String) {
        _selectedPeriod.value = period
        loadTrends()
    }

    private fun loadTrends() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = cerpsRepository.getTrends(
                from = _selectedFrom.value,
                to = _selectedTo.value,
                period = _selectedPeriod.value
            )) {
                is CerpsResult.Success -> {
                    _trendsData.value = result.data
                }
                is CerpsResult.Error -> {
                    _error.value = result.message
                    _trendsData.value = null
                }
            }

            _isLoading.value = false
        }
    }

    fun retry() {
        loadTrends()
    }
}

package com.example.budgetcontrol.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RateHistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cerpsRepository: CerpsRepository
) : ViewModel() {

    private val _availableCurrencies = MutableStateFlow<List<String>>(emptyList())
    val availableCurrencies: StateFlow<List<String>> = _availableCurrencies.asStateFlow()

    private val _selectedFrom = MutableStateFlow("USD")
    val selectedFrom: StateFlow<String> = _selectedFrom.asStateFlow()

    private val _selectedTo = MutableStateFlow("EUR")
    val selectedTo: StateFlow<String> = _selectedTo.asStateFlow()

    private val _selectedPeriod = MutableStateFlow("1D")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _trendsData = MutableStateFlow<TrendsResponse?>(null)
    val trendsData: StateFlow<TrendsResponse?> = _trendsData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    companion object {
        val PERIODS = listOf("1D", "7D", "30D", "90D", "180D")
    }

    init {
        loadCurrencies()
        loadTrends()
    }

    private fun loadCurrencies() {
        viewModelScope.launch {
            when (val result = cerpsRepository.getCurrencies()) {
                is CerpsResult.Success -> _availableCurrencies.value = result.data
                is CerpsResult.Error -> {
                    _error.value = context.getString(R.string.rate_history_error)
                }
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

    fun swapCurrencies() {
        val from = _selectedFrom.value
        _selectedFrom.value = _selectedTo.value
        _selectedTo.value = from
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

            try {
                when (val result = cerpsRepository.getTrends(
                    from = _selectedFrom.value,
                    to = _selectedTo.value,
                    period = _selectedPeriod.value
                )) {
                    is CerpsResult.Success -> {
                        _trendsData.value = result.data
                    }
                    is CerpsResult.Error -> {
                        _error.value = context.getString(R.string.rate_history_error)
                        _trendsData.value = null
                    }
                }
            } catch (_: Exception) {
                _error.value = context.getString(R.string.rate_history_error)
                _trendsData.value = null
            }

            _isLoading.value = false
        }
    }

    fun retry() {
        loadCurrencies()
        loadTrends()
    }
}

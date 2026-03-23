package com.example.budgetcontrol.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RateHistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cerpsRepository: CerpsRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _rawCurrencies = MutableStateFlow<List<String>>(emptyList())

    val baseCurrency: StateFlow<String> = preferencesManager.baseCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_BASE_CURRENCY)

    val favoriteCurrencies: StateFlow<Set<String>> = preferencesManager.favoriteCurrenciesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val availableCurrencies: StateFlow<List<String>> = combine(
        _rawCurrencies,
        preferencesManager.favoriteCurrenciesFlow
    ) { currencies, favorites ->
        val favs = currencies.filter { favorites.contains(it) }.sorted()
        val rest = currencies.filter { !favorites.contains(it) }.sorted()
        favs + rest
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFrom = MutableStateFlow("USD")
    val selectedFrom: StateFlow<String> = _selectedFrom.asStateFlow()

    private val _selectedTo = MutableStateFlow(DEFAULT_BASE_CURRENCY)
    val selectedTo: StateFlow<String> = _selectedTo.asStateFlow()

    private val _selectedPeriod = MutableStateFlow("1D")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _trendsData = MutableStateFlow<TrendsResponse?>(null)
    val trendsData: StateFlow<TrendsResponse?> = _trendsData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _sameCurrencyWarning = MutableStateFlow(false)
    val sameCurrencyWarning: StateFlow<Boolean> = _sameCurrencyWarning.asStateFlow()

    private var trendsJob: Job? = null

    companion object {
        private const val TAG = "RateHistoryVM"
        val PERIODS = listOf("1D", "7D", "30D", "90D", "180D")
    }

    init {
        viewModelScope.launch {
            preferencesManager.baseCurrencyFlow.collect { currency ->
                // Only auto-sync "to" currency if user hasn't manually changed it from the default
                if (_selectedTo.value == DEFAULT_BASE_CURRENCY) {
                    _selectedTo.value = currency
                }
            }
        }
        loadCurrencies()
        loadTrends()
    }

    private fun loadCurrencies() {
        viewModelScope.launch {
            when (val result = cerpsRepository.getCurrencies()) {
                is CerpsResult.Success -> _rawCurrencies.value = result.data
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
        Log.d(TAG, "selectPeriod: $period (was ${_selectedPeriod.value})")
        _selectedPeriod.value = period
        loadTrends()
    }

    private fun loadTrends() {
        trendsJob?.cancel()

        val from = _selectedFrom.value
        val to = _selectedTo.value
        val period = _selectedPeriod.value

        if (from == to) {
            _sameCurrencyWarning.value = true
            _trendsData.value = null
            _error.value = null
            _isLoading.value = false
            return
        }
        _sameCurrencyWarning.value = false
        _isLoading.value = true
        _error.value = null

        Log.d(TAG, "loadTrends: requesting $from→$to period=$period")

        trendsJob = viewModelScope.launch {
            try {
                when (val result = cerpsRepository.getTrends(
                    from = from,
                    to = to,
                    period = period
                )) {
                    is CerpsResult.Success -> {
                        val data = result.data
                        Log.d(TAG, "loadTrends SUCCESS: period=$period, " +
                                "response.period=${data.period}, " +
                                "points=${data.points.size}, " +
                                "dataPoints=${data.dataPoints}, " +
                                "first=${data.points.firstOrNull()?.timestamp}, " +
                                "last=${data.points.lastOrNull()?.timestamp}")
                        _trendsData.value = data
                        _error.value = null
                    }
                    is CerpsResult.Error -> {
                        Log.e(TAG, "loadTrends ERROR: period=$period, msg=${result.message}")
                        _error.value = context.getString(R.string.rate_history_error)
                        _trendsData.value = null
                    }
                }
            } catch (e: CancellationException) {
                // Must rethrow — swallowing CancellationException breaks structured concurrency
                Log.d(TAG, "loadTrends CANCELLED: period=$period")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "loadTrends EXCEPTION: period=$period", e)
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

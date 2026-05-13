package com.example.budgetcontrol.feature.analytics

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetcontrol.BuildConfig
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import com.example.budgetcontrol.core.data.repository.NetworkStatusRepository
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY
import com.example.budgetcontrol.core.util.ONE_DAY_MS
import com.example.budgetcontrol.core.util.SUBSCRIPTION_TIMEOUT_MS
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val DEFAULT_CONVERT_AMOUNT = 100.0

@Immutable
data class ScrubState(
    val rateStart: Double,
    val rateEnd: Double,
    val amount: Double
)

@HiltViewModel
class RateHistoryViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cerpsRepository: CerpsRepository,
    private val preferencesManager: PreferencesManager,
    private val networkStatusRepository: NetworkStatusRepository
) : ViewModel() {

    private val _rawCurrencies = MutableStateFlow<List<String>>(emptyList())

    val baseCurrency: StateFlow<String> = preferencesManager.baseCurrencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), DEFAULT_BASE_CURRENCY)

    val favoriteCurrencies: StateFlow<Set<String>> = preferencesManager.favoriteCurrenciesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptySet())

    val availableCurrencies: StateFlow<List<String>> = combine(
        _rawCurrencies,
        preferencesManager.favoriteCurrenciesFlow
    ) { currencies, favorites ->
        val favs = currencies.filter { favorites.contains(it) }.sorted()
        val rest = currencies.filter { !favorites.contains(it) }.sorted()
        favs + rest
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptyList())

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

    private val _scrubState = MutableStateFlow<ScrubState?>(null)
    val scrubState: StateFlow<ScrubState?> = _scrubState.asStateFlow()

    // Tracks the last amount entered by the user so it survives period/pair changes
    private var _currentAmount = DEFAULT_CONVERT_AMOUNT

    fun onScrubUpdate(rateEnd: Double, amount: Double) {
        val data = _trendsData.value ?: return
        // Use the actual first data point as the anchor, falling back to server oldRate
        val rateStart = data.points.firstOrNull()?.rate ?: data.oldRate
        if (rateStart <= 0.0) return
        _currentAmount = amount
        _scrubState.value = ScrubState(rateStart = rateStart, rateEnd = rateEnd, amount = amount)
    }

    private var trendsJob: Job? = null

    companion object {
        private const val TAG = "RateHistoryVM"
        private val ISO_DATETIME_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
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
                    _error.value = resolveErrorMessage()
                }
            }
        }
    }

    private fun resolveErrorMessage(): String {
        val offline = !networkStatusRepository.isInternetAvailable()
        val resId = if (offline) R.string.rate_history_offline
        else R.string.rate_history_service_unavailable
        return context.getString(resId)
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
        val oldFrom = _selectedFrom.value
        val oldTo = _selectedTo.value
        // Set both values atomically before triggering a single load
        _selectedFrom.value = oldTo
        _selectedTo.value = oldFrom
        loadTrends()
    }

    fun selectPeriod(period: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "selectPeriod: $period (was ${_selectedPeriod.value})")
        _selectedPeriod.value = period
        loadTrends()
    }

    private fun loadTrends() {
        trendsJob?.cancel()
        _scrubState.value = null

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
        _trendsData.value = null

        if (BuildConfig.DEBUG) Log.d(TAG, "loadTrends: requesting $from→$to period=$period")

        trendsJob = viewModelScope.launch {
            try {
                when (val result = cerpsRepository.getTrends(
                    from = from,
                    to = to,
                    period = period
                )) {
                    is CerpsResult.Success -> {
                        val raw = result.data
                        if (BuildConfig.DEBUG) Log.d(TAG, "loadTrends RAW: period=$period, " +
                                "response.period=${raw.period}, " +
                                "rawPoints=${raw.points.size}, " +
                                "dataPoints=${raw.dataPoints}, " +
                                "startDate=${raw.startDate}, endDate=${raw.endDate}, " +
                                "first=${raw.points.firstOrNull()?.timestamp}, " +
                                "last=${raw.points.lastOrNull()?.timestamp}")
                        val data = filterPointsByDateRange(raw, period)
                        if (BuildConfig.DEBUG) Log.d(TAG, "loadTrends FILTERED: period=$period, " +
                                "points=${data.points.size}, " +
                                "first=${data.points.firstOrNull()?.timestamp}, " +
                                "last=${data.points.lastOrNull()?.timestamp}")
                        _trendsData.value = data
                        // Anchor to actual first/last data points, not server-provided
                        // oldRate/newRate which can be 0 or mismatched after filtering
                        val rateStart = data.points.firstOrNull()?.rate ?: data.oldRate
                        val rateEnd = data.points.lastOrNull()?.rate ?: data.newRate
                        if (rateStart > 0) {
                            _scrubState.value = ScrubState(
                                rateStart = rateStart,
                                rateEnd = rateEnd,
                                amount = _currentAmount
                            )
                        }
                        _error.value = null
                    }
                    is CerpsResult.Error -> {
                        Log.e(TAG, "loadTrends ERROR: period=$period, msg=${result.message}")
                        _error.value = resolveErrorMessage()
                        _trendsData.value = null
                    }
                }
            } catch (e: CancellationException) {
                // Must rethrow — swallowing CancellationException breaks structured concurrency
                if (BuildConfig.DEBUG) Log.d(TAG, "loadTrends CANCELLED: period=$period")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "loadTrends EXCEPTION: period=$period", e)
                _error.value = resolveErrorMessage()
                _trendsData.value = null
            }

            _isLoading.value = false
        }
    }

    fun retry() {
        loadCurrencies()
        loadTrends()
    }

    // The API may return more points than the requested period covers.
    // For 1D: the API returns startDate/endDate spanning the entire dataset,
    // so we filter client-side to only points within the last 24 hours.
    // For other periods: filter to the response's startDate-endDate range.
    private fun filterPointsByDateRange(data: TrendsResponse, period: String): TrendsResponse {
        if (data.points.size <= 1) return data

        if (period == "1D") {
            val cutoff = Date(System.currentTimeMillis() - ONE_DAY_MS)
            val filtered = data.points.filter { point ->
                val ts = parseTimestamp(point.timestamp)
                ts != null && !ts.before(cutoff)
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "filterPointsByDateRange 1D: ${data.points.size} → ${filtered.size} points (cutoff=$cutoff)")
            return data.copy(points = filtered, dataPoints = filtered.size)
        }

        val startDate = parseTimestamp(data.startDate)
        val endDate = parseTimestamp(data.endDate)
        if (startDate == null || endDate == null) return data

        val filtered = data.points.filter { point ->
            val ts = parseTimestamp(point.timestamp)
            ts != null && !ts.before(startDate) && !ts.after(endDate)
        }

        return if (filtered.size == data.points.size || filtered.isEmpty()) {
            data
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "filterPointsByDateRange: ${data.points.size} → ${filtered.size} points")
            data.copy(points = filtered, dataPoints = filtered.size)
        }
    }

    private fun parseTimestamp(ts: String): Date? {
        val clean = ts.trimEnd('Z')
        return try {
            ISO_DATETIME_FORMAT.parse(clean)
        } catch (_: Exception) {
            try {
                ISO_DATE_FORMAT.parse(clean)
            } catch (_: Exception) {
                null
            }
        }
    }
}

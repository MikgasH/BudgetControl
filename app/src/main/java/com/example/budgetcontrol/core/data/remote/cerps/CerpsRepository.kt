package com.example.budgetcontrol.core.data.remote.cerps

import android.content.Context
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.dto.ConversionRequest
import com.example.budgetcontrol.core.data.remote.cerps.dto.ConversionResponse
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

sealed class CerpsResult<out T> {
    data class Success<T>(val data: T) : CerpsResult<T>()
    data class Error(val message: String) : CerpsResult<Nothing>()
}

@Singleton
class CerpsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: CerpsApiService,
    private val analyticsApiService: CerpsAnalyticsApiService,
    private val preferencesManager: PreferencesManager
) {

    // --- Currencies in-memory cache (one request per session) ---
    private var cachedCurrencies: List<String>? = null

    // --- Exchange rates in-memory cache ---
    private var cachedRates: Map<String, Double>? = null
    private var cachedRatesTimestamp: Long = 0L
    private var ratesLoadedThisSession: Boolean = false

    companion object {
        private const val STALE_THRESHOLD_MS = 8 * 60 * 60 * 1000L // 8 hours (for stale warning only)
    }

    suspend fun getCurrencies(): CerpsResult<List<String>> {
        cachedCurrencies?.let { return CerpsResult.Success(it) }

        return try {
            val response = apiService.getCurrencies()
            val currencies = response.body()
            if (response.isSuccessful && currencies != null) {
                cachedCurrencies = currencies
                preferencesManager.saveAvailableCurrencies(currencies)
                CerpsResult.Success(currencies)
            } else {
                val cached = preferencesManager.getAvailableCurrencies().firstOrNull()
                    ?: PreferencesManager.DEFAULT_AVAILABLE_CURRENCIES
                cachedCurrencies = cached
                CerpsResult.Success(cached)
            }
        } catch (e: Exception) {
            val cached = preferencesManager.getAvailableCurrencies().firstOrNull()
                ?: PreferencesManager.DEFAULT_AVAILABLE_CURRENCIES
            cachedCurrencies = cached
            CerpsResult.Success(cached)
        }
    }

    // --- Exchange rates loading & caching ---

    suspend fun ensureRatesLoaded(): CerpsResult<Map<String, Double>> {
        // After first successful API fetch this session, serve from memory
        val rates = cachedRates
        if (ratesLoadedThisSession && rates != null) {
            return CerpsResult.Success(rates)
        }

        // Always try API on first call after app launch
        return try {
            val response = apiService.getCurrentRates()
            val ratesResponse = response.body()
            if (response.isSuccessful && ratesResponse != null) {
                cachedRates = ratesResponse.rates
                cachedRatesTimestamp = System.currentTimeMillis()
                ratesLoadedThisSession = true
                preferencesManager.saveLastRates(ratesResponse.rates, cachedRatesTimestamp)
                CerpsResult.Success(ratesResponse.rates)
            } else {
                fallbackToCache()
            }
        } catch (e: Exception) {
            fallbackToCache()
        }
    }

    private suspend fun fallbackToCache(): CerpsResult<Map<String, Double>> {
        // Try memory cache first
        cachedRates?.let { return CerpsResult.Success(it) }

        // Then DataStore
        val dsRates = preferencesManager.getLastRates().firstOrNull() ?: emptyMap()
        val dsTimestamp = preferencesManager.getLastRatesTimestamp().firstOrNull() ?: 0L
        return if (dsRates.isNotEmpty()) {
            cachedRates = dsRates
            cachedRatesTimestamp = dsTimestamp
            CerpsResult.Success(dsRates)
        } else {
            CerpsResult.Error(
                context.getString(R.string.conversion_service_unavailable, "")
            )
        }
    }

    fun areRatesFresh(): Boolean = ratesLoadedThisSession && cachedRates != null

    fun areRatesStale(): Boolean {
        if (cachedRatesTimestamp == 0L) return true
        return (System.currentTimeMillis() - cachedRatesTimestamp) >= STALE_THRESHOLD_MS
    }

    fun getRatesTimestamp(): Long = cachedRatesTimestamp

    // --- Convert (kept for backward compatibility, no longer primary path) ---

    suspend fun convert(
        from: String,
        to: String,
        amount: Double
    ): CerpsResult<ConversionResponse> {
        return try {
            val request = ConversionRequest(amount = amount, from = from, to = to)
            val response = apiService.convert(request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.success) {
                    val rateKey = "${from}_${to}"
                    val reverseKey = "${to}_${from}"
                    val rate = body.exchangeRate.toDouble()
                    val reverseRate = if (rate > 0) 1.0 / rate else 0.0
                    val existing = preferencesManager.getLastRates().firstOrNull() ?: emptyMap()
                    val merged = existing + mapOf(rateKey to rate, reverseKey to reverseRate)
                    preferencesManager.saveLastRates(merged, System.currentTimeMillis())
                    CerpsResult.Success(body)
                } else {
                    CerpsResult.Error(context.getString(R.string.conversion_failed))
                }
            } else {
                CerpsResult.Error(context.getString(R.string.error_conversion, response.code().toString()))
            }
        } catch (e: Exception) {
            CerpsResult.Error(context.getString(R.string.conversion_service_unavailable, e.message ?: ""))
        }
    }

    suspend fun getTrends(
        from: String,
        to: String,
        period: String
    ): CerpsResult<TrendsResponse> {
        return try {
            val response = analyticsApiService.getTrends(from, to, period)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                CerpsResult.Success(body)
            } else {
                CerpsResult.Error(context.getString(R.string.error_conversion, response.code().toString()))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CerpsResult.Error(context.getString(R.string.conversion_service_unavailable, e.message ?: ""))
        }
    }

    suspend fun isServiceAvailable(): Boolean {
        return try {
            val response = apiService.healthCheck()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}

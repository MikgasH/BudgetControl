package com.example.budgetcontrol.core.data.remote.cerps

import android.content.Context
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.dto.ConversionRequest
import com.example.budgetcontrol.core.data.remote.cerps.dto.ConversionResponse
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import com.example.budgetcontrol.core.domain.repository.CurrencyRateProvider
import com.example.budgetcontrol.core.domain.repository.CurrencyRateResult
import com.example.budgetcontrol.core.util.RATES_STALE_MS
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
) : CurrencyRateProvider {

    // --- Currencies in-memory cache (one request per session) ---
    private var cachedCurrencies: List<String>? = null

    // --- Exchange rates in-memory cache ---
    private var cachedRates: Map<String, Double>? = null
    private var cachedRatesTimestamp: Long = 0L

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
                CerpsResult.Error(context.getString(R.string.error_conversion, response.code().toString()))
            }
        } catch (e: Exception) {
            CerpsResult.Error(context.getString(R.string.conversion_service_unavailable, e.message ?: ""))
        }
    }

    // --- Exchange rates loading & caching ---

    suspend fun ensureRatesLoaded(): CerpsResult<Map<String, Double>> {
        // Skip the network call while the in-memory cache is still within the stale window
        val rates = cachedRates
        if (rates != null && !areRatesStale()) {
            return CerpsResult.Success(rates)
        }

        return try {
            val response = apiService.getCurrentRates()
            val ratesResponse = response.body()
            if (response.isSuccessful && ratesResponse != null) {
                cachedRates = ratesResponse.rates
                cachedRatesTimestamp = System.currentTimeMillis()
                preferencesManager.saveLastRates(ratesResponse.rates, cachedRatesTimestamp)
                CerpsResult.Success(ratesResponse.rates)
            } else {
                fallbackToCache()
            }
        } catch (e: Exception) {
            fallbackToCache()
        }
    }

    // Cascade: memory → DataStore → error. Stale rates are better than no conversion at all.
    private suspend fun fallbackToCache(): CerpsResult<Map<String, Double>> {
        cachedRates?.let { return CerpsResult.Success(it) }

        // DataStore survives process death, so rates from a previous session can still be used
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

    // --- CurrencyRateProvider implementation ---

    override suspend fun getRates(): CurrencyRateResult {
        return when (val result = ensureRatesLoaded()) {
            is CerpsResult.Success -> CurrencyRateResult.Success(result.data)
            is CerpsResult.Error -> CurrencyRateResult.Error(result.message)
        }
    }

    override fun areRatesStale(): Boolean {
        if (cachedRatesTimestamp == 0L) return true
        return (System.currentTimeMillis() - cachedRatesTimestamp) >= RATES_STALE_MS
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
                    val rateKey = "${from}_${to}"
                    val reverseKey = "${to}_${from}"
                    val rate = body.exchangeRate.toDouble()
                    val reverseRate = if (rate > 0) 1.0 / rate else 0.0
                    val existing = preferencesManager.getLastRates().firstOrNull() ?: emptyMap()
                    val merged = existing + mapOf(rateKey to rate, reverseKey to reverseRate)
                    preferencesManager.saveLastRates(merged, System.currentTimeMillis())
                    CerpsResult.Success(body)
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
            // Must rethrow — swallowing CancellationException breaks structured concurrency
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

package com.example.budgetcontrol.core.data.remote.cerps

import android.content.Context
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.dto.ConversionRequest
import com.example.budgetcontrol.core.data.remote.cerps.dto.ConversionResponse
import com.example.budgetcontrol.core.data.remote.cerps.dto.TrendsResponse
import dagger.hilt.android.qualifiers.ApplicationContext
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

    suspend fun getCurrencies(): CerpsResult<List<String>> {
        return try {
            val response = apiService.getCurrencies()
            if (response.isSuccessful && response.body() != null) {
                val currencies = response.body()!!
                // Cache for offline use
                preferencesManager.saveAvailableCurrencies(currencies)
                CerpsResult.Success(currencies)
            } else {
                // Fall back to cached currencies
                val cached = preferencesManager.getAvailableCurrencies().firstOrNull()
                    ?: PreferencesManager.DEFAULT_AVAILABLE_CURRENCIES
                CerpsResult.Success(cached)
            }
        } catch (e: Exception) {
            // Fall back to cached currencies
            val cached = preferencesManager.getAvailableCurrencies().firstOrNull()
                ?: PreferencesManager.DEFAULT_AVAILABLE_CURRENCIES
            CerpsResult.Success(cached)
        }
    }

    suspend fun convert(
        from: String,
        to: String,
        amount: Double
    ): CerpsResult<ConversionResponse> {
        return try {
            val request = ConversionRequest(amount = amount, from = from, to = to)
            val response = apiService.convert(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    // Cache the rate for offline fallback (merge with existing)
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
            if (response.isSuccessful && response.body() != null) {
                CerpsResult.Success(response.body()!!)
            } else {
                CerpsResult.Error(context.getString(R.string.error_conversion, response.code().toString()))
            }
        } catch (e: Exception) {
            CerpsResult.Error(context.getString(R.string.conversion_service_unavailable, e.message ?: ""))
        }
    }

    suspend fun isServiceAvailable(): Boolean {
        return try {
            val response = apiService.getCurrencies()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}

package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import kotlinx.coroutines.flow.firstOrNull
import java.math.RoundingMode
import javax.inject.Inject

data class ConversionResult(
    val convertedAmount: Double,
    val exchangeRate: Double?,
    val rateSource: String?
)

sealed class ConvertCurrencyResult {
    data class Success(val conversion: ConversionResult) : ConvertCurrencyResult()
    data class Error(val message: String) : ConvertCurrencyResult()
}

class ConvertCurrencyUseCase @Inject constructor(
    private val cerpsRepository: CerpsRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(
        amount: Double,
        fromCurrency: String,
        toCurrency: String = "EUR"
    ): ConvertCurrencyResult {
        if (fromCurrency == toCurrency) {
            return ConvertCurrencyResult.Success(
                ConversionResult(amount, null, null)
            )
        }

        return when (val result = cerpsRepository.convert(fromCurrency, toCurrency, amount)) {
            is CerpsResult.Success -> {
                val roundedAmount = result.data.convertedAmount
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble()
                ConvertCurrencyResult.Success(
                    ConversionResult(roundedAmount, result.data.exchangeRate.toDouble(), null)
                )
            }
            is CerpsResult.Error -> {
                val cachedRates = preferencesManager.getLastRates().firstOrNull() ?: emptyMap()
                val rateKey = "${fromCurrency}_${toCurrency}"
                val cachedRate = cachedRates[rateKey]
                if (cachedRate != null && cachedRate > 0) {
                    val convertedAmount = (amount * cachedRate * 100).toLong() / 100.0
                    ConvertCurrencyResult.Success(
                        ConversionResult(convertedAmount, cachedRate, "CACHED_RATE")
                    )
                } else {
                    ConvertCurrencyResult.Error(result.message)
                }
            }
        }
    }
}

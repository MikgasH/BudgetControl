package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
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
    private val cerpsRepository: CerpsRepository
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

        return when (val result = cerpsRepository.ensureRatesLoaded()) {
            is CerpsResult.Success -> {
                val rates = result.data
                val rate = rates[fromCurrency]
                if (rate != null && rate > 0) {
                    val convertedAmount = Math.round(amount / rate * 100) / 100.0
                    val rateSource = if (!cerpsRepository.areRatesStale()) null else "CACHED_RATE"
                    ConvertCurrencyResult.Success(
                        ConversionResult(convertedAmount, rate, rateSource)
                    )
                } else {
                    ConvertCurrencyResult.Error("No rate available for $fromCurrency")
                }
            }
            is CerpsResult.Error -> {
                ConvertCurrencyResult.Error(result.message)
            }
        }
    }
}

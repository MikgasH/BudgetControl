package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.repository.CurrencyRateProvider
import com.example.budgetcontrol.core.domain.repository.CurrencyRateResult
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
    private val currencyRateProvider: CurrencyRateProvider
) {
    suspend operator fun invoke(
        amount: Double,
        fromCurrency: String,
        toCurrency: String
    ): ConvertCurrencyResult {
        if (fromCurrency == toCurrency) {
            return ConvertCurrencyResult.Success(
                ConversionResult(amount, null, null)
            )
        }

        return when (val result = currencyRateProvider.getRates()) {
            is CurrencyRateResult.Success -> {
                val rates = result.rates
                // Rates are EUR-based: rate[X] = how many X per 1 EUR
                val fromRate = rates[fromCurrency]
                val toRate = rates[toCurrency]

                // If toCurrency is EUR, toRate is implicitly 1.0
                val effectiveToRate = if (toCurrency == "EUR") 1.0 else toRate
                // If fromCurrency is EUR, fromRate is implicitly 1.0
                val effectiveFromRate = if (fromCurrency == "EUR") 1.0 else fromRate

                if (effectiveFromRate != null && effectiveFromRate > 0 &&
                    effectiveToRate != null && effectiveToRate > 0
                ) {
                    // Cross-rate conversion: FROM → EUR → TO
                    val convertedAmount = Math.round(amount * effectiveToRate / effectiveFromRate * 100) / 100.0
                    val crossRate = effectiveFromRate / effectiveToRate
                    val rateSource = if (!currencyRateProvider.areRatesStale()) null else "CACHED_RATE"
                    ConvertCurrencyResult.Success(
                        ConversionResult(convertedAmount, crossRate, rateSource)
                    )
                } else {
                    ConvertCurrencyResult.Error("No rate available for $fromCurrency → $toCurrency")
                }
            }
            is CurrencyRateResult.Error -> {
                ConvertCurrencyResult.Error(result.message)
            }
        }
    }
}

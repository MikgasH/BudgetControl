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
                // CERPS always returns EUR-based rates — this invariant must hold.
                // If CERPS base currency changes, this calculation must be updated.
                // rate[X] = how many X per 1 EUR
                val fromRate = rates[fromCurrency]
                val toRate = rates[toCurrency]

                // EUR itself is not in the rates map; its rate is implicitly 1.0
                val effectiveToRate = if (toCurrency == "EUR") 1.0 else toRate
                val effectiveFromRate = if (fromCurrency == "EUR") 1.0 else fromRate

                if (effectiveFromRate != null && effectiveFromRate > 0 &&
                    effectiveToRate != null && effectiveToRate > 0
                ) {
                    // Cross-rate: amount / fromRate gives EUR, * toRate gives target currency
                    val convertedAmount = Math.round(amount * effectiveToRate / effectiveFromRate * 100) / 100.0
                    // Store rate as "how many FROM units per 1 TO unit" for the detail screen display
                    val crossRate = effectiveFromRate / effectiveToRate
                    // null rateSource means live rate was used; "CACHED_RATE" warns the UI that the rate may be outdated
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

package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.RateSource
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

// CERPS rates are EUR-based: rate[X] = how many X per 1 EUR. EUR itself is implicit 1.0.
// Returns null when the required rate is missing so callers can distinguish
// "unconvertible" from "converted to zero".
fun crossConvert(
    amount: Double,
    fromCurrency: String,
    toCurrency: String,
    rates: Map<String, Double>
): Double? {
    if (fromCurrency == toCurrency) return amount
    val fromRate = if (fromCurrency == "EUR") 1.0 else rates[fromCurrency]
    val toRate = if (toCurrency == "EUR") 1.0 else rates[toCurrency]
    if (fromRate == null || fromRate <= 0.0 || toRate == null || toRate <= 0.0) return null
    return amount * toRate / fromRate
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
                val converted = crossConvert(amount, fromCurrency, toCurrency, rates)
                if (converted != null) {
                    val fromRate = if (fromCurrency == "EUR") 1.0 else rates.getValue(fromCurrency)
                    val toRate = if (toCurrency == "EUR") 1.0 else rates.getValue(toCurrency)
                    // Store rate as "how many FROM units per 1 TO unit" for the detail screen display
                    val crossRate = fromRate / toRate
                    // null rateSource means live rate was used; CACHED_RATE warns the UI that the rate may be outdated
                    val rateSource = if (!currencyRateProvider.areRatesStale()) null else RateSource.CACHED_RATE.name
                    ConvertCurrencyResult.Success(
                        ConversionResult(converted, crossRate, rateSource)
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

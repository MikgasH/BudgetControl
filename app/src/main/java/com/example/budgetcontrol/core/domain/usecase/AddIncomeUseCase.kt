package com.example.budgetcontrol.core.domain.usecase

import android.content.Context
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.local.datastore.PreferencesManager
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.domain.model.Income
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject

sealed class AddIncomeResult {
    object Success : AddIncomeResult()
    data class Error(val message: String) : AddIncomeResult()
}

class AddIncomeUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: IncomeRepository,
    private val cerpsRepository: CerpsRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(
        amount: Double,
        currency: String = "EUR",
        categoryId: String,
        description: String?,
        date: Long = System.currentTimeMillis(),
        bankName: String? = null,
        bankCommission: Double? = null
    ): AddIncomeResult {
        return try {
            data class ConversionResult(val finalAmount: Double, val exchangeRate: Double?, val rateSource: String?)

            val conversion = if (currency == "EUR") {
                ConversionResult(amount, null, null)
            } else {
                when (val result = cerpsRepository.convert(currency, "EUR", amount)) {
                    is CerpsResult.Success -> {
                        val roundedAmount = result.data.convertedAmount
                            .setScale(2, RoundingMode.HALF_UP)
                            .toDouble()
                        ConversionResult(roundedAmount, result.data.exchangeRate.toDouble(), null)
                    }
                    is CerpsResult.Error -> {
                        // Offline fallback: try cached rates
                        val cachedRates = preferencesManager.getLastRates().firstOrNull() ?: emptyMap()
                        val rateKey = "${currency}_EUR"
                        val cachedRate = cachedRates[rateKey]
                        if (cachedRate != null && cachedRate > 0) {
                            val convertedAmount = (amount * cachedRate * 100).toLong() / 100.0
                            ConversionResult(convertedAmount, cachedRate, "CACHED_RATE")
                        } else {
                            return AddIncomeResult.Error(result.message)
                        }
                    }
                }
            }

            val income = Income(
                id = UUID.randomUUID().toString(),
                amount = conversion.finalAmount,
                categoryId = categoryId,
                description = description,
                date = date,
                createdAt = System.currentTimeMillis(),
                originalAmount = amount,
                originalCurrency = currency,
                exchangeRate = conversion.exchangeRate,
                bankName = bankName,
                bankCommission = bankCommission,
                rateSource = conversion.rateSource
            )

            repository.insertIncome(income)
            categoryRepository.incrementUsageCount(categoryId)
            AddIncomeResult.Success

        } catch (e: Exception) {
            AddIncomeResult.Error(context.getString(R.string.error_saving, e.message ?: ""))
        }
    }

    suspend fun addInEur(
        amount: Double,
        categoryId: String,
        description: String?,
        date: Long = System.currentTimeMillis()
    ): AddIncomeResult {
        return invoke(amount, "EUR", categoryId, description, date)
    }

    suspend fun addWithExactEurAmount(
        originalAmount: Double,
        originalCurrency: String,
        exactEurAmount: Double,
        categoryId: String,
        description: String?,
        date: Long = System.currentTimeMillis(),
        bankName: String? = null,
        bankCommission: Double? = null,
        rateSource: String = "USER_CORRECTED"
    ): AddIncomeResult {
        return try {
            val income = Income(
                id = UUID.randomUUID().toString(),
                amount = exactEurAmount,
                categoryId = categoryId,
                description = description,
                date = date,
                createdAt = System.currentTimeMillis(),
                originalAmount = originalAmount,
                originalCurrency = originalCurrency,
                exchangeRate = if (originalAmount > 0) originalAmount / exactEurAmount else null,
                bankName = bankName,
                bankCommission = bankCommission,
                rateSource = rateSource
            )
            repository.insertIncome(income)
            categoryRepository.incrementUsageCount(categoryId)
            AddIncomeResult.Success
        } catch (e: Exception) {
            AddIncomeResult.Error(context.getString(R.string.error_saving, e.message ?: ""))
        }
    }
}

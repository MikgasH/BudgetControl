package com.example.budgetcontrol.core.domain.usecase

import android.content.Context
import com.example.budgetcontrol.R
import com.example.budgetcontrol.core.data.remote.cerps.CerpsRepository
import com.example.budgetcontrol.core.data.remote.cerps.CerpsResult
import com.example.budgetcontrol.core.domain.model.Income
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val categoryRepository: CategoryRepository
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
            val (finalAmount, exchangeRate) = if (currency == "EUR") {
                amount to null
            } else {
                when (val result = cerpsRepository.convert(currency, "EUR", amount)) {
                    is CerpsResult.Success -> {
                        val roundedAmount = result.data.convertedAmount
                            .setScale(2, RoundingMode.HALF_UP)
                            .toDouble()
                        roundedAmount to result.data.exchangeRate.toDouble()
                    }
                    is CerpsResult.Error -> {
                        return AddIncomeResult.Error(result.message)
                    }
                }
            }

            val income = Income(
                id = UUID.randomUUID().toString(),
                amount = finalAmount,
                categoryId = categoryId,
                description = description,
                date = date,
                createdAt = System.currentTimeMillis(),
                originalAmount = amount,
                originalCurrency = currency,
                exchangeRate = exchangeRate,
                bankName = bankName,
                bankCommission = bankCommission
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

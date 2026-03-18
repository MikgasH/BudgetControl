package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Income
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import java.util.UUID
import javax.inject.Inject

sealed class AddIncomeResult {
    object Success : AddIncomeResult()
    data class Error(val error: AddTransactionError) : AddIncomeResult()
}

class AddIncomeUseCase @Inject constructor(
    private val repository: IncomeRepository,
    private val convertCurrencyUseCase: ConvertCurrencyUseCase,
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
            val conversionResult = convertCurrencyUseCase(amount, currency)
            if (conversionResult is ConvertCurrencyResult.Error) {
                return AddIncomeResult.Error(AddTransactionError.ConversionFailed(conversionResult.message))
            }
            val conversion = (conversionResult as ConvertCurrencyResult.Success).conversion

            val income = Income(
                id = UUID.randomUUID().toString(),
                amount = conversion.convertedAmount,
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
            AddIncomeResult.Error(AddTransactionError.SavingFailed(e.message))
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
            AddIncomeResult.Error(AddTransactionError.SavingFailed(e.message))
        }
    }
}

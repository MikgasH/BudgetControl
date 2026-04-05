package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Account
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
        currency: String,
        categoryId: String,
        description: String?,
        date: Long = System.currentTimeMillis(),
        bankName: String? = null,
        bankCommission: Double? = null,
        baseCurrency: String,
        accountId: String = Account.DEFAULT_ACCOUNT_ID
    ): AddIncomeResult {
        return try {
            val conversionResult = convertCurrencyUseCase(amount, currency, baseCurrency)
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
                rateSource = conversion.rateSource,
                accountId = accountId
            )

            repository.insertIncome(income)
            categoryRepository.incrementUsageCount(categoryId)
            AddIncomeResult.Success

        } catch (e: Exception) {
            AddIncomeResult.Error(AddTransactionError.SavingFailed(e.message))
        }
    }

    suspend fun addInBaseCurrency(
        amount: Double,
        baseCurrency: String,
        categoryId: String,
        description: String?,
        date: Long = System.currentTimeMillis(),
        accountId: String = Account.DEFAULT_ACCOUNT_ID
    ): AddIncomeResult {
        return invoke(amount, baseCurrency, categoryId, description, date, baseCurrency = baseCurrency, accountId = accountId)
    }

    suspend fun addWithExactBaseAmount(
        originalAmount: Double,
        originalCurrency: String,
        exactBaseAmount: Double,
        categoryId: String,
        description: String?,
        date: Long = System.currentTimeMillis(),
        bankName: String? = null,
        bankCommission: Double? = null,
        rateSource: String = "USER_CORRECTED",
        accountId: String = Account.DEFAULT_ACCOUNT_ID
    ): AddIncomeResult {
        if (exactBaseAmount <= 0.0) {
            return AddIncomeResult.Error(AddTransactionError.InvalidAmount)
        }
        return try {
            val income = Income(
                id = UUID.randomUUID().toString(),
                amount = exactBaseAmount,
                categoryId = categoryId,
                description = description,
                date = date,
                createdAt = System.currentTimeMillis(),
                originalAmount = originalAmount,
                originalCurrency = originalCurrency,
                // Derive the effective rate from user-provided amounts (inverse of normal API rate direction)
                exchangeRate = if (originalAmount > 0) originalAmount / exactBaseAmount else null,
                bankName = bankName,
                bankCommission = bankCommission,
                rateSource = rateSource,
                accountId = accountId
            )
            repository.insertIncome(income)
            categoryRepository.incrementUsageCount(categoryId)
            AddIncomeResult.Success
        } catch (e: Exception) {
            AddIncomeResult.Error(AddTransactionError.SavingFailed(e.message))
        }
    }
}

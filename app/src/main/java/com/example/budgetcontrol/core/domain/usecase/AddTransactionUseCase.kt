package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.model.Transaction
import com.example.budgetcontrol.core.domain.model.TransactionType
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import com.example.budgetcontrol.core.domain.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject

sealed class AddTransactionResult {
    object Success : AddTransactionResult()
    data class Error(val error: AddTransactionError) : AddTransactionResult()
}

class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val convertCurrencyUseCase: ConvertCurrencyUseCase,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        type: TransactionType,
        amount: Double,
        currency: String,
        categoryId: String,
        description: String?,
        date: Long = System.currentTimeMillis(),
        bankName: String? = null,
        bankCommission: Double? = null,
        baseCurrency: String,
        accountId: String = Account.DEFAULT_ACCOUNT_ID
    ): AddTransactionResult {
        return try {
            val conversionResult = convertCurrencyUseCase(amount, currency, baseCurrency)
            if (conversionResult is ConvertCurrencyResult.Error) {
                return AddTransactionResult.Error(
                    AddTransactionError.ConversionFailed(conversionResult.message)
                )
            }
            val conversion = (conversionResult as ConvertCurrencyResult.Success).conversion

            val transaction = buildTransaction(
                type = type,
                amount = conversion.convertedAmount,
                categoryId = categoryId,
                description = description,
                date = date,
                originalAmount = amount,
                originalCurrency = currency,
                exchangeRate = conversion.exchangeRate,
                bankName = bankName,
                bankCommission = bankCommission,
                rateSource = conversion.rateSource,
                accountId = accountId
            )

            transactionRepository.insert(transaction)
            categoryRepository.incrementUsageCount(categoryId)
            AddTransactionResult.Success
        } catch (e: Exception) {
            AddTransactionResult.Error(AddTransactionError.SavingFailed(e.message))
        }
    }

    suspend fun addInBaseCurrency(
        type: TransactionType,
        amount: Double,
        baseCurrency: String,
        categoryId: String,
        description: String?,
        date: Long = System.currentTimeMillis(),
        accountId: String = Account.DEFAULT_ACCOUNT_ID
    ): AddTransactionResult {
        return invoke(
            type = type,
            amount = amount,
            currency = baseCurrency,
            categoryId = categoryId,
            description = description,
            date = date,
            baseCurrency = baseCurrency,
            accountId = accountId
        )
    }

    /**
     * Save a transaction where the user manually specified the exact base-currency amount
     * (e.g. taken directly from their banking app). Skips CERPS conversion entirely.
     */
    suspend fun addWithExactBaseAmount(
        type: TransactionType,
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
    ): AddTransactionResult {
        if (exactBaseAmount <= 0.0) {
            return AddTransactionResult.Error(AddTransactionError.InvalidAmount)
        }
        return try {
            val transaction = buildTransaction(
                type = type,
                amount = exactBaseAmount,
                categoryId = categoryId,
                description = description,
                date = date,
                originalAmount = originalAmount,
                originalCurrency = originalCurrency,
                // Derive the effective rate from user-provided amounts (inverse of normal API rate direction)
                exchangeRate = if (originalAmount > 0) originalAmount / exactBaseAmount else null,
                bankName = bankName,
                bankCommission = bankCommission,
                rateSource = rateSource,
                accountId = accountId
            )
            transactionRepository.insert(transaction)
            categoryRepository.incrementUsageCount(categoryId)
            AddTransactionResult.Success
        } catch (e: Exception) {
            AddTransactionResult.Error(AddTransactionError.SavingFailed(e.message))
        }
    }

    private fun buildTransaction(
        type: TransactionType,
        amount: Double,
        categoryId: String,
        description: String?,
        date: Long,
        originalAmount: Double,
        originalCurrency: String,
        exchangeRate: Double?,
        bankName: String?,
        bankCommission: Double?,
        rateSource: String?,
        accountId: String
    ): Transaction {
        val id = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
        return when (type) {
            TransactionType.EXPENSE -> Transaction.ExpenseTransaction(
                id = id,
                amount = amount,
                categoryId = categoryId,
                description = description,
                date = date,
                createdAt = createdAt,
                originalAmount = originalAmount,
                originalCurrency = originalCurrency,
                exchangeRate = exchangeRate,
                bankName = bankName,
                bankCommission = bankCommission,
                rateSource = rateSource,
                accountId = accountId
            )
            TransactionType.INCOME -> Transaction.IncomeTransaction(
                id = id,
                amount = amount,
                categoryId = categoryId,
                description = description,
                date = date,
                createdAt = createdAt,
                originalAmount = originalAmount,
                originalCurrency = originalCurrency,
                exchangeRate = exchangeRate,
                bankName = bankName,
                bankCommission = bankCommission,
                rateSource = rateSource,
                accountId = accountId
            )
        }
    }
}

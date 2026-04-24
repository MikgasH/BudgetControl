package com.example.budgetcontrol.util

import com.example.budgetcontrol.core.domain.model.Account
import com.example.budgetcontrol.core.domain.model.Expense
import com.example.budgetcontrol.core.domain.model.Income
import com.example.budgetcontrol.core.domain.model.Transaction

object TestFixtures {

    fun account(
        id: String = "acc-1",
        name: String = "Main",
        iconName: String = "wallet",
        color: String = "#FF0000",
        initialBalance: Double = 0.0,
        currency: String = "EUR",
        isDefault: Boolean = false,
        createdAt: Long = 1_700_000_000_000L,
        lastUsedAt: Long = 0L,
        sortOrder: Int = 0
    ): Account = Account(
        id = id,
        name = name,
        iconName = iconName,
        color = color,
        initialBalance = initialBalance,
        currency = currency,
        isDefault = isDefault,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
        sortOrder = sortOrder
    )

    fun expense(
        id: String = "exp-1",
        amount: Double = 10.0,
        categoryId: String = "cat-1",
        description: String? = null,
        date: Long = 1_700_000_000_000L,
        createdAt: Long = 1_700_000_000_000L,
        originalAmount: Double = amount,
        originalCurrency: String = "EUR",
        exchangeRate: Double? = null,
        bankName: String? = null,
        bankCommission: Double? = null,
        rateSource: String? = null,
        accountId: String? = null
    ): Expense = Expense(
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

    fun income(
        id: String = "inc-1",
        amount: Double = 10.0,
        categoryId: String = "cat-1",
        description: String? = null,
        date: Long = 1_700_000_000_000L,
        createdAt: Long = 1_700_000_000_000L,
        originalAmount: Double = amount,
        originalCurrency: String = "EUR",
        exchangeRate: Double? = null,
        bankName: String? = null,
        bankCommission: Double? = null,
        rateSource: String? = null,
        accountId: String? = null
    ): Income = Income(
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

    fun expenseTransaction(
        id: String = "exp-tx-1",
        amount: Double = 10.0,
        categoryId: String = "cat-1",
        description: String? = null,
        date: Long = 1_700_000_000_000L,
        createdAt: Long = 1_700_000_000_000L,
        originalAmount: Double = amount,
        originalCurrency: String = "EUR",
        exchangeRate: Double? = null,
        bankName: String? = null,
        bankCommission: Double? = null,
        rateSource: String? = null,
        accountId: String? = null
    ): Transaction.ExpenseTransaction = Transaction.ExpenseTransaction(
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

    fun incomeTransaction(
        id: String = "inc-tx-1",
        amount: Double = 10.0,
        categoryId: String = "cat-1",
        description: String? = null,
        date: Long = 1_700_000_000_000L,
        createdAt: Long = 1_700_000_000_000L,
        originalAmount: Double = amount,
        originalCurrency: String = "EUR",
        exchangeRate: Double? = null,
        bankName: String? = null,
        bankCommission: Double? = null,
        rateSource: String? = null,
        accountId: String? = null
    ): Transaction.IncomeTransaction = Transaction.IncomeTransaction(
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

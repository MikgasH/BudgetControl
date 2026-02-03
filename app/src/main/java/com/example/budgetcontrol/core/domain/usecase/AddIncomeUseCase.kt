package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Income
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import java.util.UUID
import javax.inject.Inject

class AddIncomeUseCase @Inject constructor(
    private val repository: IncomeRepository
) {
    suspend operator fun invoke(
        amount: Double,
        categoryId: String,
        description: String?,
        date: Long = System.currentTimeMillis()
    ) {
        val income = Income(
            id = UUID.randomUUID().toString(),
            amount = amount,
            categoryId = categoryId,
            description = description,
            date = date,
            createdAt = System.currentTimeMillis()
        )
        repository.insertIncome(income)
    }
}
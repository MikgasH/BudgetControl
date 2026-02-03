package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Income
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import javax.inject.Inject

class DeleteIncomeUseCase @Inject constructor(
    private val repository: IncomeRepository
) {
    suspend operator fun invoke(income: Income) {
        repository.deleteIncome(income)
    }

    suspend fun deleteById(id: String) {
        repository.deleteIncomeById(id)
    }
}
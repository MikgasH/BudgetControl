package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Income
import com.example.budgetcontrol.core.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetIncomesUseCase @Inject constructor(
    private val repository: IncomeRepository
) {
    operator fun invoke(): Flow<List<Income>> {
        return repository.getAllIncomes()
    }

    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<Income>> {
        return repository.getIncomesByDateRange(startDate, endDate)
    }

    fun getByCategory(categoryId: String): Flow<List<Income>> {
        return repository.getIncomesByCategory(categoryId)
    }
}
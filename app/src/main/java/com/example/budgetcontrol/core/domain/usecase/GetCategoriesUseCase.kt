package com.example.budgetcontrol.core.domain.usecase

import com.example.budgetcontrol.core.domain.model.Category
import com.example.budgetcontrol.core.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> {
        return repository.getAllCategories()
    }
}
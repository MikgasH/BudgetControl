package com.example.budgetcontrol.core.domain.model

import androidx.annotation.StringRes
import com.example.budgetcontrol.R

enum class OperationType(@StringRes val displayNameRes: Int) {
    EXPENSES(R.string.expenses_upper),
    INCOMES(R.string.incomes_upper)
}

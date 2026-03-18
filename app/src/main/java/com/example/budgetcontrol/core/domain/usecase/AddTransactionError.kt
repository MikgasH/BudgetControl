package com.example.budgetcontrol.core.domain.usecase

sealed class AddTransactionError {
    data class SavingFailed(val cause: String? = null) : AddTransactionError()
    data class ConversionFailed(val message: String) : AddTransactionError()
    object NetworkUnavailable : AddTransactionError()
}

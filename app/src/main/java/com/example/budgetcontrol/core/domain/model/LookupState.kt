package com.example.budgetcontrol.core.domain.model

sealed class LookupState {
    object Loading : LookupState()
    data class Success(val value: Double) : LookupState()
    object NotFound : LookupState()
    data class Error(val message: String? = null) : LookupState()
}

package com.example.budgetcontrol.core.domain.model

import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY

data class Account(
    val id: String,
    val name: String,
    val iconName: String,
    val color: String,
    val initialBalance: Double = 0.0,
    val currency: String = DEFAULT_BASE_CURRENCY,
    val isDefault: Boolean = false,
    val createdAt: Long = 0L,
    val lastUsedAt: Long = 0L,
    val sortOrder: Int = 0
) {
    companion object {
        const val DEFAULT_ACCOUNT_ID = "00000000-0000-0000-0000-000000000000"
    }
}

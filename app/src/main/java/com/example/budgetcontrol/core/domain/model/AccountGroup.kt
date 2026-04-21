package com.example.budgetcontrol.core.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AccountGroup(
    val id: String,
    val name: String,
    val memberAccountIds: List<String>,
    val createdAt: Long = 0L
)

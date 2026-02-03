package com.example.budgetcontrol.core.domain.model

data class Income(
    val id: String,
    val amount: Double,
    val categoryId: String,
    val description: String?,
    val date: Long,
    val createdAt: Long
)
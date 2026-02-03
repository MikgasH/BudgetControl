package com.example.budgetcontrol.core.data.remote.models

import com.example.budgetcontrol.core.domain.model.Expense

data class FirestoreExpense(
    val id: String = "",
    val amount: Double = 0.0,
    val categoryId: String = "",
    val description: String? = null,
    val date: Long = 0L,
    val createdAt: Long = 0L,
    val lastModified: Long = System.currentTimeMillis()
) {
    // Пустой конструктор для Firestore
    constructor() : this("", 0.0, "", null, 0L, 0L, 0L)
}

// Мапперы для конвертации
fun Expense.toFirestore(): FirestoreExpense {
    return FirestoreExpense(
        id = id,
        amount = amount,
        categoryId = categoryId,
        description = description,
        date = date,
        createdAt = createdAt,
        lastModified = System.currentTimeMillis()
    )
}

fun FirestoreExpense.toDomain(): Expense {
    return Expense(
        id = id,
        amount = amount,
        categoryId = categoryId,
        description = description,
        date = date,
        createdAt = createdAt
    )
}
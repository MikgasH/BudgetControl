package com.example.budgetcontrol.core.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val categoryId: String,
    val description: String?,
    val date: Long,
    val createdAt: Long,
    val originalAmount: Double = amount,
    val originalCurrency: String = "EUR",
    val exchangeRate: Double? = null,
    val bankName: String? = null,
    val bankCommission: Double? = null,
    val rateSource: String? = null
)
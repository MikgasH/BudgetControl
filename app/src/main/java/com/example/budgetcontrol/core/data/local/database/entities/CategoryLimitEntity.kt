package com.example.budgetcontrol.core.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_limits",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"], unique = true)]
)
data class CategoryLimitEntity(
    @PrimaryKey val categoryId: String,
    val amount: Double,
    val periodType: String = "MONTH",
    val createdAt: Long,
    val updatedAt: Long
)

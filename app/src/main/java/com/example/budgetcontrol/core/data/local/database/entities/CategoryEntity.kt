package com.example.budgetcontrol.core.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["type"])]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconName: String,
    val color: String,
    val isDefault: Boolean = false,
    val type: String = "EXPENSE",
    val nameKey: String? = null,
    val isSystem: Boolean = false,
    val usageCount: Int = 0
)
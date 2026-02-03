package com.example.budgetcontrol.core.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconName: String,
    val color: String,
    val isDefault: Boolean = false,
    val type: String = "EXPENSE"
)
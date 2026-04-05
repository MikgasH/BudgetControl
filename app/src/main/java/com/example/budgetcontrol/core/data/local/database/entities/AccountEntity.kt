package com.example.budgetcontrol.core.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.budgetcontrol.core.util.DEFAULT_BASE_CURRENCY

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["isDefault"])]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconName: String,
    val color: String,
    val initialBalance: Double = 0.0,
    val currency: String = DEFAULT_BASE_CURRENCY,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

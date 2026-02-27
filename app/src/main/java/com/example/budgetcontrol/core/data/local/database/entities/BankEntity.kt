package com.example.budgetcontrol.core.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "banks")
data class BankEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val commissionPercent: Double,
    val isDefault: Boolean = false,
    val isFavorite: Boolean = false
)


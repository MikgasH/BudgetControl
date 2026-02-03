package com.example.budgetcontrol.core.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.budgetcontrol.core.data.local.database.dao.CategoryDao
import com.example.budgetcontrol.core.data.local.database.dao.ExpenseDao
import com.example.budgetcontrol.core.data.local.database.dao.IncomeDao
import com.example.budgetcontrol.core.data.local.database.entities.CategoryEntity
import com.example.budgetcontrol.core.data.local.database.entities.ExpenseEntity
import com.example.budgetcontrol.core.data.local.database.entities.IncomeEntity

@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class, IncomeEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun incomeDao(): IncomeDao

    companion object {
        const val DATABASE_NAME = "budget_control_db"

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE expenses ADD COLUMN originalAmount REAL NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE expenses ADD COLUMN originalCurrency TEXT NOT NULL DEFAULT 'EUR'"
                )
                database.execSQL(
                    "ALTER TABLE expenses ADD COLUMN exchangeRate REAL DEFAULT NULL"
                )

                database.execSQL(
                    "UPDATE expenses SET originalAmount = amount WHERE originalAmount = 0"
                )
            }
        }
    }
}
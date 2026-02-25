package com.example.budgetcontrol.core.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.budgetcontrol.core.data.local.database.dao.BankDao
import com.example.budgetcontrol.core.data.local.database.dao.CategoryDao
import com.example.budgetcontrol.core.data.local.database.dao.ExpenseDao
import com.example.budgetcontrol.core.data.local.database.dao.IncomeDao
import com.example.budgetcontrol.core.data.local.database.entities.BankEntity
import com.example.budgetcontrol.core.data.local.database.entities.CategoryEntity
import com.example.budgetcontrol.core.data.local.database.entities.ExpenseEntity
import com.example.budgetcontrol.core.data.local.database.entities.IncomeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class, IncomeEntity::class, BankEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun incomeDao(): IncomeDao
    abstract fun bankDao(): BankDao

    companion object {
        const val DATABASE_NAME = "budget_control_db"

        val DEFAULT_BANKS = listOf(
            BankEntity(name = "Revolut", commissionPercent = 0.5, isDefault = true),
            BankEntity(name = "Wise", commissionPercent = 0.5),
            BankEntity(name = "SEB", commissionPercent = 3.0),
            BankEntity(name = "Swedbank", commissionPercent = 3.5),
            BankEntity(name = "Luminor", commissionPercent = 3.0)
        )

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE expenses ADD COLUMN bankName TEXT DEFAULT NULL"
                )
                database.execSQL(
                    "ALTER TABLE expenses ADD COLUMN bankCommission REAL DEFAULT NULL"
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS banks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        commissionPercent REAL NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE expenses ADD COLUMN rateSource TEXT DEFAULT NULL"
                )
            }
        }

        val PREPOPULATE_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    DEFAULT_BANKS.forEach { bank ->
                        db.execSQL(
                            "INSERT INTO banks (name, commissionPercent, isDefault) VALUES (?, ?, ?)",
                            arrayOf(bank.name, bank.commissionPercent, if (bank.isDefault) 1 else 0)
                        )
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val cursor = db.query("SELECT COUNT(*) FROM banks")
                    val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
                    cursor.close()
                    if (count == 0) {
                        DEFAULT_BANKS.forEach { bank ->
                            db.execSQL(
                                "INSERT INTO banks (name, commissionPercent, isDefault) VALUES (?, ?, ?)",
                                arrayOf(bank.name, bank.commissionPercent, if (bank.isDefault) 1 else 0)
                            )
                        }
                    }
                }
            }
        }
    }
}
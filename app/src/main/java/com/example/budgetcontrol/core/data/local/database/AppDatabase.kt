package com.example.budgetcontrol.core.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.budgetcontrol.core.data.local.database.dao.BankDao
import com.example.budgetcontrol.core.data.local.database.dao.CategoryDao
import com.example.budgetcontrol.core.data.local.database.dao.CurrencyExchangeDao
import com.example.budgetcontrol.core.data.local.database.dao.ExpenseDao
import com.example.budgetcontrol.core.data.local.database.dao.IncomeDao
import com.example.budgetcontrol.core.data.local.database.entities.BankEntity
import com.example.budgetcontrol.core.data.local.database.entities.CategoryEntity
import com.example.budgetcontrol.core.data.local.database.entities.CurrencyExchangeEntity
import com.example.budgetcontrol.core.data.local.database.entities.ExpenseEntity
import com.example.budgetcontrol.core.data.local.database.entities.IncomeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class, IncomeEntity::class, BankEntity::class, CurrencyExchangeEntity::class],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun incomeDao(): IncomeDao
    abstract fun bankDao(): BankDao
    abstract fun currencyExchangeDao(): CurrencyExchangeDao

    companion object {
        const val DATABASE_NAME = "budget_control_db"

        val DEFAULT_BANKS = listOf(
            BankEntity(name = "Revolut", commissionPercent = 0.5, isDefault = true, isFavorite = true),
            BankEntity(name = "Wise", commissionPercent = 0.5, isFavorite = true),
            BankEntity(name = "Luminor", commissionPercent = 3.0),
            BankEntity(name = "SEB", commissionPercent = 3.0, isFavorite = true),
            BankEntity(name = "Swedbank", commissionPercent = 3.5),
            BankEntity(name = "Citadele", commissionPercent = 3.0),
            BankEntity(name = "LHV", commissionPercent = 2.5),
            BankEntity(name = "PKO", commissionPercent = 3.0),
            BankEntity(name = "mBank", commissionPercent = 3.0),
            BankEntity(name = "Santander PL", commissionPercent = 3.5),
            BankEntity(name = "Česká spořitelna", commissionPercent = 3.0),
            BankEntity(name = "ČSOB", commissionPercent = 3.0),
            BankEntity(name = "N26", commissionPercent = 0.0),
            BankEntity(name = "DKB", commissionPercent = 1.75),
            BankEntity(name = "Deutsche Bank", commissionPercent = 2.0),
            BankEntity(name = "ING", commissionPercent = 1.99),
            BankEntity(name = "bunq", commissionPercent = 0.0),
            BankEntity(name = "BBVA", commissionPercent = 2.0),
            BankEntity(name = "Santander ES", commissionPercent = 3.0),
            BankEntity(name = "Nordea", commissionPercent = 2.5),
            BankEntity(name = "Handelsbanken", commissionPercent = 2.0),
            BankEntity(name = "Monzo", commissionPercent = 0.0),
            BankEntity(name = "Starling", commissionPercent = 0.0)
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

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE banks ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE categories ADD COLUMN nameKey TEXT DEFAULT NULL"
                )
                database.execSQL(
                    "ALTER TABLE categories ADD COLUMN isSystem INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE categories ADD COLUMN usageCount INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "UPDATE categories SET isSystem = 1 WHERE isDefault = 1"
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE incomes ADD COLUMN originalAmount REAL NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE incomes ADD COLUMN originalCurrency TEXT NOT NULL DEFAULT 'EUR'"
                )
                database.execSQL(
                    "ALTER TABLE incomes ADD COLUMN exchangeRate REAL DEFAULT NULL"
                )
                database.execSQL(
                    "ALTER TABLE incomes ADD COLUMN bankName TEXT DEFAULT NULL"
                )
                database.execSQL(
                    "ALTER TABLE incomes ADD COLUMN bankCommission REAL DEFAULT NULL"
                )
                database.execSQL(
                    "ALTER TABLE incomes ADD COLUMN rateSource TEXT DEFAULT NULL"
                )
                database.execSQL(
                    "UPDATE incomes SET originalAmount = amount WHERE originalAmount = 0"
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS currency_exchanges (
                        id TEXT NOT NULL PRIMARY KEY,
                        fromAmount REAL NOT NULL,
                        fromCurrency TEXT NOT NULL,
                        toAmount REAL NOT NULL,
                        toCurrency TEXT NOT NULL,
                        exchangeRate REAL NOT NULL,
                        location TEXT DEFAULT NULL,
                        date INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_date` ON `expenses` (`date`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_incomes_date` ON `incomes` (`date`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_currency_exchanges_date` ON `currency_exchanges` (`date`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_currency_exchanges_fromCurrency_toCurrency` ON `currency_exchanges` (`fromCurrency`, `toCurrency`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_banks_isFavorite` ON `banks` (`isFavorite`)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_type` ON `categories` (`type`)")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expenses ADD COLUMN accountId TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE incomes ADD COLUMN accountId TEXT DEFAULT NULL")
            }
        }

        val PREPOPULATE_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    DEFAULT_BANKS.forEach { bank ->
                        db.execSQL(
                            "INSERT INTO banks (name, commissionPercent, isDefault, isFavorite) VALUES (?, ?, ?, ?)",
                            arrayOf(bank.name, bank.commissionPercent, if (bank.isDefault) 1 else 0, if (bank.isFavorite) 1 else 0)
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
                                "INSERT INTO banks (name, commissionPercent, isDefault, isFavorite) VALUES (?, ?, ?, ?)",
                                arrayOf(bank.name, bank.commissionPercent, if (bank.isDefault) 1 else 0, if (bank.isFavorite) 1 else 0)
                            )
                        }
                    }
                }
            }
        }
    }
}
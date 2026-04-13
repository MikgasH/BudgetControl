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
import com.example.budgetcontrol.core.data.local.database.dao.AccountDao
import com.example.budgetcontrol.core.data.local.database.dao.AccountGroupDao
import com.example.budgetcontrol.core.data.local.database.entities.AccountEntity
import com.example.budgetcontrol.core.data.local.database.entities.AccountGroupEntity
import com.example.budgetcontrol.core.data.local.database.entities.AccountGroupMemberEntity
import com.example.budgetcontrol.core.domain.model.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class, IncomeEntity::class, BankEntity::class, CurrencyExchangeEntity::class, AccountEntity::class, AccountGroupEntity::class, AccountGroupMemberEntity::class],
    version = 15,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun incomeDao(): IncomeDao
    abstract fun bankDao(): BankDao
    abstract fun currencyExchangeDao(): CurrencyExchangeDao
    abstract fun accountDao(): AccountDao
    abstract fun accountGroupDao(): AccountGroupDao

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
                // Backfill: originalAmount=0 is the DEFAULT from ALTER TABLE, not a real user value.
                // Real expenses always have amount > 0, so 0 safely identifies unmigrated rows.
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
                // Migrate legacy isDefault semantics: all default categories become system-protected
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

        // accountId is nullable to preserve existing single-account data without a destructive migration
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expenses ADD COLUMN accountId TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE incomes ADD COLUMN accountId TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS accounts (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        iconName TEXT NOT NULL,
                        color TEXT NOT NULL,
                        initialBalance REAL NOT NULL DEFAULT 0.0,
                        currency TEXT NOT NULL DEFAULT 'EUR',
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        lastUsedAt INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_isDefault` ON `accounts` (`isDefault`)")

                val now = System.currentTimeMillis()
                database.execSQL(
                    "INSERT INTO accounts (id, name, iconName, color, initialBalance, currency, isDefault, createdAt, lastUsedAt, sortOrder) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(Account.DEFAULT_ACCOUNT_ID, "Main", "account_balance", "#4CAF50", 0.0, "EUR", 1, now, now, 0)
                )

                database.execSQL(
                    "UPDATE expenses SET accountId = ? WHERE accountId IS NULL",
                    arrayOf(Account.DEFAULT_ACCOUNT_ID)
                )
                database.execSQL(
                    "UPDATE incomes SET accountId = ? WHERE accountId IS NULL",
                    arrayOf(Account.DEFAULT_ACCOUNT_ID)
                )
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS account_groups (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS account_group_members (
                        groupId TEXT NOT NULL,
                        accountId TEXT NOT NULL,
                        PRIMARY KEY(groupId, accountId),
                        FOREIGN KEY(groupId) REFERENCES account_groups(id) ON DELETE CASCADE,
                        FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_account_group_members_accountId` ON `account_group_members` (`accountId`)")
            }
        }

        val PREPOPULATE_CALLBACK = object : RoomDatabase.Callback() {
            // Only use onOpen (not onCreate) to avoid a race condition:
            // Room fires both callbacks on first creation, and each launches an async coroutine —
            // onOpen's count check can see 0 before onCreate's inserts complete, duplicating banks.
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

                    // Ensure default "Main" account exists (fresh installs skip MIGRATION_13_14)
                    val acctCursor = db.query("SELECT COUNT(*) FROM accounts WHERE isDefault = 1")
                    val hasDefault = if (acctCursor.moveToFirst()) acctCursor.getInt(0) > 0 else false
                    acctCursor.close()
                    if (!hasDefault) {
                        val now = System.currentTimeMillis()
                        db.execSQL(
                            "INSERT INTO accounts (id, name, iconName, color, initialBalance, currency, isDefault, createdAt, lastUsedAt, sortOrder) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            arrayOf(Account.DEFAULT_ACCOUNT_ID, "Main", "account_balance", "#4CAF50", 0.0, "EUR", 1, now, now, 0)
                        )
                    }
                }
            }
        }
    }
}
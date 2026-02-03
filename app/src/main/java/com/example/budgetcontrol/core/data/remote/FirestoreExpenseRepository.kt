package com.example.budgetcontrol.core.data.remote

import com.example.budgetcontrol.core.data.remote.models.FirestoreExpense
import com.example.budgetcontrol.core.data.remote.models.toDomain
import com.example.budgetcontrol.core.data.remote.models.toFirestore
import com.example.budgetcontrol.core.domain.model.Expense
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreExpenseRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val expensesCollection = firestore.collection("expenses")

    // Загрузить все траты из Firestore
    suspend fun getAllExpenses(): List<Expense> {
        return try {
            val snapshot = expensesCollection.get().await()
            snapshot.documents.mapNotNull { document ->
                document.toObject(FirestoreExpense::class.java)?.toDomain()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Сохранить трату в Firestore
    suspend fun saveExpense(expense: Expense): Boolean {
        return try {
            val firestoreExpense = expense.toFirestore()
            expensesCollection.document(expense.id).set(firestoreExpense).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Сохранить все траты в Firestore (backup)
    suspend fun saveAllExpenses(expenses: List<Expense>): Boolean {
        return try {
            android.util.Log.d("Firestore", "Saving ${expenses.size} expenses")
            val batch = firestore.batch()
            expenses.forEach { expense ->
                val firestoreExpense = expense.toFirestore()
                val docRef = expensesCollection.document(expense.id)
                batch.set(docRef, firestoreExpense)
            }

            withTimeout(30000) { // 30 секунд
                batch.commit().await()
            }
            android.util.Log.d("Firestore", "Save successful")
            true
        } catch (e: Exception) {
            android.util.Log.e("Firestore", "Save failed: ${e.message}")
            false
        }
    }

    // Удалить трату из Firestore
    suspend fun deleteExpense(expenseId: String): Boolean {
        return try {
            expensesCollection.document(expenseId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Очистить все данные в Firestore
    suspend fun clearAllExpenses(): Boolean {
        return try {
            val snapshot = expensesCollection.get().await()
            val batch = firestore.batch()
            snapshot.documents.forEach { document ->
                batch.delete(document.reference)
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
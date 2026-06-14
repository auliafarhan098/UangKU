package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String, // "Makanan", "Transportasi", "Kuliah", "Hiburan", "Gaji", "Tabungan", "Lainnya"
    val descriptionEncrypted: String, // AES Encrypted
    val timestamp: Long,
    val bankName: String, // "CASH", "BCA", "MANDIRI", "BNI", "OVO", "GOPAY"
    val isSynced: Boolean = false
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String, // Budget tracks per category key
    val limitAmount: Double,
    val monthYear: String // "MM/YYYY" e.g., "06/2026"
)

@Entity(tableName = "saving_goals")
data class SavingGoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val deadline: Long
)

@Entity(tableName = "bill_reminders")
data class BillReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val dueDate: Long,
    val isPaid: Boolean,
    val frequency: String // "ONCE", "MONTHLY", "WEEKLY"
)

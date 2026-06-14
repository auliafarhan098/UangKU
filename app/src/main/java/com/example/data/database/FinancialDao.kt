package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialDao {

    // --- TRANSACTION CRUD ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    // --- BUDGET CRUD ---
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudget(category: String)

    // --- SAVINGS CRUD ---
    @Query("SELECT * FROM saving_goals ORDER BY deadline ASC")
    fun getAllSavingGoals(): Flow<List<SavingGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingGoal(goal: SavingGoalEntity)

    @Query("DELETE FROM saving_goals WHERE id = :id")
    suspend fun deleteSavingGoal(id: String)

    // --- BILL REMINDERS CRUD ---
    @Query("SELECT * FROM bill_reminders ORDER BY dueDate ASC")
    fun getAllBillReminders(): Flow<List<BillReminderEntity>>

    @Query("SELECT * FROM bill_reminders WHERE isPaid = 0 ORDER BY dueDate ASC")
    fun getUnpaidBillReminders(): Flow<List<BillReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillReminder(reminder: BillReminderEntity)

    @Query("DELETE FROM bill_reminders WHERE id = :id")
    suspend fun deleteBillReminder(id: String)
}

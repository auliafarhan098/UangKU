package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.*
import com.example.data.security.CryptoHelper
import com.example.network.FirebaseClient
import com.example.network.FirebaseTransactionDto
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class FinancialRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.financialDao()

    // --- STREAM REPOSITORIES ---
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = dao.getAllBudgets()
    val allSavingGoals: Flow<List<SavingGoalEntity>> = dao.getAllSavingGoals()
    val allBillReminders: Flow<List<BillReminderEntity>> = dao.getAllBillReminders()
    val unpaidBillReminders: Flow<List<BillReminderEntity>> = dao.getUnpaidBillReminders()

    // --- TRANSACTION CRUD ---
    suspend fun insertTransaction(
        id: String? = null,
        amount: Double,
        type: String,
        category: String,
        description: String,
        bankName: String,
        isSynced: Boolean = false,
        pin: String? = null
    ) {
        val targetId = id ?: UUID.randomUUID().toString()
        val encryptedDesc = CryptoHelper.encrypt(description, pin)
        val transaction = TransactionEntity(
            id = targetId,
            amount = amount,
            type = type,
            category = category,
            descriptionEncrypted = encryptedDesc,
            timestamp = System.currentTimeMillis(),
            bankName = bankName,
            isSynced = isSynced
        )
        dao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(id: String, firebaseBaseUrl: String? = null, userId: String? = null) {
        dao.deleteTransaction(id)
        if (!firebaseBaseUrl.isNullOrEmpty() && !userId.isNullOrEmpty()) {
            try {
                // Delete from cloud concurrently as part of Firebase Realtime Database design
                FirebaseClient.getService(firebaseBaseUrl).deleteTransaction(userId, id)
            } catch (e: Exception) {
                Log.e("FinancialRepository", "Failed to delete transaction from Firebase: ${e.message}")
            }
        }
    }

    suspend fun clearTransactions() {
        dao.clearAllTransactions()
    }

    // --- BUDGET CRUD ---
    suspend fun insertBudget(category: String, limitAmount: Double, monthYear: String) {
        dao.insertBudget(BudgetEntity(category, limitAmount, monthYear))
    }

    suspend fun deleteBudget(category: String) {
        dao.deleteBudget(category)
    }

    // --- SAVINGS CRUD ---
    suspend fun insertSavingGoal(id: String? = null, name: String, targetAmount: Double, currentAmount: Double, deadline: Long) {
        val targetId = id ?: UUID.randomUUID().toString()
        dao.insertSavingGoal(SavingGoalEntity(targetId, name, targetAmount, currentAmount, deadline))
    }

    suspend fun deleteSavingGoal(id: String) {
        dao.deleteSavingGoal(id)
    }

    // --- BILL REMINDERS CRUD ---
    suspend fun insertBillReminder(id: String? = null, title: String, amount: Double, dueDate: Long, isPaid: Boolean, frequency: String) {
        val targetId = id ?: UUID.randomUUID().toString()
        dao.insertBillReminder(BillReminderEntity(targetId, title, amount, dueDate, isPaid, frequency))
    }

    suspend fun deleteBillReminder(id: String) {
        dao.deleteBillReminder(id)
    }

    // --- BANK INTEGRATION: TEXT / STATEMENT PARSER ---
    /**
     * Parses raw pasted text from and Indonesian banking app (e.g. m-BCA, Livin Mandiri)
     * and automatically inserts it as a transaction.
     */
    suspend fun parseAndSaveBankPastedText(text: String, pin: String? = null): TransactionEntity? {
        val cleanText = text.uppercase()
        var bankName = "MANUAL"
        var amount = 0.0
        var type = "EXPENSE"
        var category = "Lainnya"

        if (cleanText.contains("BCA")) {
            bankName = "BCA"
        } else if (cleanText.contains("MANDIRI") || cleanText.contains("LIVIN")) {
            bankName = "MANDIRI"
        } else if (cleanText.contains("OVO")) {
            bankName = "OVO"
            category = "Makanan"
        } else if (cleanText.contains("GOPAY")) {
            bankName = "GOPAY"
            category = "Transportasi"
        } else if (cleanText.contains("DANA")) {
            bankName = "DANA"
        }

        // Detect direction
        if (cleanText.contains("TRANSFER MASUK") || cleanText.contains("KREDIT") || cleanText.contains("DIKREDIT") || cleanText.contains("TERIMA TRANSFER") || cleanText.contains("INCOME")) {
            type = "INCOME"
            category = "Gaji"
        }

        // Try extracting amount
        val regex = Regex("""(?:RP\.?|RP\s?)\s*([\d.,]+)""")
        val match = regex.find(cleanText)
        if (match != null) {
            var amountStr = match.groupValues[1]
            if (amountStr.contains(",")) {
                val parts = amountStr.split(",")
                if (parts.size == 2 && parts[1].length == 2) {
                    amountStr = parts[0]
                }
            }
            amountStr = amountStr.replace(".", "").replace(",", "")
            amount = amountStr.toDoubleOrNull() ?: 0.0
        }

        if (amount > 0) {
            val uuid = UUID.randomUUID().toString()
            val shortDesc = "E-Statement: $bankName Import"
            val encrypted = CryptoHelper.encrypt(shortDesc, pin)
            val entity = TransactionEntity(
                id = uuid,
                amount = amount,
                type = type,
                category = category,
                descriptionEncrypted = encrypted,
                timestamp = System.currentTimeMillis(),
                bankName = bankName,
                isSynced = false
            )
            dao.insertTransaction(entity)
            return entity
        }
        return null
    }

    // --- FIREBASE CRUD SYNCHRONIZATION ---
    /**
     * Uploads all transactions that are currently labelled isSynced = false
     * to the user's personal Firebase Realtime Database path.
     */
    suspend fun syncLocalToCloud(firebaseUrl: String, userId: String): Result<Unit> {
        return try {
            val unsynced = dao.getUnsyncedTransactions()
            val service = FirebaseClient.getService(firebaseUrl)
            
            for (txn in unsynced) {
                val dto = FirebaseTransactionDto(
                    id = txn.id,
                    amount = txn.amount,
                    type = txn.type,
                    category = txn.category,
                    descriptionEncrypted = txn.descriptionEncrypted,
                    timestamp = txn.timestamp,
                    bankName = txn.bankName
                )
                val response = service.uploadTransaction(userId, txn.id, dto)
                if (response.isSuccessful) {
                    // Mark as synced locally
                    dao.insertTransaction(txn.copy(isSynced = true))
                } else {
                    return Result.failure(Exception("Cloud sync failed for transaction ${txn.id}: ${response.message()}"))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads user records from Firebase Cloud REST DB and merges them into the local database.
     */
    suspend fun syncCloudToLocal(firebaseUrl: String, userId: String): Result<Unit> {
        return try {
            val service = FirebaseClient.getService(firebaseUrl)
            val response = service.getAllTransactions(userId)
            
            if (response.isSuccessful) {
                val map = response.body()
                if (map != null) {
                    for ((_, dto) in map) {
                        val localEntity = TransactionEntity(
                            id = dto.id,
                            amount = dto.amount,
                            type = dto.type,
                            category = dto.category,
                            descriptionEncrypted = dto.descriptionEncrypted,
                            timestamp = dto.timestamp,
                            bankName = dto.bankName,
                            isSynced = true
                        )
                        dao.insertTransaction(localEntity)
                    }
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Cloud fetch failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.BillReminderEntity
import com.example.data.database.BudgetEntity
import com.example.data.database.SavingGoalEntity
import com.example.data.database.TransactionEntity
import com.example.data.receiver.BankSmsReceiver
import com.example.data.repository.FinancialRepository
import com.example.data.security.CryptoHelper
import com.example.network.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class FinancialViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FinancialRepository(application)

    // --- SECURITY & CONFIG STATE ---
    private val _securityPin = MutableStateFlow("")
    val securityPin: StateFlow<String> = _securityPin.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // Firebase Settings (can be customized by users in Settings tab!)
    private val _firebaseUrl = MutableStateFlow("https://uangku-aistudio-default-rtdb.firebaseio.com/")
    val firebaseUrl: StateFlow<String> = _firebaseUrl.asStateFlow()

    private val _firebaseUserId = MutableStateFlow("mhs_pro_user_88")
    val firebaseUserId: StateFlow<String> = _firebaseUserId.asStateFlow()

    // Theme Mode: "AUTO", "LIGHT", "DARK"
    private val _themeMode = MutableStateFlow("AUTO")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // --- OBSERVABLE DB FLOWS ---
    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingGoals: StateFlow<List<SavingGoalEntity>> = repository.allSavingGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val billReminders: StateFlow<List<BillReminderEntity>> = repository.allBillReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unpaidBills: StateFlow<List<BillReminderEntity>> = repository.unpaidBillReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- AI STRATEGIST ADVICE STATE ---
    private val _aiRecommendation = MutableStateFlow<String>("")
    val aiRecommendation: StateFlow<String> = _aiRecommendation.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    // --- SYNC STATE ---
    private val _syncMessage = MutableStateFlow<String>("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    init {
        createNotificationChannelFromApp()
        // Trigger smart reminders check
        viewModelScope.launch {
            unpaidBills.collect { bills ->
                checkAndNotifyUpcomingBills(bills)
            }
        }
    }

    // --- SECURITY CONTROLLERS ---
    fun setSecurityPin(pin: String) {
        _securityPin.value = pin
        if (pin.length >= 4) {
            _isLocked.value = true
        } else {
            _isLocked.value = false
        }
    }

    fun unlockApp(pin: String): Boolean {
        return if (pin == _securityPin.value) {
            _isLocked.value = false
            true
        } else {
            false
        }
    }

    fun removeSecurity() {
        _securityPin.value = ""
        _isLocked.value = false
    }

    // --- THEME SETTING ---
    fun changeThemeMode(mode: String) {
        _themeMode.value = mode
    }

    fun isDarkThemeActive(): Boolean {
        return when (_themeMode.value) {
            "LIGHT" -> false
            "DARK" -> true
            else -> { // AUTO-TIME BASED
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                hour < 6 || hour >= 18 // Dark at night (18:00 - 06:00)
            }
        }
    }

    // --- MANUAL FIREBASE ENDPOINT UPDATE ---
    fun updateFirebaseConfigs(url: String, userId: String) {
        _firebaseUrl.value = url
        _firebaseUserId.value = userId
    }

    // --- SYNC ACTIONS (CRUD FIREBASE) ---
    fun performFullCloudSync() {
        viewModelScope.launch {
            _syncing.value = true
            _syncMessage.value = "Menghubungkan ke Firebase Cloud..."
            
            // 1. Upload unsynced local data to Cloud
            val uploadResult = repository.syncLocalToCloud(_firebaseUrl.value, _firebaseUserId.value)
            
            // 2. Download any remote transactions
            val downloadResult = repository.syncCloudToLocal(_firebaseUrl.value, _firebaseUserId.value)

            _syncing.value = false
            if (uploadResult.isSuccess && downloadResult.isSuccess) {
                _syncMessage.value = "Sinkronisasi Berhasil! Data lokal dan awan Firebase telah terpadu."
            } else {
                val errorMsg = uploadResult.exceptionOrNull()?.message ?: downloadResult.exceptionOrNull()?.message ?: "Gagal terhubung"
                _syncMessage.value = "Gagal sinkron: $errorMsg"
            }
        }
    }

    // --- TRANSACTION CREATORS WITH ENCRYPTION ---
    fun addTransaction(amount: Double, type: String, category: String, description: String, bankName: String) {
        viewModelScope.launch {
            repository.insertTransaction(
                amount = amount,
                type = type,
                category = category,
                description = description,
                bankName = bankName,
                pin = _securityPin.value.ifEmpty { null }
            )
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(
                id,
                _firebaseUrl.value,
                _firebaseUserId.value
            )
        }
    }

    fun clearAllLocally() {
        viewModelScope.launch {
            repository.clearTransactions()
        }
    }

    // --- BANKING IMPORT PARSER ---
    fun pasteEStatement(text: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val entity = repository.parseAndSaveBankPastedText(text, _securityPin.value.ifEmpty { null })
            if (entity != null) {
                onSuccess()
            } else {
                onFailure()
            }
        }
    }

    // --- AUTOMATIC BANK SIMULATOR ---
    fun simulateBankSmsNotification(bank: String, amount: Double, isIncome: Boolean) {
        viewModelScope.launch {
            val typeStr = if (isIncome) "kredit" else "debet"
            val text = "Notifikasi $bank: Transaksi $typeStr sebesar Rp$amount berhasil diproses."
            val sender = "08112233-$bank"
            BankSmsReceiver.parseAndSaveSms(getApplication(), text, sender)
        }
    }

    // --- BUDGETS CRUD ---
    fun saveBudget(category: String, limit: Double) {
        viewModelScope.launch {
            repository.insertBudget(category, limit, "06/2026")
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            repository.deleteBudget(category)
        }
    }

    // --- MENABUNG (SAVINGS) GOALS ---
    fun saveSavingGoal(id: String? = null, name: String, target: Double, current: Double, deadline: Long) {
        viewModelScope.launch {
            repository.insertSavingGoal(id, name, target, current, deadline)
        }
    }

    fun contributeSaving(goal: SavingGoalEntity, amount: Double) {
        viewModelScope.launch {
            // 1. Log transaction as expense / savings
            repository.insertTransaction(
                amount = amount,
                type = "EXPENSE",
                category = "Tabungan",
                description = "Menabung untuk: ${goal.name}",
                bankName = "CASH",
                pin = _securityPin.value.ifEmpty { null }
            )
            // 2. Update goal current amount
            val updatedAmount = goal.currentAmount + amount
            repository.insertSavingGoal(
                id = goal.id,
                name = goal.name,
                targetAmount = goal.targetAmount,
                currentAmount = updatedAmount,
                deadline = goal.deadline
            )
        }
    }

    fun deleteSavingGoal(id: String) {
        viewModelScope.launch {
            repository.deleteSavingGoal(id)
        }
    }

    // --- BILL REMINDERS CRUD ---
    fun saveBillReminder(title: String, amount: Double, dueDate: Long, frequency: String) {
        viewModelScope.launch {
            repository.insertBillReminder(null, title, amount, dueDate, false, frequency)
        }
    }

    fun markBillAsPaid(reminder: BillReminderEntity) {
        viewModelScope.launch {
            // Log as transaction expense
            repository.insertTransaction(
                amount = reminder.amount,
                type = "EXPENSE",
                category = "Tagihan",
                description = "Bayar Tagihan: ${reminder.title}",
                bankName = "CASH",
                pin = _securityPin.value.ifEmpty { null }
            )
            // Mark bill paid
            repository.insertBillReminder(
                id = reminder.id,
                title = reminder.title,
                amount = reminder.amount,
                dueDate = reminder.dueDate,
                isPaid = true,
                frequency = reminder.frequency
            )
        }
    }

    fun deleteBillReminder(id: String) {
        viewModelScope.launch {
            repository.deleteBillReminder(id)
        }
    }

    // --- AI BUDGET ADVISOR (GEMINI REST) ---
    fun fetchAiBudgetRecommendations() {
        val rawTxns = transactions.value
        val rawBudgets = budgets.value
        val rawGoals = savingGoals.value

        if (rawTxns.isEmpty()) {
            _aiRecommendation.value = "Silakan masukkan setidaknya beberapa transaksi keuangan agar AI UangKu dapat menganalisis data pengeluaran dan memberikan saran anggaran bulanan untuk menghemat uang Anda."
            return
        }

        viewModelScope.launch {
            _aiLoading.value = true
            _aiRecommendation.value = "Menganalisis anggaran Anda melalui kecerdasan buatan..."

            // Build structural context text and decrypt for AI analysis
            val txLogs = rawTxns.take(30).joinToString("\n") { tx ->
                val decryptedDesc = CryptoHelper.decrypt(tx.descriptionEncrypted, _securityPin.value.ifEmpty { null })
                "- ${if (tx.type == "INCOME") "Pemasukan" else "Pengeluaran"}: Rp${tx.amount} di kategori [${tx.category}] ($decryptedDesc) via ${tx.bankName}"
            }

            val bgtLogs = rawBudgets.joinToString("\n") { b ->
                "- Anggaran Kategori [${b.category}] batas Rp${b.limitAmount}"
            }

            val savingLogs = rawGoals.joinToString("\n") { g ->
                "- Target tabungan [${g.name}]: Rp${g.currentAmount} terkumpul dari target Rp${g.targetAmount}"
            }

            val prompt = """
                Anda adalah Ahli Perencana Keuangan UangKu khusus mahasiswa dan profesional muda di Indonesia.
                Tolong analisis data keuangan pengguna berikut untuk memberikan evaluasi penghematan:
                
                DAFTAR TRANSAKSI BARU-BARU INI:
                $txLogs
                
                ANGGARAN BULANAN YANG DIKONFIGURASI:
                $bgtLogs
                
                TARGET MENABUNG:
                $savingLogs
                
                Tolong berikan laporan dalam Bahasa Indonesia yang visual, bersahabat, bersahabat, ringkas, dan penuh solusi konkrit:
                1. Analisis pola pengeluaran terbesar yang tidak hemat.
                2. Bandingkan pengeluaran terhadap anggaran batas (jika diatur). Apakah melebihi batas?
                3. Berikan 3 Tips Konkrit & Solusi untuk menghemat uang hingga 20% bulan depan yang disesuaikan dengan kondisi mahasiswa atau profesional muda.
                4. Semangat dan motivasi singkat agar mereka sukses menabung untuk target mereka.
            """.trimIndent()

            val advice = withContext(Dispatchers.IO) {
                GeminiClient.getFinancialAdvice(prompt)
            }

            _aiRecommendation.value = advice
            _aiLoading.value = false
        }
    }

    // --- SMART NOTIFICATION SYSTEM ---
    private fun createNotificationChannelFromApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "UangKu Bill Reminder"
            val descriptionText = "Saluran Notifikasi Pintar Pengingat Tagihan Tepat Waktu UangKu"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("uangku_bill_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkAndNotifyUpcomingBills(bills: List<BillReminderEntity>) {
        val now = System.currentTimeMillis()
        val twoDaysMillis = 2 * 24 * 60 * 60 * 1000L

        for (bill in bills) {
            if (!bill.isPaid && bill.dueDate > now && (bill.dueDate - now) <= twoDaysMillis) {
                triggerSystemNotification(
                    id = bill.id.hashCode(),
                    title = "Pengingat Tagihan: ${bill.title}",
                    message = "Tagihan sebesar Rp${bill.amount} akan jatuh tempo dalam waktu dekat pada ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(bill.dueDate))}. Bayar tepat waktu untuk mengatur arus kas keuangan Anda!"
                )
            }
        }
    }

    private fun triggerSystemNotification(id: Int, title: String, message: String) {
        val context = getApplication<Application>()
        val builder = NotificationCompat.Builder(context, "uangku_bill_channel")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                // Ignore warning if they didn't grant permission yet, but check gracefully
                notify(id, builder.build())
            } catch (e: SecurityException) {
                Log.e("FinancialViewModel", "POST_NOTIFICATIONS permission not granted: ${e.message}")
            }
        }
    }
}

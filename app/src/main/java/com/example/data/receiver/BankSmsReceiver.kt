package com.example.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.database.TransactionEntity
import com.example.data.security.CryptoHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class BankSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (sms in messages) {
                    val body = sms.messageBody ?: continue
                    val sender = sms.originatingAddress ?: "Unknown"
                    parseAndSaveSms(context, body, sender)
                }
            } catch (e: Exception) {
                Log.e("BankSmsReceiver", "Error parsing incoming SMS: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BankSmsReceiver"

        /**
         * Core parsing engine for bank SMS messages. Supports typical Indonesian notifications.
         */
        suspend fun parseAndSaveSms(context: Context, body: String, sender: String): TransactionEntity? {
            val text = body.uppercase()
            var bankName = "LAINNYA"
            var amount = 0.0
            var type = "EXPENSE" // Default to Expense (most SMS alerts are expense)
            var category = "Lainnya"

            // 1. Identify bank / institution
            if (sender.contains("BCA", true) || text.contains("BCA")) {
                bankName = "BCA"
            } else if (sender.contains("MANDIRI", true) || text.contains("MANDIRI")) {
                bankName = "MANDIRI"
            } else if (sender.contains("BNI", true) || text.contains("BNI")) {
                bankName = "BNI"
            } else if (sender.contains("GOPAY", true) || text.contains("GOPAY")) {
                bankName = "GOPAY"
                category = "Transportasi"
            } else if (sender.contains("OVO", true) || text.contains("OVO")) {
                bankName = "OVO"
                category = "Makanan"
            }

            // 2. Identify transaction direction (INCOME vs EXPENSE)
            if (text.contains("KREDIT") || text.contains("MASUK") || text.contains("DIKREDIT") || text.contains("TRANSFER-IN") || text.contains("DITERIMA")) {
                type = "INCOME"
                category = "Gaji"
            } else if (text.contains("DEBET") || text.contains("TRANSFER-OUT") || text.contains("DEBIT") || text.contains("SEBESAR RP") || text.contains("BAYARKE") || text.contains("PEMBAYARAN")) {
                type = "EXPENSE"
            }

            // 3. Extract money amounts (regexes for IDR currencies like Rp 150.000 or RP1.500.000,50)
            val regex = Regex("""(?:RP\.?|RP\s?)\s*([\d.,]+)""")
            val match = regex.find(text)
            if (match != null) {
                var amountStr = match.groupValues[1]
                // Cleanup points and commas standard in Indonesian system
                // (e.g. 150.000,00 -> remove trailing ,00 then remove we dots)
                if (amountStr.contains(",")) {
                    val parts = amountStr.split(",")
                    if (parts.size == 2 && parts[1].length == 2) {
                        amountStr = parts[0] // strip cents
                    }
                }
                amountStr = amountStr.replace(".", "").replace(",", "")
                amount = amountStr.toDoubleOrNull() ?: 0.0
            }

            if (amount <= 0.0) {
                // Try alternate simple digits extractor if currency symbol wasn't adjacent
                val digitRegex = Regex("""(\d{3,10})""")
                val digitMatch = digitRegex.find(text)
                if (digitMatch != null) {
                    amount = digitMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                }
            }

            if (amount > 0) {
                val db = AppDatabase.getInstance(context)
                val rawDesc = "Bank Sync: $bankName - $body"
                val encryptedDesc = CryptoHelper.encrypt(rawDesc)
                
                val txn = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    type = type,
                    category = category,
                    descriptionEncrypted = encryptedDesc,
                    timestamp = System.currentTimeMillis(),
                    bankName = bankName,
                    isSynced = false
                )
                
                db.financialDao().insertTransaction(txn)
                Log.d(TAG, "Successfully parsed bank SMS: $txn")
                return txn
            }
            return null
        }
    }
}

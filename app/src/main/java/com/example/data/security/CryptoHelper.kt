package com.example.data.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val ALGORITHM = "AES"
    
    // Default fallback salt/key generated for general privacy
    private val DEFAULT_KEY = byteArrayOf(
        0x55, 0x61, 0x6e, 0x67, 0x4b, 0x75, 0x53, 0x65, // "UangKuSe"
        0x63, 0x75, 0x72, 0x65, 0x4b, 0x65, 0x79, 0x21  // "cureKey!"
    )

    /**
     * Encrypts plain text using either a user-provided PIN or device-level salt.
     */
    fun encrypt(plainText: String, pin: String? = null): String {
        return try {
            val keyBytes = getSecretKey(pin)
            val secretKey = SecretKeySpec(keyBytes, ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            plainText // Fallback to raw text if error occurs
        }
    }

    /**
     * Decrypts ciphertext using either a user-provided PIN or device-level salt.
     */
    fun decrypt(cipherText: String, pin: String? = null): String {
        return try {
            val keyBytes = getSecretKey(pin)
            val secretKey = SecretKeySpec(keyBytes, ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = Base64.decode(cipherText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            cipherText // Fallback to raw text if error/wrong PIN
        }
    }

    private fun getSecretKey(pin: String?): ByteArray {
        if (pin.isNullOrEmpty() || pin.length < 4) {
            return DEFAULT_KEY
        }
        // Pad or truncate user PIN to exactly 16 bytes for AES-128
        val pinBytes = pin.toByteArray(Charsets.UTF_8)
        val result = ByteArray(16)
        for (i in 0 until 16) {
            result[i] = if (i < pinBytes.size) pinBytes[i] else (0xAA.toByte())
        }
        return result
    }
}

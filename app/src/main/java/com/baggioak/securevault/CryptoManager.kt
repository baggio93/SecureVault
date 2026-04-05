package com.baggioak.securevault

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    // Questi parametri DEVONO coincidere esattamente con quelli del tuo app.js
    private const val ITERATIONS = 100000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128 // La lunghezza del tag di autenticazione GCM in bit

    /**
     * Ricrea la chiave crittografica a partire dalla password inserita.
     * Corrisponde alla tua funzione JS: deriveKey(password, salt)
     */
    fun deriveKey(password: String, salt: String): SecretKeySpec {
        // Usiamo SHA-256 per l'hashing, come da standard moderno
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")

        // Trasformiamo le stringhe in array di byte/caratteri per la crittografia
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt.toByteArray(Charsets.UTF_8),
            ITERATIONS,
            KEY_LENGTH
        )

        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    /**
     * Decripta il pacchetto ricevuto dal database.
     * Corrisponde alla tua funzione JS che usa crypto.subtle.decrypt
     */
    fun decrypt(encryptedDataB64: String, ivB64: String, key: SecretKeySpec): String {
        try {
            // Decodifichiamo le stringhe Base64 in array di Byte
            val encryptedBytes = Base64.decode(encryptedDataB64, Base64.DEFAULT)
            val ivBytes = Base64.decode(ivB64, Base64.DEFAULT)

            // Prepariamo il "lucchetto" AES-GCM
            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_LENGTH, ivBytes)

            // Inizializziamo in modalità DECRYPT
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            // Sblocchiamo i dati!
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)

        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Impossibile decriptare i dati. Password errata o dati corrotti.")
        }
    }
    // NUOVA FUNZIONE PER CRIPTARE I DATI
    fun encrypt(plainText: String, secretKey: javax.crypto.spec.SecretKeySpec): Pair<String, String> {
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12) // GCM richiede un IV da 12 byte
        java.security.SecureRandom().nextBytes(iv)

        val parameterSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Convertiamo tutto in Base64 per poterlo salvare come testo nel database
        val ivString = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP)
        val encryptedString = android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.NO_WRAP)

        return Pair(encryptedString, ivString)
    }
}
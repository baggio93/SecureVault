package com.baggioak.securevault

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.spec.SecretKeySpec

class EditActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private val gson = Gson()
    private var existingItem: DecryptedVaultItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_edit)
        // --- INIZIO MODALITÀ SCHERMO INTERO (IMMERSIVA) ---
        val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)

        // Comportamento: se l'utente scorre dall'alto, la barra appare per un secondo e poi sparisce di nuovo
        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Nascondiamo la barra di stato (orologio/batteria)
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        // --- FINE MODALITÀ SCHERMO INTERO ---

        database = AppDatabase.getDatabase(this)

        // 1. Recuperiamo la Chiave Segreta dalla schermata precedente
        val secretKeyBytes = intent.getByteArrayExtra("SECRET_KEY")
        if (secretKeyBytes == null) {
            Toast.makeText(this, "Errore: Chiave non trovata", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        val tvEditTitle = findViewById<TextView>(R.id.tvEditTitle)
        val etPlatform = findViewById<EditText>(R.id.etEditPlatform)
        val etUsername = findViewById<EditText>(R.id.etEditUsername)
        val etPassword = findViewById<EditText>(R.id.etEditPassword)
        val etPin = findViewById<EditText>(R.id.etEditPin)
        val etUrl = findViewById<EditText>(R.id.etEditUrl)
        val etNotes = findViewById<EditText>(R.id.etEditNotes)
        val btnSaveItem = findViewById<Button>(R.id.btnSaveItem)
        val etTags = findViewById<EditText>(R.id.etTags)

        // 2. Controlliamo se stiamo Modificando o Creando una nuova password
        val jsonItem = intent.getStringExtra("ITEM_DATA")
        if (jsonItem != null) {
            // MODIFICA
            existingItem = gson.fromJson(jsonItem, DecryptedVaultItem::class.java)
            tvEditTitle.text = "Modifica Password"

            etPlatform.setText(existingItem?.platform)
            etUsername.setText(existingItem?.username)
            etPassword.setText(existingItem?.password)
            etPin.setText(existingItem?.pin)
            etUrl.setText(existingItem?.url)
            etNotes.setText(existingItem?.notes)
            etTags.setText(existingItem?.tags ?: "")
        }

        // 3. Salvataggio
        btnSaveItem.setOnClickListener {
            val platform = etPlatform.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()
            val pin = etPin.text.toString() // Recuperiamo il PIN qui
            val url = etUrl.text.toString()
            val notes = etNotes.text.toString()
            val tags = etTags.text.toString()

            if (platform.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Piattaforma e Password sono obbligatori!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generiamo la data attuale in formato ISO (quello che usa il tuo server)
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val currentDate = sdf.format(Date())

            // --- INIZIO LOGICA STORICO PASSWORD E PIN ---

            // Creiamo copie modificabili delle liste esistenti (o liste vuote se è nuovo)
            val currentPasswordHistory = existingItem?.passwordHistory?.toMutableList() ?: mutableListOf()
            val currentPinHistory = existingItem?.pinHistory?.toMutableList() ?: mutableListOf()

            // 1. Estraiamo la vecchia password (se l'oggetto non esiste, sarà null)
            val oldPassword = existingItem?.password

// 2. Controllo Sicuro: se NON è nulla o vuota, E se è diversa da quella nuova
            if (!oldPassword.isNullOrBlank() && oldPassword != password) {
                // 3. Aggiungiamo la VECCHIA password allo storico
                currentPasswordHistory.add(PasswordHistoryItem(oldPassword, currentDate))
            }

            // Controllo PIN: stessa cosa per il PIN
            if (existingItem != null && existingItem!!.pin != pin && !existingItem!!.pin.isNullOrBlank()) {
                // Aggiungiamo il VECCHIO PIN allo storico, con la data del cambio (oggi)
                currentPinHistory.add(PinHistoryItem(existingItem!!.pin!!, currentDate))
            }
            // --- FINE LOGICA STORICO ---

            // RECUPERIAMO L'ID: Se stiamo modificando, DEVE essere l'id originale del server
            val targetId = existingItem?.id ?: (System.currentTimeMillis() / -1000).toInt()

            android.util.Log.d("EDIT_DEBUG", "Sto salvando l'ID: $targetId - Stato: ${if(existingItem == null) "NUOVO" else "MODIFICA"}")

            // Aggiorniamo o creiamo l'oggetto decriptato inserendo le liste aggiornate
            val updatedDecryptedItem = DecryptedVaultItem(
                id = targetId,
                platform = platform,
                username = username,
                password = password,
                pin = pin,
                url = url,
                notes = notes,
                created_at = existingItem?.created_at ?: currentDate,
                updated_at = currentDate,
                passwordHistory = currentPasswordHistory, // LISTA AGGIORNATA
                pinHistory = currentPinHistory,            // LISTA AGGIORNATA
                tags = tags
            )

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Trasformiamo i dati in JSON e li CRIPTIAMO!
                    val jsonToEncrypt = gson.toJson(updatedDecryptedItem)
                    val (encryptedData, iv) = CryptoManager.encrypt(jsonToEncrypt, secretKey)

                    // Prepariamo l'entità per il database locale (con etichetta NEW o MODIFIED)
                    val syncStatus = if (existingItem == null) "NEW" else "MODIFIED"
                    val entityToSave = VaultEntity(
                        id = updatedDecryptedItem.id,
                        encrypted_data = encryptedData,
                        iv = iv,
                        updated_at = currentDate,
                        sync_status = syncStatus
                    )

                    // Salviamo nel database
                    database.vaultDao().insertAll(listOf(entityToSave))

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditActivity, "Salvato in locale!", Toast.LENGTH_SHORT).show()
                        finish() // Chiudiamo la schermata e torniamo indietro
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditActivity, "Errore di crittografia: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
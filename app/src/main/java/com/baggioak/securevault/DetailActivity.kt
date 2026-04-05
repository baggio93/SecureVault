package com.baggioak.securevault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity() {

    private var isPassVisible = false
    private var isPinVisible = false
    private var isHistoryVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_detail)
        // --- INIZIO MODALITÀ SCHERMO INTERO (IMMERSIVA) ---
        val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)

        // Comportamento: se l'utente scorre dall'alto, la barra appare per un secondo e poi sparisce di nuovo
        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Nascondiamo la barra di stato (orologio/batteria)
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        // --- FINE MODALITÀ SCHERMO INTERO ---

        // 1. Riceviamo i dati in formato JSON passati dalla schermata precedente
        val jsonItem = intent.getStringExtra("ITEM_DATA") ?: return
        val item = Gson().fromJson(jsonItem, DecryptedVaultItem::class.java)

        // 2. Colleghiamo gli elementi grafici
        val tvPlatform = findViewById<TextView>(R.id.tvDetailPlatform)
        val tvUsername = findViewById<TextView>(R.id.tvDetailUsername)
        val tvPassword = findViewById<TextView>(R.id.tvDetailPassword)
        val tvPin = findViewById<TextView>(R.id.tvDetailPin)
        val tvUrl = findViewById<TextView>(R.id.tvDetailUrl)
        val tvNotes = findViewById<TextView>(R.id.tvDetailNotes)
        val tvHistory = findViewById<TextView>(R.id.tvDetailHistory)

        // Bottoni Copy
        val btnCopyUser = findViewById<Button>(R.id.btnCopyUser)
        val btnCopyPass = findViewById<Button>(R.id.btnCopyPass)
        val btnCopyPin = findViewById<Button>(R.id.btnCopyPin)

        // Bottoni Toggle (Occhio)
        val btnTogglePass = findViewById<Button>(R.id.btnTogglePass)
        val btnTogglePin = findViewById<Button>(R.id.btnTogglePin)
        val btnToggleHistory = findViewById<Button>(R.id.btnToggleHistory)

        // 3. Compiliamo i campi di testo base
        tvPlatform.text = if (item.platform.isNullOrBlank()) "Senza Titolo" else item.platform
        tvUsername.text = item.username
        tvUrl.text = if (item.url.isNullOrBlank()) "Nessun URL" else item.url
        tvNotes.text = if (item.notes.isNullOrBlank()) "Nessuna nota" else item.notes

        // 4. Funzioni per Copiare negli appunti
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        btnCopyUser.setOnClickListener {
            clipboard.setPrimaryClip(ClipData.newPlainText("Username", item.username))
            Toast.makeText(this, "Username copiato", Toast.LENGTH_SHORT).show()
        }

        btnCopyPass.setOnClickListener {
            clipboard.setPrimaryClip(ClipData.newPlainText("Password", item.password))
            Toast.makeText(this, "Password copiata", Toast.LENGTH_SHORT).show()
        }

        btnCopyPin.setOnClickListener {
            val pinToCopy = if (item.pin.isNullOrBlank()) "" else item.pin
            clipboard.setPrimaryClip(ClipData.newPlainText("PIN", pinToCopy))
            Toast.makeText(this, "PIN copiato", Toast.LENGTH_SHORT).show()
        }

        // 5. Funzioni per Mostrare/Nascondere le Password e i PIN
        btnTogglePass.setOnClickListener {
            isPassVisible = !isPassVisible
            tvPassword.text = if (isPassVisible) item.password else "••••••••"
            btnTogglePass.text = if (isPassVisible) "NASCONDI" else "MOSTRA"
        }

        btnTogglePin.setOnClickListener {
            isPinVisible = !isPinVisible
            val actualPin = if (item.pin.isNullOrBlank()) "Nessun PIN" else item.pin
            tvPin.text = if (isPinVisible) actualPin else "••••"
            btnTogglePin.text = if (isPinVisible) "NASCONDI" else "MOSTRA"
        }

        val tvTagsLabel = findViewById<TextView>(R.id.tvTagsLabel)
        val tvTagsDetail = findViewById<TextView>(R.id.tvTagsDetail)

        // Se il tag c'è, lo mostriamo. Altrimenti nascondiamo sia il titolo che il testo
        if (!item.tags.isNullOrBlank()) {
            tvTagsDetail.text = item.tags
            tvTagsLabel.visibility = android.view.View.VISIBLE
            tvTagsDetail.visibility = android.view.View.VISIBLE
        } else {
            tvTagsLabel.visibility = android.view.View.GONE
            tvTagsDetail.visibility = android.view.View.GONE
        }
        // 6. Costruzione dello Storico (History)
        val historyBuilder = StringBuilder()
        item.passwordHistory?.forEach { h -> historyBuilder.append("Pass: ${h.password} (${h.changed_at})\n") }
        item.pinHistory?.forEach { h -> historyBuilder.append("PIN: ${h.pin} (${h.changed_at})\n") }
        val historyText = historyBuilder.toString()

        if (historyText.isBlank()) {
            tvHistory.text = "Nessuno storico disponibile."
        } else {
            tvHistory.text = historyText
        }

        btnToggleHistory.setOnClickListener {
            isHistoryVisible = !isHistoryVisible
            if (isHistoryVisible) {
                tvHistory.visibility = View.VISIBLE
                btnToggleHistory.text = "NASCONDI STORICO"
            } else {
                tvHistory.visibility = View.GONE
                btnToggleHistory.text = "MOSTRA STORICO"
            }
        }
        val btnEditItem = findViewById<Button>(R.id.btnEditItem)
        btnEditItem.setOnClickListener {
            val editIntent = android.content.Intent(this, EditActivity::class.java)
            editIntent.putExtra("ITEM_DATA", jsonItem) // Passiamo i dati esistenti
            editIntent.putExtra("SECRET_KEY", intent.getByteArrayExtra("SECRET_KEY")) // Passiamo la chiave
            startActivity(editIntent)
            finish() // Chiudiamo il dettaglio, così al termine si torna alla lista principale
        }
        // Logica per ELIMINARE
        val btnDeleteItem = findViewById<Button>(R.id.btnDeleteItem)
        val database = AppDatabase.getDatabase(this) // Istanziamo il database

        btnDeleteItem.setOnClickListener {
            // Creiamo il Popup di conferma
            AlertDialog.Builder(this)
                .setTitle("Elimina Password")
                .setMessage("Sei sicuro di voler eliminare questa password? Sarà cancellata anche dal server alla prossima sincronizzazione.")
                .setPositiveButton("ELIMINA") { _, _ ->

                    // IL PEZZO CORRETTO È QUI SOTTO:
                    lifecycleScope.launch(Dispatchers.IO) {

                        // Usiamo l'ID dell'oggetto che stiamo visualizzando
                        database.vaultDao().markAsDeleted(item.id)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@DetailActivity, "Password eliminata!", Toast.LENGTH_SHORT).show()
                            finish() // Chiude la pagina e torna alla lista
                        }
                    }
                }
                .setNegativeButton("ANNULLA", null) // Se preme annulla, non fa nulla
                .show()
        }
    }
}
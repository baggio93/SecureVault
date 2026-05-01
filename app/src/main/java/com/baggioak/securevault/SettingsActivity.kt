package com.baggioak.securevault

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity che gestisce le impostazioni dell'applicazione.
 * Permette all'utente di configurare l'IP del server, il timeout di rete,
 * forzare la sincronizzazione ed eliminare i dati locali (Wipe).
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase

    /**
     * Metodo chiamato alla creazione della schermata.
     * Inizializza i componenti grafici, imposta i valori correnti salvati in memoria
     * e configura le azioni dei vari bottoni.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_settings)

        // --- INIZIO MODALITÀ SCHERMO INTERO (IMMERSIVA) ---
        val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)

        // Comportamento: se l'utente scorre dall'alto, la barra appare per un secondo e poi sparisce di nuovo
        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Nascondiamo la barra di stato (orologio/batteria)
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        // --- FINE MODALITÀ SCHERMO INTERO ---

        database = AppDatabase.getDatabase(this)

        val etServerIp = findViewById<EditText>(R.id.etServerIp)
        val etTimeout = findViewById<EditText>(R.id.etTimeout)
        val btnSaveIp = findViewById<Button>(R.id.btnSaveIp)
        val btnForceSync = findViewById<Button>(R.id.btnForceSync)
        val btnWipeData = findViewById<Button>(R.id.btnWipeData)

        // 1. Carica il timeout attuale tramite il Singleton SettingsManager
        val currentTimeoutSec = SettingsManager.getTimeoutMillis(this) / 1000f
        etTimeout.setText(currentTimeoutSec.toString())

        // 2. Carica l'IP attuale
        etServerIp.setText(SettingsManager.getServerIp(this))

        // 3. Salva il nuovo IP e il nuovo Timeout
        btnSaveIp.setOnClickListener {
            val newIp = etServerIp.text.toString().trim()
            if (newIp.isNotEmpty()) {
                SettingsManager.setServerIp(this, newIp)
                ApiClient.resetClient()
                Toast.makeText(this, "IP Salvato e Client resettato!", Toast.LENGTH_SHORT).show()
            }

            // Leggi il numero digitato e trasformalo in millisecondi (es. 1.5 * 1000 = 1500)
            val timeoutStr = etTimeout.text.toString()
            val timeoutMillis = if (timeoutStr.isNotEmpty()) {
                (timeoutStr.toFloat() * 1000).toLong()
            } else {
                1500L // valore di sicurezza se lasci vuoto
            }
            // Usa il Singleton passando il context (this)
            SettingsManager.setTimeoutMillis(this, timeoutMillis)
        }

        // 4. Forza Sincronizzazione
        btnForceSync.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                // Cancelliamo solo i dati "SYNCED", mantenendo quelli che stiamo modificando (NEW/MODIFIED)
                database.vaultDao().clearSyncedItems()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Cache pulita. I dati verranno riscaricati.", Toast.LENGTH_SHORT).show()
                    // Diciamo alla MainActivity di ricaricare i dati quando ci torniamo
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }

        // 5. Wipe Data (Elimina tutto)
        btnWipeData.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Attenzione!")
                .setMessage("Sei sicuro di voler eliminare TUTTI i dati salvati su questo telefono? Dovrai rifare il login.")
                .setPositiveButton("Sì, Elimina") { _, _ ->

                    // 1. Dimentichiamo l'utente!
                    SettingsManager.clearUsername(this@SettingsActivity)
                    ApiClient.resetClient() // Pulisce anche la connessione residua

                    lifecycleScope.launch(Dispatchers.IO) {
                        // 2. Cancelliamo il database
                        database.clearAllTables()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SettingsActivity, "Dati eliminati dal dispositivo", Toast.LENGTH_LONG).show()

                            // 3. Torniamo al login (che ora, essendo vuoto, mostrerà di nuovo il campo Username!)
                            val intent = Intent(this@SettingsActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                    }
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }
}
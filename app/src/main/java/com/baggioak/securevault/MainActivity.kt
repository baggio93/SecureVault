package com.baggioak.securevault

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.spec.SecretKeySpec

class MainActivity : AppCompatActivity() {

    private var secretKey: SecretKeySpec? = null
    private lateinit var database: AppDatabase
    private val gson = Gson()

    private lateinit var layoutLogin: LinearLayout
    private lateinit var layoutDashboard: LinearLayout
    private lateinit var adapter: VaultAdapter

    private val settingsLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Riscarico i dati dal server...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                loadAndShowDashboard()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_main)

        // Schermo intero
        val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())

        database = AppDatabase.getDatabase(this)

        layoutLogin = findViewById(R.id.layoutLogin)
        layoutDashboard = findViewById(R.id.layoutDashboard)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvResult = findViewById<TextView>(R.id.tvResult)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val tvItemCount = findViewById<TextView>(R.id.tvItemCount) // Aggiungi questa!
        val rvVaultItems = findViewById<RecyclerView>(R.id.rvVaultItems)
        val btnAddFab = findViewById<Button>(R.id.btnAddFab)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        if (intent.getBooleanExtra("FORCE_LOGOUT", false)) {
            secretKey = null
            layoutLogin.visibility = View.VISIBLE
            layoutDashboard.visibility = View.GONE
            tvResult.text = ""
            Toast.makeText(this, "Cassaforte bloccata.", Toast.LENGTH_SHORT).show()
        }

        val savedUser = SettingsManager.getSavedUsername(this)
        val savedPass = SettingsManager.getSavedPassword(this)

        if (savedUser.isNotEmpty() && savedPass.isNotEmpty()) {
            etUsername.setText(savedUser)
            etUsername.visibility = View.GONE
            etPassword.visibility = View.GONE

            // La chiamata alla funzione biometrica è corretta qui
            showBiometricPrompt(savedPass, etPassword, btnLogin)

        } else if (savedUser.isNotEmpty()) {
            etUsername.setText(savedUser)
            etUsername.visibility = View.GONE
            etPassword.visibility = View.VISIBLE
            etPassword.requestFocus()
        } else {
            etUsername.visibility = View.VISIBLE
            etPassword.visibility = View.VISIBLE
            etUsername.setText("")
        }

        adapter = VaultAdapter(emptyList()) { clickedItem ->
            val jsonString = gson.toJson(clickedItem)
            val intent = Intent(this@MainActivity, DetailActivity::class.java)
            intent.putExtra("ITEM_DATA", jsonString)
            intent.putExtra("SECRET_KEY", secretKey!!.encoded)
            startActivity(intent)
        }

        adapter.onDataChanged = { count ->
            // Cambia il testo (se è 1 dice "elemento", altrimenti "elementi")
            tvItemCount.text = if (count == 1) "1 elemento trovato" else "$count elementi trovati"
        }
        rvVaultItems.layoutManager = LinearLayoutManager(this)
        rvVaultItems.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString())
            }
        })

        btnSettings.setOnClickListener {
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            settingsLauncher.launch(intent)
        }

        btnLogout.setOnClickListener {
            val intent = Intent(this@MainActivity, MainActivity::class.java)
            intent.putExtra("FORCE_LOGOUT", true)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            Toast.makeText(this@MainActivity, "Cassaforte bloccata manualmente", Toast.LENGTH_SHORT).show()
        }

        btnLogin.setOnClickListener {
            val user = etUsername.text.toString()
            val pass = etPassword.text.toString()

            if (user.isBlank() || pass.isBlank()) {
                tvResult.text = "Inserisci username e password"
                return@setOnClickListener
            }
            tvResult.text = "Accesso in corso..."
            tvResult.setTextColor(android.graphics.Color.parseColor("#f8fafc"))

            lifecycleScope.launch(Dispatchers.IO) {
                val mySalt = "SecureVaultSalt_$user"
                secretKey = CryptoManager.deriveKey(pass, mySalt)

                try {
                    val request = AuthRequest(user, pass)
                    val response = ApiClient.getApiService(this@MainActivity).login(request)

                    if (response.isSuccessful) {
                        if (response.body()?.success == true) {
                            syncWithServer()
                            loadAndShowDashboard()
                            SettingsManager.saveUsername(this@MainActivity, etUsername.text.toString().trim())
                            SettingsManager.savePassword(this@MainActivity, pass)
                        } else {
                            withContext(Dispatchers.Main) {
                                tvResult.setTextColor(android.graphics.Color.parseColor("#ef4444"))
                                tvResult.text = "Errore Login: ${response.body()?.error ?: "Credenziali errate"}"
                            }
                        }
                    } else {
                        throw Exception("Errore HTTP: ${response.code()}")
                    }
                } catch (e: Exception) {
                    val localItems = database.vaultDao().getAllItems()
                    if (localItems.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            tvResult.setTextColor(android.graphics.Color.parseColor("#ef4444"))
                            tvResult.text = "Offline e nessun dato salvato."
                        }
                    } else {
                        try {
                            CryptoManager.decrypt(localItems[0].encrypted_data, localItems[0].iv, secretKey!!)
                            loadAndShowDashboard()
                            SettingsManager.saveUsername(this@MainActivity, etUsername.text.toString().trim())
                            SettingsManager.savePassword(this@MainActivity, pass)
                        } catch (cryptoError: Exception) {
                            withContext(Dispatchers.Main) {
                                tvResult.setTextColor(android.graphics.Color.parseColor("#ef4444"))
                                tvResult.text = "Server irraggiungibile e Password locale errata."
                            }
                        }
                    }
                }
            }
        }

        btnAddFab.setOnClickListener {
            val intent = Intent(this, EditActivity::class.java)
            intent.putExtra("SECRET_KEY", secretKey!!.encoded)
            startActivity(intent)
        }
    } // <-- FINE di onCreate

    // --- DA QUI IN GIÙ CI SONO TUTTE LE FUNZIONI DELLA CLASSE ---

    // La funzione biometrica ORA E' FUORI dall'onCreate!
    private fun showBiometricPrompt(savedPass: String, etPassword: EditText, btnLogin: Button) {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(this)

        val biometricPrompt = androidx.biometric.BiometricPrompt(this, executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    etPassword.setText(savedPass)
                    btnLogin.performClick()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    etPassword.visibility = View.VISIBLE
                    Toast.makeText(applicationContext, "Usa la password per accedere", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Sblocca Cassaforte")
            .setSubtitle("Usa l'impronta digitale per entrare")
            .setNegativeButtonText("Usa Password")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onResume() {
        super.onResume()
        if (secretKey != null && layoutDashboard.visibility == View.VISIBLE) {
            refreshLocalList()
        }
    }

    private suspend fun loadAndShowDashboard() {
        try {
            val response = ApiClient.getApiService(this@MainActivity).getItems()
            if (response.isSuccessful) {
                val serverItems = response.body() ?: emptyList()
                val localUnsyncedIds = database.vaultDao().getUnsyncedItems().map { it.id }
                val entitiesToSave = serverItems
                    .filter { it.id !in localUnsyncedIds }
                    .map { VaultEntity(it.id, it.encrypted_data, it.iv, it.updated_at ?: "", "SYNCED") }

                database.vaultDao().insertAll(entitiesToSave)
            }
        } catch (e: Exception) {
            // Offline
        }

        // Questo riutilizzo logico è essenziale per evitare problemi grafici
        updateDashboardView()
    }

    private fun refreshLocalList() {
        lifecycleScope.launch(Dispatchers.IO) {
            syncWithServer()
            updateDashboardView()
        }
    }

    private suspend fun updateDashboardView() {
        val localItems = database.vaultDao().getAllItems()
        val decryptedList = mutableListOf<DecryptedVaultItem>()

        // 1. INIZIALIZZIAMO UN CONTATORE DI ERRORI
        var erroriDecrittografia = 0
        var primoErroreLog = "" // <--- AGGIUNGI QUESTA VARIABILE

        for (item in localItems) {
            if (item.sync_status == "DELETED") continue
            try {
                val decryptedJsonStr = CryptoManager.decrypt(item.encrypted_data, item.iv, secretKey!!)
                val decItem = gson.fromJson(decryptedJsonStr, DecryptedVaultItem::class.java)
                val itemWithRealId = decItem.copy(id = item.id)
                decryptedList.add(itemWithRealId)
            } catch (e: Exception) {
                // 2. SE FALLISCE LA DECRITTAZIONE, AUMENTIAMO IL CONTATORE!
                erroriDecrittografia++
                // SALVIAMO IL MESSAGGIO DEL PRIMO ERRORE CHE INCONTRA
                if (primoErroreLog.isEmpty()) {
                    primoErroreLog = e.message ?: e.toString()
                }
            }
        }

        withContext(Dispatchers.Main) {
            layoutLogin.visibility = View.GONE
            layoutDashboard.visibility = View.VISIBLE
            adapter.updateData(decryptedList)

            // 3. MOSTRIAMO LA VERITÀ A SCHERMO
            //android.widget.Toast.makeText(this@MainActivity, "Trovati nel DB: ${localItems.size} | Mostrati a schermo: ${decryptedList.size}", android.widget.Toast.LENGTH_LONG).show()

            if (erroriDecrittografia > 0) {
                android.widget.Toast.makeText(this@MainActivity, "ATTENZIONE: $erroriDecrittografia elementi bloccati da una vecchia chiave!", android.widget.Toast.LENGTH_LONG).show()
                android.widget.Toast.makeText(this@MainActivity, "ERRORE: $primoErroreLog", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showIpSettingsDialog() {
        val input = EditText(this)
        input.setText(SettingsManager.getServerIp(this))
        AlertDialog.Builder(this)
            .setTitle("Impostazioni Server")
            .setView(input)
            .setPositiveButton("Salva") { _, _ -> SettingsManager.setServerIp(this, input.text.toString()) }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private suspend fun syncWithServer() {
        val unsyncedItems = database.vaultDao().getUnsyncedItems()
        if (unsyncedItems.isEmpty()) return

        withContext(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "Sincronizzazione...", Toast.LENGTH_SHORT).show()
        }

        val api = ApiClient.getApiService(this)

        for (item in unsyncedItems) {
            try {
                when (item.sync_status) {
                    "NEW" -> {
                        val req = CreateItemRequest(item.encrypted_data, item.iv)
                        val res = api.createItem(req)
                        if (res.isSuccessful && res.body()?.success == true) {
                            res.body()?.id?.let { serverId ->
                                database.vaultDao().updateIdAndSync(item.id, serverId)
                            }
                        }
                    }
                    "MODIFIED" -> {
                        val req = UpdateItemRequest(item.id, item.encrypted_data, item.iv)
                        val res = api.updateItem(req)
                        if (res.isSuccessful && res.body()?.success == true) {
                            database.vaultDao().markAsSynced(item.id)
                        }
                    }
                    "DELETED" -> {
                        val req = DeleteItemRequest(item.id)
                        val res = api.deleteItem(req)
                        if (res.isSuccessful || res.code() == 404) {
                            database.vaultDao().hardDelete(item.id)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore errors silently for next sync
            }
        }
    }
}
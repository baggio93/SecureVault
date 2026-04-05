package com.baggioak.securevault

import android.app.Application
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class SecureVaultApp : Application(), DefaultLifecycleObserver {

    private var backgroundTime: Long = 0

    // Per ora impostiamo il timeout a 1 minuto (60.000 millisecondi)
    // Cambialo a 10 * 1000 (10 secondi) per fare un test rapido!
    private val TIMEOUT_MS: Long = 10 * 1000

    override fun onCreate() {
        // CORREZIONE: Diciamo esplicitamente a Kotlin di usare l'onCreate di Application
        super<Application>.onCreate()

        // Diciamo al guardiano globale di avvisare questa classe quando l'app va in background/foreground
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        // Rimosso il 'super' perché qui non serve
        // L'UTENTE HA ABBASSATO L'APP (Background)
        backgroundTime = System.currentTimeMillis()
        android.util.Log.d("SECURITY", "App in background. Timer avviato.")
    }

    override fun onStart(owner: LifecycleOwner) {
        // Rimosso il 'super' perché qui non serve
        // L'APP TORNA IN PRIMO PIANO
        if (backgroundTime > 0) {
            val timeAway = System.currentTimeMillis() - backgroundTime

            if (timeAway > TIMEOUT_MS) {
                android.util.Log.d("SECURITY", "Tempo scaduto! Blocco l'app.")
                forceLock()
            } else {
                android.util.Log.d("SECURITY", "Bentornato. Sei stato via solo per ${timeAway / 1000} secondi.")
            }
        }
    }

    private fun forceLock() {
        android.util.Log.d("SECURITY", "Eseguo il logout forzato!")

        val intent = Intent(this, MainActivity::class.java)

        // AGGIUNGI QUESTA RIGA: il bigliettino che dice di forzare il logout
        intent.putExtra("FORCE_LOGOUT", true)

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
package com.baggioak.securevault

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "SecureVaultPrefs"
    private const val KEY_SERVER_IP = "server_ip"

    // IP di default se l'utente non lo ha ancora inserito
    private const val DEFAULT_IP = "192.168.1.11"

    fun saveUsername(context: Context, username: String) {
        val prefs = context.getSharedPreferences("secure_vault_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("saved_username", username).apply()
    }

    fun getSavedUsername(context: Context): String {
        val prefs = context.getSharedPreferences("secure_vault_prefs", Context.MODE_PRIVATE)
        return prefs.getString("saved_username", "") ?: ""
    }

    fun clearUsername(context: Context) {
        val prefs = context.getSharedPreferences("secure_vault_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("saved_username").apply()
    }
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Salva il nuovo IP
    fun setServerIp(context: Context, ip: String) {
        getPrefs(context).edit().putString(KEY_SERVER_IP, ip).apply()
    }

    // Recupera l'IP salvato
    fun getServerIp(context: Context): String {
        return getPrefs(context).getString(KEY_SERVER_IP, DEFAULT_IP) ?: DEFAULT_IP
    }

    // Costruisce l'URL base corretto per Retrofit
    fun getBaseUrl(context: Context): String {
        val ip = getServerIp(context)
        return "https://$ip/pw-manager/"
    }

    // Salva la password
    fun savePassword(context: Context, password: String) {
        val prefs = context.getSharedPreferences("secure_vault_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("saved_password", password).apply()
    }

    // Legge la password salvata
    fun getSavedPassword(context: Context): String {
        val prefs = context.getSharedPreferences("secure_vault_prefs", Context.MODE_PRIVATE)
        return prefs.getString("saved_password", "") ?: ""
    }

    // Aggiungi questo dentro la tua funzione clearUsername (o creane una clearAll)
    fun clearCredentials(context: Context) {
        val prefs = context.getSharedPreferences("secure_vault_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("saved_username").remove("saved_password").apply()
    }
}
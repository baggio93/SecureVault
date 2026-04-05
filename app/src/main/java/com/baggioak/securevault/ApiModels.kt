package com.baggioak.securevault

// Oggetto da inviare al server per il login
data class AuthRequest(
    val username: String,
    val password: String
)

// Risposta del server al login (es. {"success": true} o {"error": "..."})
data class AuthResponse(
    val success: Boolean?,
    val error: String?
)

// Il singolo elemento Password (risposta di api.php?action=items)
data class VaultItem(
    val id: Int,
    val encrypted_data: String,
    val iv: String,
    val updated_at: String
)

// --- MODELLI PER I DATI DECRIPTATI ---

data class DecryptedVaultItem(
    val id: Int,
    val platform: String? = "",
    val username: String? = "",
    val password: String? = "",
    val passwordHistory: List<PasswordHistoryItem>? = emptyList(),
    val pin: String? = "",
    val pinHistory: List<PinHistoryItem>? = emptyList(),
    val url: String? = "",
    val notes: String? = "",
    val tags: String? = "",
    val created_at: String? = "",
    val updated_at: String? = ""
)

data class PasswordHistoryItem(
    val password: String,
    val changed_at: String
)

data class PinHistoryItem(
    val pin: String,
    val changed_at: String
)


// --- MODELLI PER IL SYNC ---
data class CreateItemRequest(val encrypted_data: String, val iv: String)
data class CreateItemResponse(val success: Boolean, val id: Int?, val error: String?)

data class UpdateItemRequest(val id: Int, val encrypted_data: String, val iv: String)
data class DeleteItemRequest(val id: Int)
data class SimpleResponse(val success: Boolean, val error: String?)
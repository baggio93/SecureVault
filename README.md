# 🔐 Secure Vault - Android Password Manager

Un'applicazione Android nativa e sicura per la gestione delle password, progettata con un'interfaccia moderna in stile "Slate Dark Mode" e difese anti-spionaggio integrate.

Questo repository contiene l'applicazione client Android. Il backend/server associato a questo progetto si trova qui: **[pw-manager (Server)](https://github.com/baggio93/pw-manager)**.

## ✨ Funzionalità Principali

* **🛡️ Sicurezza Avanzata (FLAG_SECURE):** Il sistema blocca nativamente gli screenshot, la registrazione dello schermo e oscura l'anteprima dell'app nel multitasking di Android per prevenire occhiate indiscrete.
* **🔒 Crittografia Dati:** Gestione sicura delle password crittografate provenienti dal server (tramite libreria Gson e algoritmi di decrittazione).
* **🏷️ Sistema di Tagging:** Categorizzazione delle password tramite tag personalizzati a forma di "pillola" (es. Lavoro, Finanza, Social).
* **🔍 Ricerca Dinamica e Intelligente:** Barra di ricerca in tempo reale che filtra per Piattaforma, Username o Categoria, con un contatore dinamico dei risultati aggiornato istantaneamente.
* **📜 Storico Password:** Tracciamento automatico dei cambi password e PIN, con salvataggio delle vecchie credenziali e data di modifica.
* **🎨 Design Professionale:** Interfaccia utente bloccata in modalità scura riposante (palette Slate), con bottoni borderless compatti, campi di testo eleganti e layout ottimizzati che prevengono la sovrapposizione della tastiera virtuale (`adjustPan`/`adjustResize`).
* **🛡️ Tolleranza agli Errori (Null Safety):** Architettura dati robusta scritta in Kotlin per gestire senza crash l'assenza di dati dai vecchi salvataggi del database.

## 🛠️ Tecnologie Utilizzate

* **Linguaggio:** Kotlin
* **Architettura UI:** XML (Material Design Components & CardView)
* **Parsing Dati:** libreria `Gson` per la serializzazione/deserializzazione JSON
* **Componenti Chiave:** `RecyclerView` (con `Adapter` personalizzato), `ScrollView`, `Intent` per passaggio dati complessi.

## 🚀 Come avviare il progetto

1. Clona questo repository:
   ```bash
   git clone [https://github.com/baggio93/pw-manager-android.git](https://github.com/baggio93/pw-manager-android.git)

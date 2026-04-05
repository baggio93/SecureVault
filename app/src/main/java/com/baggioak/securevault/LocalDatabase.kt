package com.baggioak.securevault

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

// 1. LA TABELLA DEL DATABASE (Identica a quella del server)
@Entity(tableName = "vault_items")
data class VaultEntity(
    @PrimaryKey val id: Int, // Se creata offline, le daremo un ID negativo (es. -1) finché il server non le dà quello vero
    val encrypted_data: String,
    val iv: String,
    val updated_at: String,
    val sync_status: String = "SYNCED" // Può essere: "SYNCED", "NEW", "MODIFIED"
)

// 2. LE AZIONI CHE POSSIAMO FARE SUL DATABASE (DAO)
@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items")
    suspend fun getAllItems(): List<VaultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Questo evita i doppioni con lo stesso ID!
    suspend fun insertAll(items: List<VaultEntity>)

    @Query("DELETE FROM vault_items")
    suspend fun deleteAll()

    // Imposta lo stato su DELETED per segnalare al server di eliminarlo
    @Query("UPDATE vault_items SET sync_status = 'DELETED' WHERE id = :itemId")
    suspend fun markAsDeleted(itemId: Int)

    // --- NUOVE FUNZIONI PER IL SYNC ---

    // Trova tutti gli elementi che devono ancora essere inviati al server
    @Query("SELECT * FROM vault_items WHERE sync_status != 'SYNCED'")
    suspend fun getUnsyncedItems(): List<VaultEntity>

    // Cancella fisicamente un elemento dal telefono (usato dopo il DELETE col server)
    @Query("DELETE FROM vault_items WHERE id = :itemId")
    suspend fun hardDelete(itemId: Int)

    // Conferma che la modifica è arrivata al server
    @Query("UPDATE vault_items SET sync_status = 'SYNCED' WHERE id = :itemId")
    suspend fun markAsSynced(itemId: Int)

    // Sostituisce l'ID negativo temporaneo con quello vero dato dal server
    @Query("UPDATE vault_items SET id = :newId, sync_status = 'SYNCED' WHERE id = :oldId")
    suspend fun updateIdAndSync(oldId: Int, newId: Int)

    @Query("DELETE FROM vault_items WHERE sync_status = 'SYNCED'")
    suspend fun clearSyncedItems()

}

// 3. IL MOTORE DEL DATABASE
@Database(entities = [VaultEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "secure_vault_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

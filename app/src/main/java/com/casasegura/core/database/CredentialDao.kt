package com.casasegura.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CredentialDao {

    @Query("SELECT * FROM credentials ORDER BY alias COLLATE NOCASE")
    suspend fun listCredentials(): List<CredentialEntity>

    @Query("SELECT * FROM credentials WHERE id = :id")
    suspend fun findCredential(id: String): CredentialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCredential(entity: CredentialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSecret(entity: CredentialSecretEntity)

    @Query("SELECT * FROM credential_secrets WHERE credentialId = :id")
    suspend fun findSecret(id: String): CredentialSecretEntity?

    @Query("DELETE FROM credentials WHERE id = :id")
    suspend fun deleteCredential(id: String)

    @Query("DELETE FROM credential_secrets WHERE credentialId = :id")
    suspend fun deleteSecret(id: String)

    /** Remove metadados e senha juntos, para não deixar segredo órfão. */
    @Transaction
    suspend fun deleteAll(id: String) {
        deleteSecret(id)
        deleteCredential(id)
    }
}

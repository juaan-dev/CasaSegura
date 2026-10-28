package com.casasegura.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Metadados da credencial. Nunca contém a senha. */
@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey val id: String,
    val alias: String,
    val username: String,
    val scope: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

/** Senha cifrada pelo SecretCipher (Android Keystore). Tabela separada dos metadados. */
@Entity(tableName = "credential_secrets")
class CredentialSecretEntity(
    @PrimaryKey val credentialId: String,
    val encryptedPassword: ByteArray,
)

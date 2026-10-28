package com.casasegura.core.database

import com.casasegura.core.credentials.CredentialStore
import com.casasegura.core.model.Credential
import com.casasegura.core.security.SecretCipher

/** Implementação de [CredentialStore] com Room para metadados e [SecretCipher] para senhas. */
class RoomCredentialStore(
    private val dao: CredentialDao,
    private val cipher: SecretCipher,
) : CredentialStore {

    override suspend fun list(): List<Credential> = dao.listCredentials().map { it.toDomain() }

    override suspend fun get(id: String): Credential? = dao.findCredential(id)?.toDomain()

    override suspend fun save(credential: Credential, password: String?) {
        dao.upsertCredential(credential.toEntity())
        if (password != null) {
            val encrypted = cipher.encrypt(password.toByteArray(Charsets.UTF_8))
            dao.upsertSecret(CredentialSecretEntity(credential.id, encrypted))
        }
    }

    override suspend fun delete(id: String) = dao.deleteAll(id)

    override suspend fun readPassword(id: String): String? {
        val secret = dao.findSecret(id) ?: return null
        return cipher.decrypt(secret.encryptedPassword).toString(Charsets.UTF_8)
    }

    private fun CredentialEntity.toDomain() = Credential(
        id = id,
        alias = alias,
        username = username,
        scope = scope,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

    private fun Credential.toEntity() = CredentialEntity(
        id = id,
        alias = alias,
        username = username,
        scope = scope,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}

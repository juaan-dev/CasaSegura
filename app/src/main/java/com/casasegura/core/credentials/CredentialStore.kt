package com.casasegura.core.credentials

import com.casasegura.core.model.Credential

/**
 * Persistência de credenciais. Os metadados e a senha cifrada ficam separados;
 * a senha em texto claro só sai por [readPassword], nunca por [list] ou [get].
 */
interface CredentialStore {
    suspend fun list(): List<Credential>

    suspend fun get(id: String): Credential?

    /** Grava metadados e, se [password] não for nulo, substitui a senha cifrada. */
    suspend fun save(credential: Credential, password: String?)

    /** Remove metadados e senha cifrada. */
    suspend fun delete(id: String)

    suspend fun readPassword(id: String): String?
}

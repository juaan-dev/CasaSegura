package com.casasegura.core.credentials

import com.casasegura.core.model.Credential
import java.util.UUID

/**
 * Regras de credenciais reutilizáveis (seção 2.3 do plano).
 * Não conhece Android: recebe o relógio e o gerador de id por injeção, para teste no JVM.
 */
class CredentialService(
    private val store: CredentialStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {

    sealed interface Result {
        data class Saved(val credential: Credential) : Result
        data class Invalid(val message: String) : Result
        data class NotFound(val id: String) : Result
    }

    sealed interface DeleteResult {
        data object Deleted : DeleteResult
        data class InUse(val cameraIds: List<String>) : DeleteResult
        data class NotFound(val id: String) : DeleteResult
    }

    suspend fun list(): List<Credential> = store.list()

    suspend fun create(alias: String, username: String, password: String, scope: String = ""): Result {
        validate(alias, username, password)?.let { return Result.Invalid(it) }
        val now = clock()
        val credential = Credential(
            id = idGenerator(),
            alias = alias.trim(),
            username = username.trim(),
            scope = scope.trim(),
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        store.save(credential, password)
        return Result.Saved(credential)
    }

    /** Senha nula mantém a senha já salva. */
    suspend fun update(id: String, alias: String, username: String, password: String?, scope: String = ""): Result {
        val existing = store.get(id) ?: return Result.NotFound(id)
        validate(alias, username, password)?.let { return Result.Invalid(it) }
        val updated = existing.copy(
            alias = alias.trim(),
            username = username.trim(),
            scope = scope.trim(),
            updatedAtEpochMillis = clock(),
        )
        store.save(updated, password)
        return Result.Saved(updated)
    }

    /**
     * Exclusão exige que nenhuma câmera use a credencial, ou que o usuário confirme
     * explicitamente depois de ver a lista de câmeras afetadas.
     */
    suspend fun delete(id: String, camerasUsingCredential: List<String>, confirmed: Boolean): DeleteResult {
        if (store.get(id) == null) return DeleteResult.NotFound(id)
        if (camerasUsingCredential.isNotEmpty() && !confirmed) {
            return DeleteResult.InUse(camerasUsingCredential)
        }
        store.delete(id)
        return DeleteResult.Deleted
    }

    /** Usado pelo adaptador de protocolo no momento da conexão. Não deve ir para a UI. */
    suspend fun passwordForConnection(id: String): String? = store.readPassword(id)

    /** Senha nula significa "manter a atual" e não é validada. */
    private fun validate(alias: String, username: String, password: String?): String? = when {
        alias.isBlank() -> "Informe um nome para a credencial"
        username.isBlank() -> "Informe o usuário"
        password != null && password.isEmpty() -> "Informe a senha"
        else -> null
    }
}

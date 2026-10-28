package com.casasegura.core.credentials

import com.casasegura.core.model.Credential
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialServiceTest {

    private class InMemoryStore : CredentialStore {
        val metadata = linkedMapOf<String, Credential>()
        val secrets = mutableMapOf<String, String>()

        override suspend fun list() = metadata.values.toList()
        override suspend fun get(id: String) = metadata[id]
        override suspend fun save(credential: Credential, password: String?) {
            metadata[credential.id] = credential
            if (password != null) secrets[credential.id] = password
        }
        override suspend fun delete(id: String) {
            metadata.remove(id)
            secrets.remove(id)
        }
        override suspend fun readPassword(id: String) = secrets[id]
    }

    private fun service(store: InMemoryStore = InMemoryStore()): Pair<CredentialService, InMemoryStore> {
        var ids = 0
        val svc = CredentialService(
            store = store,
            clock = { 1_000L },
            idGenerator = { "cred-${++ids}" },
        )
        return svc to store
    }

    @Test
    fun `cria credencial sem expor a senha nos metadados`() = runBlocking {
        val (svc, store) = service()
        val result = svc.create(alias = " Casa ", username = "admin", password = "segredo")

        val saved = (result as CredentialService.Result.Saved).credential
        assertEquals("Casa", saved.alias)
        assertEquals("admin", saved.username)
        assertFalse(saved.toString().contains("segredo"))
        assertEquals("segredo", store.readPassword(saved.id))
    }

    @Test
    fun `rejeita campos obrigatorios vazios`() = runBlocking {
        val (svc, _) = service()
        assertTrue(svc.create(alias = " ", username = "u", password = "p") is CredentialService.Result.Invalid)
        assertTrue(svc.create(alias = "a", username = "", password = "p") is CredentialService.Result.Invalid)
        assertTrue(svc.create(alias = "a", username = "u", password = "") is CredentialService.Result.Invalid)
    }

    @Test
    fun `edicao sem senha mantem a senha atual`() = runBlocking {
        val (svc, store) = service()
        val id = (svc.create("Casa", "admin", "antiga") as CredentialService.Result.Saved).credential.id

        svc.update(id, alias = "Casa nova", username = "admin2", password = null)

        assertEquals("Casa nova", store.get(id)?.alias)
        assertEquals("admin2", store.get(id)?.username)
        assertEquals("antiga", store.readPassword(id))
    }

    @Test
    fun `edicao com senha substitui a senha`() = runBlocking {
        val (svc, store) = service()
        val id = (svc.create("Casa", "admin", "antiga") as CredentialService.Result.Saved).credential.id

        svc.update(id, alias = "Casa", username = "admin", password = "nova")

        assertEquals("nova", store.readPassword(id))
    }

    @Test
    fun `exclusao de credencial em uso exige confirmacao`() = runBlocking {
        val (svc, store) = service()
        val id = (svc.create("Casa", "admin", "p") as CredentialService.Result.Saved).credential.id

        val blocked = svc.delete(id, camerasUsingCredential = listOf("cam-1", "cam-2"), confirmed = false)
        assertEquals(CredentialService.DeleteResult.InUse(listOf("cam-1", "cam-2")), blocked)
        assertTrue(store.get(id) != null)

        val confirmed = svc.delete(id, camerasUsingCredential = listOf("cam-1", "cam-2"), confirmed = true)
        assertEquals(CredentialService.DeleteResult.Deleted, confirmed)
        assertNull(store.get(id))
        assertNull(store.readPassword(id))
    }

    @Test
    fun `exclusao de credencial sem uso nao pede confirmacao`() = runBlocking {
        val (svc, _) = service()
        val id = (svc.create("Casa", "admin", "p") as CredentialService.Result.Saved).credential.id

        assertEquals(
            CredentialService.DeleteResult.Deleted,
            svc.delete(id, camerasUsingCredential = emptyList(), confirmed = false),
        )
    }

    @Test
    fun `operacoes em credencial inexistente retornam NotFound`() = runBlocking {
        val (svc, _) = service()
        assertEquals(
            CredentialService.Result.NotFound("x"),
            svc.update("x", alias = "a", username = "u", password = null),
        )
        assertEquals(
            CredentialService.DeleteResult.NotFound("x"),
            svc.delete("x", emptyList(), confirmed = true),
        )
    }
}

package com.casasegura.core.layouts

import com.casasegura.core.model.Layout
import com.casasegura.core.model.StreamChoice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutServiceTest {

    private class InMemoryLayouts : LayoutRepository {
        val items = linkedMapOf<String, Layout>()
        override suspend fun list() = items.values.toList()
        override suspend fun get(id: String) = items[id]
        override suspend fun save(layout: Layout) { items[layout.id] = layout }
        override suspend fun delete(id: String) { items.remove(id) }
    }

    private fun service(): Pair<LayoutService, InMemoryLayouts> {
        val repo = InMemoryLayouts()
        var n = 0
        return LayoutService(repo, idGenerator = { "l-${++n}" }) to repo
    }

    @Test
    fun `cria matriz assimetrica com slots vazios dentro do limite`() = runBlocking {
        val (svc, repo) = service()
        val result = svc.create("Entrada", rows = 2, columns = 3, maxCells = 9) as LayoutService.Result.Saved

        assertEquals(6, result.layout.slots.size)
        assertEquals(6, repo.get("l-1")!!.slots.size)
    }

    @Test
    fun `recusa visualizacao acima da capacidade do perfil`() = runBlocking {
        val (svc, _) = service()
        val result = svc.create("Grande", rows = 5, columns = 5, maxCells = 16)
        assertTrue(result is LayoutService.Result.Invalid)
    }

    @Test
    fun `recusa nome vazio`() = runBlocking {
        val (svc, _) = service()
        assertTrue(svc.create("  ", 1, 1, 4) is LayoutService.Result.Invalid)
    }

    @Test
    fun `atribui camera a um slot sem alterar os demais`() = runBlocking {
        val (svc, repo) = service()
        svc.create("Casa", 1, 2, 4)

        svc.assign("l-1", slotIndex = 1, cameraId = "cam-sala", stream = StreamChoice.SUB)

        val slots = repo.get("l-1")!!.slots
        assertEquals(null, slots[0].cameraId)
        assertEquals("cam-sala", slots[1].cameraId)
        assertEquals(StreamChoice.SUB, slots[1].stream)
    }

    @Test
    fun `slot fora da matriz e recusado`() = runBlocking {
        val (svc, _) = service()
        svc.create("Casa", 1, 2, 4)
        assertTrue(svc.assign("l-1", slotIndex = 5, cameraId = "x") is LayoutService.Result.Invalid)
    }

    @Test
    fun `so uma visualizacao pode ser padrao por vez`() = runBlocking {
        val (svc, repo) = service()
        svc.create("A", 1, 1, 4)
        svc.create("B", 1, 1, 4)

        svc.setDefault("l-1")
        svc.setDefault("l-2")

        assertEquals(false, repo.get("l-1")!!.isDefault)
        assertEquals(true, repo.get("l-2")!!.isDefault)
    }

    @Test
    fun `favorita e separada da padrao`() = runBlocking {
        val (svc, repo) = service()
        svc.create("A", 1, 1, 4)

        svc.toggleFavorite("l-1")

        assertEquals(true, repo.get("l-1")!!.favorite)
        assertEquals(false, repo.get("l-1")!!.isDefault)
    }

    @Test
    fun `exclusao remove a visualizacao e informa o resultado`() = runBlocking {
        val (svc, repo) = service()
        svc.create("A", 1, 1, 4)

        assertEquals(LayoutService.Result.Deleted("l-1"), svc.delete("l-1"))
        assertEquals(null, repo.get("l-1"))
        assertEquals(LayoutService.Result.NotFound("l-1"), svc.delete("l-1"))
    }
}

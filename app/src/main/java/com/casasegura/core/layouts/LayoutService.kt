package com.casasegura.core.layouts

import com.casasegura.core.model.Layout
import com.casasegura.core.model.LayoutRules
import com.casasegura.core.model.LayoutSlot
import com.casasegura.core.model.StreamChoice
import java.util.UUID

interface LayoutRepository {
    suspend fun list(): List<Layout>
    suspend fun get(id: String): Layout?
    suspend fun save(layout: Layout)
    suspend fun delete(id: String)
}

/**
 * Regras de visualizações (seção 2.4 e 2.5 do plano). A capacidade máxima vem de quem chama,
 * porque depende do dispositivo (TV, tablet ou celular).
 */
class LayoutService(
    private val repository: LayoutRepository,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {

    sealed interface Result {
        data class Saved(val layout: Layout) : Result
        data class Invalid(val message: String) : Result
        data class NotFound(val id: String) : Result
        data class Deleted(val id: String) : Result
    }

    suspend fun list(): List<Layout> = repository.list()

    suspend fun create(name: String, rows: Int, columns: Int, maxCells: Int): Result {
        if (name.isBlank()) return Result.Invalid("Informe um nome para a visualização")
        when (val size = LayoutRules.validateSize(rows, columns, maxCells)) {
            LayoutRules.Result.Valid -> Unit
            is LayoutRules.Result.TooLarge -> return Result.Invalid(
                "Esta visualização tem ${size.cells} células; o limite deste dispositivo é ${size.maxCells}",
            )
            is LayoutRules.Result.Empty -> return Result.Invalid(size.reason)
        }
        val layout = Layout(
            id = idGenerator(),
            name = name.trim(),
            rows = rows,
            columns = columns,
            slots = LayoutRules.emptySlots(rows, columns),
        )
        repository.save(layout)
        return Result.Saved(layout)
    }

    /** Atribui uma câmera a um slot. [cameraId] nulo deixa o slot vazio. */
    suspend fun assign(layoutId: String, slotIndex: Int, cameraId: String?, stream: StreamChoice = StreamChoice.AUTO): Result {
        val layout = repository.get(layoutId) ?: return Result.NotFound(layoutId)
        if (slotIndex !in layout.slots.indices) return Result.Invalid("Slot $slotIndex não existe nesta visualização")
        val updated = layout.copy(
            slots = layout.slots.map { if (it.index == slotIndex) LayoutSlot(slotIndex, cameraId, stream) else it },
        )
        repository.save(updated)
        return Result.Saved(updated)
    }

    /** Define a padrão e desmarca as demais. */
    suspend fun setDefault(layoutId: String): Result {
        val target = repository.get(layoutId) ?: return Result.NotFound(layoutId)
        repository.list().filter { it.isDefault && it.id != layoutId }.forEach {
            repository.save(it.copy(isDefault = false))
        }
        val updated = target.copy(isDefault = true)
        repository.save(updated)
        return Result.Saved(updated)
    }

    suspend fun toggleFavorite(layoutId: String): Result {
        val layout = repository.get(layoutId) ?: return Result.NotFound(layoutId)
        val updated = layout.copy(favorite = !layout.favorite)
        repository.save(updated)
        return Result.Saved(updated)
    }

    suspend fun delete(layoutId: String): Result {
        if (repository.get(layoutId) == null) return Result.NotFound(layoutId)
        repository.delete(layoutId)
        return Result.Deleted(layoutId)
    }
}

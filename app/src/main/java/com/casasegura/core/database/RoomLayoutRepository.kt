package com.casasegura.core.database

import com.casasegura.core.layouts.LayoutRepository
import com.casasegura.core.model.Layout
import com.casasegura.core.model.LayoutSlot
import com.casasegura.core.model.StreamChoice

class RoomLayoutRepository(private val dao: LayoutDao) : LayoutRepository {

    override suspend fun list(): List<Layout> = dao.listLayouts().map { it.toDomain() }

    override suspend fun get(id: String): Layout? = dao.findLayout(id)?.toDomain()

    override suspend fun save(layout: Layout) = dao.replace(
        LayoutEntity(
            id = layout.id,
            name = layout.name,
            rows = layout.rows,
            columns = layout.columns,
            favorite = layout.favorite,
            isDefault = layout.isDefault,
        ),
        layout.slots.map { LayoutSlotEntity(layout.id, it.index, it.cameraId, it.stream.name) },
    )

    override suspend fun delete(id: String) = dao.deleteWithSlots(id)

    private suspend fun LayoutEntity.toDomain(): Layout = Layout(
        id = id,
        name = name,
        rows = rows,
        columns = columns,
        slots = dao.slotsOf(id).map {
            LayoutSlot(index = it.slotIndex, cameraId = it.cameraId, stream = StreamChoice.valueOf(it.stream))
        },
        favorite = favorite,
        isDefault = isDefault,
    )
}

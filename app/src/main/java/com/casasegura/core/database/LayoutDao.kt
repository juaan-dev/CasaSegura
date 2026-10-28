package com.casasegura.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LayoutDao {

    @Query("SELECT * FROM layouts ORDER BY name COLLATE NOCASE")
    suspend fun listLayouts(): List<LayoutEntity>

    @Query("SELECT * FROM layouts WHERE id = :id")
    suspend fun findLayout(id: String): LayoutEntity?

    @Query("SELECT * FROM layout_slots WHERE layoutId = :layoutId ORDER BY slotIndex")
    suspend fun slotsOf(layoutId: String): List<LayoutSlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLayout(entity: LayoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSlots(slots: List<LayoutSlotEntity>)

    @Query("DELETE FROM layout_slots WHERE layoutId = :layoutId")
    suspend fun deleteSlots(layoutId: String)

    @Query("DELETE FROM layouts WHERE id = :id")
    suspend fun deleteLayout(id: String)

    /** Regrava a visualização e seus slots juntos, para não deixar slots órfãos. */
    @Transaction
    suspend fun replace(layout: LayoutEntity, slots: List<LayoutSlotEntity>) {
        upsertLayout(layout)
        deleteSlots(layout.id)
        upsertSlots(slots)
    }

    @Transaction
    suspend fun deleteWithSlots(id: String) {
        deleteSlots(id)
        deleteLayout(id)
    }
}

package com.casasegura.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "layouts")
data class LayoutEntity(
    @PrimaryKey val id: String,
    val name: String,
    val rows: Int,
    val columns: Int,
    val favorite: Boolean,
    val isDefault: Boolean,
)

@Entity(tableName = "layout_slots", primaryKeys = ["layoutId", "slotIndex"])
data class LayoutSlotEntity(
    val layoutId: String,
    val slotIndex: Int,
    val cameraId: String?,
    val stream: String,
)

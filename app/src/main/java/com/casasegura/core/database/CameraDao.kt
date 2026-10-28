package com.casasegura.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CameraDao {

    @Query("SELECT * FROM cameras ORDER BY name COLLATE NOCASE")
    suspend fun listCameras(): List<CameraEntity>

    @Query("SELECT * FROM cameras WHERE id = :id")
    suspend fun findCamera(id: String): CameraEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCamera(entity: CameraEntity)

    @Query("DELETE FROM cameras WHERE id = :id")
    suspend fun deleteCamera(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIpChange(entity: IpChangeEventEntity)

    @Query("SELECT * FROM ip_change_events WHERE cameraId = :cameraId ORDER BY atEpochMillis DESC")
    suspend fun ipChangesFor(cameraId: String): List<IpChangeEventEntity>
}

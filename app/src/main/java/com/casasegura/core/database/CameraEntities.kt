package com.casasegura.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cameras")
data class CameraEntity(
    @PrimaryKey val id: String,
    val name: String,
    val currentHost: String?,
    val credentialId: String?,
    val connection: String,
    val identityUuid: String?,
    val identitySerial: String?,
    val identityMac: String?,
    val identityModel: String?,
    val lastSeenEpochMillis: Long?,
    val location: String,
    val tags: String,
    val group: String,
    val notes: String,
)

@Entity(tableName = "ip_change_events")
data class IpChangeEventEntity(
    @PrimaryKey val id: String,
    val cameraId: String,
    val oldHost: String?,
    val newHost: String,
    val evidence: String,
    val atEpochMillis: Long,
)

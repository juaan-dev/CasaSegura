package com.casasegura.core.cameras

import com.casasegura.core.identity.IdentityEvidence
import com.casasegura.core.model.Camera

/** Mudança de endereço registrada no histórico local. Não contém segredos. */
data class IpChangeEvent(
    val id: String,
    val cameraId: String,
    val oldHost: String?,
    val newHost: String,
    val evidence: IdentityEvidence,
    val atEpochMillis: Long,
)

interface CameraRepository {
    suspend fun list(): List<Camera>

    suspend fun get(id: String): Camera?

    suspend fun save(camera: Camera)

    suspend fun delete(id: String)

    suspend fun recordIpChange(event: IpChangeEvent)

    suspend fun ipChangeHistory(cameraId: String): List<IpChangeEvent>
}

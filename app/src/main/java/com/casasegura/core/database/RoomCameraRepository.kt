package com.casasegura.core.database

import com.casasegura.core.cameras.CameraRepository
import com.casasegura.core.cameras.IpChangeEvent
import com.casasegura.core.identity.IdentityEvidence
import com.casasegura.core.model.Camera
import com.casasegura.core.model.CameraIdentity
import com.casasegura.core.model.ConnectionMethod

class RoomCameraRepository(private val dao: CameraDao) : CameraRepository {

    override suspend fun list(): List<Camera> = dao.listCameras().map { it.toDomain() }

    override suspend fun get(id: String): Camera? = dao.findCamera(id)?.toDomain()

    override suspend fun save(camera: Camera) = dao.upsertCamera(camera.toEntity())

    override suspend fun delete(id: String) = dao.deleteCamera(id)

    override suspend fun recordIpChange(event: IpChangeEvent) = dao.insertIpChange(
        IpChangeEventEntity(
            id = event.id,
            cameraId = event.cameraId,
            oldHost = event.oldHost,
            newHost = event.newHost,
            evidence = event.evidence.name,
            atEpochMillis = event.atEpochMillis,
        ),
    )

    override suspend fun ipChangeHistory(cameraId: String): List<IpChangeEvent> =
        dao.ipChangesFor(cameraId).map {
            IpChangeEvent(
                id = it.id,
                cameraId = it.cameraId,
                oldHost = it.oldHost,
                newHost = it.newHost,
                evidence = IdentityEvidence.valueOf(it.evidence),
                atEpochMillis = it.atEpochMillis,
            )
        }

    private fun CameraEntity.toDomain() = Camera(
        id = id,
        name = name,
        currentHost = currentHost,
        credentialId = credentialId,
        connection = ConnectionMethod.valueOf(connection),
        identity = CameraIdentity(identityUuid, identitySerial, identityMac, identityModel),
        lastSeenEpochMillis = lastSeenEpochMillis,
        location = location,
        tags = if (tags.isEmpty()) emptySet() else tags.split(SEPARATOR).toSet(),
        group = group,
        notes = notes,
    )

    private fun Camera.toEntity() = CameraEntity(
        id = id,
        name = name,
        currentHost = currentHost,
        credentialId = credentialId,
        connection = connection.name,
        identityUuid = identity.uuid,
        identitySerial = identity.serial,
        identityMac = identity.mac,
        identityModel = identity.model,
        lastSeenEpochMillis = lastSeenEpochMillis,
        location = location,
        tags = tags.joinToString(SEPARATOR),
        group = group,
        notes = notes,
    )

    private companion object {
        const val SEPARATOR = "\u001F"
    }
}

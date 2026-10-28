package com.casasegura.core.cameras

import com.casasegura.core.identity.IdentityEvidence
import com.casasegura.core.identity.Observation
import com.casasegura.core.model.Camera
import com.casasegura.core.model.CameraIdentity
import com.casasegura.core.model.ConnectionMethod
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IpChangeMonitorTest {

    private class InMemoryCameras : CameraRepository {
        val cameras = linkedMapOf<String, Camera>()
        val history = mutableListOf<IpChangeEvent>()
        override suspend fun list() = cameras.values.toList()
        override suspend fun get(id: String) = cameras[id]
        override suspend fun save(camera: Camera) { cameras[camera.id] = camera }
        override suspend fun delete(id: String) { cameras.remove(id) }
        override suspend fun recordIpChange(event: IpChangeEvent) { history += event }
        override suspend fun ipChangeHistory(cameraId: String) = history.filter { it.cameraId == cameraId }
    }

    private val uuid = "urn:uuid:sala-1"

    private fun setup(): Pair<IpChangeMonitor, InMemoryCameras> {
        val repo = InMemoryCameras()
        repo.cameras["sala"] = Camera(
            id = "sala",
            name = "Sala",
            currentHost = "192.168.0.10",
            credentialId = "cred-1",
            connection = ConnectionMethod.ONVIF,
            identity = CameraIdentity(uuid = uuid, model = "IPC-A"),
        )
        return IpChangeMonitor(repo, clock = { 42L }, idGenerator = { "evt-1" }) to repo
    }

    @Test
    fun `aplica troca de ip confirmada, preserva a identidade e registra o historico`() = runBlocking {
        val (monitor, repo) = setup()

        val report = monitor.scan(listOf(Observation("192.168.0.77", uuid = uuid, verifiedByProtocol = true)))

        assertEquals(1, report.applied.size)
        val camera = repo.get("sala")!!
        assertEquals("192.168.0.77", camera.currentHost)
        assertEquals("cred-1", camera.credentialId)
        assertEquals("Sala", camera.name)
        assertEquals(IdentityEvidence.UUID, report.applied.single().evidence)
        assertEquals(listOf("192.168.0.10"), repo.history.map { it.oldHost })
    }

    @Test
    fun `identidade nao confirmada vira pendencia sem trocar o ip`() = runBlocking {
        val (monitor, repo) = setup()

        val report = monitor.scan(listOf(Observation("192.168.0.77", uuid = uuid, verifiedByProtocol = false)))

        assertTrue(report.applied.isEmpty())
        assertEquals(1, report.pending.size)
        assertEquals("192.168.0.10", repo.get("sala")!!.currentHost)
    }

    @Test
    fun `camera ainda no mesmo ip apenas atualiza o horario visto`() = runBlocking {
        val (monitor, repo) = setup()

        val report = monitor.scan(listOf(Observation("192.168.0.10", uuid = uuid, verifiedByProtocol = true)))

        assertTrue(report.applied.isEmpty())
        assertTrue(report.pending.isEmpty())
        assertEquals(42L, repo.get("sala")!!.lastSeenEpochMillis)
        assertEquals("192.168.0.10", repo.get("sala")!!.currentHost)
    }

    @Test
    fun `dois enderecos respondendo como a mesma camera nao sao aplicados`() = runBlocking {
        val (monitor, repo) = setup()

        val report = monitor.scan(
            listOf(
                Observation("192.168.0.77", uuid = uuid, verifiedByProtocol = true),
                Observation("192.168.0.88", uuid = uuid, verifiedByProtocol = true),
            ),
        )

        assertTrue(report.applied.isEmpty())
        assertEquals("192.168.0.10", repo.get("sala")!!.currentHost)
        assertTrue(report.pending.any { it.candidateHosts.containsAll(listOf("192.168.0.77", "192.168.0.88")) })
    }

    @Test
    fun `sem observacoes nada muda`() = runBlocking {
        val (monitor, repo) = setup()
        val report = monitor.scan(emptyList())
        assertTrue(report.applied.isEmpty() && report.pending.isEmpty())
        assertEquals("192.168.0.10", repo.get("sala")!!.currentHost)
    }
}

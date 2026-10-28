package com.casasegura.core.identity

import com.casasegura.core.model.Camera
import com.casasegura.core.model.CameraIdentity
import com.casasegura.core.model.ConnectionMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityTest {

    private fun camera(id: String, host: String?, identity: CameraIdentity) = Camera(
        id = id,
        name = id,
        currentHost = host,
        credentialId = null,
        connection = ConnectionMethod.ONVIF,
        identity = identity,
    )

    private val salaUuid = "urn:uuid:aaaa-1111"
    private val sala = camera("sala", "192.168.0.10", CameraIdentity(uuid = salaUuid, mac = "AA:BB:CC:00:00:01", model = "IPC-A"))
    private val garagem = camera("garagem", "192.168.0.11", CameraIdentity(uuid = "urn:uuid:bbbb-2222", mac = "AA:BB:CC:00:00:02", model = "IPC-A"))

    // ---- IdentityResolver ----

    @Test
    fun `uuid identifica a camera mesmo com ip novo`() {
        val result = IdentityResolver.resolve(listOf(sala, garagem), Observation(host = "192.168.0.99", uuid = salaUuid))
        assertEquals(MatchResult.Matched("sala", IdentityEvidence.UUID), result)
    }

    @Test
    fun `mac e comparado sem diferenca de maiusculas`() {
        val result = IdentityResolver.resolve(listOf(sala), Observation(host = "10.0.0.5", mac = "aa:bb:cc:00:00:01"))
        assertEquals(MatchResult.Matched("sala", IdentityEvidence.MAC), result)
    }

    @Test
    fun `mesmo identificador em duas cameras gera ambiguidade`() {
        val a = camera("a", "10.0.0.1", CameraIdentity(serial = "SN-1"))
        val b = camera("b", "10.0.0.2", CameraIdentity(serial = "SN-1"))
        val result = IdentityResolver.resolve(listOf(a, b), Observation(host = "10.0.0.3", serial = "SN-1"))
        assertTrue(result is MatchResult.Ambiguous)
        assertEquals(listOf("a", "b"), (result as MatchResult.Ambiguous).candidateIds)
    }

    @Test
    fun `modelo igual sem identificador forte nunca associa sozinho`() {
        val result = IdentityResolver.resolve(listOf(sala, garagem), Observation(host = "10.0.0.9", model = "IPC-A"))
        assertTrue(result is MatchResult.Ambiguous)
    }

    @Test
    fun `dispositivo desconhecido nao casa com nada`() {
        assertEquals(
            MatchResult.NoMatch,
            IdentityResolver.resolve(listOf(sala), Observation(host = "10.0.0.9", uuid = "urn:uuid:zzz")),
        )
    }

    // ---- IpChangeEvaluator ----

    @Test
    fun `mesmo ip nao gera mudanca`() {
        val decision = IpChangeEvaluator.evaluate(sala, listOf(sala), Observation(host = "192.168.0.10", uuid = salaUuid, verifiedByProtocol = true))
        assertEquals(IpChangeDecision.Unchanged, decision)
    }

    @Test
    fun `ip novo com identidade confirmada atualiza automaticamente`() {
        val decision = IpChangeEvaluator.evaluate(
            sala,
            listOf(sala, garagem),
            Observation(host = "192.168.0.99", uuid = salaUuid, verifiedByProtocol = true),
        )
        assertEquals(
            IpChangeDecision.AutoUpdate("sala", oldHost = "192.168.0.10", newHost = "192.168.0.99", evidence = IdentityEvidence.UUID),
            decision,
        )
    }

    @Test
    fun `identidade apenas anunciada pede confirmacao`() {
        val decision = IpChangeEvaluator.evaluate(
            sala,
            listOf(sala),
            Observation(host = "192.168.0.99", uuid = salaUuid, verifiedByProtocol = false),
        )
        assertTrue(decision is IpChangeDecision.NeedsConfirmation)
    }

    @Test
    fun `ip ocupado por outra camera nao e reatribuido`() {
        val decision = IpChangeEvaluator.evaluate(
            sala,
            listOf(sala, garagem),
            Observation(host = "192.168.0.11", uuid = "urn:uuid:bbbb-2222", verifiedByProtocol = true),
        )
        assertEquals(IpChangeDecision.BelongsToOtherCamera("garagem", "192.168.0.11"), decision)
    }

    @Test
    fun `se o ip antigo ainda responde como a mesma camera, pede confirmacao`() {
        val decision = IpChangeEvaluator.evaluate(
            sala,
            listOf(sala),
            observation = Observation(host = "192.168.0.99", uuid = salaUuid, verifiedByProtocol = true),
            oldHostObservation = Observation(host = "192.168.0.10", uuid = salaUuid, verifiedByProtocol = true),
        )
        assertTrue(decision is IpChangeDecision.NeedsConfirmation)
    }

    @Test
    fun `se o ip antigo responde com outra identidade, a camera pode ter mudado`() {
        val decision = IpChangeEvaluator.evaluate(
            sala,
            listOf(sala, garagem),
            observation = Observation(host = "192.168.0.99", uuid = salaUuid, verifiedByProtocol = true),
            oldHostObservation = Observation(host = "192.168.0.10", uuid = "urn:uuid:outra", verifiedByProtocol = true),
        )
        assertTrue(decision is IpChangeDecision.AutoUpdate)
    }
}

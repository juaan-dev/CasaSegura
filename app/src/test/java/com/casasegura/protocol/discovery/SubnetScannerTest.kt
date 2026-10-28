package com.casasegura.protocol.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SubnetScannerTest {

    @Test
    fun `gera 253 hosts da mesma sub-rede sem o proprio aparelho`() {
        val hosts = SubnetScanner.candidateHosts("192.168.0.42")
        assertEquals(253, hosts.size)
        assertFalse(hosts.contains("192.168.0.42"))
        assertTrue(hosts.contains("192.168.0.1"))
        assertTrue(hosts.contains("192.168.0.254"))
        assertFalse(hosts.contains("192.168.0.0"))
        assertFalse(hosts.contains("192.168.0.255"))
    }

    @Test
    fun `limita a varredura a uma sub-rede de 24 bits`() {
        assertTrue(SubnetScanner.candidateHosts("10.1.2.3").all { it.startsWith("10.1.2.") })
    }

    @Test
    fun `rejeita endereco que nao e ipv4`() {
        assertThrows(IllegalArgumentException::class.java) { SubnetScanner.candidateHosts("abc") }
        assertThrows(IllegalArgumentException::class.java) { SubnetScanner.candidateHosts("192.168.0.300") }
    }
}

package com.casasegura.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HostValidationTest {

    @Test
    fun `aceita ipv4 e nome de host valido`() {
        assertTrue(isValidHost("192.168.0.50"))
        assertTrue(isValidHost("camera-sala.local"))
    }

    @Test
    fun `rejeita ipv4 fora da faixa e texto vazio ou invalido`() {
        assertFalse(isValidHost("192.168.0.300"))
        assertFalse(isValidHost(""))
        assertFalse(isValidHost("ca mera"))
        assertFalse(isValidHost("-invalido"))
    }
}

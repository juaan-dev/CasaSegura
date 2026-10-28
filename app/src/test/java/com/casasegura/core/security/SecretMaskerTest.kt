package com.casasegura.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SecretMaskerTest {

    @Test
    fun `mascara nao revela o tamanho da senha`() {
        assertEquals(SecretMasker.MASK, SecretMasker.mask("a"))
        assertEquals(SecretMasker.MASK, SecretMasker.mask("uma-senha-bem-maior"))
        assertEquals(SecretMasker.MASK, SecretMasker.mask(null))
    }

    @Test
    fun `remove usuario e senha de url rtsp`() {
        val redacted = SecretMasker.redactUrl("rtsp://admin:senha123@192.168.0.10:554/stream1")
        assertEquals("rtsp://***@192.168.0.10:554/stream1", redacted)
        assertFalse(redacted.contains("senha123"))
    }

    @Test
    fun `remove senha passada como parametro de url`() {
        val redacted = SecretMasker.redactUrl("http://host/cgi?user=a&password=segredo&x=1")
        assertFalse(redacted.contains("segredo"))
        assertEquals("http://host/cgi?user=a&password=***&x=1", redacted)
    }
}

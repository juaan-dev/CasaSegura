package com.casasegura.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamResolverTest {

    @Test
    fun `injeta usuario e senha codificados na url rtsp`() {
        assertEquals(
            "rtsp://admin:a%20b%40c@192.168.0.50:554/stream1",
            StreamResolver.withUserInfo("rtsp://192.168.0.50:554/stream1", "admin", "a b@c"),
        )
    }

    @Test
    fun `mantem a url quando ja tem credenciais`() {
        val url = "rtsp://u:p@192.168.0.50:554/stream1"
        assertEquals(url, StreamResolver.withUserInfo(url, "outro", "x"))
    }

    @Test
    fun `preserva a query da url`() {
        assertEquals(
            "rtsp://admin:x@10.0.0.2:554/s?ch=1",
            StreamResolver.withUserInfo("rtsp://10.0.0.2:554/s?ch=1", "admin", "x"),
        )
    }
}

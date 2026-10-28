package com.casasegura.protocol.onvif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WsDiscoveryMessagesTest {

    private val response = """
        <?xml version="1.0" encoding="UTF-8"?>
        <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:a="http://schemas.xmlsoap.org/ws/2004/08/addressing"
                    xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery">
          <s:Body>
            <d:ProbeMatches>
              <d:ProbeMatch>
                <a:EndpointReference><a:Address>urn:uuid:11111111-2222-3333-4444-555555555555</a:Address></a:EndpointReference>
                <d:Types>dn:NetworkVideoTransmitter</d:Types>
                <d:Scopes>onvif://www.onvif.org/type/video_encoder onvif://www.onvif.org/name/Sala</d:Scopes>
                <d:XAddrs>http://192.168.0.42/onvif/device_service</d:XAddrs>
              </d:ProbeMatch>
              <d:ProbeMatch>
                <a:EndpointReference><a:Address>urn:uuid:aaaa-bbbb</a:Address></a:EndpointReference>
                <d:XAddrs>http://192.168.0.50:8080/onvif/device_service http://[fe80::1]/onvif</d:XAddrs>
              </d:ProbeMatch>
            </d:ProbeMatches>
          </s:Body>
        </s:Envelope>
    """.trimIndent()

    @Test
    fun `probe contem o tipo de transmissor de video e um MessageID`() {
        val probe = WsDiscoveryMessages.probe("abc-123")
        assertTrue(probe.contains("dn:NetworkVideoTransmitter"))
        assertTrue(probe.contains("uuid:abc-123"))
        assertTrue(probe.contains("schemas-xmlsoap-org:ws:2005:04:discovery"))
    }

    @Test
    fun `le uuid, enderecos e escopos de cada resposta`() {
        val matches = WsDiscoveryMessages.parseMatches(response)

        assertEquals(2, matches.size)
        val first = matches[0]
        assertEquals("11111111-2222-3333-4444-555555555555", first.endpointUuid)
        assertEquals(listOf("http://192.168.0.42/onvif/device_service"), first.xAddrs)
        assertEquals("192.168.0.42", first.host)
        assertTrue(first.scopes.contains("onvif://www.onvif.org/name/Sala"))
    }

    @Test
    fun `extrai o host de enderecos ipv6 e com porta`() {
        val second = WsDiscoveryMessages.parseMatches(response)[1]
        assertEquals("192.168.0.50", second.host)
        assertEquals(2, second.xAddrs.size)
    }

    @Test
    fun `resposta malformada nao vira dispositivo`() {
        assertEquals(emptyList<WsDiscoveryMessages.ProbeMatch>(), WsDiscoveryMessages.parseMatches("<xml quebrado"))
    }
}

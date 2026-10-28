package com.casasegura.protocol.onvif

import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnvifMessagesTest {

    private val nonce = ByteArray(16) { it.toByte() }
    private val created = "2026-10-28T12:00:00Z"

    private val deviceInfoResponse = """
        <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:tds="http://www.onvif.org/ver10/device/wsdl">
          <s:Body>
            <tds:GetDeviceInformationResponse>
              <tds:Manufacturer>Acme</tds:Manufacturer>
              <tds:Model>IPC-A</tds:Model>
              <tds:FirmwareVersion>1.2.3</tds:FirmwareVersion>
              <tds:SerialNumber>SN-000111</tds:SerialNumber>
              <tds:HardwareId>HW-9</tds:HardwareId>
            </tds:GetDeviceInformationResponse>
          </s:Body>
        </s:Envelope>
    """.trimIndent()

    private val notAuthorizedFault = """
        <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
          <s:Body>
            <s:Fault>
              <s:Code><s:Value>s:Sender</s:Value></s:Code>
              <s:Reason><s:Text>Sender not Authorized</s:Text></s:Reason>
            </s:Fault>
          </s:Body>
        </s:Envelope>
    """.trimIndent()

    @Test
    fun `digest segue a regra WS-Security SHA-1 de nonce, data e senha`() {
        val expected = MessageDigest.getInstance("SHA-1").digest(
            nonce + created.toByteArray() + "segredo".toByteArray(),
        )
        assertEquals(Base64.getEncoder().encodeToString(expected), OnvifMessages.passwordDigest(nonce, created, "segredo"))
    }

    @Test
    fun `mensagem de autenticacao inclui usuario, nonce codificado e data`() {
        val xml = OnvifMessages.getDeviceInformation("admin", "segredo", nonce, created)

        assertTrue(xml.contains("<wsse:Username>admin</wsse:Username>"))
        assertTrue(xml.contains(Base64.getEncoder().encodeToString(nonce)))
        assertTrue(xml.contains("<wsu:Created>$created</wsu:Created>"))
        assertTrue(xml.contains("GetDeviceInformation"))
        // A senha em texto claro nunca vai na mensagem; só o digest.
        assertFalse(xml.contains(">segredo<"))
    }

    @Test
    fun `escapa caracteres especiais do usuario`() {
        val xml = OnvifMessages.getDeviceInformation("a<b>&c", "x", nonce, created)
        assertTrue(xml.contains("<wsse:Username>a&lt;b&gt;&amp;c</wsse:Username>"))
    }

    @Test
    fun `le fabricante, modelo e serial da resposta`() {
        val info = OnvifMessages.parseDeviceInformation(deviceInfoResponse)!!

        assertEquals("Acme", info.manufacturer)
        assertEquals("IPC-A", info.model)
        assertEquals("SN-000111", info.serialNumber)
        assertEquals("1.2.3", info.firmwareVersion)
    }

    @Test
    fun `resposta sem informacoes do dispositivo nao e aceita`() {
        assertNull(OnvifMessages.parseDeviceInformation("<xml/>"))
        assertNull(OnvifMessages.parseDeviceInformation("nao e xml"))
    }

    @Test
    fun `soap fault de autorizacao e reconhecido`() {
        assertTrue(OnvifMessages.isAuthenticationFault(notAuthorizedFault))
        assertFalse(OnvifMessages.isAuthenticationFault(deviceInfoResponse))
    }

    @Test
    fun `classifica credencial recusada, resposta ok e erro http`() {
        val client = OnvifClient()
        assertTrue(client.classify(401, "") is OnvifClient.Result.AuthenticationFailed)
        assertTrue(client.classify(200, notAuthorizedFault) is OnvifClient.Result.AuthenticationFailed)
        assertTrue(client.classify(200, deviceInfoResponse) is OnvifClient.Result.Ok)
        assertTrue(client.classify(500, "") is OnvifClient.Result.Unexpected)
    }

    private val profilesResponse = """
        <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:trt="http://www.onvif.org/ver10/media/wsdl">
          <s:Body>
            <trt:GetProfilesResponse>
              <trt:Profiles token="Profile_1" fixed="true"/>
              <trt:Profiles token="Profile_2" fixed="true"/>
            </trt:GetProfilesResponse>
          </s:Body>
        </s:Envelope>
    """.trimIndent()

    private val streamResponse = """
        <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:trt="http://www.onvif.org/ver10/media/wsdl"
                    xmlns:tt="http://www.onvif.org/ver10/schema">
          <s:Body>
            <trt:GetStreamUriResponse>
              <trt:MediaUri>
                <tt:Uri>rtsp://192.168.0.50:554/stream1</tt:Uri>
                <tt:InvalidAfterConnect>false</tt:InvalidAfterConnect>
              </trt:MediaUri>
            </trt:GetStreamUriResponse>
          </s:Body>
        </s:Envelope>
    """.trimIndent()

    @Test
    fun `le tokens dos perfis de midia`() {
        assertEquals(listOf("Profile_1", "Profile_2"), OnvifMessages.parseProfileTokens(profilesResponse))
    }

    @Test
    fun `mensagem de stream pede RTSP e o perfil escolhido`() {
        val xml = OnvifMessages.getStreamUri("admin", "segredo", nonce, created, "Profile_1")
        assertTrue(xml.contains("<tt:Protocol>RTSP</tt:Protocol>"))
        assertTrue(xml.contains("<trt:ProfileToken>Profile_1</trt:ProfileToken>"))
        assertFalse(xml.contains(">segredo<"))
    }

    @Test
    fun `le a url rtsp da resposta e recusa outros esquemas`() {
        assertEquals("rtsp://192.168.0.50:554/stream1", OnvifMessages.parseStreamUri(streamResponse))
        assertNull(OnvifMessages.parseStreamUri(streamResponse.replace("rtsp://", "http://")))
    }

    @Test
    fun `data de envio e ISO 8601 em UTC`() {
        assertEquals("2026-10-28T12:00:00Z", OnvifMessages.createdNow(Instant.parse("2026-10-28T12:00:00Z")))
    }
}

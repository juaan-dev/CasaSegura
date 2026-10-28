package com.casasegura.protocol.onvif

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Mensagens SOAP do ONVIF com autenticação WS-UsernameToken (digest). É o padrão público do ONVIF,
 * não o protocolo proprietário do VMS. Puro: não faz rede, então é testável no JVM.
 */
object OnvifMessages {

    private const val NS_DEVICE = "http://www.onvif.org/ver10/device/wsdl"
    private const val NS_WSSE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
    private const val NS_WSU = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
    private const val NS_DIGEST = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest"

    /** Digest WS-Security: Base64(SHA-1(nonce bruto + created + senha)). */
    fun passwordDigest(nonce: ByteArray, created: String, password: String): String {
        val sha1 = MessageDigest.getInstance("SHA-1")
        sha1.update(nonce)
        sha1.update(created.toByteArray(Charsets.UTF_8))
        sha1.update(password.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(sha1.digest())
    }

    fun createdNow(now: Instant = Instant.now()): String =
        DateTimeFormatter.ISO_INSTANT.format(now.atZone(ZoneOffset.UTC).toInstant())

    private fun securityHeader(username: String, password: String, nonce: ByteArray, created: String): String {
        val digest = passwordDigest(nonce, created, password)
        val nonceB64 = Base64.getEncoder().encodeToString(nonce)
        return """
            <wsse:Security s:mustUnderstand="1" xmlns:wsse="$NS_WSSE" xmlns:wsu="$NS_WSU">
              <wsse:UsernameToken>
                <wsse:Username>${escape(username)}</wsse:Username>
                <wsse:Password Type="$NS_DIGEST">$digest</wsse:Password>
                <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">$nonceB64</wsse:Nonce>
                <wsu:Created>$created</wsu:Created>
              </wsse:UsernameToken>
            </wsse:Security>
        """.trimIndent()
    }

    fun getDeviceInformation(username: String, password: String, nonce: ByteArray, created: String): String = envelope(
        header = securityHeader(username, password, nonce, created),
        body = "<tds:GetDeviceInformation xmlns:tds=\"$NS_DEVICE\"/>",
    )

    data class DeviceInformation(
        val manufacturer: String?,
        val model: String?,
        val firmwareVersion: String?,
        val serialNumber: String?,
        val hardwareId: String?,
    )

    /** Lê a resposta de GetDeviceInformation. Retorna null se não for uma resposta válida. */
    fun parseDeviceInformation(xml: String): DeviceInformation? {
        val root = parse(xml) ?: return null
        val response = root.elements(NS_DEVICE, "GetDeviceInformationResponse").firstOrNull() ?: return null
        fun text(name: String) = response.elements(NS_DEVICE, name).firstOrNull()?.textContent?.trim()?.takeIf { it.isNotEmpty() }
        return DeviceInformation(
            manufacturer = text("Manufacturer"),
            model = text("Model"),
            firmwareVersion = text("FirmwareVersion"),
            serialNumber = text("SerialNumber"),
            hardwareId = text("HardwareId"),
        )
    }

    private const val NS_MEDIA = "http://www.onvif.org/ver10/media/wsdl"
    private const val NS_SCHEMA = "http://www.onvif.org/ver10/schema"

    fun getProfiles(username: String, password: String, nonce: ByteArray, created: String): String = envelope(
        header = securityHeader(username, password, nonce, created),
        body = "<trt:GetProfiles xmlns:trt=\"$NS_MEDIA\"/>",
    )

    /** Tokens dos perfis de mídia. Cada câmera pode ter stream principal e substream. */
    fun parseProfileTokens(xml: String): List<String> {
        val root = parse(xml) ?: return emptyList()
        return root.elements(NS_MEDIA, "Profiles").mapNotNull { it.getAttribute("token").takeIf { t -> t.isNotBlank() } }
    }

    fun getStreamUri(username: String, password: String, nonce: ByteArray, created: String, profileToken: String): String = envelope(
        header = securityHeader(username, password, nonce, created),
        body = """
            <trt:GetStreamUri xmlns:trt="$NS_MEDIA" xmlns:tt="$NS_SCHEMA">
              <trt:StreamSetup>
                <tt:Stream>RTP-Unicast</tt:Stream>
                <tt:Transport><tt:Protocol>RTSP</tt:Protocol></tt:Transport>
              </trt:StreamSetup>
              <trt:ProfileToken>${escape(profileToken)}</trt:ProfileToken>
            </trt:GetStreamUri>
        """.trimIndent(),
    )

    /** URL RTSP devolvida pela câmera, ou null se a resposta não tiver uma. */
    fun parseStreamUri(xml: String): String? {
        val root = parse(xml) ?: return null
        return root.elements(NS_MEDIA, "MediaUri").firstOrNull()
            ?.elements(NS_SCHEMA, "Uri")?.firstOrNull()
            ?.textContent?.trim()?.takeIf { it.startsWith("rtsp://") }
    }

    /** Falha de autenticação ONVIF aparece como SOAP Fault com "NotAuthorized" ou "Sender". */
    fun isAuthenticationFault(xml: String): Boolean {
        val root = parse(xml) ?: return false
        val fault = root.elements(NS_SOAP12, "Fault").firstOrNull() ?: return false
        val text = fault.textContent.lowercase()
        return text.contains("notauthorized") || text.contains("not authorized") || text.contains("sender")
    }

    private const val NS_SOAP12 = "http://www.w3.org/2003/05/soap-envelope"

    private fun envelope(header: String, body: String): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <s:Envelope xmlns:s="$NS_SOAP12">
          <s:Header>$header</s:Header>
          <s:Body>$body</s:Body>
        </s:Envelope>
    """.trimIndent()

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun parse(xml: String): Element? = try {
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
            .documentElement
    } catch (_: Exception) {
        null
    }

    private fun Element.elements(ns: String, localName: String): List<Element> {
        val nodes = getElementsByTagNameNS(ns, localName)
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }
}

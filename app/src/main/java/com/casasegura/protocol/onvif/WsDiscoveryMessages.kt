package com.casasegura.protocol.onvif

import java.io.ByteArrayInputStream
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Mensagens do WS-Discovery usadas pelo ONVIF (padrão público, não protocolo proprietário).
 * Descoberta é multicast em 239.255.255.250:3702 e só deve ser disparada por ação explícita do usuário.
 */
object WsDiscoveryMessages {

    const val MULTICAST_ADDRESS = "239.255.255.250"
    const val PORT = 3702

    data class ProbeMatch(
        /** UUID anunciado pelo dispositivo, sem o prefixo urn:uuid:. Não confirmado por autenticação. */
        val endpointUuid: String?,
        val xAddrs: List<String>,
        val scopes: List<String>,
    ) {
        /** Host extraído do primeiro endereço de serviço. */
        val host: String? get() = xAddrs.firstNotNullOfOrNull { runCatching { URI(it).host }.getOrNull() }
    }

    fun probe(messageId: String): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <e:Envelope xmlns:e="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:w="http://schemas.xmlsoap.org/ws/2004/08/addressing"
                    xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery"
                    xmlns:dn="http://www.onvif.org/ver10/network/wsdl">
          <e:Header>
            <w:MessageID>uuid:$messageId</w:MessageID>
            <w:To e:mustUnderstand="true">urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To>
            <w:Action e:mustUnderstand="true">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action>
          </e:Header>
          <e:Body>
            <d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe>
          </e:Body>
        </e:Envelope>
    """.trimIndent()

    /** Lê as respostas ProbeMatch. Respostas malformadas são ignoradas, nunca tratadas como câmera. */
    fun parseMatches(xml: String): List<ProbeMatch> {
        val document = try {
            DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        } catch (_: Exception) {
            return emptyList()
        }
        val matches = document.getElementsByTagNameNS(NS_DISCOVERY, "ProbeMatch")
        return (0 until matches.length).mapNotNull { index ->
            val element = matches.item(index) as? Element ?: return@mapNotNull null
            ProbeMatch(
                endpointUuid = element.firstText(NS_ADDRESSING, "Address")?.removePrefix("urn:uuid:")?.takeIf { it.isNotBlank() },
                xAddrs = element.firstText(NS_DISCOVERY, "XAddrs")?.split(' ')?.filter { it.isNotBlank() }.orEmpty(),
                scopes = element.firstText(NS_DISCOVERY, "Scopes")?.split(' ')?.filter { it.isNotBlank() }.orEmpty(),
            ).takeIf { it.xAddrs.isNotEmpty() }
        }
    }

    private fun Element.firstText(ns: String, localName: String): String? =
        getElementsByTagNameNS(ns, localName).item(0)?.textContent?.trim()

    private const val NS_DISCOVERY = "http://schemas.xmlsoap.org/ws/2005/04/discovery"
    private const val NS_ADDRESSING = "http://schemas.xmlsoap.org/ws/2004/08/addressing"
}

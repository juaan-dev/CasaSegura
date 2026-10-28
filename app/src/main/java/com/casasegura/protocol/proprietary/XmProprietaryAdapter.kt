package com.casasegura.protocol.proprietary

import com.casasegura.protocol.ProbeResult
import com.casasegura.protocol.ProtocolAdapter

/**
 * Adaptador do protocolo proprietário observado no VMS (família XM/DVRIP).
 *
 * Ainda não implementado: a porta, o formato do login e o handshake dependem de
 * captura de rede com a câmera real (research/findings). Por isso este adaptador
 * responde explicitamente que não há suporte, sem simular conexão.
 */
class XmProprietaryAdapter : ProtocolAdapter {

    override val method: String = "XM/DVRIP (em investigação)"

    override suspend fun probe(host: String, port: Int?): ProbeResult =
        ProbeResult.Unsupported(
            "Protocolo proprietário ainda não validado. Aguardando captura de rede da câmera.",
        )
}

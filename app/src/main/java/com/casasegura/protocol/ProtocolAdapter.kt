package com.casasegura.protocol

/**
 * Contrato de um adaptador de protocolo (seção 2.8 do plano). A UI só conhece este contrato;
 * detalhes de autenticação e de mensagens ficam dentro de cada adaptador.
 */
interface ProtocolAdapter {
    val method: String

    /** Verifica se o endereço responde a este protocolo. Nunca presume sucesso. */
    suspend fun probe(host: String, port: Int?): ProbeResult
}

sealed interface ProbeResult {
    data class Compatible(val detail: String) : ProbeResult
    data class Unsupported(val reason: String) : ProbeResult
    data class Failed(val reason: String) : ProbeResult
}

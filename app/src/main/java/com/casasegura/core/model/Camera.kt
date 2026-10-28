package com.casasegura.core.model

/**
 * Câmera cadastrada. A identidade lógica (id) é separada do endereço atual (currentHost),
 * para que uma mudança de IP não exija novo cadastro (seção 2.6 do plano).
 */
data class Camera(
    val id: String,
    val name: String,
    val currentHost: String?,
    val credentialId: String?,
    val connection: ConnectionMethod,
    val location: String = "",
    val tags: Set<String> = emptySet(),
    val group: String = "",
    val notes: String = "",
)

/** Método de conexão realmente usado (seção 2.8). Nunca presumir um único protocolo. */
enum class ConnectionMethod {
    ONVIF,
    RTSP,
    NVR,
    UNKNOWN,
}

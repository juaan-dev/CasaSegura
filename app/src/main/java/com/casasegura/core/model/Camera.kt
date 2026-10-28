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
    val identity: CameraIdentity = CameraIdentity(),
    val lastSeenEpochMillis: Long? = null,
    val location: String = "",
    val tags: Set<String> = emptySet(),
    val group: String = "",
    val notes: String = "",
)

/**
 * Identificadores estáveis, em ordem de confiança (seção 2.6 do plano):
 * UUID ONVIF, serial, MAC e modelo. Modelo sozinho nunca identifica uma câmera.
 */
data class CameraIdentity(
    val uuid: String? = null,
    val serial: String? = null,
    val mac: String? = null,
    val model: String? = null,
)

/** Método de conexão realmente usado (seção 2.8). Nunca presumir um único protocolo. */
enum class ConnectionMethod {
    ONVIF,
    RTSP,
    NVR,
    UNKNOWN,
}

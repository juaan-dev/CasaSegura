package com.casasegura.core.model

/**
 * Metadados de uma credencial reutilizável. A senha NÃO fica aqui:
 * ela é guardada de forma protegida (Android Keystore), referenciada por [id] (seção 2.3 do plano).
 */
data class Credential(
    val id: String,
    val alias: String,
    val username: String,
    val scope: String = "",
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

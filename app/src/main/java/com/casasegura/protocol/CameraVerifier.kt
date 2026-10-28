package com.casasegura.protocol

import com.casasegura.core.cameras.CameraRepository
import com.casasegura.core.credentials.CredentialService
import com.casasegura.core.model.Camera
import com.casasegura.core.model.CameraIdentity
import com.casasegura.core.model.ConnectionMethod
import com.casasegura.protocol.onvif.OnvifClient

/**
 * Confirma a identidade de uma câmera por consulta ONVIF autenticada (seção 2.2 e 2.6 do plano).
 * Só uma resposta autenticada grava o serial e o modelo: um anúncio na rede não confirma nada.
 */
class CameraVerifier(
    private val credentials: CredentialService,
    private val cameras: CameraRepository,
    private val onvif: OnvifClient,
) {

    sealed interface Outcome {
        data class Confirmed(val camera: Camera) : Outcome
        data object NoCredential : Outcome
        data object NoHost : Outcome
        data object AuthenticationFailed : Outcome
        data class Failed(val reason: String) : Outcome
        data object NotFound : Outcome
    }

    suspend fun verify(cameraId: String): Outcome {
        val camera = cameras.get(cameraId) ?: return Outcome.NotFound
        val host = camera.currentHost ?: return Outcome.NoHost
        val credentialId = camera.credentialId ?: return Outcome.NoCredential
        val username = credentials.list().firstOrNull { it.id == credentialId }?.username
            ?: return Outcome.NoCredential
        val password = credentials.passwordForConnection(credentialId) ?: return Outcome.NoCredential

        return when (val result = onvif.deviceInformation(host, username, password)) {
            is OnvifClient.Result.Ok -> {
                val confirmed = camera.copy(
                    connection = ConnectionMethod.ONVIF,
                    identity = CameraIdentity(
                        uuid = camera.identity.uuid,
                        serial = result.value.serialNumber ?: camera.identity.serial,
                        mac = camera.identity.mac,
                        model = result.value.model ?: camera.identity.model,
                    ),
                    lastSeenEpochMillis = System.currentTimeMillis(),
                )
                cameras.save(confirmed)
                Outcome.Confirmed(confirmed)
            }
            OnvifClient.Result.AuthenticationFailed -> Outcome.AuthenticationFailed
            is OnvifClient.Result.Unreachable -> Outcome.Failed(result.reason)
            is OnvifClient.Result.Unexpected -> Outcome.Failed(result.reason)
        }
    }
}

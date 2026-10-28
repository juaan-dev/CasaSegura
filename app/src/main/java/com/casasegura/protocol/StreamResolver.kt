package com.casasegura.protocol

import com.casasegura.core.cameras.CameraRepository
import com.casasegura.core.credentials.CredentialService
import com.casasegura.protocol.onvif.OnvifClient
import java.net.URI
import java.net.URLEncoder

/**
 * Obtém a URL RTSP de uma câmera para reprodução (seção 2.7 do plano).
 * A URL devolvida pode conter usuário e senha: é segredo, fica só em memória e nunca vai para logs.
 */
class StreamResolver(
    private val credentials: CredentialService,
    private val cameras: CameraRepository,
    private val onvif: OnvifClient,
) {

    sealed interface Outcome {
        /** [url] já inclui credenciais, por isso não deve ser exibida nem registrada. */
        data class Ready(val url: String) : Outcome
        data object NotFound : Outcome
        data object NoHost : Outcome
        data object NoCredential : Outcome
        data object AuthenticationFailed : Outcome
        data class Failed(val reason: String) : Outcome
    }

    suspend fun resolve(cameraId: String): Outcome {
        val camera = cameras.get(cameraId) ?: return Outcome.NotFound
        val host = camera.currentHost ?: return Outcome.NoHost
        val credentialId = camera.credentialId ?: return Outcome.NoCredential
        val username = credentials.list().firstOrNull { it.id == credentialId }?.username
            ?: return Outcome.NoCredential
        val password = credentials.passwordForConnection(credentialId) ?: return Outcome.NoCredential

        return when (val result = onvif.rtspStreamUri(host, username, password)) {
            is OnvifClient.Result.Ok -> Outcome.Ready(withUserInfo(result.value, username, password))
            OnvifClient.Result.AuthenticationFailed -> Outcome.AuthenticationFailed
            is OnvifClient.Result.Unreachable -> Outcome.Failed(result.reason)
            is OnvifClient.Result.Unexpected -> Outcome.Failed(result.reason)
        }
    }

    companion object {
        /** Acrescenta usuário e senha codificados à URL, se ela ainda não tiver credenciais. */
        internal fun withUserInfo(url: String, username: String, password: String): String {
            val uri = URI(url)
            if (uri.userInfo != null) return url
            val user = URLEncoder.encode(username, "UTF-8").replace("+", "%20")
            val pass = URLEncoder.encode(password, "UTF-8").replace("+", "%20")
            return "${uri.scheme}://$user:$pass@${uri.rawAuthority}${uri.rawPath.orEmpty()}" +
                (uri.rawQuery?.let { "?$it" } ?: "")
        }
    }
}

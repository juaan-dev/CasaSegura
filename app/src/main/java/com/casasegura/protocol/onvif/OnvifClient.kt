package com.casasegura.protocol.onvif

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cliente ONVIF mínimo, com autenticação WS-Security. Só consultas autenticadas bem-sucedidas
 * confirmam identidade ou fornecem a URL do fluxo.
 */
class OnvifClient(
    private val timeoutMillis: Int = 4_000,
    private val random: SecureRandom = SecureRandom(),
) {

    sealed interface Result<out T> {
        data class Ok<T>(val value: T) : Result<T>
        data object AuthenticationFailed : Result<Nothing>
        data class Unreachable(val reason: String) : Result<Nothing>
        data class Unexpected(val reason: String) : Result<Nothing>
    }

    /** Consulta as informações do dispositivo (fabricante, modelo, serial). */
    suspend fun deviceInformation(host: String, username: String, password: String, port: Int = 80): Result<OnvifMessages.DeviceInformation> =
        call(host, port, DEVICE_PATH, username, password) { nonce, created ->
            OnvifMessages.getDeviceInformation(username, password, nonce, created)
        }.let { r ->
            when (r) {
                is Result.Ok -> OnvifMessages.parseDeviceInformation(r.value)
                    ?.let { Result.Ok(it) } ?: Result.Unexpected("Resposta ONVIF sem informações do dispositivo")
                is Result.AuthenticationFailed -> Result.AuthenticationFailed
                is Result.Unreachable -> Result.Unreachable(r.reason)
                is Result.Unexpected -> Result.Unexpected(r.reason)
            }
        }

    /**
     * URL RTSP do primeiro perfil de mídia. A URL pode trazer credenciais do próprio dispositivo;
     * quem chama deve tratá-la como segredo: manter em memória e mascarar em logs.
     */
    suspend fun rtspStreamUri(host: String, username: String, password: String, port: Int = 80): Result<String> {
        val profiles = call(host, port, MEDIA_PATH, username, password) { nonce, created ->
            OnvifMessages.getProfiles(username, password, nonce, created)
        }
        val body = when (profiles) {
            is Result.Ok -> profiles.value
            is Result.AuthenticationFailed -> return Result.AuthenticationFailed
            is Result.Unreachable -> return Result.Unreachable(profiles.reason)
            is Result.Unexpected -> return Result.Unexpected(profiles.reason)
        }
        val token = OnvifMessages.parseProfileTokens(body).firstOrNull()
            ?: return Result.Unexpected("A câmera não informou perfis de mídia")

        return when (val stream = call(host, port, MEDIA_PATH, username, password) { nonce, created ->
            OnvifMessages.getStreamUri(username, password, nonce, created, token)
        }) {
            is Result.Ok -> OnvifMessages.parseStreamUri(stream.value)?.let { Result.Ok(it) }
                ?: Result.Unexpected("A câmera não devolveu uma URL RTSP")
            is Result.AuthenticationFailed -> Result.AuthenticationFailed
            is Result.Unreachable -> Result.Unreachable(stream.reason)
            is Result.Unexpected -> Result.Unexpected(stream.reason)
        }
    }

    private suspend fun call(
        host: String,
        port: Int,
        path: String,
        username: String,
        password: String,
        build: (ByteArray, String) -> String,
    ): Result<String> = withContext(Dispatchers.IO) {
        val nonce = ByteArray(16).also { random.nextBytes(it) }
        val body = build(nonce, OnvifMessages.createdNow())
        try {
            val connection = (URL("http://$host:$port$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = timeoutMillis
                readTimeout = timeoutMillis
                doOutput = true
                setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8")
            }
            try {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                classify(code, text)
            } finally {
                connection.disconnect()
            }
        } catch (_: SocketTimeoutException) {
            Result.Unreachable("Tempo esgotado ao falar com $host")
        } catch (_: IOException) {
            Result.Unreachable("Não foi possível conectar a $host")
        }
    }

    /** Separa resposta válida, credencial recusada e respostas inesperadas. */
    internal fun classify(code: Int, body: String): Result<String> {
        if (code == 401 || OnvifMessages.isAuthenticationFault(body)) return Result.AuthenticationFailed
        if (code !in 200..299) return Result.Unexpected("HTTP $code")
        return Result.Ok(body)
    }

    private companion object {
        const val DEVICE_PATH = "/onvif/device_service"
        const val MEDIA_PATH = "/onvif/media_service"
    }
}

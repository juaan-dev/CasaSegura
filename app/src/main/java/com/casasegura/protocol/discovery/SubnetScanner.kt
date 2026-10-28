package com.casasegura.protocol.discovery

import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Varredura TCP limitada à sub-rede /24 do aparelho (seção 2.1 do plano).
 * Porta aberta NÃO significa câmera: o resultado é só um indício, que depois precisa de validação de protocolo.
 * Só deve ser disparada por ação explícita do usuário; cancelar a coroutine interrompe a varredura.
 */
object SubnetScanner {

    /** Portas comuns de câmeras e NVRs (HTTP/ONVIF, RTSP, HTTP alternativa). */
    val DEFAULT_PORTS = listOf(80, 554, 8000)

    data class Hit(val host: String, val openPorts: List<Int>)

    /** Hosts da /24 de [localIp], sem o próprio aparelho nem o endereço de rede e broadcast. */
    fun candidateHosts(localIp: String): List<String> {
        val octets = localIp.split('.')
        require(octets.size == 4 && octets.all { it.toIntOrNull() in 0..255 }) { "IPv4 inválido: $localIp" }
        val prefix = octets.take(3).joinToString(".")
        val self = octets[3].toInt()
        return (1..254).filter { it != self }.map { "$prefix.$it" }
    }

    /** IPv4 do aparelho na rede local, sem precisar de permissão de localização. */
    fun localIpv4(): String? = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false && it.isSiteLocalAddress }
        ?.hostAddress

    /**
     * Testa [ports] em cada host com no máximo [concurrency] conexões simultâneas.
     * [onProgress] recebe (testados, total) para exibir o andamento.
     */
    suspend fun scan(
        localIp: String,
        ports: List<Int> = DEFAULT_PORTS,
        timeoutMillis: Int = 400,
        concurrency: Int = 32,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): List<Hit> = withContext(Dispatchers.IO) {
        val hosts = candidateHosts(localIp)
        val permits = Semaphore(concurrency)
        val done = AtomicInteger(0)
        coroutineScope {
            hosts.map { host ->
                async {
                    permits.withPermit {
                        val open = ports.filter { isOpen(host, it, timeoutMillis) }
                        onProgress(done.incrementAndGet(), hosts.size)
                        if (open.isNotEmpty()) Hit(host, open) else null
                    }
                }
            }.mapNotNull { it.await() }
        }
    }

    private fun isOpen(host: String, port: Int, timeoutMillis: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMillis)
            true
        }
    } catch (_: Exception) {
        false
    }
}

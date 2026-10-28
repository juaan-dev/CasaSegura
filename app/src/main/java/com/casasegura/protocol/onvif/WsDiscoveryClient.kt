package com.casasegura.protocol.onvif

import android.content.Context
import android.net.wifi.WifiManager
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Envia um Probe WS-Discovery e coleta as respostas durante [timeoutMillis].
 * Usa um MulticastLock, sem o qual o Wi-Fi descarta pacotes multicast em muitos aparelhos.
 */
class WsDiscoveryClient(private val context: Context) {

    suspend fun probe(timeoutMillis: Int = 3_000): List<WsDiscoveryMessages.ProbeMatch> = withContext(Dispatchers.IO) {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = wifi?.createMulticastLock("casasegura-ws-discovery")?.apply { setReferenceCounted(false) }
        lock?.acquire()
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = timeoutMillis.coerceAtLeast(500)
                val payload = WsDiscoveryMessages.probe(UUID.randomUUID().toString()).toByteArray(Charsets.UTF_8)
                val target = InetAddress.getByName(WsDiscoveryMessages.MULTICAST_ADDRESS)
                socket.send(DatagramPacket(payload, payload.size, target, WsDiscoveryMessages.PORT))

                val results = mutableListOf<WsDiscoveryMessages.ProbeMatch>()
                val buffer = ByteArray(16 * 1024)
                val deadline = System.currentTimeMillis() + timeoutMillis
                while (System.currentTimeMillis() < deadline) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        break
                    }
                    val xml = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    results += WsDiscoveryMessages.parseMatches(xml)
                }
                results.distinctBy { it.endpointUuid ?: it.xAddrs.first() }
            }
        } finally {
            lock?.let { if (it.isHeld) it.release() }
        }
    }
}

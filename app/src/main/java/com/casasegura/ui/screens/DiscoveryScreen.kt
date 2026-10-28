package com.casasegura.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casasegura.core.model.Camera
import com.casasegura.core.model.CameraIdentity
import com.casasegura.core.model.ConnectionMethod
import com.casasegura.protocol.discovery.SubnetScanner
import com.casasegura.protocol.onvif.WsDiscoveryMessages
import com.casasegura.ui.AppServices
import com.casasegura.ui.components.CsButton
import com.casasegura.ui.components.StatusChip
import com.casasegura.ui.theme.Dimens
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Descoberta por ação explícita do usuário, em três caminhos:
 * - WS-Discovery (ONVIF): o dispositivo anunciou a identidade, ainda não confirmada;
 * - varredura da sub-rede /24: só mostra portas abertas, que não provam que é câmera;
 * - endereço informado à mão.
 */
@Composable
fun DiscoveryScreen(services: AppServices, dimens: Dimens) {
    val scope = rememberCoroutineScope()
    val results = remember { mutableStateListOf<WsDiscoveryMessages.ProbeMatch>() }
    val scanHits = remember { mutableStateListOf<SubnetScanner.Hit>() }
    var searching by remember { mutableStateOf(false) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var scanProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var manualHost by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    val scanning = scanJob?.isActive == true

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(dimens.minTargetDp.dp / 2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Descobrir câmeras",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = (dimens.bodyTextSp + 10).sp,
            )
        }

        // 1. WS-Discovery
        item {
            CsButton(
                onClick = {
                    scope.launch {
                        searching = true
                        val found = services.discovery.probe()
                        results.clear()
                        results.addAll(found)
                        searching = false
                        message = if (found.isEmpty()) "Nenhum dispositivo ONVIF respondeu" else "${found.size} dispositivo(s) ONVIF respondeu"
                    }
                },
                enabled = !searching,
                modifier = Modifier.heightIn(min = dimens.minTargetDp.dp),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (searching) "Procurando..." else "Procurar câmeras ONVIF", fontSize = dimens.bodyTextSp.sp)
            }
        }
        items(results, key = { it.endpointUuid ?: it.xAddrs.first() }) { match ->
            val name = match.scopes.firstNotNullOfOrNull { it.substringAfter("/name/", "").takeIf { n -> n.isNotBlank() } }
                ?: match.host
                ?: "Câmera"
            DiscoveryCard(
                title = name,
                subtitle = "Endereço: ${match.host ?: "desconhecido"} · UUID: ${match.endpointUuid ?: "não informado"}",
                chip = "ONVIF anunciado · não confirmado",
                dimens = dimens,
                onAdd = {
                    scope.launch {
                        val host = match.host
                        if (host == null) {
                            message = "Dispositivo sem endereço utilizável"
                            return@launch
                        }
                        services.cameras.save(
                            Camera(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                currentHost = host,
                                credentialId = null,
                                connection = ConnectionMethod.ONVIF,
                                identity = CameraIdentity(uuid = match.endpointUuid),
                            ),
                        )
                        message = "$name adicionada. Associe uma credencial e verifique em Câmeras."
                    }
                },
            )
        }

        // 2. Varredura limitada da sub-rede
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CsButton(
                    onClick = {
                        val localIp = SubnetScanner.localIpv4()
                        if (localIp == null) {
                            message = "Não foi possível descobrir o endereço deste aparelho na rede"
                            return@CsButton
                        }
                        scanHits.clear()
                        scanProgress = 0 to 0
                        scanJob = scope.launch {
                            val hits = SubnetScanner.scan(localIp) { done, total -> scanProgress = done to total }
                            scanHits.addAll(hits)
                            message = "Varredura concluída: ${hits.size} endereço(s) com porta aberta"
                        }
                    },
                    enabled = !scanning,
                    modifier = Modifier.heightIn(min = dimens.minTargetDp.dp),
                ) {
                    Text("Varrer sub-rede", fontSize = dimens.bodyTextSp.sp)
                }
                if (scanning) {
                    CsButton(
                        onClick = {
                            scanJob?.cancel()
                            message = "Varredura cancelada"
                        },
                        modifier = Modifier.heightIn(min = dimens.minTargetDp.dp),
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cancelar", fontSize = dimens.bodyTextSp.sp)
                    }
                }
            }
        }
        if (scanning) {
            item {
                val (done, total) = scanProgress ?: (0 to 0)
                Text(
                    text = "Testando $done de $total endereços (/24, portas comuns)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = (dimens.bodyTextSp - 2).sp,
                )
            }
        }
        items(scanHits, key = { it.host }) { hit ->
            DiscoveryCard(
                title = hit.host,
                subtitle = "Portas abertas: ${hit.openPorts.joinToString()}",
                chip = "Porta aberta · não é prova de câmera",
                dimens = dimens,
                onAdd = {
                    scope.launch {
                        services.cameras.save(
                            Camera(
                                id = UUID.randomUUID().toString(),
                                name = hit.host,
                                currentHost = hit.host,
                                credentialId = null,
                                connection = ConnectionMethod.UNKNOWN,
                            ),
                        )
                        message = "${hit.host} adicionado sem protocolo validado"
                    }
                },
            )
        }

        // 3. Endereço informado à mão
        item {
            Text(
                text = "Adicionar por endereço",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (dimens.bodyTextSp - 2).sp,
            )
        }
        item {
            OutlinedTextField(
                value = manualHost,
                onValueChange = { manualHost = it },
                label = { Text("Endereço IP ou nome (ex.: 192.168.0.50)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTargetDp.dp),
            )
        }
        item {
            OutlinedTextField(
                value = manualName,
                onValueChange = { manualName = it },
                label = { Text("Nome (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTargetDp.dp),
            )
        }
        item {
            CsButton(
                onClick = {
                    val host = manualHost.trim()
                    if (!isValidHost(host)) {
                        message = "Informe um endereço IPv4 ou nome válido"
                        return@CsButton
                    }
                    scope.launch {
                        services.cameras.save(
                            Camera(
                                id = UUID.randomUUID().toString(),
                                name = manualName.trim().ifEmpty { host },
                                currentHost = host,
                                credentialId = null,
                                connection = ConnectionMethod.UNKNOWN,
                            ),
                        )
                        message = "Câmera adicionada por endereço. Associe uma credencial e verifique."
                        manualHost = ""
                        manualName = ""
                    }
                },
                modifier = Modifier.heightIn(min = dimens.minTargetDp.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Adicionar por endereço", fontSize = dimens.bodyTextSp.sp)
            }
        }

        message?.let { text ->
            item { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = dimens.bodyTextSp.sp) }
        }
    }
}

/** Card de resultado de descoberta. O chip deixa explícito o que foi validado e o que não foi. */
@Composable
private fun DiscoveryCard(
    title: String,
    subtitle: String,
    chip: String,
    dimens: Dimens,
    onAdd: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.bodyTextSp.sp)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = (dimens.bodyTextSp - 3).sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(chip, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CsButton(onClick = onAdd, modifier = Modifier.heightIn(min = dimens.minTargetDp.dp)) {
            Text("Adicionar", fontSize = (dimens.bodyTextSp - 2).sp)
        }
    }
}

/** Aceita IPv4 ou nome de host simples. Não resolve nem testa o endereço. */
internal fun isValidHost(value: String): Boolean {
    if (value.isEmpty()) return false
    val ipv4 = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")
    ipv4.find(value)?.let { m ->
        return m.groupValues.drop(1).all { it.toInt() in 0..255 }
    }
    return Regex("""^[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$""").matches(value)
}

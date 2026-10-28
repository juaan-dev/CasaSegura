package com.casasegura.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casasegura.core.model.Camera
import com.casasegura.core.model.CameraIdentity
import com.casasegura.core.model.ConnectionMethod
import com.casasegura.protocol.onvif.WsDiscoveryMessages
import com.casasegura.ui.AppServices
import com.casasegura.ui.theme.Dimens
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * Descoberta ONVIF por ação explícita do usuário. O que aparece aqui é só o que o dispositivo anunciou:
 * a identidade não é confirmada por autenticação, então não é usada para trocar IP automaticamente.
 */
@Composable
fun DiscoveryScreen(services: AppServices, dimens: Dimens) {
    val scope = rememberCoroutineScope()
    val results = remember { mutableStateListOf<WsDiscoveryMessages.ProbeMatch>() }
    var searching by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

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
        item {
            Text(
                text = "Procura câmeras ONVIF na rede local. Identidades anunciadas ainda não são confirmadas.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (dimens.bodyTextSp - 2).sp,
            )
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        searching = true
                        val found = services.discovery.probe()
                        results.clear()
                        results.addAll(found)
                        searching = false
                        message = if (found.isEmpty()) "Nenhuma câmera respondeu" else "${found.size} dispositivo(s) respondeu"
                    }
                },
                enabled = !searching,
                modifier = Modifier.heightIn(min = dimens.minTargetDp.dp),
            ) {
                Text(if (searching) "Procurando..." else "Procurar câmeras na rede", fontSize = dimens.bodyTextSp.sp)
            }
        }
        message?.let { text ->
            item { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = dimens.bodyTextSp.sp) }
        }
        items(results, key = { it.endpointUuid ?: it.xAddrs.first() }) { match ->
            val name = match.scopes.firstNotNullOfOrNull { it.substringAfter("/name/", "").takeIf { n -> n.isNotBlank() } }
                ?: match.host
                ?: "Câmera"
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(name, color = MaterialTheme.colorScheme.onBackground, fontSize = dimens.bodyTextSp.sp)
                Text(
                    text = "Endereço: ${match.host ?: "desconhecido"} · UUID: ${match.endpointUuid ?: "não informado"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = (dimens.bodyTextSp - 2).sp,
                )
                Button(
                    onClick = {
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
                            message = "$name adicionada. Associe uma credencial em Câmeras."
                        }
                    },
                    modifier = Modifier.heightIn(min = dimens.minTargetDp.dp),
                ) {
                    Text("Adicionar", fontSize = dimens.bodyTextSp.sp)
                }
            }
        }
    }
}

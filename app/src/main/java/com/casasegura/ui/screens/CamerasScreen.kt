package com.casasegura.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.casasegura.core.identity.IpChangeDecision
import com.casasegura.core.identity.Observation
import com.casasegura.core.model.Camera
import com.casasegura.ui.AppServices
import com.casasegura.ui.components.CsButton
import com.casasegura.ui.components.EmptyState
import com.casasegura.ui.components.StatusChip
import com.casasegura.ui.components.WarningLine
import com.casasegura.ui.theme.Dimens
import kotlinx.coroutines.launch

/**
 * Lista de câmeras cadastradas. "Verificar endereços" faz uma descoberta e aplica só as trocas
 * de IP seguras; o restante aparece como pendência para o usuário confirmar.
 */
@Composable
fun CamerasScreen(services: AppServices, dimens: Dimens) {
    val scope = rememberCoroutineScope()
    val cameras = remember { mutableStateListOf<Camera>() }
    val pending = remember { mutableStateListOf<String>() }
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        cameras.clear()
        cameras.addAll(services.cameras.list())
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(dimens.minTargetDp.dp / 2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Câmeras",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = (dimens.bodyTextSp + 10).sp,
            )
        }
        item {
            CsButton(
                onClick = {
                    scope.launch {
                        checking = true
                        val observations = services.discovery.probe().mapNotNull { match ->
                            match.host?.let { Observation(host = it, uuid = match.endpointUuid, verifiedByProtocol = false) }
                        }
                        val report = services.ipMonitor.scan(observations)
                        pending.clear()
                        pending += report.pending.map { describe(it, cameras) }
                        checking = false
                        reload()
                        message = when {
                            report.applied.isNotEmpty() -> "${report.applied.size} endereço(s) atualizado(s) automaticamente"
                            report.pending.isNotEmpty() -> "Há endereços que precisam de confirmação"
                            else -> "Nenhuma mudança de endereço encontrada"
                        }
                    }
                },
                enabled = !checking,
                modifier = Modifier.heightIn(min = dimens.minTargetDp.dp),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (checking) "Verificando..." else "Verificar endereços", fontSize = dimens.bodyTextSp.sp)
            }
        }
        message?.let { text ->
            item { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = dimens.bodyTextSp.sp) }
        }
        items(pending) { line ->
            WarningLine(line, dimens.bodyTextSp)
        }
        if (cameras.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.VideocamOff,
                    title = "Nenhuma câmera cadastrada",
                    body = "Use a aba Descobrir para encontrar câmeras na rede.",
                    bodySp = dimens.bodyTextSp,
                )
            }
        }
        items(cameras, key = { it.id }) { camera ->
            CameraCard(camera, dimens) {
                scope.launch {
                    services.cameras.delete(camera.id)
                    reload()
                }
            }
        }
    }
}

/** Card de câmera: ícone, nome, endereço e chip de identidade. */
@Composable
private fun CameraCard(camera: Camera, dimens: Dimens, onRemove: () -> Unit) {
    val hasHost = camera.currentHost != null
    val hasIdentity = camera.identity.uuid != null
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                imageVector = if (hasHost) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                contentDescription = null,
                tint = if (hasHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp),
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(camera.name, color = MaterialTheme.colorScheme.onSurface, fontSize = dimens.bodyTextSp.sp)
                Text(
                    text = camera.currentHost ?: "Endereço não informado",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = (dimens.bodyTextSp - 3).sp,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!hasHost) {
                StatusChip("Sem endereço", MaterialTheme.colorScheme.error)
            }
            StatusChip(
                label = if (hasIdentity) "Identidade anunciada" else "Sem identificador",
                color = if (hasIdentity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CsButton(onClick = onRemove, modifier = Modifier.heightIn(min = dimens.minTargetDp.dp)) {
            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Remover", fontSize = (dimens.bodyTextSp - 2).sp)
        }
    }
}

private fun describe(decision: IpChangeDecision.NeedsConfirmation, cameras: List<Camera>): String {
    val name = cameras.firstOrNull { it.id == decision.cameraId }?.name ?: "Câmera"
    return "Confirmar $name: ${decision.candidateHosts.joinToString()} (${decision.reason})"
}

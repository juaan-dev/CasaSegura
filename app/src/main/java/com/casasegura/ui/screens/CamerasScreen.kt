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
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Verified
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
import com.casasegura.core.credentials.CredentialService
import com.casasegura.core.identity.IpChangeDecision
import com.casasegura.core.identity.Observation
import com.casasegura.core.model.Camera
import com.casasegura.core.model.Credential
import com.casasegura.protocol.CameraVerifier
import com.casasegura.ui.AppServices
import com.casasegura.ui.components.CsButton
import com.casasegura.ui.components.EmptyState
import com.casasegura.ui.components.StatusChip
import com.casasegura.ui.components.WarningLine
import com.casasegura.ui.theme.Dimens
import kotlinx.coroutines.launch

/**
 * Lista de câmeras. Cada card permite verificar a identidade (consulta ONVIF autenticada),
 * escolher a credencial e remover. "Verificar endereços" aplica só as trocas de IP seguras.
 */
@Composable
fun CamerasScreen(services: AppServices, dimens: Dimens, onPlay: (String) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val cameras = remember { mutableStateListOf<Camera>() }
    val credentials = remember { mutableStateListOf<Credential>() }
    val pending = remember { mutableStateListOf<String>() }
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        cameras.clear()
        cameras.addAll(services.cameras.list())
        credentials.clear()
        credentials.addAll(services.credentials.list())
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
                    body = "Use a aba Descobrir para encontrar câmeras na rede ou adicionar por endereço.",
                    bodySp = dimens.bodyTextSp,
                )
            }
        }
        items(cameras, key = { it.id }) { camera ->
            CameraCard(
                camera = camera,
                credentials = credentials,
                dimens = dimens,
                onPlay = { onPlay(camera.id) },
                onVerify = {
                    scope.launch {
                        message = when (val outcome = services.verifier.verify(camera.id)) {
                            is CameraVerifier.Outcome.Confirmed -> "Identidade confirmada: ${outcome.camera.identity.serial ?: "sem serial"}"
                            CameraVerifier.Outcome.NoCredential -> "Associe uma credencial antes de verificar"
                            CameraVerifier.Outcome.NoHost -> "A câmera não tem endereço; descubra-a primeiro"
                            CameraVerifier.Outcome.AuthenticationFailed -> "Credencial recusada pela câmera"
                            is CameraVerifier.Outcome.Failed -> outcome.reason
                            CameraVerifier.Outcome.NotFound -> "Câmera não encontrada"
                        }
                        reload()
                    }
                },
                onNextCredential = {
                    scope.launch {
                        val ids = listOf<String?>(null) + credentials.map { it.id }
                        val next = ids[(ids.indexOf(camera.credentialId) + 1) % ids.size]
                        services.cameras.save(camera.copy(credentialId = next))
                        reload()
                    }
                },
                onRemove = {
                    scope.launch {
                        services.cameras.delete(camera.id)
                        reload()
                    }
                },
            )
        }
    }
}

/** Card de câmera: ícone, nome, endereço, chips de estado e ações. */
@Composable
private fun CameraCard(
    camera: Camera,
    credentials: List<Credential>,
    dimens: Dimens,
    onPlay: () -> Unit,
    onVerify: () -> Unit,
    onNextCredential: () -> Unit,
    onRemove: () -> Unit,
) {
    val hasHost = camera.currentHost != null
    val confirmed = camera.connection == com.casasegura.core.model.ConnectionMethod.ONVIF && camera.identity.serial != null
    val credentialAlias = credentials.firstOrNull { it.id == camera.credentialId }?.alias
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
            if (!hasHost) StatusChip("Sem endereço", MaterialTheme.colorScheme.error)
            if (confirmed) {
                StatusChip("Identidade confirmada", MaterialTheme.colorScheme.primary)
            } else if (camera.identity.uuid != null) {
                StatusChip("Anunciada, não confirmada", MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                StatusChip("Sem identificador", MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        CsButton(onClick = onNextCredential, modifier = Modifier.heightIn(min = dimens.minTargetDp.dp)) {
            Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Credencial: ${credentialAlias ?: "nenhuma"}",
                fontSize = (dimens.bodyTextSp - 2).sp,
                maxLines = 1,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CsButton(onClick = onPlay, enabled = hasHost, modifier = Modifier.heightIn(min = dimens.minTargetDp.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Assistir", fontSize = (dimens.bodyTextSp - 2).sp, maxLines = 1)
            }
            CsButton(onClick = onVerify, modifier = Modifier.heightIn(min = dimens.minTargetDp.dp)) {
                Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Verificar", fontSize = (dimens.bodyTextSp - 2).sp, maxLines = 1)
            }
            CsButton(onClick = onRemove, modifier = Modifier.heightIn(min = dimens.minTargetDp.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Remover", fontSize = (dimens.bodyTextSp - 2).sp, maxLines = 1)
            }
        }
    }
}

private fun describe(decision: IpChangeDecision.NeedsConfirmation, cameras: List<Camera>): String {
    val name = cameras.firstOrNull { it.id == decision.cameraId }?.name ?: "Câmera"
    return "Confirmar $name: ${decision.candidateHosts.joinToString()} (${decision.reason})"
}

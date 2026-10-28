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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casasegura.core.identity.IpChangeDecision
import com.casasegura.core.identity.Observation
import com.casasegura.core.model.Camera
import com.casasegura.ui.AppServices
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
            Button(
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
                Text(if (checking) "Verificando..." else "Verificar endereços", fontSize = dimens.bodyTextSp.sp)
            }
        }
        message?.let { text ->
            item { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = dimens.bodyTextSp.sp) }
        }
        items(pending) { line ->
            Text(line, color = MaterialTheme.colorScheme.primary, fontSize = (dimens.bodyTextSp - 2).sp)
        }
        if (cameras.isEmpty()) {
            item {
                Text(
                    text = "Nenhuma câmera cadastrada. Use a aba Descobrir para adicionar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = dimens.bodyTextSp.sp,
                )
            }
        }
        items(cameras, key = { it.id }) { camera ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(camera.name, color = MaterialTheme.colorScheme.onBackground, fontSize = dimens.bodyTextSp.sp)
                Text(
                    text = "Endereço: ${camera.currentHost ?: "não informado"} · Conexão: ${camera.connection.name} · " +
                        "Identidade: ${if (camera.identity.uuid != null) "UUID anunciado" else "sem identificador"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = (dimens.bodyTextSp - 2).sp,
                )
                Button(
                    onClick = {
                        scope.launch {
                            services.cameras.delete(camera.id)
                            reload()
                        }
                    },
                    modifier = Modifier.heightIn(min = dimens.minTargetDp.dp),
                ) {
                    Text("Remover", fontSize = dimens.bodyTextSp.sp)
                }
            }
        }
    }
}

private fun describe(decision: IpChangeDecision.NeedsConfirmation, cameras: List<Camera>): String {
    val name = cameras.firstOrNull { it.id == decision.cameraId }?.name ?: "Câmera"
    return "Confirmar $name: ${decision.candidateHosts.joinToString()} (${decision.reason})"
}

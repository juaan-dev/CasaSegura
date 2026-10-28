package com.casasegura.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.casasegura.ui.AppServices
import com.casasegura.ui.components.CsButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casasegura.core.credentials.CredentialService
import com.casasegura.core.model.Credential
import com.casasegura.core.security.SecretMasker
import com.casasegura.ui.theme.Dimens
import kotlinx.coroutines.launch

/**
 * Lista e cadastro de credenciais. A senha nunca é exibida: só a máscara.
 * A exclusão ainda não verifica câmeras, porque o repositório de câmeras é da Fase 3.
 */
@Composable
fun CredentialsScreen(services: AppServices, dimens: Dimens) {
    val service = services.credentials
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val credentials = remember { mutableStateListOf<Credential>() }
    var alias by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        credentials.clear()
        credentials.addAll(service.list())
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(dimens.minTargetDp.dp / 2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Credenciais",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = (dimens.bodyTextSp + 10).sp,
            )
        }
        item {
            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                label = { Text("Nome da credencial") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTargetDp.dp),
            )
        }
        item {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuário") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTargetDp.dp),
            )
        }
        item {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTargetDp.dp),
            )
        }
        item {
            CsButton(
                onClick = {
                    scope.launch {
                        when (val result = service.create(alias, username, password)) {
                            is CredentialService.Result.Saved -> {
                                alias = ""
                                username = ""
                                password = ""
                                message = "Credencial salva"
                                reload()
                            }
                            is CredentialService.Result.Invalid -> message = result.message
                            is CredentialService.Result.NotFound -> message = "Credencial não encontrada"
                        }
                    }
                },
                modifier = Modifier.heightIn(min = dimens.minTargetDp.dp),
            ) {
                Text("Salvar credencial", fontSize = dimens.bodyTextSp.sp)
            }
        }
        message?.let { text ->
            item {
                Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = dimens.bodyTextSp.sp)
            }
        }
        items(credentials, key = { it.id }) { credential ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = credential.alias,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = dimens.bodyTextSp.sp,
                )
                Text(
                    text = "Usuário: ${credential.username} · Senha: ${SecretMasker.mask(null)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = (dimens.bodyTextSp - 2).sp,
                )
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    CsButton(
                        onClick = {
                            scope.launch {
                                val usingIds = services.cameras.list()
                                    .filter { it.credentialId == credential.id }
                                    .map { it.id }
                                // Sem confirmação, a credencial em uso não é removida: o serviço devolve as câmeras afetadas.
                                when (val result = service.delete(credential.id, usingIds, confirmed = pendingDeleteId == credential.id)) {
                                    is CredentialService.DeleteResult.Deleted -> {
                                        pendingDeleteId = null
                                        message = "Credencial excluída"
                                    }
                                    is CredentialService.DeleteResult.InUse -> {
                                        pendingDeleteId = credential.id
                                        message = "Em uso por ${result.cameraIds.size} câmera(s). Toque em Confirmar para excluir."
                                    }
                                    is CredentialService.DeleteResult.NotFound -> message = "Credencial não encontrada"
                                }
                                reload()
                            }
                        },
                        modifier = Modifier.heightIn(min = dimens.minTargetDp.dp),
                    ) {
                        Text(
                            if (pendingDeleteId == credential.id) "Confirmar exclusão" else "Excluir",
                            fontSize = dimens.bodyTextSp.sp,
                        )
                    }
                }
            }
        }
    }
}

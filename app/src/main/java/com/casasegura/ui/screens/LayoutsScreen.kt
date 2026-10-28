package com.casasegura.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.casasegura.ui.components.CsButton
import com.casasegura.ui.components.StepperRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.casasegura.core.layouts.LayoutService
import com.casasegura.core.model.Camera
import com.casasegura.core.model.Layout
import com.casasegura.ui.AppServices
import com.casasegura.ui.theme.Dimens
import kotlinx.coroutines.launch

/**
 * Visualizações (mosaicos). Tocar em um slot percorre as câmeras cadastradas e o slot vazio.
 * A capacidade máxima segue o perfil de tela.
 */
@Composable
fun LayoutsScreen(services: AppServices, dimens: Dimens) {
    val scope = rememberCoroutineScope()
    val layouts = remember { mutableStateListOf<Layout>() }
    val cameras = remember { mutableStateListOf<Camera>() }
    var name by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf(2) }
    var columns by remember { mutableStateOf(2) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        layouts.clear()
        layouts.addAll(services.layouts.list())
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
                text = "Visualizações",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = (dimens.bodyTextSp + 10).sp,
            )
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome da visualização") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTargetDp.dp),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StepperRow(
                    label = "Linhas",
                    value = rows,
                    minTargetDp = dimens.minTargetDp,
                    bodySp = dimens.bodyTextSp,
                    onDecrement = { rows = (rows - 1).coerceAtLeast(1) },
                    onIncrement = { rows = (rows + 1).coerceAtMost(8) },
                )
                StepperRow(
                    label = "Colunas",
                    value = columns,
                    minTargetDp = dimens.minTargetDp,
                    bodySp = dimens.bodyTextSp,
                    onDecrement = { columns = (columns - 1).coerceAtLeast(1) },
                    onIncrement = { columns = (columns + 1).coerceAtMost(8) },
                )
                Text(
                    text = "Matriz $rows × $columns = ${rows * columns} células",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = (dimens.bodyTextSp - 2).sp,
                )
            }
        }
        item {
            CsButton(
                onClick = {
                    scope.launch {
                        val result = services.layouts.create(name, rows, columns, dimens.maxLayoutCells)
                        message = when (result) {
                            is LayoutService.Result.Saved -> {
                                name = ""
                                "Visualização criada"
                            }
                            is LayoutService.Result.Invalid -> result.message
                            else -> "Não foi possível criar"
                        }
                        reload()
                    }
                },
                modifier = Modifier.heightIn(min = dimens.minTargetDp.dp),
            ) {
                Text("Criar visualização (até ${dimens.maxLayoutCells} células)", fontSize = dimens.bodyTextSp.sp)
            }
        }
        message?.let { text ->
            item { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = dimens.bodyTextSp.sp) }
        }
        items(layouts, key = { it.id }) { layout ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = layout.name + (if (layout.isDefault) " · padrão" else "") + (if (layout.favorite) " · ★" else ""),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = dimens.bodyTextSp.sp,
                )
                Grid(layout, cameras, dimens) { slotIndex ->
                    scope.launch {
                        val current = layout.slots.first { it.index == slotIndex }.cameraId
                        val ids = listOf<String?>(null) + cameras.map { it.id }
                        val next = ids[(ids.indexOf(current) + 1) % ids.size]
                        services.layouts.assign(layout.id, slotIndex, next)
                        reload()
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepCsButton("Padrão", dimens) {
                        scope.launch { services.layouts.setDefault(layout.id); reload() }
                    }
                    StepCsButton("Favorita", dimens) {
                        scope.launch { services.layouts.toggleFavorite(layout.id); reload() }
                    }
                    StepCsButton("Excluir", dimens) {
                        scope.launch { services.layouts.delete(layout.id); reload() }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCsButton(label: String, dimens: Dimens, onClick: () -> Unit) {
    CsButton(onClick = onClick, modifier = Modifier.heightIn(min = dimens.minTargetDp.dp)) {
        Text(label, fontSize = (dimens.bodyTextSp - 2).sp, maxLines = 1)
    }
}

/** Pré-visualização da matriz. Cada célula mostra a câmera atribuída ou "vazio". */
@Composable
private fun Grid(layout: Layout, cameras: List<Camera>, dimens: Dimens, onSlotClick: (Int) -> Unit) {
    val names = cameras.associate { it.id to it.name }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        for (row in 0 until layout.rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until layout.columns) {
                    val index = row * layout.columns + col
                    val slot = layout.slots.first { it.index == index }
                    Box(
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(16f / 9f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSlotClick(index) },
                    ) {
                        val assigned = slot.cameraId != null
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (assigned) Icons.Rounded.Videocam else Icons.Rounded.Add,
                                contentDescription = null,
                                tint = if (assigned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                text = slot.cameraId?.let { names[it] ?: "?" } ?: "Vazio",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = (dimens.bodyTextSp - 6).sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

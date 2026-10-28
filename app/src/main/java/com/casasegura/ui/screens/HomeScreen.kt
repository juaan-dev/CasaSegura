package com.casasegura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casasegura.core.model.Camera
import com.casasegura.core.model.Layout
import com.casasegura.ui.AppServices
import com.casasegura.ui.components.EmptyState
import com.casasegura.ui.components.StatusChip
import com.casasegura.ui.theme.Dimens

/**
 * Tela inicial: a visualização padrão em modo leitura, as favoritas e um resumo das câmeras.
 * Só leitura; a edição fica em Visualizações.
 */
@Composable
fun HomeScreen(services: AppServices, dimens: Dimens) {
    val layouts = remember { mutableStateListOf<Layout>() }
    val cameras = remember { mutableStateListOf<Camera>() }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        layouts.clear()
        layouts.addAll(services.layouts.list())
        cameras.clear()
        cameras.addAll(services.cameras.list())
        loaded = true
    }

    val defaultLayout = layouts.firstOrNull { it.isDefault }
    val favorites = layouts.filter { it.favorite && it.id != defaultLayout?.id }
    val withoutHost = cameras.count { it.currentHost == null }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(dimens.minTargetDp.dp / 2),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Início",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = (dimens.bodyTextSp + 10).sp,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip("${cameras.size} câmera(s)", MaterialTheme.colorScheme.primary)
                if (withoutHost > 0) {
                    StatusChip("$withoutHost sem endereço", MaterialTheme.colorScheme.error)
                }
            }
        }

        if (loaded && defaultLayout == null) {
            item {
                EmptyState(
                    icon = Icons.Rounded.GridView,
                    title = "Nenhuma visualização padrão",
                    body = "Crie uma em Visualizações e marque como padrão para vê-la aqui.",
                    bodySp = dimens.bodyTextSp,
                )
            }
        }

        defaultLayout?.let { layout ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = layout.name,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = (dimens.bodyTextSp + 2).sp,
                        )
                        StatusChip("Padrão", MaterialTheme.colorScheme.primary)
                    }
                    LayoutGrid(layout, cameras, dimens)
                }
            }
        }

        if (favorites.isNotEmpty()) {
            item {
                Text(
                    text = "Favoritas",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = (dimens.bodyTextSp - 2).sp,
                )
            }
            items(favorites, key = { it.id }) { layout ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp),
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "${layout.name} · ${layout.rows} × ${layout.columns}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = dimens.bodyTextSp.sp,
                    )
                }
            }
        }
    }
}

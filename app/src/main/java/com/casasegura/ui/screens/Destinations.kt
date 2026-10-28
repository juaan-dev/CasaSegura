package com.casasegura.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casasegura.ui.AppServices
import com.casasegura.ui.theme.Dimens

/** Destinos principais. Os rótulos seguem o escopo funcional (seção 2 do plano). */
enum class Destination(val label: String, val icon: ImageVector) {
    HOME("Início", Icons.Rounded.Home),
    DISCOVERY("Descobrir", Icons.Rounded.Search),
    CAMERAS("Câmeras", Icons.Rounded.Videocam),
    CREDENTIALS("Credenciais", Icons.Rounded.Lock),
    LAYOUTS("Visualizações", Icons.Rounded.GridView),
    SETTINGS("Configurações", Icons.Rounded.Settings),
}

@Composable
fun DestinationContent(destination: Destination, dimens: Dimens, services: AppServices, onPlay: (String) -> Unit = {}) {
    if (destination == Destination.CREDENTIALS) {
        CredentialsScreen(services = services, dimens = dimens)
        return
    }
    if (destination == Destination.HOME) {
        HomeScreen(services = services, dimens = dimens)
        return
    }
    if (destination == Destination.DISCOVERY) {
        DiscoveryScreen(services = services, dimens = dimens)
        return
    }
    if (destination == Destination.CAMERAS) {
        CamerasScreen(services = services, dimens = dimens, onPlay = onPlay)
        return
    }
    if (destination == Destination.LAYOUTS) {
        LayoutsScreen(services = services, dimens = dimens)
        return
    }

    val (title, body) = when (destination) {
        Destination.HOME -> "Início" to "Visualização padrão e acesso rápido às listas salvas. Vazio até a Fase 6."
        else -> "Configurações" to "Preferências do app e diagnósticos. Fase 7."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimens.minTargetDp.dp / 2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = (dimens.bodyTextSp + 10).sp,
        )
        Text(
            text = body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = dimens.bodyTextSp.sp,
        )
    }
}

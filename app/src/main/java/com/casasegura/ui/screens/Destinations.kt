package com.casasegura.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casasegura.ui.AppServices
import com.casasegura.ui.theme.Dimens

/** Destinos principais. Os rótulos seguem o escopo funcional (seção 2 do plano). */
enum class Destination(val label: String) {
    HOME("Início"),
    DISCOVERY("Descobrir"),
    CAMERAS("Câmeras"),
    CREDENTIALS("Credenciais"),
    LAYOUTS("Visualizações"),
    SETTINGS("Configurações"),
}

@Composable
fun DestinationContent(destination: Destination, dimens: Dimens, services: AppServices) {
    if (destination == Destination.CREDENTIALS) {
        CredentialsScreen(service = services.credentials, dimens = dimens)
        return
    }
    if (destination == Destination.DISCOVERY) {
        DiscoveryScreen(services = services, dimens = dimens)
        return
    }
    if (destination == Destination.CAMERAS) {
        CamerasScreen(services = services, dimens = dimens)
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

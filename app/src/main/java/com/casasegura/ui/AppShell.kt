package com.casasegura.ui

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casasegura.core.common.ScreenProfile
import com.casasegura.ui.screens.Destination
import com.casasegura.ui.screens.DestinationContent
import com.casasegura.ui.theme.Dimens

/**
 * Casca de navegação responsiva. O perfil vem do tipo de dispositivo (TV) ou da largura (celular/tablet):
 * - celular: conteúdo em coluna e barra de navegação inferior;
 * - tablet e TV: menu lateral à esquerda, com a largura do protótipo (232 e 320 dp).
 */
@Composable
fun AppShell(services: AppServices) {
    val context = LocalContext.current
    val isTelevision = remember { isTelevisionDevice(context) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        val profile = ScreenProfile.resolve(isTelevision = isTelevision, widthDp = maxWidth.value.toInt())
        val dimens = Dimens.of(profile)
        var current by remember { mutableStateOf(Destination.HOME) }

        if (profile == ScreenProfile.PHONE) {
            PhoneLayout(current, dimens, services) { current = it }
        } else {
            SideRailLayout(current, dimens, services) { current = it }
        }
    }
}

/** Tipo de dispositivo informado pelo sistema: televisor ou não. */
private fun isTelevisionDevice(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

@Composable
private fun PhoneLayout(
    current: Destination,
    dimens: Dimens,
    services: AppServices,
    onSelect: (Destination) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            DestinationContent(current, dimens, services)
        }
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Destination.entries.forEach { destination ->
                    NavItem(
                        destination = destination,
                        selected = destination == current,
                        dimens = dimens,
                        compact = true,
                        modifier = Modifier.weight(1f),
                    ) { onSelect(destination) }
                }
            }
        }
    }
}

@Composable
private fun SideRailLayout(
    current: Destination,
    dimens: Dimens,
    services: AppServices,
    onSelect: (Destination) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxHeight()
                .width(dimens.railWidthDp.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = "CasaSegura",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = (dimens.bodyTextSp - 2).sp,
                    )
                }
                Destination.entries.forEach { destination ->
                    NavItem(
                        destination = destination,
                        selected = destination == current,
                        dimens = dimens,
                        compact = false,
                        modifier = Modifier.fillMaxWidth(),
                    ) { onSelect(destination) }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = (dimens.maxContentWidthDp ?: Int.MAX_VALUE).dp)
                    .fillMaxWidth(),
            ) {
                DestinationContent(current, dimens, services)
            }
        }
    }
}

/**
 * Item de navegação com ícone e rótulo. Selecionado recebe fundo de destaque; com foco, anel claro.
 * No celular o rótulo fica embaixo do ícone, para caber nas cinco ou seis colunas da barra.
 */
@Composable
private fun NavItem(
    destination: Destination,
    selected: Boolean,
    dimens: Dimens,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val background = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val ring = if (focused) BorderStroke(3.dp, MaterialTheme.colorScheme.onBackground) else null

    Surface(
        shape = if (compact) RoundedCornerShape(14.dp) else RoundedCornerShape(percent = 50),
        color = background,
        contentColor = content,
        border = ring,
        modifier = modifier
            .heightIn(min = dimens.minTargetDp.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
    ) {
        if (compact) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            ) {
                Icon(destination.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Text(destination.label, fontSize = 11.sp, maxLines = 1)
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Icon(destination.icon, contentDescription = null, modifier = Modifier.size(dimens.bodyTextSp.dp))
                Text(destination.label, fontSize = dimens.bodyTextSp.sp, maxLines = 1)
            }
        }
    }
}

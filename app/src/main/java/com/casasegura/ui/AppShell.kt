package com.casasegura.ui

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casasegura.core.common.ScreenProfile
import com.casasegura.ui.screens.Destination
import com.casasegura.ui.screens.DestinationContent
import com.casasegura.ui.screens.PlaybackScreen
import com.casasegura.ui.theme.Dimens

/** Largura do menu recolhido: só ícones, com espaço para o alvo de toque. */
private val COLLAPSED_RAIL_WIDTH: Dp = 88.dp

/**
 * Casca de navegação responsiva. O perfil vem do tipo de dispositivo (TV) ou da largura (celular/tablet):
 * - celular: conteúdo em coluna e barra de navegação inferior;
 * - tablet e TV: menu lateral à esquerda, que pode ser recolhido para só ícones.
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
        var playingCameraId by remember { mutableStateOf<String?>(null) }

        if (profile == ScreenProfile.PHONE) {
            PhoneLayout(current, dimens, services, onPlay = { playingCameraId = it }) { current = it }
        } else {
            SideRailLayout(current, dimens, services, onPlay = { playingCameraId = it }) { current = it }
        }

        // Reprodução cobre a tela inteira; sair dela fecha o player.
        playingCameraId?.let { cameraId ->
            Box(modifier = Modifier.fillMaxSize()) {
                PlaybackScreen(cameraId = cameraId, services = services, dimens = dimens) { playingCameraId = null }
            }
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
    onPlay: (String) -> Unit,
    onSelect: (Destination) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            DestinationContent(current, dimens, services, onPlay)
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
                        style = NavStyle.BOTTOM,
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
    onPlay: (String) -> Unit,
    onSelect: (Destination) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    val railWidth by animateDpAsState(
        targetValue = if (expanded) dimens.railWidthDp.dp else COLLAPSED_RAIL_WIDTH,
        animationSpec = tween(durationMillis = 240),
        label = "rail-width",
    )
    val borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Row(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxHeight()
                .width(railWidth)
                // Borda vertical na lateral direita, desenhada por cima do fundo para aparecer.
                .drawWithContent {
                    drawContent()
                    val stroke = 2.dp.toPx()
                    drawLine(
                        color = borderColor,
                        start = Offset(size.width - stroke / 2, 0f),
                        end = Offset(size.width - stroke / 2, size.height),
                        strokeWidth = stroke,
                    )
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RailHeader(expanded = expanded, dimens = dimens) { expanded = !expanded }
                Spacer(Modifier.height(8.dp))
                Destination.entries.forEach { destination ->
                    NavItem(
                        destination = destination,
                        selected = destination == current,
                        dimens = dimens,
                        style = if (expanded) NavStyle.RAIL_EXPANDED else NavStyle.RAIL_COLLAPSED,
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
                DestinationContent(current, dimens, services, onPlay)
            }
        }
    }
}

/**
 * Topo do menu: logo e nome quando expandido, só o logo quando recolhido.
 * Clicar alterna entre os dois estados; a seta indica a ação.
 */
@Composable
private fun RailHeader(expanded: Boolean, dimens: Dimens, onToggle: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val ring = if (focused) BorderStroke(3.dp, MaterialTheme.colorScheme.onBackground) else null
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = ring,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTargetDp.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onToggle),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.spacedBy(10.dp) else Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
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
            if (expanded) {
                Text(
                    text = "CasaSegura",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (dimens.bodyTextSp - 2).sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "Recolher menu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private enum class NavStyle { BOTTOM, RAIL_EXPANDED, RAIL_COLLAPSED }

/**
 * Item de navegação. Selecionado recebe fundo de destaque; com foco, anel claro.
 * Na barra inferior o rótulo fica sob o ícone; no menu expandido, ao lado; recolhido, só o ícone.
 */
@Composable
private fun NavItem(
    destination: Destination,
    selected: Boolean,
    dimens: Dimens,
    style: NavStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val ring = if (focused) BorderStroke(3.dp, MaterialTheme.colorScheme.onBackground) else null

    Surface(
        shape = if (style == NavStyle.BOTTOM) RoundedCornerShape(14.dp) else RoundedCornerShape(percent = 50),
        color = background,
        contentColor = content,
        border = ring,
        modifier = modifier
            .heightIn(min = dimens.minTargetDp.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
    ) {
        when (style) {
            NavStyle.BOTTOM -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            ) {
                Icon(destination.icon, contentDescription = destination.label, modifier = Modifier.size(24.dp))
                Text(destination.label, fontSize = 11.sp, maxLines = 1)
            }
            NavStyle.RAIL_EXPANDED -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Icon(destination.icon, contentDescription = null, modifier = Modifier.size(dimens.bodyTextSp.dp))
                Text(destination.label, fontSize = dimens.bodyTextSp.sp, maxLines = 1)
            }
            NavStyle.RAIL_COLLAPSED -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            ) {
                Icon(destination.icon, contentDescription = destination.label, modifier = Modifier.size(dimens.bodyTextSp.dp))
            }
        }
    }
}

package com.casasegura.ui

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.casasegura.core.common.ScreenProfile
import com.casasegura.core.credentials.CredentialService
import com.casasegura.ui.screens.Destination
import com.casasegura.ui.screens.DestinationContent
import com.casasegura.ui.theme.Dimens

/**
 * Casca de navegação responsiva. Escolhe o perfil pela largura disponível:
 * - celular: conteúdo em coluna e barra de navegação inferior;
 * - tablet e TV: menu lateral à esquerda, com largura do protótipo (232 e 320 dp).
 */
@Composable
fun AppShell(credentialService: CredentialService) {
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
            PhoneLayout(current, dimens, credentialService) { current = it }
        } else {
            SideRailLayout(current, dimens, credentialService) { current = it }
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
    credentialService: CredentialService,
    onSelect: (Destination) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            DestinationContent(current, dimens, credentialService)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Destination.entries.forEach { destination ->
                NavButton(
                    label = destination.label,
                    selected = destination == current,
                    dimens = dimens,
                    modifier = Modifier.weight(1f),
                ) { onSelect(destination) }
            }
        }
    }
}

@Composable
private fun SideRailLayout(
    current: Destination,
    dimens: Dimens,
    credentialService: CredentialService,
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
                Text(
                    text = "CasaSegura",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = (dimens.bodyTextSp - 2).sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Destination.entries.forEach { destination ->
                    NavButton(
                        label = destination.label,
                        selected = destination == current,
                        dimens = dimens,
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
                DestinationContent(current, dimens, credentialService)
            }
        }
    }
}

@Composable
private fun NavButton(
    label: String,
    selected: Boolean,
    dimens: Dimens,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = dimens.minTargetDp.dp),
        colors = if (selected) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
    ) {
        Text(text = label, fontSize = dimens.bodyTextSp.sp)
    }
}

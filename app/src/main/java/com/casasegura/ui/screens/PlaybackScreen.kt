package com.casasegura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import com.casasegura.protocol.StreamResolver
import com.casasegura.ui.AppServices
import com.casasegura.ui.components.CsButton
import com.casasegura.ui.theme.Dimens

/** Estado da reprodução. A URL RTSP (com credenciais) fica só aqui, em memória. */
private sealed interface PlaybackState {
    data object Resolving : PlaybackState
    data class Ready(val url: String) : PlaybackState
    data class Error(val message: String) : PlaybackState
}

/**
 * Reprodução de uma câmera em tela cheia, via RTSP (Media3). Não mostra nem registra a URL.
 * Sair da tela libera o player, para não manter o fluxo aberto.
 */
@Composable
fun PlaybackScreen(cameraId: String, services: AppServices, dimens: Dimens, onClose: () -> Unit) {
    var state by remember { mutableStateOf<PlaybackState>(PlaybackState.Resolving) }

    LaunchedEffect(cameraId) {
        state = when (val outcome = services.streams.resolve(cameraId)) {
            is StreamResolver.Outcome.Ready -> PlaybackState.Ready(outcome.url)
            StreamResolver.Outcome.NotFound -> PlaybackState.Error("Câmera não encontrada")
            StreamResolver.Outcome.NoHost -> PlaybackState.Error("A câmera não tem endereço. Descubra-a primeiro")
            StreamResolver.Outcome.NoCredential -> PlaybackState.Error("Associe uma credencial para assistir")
            StreamResolver.Outcome.AuthenticationFailed -> PlaybackState.Error("Credencial recusada pela câmera")
            is StreamResolver.Outcome.Failed -> PlaybackState.Error(outcome.reason)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
            .padding(dimens.minTargetDp.dp / 2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CsButton(onClick = onClose, modifier = Modifier.heightIn(min = dimens.minTargetDp.dp)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("Voltar", fontSize = dimens.bodyTextSp.sp)
            }
        }
        when (val current = state) {
            PlaybackState.Resolving -> StatusText("Obtendo o fluxo da câmera...", dimens.bodyTextSp)
            is PlaybackState.Error -> StatusText(current.message, dimens.bodyTextSp)
            is PlaybackState.Ready -> RtspPlayer(current.url)
        }
    }
}

@Composable
private fun StatusText(text: String, bodySp: Int) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = bodySp.sp,
        modifier = Modifier.padding(16.dp),
    )
}

@Composable
private fun RtspPlayer(url: String) {
    val context = LocalContext.current
    var playbackError by remember { mutableStateOf<String?>(null) }
    val player = remember(url) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(RtspMediaSource.Factory())
            .build()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // Só o código do erro: a mensagem bruta pode conter a URL.
                playbackError = "Falha na reprodução (${error.errorCodeName})"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                    }
                },
            )
        }
        playbackError?.let { StatusText(it, 18) }
    }
}

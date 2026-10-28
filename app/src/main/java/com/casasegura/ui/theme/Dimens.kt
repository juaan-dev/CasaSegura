package com.casasegura.ui.theme

import com.casasegura.core.common.ScreenProfile

/**
 * Dimensões por perfil de tela, derivadas do protótipo (CasaSegura.dc.html, linha 381 e 384-386).
 * Texto em sp; alvos e espaçamentos em dp.
 */
data class Dimens(
    val bodyTextSp: Int,
    val minTargetDp: Int,
    val railWidthDp: Int,
    val maxContentWidthDp: Int?,
    val cardColumns: Int,
    /** Capacidade de células de um mosaico neste perfil. Provisória até os testes de desempenho (Fase 7). */
    val maxLayoutCells: Int,
) {
    companion object {
        fun of(profile: ScreenProfile): Dimens = when (profile) {
            ScreenProfile.PHONE -> Dimens(
                bodyTextSp = 16,
                minTargetDp = 44,
                railWidthDp = 0,
                maxContentWidthDp = null,
                cardColumns = 1,
                maxLayoutCells = 4,
            )
            ScreenProfile.TABLET -> Dimens(
                bodyTextSp = 18,
                minTargetDp = 48,
                railWidthDp = 232,
                maxContentWidthDp = 1280,
                cardColumns = 2,
                maxLayoutCells = 9,
            )
            ScreenProfile.TV -> Dimens(
                bodyTextSp = 26,
                minTargetDp = 64,
                railWidthDp = 320,
                maxContentWidthDp = 1680,
                cardColumns = 3,
                maxLayoutCells = 16,
            )
        }
    }
}

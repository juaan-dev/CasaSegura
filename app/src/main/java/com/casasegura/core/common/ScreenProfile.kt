package com.casasegura.core.common

/**
 * Perfil de tela escolhido pela largura disponível (dp), não pelo tipo de aparelho.
 * Os limites seguem o protótipo em "CasaSegura - Protótipo responsivo" (linha 375) e a seção 1 do plano.
 */
enum class ScreenProfile {
    PHONE,
    TABLET,
    TV;

    companion object {
        const val PHONE_MAX_WIDTH_DP = 600
        const val TABLET_MAX_WIDTH_DP = 1200

        fun fromWidthDp(widthDp: Int): ScreenProfile = when {
            widthDp < PHONE_MAX_WIDTH_DP -> PHONE
            widthDp < TABLET_MAX_WIDTH_DP -> TABLET
            else -> TV
        }
    }
}

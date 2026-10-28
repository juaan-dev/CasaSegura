package com.casasegura.core.common

/**
 * Perfil de tela. A TV é detectada pelo tipo de dispositivo (UiModeManager), não pela largura:
 * uma TV Full HD com densidade 320 dpi tem 960 dp e seria tratada como tablet.
 * Celular e tablet são distinguidos pela largura disponível.
 * Os limites seguem o protótipo em "CasaSegura - Protótipo responsivo" (linha 375) e a seção 1 do plano.
 */
enum class ScreenProfile {
    PHONE,
    TABLET,
    TV;

    companion object {
        const val PHONE_MAX_WIDTH_DP = 600
        const val TABLET_MAX_WIDTH_DP = 1200

        fun resolve(isTelevision: Boolean, widthDp: Int): ScreenProfile = when {
            isTelevision -> TV
            widthDp < PHONE_MAX_WIDTH_DP -> PHONE
            else -> TABLET
        }
    }
}

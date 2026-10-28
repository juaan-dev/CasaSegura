package com.casasegura.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenProfileTest {

    @Test
    fun `largura abaixo de 600dp em celular e celular`() {
        assertEquals(ScreenProfile.PHONE, ScreenProfile.resolve(isTelevision = false, widthDp = 360))
        assertEquals(ScreenProfile.PHONE, ScreenProfile.resolve(isTelevision = false, widthDp = 599))
    }

    @Test
    fun `largura de 600dp ou mais em aparelho nao televisor e tablet`() {
        assertEquals(ScreenProfile.TABLET, ScreenProfile.resolve(isTelevision = false, widthDp = 600))
        assertEquals(ScreenProfile.TABLET, ScreenProfile.resolve(isTelevision = false, widthDp = 960))
    }

    @Test
    fun `televisor usa perfil TV independente da largura`() {
        // Caso real do emulador: 1920 px a 320 dpi = 960 dp, que antes caía como tablet.
        assertEquals(ScreenProfile.TV, ScreenProfile.resolve(isTelevision = true, widthDp = 960))
        assertEquals(ScreenProfile.TV, ScreenProfile.resolve(isTelevision = true, widthDp = 1920))
    }
}

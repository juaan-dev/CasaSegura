package com.casasegura.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenProfileTest {

    @Test
    fun `largura abaixo de 600dp e celular`() {
        assertEquals(ScreenProfile.PHONE, ScreenProfile.fromWidthDp(360))
        assertEquals(ScreenProfile.PHONE, ScreenProfile.fromWidthDp(599))
    }

    @Test
    fun `de 600 a 1199dp e tablet`() {
        assertEquals(ScreenProfile.TABLET, ScreenProfile.fromWidthDp(600))
        assertEquals(ScreenProfile.TABLET, ScreenProfile.fromWidthDp(1199))
    }

    @Test
    fun `a partir de 1200dp e TV`() {
        assertEquals(ScreenProfile.TV, ScreenProfile.fromWidthDp(1200))
        assertEquals(ScreenProfile.TV, ScreenProfile.fromWidthDp(1920))
    }
}

package com.casasegura.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutRulesTest {

    @Test
    fun `aceita matriz assimetrica dentro do limite`() {
        // 2 linhas x 3 colunas = 6 células; não fica preso a 1, 4, 9 ou 16
        assertEquals(LayoutRules.Result.Valid, LayoutRules.validateSize(rows = 2, columns = 3, maxCells = 9))
    }

    @Test
    fun `rejeita matriz acima da capacidade do dispositivo`() {
        assertEquals(
            LayoutRules.Result.TooLarge(cells = 16, maxCells = 9),
            LayoutRules.validateSize(rows = 4, columns = 4, maxCells = 9),
        )
    }

    @Test
    fun `rejeita matriz sem linhas ou colunas`() {
        val result = LayoutRules.validateSize(rows = 0, columns = 3, maxCells = 9)
        assertTrue(result is LayoutRules.Result.Empty)
    }

    @Test
    fun `cria slots vazios na ordem da matriz`() {
        val slots = LayoutRules.emptySlots(rows = 2, columns = 3)
        assertEquals(6, slots.size)
        assertEquals((0 until 6).toList(), slots.map { it.index })
        assertTrue(slots.all { it.cameraId == null && it.stream == StreamChoice.AUTO })
    }
}

package com.casasegura.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LayoutTest {

    @Test
    fun `layout com slots compativeis com a matriz e valido`() {
        val layout = Layout(
            id = "l1",
            name = "Entrada e garagem",
            rows = 1,
            columns = 2,
            slots = LayoutRules.emptySlots(rows = 1, columns = 2),
        )
        assertEquals(2, layout.cellCount)
    }

    @Test
    fun `layout com numero de slots diferente da matriz e recusado`() {
        assertThrows(IllegalArgumentException::class.java) {
            Layout(
                id = "l2",
                name = "invalido",
                rows = 2,
                columns = 2,
                slots = LayoutRules.emptySlots(rows = 1, columns = 3),
            )
        }
    }
}

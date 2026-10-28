package com.casasegura.core.model

/**
 * Regras de criação de visualizações. Não fixa quantidade de slots em 1, 4, 9 ou 16:
 * apenas valida o limite de capacidade do dispositivo (seção 2.4 do plano).
 */
object LayoutRules {

    sealed interface Result {
        data object Valid : Result
        data class TooLarge(val cells: Int, val maxCells: Int) : Result
        data class Empty(val reason: String) : Result
    }

    fun validateSize(rows: Int, columns: Int, maxCells: Int): Result {
        if (rows <= 0 || columns <= 0) {
            return Result.Empty("A matriz precisa ter pelo menos 1 linha e 1 coluna")
        }
        val cells = rows * columns
        return if (cells > maxCells) Result.TooLarge(cells, maxCells) else Result.Valid
    }

    /** Cria slots vazios para a matriz informada. */
    fun emptySlots(rows: Int, columns: Int): List<LayoutSlot> =
        List(rows * columns) { index -> LayoutSlot(index = index, cameraId = null) }
}

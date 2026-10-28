package com.casasegura.core.model

/**
 * Visualização (mosaico) com matriz lógica de linhas × colunas.
 * Cada célula é um slot que pode ter uma câmera ou ficar vazio (seção 2.4 do plano).
 */
data class Layout(
    val id: String,
    val name: String,
    val rows: Int,
    val columns: Int,
    val slots: List<LayoutSlot>,
) {
    init {
        require(rows > 0 && columns > 0) { "Matriz precisa ter pelo menos 1 linha e 1 coluna" }
        require(slots.size == rows * columns) { "Número de slots (${slots.size}) difere de ${rows * columns}" }
    }

    val cellCount: Int get() = rows * columns
}

data class LayoutSlot(
    val index: Int,
    val cameraId: String?,
    val stream: StreamChoice = StreamChoice.AUTO,
)

enum class StreamChoice {
    AUTO,
    MAIN,
    SUB,
}

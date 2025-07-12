package com.cuttingedge.spiralrafflepoc.ui.data

import androidx.compose.ui.geometry.Offset
import java.util.UUID

data class Player(
    val id: UUID,
    val drawableId: Int,
    val spiralId: Int,
    var animatedValue: Float = 1f,
    var offsetFromAnimatedValue: Float = 0f,
    var isActive: Boolean = false,
    var pathOffsets: List<Offset> = emptyList()
) {
    val currentPos: Offset get() {
        if (pathOffsets.isEmpty()) {
            throw Exception("Path Offsets are empty, Cannot find Current Pos for Player Item")
        }

        val index = (pathOffsets.lastIndex * animatedValue.coerceIn(0f, 1f)).toInt()

        return pathOffsets[index]
    }

    fun reset() {
        animatedValue = 1f
        offsetFromAnimatedValue = 0f
        isActive = false
        pathOffsets = emptyList()
    }
}
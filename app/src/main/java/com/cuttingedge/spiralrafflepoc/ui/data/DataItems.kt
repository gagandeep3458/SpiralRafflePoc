package com.cuttingedge.spiralrafflepoc.ui.data

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.Offset
import java.util.UUID

data class Player(
    val id: UUID,
    val drawableId: Int,
    val spiralId: Int,
    var isActive: Boolean = false,
    var pathOffsets: List<Offset> = emptyList(),
    var posAnimatable : Animatable<Float, *> = Animatable(1f),
    var scaleAnimatable : Animatable<Float, *> = Animatable(1f),
) {
    val currentPos: Offset get() {
        if (pathOffsets.isEmpty()) {
            throw Exception("Path Offsets are empty, Cannot find Current Pos for Player Item")
        }

        val animatedValue = posAnimatable.value.coerceIn(0f, 1f)

        val index = (pathOffsets.lastIndex * animatedValue).toInt()

        return pathOffsets[index]
    }

    fun reset() {
        isActive = false
        pathOffsets = emptyList()
        posAnimatable = Animatable(1f)
        scaleAnimatable = Animatable(1f)
    }
}
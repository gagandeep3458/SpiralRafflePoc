package com.cuttingedge.spiralrafflepoc.ui.compasables

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.cuttingedge.spiralrafflepoc.R
import com.cuttingedge.spiralrafflepoc.ui.data.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

fun Float.toRadian(): Float = this / 180f * Math.PI.toFloat()

fun rotatePointAroundAnotherPoint(
    pointToRotate: Offset,
    centerOfRotation: Offset,
    angleDegrees: Float
): Offset {
    // 1. Translate the point to the origin
    val translatedX = pointToRotate.x - centerOfRotation.x
    val translatedY = pointToRotate.y - centerOfRotation.y

    // Convert angle from degrees to radians
    val angleRadians = angleDegrees.toRadian()

    // 2. Apply the rotation
    val rotatedXTranslated = translatedX * cos(angleRadians) - translatedY * sin(angleRadians)
    val rotatedYTranslated = translatedX * sin(angleRadians) + translatedY * cos(angleRadians)

    // 3. Translate the point back
    val rotatedX = rotatedXTranslated + centerOfRotation.x
    val rotatedY = rotatedYTranslated + centerOfRotation.y

    return Offset(rotatedX.toFloat(), rotatedY.toFloat())
}

private const val TAG = "SpiralRaffle"
@Composable
fun SpiralRaffle(modifier: Modifier = Modifier, numOfSpirals: Int = 4, playersList: List<Player>) {


    val spiralPointsPerLane = remember { mutableMapOf<Int, List<Offset>>() }
    val spiralGuidePointsPerLane = remember { mutableMapOf<Int, List<Offset>>() }

    val defaultBitmap: ImageBitmap = ImageBitmap.imageResource(R.drawable.user)

    val activePlayers = remember { mutableStateListOf<Player>() }

    LaunchedEffect(spiralGuidePointsPerLane, spiralPointsPerLane) {

        while(true) {

            val iterator = activePlayers.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (item.animatable.value < 0.05) {
                    item.reset()
                    iterator.remove() // Safely removes the current element
                }
            }


            val newPlayers = playersList.filter { !it.isActive }.take(4).onEachIndexed { i, p ->
                val list = spiralGuidePointsPerLane[i]
                if (list != null) {
                    p.pathOffsets = list
                    p.isActive = true
                }

                launch {
                    p.animatable.animateTo(0f, animationSpec = tween(durationMillis = 8000))
                }
            }

            activePlayers.addAll(newPlayers)
            Log.d(TAG, "SpiralRaffle: active list size after adding new ones: ${activePlayers.size}")
            delay(500)
        }
    }

    Canvas(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .background(color = Color(0xFF1F1F1F))
            .clipToBounds()
    ) {

        // Spiral parameters (same as before)
        val a = 10f
        val b = 0.95f
        val numRevolutions = 1f
        val totalTheta = numRevolutions * 2 * PI.toFloat()

        val numPoints = 64 // For drawing the spiral curve
        val numPointsForGuide = 350 // For drawing the spiral curve

        val centerX = size.width / 2f
        val centerY = size.height / 2f

        val initialTheta = 0.0f

        val spiralOffset = 360f.div(numOfSpirals)
        val imageGuideSpiralOffset = spiralOffset.div(2)

        // Draw Visible Spirals
        if (spiralPointsPerLane.isNotEmpty()) {
            for (pointsList in spiralPointsPerLane.values) {
                val path = Path()

                val firstPoint = pointsList.first()

                path.moveTo(firstPoint.x, firstPoint.y)

                for (i in 1 until pointsList.size) {
                    path.lineTo(pointsList[i].x, pointsList[i].y)
                }

                drawPath(
                    path = path,
                    color = Color(0xFF5F3109),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        } else {
            for (i in 0 until numOfSpirals) {

                val points = mutableListOf<Offset>()

                // Calculate the first point of the spiral for path drawing
                var r = a * exp(b * initialTheta)
                var x = r * cos(initialTheta)
                var y = r * sin(initialTheta)
                points.add(Offset(centerX + x, centerY + y))

                // Calculate the spiral curve points
                for (j in 1..numPoints) {
                    val theta = initialTheta + (j.toFloat() / numPoints) * totalTheta
                    r = a * exp(b * theta)
                    x = r * cos(theta)
                    y = r * sin(theta)

                    val p = Offset(centerX + x, centerY + y)

                    val rotatedP = rotatePointAroundAnotherPoint(
                        pointToRotate = p,
                        centerOfRotation = center,
                        i * spiralOffset
                    )

                    points.add(Offset(rotatedP.x, rotatedP.y))
                }

                spiralPointsPerLane[i] = points
            }
        }

        // Draw Guide Spirals for Images
        if (spiralGuidePointsPerLane.isNotEmpty()) {
/*            for (pointsList in spiralGuidePointsPerLane.values) {
                val path = Path()

                val firstPoint = pointsList.first()

                path.moveTo(firstPoint.x, firstPoint.y)

                for (i in 1 until pointsList.size) {
                    path.lineTo(pointsList[i].x, pointsList[i].y)
                }

                drawPath(
                    path = path,
                    color = Color(0xFF095F0A).copy(alpha = 0.2f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }*/
        } else {
            for (i in 0 until numOfSpirals) {

                val points = mutableListOf<Offset>()

                // Calculate the first point of the spiral for path drawing
                var r = a * exp(b * initialTheta)
                var x = r * cos(initialTheta)
                var y = r * sin(initialTheta)
                points.add(Offset(centerX + x, centerY + y))

                // Calculate the spiral curve points
                for (j in 1..numPointsForGuide) {
                    val theta = initialTheta + (j.toFloat() / numPointsForGuide) * totalTheta
                    r = a * exp(b * theta)
                    x = r * cos(theta)
                    y = r * sin(theta)

                    val p = Offset(centerX + x, centerY + y)

                    val rotatedP = rotatePointAroundAnotherPoint(
                        pointToRotate = p,
                        centerOfRotation = center,
                        i * spiralOffset + imageGuideSpiralOffset
                    )

                    points.add(Offset(rotatedP.x, rotatedP.y))
                }

                spiralGuidePointsPerLane[i] = points
            }
        }

        val widthPx = 48.dp.toPx()
        val heightPx = 48.dp.toPx()


        for (p in activePlayers) {

            val pos = p.currentPos

            val size = IntSize(
                widthPx.times(p.animatable.value).coerceIn(widthPx.div(3), widthPx).toInt(),
                heightPx.times(p.animatable.value).coerceIn(heightPx.div(3), heightPx).toInt()
            )
            val halfSize = size.div(2)

            Log.d(TAG, "SpiralRaffle: ${size}")
            drawImage(
                defaultBitmap,
                dstSize = size,
                dstOffset = IntOffset(
                    pos.x.toInt().minus(halfSize.width),
                    pos.y.toInt().minus(halfSize.height)
                )
            )
        }
    }
}

@Preview
@Composable
private fun SpiralRafflePreview() {

    val playersList = mutableListOf<Player>()

    val numOfSpirals = 4

    with(playersList) {
        for (i in 0 until 12) {
            add(
                Player(
                    id = UUID.randomUUID(),
                    drawableId = R.drawable.user,
                    spiralId = i % numOfSpirals
                )
            )
        }
    }

    SpiralRaffle(numOfSpirals = numOfSpirals, playersList = playersList)
}
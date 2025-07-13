package com.cuttingedge.spiralrafflepoc.ui.compasables

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

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

    return Offset(rotatedX, rotatedY)
}

fun getTotalArcLength(a: Float, b: Float, theta: Float): Float {
    return (a * sqrt(1 + b * b) / b) * exp(b * theta)
}

private const val TAG = "SpiralRaffle"

@Composable
fun SpiralRaffle(modifier: Modifier = Modifier, numOfSpirals: Int = 4, playersList: List<Player>) {

    // Spiral parameters (same as before)
    val a = 10f
    val b = 0.95f
    val numRevolutions = 0.8f
    val thetaMin = 0.0f
    val thetaMax = numRevolutions * 2 * PI.toFloat()

    val numPoints = 64 // For drawing the spiral curve
    val numPointsForGuide = 512 // For drawing the spiral curve
    val streakSizeInPoints = 20
    val numOfPointsBeforeSpiralStops = 4

    val spiralOffset = 360f.div(numOfSpirals)
    val imageGuideSpiralOffset = spiralOffset.div(2)

    val animatedStreakPositions = remember { mutableMapOf<Int, Animatable<Float, *>>() }

    val spiralPointsPerLane = remember { mutableMapOf<Int, List<Offset>>() }
    val spiralGuidePointsPerLane = remember { mutableMapOf<Int, List<Offset>>() }

    val defaultBitmap: ImageBitmap = ImageBitmap.imageResource(R.drawable.user)

    val activePlayers = remember { mutableStateListOf<Player>() }

    LaunchedEffect(Unit) {
        for (i in 0 until numOfSpirals) {
            animatedStreakPositions[i] = Animatable(numPoints.minus(1f))
            launch {
                animatedStreakPositions[i]!!.animateTo(
                    0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 2000,
                            delayMillis = 500
                        ),
                    )
                )
            }
        }
    }

    LaunchedEffect(spiralGuidePointsPerLane, spiralPointsPerLane) {
        while (true) {

            val iterator = activePlayers.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (item.posAnimatable.value < 0.05) {
                    item.reset()
                    iterator.remove() // Safely removes the current element
                }
            }


            val newPlayers =
                playersList.filter { !it.isActive }.take(numOfSpirals).onEachIndexed { i, p ->
                    val list = spiralGuidePointsPerLane[i]
                    if (list != null) {
                        p.pathOffsets = list
                        p.isActive = true
                    }

                    launch {
                        p.posAnimatable.animateTo(
                            0f,
                            animationSpec = tween(durationMillis = 6000, easing = LinearEasing)
                        )
                    }
                    launch {
                        // Delay to scale down the Player Image
                        delay(4000)
                        p.scaleAnimatable.animateTo(
                            0.4f,
                            animationSpec = tween(
                                durationMillis = 2000,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                }

            activePlayers.addAll(newPlayers)
            // Launch Delay for each Players from spiral end
            delay(600)
        }
    }

    Canvas(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .background(color = Color(0xFF1F1F1F))
            .clipToBounds()
    ) {

        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Spirals
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
                var r = a * exp(b * thetaMin)
                var x = r * cos(thetaMin)
                var y = r * sin(thetaMin)
                points.add(Offset(centerX + x, centerY + y))

                // Calculate the spiral curve points
                for (j in 1..numPoints) {
                    val theta = thetaMin + (j.toFloat() / numPoints) * thetaMax
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

        // Guide Spirals for Images
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

            val totalArcLength =
                getTotalArcLength(a, b, thetaMax) - getTotalArcLength(a, b, thetaMin)

            val segmentLength: Float = totalArcLength / (numPointsForGuide - 1f)

            for (i in 0..numOfSpirals) {

                val points = mutableListOf<Offset>()

                var currentTheta = thetaMin

                val p = Offset(a * cos(currentTheta), a * sin(currentTheta))

                points.add(p.plus(Offset(centerX, centerY)))

                // Calculate the spiral curve points
                for (j in 1 until numPointsForGuide) {
                    val theta = j * segmentLength

                    val term1 = (theta * b) / (a * sqrt(1 + b * b))
                    val term2 = exp(b * thetaMin)

                    currentTheta = (1 / b) * ln((term1 + term2).toDouble()).toFloat()

                    val r = a * exp(b * currentTheta)

                    val p = Offset(r * cos(currentTheta), r * sin(currentTheta))

                    val rotatedP = rotatePointAroundAnotherPoint(
                        pointToRotate = p.plus(Offset(centerX, centerY)),
                        centerOfRotation = center,
                        i * spiralOffset + imageGuideSpiralOffset
                    )

                    points.add(Offset(rotatedP.x, rotatedP.y))
                }

                spiralGuidePointsPerLane[i] = points
            }
        }

        val widthPx = 42.dp.toPx()
        val heightPx = 42.dp.toPx()

        // Draw Player Images
        for (p in activePlayers) {

            val pos = p.currentPos

            val size = IntSize(
                widthPx.times(p.scaleAnimatable.value).toInt(),
                heightPx.times(p.scaleAnimatable.value).toInt()
            )
            val halfSize = size.div(2)

            drawImage(
                defaultBitmap,
                dstSize = size,
                dstOffset = IntOffset(
                    pos.x.toInt().minus(halfSize.width),
                    pos.y.toInt().minus(halfSize.height)
                )
            )
        }

        Log.d(TAG, "SpiralRaffle: animated value ${animatedStreakPositions[0]?.value?.toInt()}")

        if (animatedStreakPositions.size == numOfSpirals) {
            for (i in 0 until numOfSpirals) {

                val startIndexInCurve = animatedStreakPositions[i]!!.value.toInt()

                val diff = numPoints.minus(1) - startIndexInCurve

                val endIndexInCurve =
                    if (diff < streakSizeInPoints) startIndexInCurve.plus(diff) else startIndexInCurve.plus(
                        streakSizeInPoints
                    )

                val spiralPoints = spiralPointsPerLane[i]

                if (spiralPoints != null && spiralPoints.isNotEmpty() && startIndexInCurve > numOfPointsBeforeSpiralStops) {
                    val streakPoints = spiralPoints.slice(startIndexInCurve..endIndexInCurve)

                    if (streakPoints.size >= 2) {
                        val path = Path()
                        path.moveTo(streakPoints.first().x, streakPoints.first().y)

                        for (j in 1 until streakPoints.size) {
                            val p = streakPoints[j]
                            path.lineTo(p.x, p.y)
                        }

                        drawPath(
                            path = path,
                            color = Color(0xFFE7AC81),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
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
package com.example.myapplication

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.launch

private enum class DragAxis {
    None,
    Horizontal,
    Vertical
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NumberSelectionDialog(
    initialNumber: Int = 10,
    title: String = "RED CARD",
    onNumberConfirmed: (Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val clampedInitial = initialNumber.coerceIn(0, 99)
    var tens by remember { mutableIntStateOf(clampedInitial / 10) }
    var ones by remember { mutableIntStateOf(clampedInitial % 10) }

    var tensSlideDirection by remember { mutableIntStateOf(-1) }
    var onesSlideDirection by remember { mutableIntStateOf(1) }

    val confirmScale = remember { Animatable(1f) }
    val tensSnapScale = remember { Animatable(1f) }
    val onesSnapScale = remember { Animatable(1f) }

    val deadZonePx = with(density) { 8.dp.toPx() }
    val stepPx = with(density) { 24.dp.toPx() }
    val decay = exponentialDecay<Float>()
    val velocityThreshold = 900f

    fun tick() {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun updateTens(stepDirection: Int) {
        tens = (tens + stepDirection + 10) % 10
        tensSlideDirection = if (stepDirection > 0) -1 else 1
        tick()
    }

    fun updateOnes(stepDirection: Int) {
        ones = (ones + stepDirection + 10) % 10
        onesSlideDirection = if (stepDirection > 0) 1 else -1
        tick()
    }

    suspend fun runSnapBounce(axis: DragAxis) {
        val target = if (axis == DragAxis.Vertical) tensSnapScale else onesSnapScale
        target.snapTo(1f)
        target.animateTo(1.04f, tween(durationMillis = 50, easing = LinearOutSlowInEasing))
        target.animateTo(1f, tween(durationMillis = 50, easing = LinearOutSlowInEasing))
    }

    fun handleStep(axis: DragAxis, signedDirection: Int) {
        if (axis == DragAxis.Horizontal) {
            updateOnes(if (signedDirection > 0) 1 else -1)
        } else if (axis == DragAxis.Vertical) {
            updateTens(if (signedDirection < 0) 1 else -1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                val velocityTracker = VelocityTracker()
                var lockedAxis = DragAxis.None
                var totalOffsetX = 0f
                var totalOffsetY = 0f
                var accumulated = 0f

                detectDragGestures(
                    onDragStart = {
                        lockedAxis = DragAxis.None
                        totalOffsetX = 0f
                        totalOffsetY = 0f
                        accumulated = 0f
                        velocityTracker.resetTracking()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        totalOffsetX += dragAmount.x
                        totalOffsetY += dragAmount.y

                        if (lockedAxis == DragAxis.None) {
                            val distance = kotlin.math.sqrt(totalOffsetX * totalOffsetX + totalOffsetY * totalOffsetY)
                            if (distance < deadZonePx) {
                                return@detectDragGestures
                            }
                            lockedAxis = if (abs(totalOffsetX) > abs(totalOffsetY)) {
                                DragAxis.Horizontal
                            } else {
                                DragAxis.Vertical
                            }
                        }

                        val delta = if (lockedAxis == DragAxis.Horizontal) dragAmount.x else dragAmount.y
                        accumulated += delta

                        while (abs(accumulated) >= stepPx) {
                            val stepDirection = accumulated.sign.toInt().coerceIn(-1, 1)
                            handleStep(lockedAxis, stepDirection)
                            accumulated -= stepPx * stepDirection
                        }
                    },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity()
                        val axisVelocity = when (lockedAxis) {
                            DragAxis.Horizontal -> velocity.x
                            DragAxis.Vertical -> velocity.y
                            DragAxis.None -> 0f
                        }

                        coroutineScope.launch {
                            if (abs(axisVelocity) > velocityThreshold && lockedAxis != DragAxis.None) {
                                val animatable = Animatable(0f)
                                var lastPosition = 0f
                                var flingAccumulated = 0f

                                animatable.animateDecay(axisVelocity, decay) {
                                    val delta = value - lastPosition
                                    lastPosition = value
                                    flingAccumulated += delta
                                    while (abs(flingAccumulated) >= stepPx) {
                                        val stepDirection = flingAccumulated.sign.toInt().coerceIn(-1, 1)
                                        handleStep(lockedAxis, stepDirection)
                                        flingAccumulated -= stepPx * stepDirection
                                    }
                                }
                            }
                            if (lockedAxis != DragAxis.None) {
                                runSnapBounce(lockedAxis)
                            }
                        }

                        lockedAxis = DragAxis.None
                    },
                    onDragCancel = {
                        lockedAxis = DragAxis.None
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .pointerInput(Unit) {
                    detectTapGestures {
                        coroutineScope.launch {
                            confirmScale.snapTo(1f)
                            confirmScale.animateTo(1.1f, tween(durationMillis = 100, easing = LinearOutSlowInEasing))
                            confirmScale.animateTo(1f, tween(durationMillis = 100, easing = LinearOutSlowInEasing))
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNumberConfirmed(tens * 10 + ones)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.scale(confirmScale.value)
            ) {
                AnimatedDigit(
                    value = tens,
                    isVertical = true,
                    direction = tensSlideDirection,
                    scale = tensSnapScale.value
                )
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedDigit(
                    value = ones,
                    isVertical = false,
                    direction = onesSlideDirection,
                    scale = onesSnapScale.value
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedDigit(
    value: Int,
    isVertical: Boolean,
    direction: Int,
    scale: Float
) {
    val offsetSpec = tween<IntOffset>(durationMillis = 120, easing = LinearOutSlowInEasing)
    val fadeSpec = tween<Float>(durationMillis = 120, easing = LinearOutSlowInEasing)

    AnimatedContent(
        targetState = value,
        transitionSpec = {
            if (isVertical) {
                val inOffset = if (direction < 0) { height: Int -> height } else { height: Int -> -height }
                val outOffset = if (direction < 0) { height: Int -> -height } else { height: Int -> height }
                slideInVertically(offsetSpec, initialOffsetY = inOffset) + fadeIn(fadeSpec) togetherWith
                    slideOutVertically(offsetSpec, targetOffsetY = outOffset) + fadeOut(fadeSpec)
            } else {
                val inOffset = if (direction > 0) { width: Int -> -width } else { width: Int -> width }
                val outOffset = if (direction > 0) { width: Int -> width } else { width: Int -> -width }
                slideInHorizontally(offsetSpec, initialOffsetX = inOffset) + fadeIn(fadeSpec) togetherWith
                    slideOutHorizontally(offsetSpec, targetOffsetX = outOffset) + fadeOut(fadeSpec)
            }
        },
        label = "digit"
    ) { digit ->
        Text(
            text = digit.toString(),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.scale(scale)
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun NumberSelectionDialogPreviewSmall() {
    MaterialTheme {
        NumberSelectionDialog(
            initialNumber = 10,
            title = "RED CARD",
            onNumberConfirmed = {}
        )
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun NumberSelectionDialogPreviewLarge() {
    MaterialTheme {
        NumberSelectionDialog(
            initialNumber = 88,
            title = "GOAL",
            onNumberConfirmed = {}
        )
    }
}

@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun NumberSelectionDialogPreviewSquare() {
    MaterialTheme {
        NumberSelectionDialog(
            initialNumber = 0,
            title = "YELLOW CARD",
            onNumberConfirmed = {}
        )
    }
}

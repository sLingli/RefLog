package com.example.myapplication.ui

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Compose 版流体时钟
 * 保留原 FluidClockView 的渐变扫过效果，但用 Compose Canvas 实现
 * 每秒更新一次，比原版 60fps 更省电
 */
@Composable
fun FluidClock(
    textSizeSp: Float = 28f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // 每秒更新一次时间
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            calendar = Calendar.getInstance()
            delay(1000)
        }
    }

    val textPx = with(density) { textSizeSp.sp.toPx() }
    val colorStart = android.graphics.Color.parseColor("#00FF85")
    val colorEnd = android.graphics.Color.parseColor("#FF6D00")
    val colorBg = android.graphics.Color.WHITE
    val wiperWidth = 0.1f

    val timeText = remember(calendar.get(Calendar.SECOND)) {
        val format = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm"
        DateFormat.format(format, calendar).toString()
    }

    val seconds = calendar.get(Calendar.SECOND)
    val millis = calendar.get(Calendar.MILLISECOND)

    Canvas(modifier = modifier) {
        val width = size.width
        if (width <= 0f) return@Canvas

        val rawProgress = (seconds * 1000 + millis) / 60000f

        val gradient = if (seconds == 59 && millis > 500) {
            val wipeProgress = (millis - 500) / 500f
            LinearGradient(
                0f, 0f, width, 0f,
                intArrayOf(colorBg, colorBg, colorEnd, colorEnd),
                floatArrayOf(0f, wipeProgress, wipeProgress + wiperWidth, 1f),
                Shader.TileMode.CLAMP
            )
        } else {
            val progress = rawProgress * (60f / 59.5f)
            LinearGradient(
                0f, 0f, width, 0f,
                intArrayOf(colorStart, colorEnd, colorBg, colorBg),
                floatArrayOf(0f, progress, progress + wiperWidth, 1f),
                Shader.TileMode.CLAMP
            )
        }

        val paint = Paint().apply {
            textSize = textPx
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
            shader = gradient
            textAlign = Paint.Align.CENTER
        }

        val textY = (size.height / 2f) - ((paint.descent() + paint.ascent()) / 2f)

        val nativeCanvas = drawContext.canvas.nativeCanvas
        nativeCanvas.drawText(timeText, size.width / 2f, textY, paint)
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun FluidClockPreviewSmall() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            FluidClock(textSizeSp = 28f)
        }
    }
}

@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun FluidClockPreviewSquare() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            FluidClock(textSizeSp = 28f)
        }
    }
}

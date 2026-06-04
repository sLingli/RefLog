package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.text.format.DateFormat
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import java.util.Calendar


class FluidClockView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val calendar = Calendar.getInstance()

    private val colorStart = Color.parseColor("#00FF85")
    private val colorEnd = Color.parseColor("#FF6D00")
    private val colorBg = Color.WHITE

    private val WIPER_WIDTH = 0.1f
    override fun onDraw(canvas: Canvas) {
        val now = System.currentTimeMillis()
        calendar.timeInMillis = now
        val seconds = calendar.get(Calendar.SECOND)
        val millis = calendar.get(Calendar.MILLISECOND)
        val format = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm"
        text = DateFormat.format(format, calendar)
        val width = width.toFloat()
        if (width > 0) {
            val rawProgress = (seconds * 1000 + millis) / 60000f
            val gradient: LinearGradient
            if (seconds == 59 && millis > 500) {
                val wipeProgress = (millis - 500) / 500f
                gradient = LinearGradient(
                    0f, 0f, width, 0f,
                    intArrayOf(colorBg, colorBg, colorEnd, colorEnd),
                    floatArrayOf(0f, wipeProgress, wipeProgress + WIPER_WIDTH, 1f),
                    Shader.TileMode.CLAMP
                )
            } else {
                val progress = rawProgress * (60f / 59.5f)
                gradient = LinearGradient(
                    0f, 0f, width, 0f,
                    intArrayOf(colorStart, colorEnd, colorBg, colorBg),
                    floatArrayOf(0f, progress, progress + WIPER_WIDTH, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            paint.shader = gradient
        }
        super.onDraw(canvas)
        postInvalidateDelayed(16)
    }
}
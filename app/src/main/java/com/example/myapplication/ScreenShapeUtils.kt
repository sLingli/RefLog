package com.example.myapplication

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * 检测当前设备是否是圆形屏幕
 *
 * @return true 如果是圆形屏幕，false 如果是方形屏幕
 */
@Composable
fun isRoundScreen(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.isScreenRound
}

/**
 * 根据屏幕形状返回适合的对话框背景形状
 * 圆形屏幕使用 CircleShape
 * 方形屏幕使用 RoundedCornerShape
 *
 * @param cornerRadius 方形屏幕时的圆角半径，默认为16dp
 * @return 适合当前屏幕的 Shape
 */
@Composable
fun dialogBackgroundShape(cornerRadius: Int = 16): Shape {
    val isRound = isRoundScreen()
    return remember(isRound, cornerRadius) {
        if (isRound) CircleShape else RoundedCornerShape(cornerRadius.dp)
    }
}

/**
 * 根据屏幕形状返回适合的按钮形状
 * 圆形屏幕使用 CircleShape
 * 方形屏幕使用 RoundedCornerShape
 *
 * @param cornerRadius 方形屏幕时的圆角半径，默认为50%
 * @return 适合当前屏幕的 Shape
 */
@Composable
fun buttonShape(cornerRadius: Int = 50): Shape {
    val isRound = isRoundScreen()
    return remember(isRound, cornerRadius) {
        if (isRound) CircleShape else RoundedCornerShape(cornerRadius)
    }
}

/**
 * 根据屏幕形状返回适合的内边距
 * 圆形屏幕需要更大的边距以适应圆形边缘
 * 方形屏幕可以使用较小的边距
 *
 * @param roundPadding 圆形屏幕的边距
 * @param squarePadding 方形屏幕的边距
 * @return 适合当前屏幕的边距值
 */
@Composable
fun screenPadding(roundPadding: Int = 16, squarePadding: Int = 8): Int {
    return if (isRoundScreen()) roundPadding else squarePadding
}



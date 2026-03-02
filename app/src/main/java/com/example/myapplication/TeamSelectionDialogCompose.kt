package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.tooling.preview.devices.WearDevices

// 自定义形状：左上三角形区域（45°对角线分割）
class TopLeftTriangleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(0f, 0f) // 左上角
            lineTo(size.width, 0f) // 右上角
            lineTo(0f, size.height) // 左下角
            close()
        }
        return Outline.Generic(path)
    }
}

// 自定义形状：右下三角形区域（45°对角线分割）
class BottomRightTriangleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width, 0f) // 右上角
            lineTo(size.width, size.height) // 右下角
            lineTo(0f, size.height) // 左下角
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun TeamSelectionDialogCompose(
    onHomeTeamSelected: () -> Unit,
    onAwayTeamSelected: () -> Unit,
    onDismiss: () -> Unit,
    homeTeamColor: Color = Color(0xFF1565C0),
    awayTeamColor: Color = Color(0xFFC62828)
) {
    // 根据背景颜色亮度计算合适的图标颜色
    fun getIconColor(backgroundColor: Color): Color {
        val red = backgroundColor.red
        val green = backgroundColor.green
        val blue = backgroundColor.blue
        // 计算相对亮度 (ITU-R BT.709)
        val luminance = 0.2126f * red + 0.7152f * green + 0.0722f * blue
        // 如果背景较亮（亮度 > 0.5），使用黑色图标，否则使用白色图标
        return if (luminance > 0.5f) Color.Black else Color.White
    }

    val homeIconColor = getIconColor(homeTeamColor)
    val awayIconColor = getIconColor(awayTeamColor)

    // 检测屏幕形状
    val isRoundScreen = LocalConfiguration.current.isScreenRound
    val screenShape = if (isRoundScreen) CircleShape else RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(screenShape)
    ) {
        // 左上部分 - 主队区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(TopLeftTriangleShape())
                .background(homeTeamColor)
                .clickable(
                    onClick = onHomeTeamSelected,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_home),
                contentDescription = "Home Team",
                tint = homeIconColor,
                modifier = Modifier
                    .size(64.dp)
                    .offset(x = (-20).dp, y = (-20).dp) // 向左上偏移，使图标更居中于三角形区域
            )
        }

        // 右下部分 - 客队区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(BottomRightTriangleShape())
                .background(awayTeamColor)
                .clickable(
                    onClick = onAwayTeamSelected,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_flight),
                contentDescription = "Away Team",
                tint = awayIconColor,
                modifier = Modifier
                    .size(64.dp)
                    .offset(x = 20.dp, y = 20.dp) // 向右下偏移，使图标更居中于三角形区域
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun PreviewTeamSelectionDialogSmallRound() {
    TeamSelectionDialogCompose(
        onHomeTeamSelected = {},
        onAwayTeamSelected = {},
        onDismiss = {}
    )
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun PreviewTeamSelectionDialogLargeRound() {
    TeamSelectionDialogCompose(
        onHomeTeamSelected = {},
        onAwayTeamSelected = {},
        onDismiss = {}
    )
}

@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun PreviewTeamSelectionDialogSquare() {
    TeamSelectionDialogCompose(
        onHomeTeamSelected = {},
        onAwayTeamSelected = {},
        onDismiss = {}
    )
}

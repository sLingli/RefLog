package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 左上部分 - 主队区域（蓝色）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(TopLeftTriangleShape())
                .background(Color(0xFF1565C0))
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
                tint = Color.White,
                modifier = Modifier
                    .size(64.dp)
                    .offset(x = (-20).dp, y = (-20).dp) // 向左上偏移，使图标更居中于三角形区域
            )
        }

        // 右下部分 - 客队区域（红色）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(BottomRightTriangleShape())
                .background(Color(0xFFC62828))
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
                tint = Color.White,
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

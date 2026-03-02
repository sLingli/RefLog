package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices

// 默认颜色列表，通过ToInt转换为Int类型以匹配原有逻辑
val DEFAULT_TEAM_COLORS = listOf(
    0xFFF44336.toInt(), // 红
    0xFF2196F3.toInt(), // 蓝
    0xFF4CAF50.toInt(), // 绿
    0xFFFFEB3B.toInt(), // 黄
    0xFFFFFFFF.toInt(), // 白
    0xFF000000.toInt(), // 黑
    0xFF9C27B0.toInt(), // 紫
    0xFFFF9800.toInt()  // 橙
)

@Composable
fun ColorSelectionDialog(
    initialColor: Int = DEFAULT_TEAM_COLORS[0],
    onColorSelected: (Int) -> Unit
) {
    // 查找初始颜色的索引
    val initialIndex = DEFAULT_TEAM_COLORS.indexOf(initialColor).let { if (it == -1) 0 else it }

    // Picker状态，设置较大的初始值以支持无限滚动模拟
    val pickerState = rememberPickerState(
        initialNumberOfOptions = DEFAULT_TEAM_COLORS.size,
        initiallySelectedOption = initialIndex
    )

    // 当前选中的颜色
    var selectedColor by remember { mutableIntStateOf(initialColor) }

    // 监听Picker变化更新选中颜色
    LaunchedEffect(pickerState.selectedOption) {
        selectedColor = DEFAULT_TEAM_COLORS[pickerState.selectedOption]
    }

    // 检测屏幕形状
    val isRoundScreen = LocalConfiguration.current.isScreenRound
    val screenShape = if (isRoundScreen) CircleShape else RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(screenShape)
            .background(Color.Black), // 适配圆形和方形手表
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // 标题区域
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.apparel),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.title_set_color),
                    style = MaterialTheme.typography.caption1.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 颜色选择滚轮区域
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(100.dp) // 稍微调整高度以适应圆形屏幕
                    .fillMaxWidth()
            ) {
                // 中间的高亮背景条
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(40.dp)
                        .background(Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                )

                Picker(
                    state = pickerState,
                    contentDescription = stringResource(id = R.string.title_set_color),
                    modifier = Modifier.height(100.dp),
                    option = { optionIndex ->
                        val colorInt = DEFAULT_TEAM_COLORS[optionIndex % DEFAULT_TEAM_COLORS.size]
                        val color = Color(colorInt)
                        val isWhite = (colorInt == -1) // 0xFFFFFFFF to Int is -1

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = 2.dp,
                                        color = if (isWhite) Color.Gray else Color.White,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 确认按钮
            Button(
                onClick = { onColorSelected(selectedColor) },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(40.dp)
            ) {
                Text(text = stringResource(id = R.string.btn_ok))
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun ColorSelectionDialogPreview() {
    ColorSelectionDialog(
        initialColor = DEFAULT_TEAM_COLORS[1],
        onColorSelected = {}
    )
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun ColorSelectionDialogLargePreview() {
    ColorSelectionDialog(
        initialColor = DEFAULT_TEAM_COLORS[2],
        onColorSelected = {}
    )
}

@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun ColorSelectionDialogSquarePreview() {
    ColorSelectionDialog(
        initialColor = DEFAULT_TEAM_COLORS[0],
        onColorSelected = {}
    )
}



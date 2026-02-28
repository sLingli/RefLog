package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*

@Composable
fun SettingsScreen(
    onHistoryClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .clip(CircleShape)
    ) {
        Scaffold(
            positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
        ) {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 第一项：Settings 图标（纯装饰，不可点击）
                item {
                    Icon(
                        painter = painterResource(id = R.drawable.settings),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(bottom = 8.dp)
                    )
                }

                // 第二项：历史记录条目
                item {
                    Chip(
                        onClick = onHistoryClick,
                        label = {
                            Text(
                                text = "历史记录",
                                color = Color.White
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_history),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF333333)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }

                // 第三项：关于条目
                item {
                    Chip(
                        onClick = onAboutClick,
                        label = {
                            Text(
                                text = "关于",
                                color = Color.White
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.info),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color(0xFF333333)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}


package com.example.myapplication

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices

@Composable
fun TeamSelectionDialogCompose(
    onHomeTeamSelected: () -> Unit,
    onAwayTeamSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    // 适配圆形手表的布局，使用 Box 居中内容
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // 对应默认的手表背景
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp) // 增加水平内边距以适应圆形边缘
        ) {
            // 标题
            Text(
                text = stringResource(id = R.string.title_select_team_generic),
                style = MaterialTheme.typography.title2.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // 队伍按钮区域 (Home 和 Away)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 主队按钮 (Home)
                Button(
                    onClick = onHomeTeamSelected,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1565C0)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(80.dp) // 对应 XML 中的 weight=1, height=80dp, 这里给固定大���或者 weight
                        .weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home),
                        contentDescription = "Home Team",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp)) // 对应 marginEnd=8dp 和 marginStart=8dp

                // 客队按钮 (Away)
                Button(
                    onClick = onAwayTeamSelected,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(80.dp)
                        .weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_flight),
                        contentDescription = "Away Team",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 取消按钮
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333)),
                shape = CircleShape, // 对应 cornerRadius=30dp (在 60x60 中即为圆形)
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_close_24),
                    contentDescription = stringResource(id = R.string.btn_cancel),
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(28.dp)
                )
            }
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


package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.tooling.preview.devices.WearDevices

@Composable
fun AboutScreen() {
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // 检测屏幕形状
    val isRoundScreen = LocalConfiguration.current.isScreenRound
    val screenShape = if (isRoundScreen) CircleShape else RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .clip(screenShape)
    ) {
        Scaffold(
            positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
        ) {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentPadding = PaddingValues(top = 90.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .size(90.dp)
                            .background(Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.reflog_logo),
                            contentDescription = "RefLog Logo",
                            tint = Color.Black,
                            modifier = Modifier
                                .size(100.dp)
                        )
                    }
                }
                item {
                    Text(
                        text = "RefLog",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    Text(
                        text = "V$versionName",
                        color = Color(0xFF888888),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun AboutScreenPreview() {
    MaterialTheme {
        AboutScreen()
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun AboutScreenPreviewLarge() {
    MaterialTheme {
        AboutScreen()
    }
}

@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun AboutScreenPreviewSquare() {
    MaterialTheme {
        AboutScreen()
    }
}


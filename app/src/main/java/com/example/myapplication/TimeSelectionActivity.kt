package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

class TimeSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TimeSelectionScreen(
                    onCancel = { finish() },
                    onConfirm = { minutes ->
                        val res = Intent().apply { putExtra("SELECTED_TIME", minutes) }
                        setResult(Activity.RESULT_OK, res)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun TimeSelectionScreen(onCancel: () -> Unit, onConfirm: (Int) -> Unit) {
    val timeOptions = (1..45).toList()
    val pickerState = rememberPickerState(initialNumberOfOptions = timeOptions.size, initiallySelectedOption = 44)
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    // 震动反馈
    LaunchedEffect(pickerState.selectedOption) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    // 自动获取焦点
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onRotaryScrollEvent { event ->
                coroutineScope.launch {
                    val newTarget = if (event.verticalScrollPixels > 0) {
                        pickerState.selectedOption + 1
                    } else {
                        pickerState.selectedOption - 1
                    }
                    if (newTarget in 0 until timeOptions.size) {
                        pickerState.scrollToOption(newTarget)
                    }
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Picker(
            state = pickerState,
            contentDescription = "Select Time",
            modifier = Modifier.fillMaxHeight(0.6f)
        ) { index ->
            val isSelected = (pickerState.selectedOption == index)
            Text(
                text = "${timeOptions[index]}",
                style = TextStyle(
                    fontSize = if (isSelected) 54.sp else 30.sp,
                    color = if (isSelected) Color(0xFF00FF85) else Color.Gray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            )
        }

        Text(
            text = "min",
            color = Color(0xFF00FF85),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Center).padding(top = 50.dp)
        )

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF3B30)),
                modifier = Modifier.padding(end = 12.dp).size(52.dp)
            ) { Icon(Icons.Default.Close, null) }

            Button(
                onClick = { onConfirm(timeOptions[pickerState.selectedOption]) },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00FF85)),
                modifier = Modifier.padding(start = 12.dp).size(52.dp)
            ) { Icon(Icons.Default.Check, null, tint = Color.Black) }
        }
    }
}
// 1. 专门写一个给预览用的函数，不带参数
@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun TimeSelectionPreview() {
    // 2. 在这里调用你的主函数，随便给它传点“假动作”
    TimeSelectionScreen(
        onCancel = { /* 预览里点取消没反应 */ },
        onConfirm = { minutes -> /* 预览里点确认没反应 */ }
    )
}
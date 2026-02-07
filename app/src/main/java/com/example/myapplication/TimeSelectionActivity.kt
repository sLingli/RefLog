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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

class TimeSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TimeSelectionScreen(
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
fun TimeSelectionScreen(onConfirm: (Int) -> Unit) {
    val timeOptions = (1..45).toList()
    val pickerState = rememberPickerState(
        initialNumberOfOptions = timeOptions.size,
        initiallySelectedOption = 44
    )

    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    LaunchedEffect(pickerState.selectedOption) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

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
        Text(
            text = stringResource(R.string.label_min),
            color = Color(0xFFAAAAAA),
            style = MaterialTheme.typography.title3,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
        )

        // 2. 中间的滚轮 (Picker)
        Picker(
            state = pickerState,
            contentDescription = "Select Time",
            modifier = Modifier.fillMaxHeight(0.6f)
        ) { index ->
            val isSelected = (pickerState.selectedOption == index)
            Text(
                text = "${timeOptions[index]}",
                style = TextStyle(
                    fontSize = if (isSelected) 60.sp else 32.sp,
                    color = if (isSelected) Color.White else Color.DarkGray,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            )
        }

        Button(
            onClick = { onConfirm(timeOptions[pickerState.selectedOption]) },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00E676)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 15.dp)
                .width(100.dp)
                .height(42.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Confirm",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun TimeSelectionPreview() {
    TimeSelectionScreen(
        onConfirm = { minutes ->  }
    )
}
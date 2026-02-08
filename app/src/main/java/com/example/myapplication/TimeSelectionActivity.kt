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
fun TimeSelectionScreen(
    onConfirm: (Int) -> Unit
) {
    val items = remember { (1..45).toList() }
    val pickerState = rememberPickerState(
        initialNumberOfOptions = items.size,
        initiallySelectedOption = 44
    )

    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val view = LocalView.current

    LaunchedEffect(pickerState.selectedOption) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onRotaryScrollEvent { event ->
                if (Math.abs(event.verticalScrollPixels) > 16f) {
                    coroutineScope.launch {
                        val targetIndex = if (event.verticalScrollPixels > 0) {
                            (pickerState.selectedOption + 1).coerceAtMost(items.size - 1)
                        } else {
                            (pickerState.selectedOption - 1).coerceAtLeast(0)
                        }
                        pickerState.animateScrollToOption(targetIndex)
                    }
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Picker(
                    state = pickerState,
                    contentDescription = "时间选择",
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = -5.dp),
                    separation = 4.dp
                ) { optionIndex ->
                    val isSelected = pickerState.selectedOption == optionIndex

                    Box(
                        modifier = Modifier.height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${items[optionIndex]}",
                            style = TextStyle(
                                fontSize = if (isSelected) 100.sp else 38.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.label_min),
                    style = TextStyle(
                        fontSize = 22.sp,
                        color = Color(0xFF00FF85),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 68.dp, y = 36.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Button(
                    onClick = { onConfirm(items[pickerState.selectedOption]) },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF00FF85),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .width(110.dp)
                        .height(46.dp),
                    shape = RoundedCornerShape(23.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm",
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
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
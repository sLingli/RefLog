package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices

@Composable
fun NumberSelectionDialog(
    initialNumber: Int = 0,
    teamName: String = "Home",
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit
) {
    // State to hold the current number string
    var currentNumberString by remember { mutableStateOf(if (initialNumber > 0) initialNumber.toString() else "") }

    // Derived value for display
    val displayedNumber = if (currentNumberString.isEmpty()) "0" else currentNumberString

    val listState = rememberScalingLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display Area
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    if (teamName.isNotEmpty()) {
                        Text(
                            text = teamName,
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Text(
                        text = displayedNumber,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            // Keypad Area
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "DEL")
                    )

                    keys.forEach { rowKeys ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.weight(1f)) // Centering helper
                            rowKeys.forEach { key ->
                                KeypadButton(
                                    text = key,
                                    onClick = {
                                        when (key) {
                                            "DEL" -> {
                                                if (currentNumberString.isNotEmpty()) {
                                                    currentNumberString = currentNumberString.dropLast(1)
                                                }
                                            }
                                            "C" -> {
                                                currentNumberString = ""
                                            }
                                            else -> {
                                                if (currentNumberString.length < 2) {
                                                    val newValue = currentNumberString + key
                                                    if (currentNumberString == "0") {
                                                        currentNumberString = key
                                                    } else {
                                                        currentNumberString = newValue
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f)) // Centering helper
                        }
                    }
                }
            }

            // OK Button
            item {
                Button(
                    onClick = {
                        val number = currentNumberString.toIntOrNull() ?: 0
                        onConfirm(number)
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00C853)), // Green color for OK
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(40.dp)
                        .padding(top = 8.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }

            // Cancel Button
            item {
                CompactButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xFFD32F2F)), // Red for Cancel
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Cancel", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    val isAction = text == "DEL" || text == "C"
    Box(
        modifier = Modifier
            .size(36.dp) // Fixed size for keypad buttons
            .clip(CircleShape)
            .background(if (isAction) Color(0xFF424242) else Color(0xFF616161))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = if (text.length > 1) 10.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun NumberSelectionDialogPreview() {
    MaterialTheme {
        NumberSelectionDialog(
            initialNumber = 12,
            teamName = "Home",
            onConfirm = {},
            onCancel = {}
        )
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun NumberSelectionDialogLargePreview() {
    MaterialTheme {
        NumberSelectionDialog(
            initialNumber = 88,
            teamName = "Away",
            onConfirm = {},
            onCancel = {}
        )
    }
}

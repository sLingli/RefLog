package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PickerState
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState
import androidx.wear.tooling.preview.devices.WearDevices

import java.util.Locale

@Composable
fun NumberSelectionDialog(
    initialNumber: Int = 0,
    teamName: String = "Home",
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit
) {
    // Parse initial number into tens and ones
    val initialTens = (initialNumber / 10).coerceIn(0, 9)
    val initialOnes = (initialNumber % 10).coerceIn(0, 9)

    val tensState = rememberPickerState(
        initialNumberOfOptions = 10,
        initiallySelectedOption = initialTens
    )
    val onesState = rememberPickerState(
        initialNumberOfOptions = 10,
        initiallySelectedOption = initialOnes
    )

    val currentTens by remember { derivedStateOf { tensState.selectedOption } }
    val currentOnes by remember { derivedStateOf { onesState.selectedOption } }
    val selectedNumber = currentTens * 10 + currentOnes

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            anchorType = ScalingLazyListAnchorType.ItemStart,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.title_select_number),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item {
                Text(
                    text = teamName,
                    fontSize = 14.sp,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tens Picker
                    NumberPickerColumn(
                        state = tensState,
                        range = 0..9
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Ones Picker
                    NumberPickerColumn(
                        state = onesState,
                        range = 0..9
                    )
                }
            }

            item {
                Text(
                    text = "# ${String.format(Locale.US, "%02d", selectedNumber)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF616161)),
                        modifier = Modifier
                            .height(50.dp)
                            .weight(1f)
                            .padding(end = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_cancel),
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = { onConfirm(selectedNumber) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                        modifier = Modifier
                            .height(50.dp)
                            .weight(1f)
                            .padding(start = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_ok), // Assuming "OK" is in strings, otherwise "OK"
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NumberPickerColumn(
    state: PickerState,
    range: IntRange
) {
    Box(
        modifier = Modifier.size(50.dp, 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Picker(
            state = state,
            contentDescription = "Number Picker",
            modifier = Modifier.fillMaxSize()
        ) {
            val number = range.first + it
            Text(
                text = "$number",
                fontSize = 24.sp,
                color = if (it == state.selectedOption) Color.White else Color.Gray,
                modifier = Modifier.padding(4.dp)
            )
        }
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





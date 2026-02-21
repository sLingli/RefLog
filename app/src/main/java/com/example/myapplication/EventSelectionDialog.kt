package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold

@Composable
fun EventSelectionDialog(
    onYellowCardClick: () -> Unit,
    onRedCardClick: () -> Unit,
    onInjuryClick: () -> Unit,
    onGoalClick: () -> Unit,
    onSubstitutionClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black), // Watch background usually black
            contentAlignment = Alignment.Center
        ) {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                anchorType = ScalingLazyListAnchorType.ItemCenter,
                verticalArrangement = Arrangement.Center,
                autoCentering = AutoCenteringParams(itemIndex = 1)
            ) {
                // 第一行：黄牌 + 红牌
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        EventButton(
                            color = Color(0xFFFFEB3B),
                            onClick = onYellowCardClick,
                            contentDescription = stringResource(R.string.event_yellow)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        EventButton(
                            color = Color(0xFFF44336),
                            onClick = onRedCardClick,
                            contentDescription = stringResource(R.string.event_red)
                        )
                    }
                }

                // 第二行：伤停 + 进球
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        EventButton(
                            color = Color(0xFF2196F3),
                            onClick = onInjuryClick,
                            iconResId = R.drawable.ic_medical,
                            contentDescription = stringResource(R.string.event_injury)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        EventButton(
                            color = Color(0xFF000000), // Background black
                            onClick = onGoalClick,
                            iconResId = R.drawable.sports_soccer,
                            contentDescription = stringResource(R.string.event_goal)
                        )
                    }
                }

                // 第三行：换人 + 取消
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        EventButton(
                            color = Color(0xFF9C27B0),
                            onClick = onSubstitutionClick,
                            iconResId = R.drawable.ic_substitute,
                            contentDescription = stringResource(R.string.event_substitute)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        EventButton(
                            color = Color(0xFF616161),
                            onClick = onCancelClick,
                            iconResId = R.drawable.outline_close_24,
                            contentDescription = stringResource(R.string.btn_cancel)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventButton(
    color: Color,
    onClick: () -> Unit,
    iconResId: Int? = null,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(72.dp) // Maintain original size roughly, or adaptive?
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        if (iconResId != null) {

            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null, // Description is on the button container
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        } else {

        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun EventSelectionDialogPreviewSmall() {
    MaterialTheme {
        EventSelectionDialog(
            onYellowCardClick = {},
            onRedCardClick = {},
            onInjuryClick = {},
            onGoalClick = {},
            onSubstitutionClick = {},
            onCancelClick = {}
        )
    }
}

@Preview(device = Devices.WEAR_OS_LARGE_ROUND, showSystemUi = true)
@Composable
fun EventSelectionDialogPreviewLarge() {
    MaterialTheme {
        EventSelectionDialog(
            onYellowCardClick = {},
            onRedCardClick = {},
            onInjuryClick = {},
            onGoalClick = {},
            onSubstitutionClick = {},
            onCancelClick = {}
        )
    }
}

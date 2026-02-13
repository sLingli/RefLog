package com.example.myapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MatchSummaryScreen(
    isHistory: Boolean,
    durationMinutes: Int,
    homeGoals: Int,
    awayGoals: Int,
    yellowCount: Int,
    redCount: Int,
    stoppageTime1: String,
    stoppageTime2: String,
    events: List<MatchEvent>,
    onClose: () -> Unit
) {
    // Use LazyColumn to avoid infinite height constraint issues in Dialog
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp) // Limit max height for watch screen
            .background(Color.Black)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_trophy),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(0xFF4CAF50)),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 6.dp)
                )
                Text(
                    text = stringResource(id = if (isHistory) R.string.title_history_details else R.string.title_summary),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        // Stats Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Duration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_history),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color(0xFFAAAAAA)),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.summary_duration, durationMinutes),
                        fontSize = 12.sp,
                        color = Color(0xFFCCCCCC)
                    )
                }

                // Score
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sports_soccer),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.summary_score, homeGoals, awayGoals),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Cards (Yellow & Red)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Yellow Cards
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_card),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(Color(0xFFFFEB3B)),
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 2.dp)
                        )
                        Text(
                            text = yellowCount.toString(),
                            fontSize = 12.sp,
                            color = Color(0xFFFFEB3B)
                        )
                    }

                    // Red Cards
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_card),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(Color(0xFFF44336)),
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 2.dp)
                        )
                        Text(
                            text = redCount.toString(),
                            fontSize = 12.sp,
                            color = Color(0xFFF44336)
                        )
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF333333))
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Stoppage Time
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.more_time),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color(0xFF00FF00)),
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.summary_stoppage, stoppageTime1, stoppageTime2),
                        fontSize = 10.sp,
                        color = Color(0xFF00FF00)
                    )
                }
            }
        }

        // Event Details Label
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_event_note),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color(0xFF4CAF50)),
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 4.dp)
                )
                Text(
                    text = stringResource(id = R.string.label_event_details),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        // Events List
        if (events.isEmpty()) {
            item {
                Text(
                    text = stringResource(id = R.string.msg_no_events),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        } else {
            items(events) { event ->
                EventRow(event)
            }
        }

        // Close Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.btn_ok),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun EventRow(event: MatchEvent) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        val eventGoal = stringResource(id = R.string.event_goal)
        val eventYellow = stringResource(id = R.string.event_yellow)
        val eventRed = stringResource(id = R.string.event_red)

        val iconRes = when (event.event) {
            eventGoal -> R.drawable.sports_soccer
            eventYellow, eventRed -> R.drawable.ic_card
            else -> R.drawable.ic_event_note
        }

        val iconTint = when (event.event) {
            eventGoal -> Color.White
            eventYellow -> Color(0xFFFFEB3B)
            eventRed -> Color(0xFFF44336)
            else -> Color(0xFFAAAAAA)
        }

        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconTint),
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = "${event.timeStr} ${event.event} ${event.detail}",
            color = Color.White,
            fontSize = 11.sp
        )
    }
}

@Preview(device = androidx.wear.tooling.preview.devices.WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun MatchSummaryScreenPreview() {
    MatchSummaryScreen(
        isHistory = false,
        durationMinutes = 90,
        homeGoals = 2,
        awayGoals = 1,
        yellowCount = 3,
        redCount = 1,
        stoppageTime1 = "3:00",
        stoppageTime2 = "5:00",
        events = listOf(
            MatchEvent("15'", "Goal", "⚽", "Home Team", "1", 15),
            MatchEvent("30'", "Yellow", "🟨", "Player A", "1", 30),
            MatchEvent("75'", "Red", "🟥", "Player B", "2", 75),
            MatchEvent("88'", "Goal", "⚽", "Away Team", "2", 88)
        ),
        onClose = {}
    )
}

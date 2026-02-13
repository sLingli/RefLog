package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.MaterialTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MatchSummaryActivity : ComponentActivity() {

    companion object {
        const val EXTRA_IS_HISTORY = "is_history"
        const val EXTRA_DURATION_MINUTES = "duration_minutes"
        const val EXTRA_HOME_GOALS = "home_goals"
        const val EXTRA_AWAY_GOALS = "away_goals"
        const val EXTRA_YELLOW_COUNT = "yellow_count"
        const val EXTRA_RED_COUNT = "red_count"
        const val EXTRA_STOPPAGE_TIME_1 = "stoppage_time_1"
        const val EXTRA_STOPPAGE_TIME_2 = "stoppage_time_2"
        const val EXTRA_EVENTS_JSON = "events_json"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make activity fullscreen with black background
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)

        val isHistory = intent.getBooleanExtra(EXTRA_IS_HISTORY, false)
        val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 45)
        val homeGoals = intent.getIntExtra(EXTRA_HOME_GOALS, 0)
        val awayGoals = intent.getIntExtra(EXTRA_AWAY_GOALS, 0)
        val yellowCount = intent.getIntExtra(EXTRA_YELLOW_COUNT, 0)
        val redCount = intent.getIntExtra(EXTRA_RED_COUNT, 0)
        val stoppageTime1 = intent.getStringExtra(EXTRA_STOPPAGE_TIME_1) ?: "00:00"
        val stoppageTime2 = intent.getStringExtra(EXTRA_STOPPAGE_TIME_2) ?: "00:00"
        val eventsJson = intent.getStringExtra(EXTRA_EVENTS_JSON) ?: "[]"

        val events: List<MatchEvent> = try {
            val gson = Gson()
            val type = object : TypeToken<List<MatchEvent>>() {}.type
            gson.fromJson(eventsJson, type)
        } catch (e: Exception) {
            emptyList()
        }

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                MaterialTheme {
                    MatchSummaryScreen(
                        isHistory = isHistory,
                        durationMinutes = durationMinutes,
                        homeGoals = homeGoals,
                        awayGoals = awayGoals,
                        yellowCount = yellowCount,
                        redCount = redCount,
                        stoppageTime1 = stoppageTime1,
                        stoppageTime2 = stoppageTime2,
                        events = events,
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}


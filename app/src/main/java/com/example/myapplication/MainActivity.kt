package com.example.myapplication

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.wear.compose.material.MaterialTheme
import com.example.myapplication.ui.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // ==================== 状态常量（保留，供内部逻辑使用） ====================
    private companion object {
        const val STATE_READY = MATCH_STATE_READY
        const val STATE_RUNNING = MATCH_STATE_RUNNING
        const val STATE_PAUSED = MATCH_STATE_PAUSED
        const val STATE_HALFTIME = MATCH_STATE_HALFTIME
        const val STATE_FINISHED = MATCH_STATE_FINISHED

        const val HALF_FIRST_VAL = HALF_FIRST
        const val HALF_BREAK_VAL = HALF_BREAK
        const val HALF_SECOND_VAL = HALF_SECOND
        const val DEFAULT_HALF_TIME = 45
    }

    // ==================== Compose 可观察状态 ====================
    // 这些变量驱动 UI 重组
    private var matchState by mutableStateOf(STATE_READY)
    private var currentHalf by mutableStateOf(HALF_FIRST_VAL)
    private var mainTimeFormatted by mutableStateOf("00:00")
    private var stoppageTimeFormatted by mutableStateOf("00:00")
    private var homeTeamColor by mutableStateOf(ComposeColor(0xFF1565C0))
    private var awayTeamColor by mutableStateOf(ComposeColor(0xFFC62828))
    private var halfTimeSet by mutableIntStateOf(DEFAULT_HALF_TIME)

    // ==================== 内部计时状态（不直接驱动 UI） ====================
    private var mainTime: Long = 0
    private var stoppageTime: Long = 0
    private var firstHalfStoppage: Long = 0
    private var lastUpdateTime: Long = 0
    private var halfTimeSeconds: Long = DEFAULT_HALF_TIME * 60L
    private var matchTimeSet: Boolean = false
    private var halfTimeAlertShown: Boolean = false
    private var fullTimeAlertShown: Boolean = false

    // 事件记录
    private val matchEvents = mutableListOf<MatchEvent>()

    // 定时器
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    // 事件选择临时变量
    private var pendingEventType: String = ""
    private var selectedTeam: String = ""

    // 数据管理
    private lateinit var recordManager: MatchRecordManager

    // Activity Result
    private val timeSettingLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedMinutes = result.data?.getIntExtra("SELECTED_TIME", 45) ?: 45
            halfTimeSeconds = selectedMinutes * 60L
            halfTimeSet = selectedMinutes
            matchTimeSet = true
            addLog("⚙️ 比赛时间调整为: $selectedMinutes 分钟")
        }
    }

    // ==================== 生命周期 ====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recordManager = MatchRecordManager(this)

        setContent {
            MaterialTheme {
                MainScreen(
                    matchState = matchState,
                    currentHalf = currentHalf,
                    mainTimeFormatted = mainTimeFormatted,
                    stoppageTimeFormatted = stoppageTimeFormatted,
                    homeTeamColor = homeTeamColor,
                    awayTeamColor = awayTeamColor,
                    halfTimeSet = halfTimeSet,
                    onToggleTimer = { toggleTimer() },
                    onEndHalf = { endCurrentHalf() },
                    onOpenSettings = { showSettingsDialog() },
                    onSetMatchTime = {
                        val intent = android.content.Intent(this, TimeSelectionActivity::class.java)
                        timeSettingLauncher.launch(intent)
                    },
                    onHomeColorClick = { showColorSelectionDialog(isHome = true) },
                    onAwayColorClick = { showColorSelectionDialog(isHome = false) }
                )
            }
        }

        resetMatch()
        initializeTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }

    // ==================== 计时器 ====================

    private fun initializeTimer() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateTimer()
                handler.postDelayed(this, 100)
            }
        }
        handler.post(updateRunnable)
        Log.i("FootballTimer", "⏱️ 计时器已初始化")
    }

    private fun startUpdateLoop() {
        handler.removeCallbacks(updateRunnable)
        updateRunnable = object : Runnable {
            override fun run() {
                updateTimer()
                handler.postDelayed(this, 100)
            }
        }
        handler.post(updateRunnable)
    }

    private fun updateTimer() {
        val currentTime = System.currentTimeMillis()

        if (lastUpdateTime > 0 && (currentTime - lastUpdateTime) >= 1000) {
            if (matchState == STATE_RUNNING || matchState == STATE_PAUSED) {
                mainTime++
                if (matchState == STATE_PAUSED) {
                    stoppageTime++
                }

                runOnUiThread {
                    mainTimeFormatted = formatTime(mainTime)
                    stoppageTimeFormatted = formatTime(stoppageTime)
                }

                checkTimeAlerts()
                Log.d("计时器", "状态: $matchState, 主时间: ${formatTime(mainTime)}, 补时: ${formatTime(stoppageTime)}")
            }
            lastUpdateTime = currentTime
        } else if (lastUpdateTime == 0L) {
            lastUpdateTime = currentTime
        }
    }

    private fun checkTimeAlerts() {
        val halfTimeMin = halfTimeSeconds / 60

        when (currentHalf) {
            HALF_FIRST_VAL -> {
                if (mainTime >= halfTimeSeconds && !halfTimeAlertShown) {
                    halfTimeAlertShown = true
                    triggerAlert("${halfTimeMin}分钟", "准备中场休息")
                }
            }
            HALF_SECOND_VAL -> {
                val targetTime = halfTimeSeconds * 2
                if (mainTime >= targetTime && !fullTimeAlertShown) {
                    fullTimeAlertShown = true
                    triggerAlert("${halfTimeMin * 2}分钟", "准备结束比赛")
                }
            }
        }
    }

    private fun triggerAlert(timeStr: String, message: String) {
        Log.i("FootballTimer", "\n🔔🔔🔔🔔🔔🔔🔔🔔🔔🔔")
        Log.i("FootballTimer", "⏰ ${timeStr}到！$message")
        Log.i("FootballTimer", "🔔🔔🔔🔔🔔🔔🔔🔔🔔🔔\n")
        addLog("⏰ ${timeStr}到 - $message")
    }

    // ==================== 状态机 ====================

    private fun toggleTimer() {
        Log.d("状态机", "toggleTimer - 当前状态: $matchState, 当前半场: $currentHalf")
        when (matchState) {
            STATE_READY -> {
                Log.d("状态机", "从READY开始")
                startTimer()
            }
            STATE_RUNNING -> {
                Log.d("状态机", "从RUNNING暂停")
                pauseTimer()
            }
            STATE_PAUSED -> {
                Log.d("状态机", "从PAUSED继续")
                resumeTimer()
            }
            STATE_HALFTIME -> {
                Log.d("状态机", "从中场休息开始下半场")
                startSecondHalf()
            }
            STATE_FINISHED -> {
                Log.d("状态机", "比赛结束，重新开始")
                resetMatch()
            }
        }
    }

    private fun startTimer() {
        matchState = STATE_RUNNING
        lastUpdateTime = System.currentTimeMillis()
        addLog("🏁 比赛开始")
    }

    private fun pauseTimer() {
        matchState = STATE_PAUSED
        // 显示事件选择弹窗
        showEventDialog()
    }

    private fun resumeTimer() {
        matchState = STATE_RUNNING
    }

    private fun startSecondHalf() {
        currentHalf = HALF_SECOND_VAL
        mainTime = halfTimeSeconds
        matchState = STATE_RUNNING
        stoppageTime = 0
        lastUpdateTime = System.currentTimeMillis()
        fullTimeAlertShown = false

        mainTimeFormatted = formatTime(mainTime)
        stoppageTimeFormatted = formatTime(stoppageTime)
        startUpdateLoop()

        addLog("🏁 下半场开始 - 从 ${formatTime(mainTime)} 继续计时")
        Log.i("FootballTimer", "📢 下半场开始！从 ${formatTime(mainTime)} 计时")
    }

    private fun endFirstHalf() {
        matchState = STATE_HALFTIME
        currentHalf = HALF_BREAK_VAL
        firstHalfStoppage = stoppageTime

        mainTimeFormatted = formatTime(mainTime)

        val stoppageStr = formatTime(stoppageTime)
        addLog("📊 上半场结束 | 比赛: ${formatTime(mainTime)} | 补时: $stoppageStr")
        Log.i("FootballTimer", "📊 上半场总结：比赛时间: ${formatTime(mainTime)}, 补时: $stoppageStr")

        // 重置补时计时器准备下半场
        stoppageTime = 0
        stoppageTimeFormatted = formatTime(stoppageTime)
        halfTimeAlertShown = false
    }

    private fun endSecondHalf() {
        matchState = STATE_FINISHED

        mainTimeFormatted = formatTime(mainTime)

        val stoppageStr = formatTime(stoppageTime)
        val firstHalfStr = formatTime(firstHalfStoppage)
        val totalStoppage = stoppageTime + firstHalfStoppage
        val totalStr = formatTime(totalStoppage)

        addLog("🏆 比赛结束")
        addLog("📊 上半场补时: $firstHalfStr")
        addLog("📊 下半场补时: $stoppageStr")
        addLog("📊 总补时: $totalStr")

        saveMatchRecord()
        showMatchSummary()

        Log.i("FootballTimer", "📢 比赛结束！总补时: $totalStr")
    }

    private fun endCurrentHalf() {
        when (currentHalf) {
            HALF_FIRST_VAL -> endFirstHalf()
            HALF_SECOND_VAL -> endSecondHalf()
        }
    }

    private fun resetMatch() {
        matchState = STATE_READY
        currentHalf = HALF_FIRST_VAL
        mainTime = 0
        stoppageTime = 0
        firstHalfStoppage = 0
        halfTimeSeconds = DEFAULT_HALF_TIME * 60L
        halfTimeSet = DEFAULT_HALF_TIME
        matchTimeSet = false
        halfTimeAlertShown = false
        fullTimeAlertShown = false
        matchEvents.clear()

        mainTimeFormatted = "00:00"
        stoppageTimeFormatted = "00:00"

        Log.i("FootballTimer", "🔄 比赛已重置")
    }

    // ==================== 事件记录 ====================

    private fun recordSimpleEvent(eventType: String, emoji: String, stoppageSeconds: Int) {
        val timeStr = formatTime(mainTime)
        val halfName = if (currentHalf == HALF_FIRST_VAL) getString(R.string.status_first_half) else getString(R.string.status_second_half)
        val minute = (mainTime / 60).toInt()

        matchEvents.add(MatchEvent(
            timeStr = timeStr,
            event = eventType,
            emoji = emoji,
            detail = "",
            half = halfName,
            minute = minute
        ))
        addLog("$emoji [$timeStr] $eventType")
    }

    private fun recordEventWithDetails(eventType: String, team: String, number: String) {
        val emoji = when (eventType) {
            getString(R.string.event_yellow) -> "🟨"
            getString(R.string.event_red) -> "🟥"
            getString(R.string.event_goal) -> "⚽"
            else -> "📝"
        }
        val teamEmoji = if (team == getString(R.string.team_home)) "🏠" else "✈️"
        val detailText = "$team #$number"
        val timeStr = formatTime(mainTime)
        val halfName = if (currentHalf == HALF_FIRST_VAL) getString(R.string.status_first_half) else getString(R.string.status_second_half)
        val minute = (mainTime / 60).toInt()

        matchEvents.add(MatchEvent(
            timeStr = timeStr,
            event = eventType,
            emoji = emoji,
            detail = detailText,
            half = halfName,
            minute = minute
        ))

        stoppageTimeFormatted = formatTime(stoppageTime)
        addLog("$emoji [$timeStr] $eventType - $teamEmoji $detailText")
    }

    private fun saveMatchRecord() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val homeGoals = matchEvents.count { it.event == getString(R.string.event_goal) && it.detail.contains(getString(R.string.team_home)) }
        val awayGoals = matchEvents.count { it.event == getString(R.string.event_goal) && it.detail.contains(getString(R.string.team_away)) }

        val record = MatchRecord(
            date = currentDate,
            halfTimeMinutes = (halfTimeSeconds / 60).toInt(),
            firstHalfStoppage = formatTime(firstHalfStoppage),
            secondHalfStoppage = formatTime(stoppageTime),
            totalStoppage = formatTime(firstHalfStoppage + stoppageTime),
            goalCount = matchEvents.count { it.event == getString(R.string.event_goal) },
            yellowCount = matchEvents.count { it.event == getString(R.string.event_yellow) },
            redCount = matchEvents.count { it.event == getString(R.string.event_red) },
            substitutionCount = matchEvents.count { it.event == getString(R.string.event_substitute) },
            injuryCount = matchEvents.count { it.event == getString(R.string.event_injury) },
            events = matchEvents.toList(),
            homeGoals = homeGoals,
            awayGoals = awayGoals
        )

        recordManager.saveRecord(record)
        Log.i("FootballTimer", "📁 比赛记录已保存: 主队 $homeGoals - $awayGoals 客队")
    }

    // ==================== 弹窗（保持原有 Dialog 方式） ====================

    private fun showEventDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeViewModelStoreOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                MaterialTheme {
                    EventSelectionDialog(
                        onEventSelected = { eventType ->
                            dialog.dismiss()
                            when (eventType) {
                                EventType.YELLOW_CARD -> showTeamSelectionDialog(getString(R.string.event_yellow))
                                EventType.RED_CARD -> showTeamSelectionDialog(getString(R.string.event_red))
                                EventType.GOAL -> showTeamSelectionDialog(getString(R.string.event_goal))
                                EventType.INJURY -> recordSimpleEvent(getString(R.string.event_injury), " ", 30)
                                EventType.SUBSTITUTION -> recordSimpleEvent(getString(R.string.event_substitute), " ", 30)
                            }
                        },
                        onDismiss = { dialog.dismiss() }
                    )
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showTeamSelectionDialog(eventType: String) {
        pendingEventType = eventType
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeViewModelStoreOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                MaterialTheme {
                    TeamSelectionDialogCompose(
                        onHomeTeamSelected = {
                            selectedTeam = getString(R.string.team_home)
                            dialog.dismiss()
                            showNumberSelectionDialog(eventType, selectedTeam)
                        },
                        onAwayTeamSelected = {
                            selectedTeam = getString(R.string.team_away)
                            dialog.dismiss()
                            showNumberSelectionDialog(eventType, selectedTeam)
                        },
                        onDismiss = { dialog.dismiss() },
                        homeTeamColor = homeTeamColor,
                        awayTeamColor = awayTeamColor
                    )
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.show()
    }

    private fun showNumberSelectionDialog(eventType: String, team: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeViewModelStoreOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                MaterialTheme {
                    NumberSelectionDialog(
                        initialNumber = 10,
                        title = eventType,
                        onNumberConfirmed = { number ->
                            val numberStr = String.format(Locale.US, "%02d", number)
                            dialog.dismiss()
                            recordEventWithDetails(eventType, team, numberStr)
                        }
                    )
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun showColorSelectionDialog(isHome: Boolean) {
        val initialColorCompose = if (isHome) homeTeamColor else awayTeamColor
        val initialColor = initialColorCompose.hashCode()

        val rootView = window.decorView.findViewById<FrameLayout>(android.R.id.content)
        val overlayView = android.view.View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0x80000000.toInt())
            isClickable = true
            isFocusable = true
        }
        val composeView = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeViewModelStoreOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
        }

        val dismissDialog: () -> Unit = {
            rootView.removeView(composeView)
            rootView.removeView(overlayView)
        }
        overlayView.setOnClickListener { dismissDialog() }

        composeView.setContent {
            ColorSelectionDialog(
                initialColor = initialColor,
                onColorSelected = { selectedColor ->
                    val newColor = ComposeColor(selectedColor)
                    if (isHome) {
                        homeTeamColor = newColor
                    } else {
                        awayTeamColor = newColor
                    }
                    dismissDialog()
                }
            )
        }

        rootView.addView(overlayView)
        rootView.addView(composeView)
    }

    private fun showMatchSummary(isHistory: Boolean = false, historyRecord: MatchRecord? = null) {
        val hTime: Int = if (isHistory) {
            historyRecord?.halfTimeMinutes ?: 0
        } else {
            (halfTimeSeconds / 60L).toInt()
        }
        val st1Str = if (isHistory) historyRecord?.firstHalfStoppage ?: "00:00" else formatTime(firstHalfStoppage)
        val st2Str = if (isHistory) historyRecord?.secondHalfStoppage ?: "00:00" else formatTime(stoppageTime)
        val eventsToShow: List<MatchEvent> = if (isHistory) historyRecord?.events ?: listOf() else matchEvents.toList()

        val homeGoals = eventsToShow.count { it.event == getString(R.string.event_goal) && it.detail.contains(getString(R.string.team_home)) }
        val awayGoals = eventsToShow.count { it.event == getString(R.string.event_goal) && it.detail.contains(getString(R.string.team_away)) }
        val yellowCount = eventsToShow.count { it.event == getString(R.string.event_yellow) }
        val redCount = eventsToShow.count { it.event == getString(R.string.event_red) }

        val intent = android.content.Intent(this, MatchSummaryActivity::class.java).apply {
            putExtra(MatchSummaryActivity.EXTRA_IS_HISTORY, isHistory)
            putExtra(MatchSummaryActivity.EXTRA_DURATION_MINUTES, hTime)
            putExtra(MatchSummaryActivity.EXTRA_HOME_GOALS, homeGoals)
            putExtra(MatchSummaryActivity.EXTRA_AWAY_GOALS, awayGoals)
            putExtra(MatchSummaryActivity.EXTRA_YELLOW_COUNT, yellowCount)
            putExtra(MatchSummaryActivity.EXTRA_RED_COUNT, redCount)
            putExtra(MatchSummaryActivity.EXTRA_STOPPAGE_TIME_1, st1Str)
            putExtra(MatchSummaryActivity.EXTRA_STOPPAGE_TIME_2, st2Str)
            val gson = com.google.gson.Gson()
            putExtra(MatchSummaryActivity.EXTRA_EVENTS_JSON, gson.toJson(eventsToShow))
        }
        startActivity(intent)
    }

    private fun showSettingsDialog() {
        if (supportFragmentManager.findFragmentByTag(SettingsBottomSheetFragment.TAG) != null) return
        SettingsBottomSheetFragment().show(supportFragmentManager, SettingsBottomSheetFragment.TAG)
    }

    // ==================== 工具方法 ====================

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
    }

    private fun addLog(message: String) {
        val currentTime = formatTime(mainTime)
        val halfIndicator = when (currentHalf) {
            HALF_FIRST_VAL -> "H1"
            HALF_SECOND_VAL -> "H2"
            else -> "--"
        }
        Log.d("FootballTimer", "[$halfIndicator $currentTime] $message")
    }
}

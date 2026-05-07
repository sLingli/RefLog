package com.example.myapplication

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 足球比赛计时器主界面（Compose 版本）
 *
 * 状态机流程：READY → RUNNING ↔ PAUSED → HALFTIME → FINISHED
 * 支持双半场计时、补时统计、事件记录（黄牌/红牌/进球/伤停/换人）及比赛历史管理
 *
 * UI 完全由 Compose 驱动：MainScreen + HorizontalPager + BottomNavBar
 */
class MainActivity : AppCompatActivity() {

    // region 状态常量
    private companion object {
        const val STATE_READY = "ready"
        const val STATE_RUNNING = "running"
        const val STATE_PAUSED = "paused"
        const val STATE_HALFTIME = "halftime"
        const val STATE_FINISHED = "finished"

        const val HALF_FIRST = "code_first_half"
        const val HALF_BREAK = "code_halftime"
        const val HALF_SECOND = "code_second_half"
        const val DEFAULT_HALF_TIME = 45
    }

    // region 计时器变量
    private var state: String = STATE_READY
    private var currentHalf: String = HALF_FIRST
    private lateinit var recordManager: MatchRecordManager

    // Dashboard ViewModel
    private val dashboardViewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(MatchRecordManager(this))
    }

    private var mainTime: Long = 0
    private var stoppageTime: Long = 0
    private var firstHalfStoppage: Long = 0
    private var lastUpdateTime: Long = 0

    private var halfTimeSeconds: Long = DEFAULT_HALF_TIME * 60L
    private var matchTimeSet: Boolean = false

    private var halfTimeAlertShown: Boolean = false
    private var fullTimeAlertShown: Boolean = false

    private val matchEvents = mutableListOf<MatchEvent>()

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    private var pendingEventType: String = ""
    private var selectedTeam: String = ""

    private var homeTeamColor: Int = 0xFF1565C0.toInt()
    private var awayTeamColor: Int = 0xFFC62828.toInt()

    // region Compose UI 状态（驱动 MainScreen）
    private var timerStateCompose by mutableStateOf(STATE_READY)
    private var statusTextCompose by mutableStateOf("")
    private var statusColorCompose by mutableStateOf(Color(0xFF4CAF50))
    private var statusIconResCompose by mutableStateOf(R.drawable.sports_soccer)
    private var mainTimeTextCompose by mutableStateOf("00:00")
    private var mainTimeColorCompose by mutableStateOf(Color(0xFF4CAF50))
    private var stoppageTimeTextCompose by mutableStateOf("00:00")
    private var stoppageActiveCompose by mutableStateOf(false)
    private var showEndHalfButtonCompose by mutableStateOf(false)
    private var historyRecordsCompose by mutableStateOf<List<MatchRecord>>(emptyList())

    // region Compose 弹窗状态
    private var showTeamSelectionDialogState by mutableStateOf(false)
    private var currentEventType by mutableStateOf(EventType.YELLOW_CARD)
    private var showTimeSettingDialogState by mutableStateOf(false)
    private var showThemeSelectionDialogState by mutableStateOf(false)
    private var currentAppTheme by mutableStateOf(AppTheme.DARK_GREEN) // 临时默认，onCreate 中更新
    private var showLanguageSelectionDialogState by mutableStateOf(false)
    private var currentLanguage by mutableStateOf(AppLanguage.DEFAULT)
    private var showEventSelectionDialogState by mutableStateOf(false)
    private var showMatchSummaryDialogState by mutableStateOf(false)
    private var showColorSelectionDialogState by mutableStateOf(false)
    private var showNumberSelectionDialogState by mutableStateOf(false)
    private var numberSelectionEventType by mutableStateOf("")
    private var numberSelectionTeam by mutableStateOf("")
    private var numberSelectionTeamColor by mutableStateOf(0)
    private var numberSelectionEventIconInfo by mutableStateOf(EventIconInfo(R.drawable.ic_card, Color.White))

    // MatchSummary 数据状态
    private var matchSummaryIsHistory by mutableStateOf(false)
    private var matchSummaryHalfTimeMinutes by mutableStateOf(45)
    private var matchSummaryHomeGoals by mutableStateOf(0)
    private var matchSummaryAwayGoals by mutableStateOf(0)
    private var matchSummaryYellowCount by mutableStateOf(0)
    private var matchSummaryRedCount by mutableStateOf(0)
    private var matchSummaryFirstHalfStoppage by mutableStateOf("00:00")
    private var matchSummarySecondHalfStoppage by mutableStateOf("00:00")
    private var matchSummaryEvents by mutableStateOf<List<MatchEvent>>(emptyList())

    // region 生命周期

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        currentAppTheme = ThemeManager.currentTheme // 从持久化读取实际主题
        LanguageManager.init(this)
        currentLanguage = LanguageManager.currentLanguage
        recordManager = MatchRecordManager(this)
        initializeTimer()
        updateAllComposeState()

        // 使用 Compose 主屏幕替代 XML 布局
        setContent {
            RefLogTheme(theme = currentAppTheme) {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
                ) {
                    composable("home") {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Dashboard 状态
                            val dashboardState by dashboardViewModel.state.collectAsState()

                            // 主屏幕（含 Pager + BottomNav）
                            MainScreen(
                                // Dashboard 数据
                                dashboardState = dashboardState,
                                onDashboardStartTimer = {
                                    // 跳转到计时器页
                                },
                                onDashboardEventPreset = {
                                    // 赛事预设（占位）
                                },
                                onDashboardRecordClick = { record ->
                                    showMatchSummary(isHistory = true, historyRecord = record)
                                },

                                timerState = timerStateCompose,
                                currentHalf = currentHalf,
                                statusText = statusTextCompose,
                                statusColor = statusColorCompose,
                                statusIconRes = statusIconResCompose,
                                mainTimeText = mainTimeTextCompose,
                                mainTimeColor = mainTimeColorCompose,
                                stoppageTimeText = stoppageTimeTextCompose,
                                stoppageActive = stoppageActiveCompose,
                                showEndHalfButton = showEndHalfButtonCompose,
                                onTimerMainButtonClick = { toggleTimer() },
                                onEndHalfButtonLongPress = { onEndHalfButtonLongPress() },
                                historyRecords = historyRecordsCompose,
                                onHistoryRecordClick = { record ->
                                    showMatchSummary(isHistory = true, historyRecord = record)
                                },
                                onHistoryDeleteRecord = { record ->
                                    recordManager.deleteRecord(record.id)
                                    historyRecordsCompose = recordManager.getAllRecords()
                                    dashboardViewModel.refresh()
                                },
                                onHistoryClearAll = {
                                    recordManager.clearAllRecords()
                                    historyRecordsCompose = recordManager.getAllRecords()
                                    dashboardViewModel.refresh()
                                },
                                onThemeClick = { showThemeSelectionDialogState = true },
                                onLanguageClick = { showLanguageSelectionDialogState = true },
                                onSettingsClick = { showColorSelectionDialog() },
                                onAboutClick = { navController.navigate("about") },
                                onPageChanged = { page ->
                                    if (page == 0) {
                                        dashboardViewModel.refresh()
                                    }
                                    if (page == 2) {
                                        historyRecordsCompose = recordManager.getAllRecords()
                                    }
                                }
                            )

                            // 弹窗覆盖层（替代原 ComposeView composeDialogContainer）
                            DialogOverlay()
                        }
                    }
                    composable(
                        route = "about",
                        enterTransition = { slideInHorizontally { it } },
                        exitTransition = { slideOutHorizontally { -it } },
                        popEnterTransition = { slideInHorizontally { -it } },
                        popExitTransition = { slideOutHorizontally { it } }
                    ) {
                        AboutScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }

    // region 计时器初始化

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



    // region 弹窗覆盖层

    /**
     * 弹窗覆盖层 - 替代原 initializeComposeDialogs() 中的 ComposeView
     */
    @androidx.compose.runtime.Composable
    private fun DialogOverlay() {
        // 主题选择弹窗
        if (showThemeSelectionDialogState) {
            ThemeSelectionDialog(
                currentTheme = currentAppTheme,
                onDismiss = { showThemeSelectionDialogState = false },
                onThemeSelected = { theme ->
                    ThemeManager.currentTheme = theme
                    currentAppTheme = theme  // 触发 Compose 重组 + 自动颜色动画
                    showThemeSelectionDialogState = false
                }
            )
        }

        // 语言选择弹窗
        if (showLanguageSelectionDialogState) {
            LanguageSelectionDialog(
                currentLanguage = currentLanguage,
                onDismiss = { showLanguageSelectionDialogState = false },
                onLanguageSelected = { language ->
                    LanguageManager.currentLanguage = language
                    currentLanguage = language
                    showLanguageSelectionDialogState = false
                    LanguageManager.applyLanguage(this)
                }
            )
        }

        // 事件选择弹窗
        if (showEventSelectionDialogState) {
            EventSelectionDialog(
                onDismiss = { showEventSelectionDialogState = false },
                onEventSelected = { eventType ->
                    showEventSelectionDialogState = false
                    when (eventType) {
                        EventType.YELLOW_CARD -> showTeamSelectionDialog(getString(R.string.event_yellow))
                        EventType.RED_CARD -> showTeamSelectionDialog(getString(R.string.event_red))
                        EventType.GOAL -> showTeamSelectionDialog(getString(R.string.event_goal))
                        EventType.INJURY -> recordSimpleEvent(getString(R.string.event_injury), " ", 30)
                        EventType.SUBSTITUTION -> recordSimpleEvent(getString(R.string.event_substitute), " ", 30)
                        EventType.CANCEL -> { }
                    }
                }
            )
        }

        // 队伍选择弹窗
        if (showTeamSelectionDialogState) {
            TeamSelectionDialog(
                eventType = currentEventType,
                eventTitle = getEventTypeTitle(currentEventType),
                homeTeamColor = homeTeamColor,
                awayTeamColor = awayTeamColor,
                onDismiss = { showTeamSelectionDialogState = false },
                onTeamSelected = { selection ->
                    showTeamSelectionDialogState = false
                    when (selection) {
                        TeamSelection.HOME -> {
                            selectedTeam = getString(R.string.team_home)
                            numberSelectionEventType = pendingEventType
                            numberSelectionTeam = selectedTeam
                            numberSelectionTeamColor = homeTeamColor
                            numberSelectionEventIconInfo = getEventIconInfo(currentEventType)
                            showNumberSelectionDialogState = true
                        }
                        TeamSelection.AWAY -> {
                            selectedTeam = getString(R.string.team_away)
                            numberSelectionEventType = pendingEventType
                            numberSelectionTeam = selectedTeam
                            numberSelectionTeamColor = awayTeamColor
                            numberSelectionEventIconInfo = getEventIconInfo(currentEventType)
                            showNumberSelectionDialogState = true
                        }
                        TeamSelection.CANCEL -> { }
                    }
                }
            )
        }

        // 颜色选择弹窗
        if (showColorSelectionDialogState) {
            ColorSelectionDialog(
                homeTeamColor = homeTeamColor,
                awayTeamColor = awayTeamColor,
                onDismiss = { showColorSelectionDialogState = false },
                onConfirm = { home, away ->
                    homeTeamColor = home
                    awayTeamColor = away
                    showColorSelectionDialogState = false
                    showTimeSettingDialog()
                }
            )
        }

        // 时间设置弹窗
        if (showTimeSettingDialogState) {
            TimeSettingDialog(
                initialMinutes = 45,
                onDismiss = { showTimeSettingDialogState = false },
                onResult = { result ->
                    showTimeSettingDialogState = false
                    when (result) {
                        is TimeSettingResult.Confirmed -> {
                            halfTimeSeconds = result.minutes * 60L
                            matchTimeSet = true
                            startTimer()
                        }
                        is TimeSettingResult.Cancelled -> { }
                    }
                }
            )
        }

        // 号码选择弹窗
        if (showNumberSelectionDialogState) {
            NumberSelectionDialog(
                eventType = numberSelectionEventType,
                team = numberSelectionTeam,
                teamColor = numberSelectionTeamColor,
                eventIconInfo = numberSelectionEventIconInfo,
                onDismiss = { showNumberSelectionDialogState = false },
                onResult = { result ->
                    showNumberSelectionDialogState = false
                    when (result) {
                        is NumberSelectionResult.Confirmed -> {
                            recordEventWithDetails(numberSelectionEventType, numberSelectionTeam, result.number)
                        }
                        is NumberSelectionResult.Cancelled -> { }
                    }
                }
            )
        }

        // 比赛总结弹窗
        if (showMatchSummaryDialogState) {
            MatchSummaryDialog(
                isHistory = matchSummaryIsHistory,
                halfTimeMinutes = matchSummaryHalfTimeMinutes,
                homeGoals = matchSummaryHomeGoals,
                awayGoals = matchSummaryAwayGoals,
                yellowCount = matchSummaryYellowCount,
                redCount = matchSummaryRedCount,
                firstHalfStoppage = matchSummaryFirstHalfStoppage,
                secondHalfStoppage = matchSummarySecondHalfStoppage,
                events = matchSummaryEvents,
                onDismiss = { showMatchSummaryDialogState = false }
            )
        }

    }

    // region 状态机

    private fun toggleTimer() {
        Log.d("状态机", "toggleTimer - 当前状态: $state, 当前半场: $currentHalf")
        when (state) {
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

    private fun onEndHalfButtonLongPress() {
        when (currentHalf) {
            HALF_FIRST -> {
                endFirstHalf()
                updateStatusLabelCompose()
            }
            HALF_SECOND -> {
                endSecondHalf()
                updateStatusLabelCompose()
            }
        }
    }

    private fun startTimer() {
        if (!matchTimeSet) {
            showColorSelectionDialog()
            return
        }

        state = STATE_RUNNING
        lastUpdateTime = System.currentTimeMillis()

        syncComposeState()
        stoppageActiveCompose = false

        addLog("🏁 比赛开始")
        val halfTimeMin = halfTimeSeconds / 60
        Log.i("FootballTimer", "📢 比赛开始！每半场 $halfTimeMin 分钟")
    }

    private fun resumeTimer() {
        state = STATE_RUNNING
        syncComposeState()
        stoppageActiveCompose = false
    }

    private fun pauseTimer() {
        state = STATE_PAUSED
        syncComposeState()
        stoppageActiveCompose = true

        // 显示事件选择弹窗
        showEventSelectionDialogState = true
    }

    private fun startSecondHalf() {
        currentHalf = HALF_SECOND
        mainTime = halfTimeSeconds

        state = STATE_RUNNING
        stoppageTime = 0
        lastUpdateTime = System.currentTimeMillis()
        fullTimeAlertShown = false

        syncComposeState()
        mainTimeTextCompose = formatTime(mainTime)
        mainTimeColorCompose = Color(0xFF00FF00) // 亮绿色标记下半场开始
        stoppageActiveCompose = false
        stoppageTimeTextCompose = formatTime(stoppageTime)

        startUpdateLoop()

        addLog("🏁 下半场开始 - 从 ${formatTime(mainTime)} 继续计时")
        Log.i("FootballTimer", "📢 下半场开始！从 ${formatTime(mainTime)} 计时")
    }

    private fun endFirstHalf() {
        state = STATE_HALFTIME
        currentHalf = HALF_BREAK
        firstHalfStoppage = stoppageTime

        syncComposeState()
        mainTimeTextCompose = formatTime(mainTime)
        mainTimeColorCompose = Color(0xFF888888) // 灰色标记半场结束

        val stoppageStr = formatTime(stoppageTime)
        addLog("📊 上半场结束 | 比赛: ${formatTime(mainTime)} | 补时: $stoppageStr")
        Log.i("FootballTimer", "📊 上半场总结：比赛时间: ${formatTime(mainTime)}, 补时: $stoppageStr")

        // 重置补时计时器准备下半场
        stoppageTime = 0
        halfTimeAlertShown = false
        stoppageTimeTextCompose = formatTime(stoppageTime)
    }

    private fun endSecondHalf() {
        state = STATE_FINISHED

        syncComposeState()
        mainTimeColorCompose = Color(0xFF888888) // 灰色标记比赛结束
        mainTimeTextCompose = formatTime(mainTime)

        val stoppageStr = formatTime(stoppageTime)
        val firstHalfStr = formatTime(firstHalfStoppage)
        val totalStoppage = stoppageTime + firstHalfStoppage
        val totalStr = formatTime(totalStoppage)

        addLog("🏆 比赛结束")
        addLog("📊 上半场补时: $firstHalfStr")
        addLog("📊 下半场补时: $stoppageStr")
        addLog("📊 总补时: $totalStr")

        saveMatchRecord()
        historyRecordsCompose = recordManager.getAllRecords()

        // 自动弹出总结页
        showMatchSummary()

        Log.i("FootballTimer", "📢 比赛结束！总补时: $totalStr")
    }

    private fun resetMatch() {
        state = STATE_READY
        currentHalf = HALF_FIRST
        mainTime = 0
        stoppageTime = 0
        firstHalfStoppage = 0
        halfTimeSeconds = DEFAULT_HALF_TIME * 60L
        matchTimeSet = false
        halfTimeAlertShown = false
        fullTimeAlertShown = false
        matchEvents.clear()

        syncComposeState()
        mainTimeTextCompose = "00:00"
        mainTimeColorCompose = Color(0xFF4CAF50)
        stoppageTimeTextCompose = "00:00"
        stoppageActiveCompose = false

        Log.i("FootballTimer", "📢 比赛已重置")
    }

    // region 计时器核心

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
            if (state == STATE_RUNNING || state == STATE_PAUSED) {
                // 主计时器：只要没吹终场哨，它就一直加
                mainTime++

                // 补时计时器：只有在"暂停"状态下，才记录浪费的时间
                if (state == STATE_PAUSED) {
                    stoppageTime++
                }

                // 更新 Compose 状态
                mainTimeTextCompose = formatTime(mainTime)
                stoppageTimeTextCompose = formatTime(stoppageTime)
                showEndHalfButtonCompose = state == STATE_RUNNING || state == STATE_PAUSED

                checkTimeAlerts()

                Log.d("计时器", "状态: $state, 主时间: ${formatTime(mainTime)}, 补时: ${formatTime(stoppageTime)}")
            }

            lastUpdateTime = currentTime
        } else if (lastUpdateTime == 0L) {
            lastUpdateTime = currentTime
        }
    }

    private fun checkTimeAlerts() {
        val halfTimeMin = halfTimeSeconds / 60

        when (currentHalf) {
            HALF_FIRST -> {
                if (mainTime >= halfTimeSeconds && !halfTimeAlertShown) {
                    halfTimeAlertShown = true
                    triggerAlert("${halfTimeMin}分钟", "准备中场休息")
                    mainTimeColorCompose = Color(0xFFFF9800)
                    statusTextCompose = getString(R.string.status_first_half_stoppage)
                }
            }
            HALF_SECOND -> {
                val targetTime = halfTimeSeconds * 2
                if (mainTime >= targetTime && !fullTimeAlertShown) {
                    fullTimeAlertShown = true
                    triggerAlert("${halfTimeMin * 2}分钟", "准备结束比赛")
                    mainTimeColorCompose = Color(0xFFF44336)
                    statusTextCompose = getString(R.string.status_second_half_stoppage)

                    Log.d("时间提醒", "下半场提醒触发：当前mainTime: ${formatTime(mainTime)}, 目标: ${formatTime(targetTime)}")
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

    // region 事件记录

    private fun recordSimpleEvent(eventType: String, emoji: String, stoppageSeconds: Int) {
        val timeStr = formatTime(mainTime)
        val halfName = if (currentHalf == HALF_FIRST) getString(R.string.status_first_half) else getString(R.string.status_second_half)
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
        val halfName = if (currentHalf == HALF_FIRST) getString(R.string.status_first_half) else getString(R.string.status_second_half)
        val minute = (mainTime / 60).toInt()

        matchEvents.add(MatchEvent(
            timeStr = timeStr,
            event = eventType,
            emoji = emoji,
            detail = detailText,
            half = halfName,
            minute = minute
        ))

        stoppageTimeTextCompose = formatTime(stoppageTime)
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
            firstHalfStoppage = formatTime(firstHalfStoppage.toLong()),
            secondHalfStoppage = formatTime(stoppageTime.toLong()),
            totalStoppage = formatTime((firstHalfStoppage + stoppageTime).toLong()),
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
        dashboardViewModel.refresh()
        Log.i("FootballTimer", "📁 比赛记录已保存: 主队 $homeGoals - $awayGoals 客队")
    }

    // region 弹窗触发

    private fun showTeamSelectionDialog(eventType: String) {
        pendingEventType = eventType
        currentEventType = when (eventType) {
            getString(R.string.event_yellow) -> EventType.YELLOW_CARD
            getString(R.string.event_red) -> EventType.RED_CARD
            getString(R.string.event_goal) -> EventType.GOAL
            else -> EventType.YELLOW_CARD
        }
        showTeamSelectionDialogState = true
    }

    private fun showTimeSettingDialog() {
        showTimeSettingDialogState = true
    }

    private fun showColorSelectionDialog() {
        showColorSelectionDialogState = true
    }

    private fun showMatchSummary(isHistory: Boolean = false, historyRecord: MatchRecord? = null) {
        val eventsToShow: List<MatchEvent> = if (isHistory) {
            historyRecord?.events ?: listOf()
        } else {
            matchEvents
        }

        val homeGoals = eventsToShow.count { it.event == getString(R.string.event_goal) && it.detail.contains(getString(R.string.team_home)) }
        val awayGoals = eventsToShow.count { it.event == getString(R.string.event_goal) && it.detail.contains(getString(R.string.team_away)) }
        val yellowCount = eventsToShow.count { it.event == getString(R.string.event_yellow) }
        val redCount = eventsToShow.count { it.event == getString(R.string.event_red) }

        val hTime: Int = if (isHistory) {
            (historyRecord?.halfTimeMinutes ?: 0).toInt()
        } else {
            (halfTimeSeconds / 60L).toInt()
        }

        val st1 = if (isHistory) {
            historyRecord?.firstHalfStoppage ?: "00:00"
        } else {
            formatTime(firstHalfStoppage.toLong())
        }

        val st2 = if (isHistory) {
            historyRecord?.secondHalfStoppage ?: "00:00"
        } else {
            formatTime(stoppageTime.toLong())
        }

        matchSummaryIsHistory = isHistory
        matchSummaryHalfTimeMinutes = hTime
        matchSummaryHomeGoals = homeGoals
        matchSummaryAwayGoals = awayGoals
        matchSummaryYellowCount = yellowCount
        matchSummaryRedCount = redCount
        matchSummaryFirstHalfStoppage = st1
        matchSummarySecondHalfStoppage = st2
        matchSummaryEvents = eventsToShow
        showMatchSummaryDialogState = true
    }

    // region Compose 状态同步

    /**
     * 将内部状态机状态同步到 Compose UI 状态
     */
    private fun syncComposeState() {
        timerStateCompose = state
        showEndHalfButtonCompose = state == STATE_RUNNING || state == STATE_PAUSED
        updateStatusLabelCompose()
    }

    /**
     * 更新状态标签的 Compose 状态
     */
    private fun updateStatusLabelCompose() {
        when (state) {
            STATE_READY -> {
                statusTextCompose = getString(R.string.status_ready)
                statusIconResCompose = R.drawable.sports_soccer
            }
            STATE_RUNNING, STATE_PAUSED -> {
                statusTextCompose = getHalfText(currentHalf)
                statusIconResCompose = R.drawable.sports_soccer
            }
            STATE_HALFTIME -> {
                statusTextCompose = getString(R.string.status_halftime)
                statusIconResCompose = R.drawable.ic_coffee
            }
            STATE_FINISHED -> {
                statusTextCompose = getString(R.string.status_finished)
                statusIconResCompose = R.drawable.ic_trophy
            }
            else -> {
                statusTextCompose = getString(R.string.status_ready)
                statusIconResCompose = R.drawable.sports_soccer
            }
        }
        statusColorCompose = Color(0xFF4CAF50)
    }

    /**
     * 初始化所有 Compose 状态
     */
    private fun updateAllComposeState() {
        timerStateCompose = state
        updateStatusLabelCompose()
        mainTimeTextCompose = formatTime(mainTime)
        stoppageTimeTextCompose = formatTime(stoppageTime)
        showEndHalfButtonCompose = state == STATE_RUNNING || state == STATE_PAUSED
        historyRecordsCompose = recordManager.getAllRecords()
    }

    // region 工具方法

    private fun getEventTypeTitle(eventType: EventType): String {
        return when (eventType) {
            EventType.YELLOW_CARD -> getString(R.string.event_yellow)
            EventType.RED_CARD -> getString(R.string.event_red)
            EventType.GOAL -> getString(R.string.event_goal)
            EventType.INJURY -> getString(R.string.event_injury)
            EventType.SUBSTITUTION -> getString(R.string.event_substitute)
            EventType.CANCEL -> ""
        }
    }

    private fun getHalfText(code: String): String {
        return when (code) {
            HALF_FIRST -> getString(R.string.status_first_half)
            HALF_BREAK -> getString(R.string.status_halftime)
            HALF_SECOND -> getString(R.string.status_second_half)
            else -> ""
        }
    }

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
    }

    private fun addLog(message: String) {
        val currentTime = formatTime(mainTime)
        val halfIndicator = when (currentHalf) {
            HALF_FIRST -> "H1"
            HALF_SECOND -> "H2"
            else -> "--"
        }
        Log.d("FootballTimer", "[$halfIndicator $currentTime] $message")
    }

    private fun updateMainTimeDisplay() {
        val displayTime = when (currentHalf) {
            HALF_FIRST -> mainTime
            HALF_SECOND -> mainTime
            HALF_BREAK -> mainTime + firstHalfStoppage
            else -> mainTime
        }
        mainTimeTextCompose = formatTime(displayTime)

        if (currentHalf == HALF_SECOND) {
            Log.d("时间显示", "下半场显示：mainTime: ${formatTime(mainTime)}, 原半场时间: ${formatTime(halfTimeSeconds)}")
        }
    }
}

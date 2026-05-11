package com.example.myapplication

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
 * 重构后架构：
 * - 首页 3 Tab（首页 / 历史 / 我的）通过 HorizontalPager
 * - 计时器为独立全屏 NavHost 路由（无底部导航栏）
 * - 赛事预设库为独立 NavHost 路由
 *
 * 状态机流程：READY → RUNNING ↔ PAUSED → HALFTIME → FINISHED
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
    private lateinit var templateManager: MatchTemplateManager

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

    // UnifiedEventBottomSheet 触发状态
    private var showUnifiedEventSheet by mutableStateOf(false)

    // 当前赛事配置（由模板传入）
    private var currentMatchName: String = ""
    private var currentHomeTeamName: String = ""
    private var currentAwayTeamName: String = ""
    private var homeTeamColor: Int = 0xFF1565C0.toInt()
    private var awayTeamColor: Int = 0xFFC62828.toInt()

    // region Compose UI 状态
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
    private var themeConfigState by mutableStateOf(ThemeConfig())
    private var showLanguageSelectionDialogState by mutableStateOf(false)
    private var currentLanguage by mutableStateOf(AppLanguage.DEFAULT)
    private var showMatchSummaryDialogState by mutableStateOf(false)

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
    // 比赛总结确认后回调（用于退出计时器页）
    private var onSummaryConfirmed: (() -> Unit)? = null

    // 个人资料状态
    private var userAvatarUriString: String? by mutableStateOf(null)
    private var userNickname: String by mutableStateOf("")
    private lateinit var profilePrefs: android.content.SharedPreferences

    // region 生命周期

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        themeConfigState = ThemeManager.config
        LanguageManager.init(this)
        currentLanguage = LanguageManager.currentLanguage
        recordManager = MatchRecordManager(this)
        templateManager = MatchTemplateManager(this)

        profilePrefs = getSharedPreferences("user_profile", MODE_PRIVATE)
        userAvatarUriString = profilePrefs.getString("avatar_uri", null)
        userNickname = profilePrefs.getString("nickname", null)
            ?: getString(R.string.default_nickname)

        initializeTimer()
        updateAllComposeState()

        setContent {
            RefLogTheme(config = themeConfigState) {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
                ) {
                    // ═══════════════════════════════════════
                    // 首页（含底部导航栏 + 3 Tab Pager）
                    // ═══════════════════════════════════════
                    composable("home") {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val dashboardState by dashboardViewModel.state.collectAsState()

                            MainScreen(
                                dashboardState = dashboardState,
                                onDashboardQuickMatch = {
                                    startQuickMatch(navController)
                                },
                                onDashboardMatchTemplates = {
                                    navController.navigate("match_templates")
                                },
                                onDashboardRecordClick = { record ->
                                    showMatchSummary(isHistory = true, historyRecord = record)
                                },
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
                                onThemeClick = { navController.navigate("theme_settings") },
                                onLanguageClick = { showLanguageSelectionDialogState = true },
                                onSettingsClick = { /* 设置入口已移除独立颜色弹窗 */ },
                                onAboutClick = { navController.navigate("about") },
                                onEditProfileClick = { navController.navigate("edit_profile") },
                                avatarUri = userAvatarUriString?.let { Uri.parse(it) },
                                nickname = userNickname,
                                onPageChanged = { page ->
                                    when (page) {
                                        0 -> dashboardViewModel.refresh()
                                        1 -> historyRecordsCompose = recordManager.getAllRecords()
                                    }
                                }
                            )

                            // 弹窗覆盖层
                            DialogOverlay(navController)
                        }
                    }

                    // ═══════════════════════════════════════
                    // 赛事预设库
                    // ═══════════════════════════════════════
                    composable("match_templates") {
                        val templates = templateManager.getAllTemplates()
                        MatchTemplateScreen(
                            initialTemplates = templates,
                            templateManager = templateManager,
                            onNavigateBack = { navController.popBackStack() },
                            onTemplateSelected = { template ->
                                startMatchWithTemplate(template, navController)
                            },
                        )
                    }

                    // ═══════════════════════════════════════
                    // 全屏计时器页面（无底部导航栏）
                    // ═══════════════════════════════════════
                    composable("timer") {
                        FullscreenTimerContent(
                            onNavigateBack = {
                                resetMatch()
                                navController.popBackStack("home", false)
                            }
                        )
                    }

                    // ═══════════════════════════════════════
                    // 主题设置页面
                    // ═══════════════════════════════════════
                    composable(
                        route = "theme_settings",
                        enterTransition = { slideInHorizontally { it } },
                        exitTransition = { slideOutHorizontally { -it } },
                        popEnterTransition = { slideInHorizontally { -it } },
                        popExitTransition = { slideOutHorizontally { it } }
                    ) {
                        ThemeSettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onConfigChanged = { newConfig: ThemeConfig ->
                                themeConfigState = newConfig
                                ThemeManager.config = newConfig
                            }
                        )
                    }

                    // ═══════════════════════════════════════
                    // 关于页面
                    // ═══════════════════════════════════════
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

                    // ═══════════════════════════════════════
                    // 编辑资料页面
                    // ═══════════════════════════════════════
                    composable(
                        route = "edit_profile",
                        enterTransition = { slideInHorizontally { it } },
                        exitTransition = { slideOutHorizontally { -it } },
                        popEnterTransition = { slideInHorizontally { -it } },
                        popExitTransition = { slideOutHorizontally { it } }
                    ) {
                        EditProfileScreen(
                            currentAvatarUri = userAvatarUriString?.let { Uri.parse(it) },
                            currentNickname = userNickname,
                            onNavigateBack = { navController.popBackStack() },
                            onSave = { newNickname, newAvatarUri ->
                                userNickname = newNickname
                                profilePrefs.edit().putString("nickname", newNickname).apply()
                                newAvatarUri?.let { uri ->
                                    userAvatarUriString = uri.toString()
                                    profilePrefs.edit().putString("avatar_uri", uri.toString()).apply()
                                    try {
                                        contentResolver.takePersistableUriPermission(
                                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    } catch (_: Exception) { }
                                }
                            }
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

    // region 全屏计时器页面 Composable

    /**
     * 全屏计时器页面 - 独立路由，无底部导航栏
     *
     * 赛事配置由模板传入后写入 Activity 级状态变量，
     * 此 Composable 直接读取这些状态驱动 TimerPage。
     */
    @androidx.compose.runtime.Composable
    private fun FullscreenTimerContent(
        onNavigateBack: () -> Unit,
    ) {
        // 设置比赛总结确认后的回调
        onSummaryConfirmed = onNavigateBack

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                TimerPage(
                    matchName = currentMatchName,
                    homeTeamName = currentHomeTeamName,
                    awayTeamName = currentAwayTeamName,
                    homeTeamColor = homeTeamColor,
                    awayTeamColor = awayTeamColor,
                    state = timerStateCompose,
                    currentHalf = currentHalf,
                    statusText = statusTextCompose,
                    statusColor = statusColorCompose,
                    statusIconRes = statusIconResCompose,
                    mainTimeText = mainTimeTextCompose,
                    mainTimeColor = mainTimeColorCompose,
                    stoppageTimeText = stoppageTimeTextCompose,
                    stoppageActive = stoppageActiveCompose,
                    showEndHalfButton = showEndHalfButtonCompose,
                    onMainButtonClick = { toggleTimer() },
                    onEndHalfButtonLongPress = { onEndHalfButtonLongPress() },
                )

                // 事件相关弹窗（比赛中使用）
                DialogOverlayForTimer()
            }
        }
    }

    // region 赛事启动

    /**
     * 快速开球 - 使用默认配置直接进入计时器
     */
    private fun startQuickMatch(navController: androidx.navigation.NavController) {
        initMatchFromTemplate(
            MatchTemplate(
                name = getString(R.string.default_template_name),
                homeTeamName = getString(R.string.default_home_team),
                awayTeamName = getString(R.string.default_away_team),
                halfTimeMinutes = DEFAULT_HALF_TIME,
                homeTeamColor = 0xFF1565C0.toInt(),
                awayTeamColor = 0xFFC62828.toInt(),
            )
        )
        navController.navigate("timer")
    }

    /**
     * 使用赛事预设开始比赛
     */
    private fun startMatchWithTemplate(
        template: MatchTemplate,
        navController: androidx.navigation.NavController
    ) {
        initMatchFromTemplate(template)
        navController.navigate("timer")
    }

    /**
     * 从模板初始化比赛配置
     *
     * 重置计时器状态，并应用模板中的赛事配置。
     * 进入计时器页面后即处于 READY 状态，点击开始即可。
     */
    private fun initMatchFromTemplate(template: MatchTemplate) {
        // 重置计时器状态
        resetMatch()

        // 应用模板配置
        currentMatchName = template.name
        currentHomeTeamName = template.homeTeamName
        currentAwayTeamName = template.awayTeamName
        homeTeamColor = template.homeTeamColor
        awayTeamColor = template.awayTeamColor
        halfTimeSeconds = template.halfTimeMinutes * 60L
        matchTimeSet = true // 预设已包含所有配置，无需再弹窗确认
    }

    // region 弹窗覆盖层

    /**
     * 主页弹窗覆盖层（主题、语言选择等）
     */
    @androidx.compose.runtime.Composable
    private fun DialogOverlay(navController: androidx.navigation.NavController) {
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

        // 比赛总结弹窗（历史记录查看 / 比赛结束总结）
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
                onDismiss = {
                    showMatchSummaryDialogState = false
                    // 如果是比赛结束的总结（非历史），确认后退出计时器页
                    if (!matchSummaryIsHistory) {
                        onSummaryConfirmed?.invoke()
                    }
                }
            )
        }
    }

    /**
     * 计时器页面专用弹窗覆盖层（统一事件底部面板 + 比赛总结）
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @androidx.compose.runtime.Composable
    private fun DialogOverlayForTimer() {
        // 比赛总结弹窗（比赛结束总结）
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
                onDismiss = {
                    showMatchSummaryDialogState = false
                    // 比赛结束总结确认后退出计时器页
                    if (!matchSummaryIsHistory) {
                        onSummaryConfirmed?.invoke()
                    }
                }
            )
        }

        // 统一事件底部面板
        if (showUnifiedEventSheet) {
            val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
            // 暂存确认回调参数，等动画播放完再执行
            var pendingConfirm by remember { mutableStateOf<Triple<EventType, TeamSelection, String>?>(null) }

            // 监听 pendingConfirm: 有数据时先播放关闭动画，动画结束后再移除面板
            LaunchedEffect(pendingConfirm) {
                val data = pendingConfirm ?: return@LaunchedEffect
                // 先隐藏面板（播放下滑动画）
                sheetState.hide()
                // 等动画完成后再移除
                showUnifiedEventSheet = false
                handleEventConfirmed(data.first, data.second, data.third)
            }

            UnifiedEventBottomSheet(
                sheetState = sheetState,
                homeColor = Color(homeTeamColor),
                awayColor = Color(awayTeamColor),
                onDismiss = {
                    showUnifiedEventSheet = false
                },
                onConfirm = { eventType, team, number ->
                    pendingConfirm = Triple(eventType, team, number)
                },
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

        // 显示统一事件底部面板
        showUnifiedEventSheet = true
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
        mainTimeColorCompose = Color(0xFF00FF00)
        stoppageActiveCompose = false
        stoppageTimeTextCompose = formatTime(stoppageTime)

        startUpdateLoop()

        addLog("🏁 下半场开始 - 从 ${formatTime(mainTime)} 继续计时")
    }

    private fun endFirstHalf() {
        state = STATE_HALFTIME
        currentHalf = HALF_BREAK
        firstHalfStoppage = stoppageTime

        syncComposeState()
        mainTimeTextCompose = formatTime(mainTime)
        mainTimeColorCompose = Color(0xFF888888)

        addLog("📊 上半场结束 | 比赛: ${formatTime(mainTime)} | 补时: ${formatTime(stoppageTime)}")

        stoppageTime = 0
        halfTimeAlertShown = false
        stoppageTimeTextCompose = formatTime(stoppageTime)
    }

    private fun endSecondHalf() {
        state = STATE_FINISHED

        syncComposeState()
        mainTimeColorCompose = Color(0xFF888888)
        mainTimeTextCompose = formatTime(mainTime)

        addLog("🏆 比赛结束")
        addLog("📊 总补时: ${formatTime(stoppageTime + firstHalfStoppage)}")

        saveMatchRecord()
        historyRecordsCompose = recordManager.getAllRecords()

        // 自动弹出总结页（确认后退出计时器页回首页）
        showMatchSummary()
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

        // 重置赛事配置
        currentMatchName = ""
        currentHomeTeamName = ""
        currentAwayTeamName = ""
        homeTeamColor = 0xFF1565C0.toInt()
        awayTeamColor = 0xFFC62828.toInt()

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
                mainTime++

                if (state == STATE_PAUSED) {
                    stoppageTime++
                }

                mainTimeTextCompose = formatTime(mainTime)
                stoppageTimeTextCompose = formatTime(stoppageTime)
                showEndHalfButtonCompose = state == STATE_RUNNING || state == STATE_PAUSED

                checkTimeAlerts()
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
                }
            }
        }
    }

    private fun triggerAlert(timeStr: String, message: String) {
        Log.i("FootballTimer", "⏰ ${timeStr}到！$message")
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
            awayGoals = awayGoals,
            matchName = currentMatchName,
            homeTeamName = currentHomeTeamName,
            awayTeamName = currentAwayTeamName,
            homeTeamColor = homeTeamColor,
            awayTeamColor = awayTeamColor,
        )

        recordManager.saveRecord(record)
        dashboardViewModel.refresh()
        Log.i("FootballTimer", "📁 比赛记录已保存: 主队 $homeGoals - $awayGoals 客队")
    }

    // region 弹窗触发

    /**
     * 统一事件面板确认回调处理
     */
    private fun handleEventConfirmed(eventType: EventType, team: TeamSelection, number: String) {
        when (eventType) {
            EventType.INJURY -> {
                recordSimpleEvent(getString(R.string.event_injury), " ", 30)
            }
            EventType.SUBSTITUTION -> {
                recordSimpleEvent(getString(R.string.event_substitute), " ", 30)
            }
            EventType.CANCEL -> { /* 不应到达 */ }
            else -> {
                val eventTypeStr = when (eventType) {
                    EventType.YELLOW_CARD -> getString(R.string.event_yellow)
                    EventType.RED_CARD -> getString(R.string.event_red)
                    EventType.GOAL -> getString(R.string.event_goal)
                    else -> return
                }
                val teamStr = when (team) {
                    TeamSelection.HOME -> getString(R.string.team_home)
                    TeamSelection.AWAY -> getString(R.string.team_away)
                    TeamSelection.CANCEL -> return
                }
                recordEventWithDetails(eventTypeStr, teamStr, number)
            }
        }
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
            historyRecord?.halfTimeMinutes ?: 0
        } else {
            (halfTimeSeconds / 60L).toInt()
        }

        val st1 = if (isHistory) historyRecord?.firstHalfStoppage ?: "00:00" else formatTime(firstHalfStoppage.toLong())
        val st2 = if (isHistory) historyRecord?.secondHalfStoppage ?: "00:00" else formatTime(stoppageTime.toLong())

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

    private fun syncComposeState() {
        timerStateCompose = state
        showEndHalfButtonCompose = state == STATE_RUNNING || state == STATE_PAUSED
        updateStatusLabelCompose()
    }

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

    private fun updateAllComposeState() {
        timerStateCompose = state
        updateStatusLabelCompose()
        mainTimeTextCompose = formatTime(mainTime)
        stoppageTimeTextCompose = formatTime(stoppageTime)
        showEndHalfButtonCompose = state == STATE_RUNNING || state == STATE_PAUSED
        historyRecordsCompose = recordManager.getAllRecords()
    }

    // region 工具方法

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
}

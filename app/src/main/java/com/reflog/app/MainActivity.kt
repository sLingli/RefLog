package com.reflog.app

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reflog.app.db.HalfType
import com.reflog.app.repository.DatabaseModule
import com.reflog.app.repository.MatchRecordRepository
import com.reflog.app.repository.MatchTemplateRepository
import kotlinx.coroutines.launch
import java.io.File
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
    private lateinit var recordRepository: MatchRecordRepository
    private lateinit var templateRepository: MatchTemplateRepository

    // Dashboard ViewModel
    private val dashboardViewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(DatabaseModule.getMatchRecordRepository(this))
    }

    private var mainTime: Long = 0
    private var stoppageTime: Long = 0
    private var firstHalfStoppage: Long = 0
    private var lastUpdateTime: Long = 0

    private var halfTimeSeconds: Long = DEFAULT_HALF_TIME * 60L

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
    private val statusColorCompose = Color(0xFF4CAF50)
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
    private lateinit var profilePrefs: SharedPreferences

    // region 生命周期

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        themeConfigState = ThemeManager.config
        LanguageManager.init(this)
        currentLanguage = LanguageManager.currentLanguage
        recordRepository = DatabaseModule.getMatchRecordRepository(this)
        templateRepository = DatabaseModule.getMatchTemplateRepository(this)

        profilePrefs = getSharedPreferences("user_profile", MODE_PRIVATE)
        userAvatarUriString = profilePrefs.getString("avatar_uri", null)
        userNickname = profilePrefs.getString("nickname", null)
            ?: getString(R.string.default_nickname)

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
                                    lifecycleScope.launch {
                                        recordRepository.deleteRecord(record.id)
                                        historyRecordsCompose = recordRepository.getAllRecords()
                                        dashboardViewModel.refresh()
                                    }
                                },
                                onHistoryClearAll = {
                                    lifecycleScope.launch {
                                        recordRepository.clearAllRecords()
                                        historyRecordsCompose = recordRepository.getAllRecords()
                                        dashboardViewModel.refresh()
                                    }
                                },
                                onThemeClick = { navController.navigate("theme_settings") },
                                onLanguageClick = { showLanguageSelectionDialogState = true },
                                onSettingsClick = { /* 设置入口已移除独立颜色弹窗 */ },
                                onAboutClick = { navController.navigate("about") },
                                onEditProfileClick = { navController.navigate("edit_profile") },
                                avatarUri = userAvatarUriString?.let { Uri.fromFile(File(it)) },
                                nickname = userNickname,
                                onPageChanged = { page ->
                                    when (page) {
                                        0 -> dashboardViewModel.refresh()
                                        1 -> lifecycleScope.launch { historyRecordsCompose = recordRepository.getAllRecords() }
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
                        MatchTemplateScreen(
                            templateRepository = templateRepository,
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
                            currentAvatarPath = userAvatarUriString,
                            currentNickname = userNickname,
                            onNavigateBack = { navController.popBackStack() },
                            onSave = { newNickname, newAvatarLocalPath ->
                                userNickname = newNickname
                                profilePrefs.edit().putString("nickname", newNickname).apply()
                                if (newAvatarLocalPath != null) {
                                    userAvatarUriString = newAvatarLocalPath
                                    profilePrefs.edit().putString("avatar_uri", newAvatarLocalPath).apply()
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
        if (::updateRunnable.isInitialized) handler.removeCallbacks(updateRunnable)
    }

    // region 全屏计时器页面 Composable

    /**
     * 全屏计时器页面 - 独立路由，无底部导航栏
     *
     * 赛事配置由模板传入后写入 Activity 级状态变量，
     * 此 Composable 直接读取这些状态驱动 TimerPage。
     */
    @Composable
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
    private fun startQuickMatch(navController: NavController) {
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
        navController: NavController
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
    }

    // region 弹窗覆盖层

    /**
     * 主页弹窗覆盖层（主题、语言选择等）
     */
    @Composable
    private fun DialogOverlay(navController: NavController) {
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
    @Composable
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
            val sheetState = rememberModalBottomSheetState(
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
        when (state) {
            STATE_READY -> startTimer()
            STATE_RUNNING -> pauseTimer()
            STATE_PAUSED -> resumeTimer()
            STATE_HALFTIME -> startSecondHalf()
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

        startUpdateLoop()
    }

    private fun resumeTimer() {
        state = STATE_RUNNING
        syncComposeState()
        stoppageActiveCompose = false

        startUpdateLoop()
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
    }

    private fun endFirstHalf() {
        state = STATE_HALFTIME
        currentHalf = HALF_BREAK
        firstHalfStoppage = stoppageTime

        syncComposeState()
        mainTimeTextCompose = formatTime(mainTime)
        mainTimeColorCompose = Color(0xFF888888)

        stoppageTime = 0
        halfTimeAlertShown = false
        stoppageTimeTextCompose = formatTime(stoppageTime)

        if (::updateRunnable.isInitialized) handler.removeCallbacks(updateRunnable)
    }

    private fun endSecondHalf() {
        state = STATE_FINISHED

        syncComposeState()
        mainTimeColorCompose = Color(0xFF888888)
        mainTimeTextCompose = formatTime(mainTime)

        if (::updateRunnable.isInitialized) handler.removeCallbacks(updateRunnable)

        lifecycleScope.launch {
            saveMatchRecord()
            historyRecordsCompose = recordRepository.getAllRecords()
        }

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
        halfTimeAlertShown = false
        fullTimeAlertShown = false
        matchEvents.clear()

        if (::updateRunnable.isInitialized) handler.removeCallbacks(updateRunnable)

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
    }

    // region 计时器核心

    private fun startUpdateLoop() {
        if (::updateRunnable.isInitialized) handler.removeCallbacks(updateRunnable)
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
        when (currentHalf) {
            HALF_FIRST -> {
                if (mainTime >= halfTimeSeconds && !halfTimeAlertShown) {
                    halfTimeAlertShown = true
                    mainTimeColorCompose = Color(0xFFFF9800)
                    statusTextCompose = getString(R.string.status_first_half_stoppage)
                }
            }
            HALF_SECOND -> {
                val targetTime = halfTimeSeconds * 2
                if (mainTime >= targetTime && !fullTimeAlertShown) {
                    fullTimeAlertShown = true
                    mainTimeColorCompose = Color(0xFFF44336)
                    statusTextCompose = getString(R.string.status_second_half_stoppage)
                }
            }
        }
    }

    // region 事件记录

    private fun recordSimpleEvent(eventType: EventType) {
        val timeStr = formatTime(mainTime)
        val halfType = if (currentHalf == HALF_FIRST) HalfType.FIRST_HALF else HalfType.SECOND_HALF
        val minute = (mainTime / 60).toInt()
        val emoji = eventType.toEmoji()

        matchEvents.add(MatchEvent(
            timeStr = timeStr,
            event = eventType.name,
            emoji = emoji,
            detail = "",
            half = halfType.name,
            minute = minute
        ))
    }

    private fun recordEventWithDetails(eventType: EventType, team: TeamSelection, number: String) {
        val emoji = eventType.toEmoji()

        val teamStr = if (team == TeamSelection.HOME) "Home" else "Away"
        val detailText = "$teamStr #$number"
        val timeStr = formatTime(mainTime)
        val halfType = if (currentHalf == HALF_FIRST) HalfType.FIRST_HALF else HalfType.SECOND_HALF
        val minute = (mainTime / 60).toInt()

        matchEvents.add(MatchEvent(
            timeStr = timeStr,
            event = eventType.name,
            emoji = emoji,
            detail = detailText,
            half = halfType.name,
            minute = minute
        ))

        stoppageTimeTextCompose = formatTime(stoppageTime)
    }

    private suspend fun saveMatchRecord() {
        val record = MatchRecordRepository.Companion.buildMatchRecord(
            matchEvents = matchEvents.toList(),
            halfTimeSeconds = halfTimeSeconds,
            firstHalfStoppage = firstHalfStoppage,
            stoppageTime = stoppageTime,
            currentMatchName = currentMatchName,
            currentHomeTeamName = currentHomeTeamName,
            currentAwayTeamName = currentAwayTeamName,
            homeTeamColor = homeTeamColor,
            awayTeamColor = awayTeamColor,
        )

        recordRepository.saveRecord(record)
        dashboardViewModel.refresh()
    }

    // region 弹窗触发

    /**
     * 统一事件面板确认回调处理
     */
    private fun handleEventConfirmed(eventType: EventType, team: TeamSelection, number: String) {
        when (eventType) {
            EventType.INJURY, EventType.SUBSTITUTION -> {
                recordSimpleEvent(eventType)
            }
            EventType.CANCEL -> {  }
            else -> {
                if (team == TeamSelection.CANCEL) return
                recordEventWithDetails(eventType, team, number)
            }
        }
    }

    private fun showMatchSummary(isHistory: Boolean = false, historyRecord: MatchRecord? = null) {
        val eventsToShow: List<MatchEvent> = if (isHistory) {
            historyRecord?.events ?: listOf()
        } else {
            matchEvents.toList()  // 防御性拷贝
        }

        val homeGoals = historyRecord?.homeGoals ?: eventsToShow.count {
            parseEventType(it.event) == EventType.GOAL && it.detail.contains("Home", ignoreCase = true)
        }
        val awayGoals = historyRecord?.awayGoals ?: eventsToShow.count {
            parseEventType(it.event) == EventType.GOAL && it.detail.contains("Away", ignoreCase = true)
        }
        val yellowCount = historyRecord?.yellowCount ?: eventsToShow.count { parseEventType(it.event) == EventType.YELLOW_CARD }
        val redCount = historyRecord?.redCount ?: eventsToShow.count { parseEventType(it.event) == EventType.RED_CARD }

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
    }

    private fun updateAllComposeState() {
        timerStateCompose = state
        updateStatusLabelCompose()
        mainTimeTextCompose = formatTime(mainTime)
        stoppageTimeTextCompose = formatTime(stoppageTime)
        showEndHalfButtonCompose = state == STATE_RUNNING || state == STATE_PAUSED
        lifecycleScope.launch {
            historyRecordsCompose = recordRepository.getAllRecords()
        }
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

    /**
     * 从事件字符串解析 EventType（支持枚举名和旧的本地化字符串）
     */
    private fun parseEventType(eventStr: String): EventType {
        try { return EventType.valueOf(eventStr) } catch (_: Exception) {}
        return when (eventStr) {
            "进球", "Goal" -> EventType.GOAL
            "黄牌", "Yellow Card" -> EventType.YELLOW_CARD
            "红牌", "Red Card" -> EventType.RED_CARD
            "换人", "Sub" -> EventType.SUBSTITUTION
            "受伤", "Injury" -> EventType.INJURY
            else -> EventType.GOAL
        }
    }
}

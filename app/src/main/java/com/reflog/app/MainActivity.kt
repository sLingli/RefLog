package com.reflog.app

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reflog.app.repository.DatabaseModule
import com.reflog.app.repository.MatchRecordRepository
import com.reflog.app.repository.MatchTemplateRepository
import kotlinx.coroutines.launch
import java.io.File

/**
 * 足球比赛计时器主界面
 *
 * 职责：NavHost 路由 + 个人资料 + 主题语言 + 弹窗组合
 * 计时器逻辑委托给 [TimerViewModel]
 */
class MainActivity : AppCompatActivity() {

    private lateinit var recordRepository: MatchRecordRepository
    private lateinit var templateRepository: MatchTemplateRepository

    private val dashboardViewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(DatabaseModule.getMatchRecordRepository(this))
    }

    private val timerViewModel: TimerViewModel by viewModels {
        TimerViewModel.Factory(DatabaseModule.getMatchRecordRepository(this))
    }

    // 个人资料
    private var userAvatarUriString: String? by mutableStateOf(null)
    private var userNickname: String by mutableStateOf("")
    private lateinit var profilePrefs: SharedPreferences

    // 主题/语言
    private var themeConfigState by mutableStateOf(ThemeConfig())
    private var showLanguageSelectionDialogState by mutableStateOf(false)
    private var currentLanguage by mutableStateOf(AppLanguage.DEFAULT)

    // 历史记录（供 MainScreen 历史 tab）
    private var historyRecordsCompose by mutableStateOf<List<MatchRecord>>(emptyList())

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

        lifecycleScope.launch {
            historyRecordsCompose = recordRepository.getAllRecords()
        }

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
                    composable("home") {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val dashboardState by dashboardViewModel.state.collectAsState()
                            MainScreen(
                                dashboardState = dashboardState,
                                onDashboardQuickMatch = {
                                    timerViewModel.initMatch(MatchTemplate(
                                        name = getString(R.string.default_template_name),
                                        homeTeamName = getString(R.string.default_home_team),
                                        awayTeamName = getString(R.string.default_away_team),
                                    ))
                                    navController.navigate("timer")
                                },
                                onDashboardMatchTemplates = { navController.navigate("match_templates") },
                                onDashboardRecordClick = { timerViewModel.showHistoryMatchSummary(it) },
                                historyRecords = historyRecordsCompose,
                                onHistoryRecordClick = { timerViewModel.showHistoryMatchSummary(it) },
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
                                onSettingsClick = { },
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
                            HomeDialogOverlay()
                        }
                    }

                    composable("match_templates") {
                        MatchTemplateScreen(
                            templateRepository = templateRepository,
                            onNavigateBack = { navController.popBackStack() },
                            onTemplateSelected = { template ->
                                timerViewModel.initMatch(template)
                                navController.navigate("timer")
                            },
                        )
                    }

                    composable("timer") {
                        FullscreenTimerContent(
                            onNavigateBack = {
                                timerViewModel.resetMatch()
                                lifecycleScope.launch {
                                    historyRecordsCompose = recordRepository.getAllRecords()
                                }
                                dashboardViewModel.refresh()
                                navController.popBackStack("home", false)
                            }
                        )
                    }

                    composable(
                        route = "theme_settings",
                        enterTransition = { slideInHorizontally { it } },
                        exitTransition = { slideOutHorizontally { -it } },
                        popEnterTransition = { slideInHorizontally { -it } },
                        popExitTransition = { slideOutHorizontally { it } }
                    ) {
                        ThemeSettingsScreen(onConfigChanged = { newConfig ->
                            themeConfigState = newConfig
                            ThemeManager.config = newConfig
                        })
                    }

                    composable(
                        route = "about",
                        enterTransition = { slideInHorizontally { it } },
                        exitTransition = { slideOutHorizontally { -it } },
                        popEnterTransition = { slideInHorizontally { -it } },
                        popExitTransition = { slideOutHorizontally { it } }
                    ) {
                        AboutScreen(onNavigateBack = { navController.popBackStack() })
                    }

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

    // ── 计时器页面 ──────────────────────────────

    @Composable
    private fun FullscreenTimerContent(onNavigateBack: () -> Unit) {
        val uiState by timerViewModel.uiState.collectAsState()
        val formatTime = timerViewModel::formatTime

        val statusText = when (uiState.timerState) {
            TimerState.READY -> stringResource(R.string.status_ready)
            TimerState.RUNNING, TimerState.PAUSED -> when (uiState.currentHalf) {
                HalfState.FIRST -> stringResource(R.string.status_first_half)
                HalfState.BREAK -> stringResource(R.string.status_halftime)
                HalfState.SECOND -> stringResource(R.string.status_second_half)
            }
            TimerState.HALFTIME -> stringResource(R.string.status_halftime)
            TimerState.FINISHED -> stringResource(R.string.status_finished)
        }

        val statusIconRes = when (uiState.timerState) {
            TimerState.HALFTIME -> R.drawable.ic_coffee
            TimerState.FINISHED -> R.drawable.ic_trophy
            else -> R.drawable.sports_soccer
        }

        val isInStoppage = timerViewModel.isInStoppage()
        val mainTimeColor = when {
            uiState.timerState == TimerState.HALFTIME || uiState.timerState == TimerState.FINISHED -> Color(0xFF888888)
            uiState.currentHalf == HalfState.SECOND && isInStoppage -> Color(0xFFF44336)
            isInStoppage -> Color(0xFFFF9800)
            uiState.timerState == TimerState.RUNNING && uiState.currentHalf == HalfState.SECOND -> Color(0xFF00FF00)
            else -> Color(0xFF4CAF50)
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                TimerPage(
                    matchName = uiState.matchName,
                    homeTeamName = uiState.homeTeamName,
                    awayTeamName = uiState.awayTeamName,
                    homeTeamColor = uiState.homeTeamColor,
                    awayTeamColor = uiState.awayTeamColor,
                    state = uiState.timerState,
                    currentHalf = uiState.currentHalf,
                    statusText = statusText,
                    statusColor = Color(0xFF4CAF50),
                    statusIconRes = statusIconRes,
                    mainTimeText = formatTime(uiState.mainTimeSeconds),
                    mainTimeColor = mainTimeColor,
                    stoppageTimeText = formatTime(uiState.stoppageSeconds),
                    stoppageActive = uiState.timerState == TimerState.PAUSED,
                    showEndHalfButton = uiState.showEndHalfButton,
                    onMainButtonClick = { timerViewModel.toggleTimer() },
                    onEndHalfButtonLongPress = { timerViewModel.onEndHalfButtonLongPress() },
                )
                TimerDialogOverlay(uiState, onNavigateBack)
            }
        }
    }

    // ── 弹窗覆盖层 ──────────────────────────────

    @Composable
    private fun HomeDialogOverlay() {
        if (showLanguageSelectionDialogState) {
            LanguageSelectionDialog(
                currentLanguage = currentLanguage,
                onDismiss = { showLanguageSelectionDialogState = false },
                onLanguageSelected = { language ->
                    LanguageManager.currentLanguage = language
                    currentLanguage = language
                    showLanguageSelectionDialogState = false
                    LanguageManager.applyLanguage(this@MainActivity)
                }
            )
        }
        val matchSummary = timerViewModel.uiState.collectAsState().value.matchSummary
        if (matchSummary != null) {
            MatchSummaryDialog(
                isHistory = matchSummary.isHistory,
                halfTimeMinutes = matchSummary.halfTimeMinutes,
                homeGoals = matchSummary.homeGoals,
                awayGoals = matchSummary.awayGoals,
                yellowCount = matchSummary.yellowCount,
                redCount = matchSummary.redCount,
                firstHalfStoppage = matchSummary.firstHalfStoppage,
                secondHalfStoppage = matchSummary.secondHalfStoppage,
                events = matchSummary.events,
                onDismiss = { timerViewModel.dismissMatchSummary() }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TimerDialogOverlay(uiState: TimerUiState, onNavigateBack: () -> Unit) {
        val matchSummary = uiState.matchSummary
        if (matchSummary != null) {
            MatchSummaryDialog(
                isHistory = matchSummary.isHistory,
                halfTimeMinutes = matchSummary.halfTimeMinutes,
                homeGoals = matchSummary.homeGoals,
                awayGoals = matchSummary.awayGoals,
                yellowCount = matchSummary.yellowCount,
                redCount = matchSummary.redCount,
                firstHalfStoppage = matchSummary.firstHalfStoppage,
                secondHalfStoppage = matchSummary.secondHalfStoppage,
                events = matchSummary.events,
                onDismiss = {
                    timerViewModel.dismissMatchSummary()
                    if (uiState.timerState == TimerState.FINISHED && !matchSummary.isHistory) {
                        onNavigateBack()
                    }
                }
            )
        }

        if (uiState.showEventSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            var pendingConfirm by remember { mutableStateOf<Triple<EventType, TeamSelection, String>?>(null) }

            LaunchedEffect(pendingConfirm) {
                val data = pendingConfirm ?: return@LaunchedEffect
                sheetState.hide()
                timerViewModel.handleEventConfirmed(data.first, data.second, data.third)
                pendingConfirm = null
            }

            UnifiedEventBottomSheet(
                sheetState = sheetState,
                homeColor = Color(uiState.homeTeamColor),
                awayColor = Color(uiState.awayTeamColor),
                homeTeamName = uiState.homeTeamName,
                awayTeamName = uiState.awayTeamName,
                onDismiss = { timerViewModel.onEventSheetDismissed() },
                onConfirm = { eventType, team, number ->
                    pendingConfirm = Triple(eventType, team, number)
                },
            )
        }
    }
}

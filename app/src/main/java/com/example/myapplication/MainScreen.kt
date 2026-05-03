package com.example.myapplication

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

/**
 * 主屏幕 - Compose 版本
 *
 * 对齐原 activity_main.xml 的功能结构：
 * - Scaffold + BottomNavBar（替代 BottomNavigationView）
 * - HorizontalPager（替代 ViewPager2，3 个页面：计时器 / 历史 / 我的）
 * - 弹窗覆盖层（替代 ComposeView composeDialogContainer）
 *
 * 使用方式：
 * ```kotlin
 * // 在 MainActivity 中替代 setContentView(R.layout.activity_main)
 * setContent {
 *     RefLogTheme {
 *         MainScreen(...)
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    // === 计时器页面状态 ===
    timerState: String = TIMER_STATE_READY,
    currentHalf: String = HALF_FIRST_CODE,
    statusText: String = "",
    statusColor: Color = Color(0xFF4CAF50),
    statusIconRes: Int = 0,
    mainTimeText: String = "00:00",
    mainTimeColor: Color = Color(0xFF4CAF50),
    stoppageTimeText: String = "00:00",
    stoppageActive: Boolean = false,
    showEndHalfButton: Boolean = false,
    onTimerMainButtonClick: () -> Unit = {},
    onEndHalfButtonLongPress: () -> Unit = {},

    // === 历史页面状态 ===
    historyRecords: List<MatchRecord> = emptyList(),
    onHistoryRecordClick: (MatchRecord) -> Unit = {},
    onHistoryDeleteRecord: (MatchRecord) -> Unit = {},
    onHistoryClearAll: () -> Unit = {},

    // === 我的页面回调 ===
    onThemeClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},

    // === 页面切换回调 ===
    onPageChanged: (Int) -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(BottomNavTab.TIMER) }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 3 } // TIMER, HISTORY, PROFILE
    )

    // Pager → Tab 同步（使用 targetPage 实现即时响应，消除导航栏高亮滞后）
    LaunchedEffect(pagerState.targetPage) {
        val newTab = BottomNavTab.fromIndex(pagerState.targetPage)
        if (selectedTab != newTab) {
            selectedTab = newTab
            onPageChanged(pagerState.targetPage)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    coroutineScope.launch {
                        val targetPage = tab.index
                        val currentPage = pagerState.settledPage
                        if (kotlin.math.abs(targetPage - currentPage) > 1) {
                            // 跨级切换：瞬间跳转，避免中间页面闪烁
                            pagerState.scrollToPage(targetPage)
                        } else {
                            // 相邻切换：保留丝滑滑动动画
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 页面容器（替代 ViewPager2）
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    // 计时器页面
                    0 -> {
                        TimerPage(
                            state = timerState,
                            currentHalf = currentHalf,
                            statusText = statusText,
                            statusColor = statusColor,
                            statusIconRes = statusIconRes,
                            mainTimeText = mainTimeText,
                            mainTimeColor = mainTimeColor,
                            stoppageTimeText = stoppageTimeText,
                            stoppageActive = stoppageActive,
                            showEndHalfButton = showEndHalfButton,
                            onMainButtonClick = onTimerMainButtonClick,
                            onEndHalfButtonLongPress = onEndHalfButtonLongPress,
                        )
                    }

                    // 历史记录页面
                    1 -> {
                        HistoryPageContent(
                            records = historyRecords,
                            onRecordClick = onHistoryRecordClick,
                            onDeleteRecord = onHistoryDeleteRecord,
                            onClearAll = onHistoryClearAll
                        )
                    }

                    // 我的页面
                    2 -> {
                        MeScreenContent(
                            onThemeClick = onThemeClick,
                            onSettingsClick = onSettingsClick,
                            onAboutClick = onAboutClick,
                            onDismiss = { /* 嵌入在 Pager 中，不需要单独关闭 */ }
                        )
                    }
                }
            }
        }
    }
}

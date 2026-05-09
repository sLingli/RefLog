package com.example.myapplication

import android.net.Uri
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
import kotlinx.coroutines.launch

/**
 * 主屏幕 - Compose 版本
 *
 * 重构后结构（3 个页面）：
 * - Scaffold + BottomNavBar（首页 / 历史 / 我的）
 * - HorizontalPager（替代 ViewPager2，3 个页面）
 * - 计时器已移至独立全屏路由，不再占用底部导航栏
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    // === 首页 Dashboard 状态 ===
    dashboardState: DashboardState = DashboardState(),
    onDashboardQuickMatch: () -> Unit = {},
    onDashboardMatchTemplates: () -> Unit = {},
    onDashboardRecordClick: (MatchRecord) -> Unit = {},

    // === 历史页面状态 ===
    historyRecords: List<MatchRecord> = emptyList(),
    onHistoryRecordClick: (MatchRecord) -> Unit = {},
    onHistoryDeleteRecord: (MatchRecord) -> Unit = {},
    onHistoryClearAll: () -> Unit = {},

    // === 我的页面回调 ===
    onThemeClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    avatarUri: Uri? = null,
    nickname: String = "",

    // === 页面切换回调 ===
    onPageChanged: (Int) -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(BottomNavTab.HOME) }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 3 } // HOME(0), HISTORY(1), PROFILE(2)
    )

    // Pager → Tab 同步
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
                            pagerState.scrollToPage(targetPage)
                        } else {
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
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    // 首页 Dashboard
                    0 -> {
                        DashboardScreen(
                            state = dashboardState,
                            onQuickMatch = onDashboardQuickMatch,
                            onMatchTemplates = onDashboardMatchTemplates,
                            onRecordClick = onDashboardRecordClick,
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
                            onLanguageClick = onLanguageClick,
                            onSettingsClick = onSettingsClick,
                            onAboutClick = onAboutClick,
                            onDismiss = { /* 嵌入在 Pager 中，不需要单独关闭 */ },
                            avatarUri = avatarUri,
                            nickname = nickname,
                            onEditProfileClick = onEditProfileClick,
                        )
                    }
                }
            }
        }
    }
}

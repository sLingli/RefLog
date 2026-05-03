package com.example.myapplication

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * 底部导航栏标签枚举
 */
enum class BottomNavTab(val index: Int) {
    TIMER(0),
    HISTORY(1),
    PROFILE(2);

    companion object {
        fun fromIndex(index: Int): BottomNavTab = entries.firstOrNull { it.index == index } ?: TIMER
    }
}

/**
 * 底部导航栏 - Compose 版本
 *
 * 对齐原 XML 布局中的 BottomNavigationView：
 * - 3 个标签：计时器 / 历史 / 我的
 * - 只显示图标（labelVisibilityMode = unlabeled）
 * - 选中颜色 #4CAF50，未选中 #888888
 */
@Composable
fun BottomNavBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit
) {
    val selectedColor = Color(0xFF4CAF50)
    val unselectedColor = Color(0xFF888888)

    NavigationBar(
        containerColor = Color.Unspecified, // 由主题 attr/colorSurfaceContainer 控制
        tonalElevation = 0.dp
    ) {
        // 计时器
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.TIMER,
            onClick = { onTabSelected(BottomNavTab.TIMER) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_play_arrow_24),
                    contentDescription = stringResource(R.string.nav_timer)
                )
            },
            label = null, // 只显示图标
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                unselectedIconColor = unselectedColor,
                indicatorColor = Color.Transparent
            )
        )

        // 历史
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.HISTORY,
            onClick = { onTabSelected(BottomNavTab.HISTORY) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_history),
                    contentDescription = stringResource(R.string.nav_history)
                )
            },
            label = null,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                unselectedIconColor = unselectedColor,
                indicatorColor = Color.Transparent
            )
        )

        // 我的
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.PROFILE,
            onClick = { onTabSelected(BottomNavTab.PROFILE) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_profile),
                    contentDescription = stringResource(R.string.nav_me)
                )
            },
            label = null,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                unselectedIconColor = unselectedColor,
                indicatorColor = Color.Transparent
            )
        )
    }
}

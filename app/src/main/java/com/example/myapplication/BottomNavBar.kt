package com.example.myapplication

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 底部导航栏标签枚举
 *
 * 重构后仅保留 3 个 Tab：首页 / 历史 / 我的
 * 计时器页面已独立为全屏沉浸式页面，不再占用底部导航栏
 */
enum class BottomNavTab(val index: Int) {
    HOME(0),
    HISTORY(1),
    PROFILE(2);

    companion object {
        fun fromIndex(index: Int): BottomNavTab = entries.firstOrNull { it.index == index } ?: HOME
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
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        // 首页
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.HOME,
            onClick = { onTabSelected(BottomNavTab.HOME) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home),
                    contentDescription = stringResource(R.string.nav_home),
                    modifier = Modifier.size(28.dp)
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.nav_home),
                    style = MaterialTheme.typography.labelSmall
                )
            },
            alwaysShowLabel = false,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                unselectedIconColor = unselectedColor,
                selectedTextColor = selectedColor,
                unselectedTextColor = unselectedColor,
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
                    contentDescription = stringResource(R.string.nav_history),
                    modifier = Modifier.size(28.dp)
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.nav_history),
                    style = MaterialTheme.typography.labelSmall
                )
            },
            alwaysShowLabel = false,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                unselectedIconColor = unselectedColor,
                selectedTextColor = selectedColor,
                unselectedTextColor = unselectedColor,
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
                    contentDescription = stringResource(R.string.nav_me),
                    modifier = Modifier.size(28.dp)
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.nav_me),
                    style = MaterialTheme.typography.labelSmall
                )
            },
            alwaysShowLabel = false,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                unselectedIconColor = unselectedColor,
                selectedTextColor = selectedColor,
                unselectedTextColor = unselectedColor,
                indicatorColor = Color.Transparent
            )
        )
    }
}

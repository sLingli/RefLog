package com.example.myapplication

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.wear.compose.material.MaterialTheme

/**
 * Compose 组件预加载器
 * 在 App 启动时同步预渲染所有 Compose 组件，避免首次打开弹窗时的加载延迟
 */
object ComposePreloader {

    @Volatile
    private var isPreloaded = false

    private var preloadView: ComposeView? = null

    /**
     * 同步预加载所有 Compose 组件
     * 应在 MainActivity.onCreate() 的 setContentView 之后立即调用
     * 会阻塞直到 Compose 组件渲染完成
     */
    fun preload(activity: ComponentActivity) {
        if (isPreloaded) return

        try {
            val rootView = activity.window.decorView.findViewById<FrameLayout>(android.R.id.content)

            // 创建预加载的 ComposeView
            preloadView = ComposeView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(1, 1)
                visibility = View.INVISIBLE
                alpha = 0f

                setViewTreeLifecycleOwner(activity)
                setViewTreeViewModelStoreOwner(activity)
                setViewTreeSavedStateRegistryOwner(activity)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

                setContent {
                    MaterialTheme {
                        PreloadAllComponents()
                    }
                }
            }

            // 添加到视图层级以触发 Compose 编译
            rootView.addView(preloadView)

            // 使用 ViewTreeObserver 监听预渲染完成
            preloadView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    preloadView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    // 延迟一帧后移除，确保渲染完成
                    Handler(Looper.getMainLooper()).post {
                        cleanup(rootView)
                    }
                }
            })

            isPreloaded = true
        } catch (_: Exception) {
            // 忽略预加载过程中的异常
        }
    }

    private fun cleanup(rootView: FrameLayout) {
        try {
            preloadView?.let { rootView.removeView(it) }
            preloadView = null
        } catch (_: Exception) {
            // 忽略清理过程中的异常
        }
    }
}

/**
 * 预渲染所有 Compose 组件的占位 Composable
 * 通过实例化所有组件来触发 Compose 编译器缓存
 */
@Composable
private fun PreloadAllComponents() {
    // 使用 Box 包裹所有组件，设置极小尺寸使其不可见
    Box(modifier = Modifier.size(1.dp)) {
        // 预加载 EventSelectionDialog
        PreloadEventSelectionDialog()

        // 预加载 TeamSelectionDialogCompose
        PreloadTeamSelectionDialog()

        // 预加载 NumberSelectionDialog
        PreloadNumberSelectionDialog()

        // 预加载 ColorSelectionDialog
        PreloadColorSelectionDialog()

        // 预加载 SettingsScreen
        PreloadSettingsScreen()

        // 预加载 AboutScreen
        PreloadAboutScreen()

        // 预加载 MatchSummaryScreen
        PreloadMatchSummaryScreen()

        // 预加载 HistoryScreen
        PreloadHistoryScreen()
    }
}

@Composable
private fun PreloadEventSelectionDialog() {
    EventSelectionDialog(
        onEventSelected = {},
        onDismiss = {}
    )
}

@Composable
private fun PreloadTeamSelectionDialog() {
    TeamSelectionDialogCompose(
        onHomeTeamSelected = {},
        onAwayTeamSelected = {},
        onDismiss = {},
        homeTeamColor = Color(0xFF1565C0),
        awayTeamColor = Color(0xFFC62828)
    )
}

@Composable
private fun PreloadNumberSelectionDialog() {
    NumberSelectionDialog(
        initialNumber = 10,
        title = "",
        onNumberConfirmed = {}
    )
}

@Composable
private fun PreloadColorSelectionDialog() {
    ColorSelectionDialog(
        initialColor = 0xFFF44336.toInt(),
        onColorSelected = {}
    )
}

@Composable
private fun PreloadSettingsScreen() {
    SettingsScreen(
        onHistoryClick = {},
        onAboutClick = {}
    )
}

@Composable
private fun PreloadAboutScreen() {
    AboutScreen()
}

@Composable
private fun PreloadMatchSummaryScreen() {
    MatchSummaryScreen(
        isHistory = false,
        durationMinutes = 45,
        homeGoals = 0,
        awayGoals = 0,
        yellowCount = 0,
        redCount = 0,
        stoppageTime1 = "00:00",
        stoppageTime2 = "00:00",
        events = emptyList(),
        onClose = {}
    )
}

@Composable
private fun PreloadHistoryScreen() {
    HistoryScreen(
        initialRecords = emptyList(),
        onClose = {},
        onClearAll = {},
        onDeleteOne = {},
        onRecordClick = {}
    )
}



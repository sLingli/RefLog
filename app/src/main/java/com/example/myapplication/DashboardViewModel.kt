package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 仪表盘 ViewModel
 *
 * 负责从 [MatchRecordManager] 读取历史记录，
 * 聚合计算出 Dashboard 页面所需的全部统计数据。
 */
class DashboardViewModel(
    private val recordManager: MatchRecordManager,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        refresh()
    }

    /**
     * 刷新数据
     * 在比赛结束、删除记录等事件后调用，重新计算聚合数据。
     */
    fun refresh() {
        viewModelScope.launch {
            val records = recordManager.getAllRecords()
            _state.value = DashboardState(
                totalMatches = records.size,
                totalGoals = records.sumOf { it.goalCount },
                totalYellowCards = records.sumOf { it.yellowCount },
                totalRedCards = records.sumOf { it.redCount },
                recentRecords = records.take(3),
            )
        }
    }

    /**
     * ViewModel Factory
     *
     * 因为需要注入 [MatchRecordManager]，使用自定义 Factory。
     */
    class Factory(
        private val recordManager: MatchRecordManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(recordManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

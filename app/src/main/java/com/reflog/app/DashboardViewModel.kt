package com.reflog.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reflog.app.repository.MatchRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 仪表盘 ViewModel
 *
 * 负责从 [MatchRecordRepository] 读取历史记录，
 * 聚合计算出 Dashboard 页面所需的全部统计数据。
 * 使用 SQL 聚合查询，不在内存中遍历。
 */
class DashboardViewModel(
    private val recordRepository: MatchRecordRepository,
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
        viewModelScope.launch(Dispatchers.IO) {
            val stats = recordRepository.getAggregateStats()
            val recentRecords = recordRepository.getRecentRecords(3)
            _state.value = DashboardState(
                totalMatches = stats.totalMatches,
                totalGoals = stats.totalGoals,
                totalYellowCards = stats.totalYellowCards,
                totalRedCards = stats.totalRedCards,
                recentRecords = recentRecords,
            )
        }
    }

    /**
     * ViewModel Factory
     *
     * 因为需要注入 [MatchRecordRepository]，使用自定义 Factory。
     */
    class Factory(
        private val recordRepository: MatchRecordRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(recordRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.wear.compose.material.MaterialTheme
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class HistoryBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "HistoryBottomSheet"
    }

    private lateinit var recordManager: MatchRecordManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordManager = MatchRecordManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                // 1. 拉取最新历史数据并转换为 UI 模型
                val rawRecords = recordManager.getAllRecords()
                val uiRecords = rawRecords.map { record ->
                    MatchHistoryUiModel(
                        id = record.id,
                        date = record.date,
                        duration = "${record.halfTimeMinutes}分钟/半场",
                        stoppage = "补时: 上+${record.firstHalfStoppage} / 下+${record.secondHalfStoppage}",
                        events = "进球:${record.goalCount}  红牌:${record.redCount}  换人:${record.substitutionCount}"
                    )
                }

                MaterialTheme {
                    HistoryScreen(
                        initialRecords = uiRecords,
                        onClose = { dismissAllowingStateLoss() },
                        onClearAll = {
                            recordManager.clearAllRecords()
                            Toast.makeText(
                                requireContext(),
                                "历史记录已清空",
                                Toast.LENGTH_SHORT
                            ).show()
                            dismissAllowingStateLoss()
                        },
                        onDeleteOne = { uiModel ->
                            recordManager.deleteRecord(uiModel.id)
                            Toast.makeText(
                                requireContext(),
                                "记录已删除",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}


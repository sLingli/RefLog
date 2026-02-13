package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
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

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as com.google.android.material.bottomsheet.BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                // 强制展开且不可拖拽，使得列表滚动时不会导致弹窗被拖动
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isDraggable = false
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val rawRecords = recordManager.getAllRecords()
                val uiRecords = rawRecords.map { record ->
                    MatchHistoryUiModel(
                        id = record.id,
                        date = record.date,
                        duration = "${record.halfTimeMinutes}${getString(R.string.unit_min_half)}",
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
                        },
                        onRecordClick = { uiModel ->
                            val fullRecord = recordManager.getAllRecords().find { it.id == uiModel.id }
                            if (fullRecord != null) {
                                showRecordSummary(fullRecord)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun showRecordSummary(record: MatchRecord) {
        // Calculate goals from events for backwards compatibility
        val calculatedHomeGoals = record.events.count {
            it.event == getString(R.string.event_goal) && it.detail.contains(getString(R.string.team_home))
        }
        val calculatedAwayGoals = record.events.count {
            it.event == getString(R.string.event_goal) && it.detail.contains(getString(R.string.team_away))
        }

        val homeGoals = if (record.homeGoals == 0 && record.awayGoals == 0) calculatedHomeGoals else record.homeGoals
        val awayGoals = if (record.homeGoals == 0 && record.awayGoals == 0) calculatedAwayGoals else record.awayGoals

        // Launch MatchSummaryActivity instead of Dialog
        val intent = android.content.Intent(requireContext(), MatchSummaryActivity::class.java).apply {
            putExtra(MatchSummaryActivity.EXTRA_IS_HISTORY, true)
            putExtra(MatchSummaryActivity.EXTRA_DURATION_MINUTES, record.halfTimeMinutes)
            putExtra(MatchSummaryActivity.EXTRA_HOME_GOALS, homeGoals)
            putExtra(MatchSummaryActivity.EXTRA_AWAY_GOALS, awayGoals)
            putExtra(MatchSummaryActivity.EXTRA_YELLOW_COUNT, record.yellowCount)
            putExtra(MatchSummaryActivity.EXTRA_RED_COUNT, record.redCount)
            putExtra(MatchSummaryActivity.EXTRA_STOPPAGE_TIME_1, record.firstHalfStoppage)
            putExtra(MatchSummaryActivity.EXTRA_STOPPAGE_TIME_2, record.secondHalfStoppage)
            // Serialize events to JSON
            val gson = com.google.gson.Gson()
            putExtra(MatchSummaryActivity.EXTRA_EVENTS_JSON, gson.toJson(record.events))
        }
        startActivity(intent)
    }
}

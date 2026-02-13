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
                        }
                    )
                }
            }
        }
    }
}


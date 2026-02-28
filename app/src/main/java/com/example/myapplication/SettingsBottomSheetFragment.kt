package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.wear.compose.material.MaterialTheme
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettingsBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "SettingsBottomSheet"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as com.google.android.material.bottomsheet.BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                // 强制展开且不可拖拽
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
                MaterialTheme {
                    SettingsScreen(
                        onHistoryClick = {
                            // 跳转到历史记录页面
                            dismissAllowingStateLoss()
                            val intent = Intent(requireContext(), HistoryActivity::class.java)
                            startActivity(intent)
                        },
                        onAboutClick = {
                            // 跳转到关于页面
                            dismissAllowingStateLoss()
                            val intent = Intent(requireContext(), AboutActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}


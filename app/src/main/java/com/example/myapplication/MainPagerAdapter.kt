package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView

/**
 * ViewPager2 Adapter - 管理3个页面：计时器、历史记录、我的
 */
class MainPagerAdapter(
    private val onTimerPageBound: (View) -> Unit,
    private val onHistoryPageBound: (ComposeView) -> Unit,
    private val onProfilePageBound: (ComposeView) -> Unit
) : RecyclerView.Adapter<MainPagerAdapter.PageViewHolder>() {

    companion object {
        const val PAGE_TIMER = 0
        const val PAGE_HISTORY = 1
        const val PAGE_PROFILE = 2
        const val PAGE_COUNT = 3
    }

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemCount(): Int = PAGE_COUNT

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val layoutId = when (viewType) {
            PAGE_TIMER -> R.layout.page_timer
            PAGE_HISTORY -> R.layout.page_history
            PAGE_PROFILE -> R.layout.page_profile
            else -> throw IllegalArgumentException("Unknown page: $viewType")
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        when (position) {
            PAGE_TIMER -> {
                onTimerPageBound(holder.itemView)
            }
            PAGE_HISTORY -> {
                val composeView = holder.itemView.findViewById<ComposeView>(R.id.historyComposeView)
                onHistoryPageBound(composeView)
            }
            PAGE_PROFILE -> {
                val composeView = holder.itemView.findViewById<ComposeView>(R.id.profileComposeView)
                onProfilePageBound(composeView)
            }
        }
    }
}

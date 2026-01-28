package com.example.myapplication

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.min

class CenterScaleLayoutManager(context: Context) : LinearLayoutManager(context) {

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        val orientation = super.scrollVerticallyBy(dy, recycler, state)
        scaleChildren()
        return orientation
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        super.onLayoutChildren(recycler, state)
        scaleChildren()
    }

    private fun scaleChildren() {
        val mid = height / 2.0f
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            val childMid = (child.top + child.bottom) / 2.0f

            // 计算距离中心的距离
            val d = kotlin.math.min(mid, kotlin.math.abs(mid - childMid))


            val scale = 1.5f - 0.9f * (d / mid)

            child.scaleX = scale
            child.scaleY = scale
            child.alpha = if (scale > 1.0f) 1.0f else 0.4f
        }
    }
}
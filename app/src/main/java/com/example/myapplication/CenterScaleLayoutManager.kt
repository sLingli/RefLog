package com.example.myapplication // ⚠️ 保留你的包名！

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

            // 🔥 激进缩放算法：
            // 离中间越近，系数越接近 1.0 (保持 1.5倍)
            // 离中间越远，系数越小 (最小 0.5倍)

            // 1.5f 是最大倍数，0.9f 是缩放幅度
            val scale = 1.5f - 0.9f * (d / mid)

            child.scaleX = scale
            child.scaleY = scale
            child.alpha = if (scale > 1.0f) 1.0f else 0.4f // 远的变得更透明
        }
    }
}
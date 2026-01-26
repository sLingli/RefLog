package com.example.myapplication // ⚠️ 这一行要保留你自己原来的包名！

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class ColorWheelAdapter(
    private val colors: List<Int>,
    private val onColorSelected: (Int) -> Unit
) : RecyclerView.Adapter<ColorWheelAdapter.ColorViewHolder>() {

    override fun getItemCount(): Int = Int.MAX_VALUE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        // 确保你已经创建了 R.layout.item_color_wheel
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_color_wheel, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val realPosition = position % colors.size
        val color = colors[realPosition]
        holder.circle.setCardBackgroundColor(color)

        // 智能描边：如果是白色(0xFFFFFFFF)，边框变灰，否则白色
        val isWhite = (color == 0xFFFFFFFF.toInt())
        holder.circle.strokeColor = if (isWhite) 0xFF888888.toInt() else 0xFFFFFFFF.toInt()
    }

    class ColorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val circle: MaterialCardView = view.findViewById(R.id.colorCircle)
    }
}
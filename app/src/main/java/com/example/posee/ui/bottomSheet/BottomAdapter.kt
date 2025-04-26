package com.example.poseeui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.posee.R

class BottomAdapter :
    ListAdapter<BottomItem, BottomAdapter.BottomViewHolder>(BottomDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.bottom_sheet_item, parent, false)
        return BottomViewHolder(view)
    }

    override fun onBindViewHolder(holder: BottomViewHolder, position: Int) {
        holder.bind(getItem(position)) //position에 해당하는 data type을 전달
    }

    class BottomViewHolder (private val view: View) : RecyclerView.ViewHolder(view) {
        private val time = view.findViewById<TextView>(R.id.count_time)
        private val explanation = view.findViewById<TextView>(R.id.count_explain)
        private val img = view.findViewById<ImageView>(R.id.count_img)
        fun bind(item: BottomItem) {
            // R.drawable 내 리소스를 바로 세팅
            img.setImageResource(item.imageRes)
            time.text = item.time
            explanation.text = item.explanation
        }
    }
}

class BottomDiffCallback : DiffUtil.ItemCallback<BottomItem>() {
    override fun areItemsTheSame(oldItem: BottomItem, newItem: BottomItem): Boolean {
        return oldItem.time == newItem.time
    }

    override fun areContentsTheSame(oldItem: BottomItem, newItem: BottomItem): Boolean {
        return oldItem == newItem
    }
}
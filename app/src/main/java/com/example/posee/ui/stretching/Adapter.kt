package com.example.posee.ui.stretching

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.lang.reflect.Member
import androidx.core.graphics.toColorInt
import com.example.posee.R

class Adapter :
    ListAdapter<Item, Adapter.ViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position)) //position에 해당하는 data type을 전달
    }

    class ViewHolder (private val view: View) : RecyclerView.ViewHolder(view) {
        private val category = view.findViewById<TextView>(R.id.item_category)
        private val title = view.findViewById<TextView>(R.id.item_title)
        private val sub = view.findViewById<TextView>(R.id.item_sub)
        fun bind(item: Item) {
            category.text = item.category
            title.text = item.title
            sub.text = item.sub
            val drawable = ContextCompat.getDrawable(category.context, item.color)
            category.background = drawable
        }
    }
}

class DiffCallback : DiffUtil.ItemCallback<Item>() {
    override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
        return oldItem == newItem
    }
}
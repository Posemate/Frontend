package com.example.posee.ui.stretching

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.posee.R

class StretchingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_stretching)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // recycler view 연결
        val recyclerView = findViewById<RecyclerView>(R.id.rv_item)
        val adapter = Adapter()
        val itemList = listOf(
            Item("목 전체", "목 전체를 위한 가벼운 스트레칭", "목의 긴장을 풀고 유연성을 높이기", R.drawable.rounded_blue, "https://youtu.be/mUnSpfItRf0?si=i26ZM0gBEBuklVIZ"),
            Item("승모근", "어깨 및 견갑골 주변 스트레칭", "뭉친 승모근 근육을 이완시키기", R.drawable.rounded_purple, "https://youtu.be/WjSnos8l3fM?si=uPBzVofXFBQwo54g"),
            Item("어깨", "목 전체를 위한 가벼운 스트레칭", "어깨 및 견갑골 근육을 풀어주고 가동성 높이기", R.drawable.rounded_pink, "https://youtu.be/U2H_rvHUcEw?si=nbbIuZ7P_6JxKTM-"),
            Item("거북목", "거북목 및 일자목을 위한 스트레칭", "정상적인 경추의 곡선을 회복하고, 근육의 긴장을 풀어주기", R.drawable.rounded_green, "https://youtu.be/kgCj8UUEWjU?si=pvMQhdRD7F5gjo41"),
            Item("승모근", "승모근 이완 스트레칭", "딱딱하게 뭉친 승모근을 시원하게 풀어주기", R.drawable.rounded_purple, "https://youtu.be/dJXZRZvqbYg?si=XLvHuaqaDHId98q7"),
        )
        recyclerView.adapter = adapter
        adapter.submitList(itemList)
    }
}
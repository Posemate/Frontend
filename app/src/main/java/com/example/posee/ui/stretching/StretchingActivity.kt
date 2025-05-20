package com.example.posee.ui.stretching

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.posee.R
import com.example.posee.databinding.ActivityStretchingBinding
import com.google.firebase.database.FirebaseDatabase
import androidx.activity.viewModels
import com.example.posee.ui.mypage.MypageViewModel


class StretchingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStretchingBinding
    private val mypageViewModel: MypageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityStretchingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = sharedPref.getString("logged_in_userId", null)

        if (userId != null) {
            val databaseRef = FirebaseDatabase.getInstance().reference
            databaseRef.child("Users").child(userId).child("username").get()
                .addOnSuccessListener { dataSnapshot ->
                    val username = dataSnapshot.value as? String
                    binding.neckTitle.text = if (username != null) username + "님을 위한 목·어깨 스트레칭 추천" else "목·어깨 스트레칭"
                }
                .addOnFailureListener {
                    binding.neckTitle.text = "목·어깨 스트레칭"
                }

            // 월별 알림 데이터를 서버에서 불러오기
            mypageViewModel.loadChartData(userId)
        } else {
            binding.neckTitle.text = "목·어깨 스트레칭"
        }

        // toolbar
        setSupportActionBar(binding.toolbarStretch)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()  // 현재 액티비티 종료 → 뒤로가기
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
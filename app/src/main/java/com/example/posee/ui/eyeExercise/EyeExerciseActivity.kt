package com.example.posee.ui.eyeExercise

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.posee.R
import com.example.posee.databinding.ActivityEyeExerciseBinding
import com.example.posee.databinding.ActivityStretchingBinding
import com.example.posee.ui.stretching.Adapter
import com.example.posee.ui.stretching.Item

class EyeExerciseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEyeExerciseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_eye_exercise)

        binding = ActivityEyeExerciseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_eye)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // toolbar
        setSupportActionBar(binding.toolbarEye)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // recycler view 연결
        val recyclerView = findViewById<RecyclerView>(R.id.rv_item_eye)
        val adapter = Adapter()
        val itemList = listOf(
            Item("침침한 눈", "침침한 눈 회복 운동", "눈의 피로를 개선하고 눈을 맑게 하기", R.drawable.rounded_orange, "https://www.youtube.com/watch?v=4DRQaBQ1cao"),
            Item("안구건조증", "눈 깜빡임 운동", "노안과 안구건조증 방지하기", R.drawable.rounded_dark_green, "https://www.youtube.com/watch?v=6wBEKwkGvzg"),
            Item("눈의 피로", "눈의 피로 개선을 위한 지압 마사지", "눈이 피곤하고 무거울 때를 위한 마사지 방법", R.drawable.rounded_pink, "https://www.youtube.com/watch?v=H996FQ8e1x4"),
            Item("눈 근육", "간단 눈 근육 스트레칭 운동", "눈의 피로를 풀어주고 시력을 보호하는 스트레칭", R.drawable.rounded_green, "https://www.youtube.com/watch?v=IZHcdtWYtcs"),
            Item("눈 마사지", "눈 주변 지압·마사지 운동", "눈 주변 순환을 촉진하여 눈물샘 및 피로를 완화", R.drawable.rounded_purple, "https://www.youtube.com/watch?v=14i0PK4dEG4"),
            Item("온찜질", "안구 온찜질 방법", "눈의 온찜질로 피로 해소 및 건조증 완화", R.drawable.rounded_blue, "https://www.youtube.com/watch?v=B9RPlmXKQY8"),
            Item("시력 회복", "마츠자키 이사오 시력 회복 운동", "시력 박사의 시력 회복 운동법", R.drawable.rounded_gray, "https://youtu.be/tdDRvzZQ3n4?si=cJw-CznikW76Xhya"),
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
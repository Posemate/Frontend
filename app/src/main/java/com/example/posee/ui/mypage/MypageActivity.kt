package com.example.posee.ui.mypage

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.posee.R
import com.example.posee.databinding.FragmentNotificationsBinding
import com.example.posee.ui.eyeExercise.EyeExerciseActivity
import com.example.posee.ui.loginSignup.LoginActivity
import com.example.posee.ui.stretching.StretchingActivity
import com.example.posee.ui.camera.PoseDetectionService
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class MypageActivity : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val mypageViewModel: MypageViewModel by viewModels()

    private var selectedIndex: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbarMy)
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
        setHasOptionsMenu(true)

        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("logged_in_userId", null)

        if (userId != null) {
            val databaseRef = FirebaseDatabase.getInstance().reference
            databaseRef.child("Users").child(userId).child("username").get()
                .addOnSuccessListener { dataSnapshot ->
                    val username = dataSnapshot.value as? String
                    binding.userNameText.text = if (username != null) username + "님" else "사용자"
                }
                .addOnFailureListener {
                    binding.userNameText.text = "사용자"
                }

            // 월별 알림 데이터를 서버에서 불러오기
            mypageViewModel.loadChartData(userId)
        } else {
            binding.userNameText.text = "사용자"
        }

        val chart = binding.myChart as BarChart
        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        // ViewModel에서 data를 설정
        mypageViewModel.chartData.observe(viewLifecycleOwner) { entries ->
            // 1~12월을 기본값 0으로 초기화한 뒤, 실제 값으로 덮어씌움
            val fullData = (1..12).associateWith { 0 }.toMutableMap()
            entries.forEach { (key, value) ->
                val month = key.split("-")[1].toInt()
                fullData[month] = value
            }

            // BarEntry: 1~12월 전체 생성
            val barEntries = fullData.entries.map {
                BarEntry(it.key.toFloat(), it.value.toFloat())
            }

            val defaultColor = ContextCompat.getColor(requireContext(), R.color.main)
            val highlightColor = Color.rgb(
                (Color.red(defaultColor) * 0.98).toInt(),
                (Color.green(defaultColor) * 0.98).toInt(),
                (Color.blue(defaultColor) * 0.98).toInt()
            )

            // BarDataSet 생성
            val dataSet = BarDataSet(barEntries, "AlarmCount").apply {
                colors = List(barEntries.size) { defaultColor }
                valueTextColor = ContextCompat.getColor(requireContext(), R.color.black)
                valueTextSize = 11f
                setDrawValues(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getBarLabel(barEntry: BarEntry?): String {
                        return if (barEntry != null && selectedIndex != -1 &&
                            barEntry.x == barEntries[selectedIndex].x
                        ) {
                            barEntry.y.toInt().toString()
                        } else {
                            ""
                        }
                    }
                }
            }

            val barData = BarData(dataSet).apply {
                barWidth = 0.8f
            }

            val chart = binding.myChart
            chart.data = barData

            // X축 설정
            chart.xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                axisMinimum = 0.5f
                axisMaximum = 12.5f
                granularity = 1f
                labelCount = 12
                textSize = 11f
                textColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val intVal = value.toInt()
                        return if (intVal in 1..12) intVal.toString() else ""
                    }
                }
                setDrawGridLines(false)
            }

            // Y축 설정
            chart.axisLeft.apply {
                isEnabled = false
                axisMinimum = 0f  // Y축의 최소값을 0으로 강제 설정
            }

            chart.axisRight.apply {
                isEnabled = true
                axisMinimum = 0f
                granularity = 1f
                setLabelCount(4, true)
                textSize = 11f
                textColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)
            }

            // 바차트 클릭 이벤트 처리
            chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {
                        val epsilon = 0.001f
                        val index = barEntries.indexOfFirst { entry ->
                            kotlin.math.abs(entry.x - e.x) < epsilon
                        }
                        if (index != -1) {
                            selectedIndex = index

                            val updatedColors = MutableList(barEntries.size) { defaultColor }
                            updatedColors[index] = highlightColor

                            dataSet.colors = updatedColors
                            chart.invalidate()
                        }
                    }
                }

                override fun onNothingSelected() {
                    selectedIndex = -1
                    dataSet.colors = List(barEntries.size) { defaultColor }
                    chart.invalidate()
                }
            })

            chart.description.isEnabled = false
            chart.legend.isEnabled = false
            chart.invalidate()
        }


        binding.stretchLeft.setOnClickListener {
            startActivity(Intent(requireContext(), StretchingActivity::class.java))
        }

        binding.stretchRight.setOnClickListener {
            startActivity(Intent(requireContext(), EyeExerciseActivity::class.java))
        }

        binding.logoutText.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("로그아웃")
                .setMessage("정말 로그아웃 하시겠습니까?")
                .setPositiveButton("네") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(requireContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    requireActivity().finish()
                }
                .setNegativeButton("아니오") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout_my)
        requireActivity().findViewById<View>(R.id.btn_close_drawer)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        listOf("eye", "neck", "overlay", "background").forEach { key ->
            val switch = requireActivity().findViewById<Switch>(
                resources.getIdentifier("switch_$key", "id", requireActivity().packageName)
            )
            if (switch != null) {
                switch.isChecked = loadSwitchState("${key}_switch_state")
                switch.setOnCheckedChangeListener { _, isChecked ->
                    saveSwitchState("${key}_switch_state", isChecked)
                    if (key == "background") {
                        val serviceIntent = Intent(requireContext(), PoseDetectionService::class.java)
                        if (isChecked) ContextCompat.startForegroundService(requireContext(), serviceIntent)
                        else requireContext().stopService(serviceIntent)
                    }
                }
            }
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {
            val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout_my)
            val closeButton = requireActivity().findViewById<View>(R.id.btn_close_drawer)

            closeButton?.setOnClickListener {
                drawerLayout.closeDrawer(GravityCompat.END)
            } ?: run {
                Toast.makeText(requireContext(), "closeButton을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }

            val switchEye = requireActivity().findViewById<Switch>(R.id.switch_eye)
            val switchNeck = requireActivity().findViewById<Switch>(R.id.switch_neck)
            val switchOverlay = requireActivity().findViewById<Switch>(R.id.switch_overlay)
            val switchBackground = requireActivity().findViewById<Switch>(R.id.switch_background)

            if (switchEye == null || switchNeck == null || switchOverlay == null || switchBackground == null) {
                Toast.makeText(requireContext(), "스위치가 Activity 레이아웃에 존재하지 않습니다.", Toast.LENGTH_LONG).show()
                return@post
            }

            switchEye.isChecked = loadSwitchState("eye_switch_state")
            switchNeck.isChecked = loadSwitchState("neck_switch_state")
            switchOverlay.isChecked = loadSwitchState("overlay_switch_state")
            switchBackground.isChecked = loadSwitchState("background_switch_state")

            switchEye.setOnCheckedChangeListener { _, isChecked ->
                saveSwitchState("eye_switch_state", isChecked)
            }

            switchNeck.setOnCheckedChangeListener { _, isChecked ->
                saveSwitchState("neck_switch_state", isChecked)
            }

            switchOverlay.setOnCheckedChangeListener { _, isChecked ->
                saveSwitchState("overlay_switch_state", isChecked)
            }

            switchBackground.setOnCheckedChangeListener { _, isChecked ->
                saveSwitchState("background_switch_state", isChecked)

                val serviceIntent = Intent(requireContext(), PoseDetectionService::class.java)
                if (isChecked) {
                    ContextCompat.startForegroundService(requireContext(), serviceIntent)
                } else {
                    requireContext().stopService(serviceIntent)
                }
            }
        }
    }

    private fun saveSwitchState(key: String, isChecked: Boolean) {
        val sharedPref = requireActivity().getSharedPreferences("drawer_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean(key, isChecked)
            apply()
        }
    }

    private fun loadSwitchState(key: String): Boolean {
        val sharedPref = requireActivity().getSharedPreferences("drawer_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean(key, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.alarm_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notification -> {
                val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout_my)
                drawerLayout.openDrawer(GravityCompat.END)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout_my)
        drawerLayout.closeDrawer(GravityCompat.END)
    }
}
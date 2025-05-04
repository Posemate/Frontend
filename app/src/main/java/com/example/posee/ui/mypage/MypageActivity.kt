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

class MypageActivity : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val mypageViewModel: MypageViewModel by viewModels()

    private var selectedIndex: Int = -1 // 선택된 막대 index 저장

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbarMy)
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
        setHasOptionsMenu(true)

        val chart = binding.myChart as BarChart
        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        mypageViewModel.chartData.observe(viewLifecycleOwner) { entries ->

            val barEntries = entries.map { it.entry }

            val defaultColor = ContextCompat.getColor(requireContext(), R.color.main)
            val highlightColor = Color.rgb(
                (Color.red(defaultColor) * 0.98).toInt(),
                (Color.green(defaultColor) * 0.98).toInt(),
                (Color.blue(defaultColor) * 0.98).toInt()
            )

            val dataSet = BarDataSet(barEntries, "Stretching").apply {
                colors = List(barEntries.size) { defaultColor }
                valueTextColor = ContextCompat.getColor(requireContext(), R.color.black)
                valueTextSize = 11f
                setDrawValues(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getBarLabel(barEntry: BarEntry?): String {
                        return if (barEntry != null && selectedIndex != -1 &&
                            barEntry.x == barEntries[selectedIndex].x
                        ) {
                            barEntry.y.toString()
                        } else {
                            ""
                        }
                    }
                }
            }

            val barData = BarData(dataSet).apply { barWidth = 0.6f }
            chart.data = barData

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

            chart.axisLeft.isEnabled = false
            chart.axisRight.apply {
                isEnabled = true
                axisMinimum = 0f
                axisMaximum = 6f
                granularity = 1f
                setLabelCount(4, true)
                textSize = 11f
                textColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)
            }

            chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {
                        val index = barEntries.indexOfFirst { it.x == e.x }
                        if (index != -1) {
                            selectedIndex = index
                            dataSet.colors = List(barEntries.size) { defaultColor }.toMutableList().apply {
                                this[index] = highlightColor
                            }
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

        /** Drawer 및 스위치 설정 **/
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
                requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout_my).openDrawer(GravityCompat.END)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout_my).closeDrawer(GravityCompat.END)
    }
}

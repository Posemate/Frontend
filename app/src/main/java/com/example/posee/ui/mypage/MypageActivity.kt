package com.example.posee.ui.mypage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.posee.R
import com.example.posee.databinding.FragmentNotificationsBinding
import com.example.posee.ui.eyeExercise.EyeExerciseActivity
import com.example.posee.ui.loginSignup.LoginActivity
import com.example.posee.ui.stretching.StretchingActivity
import com.example.posee.ui.camera.PoseDetectionService
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth

class MypageActivity : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val mypageViewModel = ViewModelProvider(this).get(MypageViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbarMy)
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
        setHasOptionsMenu(true)

        val chart = binding.myChart
        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        mypageViewModel.chartData.observe(viewLifecycleOwner) { entries ->
            val dataSet = LineDataSet(entries, "Stretching").apply {
                color = ContextCompat.getColor(requireContext(), R.color.gray)
                setCircleColor(ContextCompat.getColor(requireContext(), R.color.main))
                valueTextColor = ContextCompat.getColor(requireContext(), R.color.black)
                lineWidth = 1f
                setDrawCircles(true)
                circleRadius = 2f
                setDrawValues(false)
            }

            val lineData = LineData(dataSet)
            chart.data = lineData

            chart.xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                axisMinimum = 1f
                axisMaximum = 12f
                granularity = 1f
                labelCount = 12
                textSize = 11f
                textColor = ContextCompat.getColor(requireContext(), R.color.main)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val intVal = value.toInt()
                        return if (intVal in 1..12) intVal.toString() else ""
                    }
                }
                setDrawGridLines(false)
            }

            chart.axisRight.apply {
                isEnabled = true
                axisMinimum = 0f
                axisMaximum = 150f
                granularity = 50f
                setLabelCount(4, true)
                textSize = 11f
                textColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)
                enableGridDashedLine(2f, 10f, 0f)
                gridColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
            }
            chart.axisLeft.isEnabled = false
            chart.invalidate()
        }

        mypageViewModel.text.observe(viewLifecycleOwner) { }

        binding.stretchLeft.setOnClickListener {
            val intent = Intent(requireContext(), StretchingActivity::class.java)
            startActivity(intent)
        }

        binding.stretchRight.setOnClickListener {
            val intent = Intent(requireContext(), EyeExerciseActivity::class.java)
            startActivity(intent)
        }

        binding.logoutText.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("로그아웃")
            builder.setMessage("정말 로그아웃 하시겠습니까?")
            builder.setPositiveButton("네") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(requireContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)

                requireActivity().finish()
            }
            builder.setNegativeButton("아니오") { dialog, _ -> dialog.dismiss() }
            builder.show()
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

            /** 백그라운드 알림 스위치 **/
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

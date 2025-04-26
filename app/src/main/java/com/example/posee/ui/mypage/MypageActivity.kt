package com.example.posee.ui.mypage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.DefaultTab.AlbumsTab.value
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.posee.R
import com.example.posee.databinding.FragmentNotificationsBinding
import com.example.posee.ui.loginSignup.LoginActivity
import com.example.posee.ui.stretching.StretchingActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth

class MypageActivity : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mypageViewModel =
            ViewModelProvider(this).get(MypageViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // toolbar
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbarMy)
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
        setHasOptionsMenu(true)

        // 차트 연결
        val chart = binding.myChart

        // 그래프 설명(Description)과 범례(Legend) 숨기기 설정 추가
        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        mypageViewModel.chartData.observe(viewLifecycleOwner) { entries ->
            val dataSet = LineDataSet(entries, "Stretching").apply {
                color = ContextCompat.getColor(requireContext(), R.color.gray)
                // 데이터 포인트(원)의 색상 설정
                setCircleColor(ContextCompat.getColor(requireContext(), R.color.main))
                valueTextColor = ContextCompat.getColor(requireContext(), R.color.black)
                lineWidth = 1f
                setDrawCircles(true)
                circleRadius = 2f
                // 데이터 포인트 위의 숫자(값)를 표시하지 않도록 설정
                setDrawValues(false)
            }

            val lineData = LineData(dataSet)
            chart.data = lineData

            // X축 커스터마이징: 하단에 배치, 1~12까지 레이블, 폰트 크기 11, 지정 색상
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
                setDrawGridLines(false)  // 여기서 그리드 라인 비활성화
            }

            // 오른쪽 Y축 커스터마이징: 0 ~ 150, 간격 50, 폰트 크기 11, 지정 색상 적용
            chart.axisRight.apply {
                isEnabled = true
                axisMinimum = 0f
                axisMaximum = 150f
                granularity = 50f
                setLabelCount(4, true)
                textSize = 11f
                textColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)
                // Y축 그리드 라인을 점선(대시드 라인) 스타일로 변경:
                // 첫 번째 인자: 점(또는 짧은 dash) 길이, 두 번째 인자: 간격, 세 번째 인자: phase (시작점)
                enableGridDashedLine(2f, 10f, 0f)
                gridColor = ContextCompat.getColor(requireContext(), R.color.dark_gray)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
            }
            // 왼쪽 Y축은 사용하지 않음
            chart.axisLeft.isEnabled = false

            chart.invalidate()
        }

        //val textView: TextView = binding.textNotifications
        mypageViewModel.text.observe(viewLifecycleOwner) {
            //textView.text = it
        }

        // 목 스트레칭 버튼 동작
        binding.stretchLeft.setOnClickListener {
            val intent = Intent(requireContext(), StretchingActivity::class.java)
            startActivity(intent)
        }

        val logoutTextView = binding.logoutText  // 로그아웃 TextView

        logoutTextView.setOnClickListener {
            // 다이얼로그 띄우기
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("로그아웃")
            builder.setMessage("정말 로그아웃 하시겠습니까?")
            builder.setPositiveButton("네") { _, _ ->
                // 네 클릭 시 로그아웃 진행
                FirebaseAuth.getInstance().signOut()  // Firebase 로그아웃
                Toast.makeText(requireContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)

                requireActivity().finish()  // 현재 Activity 종료
            }
            builder.setNegativeButton("아니오") { dialog, _ ->
                // 아니오 클릭 시 다이얼로그 닫기
                dialog.dismiss()
            }
            builder.show()
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {
            // drawer
            val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout_my)

            // X 버튼 누르면 닫힘
            val closeButton = requireActivity().findViewById<View>(R.id.btn_close_drawer)
            closeButton?.setOnClickListener {
                // 버튼 클릭 시 Drawer 닫기
                drawerLayout.closeDrawer(GravityCompat.END) // 오른쪽 Drawer인 경우, 또는 필요에 따라 Gravity.START 사용
            } ?: run {
                // null 인 경우 로그 출력
                Toast.makeText(requireContext(), "closeButton을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }

            // Drawer 내부에 위치한 알림 스위치 상태 저장
            val switchEye = requireActivity().findViewById<Switch>(R.id.switch_eye)
            val switchNeck = requireActivity().findViewById<Switch>(R.id.switch_neck)
            val switchOverlay = requireActivity().findViewById<Switch>(R.id.switch_overlay)
            val switchBackground = requireActivity().findViewById<Switch>(R.id.switch_background)

            if (switchEye == null || switchNeck == null || switchOverlay == null || switchBackground == null) {
                // 스위치 중 하나라도 null이면 로그를 남기고 조기 리턴
                Toast.makeText(requireContext(), "스위치가 Activity 레이아웃에 존재하지 않습니다.", Toast.LENGTH_LONG).show()
                return@post
            }

            // 저장된 상태 불러와 초기 상태 적용
            switchEye.isChecked = loadSwitchState("eye_switch_state")
            switchNeck.isChecked = loadSwitchState("neck_switch_state")
            switchOverlay.isChecked = loadSwitchState("overlay_switch_state")
            switchBackground.isChecked = loadSwitchState("background_switch_state")

            // 스위치 상태 변경 시 SharedPreferences에 저장
            switchEye.setOnCheckedChangeListener { _, isChecked ->
                Log.e("SwitchEvent", "switchEye toggled: $isChecked")
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
            }
        }
    }

    // SharedPreferences를 통해 스위치 상태 저장
    private fun saveSwitchState(key: String, isChecked: Boolean) {
        val sharedPref = requireActivity().getSharedPreferences("drawer_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean(key, isChecked)
            apply()
        }
        Log.e("SwitchState", "Saved $key = $isChecked")
    }

    // SharedPreferences를 통해 저장된 스위치 상태 불러오기 (저장된 값이 없으면 기본값 false)
    private fun loadSwitchState(key: String): Boolean {
        val sharedPref = requireActivity().getSharedPreferences("drawer_prefs", Context.MODE_PRIVATE)
        val value = sharedPref.getBoolean(key, false)  // 여기서 value를 먼저 읽고
        Log.e("SwitchState", "Loaded $key = $value")  // 그리고 로그 찍기
        return value
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.alarm_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    // drawer 연결
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notification -> {
                val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout_my)
                drawerLayout.openDrawer(GravityCompat.END)  // 오른쪽에서 drawer 열기
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        // Activity에 있는 DrawerLayout을 가져와서 닫기
        val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout_my)
        // Drawer가 열려 있다면 닫기 (오른쪽 Drawer인 경우 Gravity.END 사용)
        drawerLayout.closeDrawer(GravityCompat.END)
    }
}
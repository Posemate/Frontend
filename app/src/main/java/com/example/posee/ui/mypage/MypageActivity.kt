package com.example.posee.ui.mypage

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.posee.R
import com.example.posee.databinding.FragmentNotificationsBinding
import com.example.posee.ui.stretching.StretchingActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

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

        return root
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
                drawerLayout.openDrawer(android.view.Gravity.END)  // 오른쪽에서 drawer 열기
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
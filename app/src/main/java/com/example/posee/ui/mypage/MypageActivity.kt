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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.posee.R
import com.example.posee.databinding.FragmentNotificationsBinding
import com.example.posee.ui.stretching.StretchingActivity
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

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

        mypageViewModel.chartData.observe(viewLifecycleOwner) { entries ->
            val dataSet = LineDataSet(entries, "Stretching").apply {
                color = ContextCompat.getColor(requireContext(), R.color.main)
                valueTextColor = ContextCompat.getColor(requireContext(), R.color.black)
                lineWidth = 2f
                setDrawCircles(true)
                circleRadius = 4f
            }

            val lineData = LineData(dataSet)
            chart.data = lineData
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notification -> {
                Toast.makeText(requireContext(), "알림 클릭됨!", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
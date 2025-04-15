package com.example.posee.ui.calendar

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.posee.R
import com.example.posee.databinding.FragmentHomeBinding
import com.example.poseeui.BottomAdapter
import com.example.poseeui.BottomItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener


class CalendarActivity : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var calendarView: MaterialCalendarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val calendarViewModel =
            ViewModelProvider(this).get(CalendarViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // toolbar
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)


        //val textView: TextView = binding.textHome
        calendarViewModel.text.observe(viewLifecycleOwner) {
            //textView.text = it
        }

        val calendarView = binding.root.findViewById<MaterialCalendarView>(R.id.calendar_view)

        // 날짜 선택 리스너 등록
        calendarView.setOnDateChangedListener(OnDateSelectedListener { widget, date, selected ->
            val formattedDate = "${date.year}년 ${date.month + 1}월 ${date.day}일"
            showBottomSheet(formattedDate)
        })

        // 달력 초기화 옵션 (예: 오늘 날짜로 셋팅)
        calendarView.selectedDate = CalendarDay.today()

        /**
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // month는 0부터 시작하므로 +1 해줘야 합니다!
            val formattedDate = "${year}년 ${month + 1}월 ${dayOfMonth}일"
            showBottomSheet(formattedDate)
        }**/

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // BottomSheetDialog를 띄워 날짜 정보를 보여주는 함수
    private fun showBottomSheet(dateString: String) {
        val bottomSheetView = layoutInflater.inflate(R.layout.activity_bottom_sheet, null)

        // 날짜 텍스트 설정
        val dateTextView = bottomSheetView.findViewById<TextView>(R.id.calendar_date)
        dateTextView.text = dateString

        // RecyclerView 세팅
        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.rv_bottom_item)
        val adapter = BottomAdapter()
        val itemList = listOf(
            BottomItem("17:05", "화면과 너무 가까워요."),
            BottomItem("16:55", "10분 동안 적당한 거리를 유지했어요.")
        )
        recyclerView.adapter = adapter
        adapter.submitList(itemList)

        val bottomSheetDialog = BottomSheetDialog(requireContext())

        // BottomSheetDialog 배경 투명하게 설정
        bottomSheetDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()

        // 내부 View의 기본 배경도 제거 (투명하게)
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.setBackgroundResource(android.R.color.transparent)

        bottomSheet?.let {
            it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            it.requestLayout()

            val displayMetrics = resources.displayMetrics
            val halfScreenHeight = displayMetrics.heightPixels / 2

            val behavior = BottomSheetBehavior.from(it)
            behavior.peekHeight = halfScreenHeight
            behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.alarm_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    // drawer 연결
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notification -> {
                val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout)
                drawerLayout.openDrawer(android.view.Gravity.END)  // 오른쪽에서 drawer 열기
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
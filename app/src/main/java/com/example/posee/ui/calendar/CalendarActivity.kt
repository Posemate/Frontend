package com.example.posee.ui.calendar

import android.content.Context
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
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
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

        // 색 설정
        val eventDates1 = listOf(
            CalendarDay.from(2025, 4, 18),
            CalendarDay.from(2025, 4, 20),
            CalendarDay.from(2025, 4, 22)
        )
        val eventDates2 = listOf(
            CalendarDay.from(2025, 4, 1),
            CalendarDay.from(2025, 4, 4),
            CalendarDay.from(2025, 4, 21)
        )
        val eventDates3 = listOf(
            CalendarDay.from(2025, 4, 5),
            CalendarDay.from(2025, 4, 17),
            CalendarDay.from(2025, 4, 20)
        )
        val eventDates4 = listOf(
            CalendarDay.from(2025, 4, 6),
            CalendarDay.from(2025, 4, 10),
            CalendarDay.from(2025, 4, 29)
        )
        // ContextCompat.getColor()를 사용해 색상을 리소스에서 가져옵니다.
        val eventColor1 = ContextCompat.getColor(requireContext(), R.color.main_20)
        val eventColor2 = ContextCompat.getColor(requireContext(), R.color.main_40)
        val eventColor3 = ContextCompat.getColor(requireContext(), R.color.main_60)
        val eventColor4 = ContextCompat.getColor(requireContext(), R.color.main_90)

        // 커스텀 데코레이터(EventDecorator)를 적용합니다.
        calendarView.addDecorator(CalendarDecorator(eventColor1, eventDates1))
        calendarView.addDecorator(CalendarDecorator(eventColor2, eventDates2))
        calendarView.addDecorator(CalendarDecorator(eventColor3, eventDates3))
        calendarView.addDecorator(CalendarDecorator(eventColor4, eventDates4))

        // 필요시 데코레이터 갱신
        calendarView.invalidateDecorators()

        /**
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // month는 0부터 시작하므로 +1 해줘야 합니다!
            val formattedDate = "${year}년 ${month + 1}월 ${dayOfMonth}일"
            showBottomSheet(formattedDate)
        }**/

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {
            // drawer
            val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout)

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
    }

    // SharedPreferences를 통해 저장된 스위치 상태 불러오기 (저장된 값이 없으면 기본값 false)
    private fun loadSwitchState(key: String): Boolean {
        val sharedPref = requireActivity().getSharedPreferences("drawer_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean(key, false)  // 기본값은 false로 지정
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
            BottomItem(R.drawable.ic_eyes, "17:05", "화면과 너무 가까워요."),
            BottomItem(R.drawable.neck,"16:55", "자세를 조금만 고쳐볼까요!")
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
                drawerLayout.openDrawer(GravityCompat.END)  // 오른쪽에서 drawer 열기
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        // Activity에 있는 DrawerLayout을 가져와서 닫기
        val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout)
        // Drawer가 열려 있다면 닫기 (오른쪽 Drawer인 경우 Gravity.END 사용)
        drawerLayout.closeDrawer(GravityCompat.END)
    }
}
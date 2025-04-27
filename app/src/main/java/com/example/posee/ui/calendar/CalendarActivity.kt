package com.example.posee.ui.calendar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.posee.R
import com.example.posee.databinding.FragmentHomeBinding
import com.example.posee.ui.camera.PoseDetectionService
import com.example.poseeui.BottomAdapter
import com.example.poseeui.BottomItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener

class CalendarActivity : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var calendarView: MaterialCalendarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val calendarViewModel = ViewModelProvider(this).get(CalendarViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)

        calendarViewModel.text.observe(viewLifecycleOwner) {}

        val calendarView = binding.root.findViewById<MaterialCalendarView>(R.id.calendar_view)

        calendarView.setOnDateChangedListener(OnDateSelectedListener { widget, date, selected ->
            val formattedDate = "${date.year}년 ${date.month + 1}월 ${date.day}일"
            showBottomSheet(formattedDate)
        })

        calendarView.selectedDate = CalendarDay.today()

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

        val eventColor1 = ContextCompat.getColor(requireContext(), R.color.main_20)
        val eventColor2 = ContextCompat.getColor(requireContext(), R.color.main_40)
        val eventColor3 = ContextCompat.getColor(requireContext(), R.color.main_60)
        val eventColor4 = ContextCompat.getColor(requireContext(), R.color.main_90)

        calendarView.addDecorator(CalendarDecorator(eventColor1, eventDates1))
        calendarView.addDecorator(CalendarDecorator(eventColor2, eventDates2))
        calendarView.addDecorator(CalendarDecorator(eventColor3, eventDates3))
        calendarView.addDecorator(CalendarDecorator(eventColor4, eventDates4))

        calendarView.invalidateDecorators()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {
            val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout)
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

    private fun showBottomSheet(dateString: String) {
        val bottomSheetView = layoutInflater.inflate(R.layout.activity_bottom_sheet, null)

        val dateTextView = bottomSheetView.findViewById<TextView>(R.id.calendar_date)
        dateTextView.text = dateString

        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.rv_bottom_item)
        val adapter = BottomAdapter()
        val itemList = listOf(
            BottomItem(R.drawable.ic_eyes, "17:05", "화면과 너무 가까워요."),
            BottomItem(R.drawable.neck, "16:55", "자세를 조금만 고쳐볼까요!")
        )
        recyclerView.adapter = adapter
        adapter.submitList(itemList)

        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notification -> {
                val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout)
                drawerLayout.openDrawer(GravityCompat.END)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.END)
    }
}

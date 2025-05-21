package com.example.posee.ui.calendar

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.posee.R
import com.example.posee.databinding.FragmentHomeBinding
import com.example.posee.network.AlarmLogResponse
import com.example.posee.network.RetrofitClient
import com.example.posee.ui.camera.PoseDetectionService
import com.example.poseeui.BottomAdapter
import com.example.poseeui.BottomItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter

class CalendarActivity : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var calendarView: MaterialCalendarView
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val calendarViewModel = ViewModelProvider(this).get(CalendarViewModel::class.java)

        val sharedPref = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userId = sharedPref.getString("logged_in_userId", null)
            ?: throw IllegalStateException("로그인된 사용자 ID가 없습니다.")

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)

        calendarViewModel.text.observe(viewLifecycleOwner) {}

        calendarView = binding.root.findViewById(R.id.calendar_view)

        calendarView.setOnDateChangedListener(OnDateSelectedListener { _, date, _ ->
            showBottomSheet(date.date)
        })

        calendarView.selectedDate = CalendarDay.today()

        val eventColor1 = ContextCompat.getColor(requireContext(), R.color.main_10)
        val eventColor2 = ContextCompat.getColor(requireContext(), R.color.main_30)
        val eventColor3 = ContextCompat.getColor(requireContext(), R.color.main_50)
        val eventColor4 = ContextCompat.getColor(requireContext(), R.color.main_90)

        RetrofitClient.apiService().getAlarmCountByDate(userId)
            .enqueue(object : Callback<Map<String, Long>> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<Map<String, Long>>, response: Response<Map<String, Long>>) {
                    if (response.isSuccessful) {
                        val countMap = response.body() ?: return
                        val group1 = mutableListOf<CalendarDay>()
                        val group2 = mutableListOf<CalendarDay>()
                        val group3 = mutableListOf<CalendarDay>()
                        val group4 = mutableListOf<CalendarDay>()

                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        for ((dateStr, count) in countMap) {
                            if (count < 5) continue
                            val localDate = LocalDate.parse(dateStr, formatter)
                            val calendarDay = CalendarDay.from(localDate.year, localDate.monthValue, localDate.dayOfMonth)

                            Log.d("디버깅", "dateStr=$dateStr → localDate=$localDate → calendarDay=${calendarDay.year}-${calendarDay.month}-${calendarDay.day}")

                            when (count) {
                                in 1..20 -> group1.add(calendarDay)
                                in 21..40 -> group2.add(calendarDay)
                                in 41..60 -> group3.add(calendarDay)
                                else -> group4.add(calendarDay)
                            }
                        }

                        calendarView.addDecorator(CalendarDecorator(requireContext(), eventColor1, group1))
                        calendarView.addDecorator(CalendarDecorator(requireContext(), eventColor2, group2))
                        calendarView.addDecorator(CalendarDecorator(requireContext(), eventColor3, group3))
                        calendarView.addDecorator(CalendarDecorator(requireContext(), eventColor4, group4))

                        calendarView.invalidateDecorators()
                    }
                }

                override fun onFailure(call: Call<Map<String, Long>>, t: Throwable) {
                    Toast.makeText(requireContext(), "서버 통신 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })

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

    private fun showBottomSheet(selectedDate: LocalDate) {
        val dateParam = selectedDate.format(DateTimeFormatter.ISO_DATE)
        val dateString = "${selectedDate.year}년 ${selectedDate.monthValue}월 ${selectedDate.dayOfMonth}일"

        val bottomSheetView = layoutInflater.inflate(R.layout.activity_bottom_sheet, null)
        val dateTextView = bottomSheetView.findViewById<TextView>(R.id.calendar_date)
        val countTextView = bottomSheetView.findViewById<TextView>(R.id.calendar_count)
        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.rv_bottom_item)
        val spinner = bottomSheetView.findViewById<Spinner>(R.id.option_spinner)
        val adapter = BottomAdapter()
        recyclerView.adapter = adapter

        dateTextView.text = dateString

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val filter = when (position) {
                    1 -> "poor"
                    2 -> "close"
                    else -> "all"
                }

                RetrofitClient.apiService()
                    .getLogs(userId = userId, date = dateParam, filter = filter)
                    .enqueue(object : Callback<List<AlarmLogResponse>> {
                        override fun onResponse(call: Call<List<AlarmLogResponse>>, response: Response<List<AlarmLogResponse>>) {
                            if (response.isSuccessful) {
                                val body = response.body() ?: emptyList()

                                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                                val sortedByRecent = body.sortedByDescending { dto ->
                                    LocalTime.parse(dto.time, timeFormatter)
                                }

                                val items = sortedByRecent.map { dto ->
                                    val resId = when (dto.postureType) {
                                        3 -> R.drawable.ic_eyes
                                        2 -> R.drawable.neck
                                        else -> R.drawable.posee_logo
                                    }
                                    BottomItem(
                                        imageRes = resId,
                                        time = dto.time,
                                        explanation = when (dto.postureType) {
                                            3 -> "너무 가까워요!"
                                            2 -> "자세를 조금만 고쳐볼까요!"
                                            else -> ""
                                        }
                                    )
                                }
                                adapter.submitList(items)
                                countTextView.text = items.size.toString()
                            } else {
                                Toast.makeText(requireContext(), "알람 내역 조회 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<List<AlarmLogResponse>>, t: Throwable) {
                            Toast.makeText(requireContext(), "서버 통신 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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

        spinner.setSelection(0)
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

package com.example.posee.ui.calender

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.posee.R
import com.example.posee.databinding.FragmentHomeBinding
import com.example.poseeui.BottomAdapter
import com.example.poseeui.BottomItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class CalenderFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val calenderViewModel =
            ViewModelProvider(this).get(CalenderViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
// 👉 여기서 바텀시트 뷰 inflate
        val bottomSheetView = layoutInflater.inflate(R.layout.activity_bottom_sheet, null)

        // RecyclerView & Adapter 연결
        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.rv_bottom_item)
        val adapter = BottomAdapter()
        val itemList = listOf(
            BottomItem("17:05", "화면과 너무 가까워요."),
            BottomItem("16:55", "10분 동안 적당한 거리를 유지했어요.")
        )
        recyclerView.adapter = adapter
        adapter.submitList(itemList)

        // BottomSheetDialog 설정
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()

        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            it.requestLayout()

            val displayMetrics = resources.displayMetrics
            val halfScreenHeight = displayMetrics.heightPixels / 2

            val behavior = BottomSheetBehavior.from(it)
            behavior.peekHeight = halfScreenHeight
            behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        }

        //val textView: TextView = binding.textHome
        calenderViewModel.text.observe(viewLifecycleOwner) {
            //textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
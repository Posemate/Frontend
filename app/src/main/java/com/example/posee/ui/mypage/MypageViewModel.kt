package com.example.posee.ui.mypage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.BarEntry

class MypageViewModel : ViewModel() {

    private val _chartData = MutableLiveData<List<CustomEntry>>()
    val chartData: LiveData<List<CustomEntry>> = _chartData

    init {
        loadChartData()
    }

    private fun loadChartData() {
        val entries = listOf(
            CustomEntry(1, BarEntry(1f, 4.2f)),
            CustomEntry(2, BarEntry(2f, 4.0f)),
            CustomEntry(3, BarEntry(3f, 4.4f)),
            CustomEntry(4, BarEntry(4f, 3.8f)),
            CustomEntry(5, BarEntry(5f, 3.2f)),
            CustomEntry(6, BarEntry(6f, 3.3f)),
            CustomEntry(7, BarEntry(7f, 2.7f)),
            CustomEntry(8, BarEntry(8f, 2.9f)),
            CustomEntry(9, BarEntry(9f, 3.0f)),
            CustomEntry(10, BarEntry(10f, 2.1f)),
            CustomEntry(11, BarEntry(11f, 1.9f)),
            CustomEntry(12, BarEntry(12f, 1.6f))
        )
        _chartData.value = entries
    }
}

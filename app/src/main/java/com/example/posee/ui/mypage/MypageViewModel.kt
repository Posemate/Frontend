package com.example.posee.ui.mypage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.Entry

class MypageViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = ""
    }
    val text: LiveData<String> = _text

    // 차트 데이터 추가
    private val _chartData = MutableLiveData<List<Entry>>()
    val chartData: LiveData<List<Entry>> = _chartData

    init {
        loadChartData()
    }

    private fun loadChartData() {
        val entries = listOf(
            Entry(1f, 4.2f),
            Entry(2f, 4.0f),
            Entry(3f, 4.4f),
            Entry(4f, 3.8f),
            Entry(5f, 3.2f),
            Entry(6f, 3.3f),
            Entry(7f, 2.7f),
            Entry(8f, 2.9f),
            Entry(9f, 3.0f),
            Entry(10f, 2.1f),
            Entry(11f, 1.9f),
            Entry(12f, 1.6f)
        )
        _chartData.value = entries
    }
}
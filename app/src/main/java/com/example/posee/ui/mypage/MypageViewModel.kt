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
            Entry(1f, 1f),
            Entry(2f, 2f),
            Entry(3f, 0f),
            Entry(4f, 4f),
            Entry(5f, 3f)
        )
        _chartData.value = entries
    }
}
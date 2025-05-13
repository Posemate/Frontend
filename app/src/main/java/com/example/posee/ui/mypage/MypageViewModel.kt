package com.example.posee.ui.mypage

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.posee.network.RetrofitClient

class MypageViewModel : ViewModel() {

    private val _monthlyAlarmCounts = MutableLiveData<Map<String, Int>>()
    val chartData: LiveData<Map<String, Int>> get() = _monthlyAlarmCounts

    fun loadChartData(userId: String) {
        val call = RetrofitClient.apiService().getAlarmCountByDate(userId)
        call.enqueue(object : Callback<Map<String, Long>> {
            override fun onResponse(
                call: Call<Map<String, Long>>,
                response: Response<Map<String, Long>>
            ) {
                if (response.isSuccessful) {
                    val alarmCounts = response.body() ?: emptyMap()

                    // 월별로 그룹핑
                    val monthlyCounts = mutableMapOf<String, Int>()

                    for ((dateStr, count) in alarmCounts) {
                        val monthKey = dateStr.substring(0, 7) // "YYYY-MM"
                        monthlyCounts[monthKey] = (monthlyCounts[monthKey] ?: 0) + count.toInt()
                    }

                    _monthlyAlarmCounts.postValue(monthlyCounts)
                } else {
                    println("API 오류: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Map<String, Long>>, t: Throwable) {
                println("API 호출 실패: ${t.message}")
            }
        })
    }
}

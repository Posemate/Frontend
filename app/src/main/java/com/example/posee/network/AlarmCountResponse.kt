package com.example.posee.network

// 알림 횟수를 월별로 그룹핑하기 위한 데이터 클래스
data class AlarmCountResponse(

    val date: String,  // "YYYY-MM" 형식으로 월별 날짜
    val count: Long    // 해당 월에 대한 알림 횟수
)

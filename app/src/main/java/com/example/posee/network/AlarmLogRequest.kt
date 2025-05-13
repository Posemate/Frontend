package com.example.posee.network

data class AlarmLogRequest(
    val userId: String,
    val alarmTime: String,   // ISO 8601 포맷: "2025-05-13T16:09:52.248Z"
    val postureType: Int
)

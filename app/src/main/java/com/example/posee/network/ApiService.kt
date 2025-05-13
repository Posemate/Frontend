package com.example.posee.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("api/alarm-logs")
    fun getLogs(
        @Query("userId") userId: String,
        @Query("date") date: String,       // "YYYY-MM-DD"
        @Query("filter") filter: String    // "all"|"good"|"poor"|"close"
    ): Call<List<AlarmLogResponse>>

    @POST("/api/alarm-logs")
    fun postAlarmLog(
        @Body request: AlarmLogRequest
    ): Call<Void>

    @GET("api/alarm-logs/count-by-date")
    fun getAlarmCountByDate(
        @Query("userId") userId: String
    ): Call<Map<String, Long>>
}
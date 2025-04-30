package com.example.posee.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api/alarm-logs")
    fun getLogs(
        @Query("userId") userId: String,
        @Query("date") date: String,       // "YYYY-MM-DD"
        @Query("filter") filter: String    // "all"|"good"|"poor"|"close"
    ): Call<List<AlarmLogResponse>>
}
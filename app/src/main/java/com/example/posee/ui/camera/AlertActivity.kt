package com.example.posee.ui.camera

import android.app.Activity
import android.os.Bundle
import com.example.posee.R

class AlertActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_result_bubble)

        // 몇 초 뒤 자동 종료하거나 사용자 인터랙션 대기
        // finish() 도 사용 가능
    }
}
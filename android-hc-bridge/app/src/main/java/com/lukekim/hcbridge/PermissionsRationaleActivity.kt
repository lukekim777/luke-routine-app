package com.lukekim.hcbridge

import android.app.Activity
import android.os.Bundle
import android.widget.Button

/**
 * Android 14+ Health Connect 가 요구하는 "권한 사용 이유" 화면입니다.
 * Health Connect 설정 화면에서 앱 항목을 눌렀을 때 이 화면이 열립니다.
 */
class PermissionsRationaleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rationale)
        findViewById<Button>(R.id.closeButton).setOnClickListener { finish() }
    }
}

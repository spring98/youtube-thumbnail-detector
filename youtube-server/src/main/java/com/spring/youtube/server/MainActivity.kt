package com.spring.youtube.server

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serviceIntent =
            Intent(this, HostingService::class.java) // MyBackgroundService 를 실행하는 인텐트 생성

        // 현재 안드로이드 버전 점검
        startForegroundService(serviceIntent) // 서비스 인텐트를 전달한 foregroundService 시작 메서드 실행
    }

}
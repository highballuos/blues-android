package com.highballuos.blues

import android.app.Application
import com.highballuos.blues.setting.GlobalSharedPreferences

class App: Application() {
    companion object {
        lateinit var PREFS: GlobalSharedPreferences

        // Setting static variables
        var CURRENT_PACKAGE_NAME = ""
        var CAPITALIZATION = false   // 영어 첫 글자 대문자화 Flag
        var PREDICTION = true  // 키보드 자동 완성 기능 활성화 Flag
        var DEBOUNCE_DELAY_MILLIS = 700L    // debounce 대기 시간
    }

    override fun onCreate() {
        PREFS = GlobalSharedPreferences(applicationContext)
        super.onCreate()
    }
}
package com.highballuos.blues.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.highballuos.blues.R
import com.highballuos.blues.ui.check_setting.CheckSettingActivity

class SplashActivity: AppCompatActivity() {
    private val splashDisplayLength = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({
            val mIntent = Intent(this, CheckSettingActivity::class.java)
            startActivity(mIntent)
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }, splashDisplayLength)
    }

    override fun onBackPressed() {
        // We don't want the splash screen to be interrupted
    }
}
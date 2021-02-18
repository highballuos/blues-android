package com.highballuos.blues.ui.splash

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.highballuos.blues.R
import com.highballuos.blues.ui.check_setting.CheckSettingActivity
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        splash_lottie_animation.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                val mIntent = Intent(this@SplashActivity, CheckSettingActivity::class.java)
                startActivity(mIntent)
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }

            override fun onAnimationCancel(animation: Animator?) {}

            override fun onAnimationRepeat(animation: Animator?) {}
        })

        splash_lottie_animation.playAnimation()
    }

    override fun onBackPressed() {
        // We don't want the splash screen to be interrupted
    }
}
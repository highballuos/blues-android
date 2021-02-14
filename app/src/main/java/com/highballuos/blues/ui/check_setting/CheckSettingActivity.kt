package com.highballuos.blues.ui.check_setting

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.highballuos.blues.R
import com.highballuos.blues.inputmethod.service.BluesIME
import com.highballuos.blues.ui.main.MainActivity
import kotlinx.android.synthetic.main.activity_check_setting.*

class CheckSettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_setting)

        setupUI()
        updateViewWithIMESettings()
        setupFAB()
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        // SDK 버전에 따라 필수 설정 요소가 달라짐
        // 권한 사용하지 않을 것이므로 첫 번째 LinearLayout 제거
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ll_container.removeView(ll_permission)
            tv_enable_index.text = "01"
            tv_default_index.text = "02"
        }
    }

    private fun setupFAB() {
        fab1_check_setting_activity.setOnClickListener {
            if (Build.VERSION.SDK_INT > 20) {
                val mIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(mIntent)
            }
        }

        fab2_check_setting_activity.setOnClickListener {
            val mIntent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            startActivity(mIntent)
        }

        fab3_check_setting_activity.setOnClickListener {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showInputMethodPicker()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            updateViewWithIMESettings()
        }
    }

    private fun updateViewWithIMESettings() {
        var isPermissionGranted = false
        var isEnabled = false
        var isDefault = false

        // 1. Usage Stats 권한 검사 / L 이상일 경우만 하면 됨
        if (Build.VERSION.SDK_INT > 20) {
            if (isPermissionGranted()) {
                fab1_check_setting_activity.isClickable = false
                fab1_check_setting_activity.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_baseline_check_24
                    )
                )
                isPermissionGranted = true
            } else {
                fab1_check_setting_activity.isClickable = true
                fab1_check_setting_activity.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_baseline_east_24
                    )
                )
            }
        }

        // 2. 활성화 검사
        if (isEnabled()) {
            fab2_check_setting_activity.isClickable = false
            fab2_check_setting_activity.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_baseline_check_24
                )
            )
            isEnabled = true
        } else {
            fab2_check_setting_activity.isClickable = true
            fab2_check_setting_activity.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_baseline_east_24
                )
            )
        }

        // 3. 기본 IME 검사
        if (isDefault()) {
            fab3_check_setting_activity.isClickable = false
            fab3_check_setting_activity.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_baseline_check_24
                )
            )
            isDefault = true
        } else {
            fab3_check_setting_activity.isClickable = true
            fab3_check_setting_activity.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_baseline_east_24
                )
            )
        }

        if (isPermissionGranted && isEnabled && isDefault) {
            val mIntent = Intent(this, MainActivity::class.java)
            startActivity(mIntent)
            finish()
        }
    }

    private fun isPermissionGranted(): Boolean {
        var granted = false
        val appOpsManager = getSystemService(APP_OPS_SERVICE) as AppOpsManager

        val mode = if (Build.VERSION.SDK_INT > 28) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else if (Build.VERSION.SDK_INT > 20) {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            -1
        }

        if (mode == AppOpsManager.MODE_DEFAULT && Build.VERSION.SDK_INT > 22) {
            granted =
                checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {    // 22 이하에서는 그냥 이렇게 해도 되는 듯
            granted = (mode == AppOpsManager.MODE_ALLOWED)
        }
        return granted
    }

    private fun isDefault(): Boolean {
        val defaultInputMethodId: String = Settings.Secure.getString(
            application.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
        val defaultInputMethod = ComponentName.unflattenFromString(defaultInputMethodId)
        val mInputMethod = ComponentName(application, BluesIME::class.java)
        return mInputMethod == defaultInputMethod
    }

    private fun isEnabled(): Boolean {
        var isEnabled = false
        val packageLocal = packageName
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val mInputMethodProperties: List<InputMethodInfo> =
            inputMethodManager.enabledInputMethodList

        for (inputMethod in mInputMethodProperties) {
            val packageName = inputMethod.packageName
            if (packageName == packageLocal) {
                isEnabled = true
            }
        }
        return isEnabled
    }
}
package com.highballuos.blues.sharedpreferences

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    companion object {
        const val PREFS_FILENAME = "blues_preference"
        const val CAPITALIZATION_KEY = "CAPITALIZATION_KEY"
        const val DEBOUNCE_DELAY_MILLIS_KEY = "DEBOUNCE_DELAY_MILLIS_KEY"
    }
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    fun getString(key: String, defValue: String): String {
        return prefs.getString(key, defValue).toString()
    }

    fun setString(key: String, str: String) {
        prefs.edit().putString(key, str).apply()
    }

    fun getLong(key: String, defValue: Long): Long {
        return prefs.getLong(key, defValue)
    }

    fun setLong(key: String, longVal: Long) {
        prefs.edit().putLong(key, longVal).apply()
    }

    fun getBoolean(key: String, defValue: Boolean): Boolean {
        return prefs.getBoolean(key, defValue)
    }

    fun setBoolean(key: String, booleanVal: Boolean) {
        prefs.edit().putBoolean(key, booleanVal).apply()
    }

    fun removeValue(key: String) {
        prefs.edit().remove(key).apply()
    }
}
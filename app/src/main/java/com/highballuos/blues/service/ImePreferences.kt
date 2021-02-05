package com.highballuos.blues.service

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceActivity
import com.highballuos.blues.R
import com.highballuos.blues.inputmethodcommon.InputMethodSettingsFragment

/**
 * Displays the IME preferences inside the input method setting.
 */
class ImePreferences : PreferenceActivity() {
    override fun getIntent(): Intent {
        val modIntent = Intent(super.getIntent())
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, Settings::class.java.name)
        modIntent.putExtra(EXTRA_NO_HEADERS, true)
        return modIntent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // We overwrite the title of the activity, as the default one is "Voice Search".
        setTitle(R.string.settings_name)
    }

    override fun isValidFragment(fragmentName: String): Boolean {
        return Settings::class.java.name == fragmentName
    }

    class Settings : InputMethodSettingsFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setInputMethodSettingsCategoryTitle(R.string.language_selection_title)
            setSubtypeEnablerTitle(R.string.select_language)

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.ime_preferences)
        }
    }
}

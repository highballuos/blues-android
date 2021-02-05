package com.highballuos.blues.ui.main

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.highballuos.blues.App.Companion.CAPITALIZATION
import com.highballuos.blues.App.Companion.DEBOUNCE_DELAY_MILLIS
import com.highballuos.blues.App.Companion.PREFS
import com.highballuos.blues.R
import com.highballuos.blues.setting.GlobalSharedPreferences.Companion.CAPITALIZATION_KEY
import com.highballuos.blues.setting.GlobalSharedPreferences.Companion.DEBOUNCE_DELAY_MILLIS_KEY
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupUI()
    }

    private fun setupUI() {
        setupSwitch()
        setupSpinner()
        setupListener()
    }

    private fun setupSwitch() {
        sw_capitalization.isChecked = PREFS.getBoolean(CAPITALIZATION_KEY, false)
    }

    private fun setupSpinner() {
        val spinnerValue = PREFS.getLong(DEBOUNCE_DELAY_MILLIS_KEY, 700L).toString()

        ArrayAdapter.createFromResource(
            this,
            R.array.spinner_labels_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner_delay_millis.adapter = adapter
            spinner_delay_millis.setSelection(adapter.getPosition(spinnerValue))
        }
    }

    private fun setupListener() {
        sw_capitalization.setOnCheckedChangeListener { _, isChecked ->
            PREFS.setBoolean(CAPITALIZATION_KEY, isChecked)
            CAPITALIZATION = isChecked
        }

        spinner_delay_millis.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                parent?.let {
                    val delayMillis = it.getItemAtPosition(position).toString().toLong()
                    PREFS.setLong(DEBOUNCE_DELAY_MILLIS_KEY, delayMillis)
                    DEBOUNCE_DELAY_MILLIS = delayMillis
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}
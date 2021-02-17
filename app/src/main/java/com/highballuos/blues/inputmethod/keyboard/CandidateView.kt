/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.highballuos.blues.inputmethod.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.view.View
import android.widget.LinearLayout
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.highballuos.blues.App.Companion.CURRENT_PACKAGE_NAME
import com.highballuos.blues.App.Companion.PREDICTION
import com.highballuos.blues.App.Companion.PREFS
import com.highballuos.blues.R
import com.highballuos.blues.inputmethod.service.BluesIME
import kotlinx.android.synthetic.main.layout_candidate.view.*

@SuppressLint("UseCompatLoadingForDrawables")
class CandidateView(context: Context) : LinearLayout(context) {
    private var mService: BluesIME? = null
    private var mSuggestions: List<String>? = null

    /**
     * Construct a CandidateView for showing suggested words for completion.
     */
    init {
        View.inflate(context, R.layout.layout_candidate, this)

        updateView(
            !PREFS.getBoolean(
                CURRENT_PACKAGE_NAME,
                false   // default false : 기본 상태는 블랙리스트에 올라가있지 않은 상태
            )
        )

        // 일단은 prediction 하나 고정이므로 index 0으로 고정
        // first_prediction.setOnClickListener { v -> service?.pickSuggestion((v as TextView).text.toString()) }
        tv_prediction.setOnClickListener { mService?.pickSuggestionManually(0) }

        btn_lottie_animation.setOnClickListener {
            // 현재 앱이 추론 제외 대상으로 지정되어 있다면 해제, 지정되어 있지 않다면 추가
            if (PREFS.getBoolean(
                    CURRENT_PACKAGE_NAME,
                    false   // default false : 기본 상태는 블랙리스트에 올라가있지 않은 상태
                )
            ) {     // true, 즉 블랙리스트에 올라가있을 때
                PREFS.removeValue(CURRENT_PACKAGE_NAME)     // 블랙리스트에서 삭제
                PREDICTION = true  // 추론 기능 활성화
            } else {    // false, 즉 블랙리스트에 올라가있지 않을 때
                PREFS.setBoolean(CURRENT_PACKAGE_NAME, true)    // 항목 추가
                PREDICTION = false  // 추론 기능 비활성화
            }
            updateView(PREDICTION)
        }
    }

    /**
     * A connection back to the service to communicate with the text field
     * @param listener
     */
    fun setService(listener: BluesIME?) {
        mService = listener
    }

    fun updateView(isPredictionOn: Boolean) {
        if (isPredictionOn) {
            setSuggestions(emptyList(), completions = false, typedWordValid = false)
            btn_lottie_animation.addValueCallback(
                KeyPath("**"),
                LottieProperty.COLOR_FILTER,
                {
                    return@addValueCallback null
                }
            )
        } else {
            setSuggestions(
                listOf("추론 기능이 꺼져있습니다."),
                completions = false,
                typedWordValid = false
            )
            btn_lottie_animation.addValueCallback(
                KeyPath("**"),
                LottieProperty.COLOR_FILTER,
                {
                    return@addValueCallback PorterDuffColorFilter(
                        Color.GRAY,
                        PorterDuff.Mode.MULTIPLY
                    )
                }
            )
        }
    }

    fun startLottieAnimation() {
        btn_lottie_animation.playAnimation()
        btn_lottie_animation.loop(true)
    }

    fun stopLottieAnimation() {
        btn_lottie_animation.cancelAnimation()
    }

    fun setSuggestions(
        suggestions: List<String>,
        completions: Boolean,
        typedWordValid: Boolean
    ) {
        clear()
        updatePredictions(suggestions)
        invalidate()
        requestLayout()
    }

    private fun updatePredictions(prediction: List<String>) {
        tv_prediction.text = ""
        if (prediction.isNotEmpty()) {
            tv_prediction.text = prediction[0]
            tv_prediction.setBackgroundResource(R.drawable.bordered)
        } else {
            tv_prediction.text = ""
            tv_prediction.setBackgroundResource(0)
        }
    }

    fun clear() {
        mSuggestions = emptyList()
        invalidate()
    }
}
package com.highballuos.blues.inputmethod.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.inputmethodservice.Keyboard
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.highballuos.blues.R

class QwertyKeyboard : Keyboard {
    private var mEnterKey: Key? = null
    private var mSpaceKey: Key? = null

    /**
     * Stores the current state of the mode change key. Its width will be dynamically updated to
     * match the region of [.mModeChangeKey] when [.mModeChangeKey] becomes invisible.
     */
    private var mModeChangeKey: Key? = null

    /**
     * Stores the current state of the language switch key (a.k.a. globe key). This should be
     * visible while [InputMethodManager.shouldOfferSwitchingToNextInputMethod]
     * returns true. When this key becomes invisible, its width will be shrunk to zero.
     */
    private var mLanguageSwitchKey: Key? = null

    /**
     * Stores the size and other information of [.mModeChangeKey] when
     * [.mLanguageSwitchKey] is visible. This should be immutable and will be used only as a
     * reference size when the visibility of [.mLanguageSwitchKey] is changed.
     */
    private var mSavedModeChangeKey: Key? = null

    /**
     * Stores the size and other information of [.mLanguageSwitchKey] when it is visible.
     * This should be immutable and will be used only as a reference size when the visibility of
     * [.mLanguageSwitchKey] is changed.
     */
    private var mSavedLanguageSwitchKey: Key? = null

    constructor(
        context: Context, xmlLayoutId: Int
    ) : super(context, xmlLayoutId)

    constructor(
        context: Context, layoutTemplateResId: Int, characters: CharSequence,
        columns: Int, horizontalPadding: Int
    ) : super(context, layoutTemplateResId, characters, columns, horizontalPadding)

    override fun createKeyFromXml(
        res: Resources?, parent: Row?, x: Int, y: Int,
        parser: XmlResourceParser?
    ): Key {
        val key: Key = QwertyKey(res, parent, x, y, parser)
        if (key.codes[0] == KEYCODE_DONE) {
            mEnterKey = key
        } else if (key.codes[0] == ' '.toInt()) {
            mSpaceKey = key
        } else if (key.codes[0] == KEYCODE_MODE_CHANGE) {
            mModeChangeKey = key
            mSavedModeChangeKey = QwertyKey(res, parent, x, y, parser)
        } else if (key.codes[0] == QwertyKeyboardView.KEYCODE_LANGUAGE_SWITCH) {
            mLanguageSwitchKey = key
            mSavedLanguageSwitchKey = QwertyKey(res, parent, x, y, parser)
        }
        return key
    }

    /**
     * Dynamically change the visibility of the language switch key (a.k.a. globe key).
     * @param visible True if the language switch key should be visible.
     */
    fun setLanguageSwitchKeyVisibility(visible: Boolean) {
        if (visible) {
            // The language switch key should be visible. Restore the size of the mode change key
            // and language switch key using the saved layout.
            mModeChangeKey!!.width = mSavedModeChangeKey!!.width
            mModeChangeKey!!.x = mSavedModeChangeKey!!.x
            mLanguageSwitchKey!!.width = mSavedLanguageSwitchKey!!.width
            mLanguageSwitchKey!!.icon = mSavedLanguageSwitchKey!!.icon
            mLanguageSwitchKey!!.iconPreview = mSavedLanguageSwitchKey!!.iconPreview
        } else {
            // The language switch key should be hidden. Change the width of the mode change key
            // to fill the space of the language key so that the user will not see any strange gap.
            mModeChangeKey!!.width = mSavedModeChangeKey!!.width + mSavedLanguageSwitchKey!!.width
            mLanguageSwitchKey!!.width = 0
            mLanguageSwitchKey!!.icon = null
            mLanguageSwitchKey!!.iconPreview = null
        }
    }

    /**
     * [SoftKeyboard.onStartInput], 즉 입력을 시작하기 위해 에디터에 새로 포커싱 할 때마다 호출되는 메소드
     * 여기서 Enter 키의 코드를 수정하고 [SoftKeyboard.onKey] 에서 각 코드 별 작업 분배
     * xml 에서의 Enter 키 기본 코드는 -4 (KEYCODE_DONE)
     * This looks at the ime options given by the current editor, to set the
     * appropriate label on the keyboard's enter key (if it has one).
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    fun setImeOptions(res: Resources, options: Int) {
        if (mEnterKey == null) {
            return
        }
        when (options and (EditorInfo.IME_MASK_ACTION or EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            EditorInfo.IME_ACTION_GO -> {
                Log.v("EditorInfo Log", "EditorInfo.IME_ACTION_GO")
                mEnterKey!!.iconPreview = null
                mEnterKey!!.icon = null
                mEnterKey!!.label = res.getText(R.string.label_go_key)
                mEnterKey!!.codes[0] = QwertyKeyboardView.KEYCODE_ENTER_AS_GO
            }
            EditorInfo.IME_ACTION_NEXT -> {
                Log.v("EditorInfo Log", "EditorInfo.IME_ACTION_NEXT")
                mEnterKey!!.iconPreview = null
                mEnterKey!!.icon = null
                mEnterKey!!.label = res.getText(R.string.label_next_key)
                mEnterKey!!.codes[0] = QwertyKeyboardView.KEYCODE_ENTER_AS_NEXT
            }
            EditorInfo.IME_ACTION_SEARCH -> {
                Log.v("EditorInfo Log", "EditorInfo.IME_ACTION_SEARCH")
                mEnterKey!!.icon = res.getDrawable(R.drawable.ic_baseline_search_24)
                mEnterKey!!.label = null
                mEnterKey!!.codes[0] = QwertyKeyboardView.KEYCODE_ENTER_AS_SEARCH
            }
            EditorInfo.IME_ACTION_SEND -> {
                Log.v("EditorInfo Log", "EditorInfo.IME_ACTION_SEND")
                mEnterKey!!.iconPreview = null
                mEnterKey!!.icon = null
                mEnterKey!!.label = res.getText(R.string.label_send_key)
                mEnterKey!!.codes[0] = QwertyKeyboardView.KEYCODE_ENTER_AS_SEND
            }
            EditorInfo.IME_ACTION_DONE -> {
                Log.v("EditorInfo Log", "EditorInfo.IME_ACTION_DONE")
                mEnterKey!!.iconPreview = null
                mEnterKey!!.icon = null
                mEnterKey!!.label = res.getText(R.string.label_done_key)
                mEnterKey!!.codes[0] = KEYCODE_DONE
            }
            else -> {   // 일반적인 텍스트?
                Log.v("EditorInfo Log", "else")
                mEnterKey!!.icon = res.getDrawable(R.drawable.ic_baseline_keyboard_return_24)
                mEnterKey!!.label = null
                mEnterKey!!.codes[0] = 10   // Line Feed (개행)
            }
        }
    }

    fun setSpaceLabel(label: String) {
        mSpaceKey?.let {
            it.label = label
        }
    }

    fun setModeChangeLabel(label: String) {
        mModeChangeKey?.let {
            it.label = label
        }
    }

    internal class QwertyKey(
        res: Resources?, parent: Row?, x: Int, y: Int,
        parser: XmlResourceParser?
    ) :
        Key(res, parent, x, y, parser) {
        /**
         * Overriding this method so that we can reduce the target area for the key that
         * closes the keyboard.
         */
        override fun isInside(x: Int, y: Int): Boolean {
            return super.isInside(x, if (codes[0] == KEYCODE_CANCEL) y - 10 else y)
        }
    }
}
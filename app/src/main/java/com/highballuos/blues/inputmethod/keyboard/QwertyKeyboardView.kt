package com.highballuos.blues.inputmethod.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.view.inputmethod.InputMethodSubtype
import androidx.core.content.ContextCompat
import com.highballuos.blues.R
import com.highballuos.blues.inputmethod.service.utils.TypefaceUtils

class QwertyKeyboardView : KeyboardView {
    companion object {
        const val KEYCODE_OPTIONS = -100
        const val KEYCODE_LANGUAGE_SWITCH = -101
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun onLongPress(popupKey: Keyboard.Key?): Boolean {
        if (popupKey!!.codes[0] == Keyboard.KEYCODE_CANCEL) {
            onKeyboardActionListener.onKey(KEYCODE_OPTIONS, null)
            return true
        } else {
            return super.onLongPress(popupKey)
        }
    }

    fun setSubtypeOnSpaceKey(subtype: InputMethodSubtype) {
        val keyboard = keyboard as QwertyKeyboard?
        keyboard?.setSpaceLabel("한국어")
        invalidateAllKeys()
    }

    fun setLabelOnModeChangeKey(label: String) {
        val keyboard = keyboard as QwertyKeyboard?
        keyboard?.setModeChangeLabel(label)
        invalidateAllKeys()
    }

    private fun drawIcon(
        canvas: Canvas, icon: Drawable,
        x: Int, y: Int, width: Int, height: Int
    ) {
        canvas.translate(x.toFloat(), y.toFloat())
        icon.setBounds(0, 0, width, height)
        icon.draw(canvas)
        canvas.translate(-x.toFloat(), -y.toFloat())
    }

    // TODO 너무 많이 호출되는 메소드. 로직 최소화 필요. QwertyKeyboard.kt 에는 canvas 가 없는데...
    @SuppressLint("UseCompatLoadingForDrawables", "DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val keyboard = keyboard as QwertyKeyboard
        val keys = keyboard.keys
        for (key in keys) {
            when (key.codes[0]) {
                -1,     // Shift
                -5,     // Backspace
                10,     // Line Feed
                Keyboard.KEYCODE_DONE,     // Enter
                Keyboard.KEYCODE_MODE_CHANGE,
                KEYCODE_LANGUAGE_SWITCH -> {
                    var dr: Drawable? = null
                    dr = if (key.pressed) {
                        context.resources.getDrawable(R.drawable.pressed) as Drawable
                    } else {
                        context.resources.getDrawable(R.drawable.special) as Drawable
                    }
                    dr.setBounds(key.x, key.y, key.x + key.width, key.y + key.height)
                    canvas?.let { dr.draw(it) }

                    val paint = Paint()
                    paint.textAlign = Paint.Align.CENTER
                    paint.textSize = 44F
                    paint.color = ContextCompat.getColor(context, R.color.keyboard_key_color)

                    if (key.label != null) {
                        val labelCharHeight = TypefaceUtils.getReferenceCharHeight(paint)

                        canvas?.drawText(
                            key.label.toString(), (key.x + key.width / 2).toFloat(),
                            (key.y + (key.height / 2 + labelCharHeight / 2)), paint
                        )
                    } else if (key.icon != null) {
                        val iconWidth = key.icon.intrinsicWidth.coerceAtMost(key.width)
                        val iconHeight = key.icon.intrinsicHeight
                        val iconY =
                            key.y + (key.height - iconHeight) / 2    // Align vertically center.
                        val iconX =
                            key.x + (key.width - iconWidth) / 2  // Align horizontally center.
                        canvas?.let {
                            drawIcon(
                                canvas,
                                key.icon,
                                iconX,
                                iconY,
                                iconWidth,
                                iconHeight
                            )
                        }
                    }
                }
            }
        }
    }
}
package com.highballuos.blues.inputmethod.service.utils

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.SparseArray

/*
 * Copyright (C) 2013 The Android Open Source Project
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

object TypefaceUtils {
    private val KEY_LABEL_REFERENCE_CHAR = charArrayOf('M')
    private val KEY_NUMERIC_HINT_LABEL_REFERENCE_CHAR = charArrayOf('8')

    // This sparse array caches key label text height in pixel indexed by key label text size.
    private val sTextHeightCache = SparseArray<Float>()

    // Working variable for the following method.
    private val sTextHeightBounds = Rect()
    private fun getCharHeight(referenceChar: CharArray, paint: Paint): Float {
        val key = getCharGeometryCacheKey(referenceChar[0], paint)
        synchronized(sTextHeightCache) {
            val cachedValue = sTextHeightCache[key]
            if (cachedValue != null) {
                return cachedValue
            }
            paint.getTextBounds(referenceChar, 0, 1, sTextHeightBounds)
            val height = sTextHeightBounds.height().toFloat()
            sTextHeightCache.put(key, height)
            return height
        }
    }

    // This sparse array caches key label text width in pixel indexed by key label text size.
    private val sTextWidthCache = SparseArray<Float>()

    // Working variable for the following method.
    private val sTextWidthBounds = Rect()
    private fun getCharWidth(referenceChar: CharArray, paint: Paint): Float {
        val key = getCharGeometryCacheKey(referenceChar[0], paint)
        synchronized(sTextWidthCache) {
            val cachedValue = sTextWidthCache[key]
            if (cachedValue != null) {
                return cachedValue
            }
            paint.getTextBounds(referenceChar, 0, 1, sTextWidthBounds)
            val width = sTextWidthBounds.width().toFloat()
            sTextWidthCache.put(key, width)
            return width
        }
    }

    private fun getCharGeometryCacheKey(referenceChar: Char, paint: Paint): Int {
        val labelSize = paint.textSize.toInt()
        val face = paint.typeface
        val codePointOffset = referenceChar.toInt() shl 15
        return if (face === Typeface.DEFAULT) {
            codePointOffset + labelSize
        } else if (face === Typeface.DEFAULT_BOLD) {
            codePointOffset + labelSize + 0x1000
        } else if (face === Typeface.MONOSPACE) {
            codePointOffset + labelSize + 0x2000
        } else {
            codePointOffset + labelSize
        }
    }

    fun getReferenceCharHeight(paint: Paint): Float {
        return getCharHeight(KEY_LABEL_REFERENCE_CHAR, paint)
    }

    fun getReferenceCharWidth(paint: Paint): Float {
        return getCharWidth(KEY_LABEL_REFERENCE_CHAR, paint)
    }

    fun getReferenceDigitWidth(paint: Paint): Float {
        return getCharWidth(KEY_NUMERIC_HINT_LABEL_REFERENCE_CHAR, paint)
    }

    // Working variable for the following method.
    private val sStringWidthBounds = Rect()
    fun getStringWidth(string: String, paint: Paint): Float {
        synchronized(sStringWidthBounds) {
            paint.getTextBounds(string, 0, string.length, sStringWidthBounds)
            return sStringWidthBounds.width().toFloat()
        }
    }
}
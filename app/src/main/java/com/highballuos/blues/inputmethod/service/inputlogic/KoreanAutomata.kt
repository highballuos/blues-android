package com.highballuos.blues.inputmethod.service.inputlogic

class KoreanAutomata {
    private var mState = 0
    // TODO 수정 잦으므로 StringBuilder 고려
    private var mCompositionString = ""
    private var mCompleteString = ""
    private var mKoreanMode = false
    fun getState(): Int {
        return mState
    }

    fun getCompositionString(): String {
        return mCompositionString
    }

    fun getCompleteString(): String {
        return mCompleteString
    }

    fun toggleMode() {
        mKoreanMode = !mKoreanMode
    }

    fun isKoreanMode(): Boolean {
        return mKoreanMode
    }

    fun isHangul(code: Char): Boolean {
        if (code.toInt() in HANGUL_START..HANGUL_END) return true
        return code.toInt() in HANGUL_JAMO_START..HANGUL_JAMO_END
    }

    fun isJAMO(code: Char): Boolean {
        return code.toInt() in HANGUL_JAMO_START..HANGUL_JAMO_END
    }

    fun isConsonant(code: Char): Boolean {
        return code.toInt() in HANGUL_JAMO_START until HANGUL_MO_START
    }

    fun isVowel(code: Char): Boolean {
        return code.toInt() in HANGUL_MO_START..HANGUL_JAMO_END
    }

    /** not used.
     * public boolean IsLastConsonanted(char code)
     * {
     * if (IsHangul(code))
     * {
     * if (IsJAMO(code)) // <- need to fix, if this routine is to be used...
     * return true;
     * int offset = code - HANGUL_START;
     * if (offset % InputTables.NUM_OF_LAST_INDEX == 0)
     * return false;
     * else
     * return true;
     * }
     * else
     * {
     * // wrong input
     * return false;
     * }
     * }
     */

    fun getLastConsonantIndex(code: Char): Int {
        var lcIndex = -1
        if (isHangul(code)) {
            if (isJAMO(code)) {
                if (isConsonant(code)) {
                    lcIndex = 0
                    while (lcIndex < InputTables.NUM_OF_LAST_INDEX) {
                        if (code == InputTables.LastConsonants.Code[lcIndex]) break
                        lcIndex++
                    }
                    if (lcIndex >= InputTables.NUM_OF_LAST_INDEX) lcIndex = -1
                } else lcIndex = -1
            } else {
                val offset = code.toInt() - HANGUL_START
                lcIndex = offset % InputTables.NUM_OF_LAST_INDEX
            }
        }
        return lcIndex
    }

    fun getLastConsonant(code: Char): Char {
        val lcCode: Char
        val lcIndex = getLastConsonantIndex(code)
        lcCode = if (lcIndex < 0) 0.toChar() else InputTables.LastConsonants.Code[lcIndex]
        return lcCode
    }

    fun getFirstConsonantIndex(code: Char): Int {
        var fcIndex = -1
        if (isHangul(code)) {
            if (isConsonant(code)) {
                fcIndex = 0
                while (fcIndex < InputTables.NUM_OF_FIRST) {
                    if (code == InputTables.FirstConsonantCodes[fcIndex]) break
                    fcIndex++
                }
                if (fcIndex >= InputTables.NUM_OF_FIRST) fcIndex = -1
            } else if (isVowel(code)) {
                fcIndex = -1
            } else {
                val offset = code.toInt() - HANGUL_START
                fcIndex = offset / (InputTables.NUM_OF_MIDDLE * InputTables.NUM_OF_LAST_INDEX)
            }
        }
        return fcIndex
    }

    fun getFirstConsonant(code: Char): Char {
        val fcCode: Char
        val fcIndex = getFirstConsonantIndex(code)
        fcCode = if (fcIndex < 0) 0.toChar() else InputTables.FirstConsonantCodes[fcIndex]
        return fcCode
    }

    fun getVowelIndex(code: Char): Int {
        var vIndex = -1
        if (isHangul(code)) {
            vIndex = if (isVowel(code)) // vowel only character..
            {
                convertVowelCodeToIndex(code)
            } else {
                val offset = code.toInt() - HANGUL_START
                offset % (InputTables.NUM_OF_MIDDLE * InputTables.NUM_OF_LAST_INDEX) / InputTables.NUM_OF_LAST_INDEX
            }
        }
        return vIndex
    }

    fun getVowel(code: Char): Char {
        val vCode: Char
        val vIndex = getVowelIndex(code)
        vCode = if (vIndex < 0) 0.toChar() else InputTables.Vowels.Code[vIndex]
        return vCode
    }

    fun convertFirstConsonantCodeToIndex(fcCode: Char): Int // fcCode should be one of "First Consonants" otherwise return -1
    {
        var fcIndex = 0
        while (fcIndex < InputTables.NUM_OF_FIRST) {
            if (fcCode == InputTables.FirstConsonantCodes[fcIndex]) break
            fcIndex++
        }
        if (fcIndex == InputTables.NUM_OF_FIRST) fcIndex = -1
        return fcIndex
    }

    fun convertLastConsonantCodeToIndex(lcCode: Char): Int // fcCode should be one of "Last Consonants", otherwise return -1
    {
        var lcIndex = 0
        while (lcIndex < InputTables.NUM_OF_LAST_INDEX) {
            if (lcCode == InputTables.LastConsonants.Code[lcIndex]) break
            lcIndex++
        }
        if (lcIndex == InputTables.NUM_OF_LAST_INDEX) lcIndex = -1
        return lcIndex
    }

    fun convertVowelCodeToIndex(vCode: Char): Int {
        if (vCode < InputTables.Vowels.Code[0]) return -1
        val vIndex: Int = vCode - InputTables.Vowels.Code[0]
        return if (vIndex >= InputTables.NUM_OF_MIDDLE) -1 else vIndex
    }

    fun combineLastConsonantWithIndex(cIndex1: Int, cIndex2: Int): Int {
        var newIndex = 0
        var newCode = 0.toChar()
        if (InputTables.LastConsonants.Code[cIndex1].toInt() == 0x3131 && InputTables.LastConsonants.Code[cIndex2].toInt() == 0x3145
        ) newCode = 0x3133.toChar() // ã„³
        if (InputTables.LastConsonants.Code[cIndex1].toInt() == 0x3142 && InputTables.LastConsonants.Code[cIndex2].toInt() == 0x3145
        ) newCode = 0x3144.toChar() // ã…„
        if (InputTables.LastConsonants.Code[cIndex1].toInt() == 0x3134) {
            if (InputTables.LastConsonants.Code[cIndex2].toInt() == 0x3148) newCode =
                0x3135.toChar() // ã„µ
            else if (InputTables.LastConsonants.Code[cIndex2].toInt() == 0x314E) newCode =
                0x3136.toChar() // ã„¶
        }
        if (InputTables.LastConsonants.Code[cIndex1].toInt() == 0x3139) {
            if (InputTables.LastConsonants.Code[cIndex2].toInt() == 0x3131) newCode =
                0x313A.toChar() // ã„º
            else if (InputTables.LastConsonants.Code[cIndex2].toInt() == 0x3141) newCode =
                0x313B.toChar() // ã„»
            else if (InputTables.LastConsonants.Code[cIndex2].toInt() == 0x3142) newCode =
                0x313C.toChar() // ã„¼
            else if (InputTables.LastConsonants.Code[cIndex2].toInt() == 0x3145) newCode =
                0x313D.toChar() // ã„½
            else if (InputTables.LastConsonants.Code[cIndex2].toInt() == 0x314C) newCode =
                0x313E.toChar() // ã„¾
            else if (InputTables.LastConsonants.Code[cIndex2].toInt() == 0x314D) newCode =
                0x313F.toChar() // ã„¿
            else if (InputTables.LastConsonants.Code[cIndex2].toInt() == 0x314E) newCode =
                0x3140.toChar() // ã…€
        }
        newIndex = if (newCode == 0.toChar()) -1 else convertLastConsonantCodeToIndex(newCode)
        return newIndex
    }

    fun combineLastConsonantWithCode(lcCode1: Char, lcCode2: Char): Char {
        var newCode = 0.toChar()
        if (lcCode1.toInt() == 0x3131 && lcCode2.toInt() == 0x3145) newCode = 0x3133.toChar() // ã„³
        else if (lcCode1.toInt() == 0x3142 && lcCode2.toInt() == 0x3145) newCode =
            0x3144.toChar() // ã…„
        else if (lcCode1.toInt() == 0x3134) {
            if (lcCode2.toInt() == 0x3148) newCode = 0x3135.toChar() // ã„µ
            else if (lcCode2.toInt() == 0x314E) newCode = 0x3136.toChar() // ã„¶
        } else if (lcCode1.toInt() == 0x3139) {
            if (lcCode2.toInt() == 0x3131) newCode = 0x313A.toChar() // ã„º
            else if (lcCode2.toInt() == 0x3141) newCode = 0x313B.toChar() // ã„»
            else if (lcCode2.toInt() == 0x3142) newCode = 0x313C.toChar() // ã„¼
            else if (lcCode2.toInt() == 0x3145) newCode = 0x313D.toChar() // ã„½
            else if (lcCode2.toInt() == 0x314C) newCode = 0x313E.toChar() // ã„¾
            else if (lcCode2.toInt() == 0x314D) newCode = 0x313F.toChar() // ã„¿
            else if (lcCode2.toInt() == 0x314E) newCode = 0x3140.toChar() // ã…€
        }
        return newCode
    }

    fun combineVowelWithCode(vCode1: Char, vCode2: Char): Char {
        var newCode = 0.toChar()
        if (vCode1.toInt() == 0x3157) // ã…—
        {
            if (vCode2.toInt() == 0x314F) // ã…�
                newCode = 0x3158.toChar() // ã…˜
            else if (vCode2.toInt() == 0x3150) // ã…�
                newCode = 0x3159.toChar() // ã…™
            else if (vCode2.toInt() == 0x3163) // ã…£
                newCode = 0x315A.toChar() // ã…š
        } else if (vCode1.toInt() == 0x315C) // ã…œ
        {
            if (vCode2.toInt() == 0x3153) // ã…“
                newCode = 0x315D.toChar() // ã…�
            else if (vCode2.toInt() == 0x3154) // ã…”
                newCode = 0x315E.toChar() // ã…ž
            else if (vCode2.toInt() == 0x3163) // ã…£
                newCode = 0x315F.toChar() // ã…Ÿ
        } else if (vCode1.toInt() == 0x3161) // ã…¡
        {
            if (vCode2.toInt() == 0x3163) // ã…£
                newCode = 0x3162.toChar() // ã…¢
        }
        return newCode
    }

    fun combineVowelWithIndex(vIndex1: Int, vIndex2: Int): Int {
        var newIndex = -1
        val vCode1: Char = InputTables.Vowels.Code[vIndex1]
        val vCode2: Char = InputTables.Vowels.Code[vIndex2]
        val newCode = combineVowelWithCode(vCode1, vCode2)
        if (newCode != 0.toChar()) {
            newIndex = convertVowelCodeToIndex(newCode)
        }
        return newIndex
    }

    fun composeCharWithIndexs(fcIndex: Int, vIndex: Int, lcIndex: Int): Char {
        var Code = 0.toChar()
        if (fcIndex >= 0 && fcIndex < InputTables.NUM_OF_FIRST) {
            if (vIndex >= 0 && vIndex < InputTables.NUM_OF_MIDDLE) {
                if (lcIndex >= 0 && lcIndex < InputTables.NUM_OF_LAST) {
                    val offset: Int =
                        fcIndex * InputTables.NUM_OF_MIDDLE * InputTables.NUM_OF_LAST_INDEX + vIndex * InputTables.NUM_OF_LAST_INDEX + lcIndex
                    Code = (offset + HANGUL_START).toChar()
                }
            }
        }
        return Code
    }

    fun getAlphabetIndex(code: Char): Int {
        if (code in 'a'..'z') return (code - 'a')
        return if (code in 'A'..'Z') (code - 'A') else -1
    }

    fun isNumber(code: Char): Boolean {
        return (code in '0'..'9')
    }

    fun isDot(code: Char): Boolean {
        return (code == '.' || code == ',')
    }

    fun isSpacer(code: Char): Boolean {
        return code.toInt() <= 0x0020 &&
                1L shl 0x0009 or
                (1L shl 0x000A) or
                (1L shl 0x000C) or
                (1L shl 0x000D) or
                (1L shl 0x0020) shr code.toInt() and 1L != 0L
    }

    /* not used...
	public boolean IsToggleKey(char code, int KeyState)
	{
		boolean bRet = false;
		if ((code == ' ') && ((KeyState & InputTables.KEYSTATE_SHIFT_MASK) != 0)) // SHIFT-SPACE
			bRet = true;
		return bRet;
	}
	*/
    fun doBackSpace(): Int {
        var ret = ACTION_NONE
        var code: Char
        code = if (mCompositionString !== "") mCompositionString[0] else 0.toChar()
        if (mState != 0 && code == 0.toChar()) {
            // Log.v(TAG, "DoBackSpace -- Error. CompositionString is NULL. mState = " + mState);
            return ACTION_ERROR
        }
        when (mState) {
            0 -> ret = ACTION_USE_INPUT_AS_RESULT
            1, 4 -> {
                mCompositionString = ""
                mState = 0
                // ret = ACTION_UPDATE_COMPOSITIONSTR;
                ret = ACTION_USE_INPUT_AS_RESULT
            }
            2 -> {
                run {
                    val fcIndex = getFirstConsonantIndex(code)
                    code = InputTables.FirstConsonantCodes[fcIndex]
                    mCompositionString = ""
                    mCompositionString += code
                    mState = 1
                }
                ret = ACTION_UPDATE_COMPOSITIONSTR
            }
            3 -> {
                run {
                    val lcIndex = getLastConsonantIndex(code)
                    code = (code.toInt() - lcIndex).toChar()
                    mCompositionString = ""
                    mCompositionString += code
                    mState = 2
                }
                ret = ACTION_UPDATE_COMPOSITIONSTR
            }
            5 -> {
                run {
                    val vIndex = getVowelIndex(code)
                    if (vIndex < 0) {
                        ret = ACTION_ERROR
                    }
                    val newIndex: Int = InputTables.Vowels.iMiddle[vIndex]
                    if (newIndex < 0) {
                        ret = ACTION_ERROR
                    }
                    code = InputTables.Vowels.Code[newIndex]
                    mCompositionString = ""
                    mCompositionString += code
                    mState = 4
                }
                ret = ACTION_UPDATE_COMPOSITIONSTR
            }
            10 -> {
                run {
                    val lcIndex = getLastConsonantIndex(code)
                    if (lcIndex < 0) {
                        ret = ACTION_ERROR
                    }
                    val newIndex: Int = InputTables.LastConsonants.iLast[lcIndex]
                    if (newIndex < 0) {
                        ret = ACTION_ERROR
                    }
                    code = InputTables.LastConsonants.Code[newIndex]
                    mCompositionString = ""
                    mCompositionString += code
                    mState = 1
                }
                ret = ACTION_UPDATE_COMPOSITIONSTR
            }
            11 -> {
                run {
                    val lcIndex = getLastConsonantIndex(code)
                    if (lcIndex < 0) {
                        ret = ACTION_ERROR
                    }
                    val newIndex: Int = InputTables.LastConsonants.iLast[lcIndex]
                    if (newIndex < 0) {
                        ret = ACTION_ERROR
                    }
                    code = (code.toInt() - lcIndex + newIndex).toChar()
                    mCompositionString = ""
                    mCompositionString += code
                    mState = 3
                }
                ret = ACTION_UPDATE_COMPOSITIONSTR
            }
            20 -> {
                run {
                    val fcIndex = getFirstConsonantIndex(code)
                    val vIndex = getVowelIndex(code)
                    val newIndex: Int = InputTables.Vowels.iMiddle[vIndex]
                    if (newIndex < 0) {
                        ret = ACTION_ERROR
                    }
                    code = composeCharWithIndexs(fcIndex, newIndex, 0)
                    mCompositionString = ""
                    mCompositionString += code
                    mState = 2
                }
                ret = ACTION_UPDATE_COMPOSITIONSTR
            }
            21 -> {
                run {
                    val lcIndex = getLastConsonantIndex(code)
                    code = (code.toInt() - lcIndex).toChar()
                    mCompositionString = ""
                    mCompositionString += code
                    mState = 20
                }
                ret = ACTION_UPDATE_COMPOSITIONSTR
            }
            22 -> {
                run {
                    val lcIndex = getLastConsonantIndex(code)
                    if (lcIndex < 0) {
                        ret = ACTION_ERROR
                    }
                    val newIndex: Int = InputTables.LastConsonants.iLast[lcIndex]
                    if (newIndex < 0) {
                        ret = ACTION_ERROR
                    }
                    code = (code.toInt() - lcIndex + newIndex).toChar()
                    mCompositionString = ""
                    mCompositionString += code
                    mState = 21
                }
                ret = ACTION_UPDATE_COMPOSITIONSTR
            }
            else -> ret = ACTION_ERROR // error. should not be here in any circumstance.
        }
        return ret
    }

    fun finishAutomataWithoutInput(): Int // Input is ended by external causes
    {
        val ret = ACTION_NONE
        // 원래는 한국어 키보드 상태에서만 초기화 가능했는데 한/영 키보드 전환 사이에 버그가 너무 많음
        // 현재 키보드에 관계없이 조합, 완성된 문자열 그리고 state 초기화
        // if (mKoreanMode) //  && mState > 0)
        // {
            mCompleteString = ""
            mCompositionString = ""
            mState = 0
            //ret |= ACTION_UPDATE_COMPOSITIONSTR;
            //ret |= ACTION_UPDATE_COMPLETESTR;
        // }
        return ret
    }

    fun doAutomata(code: Char, KeyState: Int): Int // , String CurrentCompositionString)
    {
        // Log.v(TAG, "DoAutomata Entered - code = "+ code + " KeyState = " + KeyState + " mState = " + mState);
        var result = ACTION_NONE
        val alphaIndex = getAlphabetIndex(code)
        val isNumber = isNumber(code)
        val isDot = isDot(code)
        val isSpacer = isSpacer(code)
        val hcode: Char

        /* remove toggle key check and backspace check.
		// check toggle key first
		if (IsToggleKey(code, KeyState)) // SHIFT-SPACE
		{
			// toggle Korean/English
			if (mState != 0) // flushing..
			{
				mCompleteString = mCompositionString;
				mCompositionString = "";
				mState = 0;
				result = ACTION_UPDATE_COMPLETESTR | ACTION_UPDATE_COMPOSITIONSTR;
			}
			mKoreanMode = !mKoreanMode; // input mode toggle
		}
		else if (code == InputTables.BACK_SPACE)
		{
			// do back space
		}
		else */
        if (alphaIndex < 0) // 알파벳이 아니면
        {
            if (mKoreanMode) {
                // flush Korean characters first.
                mCompleteString = mCompositionString
                mCompositionString = ""
                mState = 0
                result = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
            }
            // 숫자거나 반점 온점이거나 스페이스 같은 공간이거나
            // 이 외의 경우는 한국어 키보드가 아닐 때, 즉 mKoreanMode false 이므로 상관없음
            // process the code as English
            if (isNumber || isDot || isSpacer) {
                result = ACTION_USE_INPUT_AS_RESULT
            }
            // process the code as English
            if (KeyState and (InputTables.KEYSTATE_ALT_MASK or InputTables.KEYSTATE_CTRL_MASK or InputTables.KEYSTATE_FN) == 0) {
                result = result or ACTION_USE_INPUT_AS_RESULT
            }
        } else if (!mKoreanMode) {
            // process the code as English
            result = ACTION_USE_INPUT_AS_RESULT
        } else {
            hcode = if (KeyState and InputTables.KEYSTATE_SHIFT_MASK == 0) {
                InputTables.NormalKeyMap.Code[alphaIndex]
            } else {
                InputTables.ShiftedKeyMap.Code[alphaIndex]
            }
            result = when (mState) {
                0 -> doState00(hcode)
                1 -> doState01(hcode)
                2 -> doState02(hcode)
                3 -> doState03(hcode)
                4 -> doState04(hcode)
                5 -> doState05(hcode)
                10 -> doState10(hcode)
                11 -> doState11(hcode)
                20 -> doState20(hcode)
                21 -> doState21(hcode)
                22 -> doState22(hcode)
                else -> ACTION_ERROR // error. should not be here in any circumstance.
            }
        }
        return result
    }

    private fun doState00(code: Char): Int // current composition string: NULL
    {
        // Log.v(TAG, "State 0 Entered - code = "+ code );
        mState = if (isConsonant(code)) {
            1
        } else {
            4
        }
        mCompleteString = ""
        mCompositionString = ""
        mCompositionString += code
        return ACTION_UPDATE_COMPOSITIONSTR or ACTION_APPEND
    }

    private fun doState01(code: Char): Int // current composition string: single consonant only
    {
        // Log.v(TAG, "State 1 Entered - code = "+ code );
        if (mCompositionString === "") {
            // Log.v(TAG, "DoState 01 -- Error. CompositionString is NULL");
            return ACTION_ERROR
        }
        var ret = ACTION_NONE
        if (isConsonant(code)) {
            val newCode = combineLastConsonantWithCode(mCompositionString[0], code)
            if (newCode == 0.toChar()) // cannot combine last consonants
            {
                mCompleteString = mCompositionString // flush
                mCompositionString = ""
                mCompositionString += code
                mState = 1
                ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
            } else  // can combine last consonants
            {
                mCompleteString = ""
                mCompositionString = ""
                mCompositionString += newCode
                mState = 10
                ret = ACTION_UPDATE_COMPOSITIONSTR
            }
        } else {
            val fcIndex = convertFirstConsonantCodeToIndex(mCompositionString[0])
            val vIndex = convertVowelCodeToIndex(code)
            val newCode = composeCharWithIndexs(fcIndex, vIndex, 0)
            mCompleteString = ""
            mCompositionString = ""
            mCompositionString += newCode
            mState = 2
            ret = ACTION_UPDATE_COMPOSITIONSTR
        }
        return ret
    }

    private fun doState02(code: Char): Int // current composition string: single consonant + single vowel
    {
        // Log.v(TAG, "State 2 Entered - code = "+ code );
        if (mCompositionString === "") {
            // Log.v(TAG, "DoState-02 -- Error. CompositionString is NULL");
            return ACTION_ERROR
        }
        var ret = ACTION_NONE
        if (isConsonant(code)) {
            val lcIndex = getLastConsonantIndex(code)
            if (lcIndex != -1) // code can be last consonant..
            {
                val newCode = (mCompositionString[0].toInt() + lcIndex).toChar()
                mCompleteString = ""
                mCompositionString = ""
                mCompositionString += newCode
                mState = 3
                ret = ACTION_UPDATE_COMPOSITIONSTR
            } else {
                mCompleteString = mCompositionString
                mCompositionString = "" // flush
                mCompositionString += code
                mState = 1
                ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
            }
        } else  // vowel
        {
            val vCode = getVowel(mCompositionString[0])
            val newCode = combineVowelWithCode(vCode, code)
            if (newCode != 0.toChar()) {
                val fcIndex = getFirstConsonantIndex(mCompositionString[0])
                val vIndex = convertVowelCodeToIndex(newCode)
                val newChar = composeCharWithIndexs(fcIndex, vIndex, 0)
                mCompleteString = ""
                mCompositionString = ""
                mCompositionString += newChar
                mState = 20
                ret = ACTION_UPDATE_COMPOSITIONSTR
            } else {
                mCompleteString = mCompositionString
                mCompositionString = ""
                mCompositionString += code
                mState = 4
                ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
            }
        }
        return ret
    }

    private fun doState03(code: Char): Int // current composition string: single consonant + single vowel + single consonant
    {
        // Log.v(TAG, "State 3 Entered - code = "+ code );
        if (mCompositionString === "") {
            // Log.v(TAG, "DoState 03 -- Error. CompositionString is NULL");
            return ACTION_ERROR
        }
        var ret = ACTION_NONE
        if (isConsonant(code)) {
            val lcIndex = getLastConsonantIndex(mCompositionString[0])
            if (lcIndex < 0) {
                // Log.v(TAG, " -- Error. consonant, lcIndex = " + lcIndex);
                return ACTION_ERROR
            }
            val newCode =
                combineLastConsonantWithCode(InputTables.LastConsonants.Code[lcIndex], code)
            if (newCode != 0.toChar()) // Last Consonants can be combined
            {
                val newChar =
                    (mCompositionString[0].toInt() - lcIndex + getLastConsonantIndex(newCode)).toChar()
                mCompleteString = ""
                mCompositionString = ""
                mCompositionString += newChar
                mState = 11
                ret = ACTION_UPDATE_COMPOSITIONSTR
            } else {
                mCompleteString = mCompositionString
                mCompositionString = ""
                mCompositionString += code
                mState = 1
                ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
            }
        } else  // vowel
        {
            val lcIndex = getLastConsonantIndex(mCompositionString[0])
            if (lcIndex < 0) {
                // Log.v(TAG, " -- complete Error. vowel, lcIndex = " + lcIndex);
                return ACTION_ERROR
            }
            val newChar =
                (mCompositionString[0].toInt() - lcIndex).toChar() // remove last consonant and flush it.
            mCompleteString = ""
            mCompleteString += newChar
            val fcIndex = getFirstConsonantIndex(InputTables.LastConsonants.Code[lcIndex])
            if (fcIndex < 0) {
                // Log.v(TAG, " -- composition Error, vowel, lcIndex = " + lcIndex);
                return ACTION_ERROR
            }
            val vIndex = getVowelIndex(code)
            val newCode =
                composeCharWithIndexs(fcIndex, vIndex, 0) // compose new composition string
            mCompositionString = ""
            mCompositionString += newCode
            mState = 2
            ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
        }
        return ret
    }

    private fun doState04(code: Char): Int // current composition string: single vowel
    {
        // Log.v(TAG, "State 4 Entered - code = "+ code );
        if (mCompositionString === "") {
            // Log.v(TAG, "DoState 04 -- Error. CompositionString is NULL");
            return ACTION_ERROR
        }
        var ret = ACTION_NONE
        if (isConsonant(code)) {
            mCompleteString = mCompositionString
            mCompositionString = ""
            mCompositionString += code
            mState = 1
            ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
        } else {
            val newCode = combineVowelWithCode(mCompositionString[0], code)
            if (newCode != 0.toChar()) {
                mCompleteString = ""
                mCompositionString = ""
                mCompositionString += newCode
                mState = 5
                ret = ACTION_UPDATE_COMPOSITIONSTR
            } else {
                mCompleteString = mCompositionString
                mCompositionString = ""
                mCompositionString += code
                mState = 4
                ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
            }
        }
        return ret
    }

    private fun doState05(code: Char): Int // current composition string: a combined vowel
    {
        // Log.v(TAG, "State 5 Entered - code = "+ code );
        if (mCompositionString === "") {
            // Log.v(TAG, "DoState 05 -- Error. CompositionString is NULL");
            return ACTION_ERROR
        }
        var ret = ACTION_NONE
        if (isConsonant(code)) {
            mCompleteString = mCompositionString
            mCompositionString = ""
            mCompositionString += code
            mState = 1
            ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
        } else {
            mCompleteString = mCompositionString
            mCompositionString = ""
            mCompositionString += code
            mState = 4
            ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
        }
        return ret
    }

    private fun doState10(code: Char): Int // current composition string: a combined consonant
    {
        // Log.v(TAG, "State 10 Entered - code = "+ code );
        if (mCompositionString === "") {
            // Log.v(TAG, "DoState 10 -- Error. CompositionString is NULL");
            return ACTION_ERROR
        }
        var ret = ACTION_NONE
        if (isConsonant(code)) {
            mCompleteString = mCompositionString
            mCompositionString = ""
            mCompositionString += code
            mState = 1
            ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
        } else {
            val lcIndex0 = getLastConsonantIndex(mCompositionString[0])
            val lcIndex1: Int = InputTables.LastConsonants.iLast[lcIndex0]
            val fcIndex: Int = InputTables.LastConsonants.iFirst[lcIndex0]
            mCompleteString = ""
            mCompleteString += InputTables.LastConsonants.Code[lcIndex1]
            val vIndex = getVowelIndex(code)
            val newChar = composeCharWithIndexs(fcIndex, vIndex, 0)
            mCompositionString = ""
            mCompositionString += newChar
            mState = 2
            ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
        }
        return ret
    }

    private fun doState11(code: Char): Int // current composition string: single consonant + single vowel + a combined consonant
    {
        // Log.v(TAG, "State 11 Entered - code = "+ code );
        if (mCompositionString === "") {
            // Log.v(TAG, "DoState 11 -- Error. CompositionString is NULL");
            return ACTION_ERROR
        }
        var ret = ACTION_NONE
        if (isConsonant(code)) {
            mCompleteString = mCompositionString
            mCompositionString = ""
            mCompositionString += code
            mState = 1
            ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
        } else {
            val lcIndexOrg = getLastConsonantIndex(mCompositionString[0])
            val fcIndexOrg = getFirstConsonantIndex(mCompositionString[0])
            val vIndexOrg = getVowelIndex(mCompositionString[0])
            val lcIndexNew: Int = InputTables.LastConsonants.iLast[lcIndexOrg]
            var newChar = composeCharWithIndexs(fcIndexOrg, vIndexOrg, lcIndexNew)
            val fcIndexNew: Int = InputTables.LastConsonants.iFirst[lcIndexOrg]
            val vIndexNew = convertVowelCodeToIndex(code)
            mCompleteString = ""
            mCompleteString += newChar
            newChar = composeCharWithIndexs(fcIndexNew, vIndexNew, 0)
            mCompositionString = ""
            mCompositionString += newChar
            mState = 2
            ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
        }
        return ret
    }

    private fun doState20(code: Char): Int // current composition string: single consonant + a combined vowel
    {
        // Log.v(TAG, "State 20 Entered - code = "+ code );
        if (mCompositionString === "") {
            // Log.v(TAG, "DoState 20 -- Error. CompositionString is NULL");
            return ACTION_ERROR
        }
        var ret = ACTION_NONE
        if (isConsonant(code)) {
            val lcIndex = convertLastConsonantCodeToIndex(code)
            if (lcIndex < 0) // cannot compose the code with composition string. flush it.
            {
                mCompleteString = mCompositionString
                mCompositionString = ""
                mCompositionString += code
                mState = 1
                ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
            } else  // compose..
            {
                var newChar = mCompositionString[0]
                newChar = (newChar.toInt() + lcIndex).toChar()
                mCompleteString = ""
                mCompositionString = ""
                mCompositionString += newChar
                mState = 21
                ret = ACTION_UPDATE_COMPOSITIONSTR
            }
        } else {
            mCompleteString = mCompositionString
            mCompositionString = ""
            mCompositionString += code
            mState = 4
            ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
        }
        return ret
    }

    private fun doState21(code: Char): Int // current composition string: single consonant + a combined vowel + single consonant
    {
        // Log.v(TAG, "State 21 Entered - code = "+ code );
        if (mCompositionString === "") {
            // Log.v(TAG, "DoState 20 -- Error. CompositionString is NULL");
            return ACTION_ERROR
        }
        var ret = ACTION_NONE
        if (isConsonant(code)) {
            val lcIndex = getLastConsonantIndex(mCompositionString[0])
            val lcIndexTemp = convertLastConsonantCodeToIndex(code)
            if (lcIndexTemp < 0) {
                mCompleteString = mCompositionString
                mCompositionString = ""
                mCompositionString += code
                mState = 1
                ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
            } else {
                val lcIndexNew = combineLastConsonantWithIndex(lcIndex, lcIndexTemp)
                if (lcIndexNew < 0) {
                    mCompleteString = mCompositionString
                    mCompositionString = ""
                    mCompositionString += code
                    mState = 1
                    ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
                } else {
                    var newChar = mCompositionString[0]
                    newChar = (newChar.toInt() - lcIndex + lcIndexNew).toChar()
                    mCompleteString = ""
                    mCompositionString = ""
                    mCompositionString += newChar
                    mState = 22
                    ret = ACTION_UPDATE_COMPOSITIONSTR
                }
            }
        } else {
            var newChar = mCompositionString[0]
            val lcIndex = getLastConsonantIndex(newChar)
            newChar = (newChar.toInt() - lcIndex).toChar()
            mCompleteString = ""
            mCompleteString += newChar
            val fcIndex =
                convertFirstConsonantCodeToIndex(InputTables.LastConsonants.Code[lcIndex])
            val vIndex = convertVowelCodeToIndex(code)
            newChar = composeCharWithIndexs(fcIndex, vIndex, 0)
            mCompositionString = ""
            mCompositionString += newChar
            mState = 2
            ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
        }
        return ret
    }

    private fun doState22(code: Char): Int // current composition string: single consonant + a combined vowel + a combined consonant
    {
        // Log.v(TAG, "State 22 Entered - code = "+ code );
        if (mCompositionString === "") {
            // Log.v(TAG, "DoState 22 -- Error. CompositionString is NULL");
            return ACTION_ERROR
        }
        var ret = ACTION_NONE
        if (isConsonant(code)) {
            mCompleteString = mCompositionString
            mCompositionString = ""
            mCompositionString += code
            mState = 1
            ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
        } else {
            var tempChar = mCompositionString[0]
            val lcIndex0 = getLastConsonantIndex(tempChar)
            val lcIndex1: Int = InputTables.LastConsonants.iLast[lcIndex0]
            val fcIndex: Int = InputTables.LastConsonants.iFirst[lcIndex0]
            tempChar = (tempChar.toInt() - lcIndex0 + lcIndex1).toChar()
            mCompleteString = ""
            mCompleteString += tempChar
            val vIndex = getVowelIndex(code)
            val newChar = composeCharWithIndexs(fcIndex, vIndex, 0)
            mCompositionString = ""
            mCompositionString += newChar
            mState = 2
            ret = ACTION_UPDATE_COMPLETESTR or ACTION_UPDATE_COMPOSITIONSTR
        }
        return ret
    }

    companion object {
        var HANGUL_START = 0xAC00
        var HANGUL_END = 0xD7A3
        var HANGUL_JAMO_START = 0x3131
        var HANGUL_MO_START = 0x314F
        var HANGUL_JAMO_END = 0x3163
        private const val TAG = "KoreanAutomata"

        // Action Codes
        const val ACTION_NONE = 0
        const val ACTION_UPDATE_COMPOSITIONSTR = 1
        const val ACTION_UPDATE_COMPLETESTR = 2
        const val ACTION_USE_INPUT_AS_RESULT = 4
        const val ACTION_APPEND = 8

        // public static final int ACTION_BACKSPACE = 8; not used.
        const val ACTION_ERROR = -1
    }

    init {
        mState = 0
        mCompositionString = ""
        mCompleteString = ""
        mKoreanMode = false
    }
}
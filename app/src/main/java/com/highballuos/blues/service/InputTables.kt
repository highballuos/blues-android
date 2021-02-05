package com.highballuos.blues.service

object InputTables {
    const val NUM_OF_FIRST = 19
    const val NUM_OF_MIDDLE = 21
    const val NUM_OF_LAST = 27
    const val NUM_OF_LAST_INDEX = NUM_OF_LAST + 1 // add 1 for non-last consonant added characters
    const val KEYSTATE_NONE = 0
    const val KEYSTATE_SHIFT = 1
    const val KEYSTATE_SHIFT_LEFT = 1
    const val KEYSTATE_SHIFT_RIGHT = 2
    const val KEYSTATE_SHIFT_MASK = 3
    const val KEYSTATE_ALT = 4
    const val KEYSTATE_ALT_LEFT = 4
    const val KEYSTATE_ALT_RIGHT = 8
    const val KEYSTATE_ALT_MASK = 12
    const val KEYSTATE_CTRL = 16
    const val KEYSTATE_CTRL_LEFT = 16
    const val KEYSTATE_CTRL_RIGHT = 32
    const val KEYSTATE_CTRL_MASK = 48
    const val KEYSTATE_FN = 64 // just for future usage...
    const val BACK_SPACE = 0x8.toChar()
    val FirstConsonantCodes = charArrayOf(
        0x3131.toChar(),
        0x3132.toChar(),
        0x3134.toChar(),
        0x3137.toChar(),
        0x3138.toChar(),
        0x3139.toChar(),
        0x3141.toChar(),
        0x3142.toChar(),
        0x3143.toChar(),
        0x3145.toChar(),
        0x3146.toChar(),
        0x3147.toChar(),
        0x3148.toChar(),
        0x3149.toChar(),
        0x314A.toChar(),
        0x314B.toChar(),
        0x314C.toChar(),
        0x314D.toChar(),
        0x314E.toChar()
    )

    // formula to get HANGUL_CODE by composing consonants and vowel indexes
    // HANGUL_CODE = HANGUL_START + iFirst*NUM_OF_MIDDLE*NUM_OF_LAST_INDEX + iMiddle*NUM_OF_LAST_INDEX + iLast
    // getting the first consonant index from code
    // iFirst = (vCode - HANGUL_START) / (NUM_OF_MIDDLE * NUM_OF_LAST_INDEX)
    // getting the vowel index from code
    // iMiddle = ((vCode - HANGUL_START) % (NUM_OF_MIDDLE * NUM_OF_LAST_INDEX)) / NUM_OF_LAST_INDEX
    // getting the last consonant index from code
    // iLast = (vCode - HANGUL_START) % NUM_OF_LAST_INDEX
    object NormalKeyMap {
        val Code = charArrayOf(
            0x3141.toChar(),
            0x3160.toChar(),
            0x314A.toChar(),
            0x3147.toChar(),
            0x3137.toChar(),
            0x3139.toChar(),
            0x314E.toChar(),
            0x3157.toChar(),
            0x3151.toChar(),
            0x3153.toChar(),
            0x314F.toChar(),
            0x3163.toChar(),
            0x3161.toChar(),
            0x315C.toChar(),
            0x3150.toChar(),
            0x3154.toChar(),
            0x3142.toChar(),
            0x3131.toChar(),
            0x3134.toChar(),
            0x3145.toChar(),
            0x3155.toChar(),
            0x314D.toChar(),
            0x3148.toChar(),
            0x314C.toChar(),
            0x315B.toChar(),
            0x314B.toChar()
        )
        val FirstIndex = intArrayOf(
            6,
            -1,
            14,
            11,
            3,
            5,
            18,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            7,
            0,
            2,
            9,
            -1,
            17,
            12,
            16,
            -1,
            15
        )
        val MiddleIndex = intArrayOf(
            -1,
            17,
            -1,
            -1,
            -1,
            -1,
            -1,
            8,
            2,
            4,
            0,
            20,
            18,
            13,
            1,
            5,
            -1,
            -1,
            -1,
            -1,
            6,
            -1,
            -1,
            -1,
            12,
            -1
        )
        val LastIndex = intArrayOf(
            16,
            -1,
            23,
            21,
            7,
            8,
            27,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            17,
            1,
            4,
            19,
            -1,
            26,
            22,
            25,
            -1,
            24
        )
    }

    object ShiftedKeyMap {
        val Code = charArrayOf(
            0x3141.toChar(),
            0x3160.toChar(),
            0x314A.toChar(),
            0x3147.toChar(),
            0x3138.toChar(),
            0x3139.toChar(),
            0x314E.toChar(),
            0x3157.toChar(),
            0x3151.toChar(),
            0x3153.toChar(),
            0x314F.toChar(),
            0x3163.toChar(),
            0x3161.toChar(),
            0x315C.toChar(),
            0x3152.toChar(),
            0x3156.toChar(),
            0x3143.toChar(),
            0x3132.toChar(),
            0x3134.toChar(),
            0x3146.toChar(),
            0x3155.toChar(),
            0x314D.toChar(),
            0x3149.toChar(),
            0x314C.toChar(),
            0x315B.toChar(),
            0x314B.toChar()
        )
        val FirstIndex = intArrayOf(
            6,
            -1,
            14,
            11,
            4,
            5,
            18,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            8,
            1,
            2,
            10,
            -1,
            17,
            13,
            16,
            -1,
            15
        )
        val MiddleIndex = intArrayOf(
            -1,
            17,
            -1,
            -1,
            -1,
            -1,
            -1,
            8,
            2,
            4,
            0,
            20,
            18,
            13,
            3,
            7,
            -1,
            -1,
            -1,
            -1,
            6,
            -1,
            -1,
            -1,
            12,
            -1
        )
        val LastIndex = intArrayOf(
            16,
            -1,
            23,
            21,
            -1,
            8,
            27,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            2,
            4,
            20,
            -1,
            26,
            -1,
            25,
            -1,
            24
        )
    }

    object LastConsonants {
        val Code = charArrayOf(
            0x0.toChar(),
            0x3131.toChar(),
            0x3132.toChar(),
            0x3133.toChar(),
            0x3134.toChar(),
            0x3135.toChar(),
            0x3136.toChar(),
            0x3137.toChar(),
            0x3139.toChar(),
            0x313A.toChar(),
            0x313B.toChar(),
            0x313C.toChar(),
            0x313D.toChar(),
            0x313E.toChar(),
            0x313F.toChar(),
            0x3140.toChar(),
            0x3141.toChar(),
            0x3142.toChar(),
            0x3144.toChar(),
            0x3145.toChar(),
            0x3146.toChar(),
            0x3147.toChar(),
            0x3148.toChar(),
            0x314A.toChar(),
            0x314B.toChar(),
            0x314C.toChar(),
            0x314D.toChar(),
            0x314E.toChar()
        )
        val iLast = intArrayOf(
            -1,
            -1,
            -1,
            1,
            -1,
            4,
            4,
            -1,
            -1,
            8,
            8,
            8,
            8,
            8,
            8,
            8,
            -1,
            -1,
            17,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1
        )
        val iFirst = intArrayOf(
            -1,
            -1,
            -1,
            9,
            -1,
            12,
            18,
            -1,
            -1,
            0,
            6,
            7,
            9,
            16,
            17,
            18,
            -1,
            -1,
            9,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1
        )
    }

    object Vowels {
        val Code = charArrayOf(
            0x314F.toChar(),
            0x3150.toChar(),
            0x3151.toChar(),
            0x3152.toChar(),
            0x3153.toChar(),
            0x3154.toChar(),
            0x3155.toChar(),
            0x3156.toChar(),
            0x3157.toChar(),
            0x3158.toChar(),
            0x3159.toChar(),
            0x315A.toChar(),
            0x315B.toChar(),
            0x315C.toChar(),
            0x315D.toChar(),
            0x315E.toChar(),
            0x315F.toChar(),
            0x3160.toChar(),
            0x3161.toChar(),
            0x3162.toChar(),
            0x3163.toChar()
        )
        val iMiddle = intArrayOf(
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            8,
            8,
            8,
            -1,
            -1,
            13,
            13,
            13,
            -1,
            -1,
            18,
            -1
        )
    }
}
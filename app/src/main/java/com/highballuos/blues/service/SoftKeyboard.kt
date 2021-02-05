package com.highballuos.blues.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.InputType
import android.text.method.MetaKeyKeyListener
import android.util.Log
import android.view.*
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.inputmethod.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.highballuos.blues.App.Companion.CAPITALIZATION
import com.highballuos.blues.App.Companion.CURRENT_PACKAGE_NAME
import com.highballuos.blues.App.Companion.DEBOUNCE_DELAY_MILLIS
import com.highballuos.blues.App.Companion.PREDICTION
import com.highballuos.blues.App.Companion.PREFS
import com.highballuos.blues.R
import com.highballuos.blues.setting.GlobalSharedPreferences.Companion.CAPITALIZATION_KEY
import com.highballuos.blues.setting.GlobalSharedPreferences.Companion.DEBOUNCE_DELAY_MILLIS_KEY
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext


class SoftKeyboard : InputMethodService(), KeyboardView.OnKeyboardActionListener, CoroutineScope {
    companion object {
        /**
         * This boolean indicates the optional example code for performing
         * processing of hard keys in addition to regular text generation
         * from on-screen interaction.  It would be used for input methods that
         * perform language translations (such as converting text entered on
         * a QWERTY keyboard to Chinese), but may not be used for input methods
         * that are primarily intended to be used for on-screen text entry.
         */
        const val PROCESS_HARD_KEYS = false
    }

    // Coroutine
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job  // Default Context is Main(UI)
    private lateinit var job: Job
    private var updateCandidatesJob: Job? = null

    // Debounce
    private val mHandler = Handler(Looper.getMainLooper())
    private val mRunnable = Runnable { updateCandidates() }

    private lateinit var mInputMethodManager: InputMethodManager    // 키보드와 앱 사이의 중재자 역할을 하는 IMM

    private var mSymbolsKeyboard: QwertyKeyboard? = null // 특수문자 1 키보드
    private var mSymbolsShiftedKeyboard: QwertyKeyboard? = null  // 특수문자 2 키보드
    private var mEnglishKeyboard: QwertyKeyboard? = null  // 영문 쿼티 키보드
    private var mKoreanKeyboard: QwertyKeyboard? = null // 한글 쿼티 키보드
    private var mKoreanKeyboardShifted: QwertyKeyboard? = null    // 한글 쿼티 키보드 Shifted
    private var mCurrentKeyboard: QwertyKeyboard? = null     // 현재 나타낼 키보드. 위 세 개 중 하나가 경우에 따라 할당됨
    private var mLastLetterKeyboard: QwertyKeyboard? = null // 가장 마지막으로 사용했던 문자 키보드 저장

    private var mLastDisplayWidth: Int? = null  // UI 작업에 잠깐 사용되는 변수

    private var mInputView: QwertyKeyboardView? = null   // 키보드 객체가 담길 뷰

    private var mCandidateView: CandidateView? = null   // 자동 완성 뷰
    private val mComposing = StringBuilder()    // 조합 중인 문자열을 담아두는 변수(밑줄)

    private var mMetaState: Long? = null

    private var mCapsLock = false   // 영어 키보드 CapsLock Flag
    private var mHardShift = false  // 물리 키보드 Shift Flag

    private var mCompletions: Array<CompletionInfo>? = null // EditText 자체 내장 자동 완성 데이터를 저장되기 위한 변수
    private var mCompositionOn =
        true   // Composition, 즉 Commit 은 하지 않고 입력에 밑줄만 들어간 상태를 사용할 지에 대한 Flag
    private var mCandidateViewDrawOn = false   // CandidateView 보여줄 것인지에 대한 Flag
    private var mCompletionOn = false   // EditText 자체 내장 자동 완성 데이터 사용할 것인지에 대한 Flag

    private var koreanAutomata: KoreanAutomata? = null
    private var mNoKorean = false

    // special key definitions. (HARD KEY)
    private val KEYCODE_HANGUL = 218 // KeyEvent.KEYCODE_KANA is available from API 16
    private val KEYCODE_HANJA = 212 // KeyEvent.KEYCODE_EISU is available from API 16
    private val KEYCODE_WIN_LEFT = 117 // KeyEvent.KEYCODE_META_LEFT is available from API 11
    private val KEYCODE_SYSREQ = 120 // KeyEvent.KEYCODE_SYSREQ is available from API 11

    private val logTAG = "SoftKeyboard.kt"

    /**
     * [onCreate]
     * 서비스 LifeCycle 내에서 1회 호출
     * 서비스가 시작, 즉 생성될 때 가장 먼저 실행되는 메소드
     * 외부 환경과 IME 사이에서 중재자 역할을 하는 InputMethodManager 초기화
     */
    override fun onCreate() {
        Log.v(logTAG, "onCreate() 함수 시작")
        super.onCreate()
        initializeSettingValues()
        job = SupervisorJob()   // job 초기화 (자식 Coroutine 이 독립적으로 실패할 수 있게 Supervisor)
        mInputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    }

    /**
     * [initializeSettingValues]
     * 서비스 실행 초기에 설정 값을 SharedPreferences 에서 불러오는 메소드
     * MainActivity 에서도 초기화를 해주지만 서비스는 MainActivity 를 거치지 않고도 얼마든지 실행될 수 있음
     */
    private fun initializeSettingValues() {
        CAPITALIZATION = PREFS.getBoolean(CAPITALIZATION_KEY, false)
        DEBOUNCE_DELAY_MILLIS = PREFS.getLong(DEBOUNCE_DELAY_MILLIS_KEY, 700L)
    }

    /**
     * [onDestroy]
     * 서비스가 종료, 즉 소멸될 때 실행되는 메소드
     * 구체적으로는 키보드를 변경 (삼성 키보드, 구글 키보드 등) 혹은 입력 방식 설정 비활성화 할 때 호출됨
     * Service Lifecycle 에 맞춰 Coroutine 종료 (CASCADE, CoroutineContext 달라도 Scope 만 같으면 OK)
     * <<주의>> launch 내부에 Coroutine Logic 이 없을 경우 중단되지 않고 계속 동작? (while true)
     */
    override fun onDestroy() {
        Log.v(logTAG, "onDestroy() 함수 시작")
        super.onDestroy()
        job.cancel()
    }

    /**
     * [getDisplayContext]
     * [onInitializeInterface] 메소드 내부에서 호출되며, SDK 버전에 따라 DisplayContext 를 반환함
     */
    private fun getDisplayContext(): Context {
        Log.v(logTAG, "getDisplayContext() 함수 시작")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // createDisplayContext is not available.
            return this
        }
        // TODO (b/133825283): Non-activity components Resources / DisplayMetrics update when
        //  moving to external display.
        // An issue in Q that non-activity components Resources / DisplayMetrics in
        // Context doesn't well updated when the IME window moving to external display.
        // Currently we do a workaround is to create new display context directly and re-init
        // keyboard layout with this context.
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        return createDisplayContext(wm.defaultDisplay)
    }

    /**
     * [onInitializeInterface]
     * 서비스 LifeCycle 내에서 1회 호출
     * DisplayContext 를 이용하여 키보드 객체를 초기화
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    override fun onInitializeInterface() {
        Log.v(logTAG, "onInitializeInterface() 함수 시작")
        // super.onInitializeInterface()
        val displayContext = getDisplayContext()
        if (mEnglishKeyboard != null) {
            val displayWidth = maxWidth
            if (displayWidth == mLastDisplayWidth) return
            mLastDisplayWidth = displayWidth
        }
        koreanAutomata = KoreanAutomata()
        mEnglishKeyboard = QwertyKeyboard(displayContext, R.xml.qwerty_eng)
        mSymbolsKeyboard = QwertyKeyboard(displayContext, R.xml.qwerty_symbols)
        mSymbolsShiftedKeyboard = QwertyKeyboard(displayContext, R.xml.qwerty_symbols_shift)
        mKoreanKeyboard = QwertyKeyboard(displayContext, R.xml.qwerty_kor)
        mKoreanKeyboardShifted = QwertyKeyboard(displayContext, R.xml.qwerty_kor_shift)
    }

    /**
     * [onCreateInputView]
     * 서비스 LifeCycle 내에서 1회 호출
     * 키보드 객체가 위치할 InputView 를 생성하는 메소드
     */
    @SuppressLint("InflateParams")
    override fun onCreateInputView(): View {
        Log.v(logTAG, "onCreateInputView() 함수 시작")
        mInputView = layoutInflater.inflate(R.layout.input, null) as QwertyKeyboardView
        mInputView?.setOnKeyboardActionListener(this)
        setKeyboardToInputView(mEnglishKeyboard)
        return mInputView!!
    }

    /**
     * [setKeyboardToInputView]
     * nextKeyboard 로 쿼티 영문, 특수문자1, 특수문자2 등의 키보드 객체를 받아
     * 키보드 뷰 (mInputView) 에 키보드를 연결해주는 메소드
     */
    private fun setKeyboardToInputView(nextKeyboard: QwertyKeyboard?) {
        Log.v(logTAG, "setKeyboardToInputView() 함수 시작")
        // xml/method.xml supportsSwitchingToNextInputMethod 값을 읽어옴
        val shouldSupportLanguageSwitchKey = if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
            this.shouldOfferSwitchingToNextInputMethod()
        } else {
            mInputMethodManager.shouldOfferSwitchingToNextInputMethod(getToken())
        }
        // TODO nextKeyboard?.setLanguageSwitchKeyVisibility(shouldSupportLanguageSwitchKey)
        mInputView?.keyboard = nextKeyboard
    }

    /**
     * [onCreateCandidatesView]
     * 서비스 LifeCycle 내에서 1회 호출
     * 키보드 상단의 자동 완성 (후보) View 를 생성하는 메소드
     */
    override fun onCreateCandidatesView(): View {
        Log.v(logTAG, "onCreateCandidatesView() 함수 시작")
        mCandidateView = CandidateView(getDisplayContext())
        mCandidateView!!.setService(this)
        return mCandidateView as CandidateView
    }

    /**
     * [onComputeInsets]
     * CandidateView 가 일부 액티비티 뷰를 가리는 현상을 제거하기 위한 코드
     */
    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        if (!isFullscreenMode) {
            outInsets?.contentTopInsets = outInsets?.visibleTopInsets
        }
    }

    /**
     * [getCurrentForegroundAppPackageName]
     * 현재 Foreground 에서 실행중인 앱의 Package Name 을 반환하는 메소드
     * Android L(21) 부터 UsageStatsManager 를 이용해야 하는데 Context.USAGE_STATS_SERVICE 가 22부터 지원함
     * (정확히는 21에도 정의되어 있지만 @hide Annotation 으로 접근을 막아놨음)
     * usagestats 문자열로 직접 입력하여 21에서도 호출할 수 있도록 한 뒤 WrongConstant Annotation 붙여줌.
     */
    @SuppressLint("WrongConstant")
    private fun getCurrentForegroundAppPackageName(): String {
        var currentAppPackageName = ""
        if (Build.VERSION.SDK_INT > 20) {
            val usm = getSystemService("usagestats") as UsageStatsManager
            val time = System.currentTimeMillis()
            val appList =
                usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time)
            if (appList != null && appList.isNotEmpty()) {
                val mSortedMap: SortedMap<Long, UsageStats> = TreeMap()
                for (usageStats in appList) {
                    mSortedMap[usageStats.lastTimeUsed] = usageStats
                }
                if (mSortedMap.isNotEmpty()) {
                    currentAppPackageName = mSortedMap[mSortedMap.lastKey()]?.packageName ?: ""
                }
            }
        } else {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            currentAppPackageName = am.getRunningTasks(1)[0].topActivity?.packageName ?: ""
        }
        Log.v(logTAG, "현재 실행중인 앱의 패키지 이름 : $currentAppPackageName")
        Toast.makeText(this, currentAppPackageName, Toast.LENGTH_SHORT).show()
        return currentAppPackageName
    }

    /**
     * [onStartInput]
     * 본격적인 입력을 위해 EditText 에 Focus 가 맞춰졌을 때 호출되는 메소드 (몇 번이고 호출 가능)
     * 파라미터로 입력창(EditText) 정보를 받아옴. ex) 전화번호 입력창, 이메일 입력창, URL 입력창, 일반 텍스트 입력창
     * 이 Attribute 들을 기준으로 Case 를 나눠 어떤 키보드를 띄울지를 결정함.
     * 또한, 자동 완성 기능을 사용할지 말지 또한 결정함
     * [PREDICTION] : 추론 기능 On/Off Flag
     * [mCandidateViewDrawOn] : CandidateView 띄울지 말지에 대한 Flag
     * [mCompositionOn] : false 일 시 모든 입력을 Composing 없이 바로 Commit
     * [mCurrentKeyboard] : 현재 키보드
     */
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.v(logTAG, "onStartInput() 함수 시작")

        // 우선 새로운 입력이므로 CandidateView 내용을 초기화
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        initializeInputState()   // 입력이 시작되는 순간이므로 추론 필요 없음.

        if (!restarting) {
            // Clear shift states
            mMetaState = 0
        }

        koreanAutomata?.finishAutomataWithoutInput()

        Log.v(logTAG, attribute?.inputType?.and(InputType.TYPE_MASK_CLASS).toString())

        // Binary AND 연산 : Mask 를 통해 InputType 판단
        when (attribute?.inputType?.and(InputType.TYPE_MASK_CLASS)) {
            // Case 1 : 숫자와 날짜 -> SymbolsKeyboard
            // Numbers and dates default to the symbols keyboard, with
            // no extra features.
            InputType.TYPE_CLASS_NUMBER -> mCurrentKeyboard = mSymbolsKeyboard
            InputType.TYPE_CLASS_DATETIME -> {
                mCurrentKeyboard = mSymbolsKeyboard
                mNoKorean = true
                if (koreanAutomata?.isKoreanMode()!!) koreanAutomata?.toggleMode()
            }

            // Case 2 : 휴대폰 번호 -> SymbolsKeyboard
            // Phones will also default to the symbols keyboard, though
            // often you will want to have a dedicated phone keyboard.
            InputType.TYPE_CLASS_PHONE -> {
                mCurrentKeyboard = mSymbolsKeyboard
                mNoKorean = true
                if (koreanAutomata?.isKoreanMode()!!) koreanAutomata?.toggleMode()
            }

            // Case 3 : 일반 텍스트 -> EnglishKeyboard
            // 가장 중요한 Case, 자동 완성 On
            // This is general text editing.  We will default to the
            // normal alphabetic keyboard, and assume that we should
            // be doing predictive text (showing candidates as the
            // user types).
            InputType.TYPE_CLASS_TEXT -> {
                mCurrentKeyboard = mEnglishKeyboard
                mCandidateViewDrawOn = true
                mCompositionOn = true

                // 특수 Case 를 걸러내기 위한 variation 변수
                // We now look for a few special variations of text that will
                // modify our behavior.
                val variation = attribute.inputType.and(InputType.TYPE_MASK_VARIATION)

                Log.v(logTAG, "VARIATION : $variation")

                // 1. 비밀번호 입력에서는 자동완성 X
                // Termius 의 경우 InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD (0x90)
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                ) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mCandidateViewDrawOn = false
                    // 제안도 필요없고 한국어 조합할 필요도 없음
                    mCompositionOn = false
                    mNoKorean = true
                }

                // 2. URI, Email 역시 자동 완성 X
                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                    variation == InputType.TYPE_TEXT_VARIATION_URI ||
                    variation == InputType.TYPE_TEXT_VARIATION_FILTER
                ) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mCandidateViewDrawOn = false
                    // 제안은 안 하지만 한국어 조합은 해야 함 (크롬)
                    mCompositionOn = true
                    mNoKorean = true
                }

                // 3. 자체 자동 완성 기능 탑재된 EditText 뷰라면 추천 안함.
                if (attribute.inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mCandidateViewDrawOn = false
                    mCompositionOn = true
                    mCompletionOn = isFullscreenMode
                    mNoKorean = true
                }

                mCurrentKeyboard = if (mNoKorean || !koreanAutomata?.isKoreanMode()!!) {
                    mEnglishKeyboard
                } else {
                    mKoreanKeyboard
                }

                // 일반 텍스트 입력 Case 에서 첫 시작은 대문자로 하고 설정
                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute)
            }

            // Case 4 : 이외의 모든 경우 -> EnglishKeyboard, 대문자 시작
            else -> {
                mCurrentKeyboard = if (mNoKorean || !koreanAutomata?.isKoreanMode()!!) {
                    mEnglishKeyboard
                } else {
                    mKoreanKeyboard
                }
                updateShiftKeyState(attribute)
            }
        }

        // 현재 Foreground 에서 실행중인 앱이 추론 기능 Off 로 설정되어있는지 확인
        CURRENT_PACKAGE_NAME = getCurrentForegroundAppPackageName()
        PREDICTION = !PREFS.getBoolean(CURRENT_PACKAGE_NAME, false)

        // 엔터키 역할을 그때 그때 상황에 따라 다르게 설정하는 코드
        // ex) 검색창에서는 엔터 치면 검색, 카카오톡에서는 엔터 치면 줄바꿈
        // TODO 키보드마다 다 바꿔줘야되긴 하는데 더 효율적인 방법 없을까?
        // Update the label on the enter key, depending on what the application
        // says it will do.
        attribute?.imeOptions?.let {
            mEnglishKeyboard?.setImeOptions(resources, it)
            mKoreanKeyboard?.setImeOptions(resources, it)
            mKoreanKeyboardShifted?.setImeOptions(resources, it)
            mSymbolsKeyboard?.setImeOptions(resources, it)
            mSymbolsShiftedKeyboard?.setImeOptions(resources, it)
        }
    }

    /**
     * [onFinishInput]
     * 사용자가 다른 EditText 에 Focus 를 둘 때와 같이 현재 에디터에서 입력 종료 시 호출
     * [onStartInput]과 마찬가지로 여러 번 호출 가능.
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    override fun onFinishInput() {
        Log.v(logTAG, "onFinishInput() 함수 시작")
        super.onFinishInput()

        koreanAutomata?.finishAutomataWithoutInput()
        mNoKorean = false

        // Candidate 초기화
        // Clear current composing text and candidates.
        initializeInputState()   // 입력 중단 되는 순간이므로 추론 필요없음

        // CandidateView 숨기고 InputView 닫기
        // 완전 없애는게 아니고 숨기기만 하는 이유는 UI 적인 이유
        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false)
        mCurrentKeyboard = mEnglishKeyboard
        if (mInputView != null) {
            mInputView!!.closing()
        }
    }

    /**
     * [onStartInputView]
     * 앞서 실행된 [onCreateInputView]에서 할당된 공간에 [onStartInput]에서 결정한 키보드를 띄움
     * 즉, [onStartInput]가 먼저 실행된 이후 [onStartInputView]가 실행됨
     */
    override fun onStartInputView(attribute: EditorInfo?, restarting: Boolean) {
        Log.v(logTAG, "onStartInputView() 함수 시작")
        super.onStartInputView(attribute, restarting)

        // 키보드 배경 색과 네비게이션 바 색깔을 동일하게 맞춰줌
        changeNavigationBarColor()

        if (mLastLetterKeyboard === mEnglishKeyboard) {
            mCurrentKeyboard = mEnglishKeyboard
        } else {
            mCurrentKeyboard = mKoreanKeyboard
            if (!koreanAutomata?.isKoreanMode()!!) {
                koreanAutomata?.toggleMode()
            }
        }

        // Apply the selected keyboard to the input view.
        setKeyboardToInputView(mCurrentKeyboard)

        mInputView!!.closing()

        if (mCandidateViewDrawOn && !mCompletionOn) {    // 제안 가능, 에디터 자체 완성 기능 없을 때
            setCandidatesViewShown(true)
        }

        // subtype 은 언어를 의미...! 한국어 - 영어 - 일본어 - 중국어 등...
        // Custom Keyboard 가 언어 변경 시 타 키보드 (삼성, 구글 키보드 등)와 엮이지 않는 것을 희망한다면
        // switchToNextInputMethod()에 true 를 파라미터로 전달
        // 다른 키보드에서 내 키보드로 넘어오는 것을 막으려면?
        // method.xml 파일에서 android:supportsSwitchingToNextInputMethod="false"

        // TODO 나중에 subtype Logic 제대로 구현
        // val subtype = mInputMethodManager.currentInputMethodSubtype
        // mInputView?.setSubtypeOnSpaceKey(subtype)
    }

    /**
     * [changeNavigationBarColor]
     * 키보드 배경 색과 네비게이션 바 색깔을 동일하게 맞춰주는 메소드
     * 버전에 따라 다르게 기능함.
     */
    private fun changeNavigationBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window? = window.window
            window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window?.statusBarColor = ContextCompat.getColor(this, R.color.keyboard_background)
            window?.navigationBarColor = ContextCompat.getColor(this, R.color.keyboard_background)
            // O 버전 이상일 경우 네비게이션 아이콘 색깔 설정까지
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window?.decorView?.systemUiVisibility = SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    }

    /**
     * [onFinishInputView]
     * InputView 가 내려갈 때 호출
     */
    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        if (koreanAutomata?.isKoreanMode()!!) {
            koreanAutomata?.finishAutomataWithoutInput()
            mKoreanKeyboard?.isShifted = false
        }
    }

    /**
     * [onCurrentInputMethodSubtypeChanged]
     * CurrentInputMethod 의 Subtype 변경 시 호출
     * 순서 : [onKey] 메소드에서 KEYCODE_LANGUAGE_SWITCH 일 경우 [handleLanguageSwitch]를 호출하고
     * [handleLanguageSwitch]는 내부에서 mInputMethodManager.switchToNextInputMethod(getToken(), false /* onlyCurrentIme */) 호출
     * 이는 내부적으로 subtype 을 변경함 따라서 최종적으로 이 메소드가 호출됨
     */
    override fun onCurrentInputMethodSubtypeChanged(subtype: InputMethodSubtype?) {
        Log.v(logTAG, "onCurrentInputMethodSubtypeChanged() 함수 시작")
        mInputView?.setSubtypeOnSpaceKey(subtype!!)
    }

    /**
     * [onUpdateSelection]
     * 커서 위치 변경 시 마다 호출되는 메소드
     * 텍스트 입력으로 인한 커서 변경 또한 호출의 트리거가 됨
     * Deal with the editor reporting movement of its cursor.
     */
    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd,
            candidatesStart, candidatesEnd
        )
        Log.v(logTAG, "onUpdateSelection() 함수 시작")

        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.isNotEmpty() && (newSelStart != candidatesEnd
                    || newSelEnd != candidatesEnd)
        ) {
            koreanAutomata?.finishAutomataWithoutInput()
            initializeInputState()
            val ic = currentInputConnection
            ic?.finishComposingText()
        }
    }

    /**
     * [onDisplayCompletions]
     * 자체 자동 완성 데이터를 가진 EditText 를 위한 메소드
     * 카카오톡의 경우 parameter 가 null 이 되기 때문에 꼭 ? 붙여줘야 함.
     * 전체화면 모드에서는 에디터 내장 자동 완성 기능이 작동하지 않나봄
     * 그래서 Edit Text Completion 을 따로 가져와서 CandidateView 에서 보여주는 듯
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    override fun onDisplayCompletions(completions: Array<CompletionInfo>?) {
        Log.v(logTAG, "onDisplayCompletions() 함수 시작")
        if (mCompletionOn) {
            mCompletions = completions
            if (completions == null) {
                setSuggestions(emptyList(), completions = false, typedWordValid = false)
                return
            }

            val stringList: MutableList<String> = ArrayList()
            for (i in completions.indices) {
                val ci = completions[i]
                stringList.add(ci.text.toString())
            }
            setSuggestions(stringList, completions = true, typedWordValid = true)
        }
    }

    /**
     * [translateKeyDown]
     * 하드 키 이벤트를 [onKey]를 이용하여 소프트키 이벤트로 강제 변환
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private fun translateKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Log.v(logTAG, "translateKeyDown() 함수 시작")
        mMetaState = MetaKeyKeyListener.handleKeyDown(
            mMetaState!!,
            keyCode, event
        )
        var c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState!!))
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState!!)
        val ic = currentInputConnection
        if (c == 0 || ic == null) {
            return false
        }
        // var dead = false
        if (c and KeyCharacterMap.COMBINING_ACCENT != 0) {
            // dead = true
            c = c and KeyCharacterMap.COMBINING_ACCENT_MASK
        }
        if (mComposing.isNotEmpty()) {
            val accent = mComposing[mComposing.length - 1]
            val composed = KeyEvent.getDeadChar(accent.toInt(), c)
            if (composed != 0) {
                c = composed
                mComposing.setLength(mComposing.length - 1)
            }
        }
        onKey(c, null)
        return true
    }

    /**
     * [onKeyDown]
     * 하드 키가 눌릴 때 호출되는 메소드로,
     * 하드 키 이벤트를 [onKey]를 이용하여 소프트키 이벤트로 강제 변환해주도록 작성되었음.
     * 몇몇 치명적인 오류를 방지하기 위함.
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Log.v(logTAG, "onKeyDown() 함수 시작")
        if (event.isShiftPressed) mHardShift = true

        // ALT CTRL 눌려있다면 if 문 안으로 들어가지 않고 통과
        // if ALT or CTRL meta keys are using, the key event should not be touched here and be passed through to.
        if ((event.metaState and (KeyEvent.META_ALT_MASK or KeyEvent.META_CTRL_MASK)) == 0) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK ->    // 휴대폰 하단에 있는 뒤로가기 버튼
                    // pop-up window(길게 눌렀을 때 뜨는 추가 특수문자 창)로 인한 치명적 오류 발생 가능성 차단
                    // The InputMethodService already takes care of the back
                    // key for us, to dismiss the input method if it is shown.
                    // However, our keyboard could be showing a pop-up window
                    // that back should dismiss, so we first allow it to do that.
                    if (event.repeatCount == 0 && mInputView != null) {
                        // 결국 pop-up window 가 있으면 그거부터 없애고 키보드를 없애라는 코드
                        if (mInputView!!.handleBack()) {
                            return true
                        }
                    }
                KeyEvent.KEYCODE_SPACE -> {
                    // Shift + Space = 즉시 Commit?
                    if (!mNoKorean && event.isShiftPressed) {
                        if (mComposing.isNotEmpty()) {
                            val ic = currentInputConnection
                            ic?.let { commitTyped(ic) }
                            initializeInputState()
                        }
                        if (koreanAutomata?.isKoreanMode()!!) {
                            koreanAutomata?.finishAutomataWithoutInput()
                        }
                        koreanAutomata?.toggleMode()
                        return true
                    } else {
                        // 그냥 Space 이벤트
                        translateKeyDown(keyCode, event)
                        return true
                    }
                }
                KEYCODE_HANGUL -> {
                    if (!mNoKorean) {
                        if (mComposing.isNotEmpty()) {
                            val ic = currentInputConnection
                            ic?.let { commitTyped(ic) }
                            initializeInputState()
                        }
                        if (koreanAutomata?.isKoreanMode()!!) {
                            koreanAutomata?.finishAutomataWithoutInput()
                        }
                        koreanAutomata?.toggleMode()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DEL ->    // 일반적인 물리 키보드의 Backspace 버튼
                    // Special handling of the delete key: if we currently are
                    // composing text for the user, we want to modify that instead
                    // of let the application to the delete itself.
                    if (mComposing.isNotEmpty()) {
                        // 소프트 키보드의 Backspace 버튼을 누른 이벤트를 강제 호출
                        onKey(Keyboard.KEYCODE_DELETE, null)
                        // return true 의 의미 : (onKeyUp 호출 생략)
                        // 이 키 입력의 처리는 내 선에서 끝났으니 키보드를 뗄 때 아무 동작도 하지 말 것.
                        return true
                    }
                KeyEvent.KEYCODE_ENTER -> { // 일반적인 물리 키보드의 Enter 버튼
                    if (koreanAutomata?.isKoreanMode()!!) koreanAutomata?.finishAutomataWithoutInput()
                    // return false 의 의미 : Enter 키에 대해서는 따로 코딩을 하지 않고 Editor 가 알아서 하게 냅두자
                    // 검색창에서 엔터 : 검색, 카톡에서 엔터 : 줄바꿈...
                    // 이 경우 onKeyUp 호출 됨
                    // Let the underlying text editor always handle these.
                    return false
                }
                else ->    // 나머지 경우
                    // PROCESS_HARD_KEY 가 true 라면 Keyboard Action Interception 수행
                    // false 라면 수행하지 않음. 즉 컴퓨터에서 키보드 쓰는 것 처럼 처리할 지 소프트 키 입력으로 강제 변환할지
                    // For all other keys, if we want to do transformations on
                    // text being entered with a hard keyboard, we need to process
                    // it and do the appropriate action.
                    if (PROCESS_HARD_KEYS) {
                        // translateKeyDown 내부적으로 결구 onKey() 메소드를 호출
                        if (translateKeyDown(keyCode, event)) return true
                    }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * [onKeyUp]
     * 물리 키보드에서 손을 뗄 때 호출되는 메소드
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        Log.v(logTAG, "onKeyUp() 함수 시작")
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        Log.v(logTAG, "keycode is $keyCode")
        if (PROCESS_HARD_KEYS) {
            // if (mCandidateViewDrawOn) {
            mMetaState = MetaKeyKeyListener.handleKeyUp(
                mMetaState!!,
                keyCode, event
            )
            // }
        }
        mHardShift = false
        return super.onKeyUp(keyCode, event)
    }

    /**
     * [commitTyped]
     * 입력된 문자열(밑줄)을 commit 하고 입력중인 문자열 및 자동 완성 목록을 초기화 해주는 메소드
     * 실제로 commit 하는 것은 inputConnection.commitText 메소드
     * Helper function to commit any text being composed in to the editor.
     */
    private fun commitTyped(inputConnection: InputConnection) {
        Log.v(logTAG, "commitTyped() 함수 시작")
        if (mComposing.isNotEmpty()) {
            inputConnection.commitText(mComposing, mComposing.length)

            // 제안 목록은 초기화
            // Commit 된거니까 딱히 추론할 필요도 없음
            initializeInputState()
        }
    }

    /**
     * [updateShiftKeyState]
     * 키보드를 쉬프트 눌린 상태로 만들어주는 메소드
     * 영어 키보드 첫 글자 입력 시 대문자 상태를 만들어 주기 위함
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private fun updateShiftKeyState(attr: EditorInfo?) {
        Log.v(logTAG, "updateShiftKeyState() 함수 시작")
        if (attr != null && mInputView != null) {
            if (CAPITALIZATION) {   // 영어 첫 글자 대문자화 설정 활성화 되어있을 경우
                if (mEnglishKeyboard === mInputView?.keyboard) {
                    var caps = 0
                    val ei = currentInputEditorInfo
                    if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                        caps = currentInputConnection.getCursorCapsMode(attr.inputType)
                    }
                    mInputView?.isShifted = mCapsLock || caps != 0
                }
            }

            // 한국어는 설정에 영향을 받지 않음
            if (mKoreanKeyboardShifted === mInputView?.keyboard) {
                // 한국어 키보드는 필요 없음. 쉬프트 강제 해제
                mKoreanKeyboardShifted?.isShifted = false
                mInputView?.keyboard = mKoreanKeyboard
                mKoreanKeyboard?.isShifted = false
            }
            changeShiftKeyIcon()
        }
    }

    /**
     * [keyDownUp]
     * keyEventCode 를 받아 특정 키가 한번 눌렸다가 떼진 효과를 발생시키는 메소드
     * [sendKey] 에서 keyCode 가 keyEventCode 각각으로 분류되고 [keyDownUp]을 호출
     * Helper to send a key down / key up pair to the current editor.
     */
    private fun keyDownUp(keyEventCode: Int) {
        Log.v(logTAG, "KeyDownUp() 함수 시작")
        currentInputConnection.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode)
        )
        currentInputConnection.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, keyEventCode)
        )
        // Twitch...?
        if (keyEventCode == KeyEvent.KEYCODE_ENTER) {
            currentInputConnection.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode)
            )
        }
    }

    /**
     * [sendKey]
     * keyCode 를 받아 특정 키가 한번 눌렸다가 떼진 효과를 발생시키는 메소드
     * 내부적으로 [keyDownUp]을 호출
     * Helper to send a character to the editor as raw key events.
     */
    private fun sendKey(keyCode: Int) {
        Log.v(logTAG, "sendKey() 함수 시작")
        when (keyCode) {
            // 1. 줄 바꿈
            '\n'.toInt() -> keyDownUp(KeyEvent.KEYCODE_ENTER)   // TODO 뒤에 toInt() 붙였는데 테스트해보기
            else -> {
                // 2. 숫자
                if (keyCode >= '0'.toInt() && keyCode <= '9'.toInt()) {
                    keyDownUp(keyCode - '0'.toInt() + KeyEvent.KEYCODE_0)
                } else {
                    // 3. 그 외
                    currentInputConnection.commitText((keyCode.toChar()).toString(), 1)
                }
            }
        }
    }

    /**
     * [onKey]
     * Implementation of [KeyboardView.OnKeyboardActionListener]
     * 소프트 키보드에서 눌렀다 뗄 때, 즉 클릭하는 동작이 감지될 경우 호출되는 메소드
     * 추가적으로, onPress()는 누르는 시점, onRelease()는 떼는 시점에서 호출됨
     */
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        Log.v(logTAG, "onKey() 함수 시작")
        // Character 빼고는 다 Preview Enable False
        mInputView?.isPreviewEnabled = false
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> handleBackspace()
            Keyboard.KEYCODE_SHIFT -> handleShift()
            Keyboard.KEYCODE_CANCEL -> handleClose()
            Keyboard.KEYCODE_DONE -> keyDownUp(KeyEvent.KEYCODE_ENTER)
            QwertyKeyboardView.KEYCODE_LANGUAGE_SWITCH -> handleLanguageSwitch()
            QwertyKeyboardView.KEYCODE_OPTIONS -> return
            Keyboard.KEYCODE_MODE_CHANGE -> mInputView?.let { handleModeChange() }
            32 -> handleCharacter(primaryCode, keyCodes)    // Space 는 문자열처럼 처리해주어야 하되, Preview 꺼야함.
            else -> {
                mInputView?.isPreviewEnabled = true
                handleCharacter(primaryCode, keyCodes)
            }
        }
    }

    /**
     * [onText]
     * Implementation of [KeyboardView.OnKeyboardActionListener]
     * 소프트 키보드에서 눌렀다 뗄 때, 즉 클릭하는 동작이 감지될 경우 호출되는 메소드
     * [onKey] 메소드와 호출 타이밍은 같지만 [onText]의 경우 키 하나를 눌렀다 뗄 때 자동으로 문자열이 입력되게 하는 경우 사용
     * 차이는 키 하나를 클릭했을 때 문자 하나가 입력되는지 문자열이 입력되는지.
     */
    override fun onText(text: CharSequence?) {
        Log.v(logTAG, "onText() 함수 시작")
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        if (mComposing.isNotEmpty()) {
            commitTyped(ic)
        }
        ic.commitText(text, 0)
        ic.endBatchEdit()
        updateShiftKeyState(currentInputEditorInfo)
    }

    /**
     * [updateCandidatesAsDebounce]
     * CandidateView 제안 목록 업데이트 요청을 Debouncing
     * 진행중인 Coroutine 중단, delay 시간 초기화
     * [onKey]에는 쉬프트, 언어변경 같은 키 입력도 포함되어 있어 제안 문자열과 관계 없는 경우도 존재
     */
    private fun updateCandidatesAsDebounce() {
        Log.v(logTAG, "updateCandidates() 함수 시작")
        updateCandidatesJob?.cancel()
        mHandler.removeCallbacks(mRunnable)
        if (PREDICTION) {
            mHandler.postDelayed(mRunnable, DEBOUNCE_DELAY_MILLIS)
        } else {
            setSuggestions(listOf("추론 기능이 꺼져있습니다."), completions = false, typedWordValid = false)
        }
    }

    /**
     * [updateCandidates]
     * 실질적으로 CandidateView 제안 목록 업데이트를 실행하는 함수
     * [mRunnable] 안에서 실행
     * CoroutineContext Main(UI) 아닐 경우 에러 발생
     * 네트워크 작업만 따로 Dispatchers.IO or Dispatchers.Default 로 처리
     */
    private fun updateCandidates() {
        if (PREDICTION && mCandidateViewDrawOn) {    // 추론 켜져있고 CandidateView 활성화일 경우에만 작업 실행 (비밀번호)
            updateCandidatesJob = launch {
                // delay(3000)  // For Test
                if (!mCompletionOn) {
                    if (mComposing.isNotEmpty()) {
                        val list = ArrayList<String>()
                        // Test : 거꾸로 표시
                        list.add(mComposing.toString().reversed())

                        Log.v(logTAG, "정상적으로 취소된다면 키보드 변경 시 출력되지 않음")

                        setSuggestions(list, completions = true, typedWordValid = true)
                    } else {
                        setSuggestions(emptyList(), completions = false, typedWordValid = false)
                    }
                }
            }
        }
    }

    /**
     * [initializeInputState]
     * mComposing 을 "" 로 초기화
     * CandidateView 의 Suggestion 을 delay 없이 즉시 초기화
     * 진행중이던 [updateCandidatesJob] cancel (안할 시 지정 시간 이후 추론 문자열이 위치할 수 있음)
     */
    private fun initializeInputState() {
        Log.v(logTAG, "initializeInputState() 함수 시작")
        mComposing.setLength(0) // Composing 은 제안 가능하지 않더라도 이루어질 수 있음 (크롬)
        updateCandidatesJob?.cancel()   // 진행중인 debounceJob 있다면 중단
        mHandler.removeCallbacks(mRunnable) // 실행 예정이던 작업도 취소
        if (PREDICTION && mCandidateViewDrawOn && !mCompletionOn) {
            setSuggestions(emptyList(), completions = false, typedWordValid = false)
        } else if (!PREDICTION) {
            setSuggestions(listOf("추론 기능이 꺼져있습니다."), completions = false, typedWordValid = false)
        }
    }

    /**
     * [setSuggestions]
     * 제안 목록의 상태에 따라 CandidateView 보여지는것 다르게 해주는 메소드
     * 어떤 경우에도 suggestions 에 null 이 들어가지는 않음
     * 차라리 빈 리스트를 넣음.
     */
    private fun setSuggestions(
        suggestions: List<String>, completions: Boolean,
        typedWordValid: Boolean
    ) {
        Log.v(logTAG, "setSuggestions() 함수 시작")
        if (isExtractViewShown) {
            setCandidatesViewShown(true)
        }
        mCandidateView?.setSuggestions(suggestions, completions, typedWordValid)
    }

    /**
     * [handleBackspace]
     * handle~~ 메소드는 전부 [onKey] 메소드 내부에서 각 키 입력에 대한 문자열 처리를 해줌
     * 소프트키 Backspace 입력이 들어왔을 때 처리할 작업
     * Composing 단위가 입력된 문장 전체이므로 KoreanAutomata 의 ComposingString 을 한 글자 이상으로 넘기는 것은 비효율적
     */
    private fun handleBackspace() {
        Log.v(logTAG, "handleBackspace() 함수 시작")
        if (koreanAutomata?.isKoreanMode()!!) {
            val ret = koreanAutomata?.doBackSpace()
            if (ret == KoreanAutomata.ACTION_ERROR) {
                updateShiftKeyState(currentInputEditorInfo)
                return
            }
            ret?.let {
                if (it and KoreanAutomata.ACTION_UPDATE_COMPOSITIONSTR != 0) {
                    if (koreanAutomata?.getCompositionString() != "") {
                        if (mComposing.isNotEmpty()) {
                            mComposing.replace(
                                mComposing.length - 1,
                                mComposing.length,
                                koreanAutomata?.getCompositionString()!!
                            )
                            if (mComposing.isEmpty()) {
                                koreanAutomata?.finishAutomataWithoutInput()
                            }
                            currentInputConnection.setComposingText(mComposing, 1)
                        }
                        updateShiftKeyState(currentInputEditorInfo)
                        return
                    }
                }
            }
        }
        val length = mComposing.length
        when {
            length > 1 -> {
                mComposing.delete(length - 1, length)
                currentInputConnection.setComposingText(mComposing, 1)
                updateCandidatesAsDebounce()
            }
            length > 0 -> {
                currentInputConnection.commitText("", 0)
                initializeInputState()   // "" 되는 순간 진행중이던 추론 중단하고 Suggestion 초기화
            }
            else -> {
                Log.v(logTAG, "KEYCODE_DEL 실행")
                keyDownUp(KeyEvent.KEYCODE_DEL)
            }
        }
        // 입력된 한글을 영어 키보드의 Backspace 로 지운 후 한글을 입력하려고 할 때 이전 CompositionString 의 영향 받는 것을 방지
        // doBackspace 에서 CompositionString 을 구해주므로 초기화해도 상관없음
        koreanAutomata?.finishAutomataWithoutInput()
        updateShiftKeyState(currentInputEditorInfo)
    }

    /**
     * [handleShift]
     * 소프트키 Shift 입력이 들어왔을 때 처리할 작업
     */
    private fun handleShift() {
        Log.v(logTAG, "handleShift() 함수 시작")
        if (mInputView == null) {
            return
        }
        val currentKeyboard = mInputView!!.keyboard
        if (currentKeyboard === mEnglishKeyboard) {
            mCapsLock =
                if (!mCapsLock && !mEnglishKeyboard!!.isShifted) false
                else !mCapsLock && mEnglishKeyboard!!.isShifted
            mEnglishKeyboard!!.isShifted = mCapsLock || !mEnglishKeyboard!!.isShifted
            setKeyboardToInputView(mEnglishKeyboard)
            changeShiftKeyIcon()
        } else if (currentKeyboard === mKoreanKeyboard) {
            mKoreanKeyboard?.isShifted = true
            setKeyboardToInputView(mKoreanKeyboardShifted)
            mKoreanKeyboardShifted?.isShifted = true
            changeShiftKeyIcon()
        } else if (currentKeyboard === mKoreanKeyboardShifted) {
            mKoreanKeyboardShifted?.isShifted = false
            setKeyboardToInputView(mKoreanKeyboard)
            mKoreanKeyboard?.isShifted = false
            changeShiftKeyIcon()
        } else if (currentKeyboard === mSymbolsKeyboard) {
            mSymbolsKeyboard!!.isShifted = true
            setKeyboardToInputView(mSymbolsShiftedKeyboard)
            mSymbolsShiftedKeyboard!!.isShifted = true
        } else if (currentKeyboard === mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard!!.isShifted = false
            setKeyboardToInputView(mSymbolsKeyboard)
            mSymbolsKeyboard!!.isShifted = false
        }
    }

    /**
     * [changeShiftKeyIcon]
     * Shift(CapsLock) 활성화 여부에 따라 아이콘 표시 다르게 설정해주는 메소드
     * 문자(한글/영어) 키보드만 따짐
     * 반드시 mCapsLock 변수 갱신 이후에 위치해야 함.
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun changeShiftKeyIcon() {
        Log.v(logTAG, "changeShiftKeyIcon() 함수 시작")
        val currentKeyboard = mInputView?.keyboard
        val keys = currentKeyboard?.keys   // 모든 Key 객체 정보를 담은 리스트 (xml 기준)
        keys?.let {
            if (currentKeyboard !== mSymbolsKeyboard && currentKeyboard !== mSymbolsShiftedKeyboard) {
                for (key in it) {
                    if (key.codes[0] == -1) {
                        if (mCapsLock && currentKeyboard === mEnglishKeyboard) {
                            key.icon =
                                resources.getDrawable(R.drawable.ic_baseline_keyboard_capslock_activated_24)
                        } else if (currentKeyboard.isShifted) {
                            key.icon =
                                resources.getDrawable(R.drawable.ic_baseline_keyboard_capslock_24)
                        } else {
                            key.icon =
                                resources.getDrawable(R.drawable.ic_baseline_keyboard_arrow_up_24)
                        }
                    }
                }
            }
        }
    }

    private fun handleModeChange() {
        val current = mInputView!!.keyboard as QwertyKeyboard
        if (current === mSymbolsKeyboard || current === mSymbolsShiftedKeyboard) {  // 특수 문자 -> 문자
            // 백업 있으면 그걸로 바꾸고 없으면 영어 키보드로 전환
            if (mLastLetterKeyboard == null) {
                setKeyboardToInputView(mEnglishKeyboard)
            } else {
                // Symbol 상태에서는 오토마타 비활성화였기 때문에 Symbol -> Korean 에서는 Toggle
                if (mLastLetterKeyboard !== mEnglishKeyboard) {
                    koreanAutomata?.toggleMode()
                }
                setKeyboardToInputView(mLastLetterKeyboard)
                // 안 해주면 영어 첫 글자 Shift 안 먹음
                updateShiftKeyState(currentInputEditorInfo)
                changeShiftKeyIcon()
            }
        } else {    // 문자 -> 특수 문자
            val label: String

            // 쉬프트 해제하고 백업
            mCapsLock = false
            current.isShifted = false
            if (current === mEnglishKeyboard) { // 영어 키보드
                mLastLetterKeyboard = mEnglishKeyboard
                label = "ABC"
            } else {    // 한국어 키보드
                // Symbol Keyboard 에서는 오토마타 비활성화 해야 함
                koreanAutomata?.toggleMode()
                mLastLetterKeyboard = mKoreanKeyboard
                label = "가"
            }
            setKeyboardToInputView(mSymbolsKeyboard)
            mSymbolsKeyboard!!.isShifted = false
            mInputView?.setLabelOnModeChangeKey(label)
        }
    }

    /**
     * [handleCharacter]
     * 소프트키 일반 텍스트 입력이 들어왔을 때 처리할 작업
     */
    private fun handleCharacter(primaryCode: Int, keyCodes: IntArray?) {
        Log.v(logTAG, "handleCharacter() 함수 시작")
        var keyState = InputTables.KEYSTATE_NONE    // 0
        var mPrimaryCode = primaryCode
        if (isInputViewShown) {
            if (mInputView!!.isShifted) {
                // 알파벳인 경우에만 대문자로 바꿔주는데 한글, 숫자는 영향 X
                mPrimaryCode = Character.toUpperCase(primaryCode)
                keyState = keyState or InputTables.KEYSTATE_SHIFT   // 1
            }
        }

        // mComposition true 일 경우 입력 그대로 Commit 은 하지 않고 밑줄만 그이도록
        if (mCompositionOn) {
            val ret = koreanAutomata?.doAutomata(mPrimaryCode.toChar(), keyState)
            ret?.let {
                if (it < 0) {   // ACTION_ERROR (-1)
                    if (koreanAutomata?.isKoreanMode()!!) {
                        koreanAutomata?.toggleMode()
                    }
                } else {
                    Log.v(logTAG, "handleCharacter - After calling DoAutomata()")
                    Log.v(
                        logTAG,
                        "   KoreanMode = [" + (if (koreanAutomata?.isKoreanMode()!!) "true" else "false") + "]"
                    )
                    Log.v(
                        logTAG,
                        "   CompleteString = [" + koreanAutomata?.getCompleteString() + "]"
                    )
                    Log.v(
                        logTAG,
                        "   CompositionString = [" + koreanAutomata?.getCompositionString() + "]"
                    )
                    Log.v(
                        logTAG,
                        "   State = [" + koreanAutomata?.getState() + "]"
                    )
                    Log.v(logTAG, "   ret = [$ret]")
                    if (it and KoreanAutomata.ACTION_UPDATE_COMPLETESTR != 0) {
                        if (mComposing.isNotEmpty()) {
                            mComposing.replace(
                                mComposing.length - 1,
                                mComposing.length,
                                koreanAutomata?.getCompleteString()!!
                            )
                        } else {
                            mComposing.append(koreanAutomata?.getCompleteString())
                        }
                        if (mComposing.isNotEmpty()) {
                            currentInputConnection.setComposingText(mComposing, 1)
                        }
                    }
                    if (it and KoreanAutomata.ACTION_UPDATE_COMPOSITIONSTR != 0) {
                        if (mComposing.isNotEmpty() && (it and KoreanAutomata.ACTION_UPDATE_COMPLETESTR) == 0
                            && (it and KoreanAutomata.ACTION_APPEND) == 0
                        ) {
                            mComposing.replace(
                                mComposing.length - 1,
                                mComposing.length,
                                koreanAutomata?.getCompositionString()!!
                            )
                        } else {
                            mComposing.append(koreanAutomata?.getCompositionString())
                        }
                        currentInputConnection.setComposingText(mComposing, 1)
                    }
                }
                // mKoreanMode, 즉 한국어 키보드가 아닐 때 입력 그대로를 결과로 출력
                if (it and KoreanAutomata.ACTION_USE_INPUT_AS_RESULT != 0) {
                    mComposing.append(mPrimaryCode.toChar())    // Not primaryCode
                    currentInputConnection.setComposingText(mComposing, 1)
                    // 한국어 입력이 아니므로 조합중인 문자 초기화
                    koreanAutomata?.finishAutomataWithoutInput()
                }
                updateShiftKeyState(currentInputEditorInfo)
                updateCandidatesAsDebounce()
            }
        } else {    // mComposition false 일 경우 입력하는 족족 바로 Commit 되도록
            if (mComposing.isNotEmpty()) {
                // 기존 Composing 문자열 다 밀어넣고 초기화
                currentInputConnection.commitText(mComposing, 1)
                // Composing 자체가 안되므로 추론도 불가능
                initializeInputState()
            }
            koreanAutomata?.finishAutomataWithoutInput()
            currentInputConnection.commitText(mPrimaryCode.toChar().toString(), 1)
        }
    }

    /**
     * [handleClose]
     * 소프트키 키보드 닫기 입력이 들어왔을 때 처리할 작업
     */
    private fun handleClose() {
        Log.v(logTAG, "handleClose() 함수 시작")
        commitTyped(currentInputConnection)
        requestHideSelf(0)
        mInputView!!.closing()
    }

    /**
     * [getToken]
     * 윈도우 Token 얻는 메소드
     */
    private fun getToken(): IBinder? {
        Log.v(logTAG, "getToken() 함수 시작")
        val dialog = window ?: return null
        val window = dialog.window ?: return null
        return window.attributes.token
    }

    /**
     * [handleLanguageSwitch]
     * 소프트 키 언어 변경 입력이 들어왔을 때 처리할 작업
     */
    private fun handleLanguageSwitch() {
        Log.v(logTAG, "handleLanguageSwitch() 함수 시작")
        // onlyCurrentIme true : 언어 변경 키 클릭 시 다른 키보드로 이동하지 않음 (구글, 삼성 키보드)
        if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
            this.switchToNextInputMethod(true)
        } else {
            mInputMethodManager.switchToNextInputMethod(getToken(), true)
        }

        mCapsLock = false
        val current = mInputView?.keyboard
        if (current === mSymbolsKeyboard || current === mSymbolsShiftedKeyboard) {  // 특수문자 키보드일 때
            // 백업 있으면 그걸로 바꾸고 없으면 영어 키보드로 전환
            if (mLastLetterKeyboard == null) {
                setKeyboardToInputView(mEnglishKeyboard)
            } else {
                // Symbol 상태에서는 오토마타 비활성화였기 때문에 Symbol -> Korean 에서는 Toggle
                if (mLastLetterKeyboard !== mEnglishKeyboard) {
                    koreanAutomata?.toggleMode()
                }
                setKeyboardToInputView(mLastLetterKeyboard)
            }
        } else {    // 문자 키보드일 때
            if (current !== mEnglishKeyboard) {
                mEnglishKeyboard?.isShifted = false
                mLastLetterKeyboard = mEnglishKeyboard
                setKeyboardToInputView(mEnglishKeyboard)
                koreanAutomata?.toggleMode()
            } else {
                mKoreanKeyboard?.isShifted = false
                mLastLetterKeyboard = mKoreanKeyboard
                setKeyboardToInputView(mKoreanKeyboard)
                koreanAutomata?.toggleMode()
            }
        }
        // 안 해주면 첫 글자 Shift 안 먹음
        updateShiftKeyState(currentInputEditorInfo)
        changeShiftKeyIcon()
    }

    /**
     * [pickSuggestionManually]
     * TODO 목적에 맞게 최적화 필요 - 인덱스가 아닌 문자열?
     * 인덱스에 해당되는 자동 완성 선택지를 골라주는 메소드
     */
    fun pickSuggestionManually(index: Int) {
        Log.v(logTAG, "pickSuggestionManually() 함수 시작")
        if (mCompletionOn && mCompletions != null && index >= 0) {
            if (index < mCompletions!!.size) {
                val ci = mCompletions!![index]
                currentInputConnection.commitCompletion(ci)
                mCandidateView?.clear()
                updateShiftKeyState(currentInputEditorInfo)
            }
        } else if (mComposing.isNotEmpty()) {
            // 현재 텍스트에 대한 candidate suggestions 를 생성해두었다면
            // 사용자에 의해 선택된 것을 여기서 Commit 하면 됨.
            // If we were generating candidate suggestions for the current
            // text, we would commit one of them here.  But for this sample,
            // we will just commit the current text.
            currentInputConnection.commitText(mComposing.reverse(), mComposing.length)

            // 자동 완성 문자열 치환 후 한글 입력시 직전 문자에 영향을 받는 현상을 제거
            // mCompleteString, mCompositionString 초기화
            koreanAutomata?.finishAutomataWithoutInput()

            // 자동 완성 문자열을 선택하였으므로 더 이상 delay 줄 필요도 없이 즉시 초기화
            initializeInputState()
        }
    }

    /**
     * [swipeRight]
     * Implementation of [KeyboardView.OnKeyboardActionListener]
     * 키보드 왼쪽에서 오른쪽으로 빠르게 슬라이드 했을 때 호출되는 메소드
     * 현재 프로젝트에서는 필요 없을 듯
     */
    override fun swipeRight() {
        // Sample Code
//        if (mCompletionOn) {
//            pickDefaultCandidate()
//        }
    }

    /**
     * [swipeLeft]
     * Implementation of [KeyboardView.OnKeyboardActionListener]
     * 키보드 오른쪽에서 왼쪽으로 빠르게 슬라이드 했을 때 호출되는 메소드
     * 샘플 코드는 슬라이드 했을 때 한 글자 지워지도록 작성되었음.
     * 역시 현재 프로젝트에서는 필요 없을 듯
     */
    override fun swipeLeft() {
        // Sample Code
        // handleBackspace()
    }

    /**
     * [swipeDown]
     * Implementation of [KeyboardView.OnKeyboardActionListener]
     * 키보드 위쪽에서 아래쪽으로 빠르게 슬라이드 했을 때 호출되는 메소드
     * 샘플 코드는 슬라이드 했을 때 키보드가 닫히도록 작성되었음.
     * 역시 현재 프로젝트에서는 필요 없을 듯
     */
    override fun swipeDown() {
        // Sample Code
        // handleClose()
    }

    /**
     * [swipeUp]
     * Implementation of [KeyboardView.OnKeyboardActionListener]
     * 키보드 아래쪽에서 위쪽으로 빠르게 슬라이드 했을 때 호출되는 메소드
     */
    override fun swipeUp() {}

    /**
     * [onPress]
     * Implementation of [KeyboardView.OnKeyboardActionListener]
     * 소프트 키보드의 키가 눌린 순간 호출되는 메소드
     */
    override fun onPress(primaryCode: Int) {}

    /**
     * [onRelease]
     * Implementation of [KeyboardView.OnKeyboardActionListener]
     * 소프트 키보드의 키가 눌린 상태에서 떼어지는 순간 호출되는 메소드
     */
    override fun onRelease(primaryCode: Int) {}
}
package io.github.elsaphaba.adguardwaker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Insets
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var password: EditText
    private lateinit var startMinutes: EditText
    private lateinit var updateMinutes: EditText
    private lateinit var master: CheckBox
    private lateinit var start: CheckBox
    private lateinit var update: CheckBox
    private lateinit var exact: CheckBox
    private lateinit var overviewStatus: TextView
    private lateinit var startStatus: TextView
    private lateinit var updateStatus: TextView

    private val autoSaveHandler = Handler(Looper.getMainLooper())
    private var pendingAutoSave: Runnable? = null

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        if (::overviewStatus.isInitialized) refresh()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setOnApplyWindowInsetsListener { view, insets ->
                val system = insets.getInsets(
                    WindowInsets.Type.systemBars() or
                        WindowInsets.Type.displayCutout() or WindowInsets.Type.ime()
                )
                view.setPadding(system.left, system.top, system.right, system.bottom)
                insets
            }
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
        }
        scroll.addView(root)
        setContentView(scroll)
        scroll.requestApplyInsets()

        text("AdGuard Waker", 29).setTypeface(null, Typeface.BOLD)
        val p = Scheduler.prefs(this)

        text("AdGuard 자동화 비밀번호", 17)
        password = field(
            p.getString(Scheduler.PASSWORD, "").orEmpty(),
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        ).apply {
            hint = "AdGuard에서 발급한 비밀번호"
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            isSaveEnabled = false
        }

        master = check("자동화 활성화 (부팅 시 자동 복구)", p.getBoolean(Scheduler.MASTER, false))
        space(22) // Distinguish the master switch from the per-command settings.

        start = check("보호 시작 (start)", p.getBoolean("start_enabled", true))
        text("시작 주기 (분)", 15)
        startMinutes = minuteField("시작 주기", Scheduler.minutes(p, Scheduler.START))

        space(22) // Clearly separate start and update controls.
        update = check("업데이트 확인 (update)", p.getBoolean("update_enabled", true))
        text("업데이트 주기 (분)", 15)
        updateMinutes = minuteField("업데이트 주기", Scheduler.minutes(p, Scheduler.UPDATE))

        space(12)
        exact = check("정확한 알람 우선 사용", p.getBoolean(Scheduler.EXACT, true))
        space(8)
        button("설정 저장 및 적용") { saveSettings(showErrors = true) }
        button("지금 보호 시작") { manual(Scheduler.START) }
        button("지금 업데이트 확인") { manual(Scheduler.UPDATE) }
        button("알람 및 리마인더 권한") {
            openSettings(Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:$packageName")
            ))
        }
        button("배터리 최적화 설정") { openBatterySettingsForWaker() }
        button("AdGuard 열기") {
            val launch = packageManager.getLaunchIntentForPackage(AdGuard.PACKAGE)
            if (launch == null) toast("AdGuard를 열 수 없습니다.") else openSettings(launch)
        }

        addStatusCard()
        enableAutoSave()
        // Normalize old intervals when upgrading from versions with a larger upper bound.
        saveSettings(showErrors = false)
    }

    override fun onResume() {
        super.onResume()
        Scheduler.reconcile(this, false)
        Scheduler.prefs(this).registerOnSharedPreferenceChangeListener(prefListener)
        refresh()
    }

    override fun onPause() {
        Scheduler.prefs(this).unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onPause()
    }

    override fun onStop() {
        flushAutoSave() // Save even if the debounce window has not elapsed.
        super.onStop()
    }

    override fun onDestroy() {
        pendingAutoSave?.let(autoSaveHandler::removeCallbacks)
        pendingAutoSave = null
        super.onDestroy()
    }

    private fun enableAutoSave() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) { queueAutoSave() }
        }
        listOf(password, startMinutes, updateMinutes).forEach { it.addTextChangedListener(watcher) }
        listOf(master, start, update, exact).forEach {
            it.setOnCheckedChangeListener { _, _ -> flushAutoSave() }
        }
    }

    private fun queueAutoSave() {
        pendingAutoSave?.let(autoSaveHandler::removeCallbacks)
        val task = Runnable {
            pendingAutoSave = null
            saveSettings(showErrors = false)
        }
        pendingAutoSave = task
        autoSaveHandler.postDelayed(task, 650L)
    }

    private fun flushAutoSave() {
        pendingAutoSave?.let(autoSaveHandler::removeCallbacks)
        pendingAutoSave = null
        if (::startMinutes.isInitialized && ::updateMinutes.isInitialized) {
            saveSettings(showErrors = false)
        }
    }

    private fun text(value: String, size: Int): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setPadding(0, dp(7), 0, dp(7))
            root.addView(this)
        }

    private fun space(height: Int) {
        root.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun field(value: String, type: Int): EditText =
        EditText(this).apply {
            inputType = type
            setSingleLine(true)
            setText(value)
            root.addView(this, LinearLayout.LayoutParams(-1, -2))
        }

    private fun check(title: String, checked: Boolean): CheckBox =
        CheckBox(this).apply {
            text = title
            isChecked = checked
            root.addView(this)
        }

    private fun button(title: String, onClick: () -> Unit) {
        root.addView(Button(this).apply {
            text = title
            isAllCaps = false
            setOnClickListener { onClick() }
        })
    }

    private fun minuteField(title: String, minutes: Int): EditText {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setSelectAllOnFocus(true)
            setText(minutes.toString())
            hint = "1~720"
            contentDescription = "$title (분)"
        }
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(this).apply {
            text = "분"
            setPadding(dp(6), 0, dp(12), 0)
        })
        row.addView(Button(this).apply {
            text = "다이얼"
            isAllCaps = false
            contentDescription = "$title 다이얼 선택"
            setOnClickListener { showMinuteDial(input, title, minutes) }
        })
        root.addView(row, LinearLayout.LayoutParams(-1, -2))
        return input
    }

    private fun showMinuteDial(input: EditText, title: String, fallback: Int) {
        val initial = try {
            ScheduleMath.parseMinutes(input.text.toString())
        } catch (_: NumberFormatException) {
            fallback
        }
        val wheel = NumberPicker(this).apply {
            minValue = ScheduleMath.MIN_MINUTES
            maxValue = ScheduleMath.MAX_MINUTES
            wrapSelectorWheel = false
            value = initial
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            contentDescription = "$title 회전형 분 선택기"
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(12), dp(24), dp(12))
            addView(wheel, LinearLayout.LayoutParams(-2, -2))
            addView(TextView(this@MainActivity).apply {
                text = "분 · 1~720"
                gravity = Gravity.CENTER
            })
        }
        AlertDialog.Builder(this)
            .setTitle("$title (분)")
            .setView(panel)
            .setNegativeButton("취소", null)
            .setPositiveButton("확인") { _, _ ->
                input.setText(wheel.value.toString())
                input.error = null
                flushAutoSave()
            }
            .show()
    }

    private fun addStatusCard() {
        space(22)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setBackgroundResource(R.drawable.status_card)
        }
        val heading = TextView(this).apply {
            text = "자동화 상태"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(10))
        }
        card.addView(heading)
        overviewStatus = statusText(card)
        statusDivider(card)
        startStatus = statusText(card)
        statusDivider(card)
        updateStatus = statusText(card)
        root.addView(card, LinearLayout.LayoutParams(-1, -2))
    }

    private fun statusText(card: LinearLayout): TextView =
        TextView(this).apply {
            textSize = 15f
            setLineSpacing(dp(3).toFloat(), 1f)
            setPadding(0, dp(7), 0, dp(7))
            card.addView(this, LinearLayout.LayoutParams(-1, -2))
        }

    private fun statusDivider(card: LinearLayout) {
        val line = View(this).apply { setBackgroundResource(R.drawable.status_divider) }
        val layout = LinearLayout.LayoutParams(-1, dp(1)).apply {
            topMargin = dp(7)
            bottomMargin = dp(7)
        }
        card.addView(line, layout)
    }

    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()

    private fun openSettings(intent: Intent) {
        if (!tryOpenSettings(intent)) toast("이 기기에서 해당 화면을 열 수 없습니다.")
    }

    private fun tryOpenSettings(intent: Intent): Boolean =
        try {
            startActivity(intent)
            true
        } catch (_: RuntimeException) {
            false
        }

    private fun openBatterySettingsForWaker() {
        val packageUri = Uri.parse("package:$packageName")
        // AOSP app-specific battery settings deep link (hidden API action string).
        // Galaxy One UI often routes Settings through com.android.settings.
        val action = "android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL"
        if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            val samsungSettings = Intent(action, packageUri)
                .setPackage("com.android.settings")
                .putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
            if (tryOpenSettings(samsungSettings)) return
        }
        // Other Android implementations may expose an implicit handler.
        if (tryOpenSettings(Intent(action, packageUri).putExtra(Intent.EXTRA_PACKAGE_NAME, packageName))) return
        // No public cross-OEM guarantee for the app's Battery subpage:
        // fall back to the correct app's OS settings, never to the device battery log.
        if (tryOpenSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))) {
            toast("앱 정보에서 '배터리'를 선택하세요.")
            return
        }
        toast("이 기기는 앱별 배터리 설정 바로가기를 지원하지 않습니다.")
    }

    private fun saveSettings(showErrors: Boolean): Boolean {
        val p = Scheduler.prefs(this)
        val startValue = try {
            ScheduleMath.parseMinutes(startMinutes.text.toString())
        } catch (_: NumberFormatException) {
            if (showErrors) {
                startMinutes.error = "1~720분을 입력하세요."
                return false
            }
            // Preserve the last valid interval instead of persisting incomplete keyboard input.
            Scheduler.minutes(p, Scheduler.START)
        }
        val updateValue = try {
            ScheduleMath.parseMinutes(updateMinutes.text.toString())
        } catch (_: NumberFormatException) {
            if (showErrors) {
                updateMinutes.error = "1~720분을 입력하세요."
                return false
            }
            Scheduler.minutes(p, Scheduler.UPDATE)
        }
        val pass = password.text.toString()
        if (master.isChecked && pass.isBlank()) {
            if (showErrors) password.error = "자동화 비밀번호가 필요합니다."
            return false
        }
        if (master.isChecked && !start.isChecked && !update.isChecked) {
            if (showErrors) toast("최소 한 가지 동작을 선택하세요.")
            return false
        }

        val changed = pass != p.getString(Scheduler.PASSWORD, "") ||
            master.isChecked != p.getBoolean(Scheduler.MASTER, false) ||
            start.isChecked != p.getBoolean("start_enabled", true) ||
            update.isChecked != p.getBoolean("update_enabled", true) ||
            startValue != p.getInt(Scheduler.START + "_minutes", 5) ||
            updateValue != p.getInt(Scheduler.UPDATE + "_minutes", 60) ||
            exact.isChecked != p.getBoolean(Scheduler.EXACT, true)

        if (changed) {
            Scheduler.save(
                this, pass, master.isChecked, start.isChecked, startValue,
                update.isChecked, updateValue, exact.isChecked
            )
        }
        refresh()
        if (showErrors) toast("설정을 저장했습니다.")
        return true
    }

    private fun manual(action: String) {
        val pass = password.text.toString()
        if (pass.isBlank()) {
            password.error = "자동화 비밀번호가 필요합니다."
            return
        }
        val sent = Scheduler.dispatch(this, action, pass)
        refresh()
        toast(
            if (sent) "명령을 전송했습니다. AdGuard에서 결과를 확인하세요."
            else "전송 실패: AdGuard 설치/활성화 상태를 확인하세요."
        )
    }

    private fun refresh() {
        if (!::overviewStatus.isInitialized) return
        val p = Scheduler.prefs(this)
        val power = getSystemService(PowerManager::class.java)
        overviewStatus.text =
            "저장된 자동화: " + (if (p.getBoolean(Scheduler.MASTER, false)) "활성" else "비활성") +
            "\nAdGuard 인터페이스: " + (if (AdGuard.receiver(this) != null) "찾음" else "찾을 수 없음") +
            "\n알람: " + (if (p.getBoolean(Scheduler.EXACT, true) && Scheduler.exactAvailable(this))
                "정확한 알람 우선" else "비정확 알람 (절전 시 지연 가능)") +
            "\n배터리 최적화: " + (if (power.isIgnoringBatteryOptimizations(packageName))
                "제외됨" else "적용 중")

        startStatus.text = formatActionStatus(p, Scheduler.START, "보호 시작 · start")
        updateStatus.text = formatActionStatus(p, Scheduler.UPDATE, "업데이트 확인 · update")
    }

    private fun formatActionStatus(p: SharedPreferences, action: String, label: String): String {
        val due = Scheduler.deadline(p, action)
        val next = if (due == ScheduleMath.OFF) "없음" else {
            format(System.currentTimeMillis() + maxOf(0L, due - SystemClock.elapsedRealtime()))
        }
        val last = p.getLong(action + "_last", 0)
        val attempt = if (last == 0L) "없음" else {
            format(last) + if (p.getBoolean(action + "_sent", false)) " / 전송 요청" else " / 전송 실패"
        }
        return label + "\n다음 목표: " + next + "\n최근 시도: " + attempt
    }

    private fun format(epoch: Long): String = DateFormat.getDateTimeInstance().format(Date(epoch))
}

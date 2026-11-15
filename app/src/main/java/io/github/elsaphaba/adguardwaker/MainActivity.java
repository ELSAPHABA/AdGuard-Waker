package io.github.elsaphaba.adguardwaker;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;

public final class MainActivity extends Activity {
    private EditText password, startMinutes, updateMinutes;
    private CheckBox master, start, update, exact;
    private TextView status;
    private LinearLayout root;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (p, key) -> refresh();
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(24));
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            view.setPadding(i.left, i.top, i.right, i.bottom);
            return insets;
        });
        scroll.requestApplyInsets();
        text("AdGuard Waker", 29);
        text("AdGuard 전용 자동화 · Tasker 불필요", 15);
        SharedPreferences p = Scheduler.prefs(this);
        text("AdGuard 자동화 비밀번호", 17);
        password = field(p.getString(Scheduler.PASSWORD, ""), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setHint("AdGuard에서 발급한 비밀번호");
        password.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        password.setSaveEnabled(false);
        master = check("자동화 활성화 (부팅 시 자동 복구)", p.getBoolean(Scheduler.MASTER, false));
        start = check("보호 시작 (start)", p.getBoolean("start_enabled", true));
        text("시작 주기 (분)", 15);
        startMinutes = field(Integer.toString(Scheduler.minutes(p, Scheduler.START)), InputType.TYPE_CLASS_NUMBER);
        update = check("업데이트 확인 (update)", p.getBoolean("update_enabled", true));
        text("업데이트 주기 (분)", 15);
        updateMinutes = field(Integer.toString(Scheduler.minutes(p, Scheduler.UPDATE)), InputType.TYPE_CLASS_NUMBER);
        exact = check("정확한 알람 우선 사용", p.getBoolean(Scheduler.EXACT, true));
        button("설정 저장 및 적용", v -> save());
        button("지금 보호 시작", v -> manual(Scheduler.START));
        button("지금 업데이트 확인", v -> manual(Scheduler.UPDATE));
        button("알람 및 리마인더 권한", v -> openSettings(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()))));
        button("배터리 최적화 설정", v -> openSettings(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
        button("AdGuard 열기", v -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage(AdGuard.PACKAGE);
            if (launch == null) toast("AdGuard를 열 수 없습니다.");
            else openSettings(launch);
        });
        status = text("", 15);
        text("주기는 1~10,080분입니다. 절전 모드에서는 설정한 시각보다 늦어질 수 있습니다. "
            + "명령 전송은 실제 보호 상태나 업데이트 성공을 보증하지 않습니다. "
            + "AdGuard의 자체 알림과 VPN 아이콘은 이 앱이 제어하지 않습니다.", 14);
    }
    @Override protected void onResume() {
        super.onResume();
        Scheduler.reconcile(this, false);
        Scheduler.prefs(this).registerOnSharedPreferenceChangeListener(listener);
        refresh();
    }
    @Override protected void onPause() {
        Scheduler.prefs(this).unregisterOnSharedPreferenceChangeListener(listener);
        super.onPause();
    }
    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setPadding(0, dp(7), 0, dp(7));
        root.addView(t); return t;
    }
    private EditText field(String value, int type) {
        EditText t = new EditText(this);
        t.setInputType(type); t.setSingleLine(true); t.setText(value);
        root.addView(t, new LinearLayout.LayoutParams(-1, -2)); return t;
    }
    private CheckBox check(String title, boolean checked) {
        CheckBox c = new CheckBox(this); c.setText(title); c.setChecked(checked); root.addView(c); return c;
    }
    private void button(String title, View.OnClickListener click) {
        Button b = new Button(this); b.setText(title); b.setAllCaps(false); b.setOnClickListener(click); root.addView(b);
    }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    private void openSettings(Intent i) {
        try { startActivity(i); } catch (ActivityNotFoundException | SecurityException e) { toast("이 기기에서 해당 화면을 열 수 없습니다."); }
    }
    private void save() {
        int s, u;
        try { s = ScheduleMath.parseMinutes(startMinutes.getText().toString()); }
        catch (NumberFormatException e) { startMinutes.setError("1~10,080분"); return; }
        try { u = ScheduleMath.parseMinutes(updateMinutes.getText().toString()); }
        catch (NumberFormatException e) { updateMinutes.setError("1~10,080분"); return; }
        String pass = password.getText().toString();
        if (master.isChecked() && pass.isBlank()) { password.setError("자동화 비밀번호가 필요합니다."); return; }
        if (master.isChecked() && !start.isChecked() && !update.isChecked()) { toast("최소 한 가지 동작을 선택하세요."); return; }
        Scheduler.save(this, pass, master.isChecked(), start.isChecked(), s, update.isChecked(), u, exact.isChecked());
        refresh(); toast("설정을 저장했습니다.");
    }
    private void manual(String action) {
        String pass = password.getText().toString();
        if (pass.isBlank()) { password.setError("자동화 비밀번호가 필요합니다."); return; }
        boolean sent = Scheduler.dispatch(this, action, pass);
        refresh(); toast(sent ? "명령을 전송했습니다. AdGuard에서 결과를 확인하세요." : "전송 실패: AdGuard 설치/활성화 상태를 확인하세요.");
    }
    private void refresh() {
        if (status == null) return;
        SharedPreferences p = Scheduler.prefs(this);
        PowerManager power = getSystemService(PowerManager.class);
        String summary = "저장된 자동화: " + (p.getBoolean(Scheduler.MASTER, false) ? "활성" : "비활성")
            + "\nAdGuard 인터페이스: " + (AdGuard.receiver(this) != null ? "찾음" : "찾을 수 없음")
            + "\n알람: " + (p.getBoolean(Scheduler.EXACT, true) && Scheduler.exactAvailable(this) ? "정확한 알람 우선" : "비정확 알람 (절전 시 지연 가능)")
            + "\n배터리 최적화: " + (power.isIgnoringBatteryOptimizations(getPackageName()) ? "제외됨" : "적용 중");
        for (String action : new String[]{Scheduler.START, Scheduler.UPDATE}) {
            long last = p.getLong(action + "_last", 0);
            long due = Scheduler.deadline(p, action);
            String next = due == ScheduleMath.OFF ? "없음" : format(System.currentTimeMillis() + Math.max(0, due - SystemClock.elapsedRealtime()));
            summary += "\n\n" + action + " 다음 목표: " + next + "\n최근 시도: "
                + (last == 0 ? "없음" : format(last) + (p.getBoolean(action + "_sent", false) ? " / 전송 요청" : " / 전송 실패"));
        }
        status.setText(summary);
    }
    private String format(long epoch) { return DateFormat.getDateTimeInstance().format(new Date(epoch)); }
}

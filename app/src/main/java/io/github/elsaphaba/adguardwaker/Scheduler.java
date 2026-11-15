package io.github.elsaphaba.adguardwaker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;

final class Scheduler {
    static final String START = "start", UPDATE = "update";
    static final String PASSWORD = "password", MASTER = "enabled", EXACT = "exact";
    private static final int JOB_ID = 7031, REQUEST = 7032;
    private static final long RECOVERY_PERIOD = 15 * 60_000L;
    private Scheduler() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("waker", Context.MODE_PRIVATE);
    }
    static int minutes(SharedPreferences p, String action) {
        return p.getInt(action + "_minutes", START.equals(action) ? 5 : 60);
    }
    static boolean enabled(SharedPreferences p, String action) {
        return p.getBoolean(MASTER, false) && !p.getString(PASSWORD, "").isEmpty()
            && p.getBoolean(action + "_enabled", true);
    }
    static long deadline(SharedPreferences p, String action) {
        return p.getLong(action + "_due", ScheduleMath.OFF);
    }
    static boolean exactAvailable(Context c) {
        return c.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }
    private static int bootCount(Context c) {
        return Settings.Global.getInt(c.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
    }
    // All entry points run on the app's main thread. No asynchronous duplicate dispatch.
    static void save(Context c, String password, boolean master, boolean start, int startMinutes,
                     boolean update, int updateMinutes, boolean exact) {
        SharedPreferences p = prefs(c);
        long now = SystemClock.elapsedRealtime();
        boolean oldStart = enabled(p, START), oldUpdate = enabled(p, UPDATE);
        boolean changedPassword = !password.equals(p.getString(PASSWORD, ""));
        SharedPreferences.Editor e = p.edit().putString(PASSWORD, password).putBoolean(MASTER, master)
            .putBoolean(START + "_enabled", start).putBoolean(UPDATE + "_enabled", update)
            .putInt(START + "_minutes", startMinutes).putInt(UPDATE + "_minutes", updateMinutes)
            .putBoolean(EXACT, exact);
        if (!master || !start || password.isEmpty()) e.remove(START + "_due");
        else if (!oldStart || changedPassword) e.putLong(START + "_due", now);
        else if (minutes(p, START) != startMinutes) e.putLong(START + "_due", ScheduleMath.next(now, startMinutes));
        if (!master || !update || password.isEmpty()) e.remove(UPDATE + "_due");
        else if (!oldUpdate || changedPassword || minutes(p, UPDATE) != updateMinutes)
            e.putLong(UPDATE + "_due", ScheduleMath.next(now, updateMinutes));
        e.commit();
        reconcile(c, false);
    }

    static void reconcile(Context c, boolean bootBroadcast) {
        SharedPreferences p = prefs(c);
        long now = SystemClock.elapsedRealtime();
        int boot = bootCount(c);
        boolean reset = (boot != -1 && boot != p.getInt("boot", -1))
            || (bootBroadcast && boot == -1);
        SharedPreferences.Editor e = p.edit().putInt("boot", boot);
        for (String action : new String[]{START, UPDATE}) {
            if (!enabled(p, action)) e.remove(action + "_due");
            else if (reset || deadline(p, action) == ScheduleMath.OFF) {
                e.putLong(action + "_due", START.equals(action) ? now : ScheduleMath.next(now, minutes(p, action)));
            }
        }
        e.commit();
        for (String action : new String[]{START, UPDATE}) {
            if (enabled(p, action) && ScheduleMath.due(deadline(p, action), now)) {
                // Skip missed repetitions rather than sending a burst after Doze/reboot.
                p.edit().putLong(action + "_due", ScheduleMath.next(now, minutes(p, action))).commit();
                dispatch(c, action, p.getString(PASSWORD, ""));
            }
        }
        arm(c);
    }

    static boolean dispatch(Context c, String action, String password) {
        boolean sent = AdGuard.send(c, action, password);
        prefs(c).edit().putLong(action + "_last", System.currentTimeMillis())
            .putBoolean(action + "_sent", sent).commit();
        return sent;
    }
    private static PendingIntent alarm(Context c) {
        return PendingIntent.getBroadcast(c, REQUEST, new Intent(c, WakeReceiver.class)
            .setAction("io.github.elsaphaba.adguardwaker.TICK"),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private static void arm(Context c) {
        SharedPreferences p = prefs(c);
        AlarmManager am = c.getSystemService(AlarmManager.class);
        JobScheduler js = c.getSystemService(JobScheduler.class);
        long due = Math.min(deadline(p, START), deadline(p, UPDATE));
        PendingIntent pi = alarm(c);
        if (due == ScheduleMath.OFF) {
            am.cancel(pi);
            pi.cancel();
            js.cancel(JOB_ID);
            return;
        }
        long trigger = Math.max(SystemClock.elapsedRealtime() + 1000L, due);
        try {
            if (p.getBoolean(EXACT, true) && am.canScheduleExactAlarms())
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi);
            else am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi);
        } catch (SecurityException revoked) {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi);
        }
        // Recover silently if the OS cancels exact alarms when access is revoked.
        if (js.getPendingJob(JOB_ID) == null) {
            js.schedule(new JobInfo.Builder(JOB_ID, new ComponentName(c, RecoveryJob.class))
                .setPeriodic(RECOVERY_PERIOD).setPersisted(true).build());
        }
    }
}

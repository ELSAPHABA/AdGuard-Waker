package io.github.elsaphaba.adguardwaker

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings

internal object Scheduler {
    const val START = "start"
    const val UPDATE = "update"
    const val PASSWORD = "password"
    const val MASTER = "enabled"
    const val EXACT = "exact"

    private const val JOB_ID = 7031
    private const val REQUEST_ID = 7032
    private const val RECOVERY_PERIOD = 15 * 60_000L
    private const val TICK = "io.github.elsaphaba.adguardwaker.TICK"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("waker", Context.MODE_PRIVATE)

    fun minutes(p: SharedPreferences, action: String): Int =
        p.getInt(action + "_minutes", if (action == START) 5 else 60)
            .coerceIn(ScheduleMath.MIN_MINUTES, ScheduleMath.MAX_MINUTES)

    fun enabled(p: SharedPreferences, action: String): Boolean =
        p.getBoolean(MASTER, false) &&
            !p.getString(PASSWORD, "").isNullOrEmpty() &&
            p.getBoolean(action + "_enabled", true)

    fun deadline(p: SharedPreferences, action: String): Long =
        p.getLong(action + "_due", ScheduleMath.OFF)

    fun exactAvailable(context: Context): Boolean =
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun bootCount(context: Context): Int =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    fun save(
        context: Context, password: String, master: Boolean, start: Boolean,
        startMinutes: Int, update: Boolean, updateMinutes: Int, exact: Boolean
    ) {
        val p = prefs(context)
        val now = SystemClock.elapsedRealtime()
        val wasStartEnabled = enabled(p, START)
        val wasUpdateEnabled = enabled(p, UPDATE)
        val changedPassword = password != p.getString(PASSWORD, "")
        val e = p.edit().putString(PASSWORD, password)
            .putBoolean(MASTER, master)
            .putBoolean(START + "_enabled", start)
            .putBoolean(UPDATE + "_enabled", update)
            .putInt(START + "_minutes", startMinutes)
            .putInt(UPDATE + "_minutes", updateMinutes)
            .putBoolean(EXACT, exact)

        if (!master || !start || password.isEmpty()) e.remove(START + "_due")
        else if (!wasStartEnabled) e.putLong(START + "_due", now)
        else if (changedPassword || p.getInt(START + "_minutes", 5) != startMinutes)
            e.putLong(START + "_due", ScheduleMath.next(now, startMinutes))

        if (!master || !update || password.isEmpty()) e.remove(UPDATE + "_due")
        else if (!wasUpdateEnabled || changedPassword || p.getInt(UPDATE + "_minutes", 60) != updateMinutes)
            e.putLong(UPDATE + "_due", ScheduleMath.next(now, updateMinutes))
        e.commit()
        reconcile(context, false)
    }

    fun reconcile(context: Context, bootBroadcast: Boolean) {
        val p = prefs(context)
        val now = SystemClock.elapsedRealtime()
        // Existing users may have a 721–10,080 minute interval from older releases.
        // Clamp both the stored value and its pending deadline even if the UI is not opened.
        val migration = p.edit()
        var migrated = false
        for (action in listOf(START, UPDATE)) {
            val oldValue = p.getInt(action + "_minutes", if (action == START) 5 else 60)
            val limited = oldValue.coerceIn(ScheduleMath.MIN_MINUTES, ScheduleMath.MAX_MINUTES)
            if (oldValue != limited) {
                migration.putInt(action + "_minutes", limited)
                if (enabled(p, action)) {
                    migration.putLong(action + "_due", ScheduleMath.next(now, limited))
                }
                migrated = true
            }
        }
        if (migrated) migration.commit()
        val boot = bootCount(context)
        val reset = (boot != -1 && boot != p.getInt("boot", -1)) ||
            (bootBroadcast && boot == -1)
        val e = p.edit().putInt("boot", boot)
        for (action in listOf(START, UPDATE)) {
            if (!enabled(p, action)) e.remove(action + "_due")
            else if (reset || deadline(p, action) == ScheduleMath.OFF) {
                e.putLong(
                    action + "_due",
                    if (action == START) now else ScheduleMath.next(now, minutes(p, action))
                )
            }
        }
        e.commit()
        for (action in listOf(START, UPDATE)) {
            if (enabled(p, action) && ScheduleMath.due(deadline(p, action), now)) {
                // Never burst missed repetitions after Doze / reboot.
                p.edit().putLong(action + "_due", ScheduleMath.next(now, minutes(p, action))).commit()
                dispatch(context, action, p.getString(PASSWORD, "").orEmpty())
            }
        }
        arm(context)
    }

    fun dispatch(context: Context, action: String, password: String): Boolean {
        val sent = AdGuard.send(context, action, password)
        prefs(context).edit().putLong(action + "_last", System.currentTimeMillis())
            .putBoolean(action + "_sent", sent).commit()
        return sent
    }

    private fun alarm(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, REQUEST_ID, Intent(context, WakeReceiver::class.java).setAction(TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun arm(context: Context) {
        val p = prefs(context)
        val am = context.getSystemService(AlarmManager::class.java)
        val jobs = context.getSystemService(JobScheduler::class.java)
        val due = minOf(deadline(p, START), deadline(p, UPDATE))
        val pending = alarm(context)
        if (due == ScheduleMath.OFF) {
            am.cancel(pending)
            pending.cancel()
            jobs.cancel(JOB_ID)
            return
        }
        val trigger = maxOf(SystemClock.elapsedRealtime() + 1_000L, due)
        try {
            if (p.getBoolean(EXACT, true) && am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pending)
        }
        if (jobs.getPendingJob(JOB_ID) == null) {
            jobs.schedule(
                JobInfo.Builder(JOB_ID, ComponentName(context, RecoveryJob::class.java))
                    .setPeriodic(RECOVERY_PERIOD).setPersisted(true).build()
            )
        }
    }
}

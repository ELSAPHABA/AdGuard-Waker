package io.github.elsaphaba.adguardwaker;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class RestoreReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) Scheduler.reconcile(context, true);
        else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
            || AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(action))
            Scheduler.reconcile(context, false);
    }
}

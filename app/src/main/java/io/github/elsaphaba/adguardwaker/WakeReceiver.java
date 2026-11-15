package io.github.elsaphaba.adguardwaker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class WakeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if ("io.github.elsaphaba.adguardwaker.TICK".equals(intent.getAction()))
            Scheduler.reconcile(context, false);
    }
}

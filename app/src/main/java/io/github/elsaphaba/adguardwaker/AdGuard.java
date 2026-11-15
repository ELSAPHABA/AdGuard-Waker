package io.github.elsaphaba.adguardwaker;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ActivityInfo;

final class AdGuard {
    static final String PACKAGE = "com.adguard.android";
    private static final String[] RECEIVERS = {
        PACKAGE + ".receiver.AutomationReceiver",
        PACKAGE + ".receivers.AutomationReceiver"
    };
    private AdGuard() {}

    static ComponentName receiver(Context c) {
        for (String name : RECEIVERS) {
            ComponentName component = new ComponentName(PACKAGE, name);
            try {
                ActivityInfo info = c.getPackageManager().getReceiverInfo(component, 0);
                if (info.exported && info.enabled && info.applicationInfo.enabled) return component;
            } catch (PackageManager.NameNotFoundException | SecurityException ignored) { }
        }
        return null;
    }

    static boolean send(Context c, String action, String password) {
        if (!("start".equals(action) || "update".equals(action)) || password.isEmpty()) return false;
        ComponentName receiver = receiver(c);
        if (receiver == null) return false;
        try {
            c.sendBroadcast(new Intent(action).setComponent(receiver)
                .putExtra("password", password).putExtra("quiet", true));
            return true; // Submission only: AdGuard provides no documented result callback.
        } catch (RuntimeException ignored) { return false; }
    }
}

package io.github.elsaphaba.adguardwaker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "io.github.elsaphaba.adguardwaker.TICK")
            Scheduler.reconcile(context, false)
    }
}

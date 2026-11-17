package io.github.elsaphaba.adguardwaker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

internal object AdGuard {
    const val PACKAGE = "com.adguard.android"
    private val receivers = listOf(
        "$PACKAGE.receiver.AutomationReceiver",
        "$PACKAGE.receivers.AutomationReceiver"
    )

    fun receiver(context: Context): ComponentName? {
        for (name in receivers) {
            val component = ComponentName(PACKAGE, name)
            try {
                val info = context.packageManager.getReceiverInfo(component, 0)
                if (info.exported && info.enabled && info.applicationInfo?.enabled == true) return component
            } catch (_: PackageManager.NameNotFoundException) {
                // Fall through to the compatibility receiver.
            } catch (_: SecurityException) {
                // Package visibility or permission restriction.
            }
        }
        return null
    }

    // Submission is not an acknowledgment that AdGuard completed the request.
    fun send(context: Context, action: String, password: String): Boolean {
        if ((action != "start" && action != "update") || password.isEmpty()) return false
        val target = receiver(context) ?: return false
        return try {
            context.sendBroadcast(Intent(action).apply {
                component = target
                putExtra("password", password)
                putExtra("quiet", true)
            })
            true
        } catch (_: RuntimeException) {
            false
        }
    }
}

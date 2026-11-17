package io.github.elsaphaba.adguardwaker

import android.app.job.JobParameters
import android.app.job.JobService

class RecoveryJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Scheduler.reconcile(this, false)
        return false
    }
    override fun onStopJob(params: JobParameters): Boolean = false
}

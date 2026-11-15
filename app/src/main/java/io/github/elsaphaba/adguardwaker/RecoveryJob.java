package io.github.elsaphaba.adguardwaker;

import android.app.job.JobParameters;
import android.app.job.JobService;

public final class RecoveryJob extends JobService {
    @Override public boolean onStartJob(JobParameters params) {
        Scheduler.reconcile(this, false);
        return false;
    }
    @Override public boolean onStopJob(JobParameters params) { return false; }
}

package dev.hydranet.batman

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context

class BatteryMonitorJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        if (BatteryWarningStore.isMonitoringEnabled(this)) {
            BatteryWarningNotifier.checkAndNotify(this)
        }
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false

    companion object {
        private const val JOB_ID = 4220
        private const val FIFTEEN_MINUTES_MS = 15 * 60 * 1000L

        fun schedule(context: Context) {
            if (!BatteryWarningStore.isMonitoringEnabled(context)) {
                cancel(context)
                return
            }

            val job = JobInfo.Builder(
                JOB_ID,
                ComponentName(context, BatteryMonitorJobService::class.java),
            )
                .setPeriodic(FIFTEEN_MINUTES_MS)
                .setPersisted(true)
                .build()

            context.getSystemService(JobScheduler::class.java).schedule(job)
        }

        fun cancel(context: Context) {
            context.getSystemService(JobScheduler::class.java).cancel(JOB_ID)
        }
    }
}

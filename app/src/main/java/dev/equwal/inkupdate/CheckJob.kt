// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 equwal
package dev.equwal.inkupdate

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context

/**
 * The daily check.
 *
 * The job is persisted, so it lives through a restart of the device. That is
 * what the RECEIVE_BOOT_COMPLETED permission is for: the app has no receiver.
 * The system decides the exact minute, and it waits for a network.
 */
class CheckJob : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        Thread {
            val store = Store(applicationContext)
            try {
                Checker.checkAll(applicationContext, store)
            } catch (e: Exception) {
                store.checked(System.currentTimeMillis(), false)
            }
            jobFinished(params, false)
        }.start()
        // True: the work goes on after this function returns.
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = true

    companion object {

        private const val ID = 1
        private const val DAY_MS = 24L * 60L * 60L * 1000L

        /** Starts the daily job. A second call replaces the first. */
        fun schedule(context: Context) {
            val s = context.getSystemService(JobScheduler::class.java) ?: return
            val job = JobInfo.Builder(ID, ComponentName(context, CheckJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(DAY_MS)
                .setPersisted(true)
                .build()
            try {
                s.schedule(job)
            } catch (e: Exception) {
                // A device can refuse the job. The "Check now" button still works.
            }
        }

        /** Stops the daily job. */
        fun cancel(context: Context) {
            context.getSystemService(JobScheduler::class.java)?.cancel(ID)
        }
    }
}

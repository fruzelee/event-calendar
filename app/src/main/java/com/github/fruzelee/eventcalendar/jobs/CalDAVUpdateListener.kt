package com.github.fruzelee.eventcalendar.jobs

import android.annotation.TargetApi
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import com.github.fruzelee.eventcalendar.extensions.recheckEveCalCalendars

// based on https://developer.android.com/reference/android/app/job/JobInfo.Builder.html#addTriggerContentUri(android.app.job.JobInfo.TriggerContentUri)
@TargetApi(Build.VERSION_CODES.N)
class EveCalUpdateListener : JobService() {
    companion object {
        const val CALDAV_EVENT_CONTENT_JOB = 1
    }

    private val mHandler = Handler(Looper.getMainLooper())
    private val mWorker = Runnable {
        scheduleJob(this@EveCalUpdateListener)
        jobFinished(mRunningParams, false)
    }

    private var mRunningParams: JobParameters? = null

    fun scheduleJob(context: Context) {
        val componentName = ComponentName(context, EveCalUpdateListener::class.java)
        val uri = CalendarContract.Calendars.CONTENT_URI
        JobInfo.Builder(CALDAV_EVENT_CONTENT_JOB, componentName).apply {
            addTriggerContentUri(JobInfo.TriggerContentUri(uri, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
            (context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).schedule(build())
        }
    }

    fun isScheduled(context: Context): Boolean {
        val jobScheduler = context.getSystemService(JobScheduler::class.java)
        val jobs = jobScheduler.allPendingJobs
        return jobs.any { it.id == CALDAV_EVENT_CONTENT_JOB }
    }

    fun cancelJob(context: Context) {
        val js = context.getSystemService(JobScheduler::class.java)
        js.cancel(CALDAV_EVENT_CONTENT_JOB)
    }

    override fun onStartJob(params: JobParameters): Boolean {
        mRunningParams = params

        if (params.triggeredContentAuthorities != null && params.triggeredContentUris != null) {
            recheckEveCalCalendars {}
        }

        mHandler.post(mWorker)
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        mHandler.removeCallbacks(mWorker)
        return false
    }
}

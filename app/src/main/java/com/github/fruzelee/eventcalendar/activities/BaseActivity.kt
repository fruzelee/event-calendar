package com.github.fruzelee.eventcalendar.activities

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import com.github.fruzelee.eventcalendar.R
import com.github.fruzelee.eventcalendar.helpers.ensureBackgroundThread

open class BaseActivity : BaseEventActivity() {
    val calDAVRefreshHandler = Handler(Looper.getMainLooper())
    var calDAVRefreshCallback: (() -> Unit)? = null

    override fun getAppIconIDs() = arrayListOf(
        R.mipmap.ic_launcher
    )

    override fun getAppLauncherName() = getString(R.string.app_launcher_name)

    // caldav refresh content observer triggers multiple times in a row at updating, so call the callback only a few seconds after the (hopefully) last one
    private val calDAVSyncObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            if (!selfChange) {
                calDAVRefreshHandler.removeCallbacksAndMessages(null)
                calDAVRefreshHandler.postDelayed({
                    ensureBackgroundThread {
                        unregisterObserver()
                        calDAVRefreshCallback?.invoke()
                        calDAVRefreshCallback = null
                    }
                }, CALDAV_REFRESH_DELAY)
            }
        }
    }

    private fun unregisterObserver() {
        contentResolver.unregisterContentObserver(calDAVSyncObserver)
    }

    companion object {
        private const val CALDAV_REFRESH_DELAY = 3000L
    }
}

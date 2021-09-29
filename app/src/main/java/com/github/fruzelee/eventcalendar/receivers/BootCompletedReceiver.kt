package com.github.fruzelee.eventcalendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.fruzelee.eventcalendar.extensions.notifyRunningEvents
import com.github.fruzelee.eventcalendar.extensions.recheckEveCalCalendars
import com.github.fruzelee.eventcalendar.extensions.scheduleAllEvents
import com.github.fruzelee.eventcalendar.helpers.ensureBackgroundThread

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ensureBackgroundThread {
            context.apply {
                scheduleAllEvents()
                notifyRunningEvents()
                recheckEveCalCalendars {}
            }
        }
    }
}

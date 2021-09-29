package com.github.fruzelee.eventcalendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.fruzelee.eventcalendar.extensions.config
import com.github.fruzelee.eventcalendar.extensions.refreshEveCalCalendars

class EveCalSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.config.caldavSync) {
            context.refreshEveCalCalendars(context.config.caldavSyncedCalendarIds, false)
        }
    }
}

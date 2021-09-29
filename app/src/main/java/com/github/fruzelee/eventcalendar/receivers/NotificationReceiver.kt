package com.github.fruzelee.eventcalendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.github.fruzelee.eventcalendar.extensions.eventsDB
import com.github.fruzelee.eventcalendar.extensions.notifyEvent
import com.github.fruzelee.eventcalendar.extensions.scheduleNextEventReminder
import com.github.fruzelee.eventcalendar.helpers.EVENT_ID
import com.github.fruzelee.eventcalendar.helpers.Formatter
import com.github.fruzelee.eventcalendar.helpers.REMINDER_NOTIFICATION
import com.github.fruzelee.eventcalendar.helpers.ensureBackgroundThread

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakelock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "event_calendar:notification_receiver"
        )
        wakelock.acquire(3000)

        ensureBackgroundThread {
            handleIntent(context, intent)
        }
    }

    private fun handleIntent(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EVENT_ID, -1L)
        if (id == -1L) {
            return
        }

        //context.updateListWidget()
        val event = context.eventsDB.getEventWithId(id)
        if (event == null || event.getReminders()
                .none { it.type == REMINDER_NOTIFICATION } || event.repetitionExceptions.contains(
                Formatter.getTodayCode()
            )
        ) {
            return
        }

        if (!event.repetitionExceptions.contains(Formatter.getDayCodeFromTS(event.startTS))) {
            context.notifyEvent(event)
        }
        context.scheduleNextEventReminder(event, false)
    }
}

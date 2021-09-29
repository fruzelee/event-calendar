package com.github.fruzelee.eventcalendar.services

import android.app.IntentService
import android.content.Intent
import com.github.fruzelee.eventcalendar.extensions.config
import com.github.fruzelee.eventcalendar.extensions.eventsDB
import com.github.fruzelee.eventcalendar.extensions.rescheduleReminder
import com.github.fruzelee.eventcalendar.helpers.EVENT_ID

@Suppress("DEPRECATION")
class SnoozeService : IntentService("Snooze") {
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val eventId = intent.getLongExtra(EVENT_ID, 0L)
            val event = eventsDB.getEventWithId(eventId)
            rescheduleReminder(event, config.snoozeTime)
        }
    }
}

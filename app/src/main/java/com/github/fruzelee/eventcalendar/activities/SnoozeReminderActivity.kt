package com.github.fruzelee.eventcalendar.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.fruzelee.eventcalendar.extensions.config
import com.github.fruzelee.eventcalendar.extensions.eventsDB
import com.github.fruzelee.eventcalendar.extensions.rescheduleReminder
import com.github.fruzelee.eventcalendar.extensions.showPickSecondsDialogHelper
import com.github.fruzelee.eventcalendar.helpers.EVENT_ID
import com.github.fruzelee.eventcalendar.helpers.ensureBackgroundThread

class SnoozeReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showPickSecondsDialogHelper(config.snoozeTime, true) {
            ensureBackgroundThread {
                val eventId = intent.getLongExtra(EVENT_ID, 0L)
                val event = eventsDB.getEventWithId(eventId)
                config.snoozeTime = it / 60
                rescheduleReminder(event, it / 60)
                runOnUiThread {
                    finishActivity()
                }
            }
        }
    }

    private fun finishActivity() {
        finish()
        overridePendingTransition(0, 0)
    }
}

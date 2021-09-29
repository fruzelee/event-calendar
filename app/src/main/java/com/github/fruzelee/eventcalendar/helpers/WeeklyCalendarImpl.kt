package com.github.fruzelee.eventcalendar.helpers

import android.content.Context
import com.github.fruzelee.eventcalendar.extensions.eventsHelper
import com.github.fruzelee.eventcalendar.interfaces.WeeklyCalendar
import com.github.fruzelee.eventcalendar.models.Event
import java.util.*

class WeeklyCalendarImpl(val callback: WeeklyCalendar, val context: Context) {
    private var mEvents = ArrayList<Event>()

    fun updateWeeklyCalendar(weekStartTS: Long) {
        val endTS = weekStartTS + 2 * WEEK_SECONDS
        context.eventsHelper.getEvents(weekStartTS - DAY_SECONDS, endTS) {
            mEvents = it
            callback.updateWeeklyCalendar(it)
        }
    }
}

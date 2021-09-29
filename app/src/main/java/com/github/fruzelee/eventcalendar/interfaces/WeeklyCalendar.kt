package com.github.fruzelee.eventcalendar.interfaces

import com.github.fruzelee.eventcalendar.models.Event

interface WeeklyCalendar {
    fun updateWeeklyCalendar(events: ArrayList<Event>)
}

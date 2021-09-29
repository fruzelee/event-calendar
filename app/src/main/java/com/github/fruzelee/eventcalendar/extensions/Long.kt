package com.github.fruzelee.eventcalendar.extensions

import com.github.fruzelee.eventcalendar.helpers.Formatter
import com.github.fruzelee.eventcalendar.models.Event
import kotlin.math.pow

fun Long.isTsOnProperDay(event: Event): Boolean {
    val dateTime = Formatter.getDateTimeFromTS(this)
    val power = 2.0.pow((dateTime.dayOfWeek - 1).toDouble()).toInt()
    return event.repeatRule and power != 0
}


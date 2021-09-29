package com.github.fruzelee.eventcalendar.helpers

import android.content.Context
import com.github.fruzelee.eventcalendar.R
import com.github.fruzelee.eventcalendar.extensions.config
import com.github.fruzelee.eventcalendar.extensions.seconds
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import kotlin.math.min

object Formatter {
    private const val DAY_CODE_PATTERN = "YYYYMMdd"
    private const val YEAR_PATTERN = "YYYY"
    private const val DAY_PATTERN = "d"
    private const val DAY_OF_WEEK_PATTERN = "EEE"
    private const val PATTERN_TIME_12 = "hh:mm a"
    private const val PATTERN_TIME_24 = "HH:mm"
    private const val PATTERN_HOURS_12 = "h a"
    private const val PATTERN_HOURS_24 = "HH"

    fun getDateFromCode(context: Context, dayCode: String, shortMonth: Boolean = false): String {
        val dateTime = getDateTimeFromCode(dayCode)
        val day = dateTime.toString(DAY_PATTERN)
        val year = dateTime.toString(YEAR_PATTERN)
        val monthIndex = Integer.valueOf(dayCode.substring(4, 6))
        var month = getMonthName(context, monthIndex)
        if (shortMonth)
            month = month.substring(0, min(month.length, 3))
        var date = "$month $day"
        if (year != DateTime().toString(YEAR_PATTERN))
            date += " $year"
        return date
    }

    private fun getDayTitle(context: Context, dayCode: String, addDayOfWeek: Boolean = true): String {
        val date = getDateFromCode(context, dayCode)
        val dateTime = getDateTimeFromCode(dayCode)
        val day = dateTime.toString(DAY_OF_WEEK_PATTERN)
        return if (addDayOfWeek)
            "$date ($day)"
        else
            date
    }

    fun getDate(context: Context, dateTime: DateTime, addDayOfWeek: Boolean = true) =
        getDayTitle(context, getDayCodeFromDateTime(dateTime), addDayOfWeek)

    fun getTodayCode() = getDayCodeFromTS(getNowSeconds())

    fun getHours(context: Context, dateTime: DateTime): String = dateTime.toString(getHourPattern(context))

    fun getTime(context: Context, dateTime: DateTime): String = dateTime.toString(getTimePattern(context))

    private fun getDateTimeFromCode(dayCode: String): DateTime = DateTimeFormat.forPattern(DAY_CODE_PATTERN).withZone(
        DateTimeZone.UTC
    ).parseDateTime(dayCode)

    private fun getLocalDateTimeFromCode(dayCode: String): DateTime =
        DateTimeFormat.forPattern(DAY_CODE_PATTERN).withZone(DateTimeZone.getDefault())
            .parseLocalDate(dayCode).toDateTimeAtStartOfDay()

    fun getTimeFromTS(context: Context, ts: Long) = getTime(context, getDateTimeFromTS(ts))

    fun getDayStartTS(dayCode: String) = getLocalDateTimeFromCode(dayCode).seconds()

    fun getDayEndTS(dayCode: String) =
        getLocalDateTimeFromCode(dayCode).plusDays(1).minusMinutes(1).seconds()

    fun getDayCodeFromDateTime(dateTime: DateTime): String = dateTime.toString(DAY_CODE_PATTERN)

    fun getDateFromTS(ts: Long) = LocalDate(ts * 1000L, DateTimeZone.getDefault())

    fun getDateTimeFromTS(ts: Long) = DateTime(ts * 1000L, DateTimeZone.getDefault())

    fun getUTCDateTimeFromTS(ts: Long) = DateTime(ts * 1000L, DateTimeZone.UTC)

    // use manually translated month names, as DateFormat and JoDa have issues with a lot of languages
    fun getMonthName(context: Context, id: Int): String =
        context.resources.getStringArray(R.array.months)[id - 1]

    private fun getHourPattern(context: Context) =
        if (context.config.use24HourFormat) PATTERN_HOURS_24 else PATTERN_HOURS_12

    private fun getTimePattern(context: Context) =
        if (context.config.use24HourFormat) PATTERN_TIME_24 else PATTERN_TIME_12

    fun getDayCodeFromTS(ts: Long): String {
        val dayCode = getDateTimeFromTS(ts).toString(DAY_CODE_PATTERN)
        return if (dayCode.isNotEmpty()) {
            dayCode
        } else {
            "0"
        }
    }

    fun getUTCDayCodeFromTS(ts: Long): String = getUTCDateTimeFromTS(ts).toString(DAY_CODE_PATTERN)

    fun getShiftedImportTimestamp(ts: Long) = getUTCDateTimeFromTS(ts).withTime(13, 0, 0, 0)
        .withZoneRetainFields(DateTimeZone.getDefault()).seconds()
}

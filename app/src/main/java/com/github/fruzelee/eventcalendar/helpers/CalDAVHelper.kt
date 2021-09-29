package com.github.fruzelee.eventcalendar.helpers

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract.*
import com.github.fruzelee.eventcalendar.R
import com.github.fruzelee.eventcalendar.extensions.*
import com.github.fruzelee.eventcalendar.models.*
import com.github.fruzelee.eventcalendar.objects.States.isUpdatingEveCal
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max

@SuppressLint("MissingPermission")
class EveCalHelper(val context: Context) {
    private val eventsHelper = context.eventsHelper

    fun refreshCalendars(showToasts: Boolean, callback: () -> Unit) {
        if (isUpdatingEveCal) {
            return
        }

        isUpdatingEveCal = true
        try {
            val calDAVCalendars = getEveCalCalendars(context.config.caldavSyncedCalendarIds, showToasts)
            for (calendar in calDAVCalendars) {
                val localEventType = eventsHelper.getEventTypeWithEveCalCalendarId(calendar.id) ?: continue
                localEventType.apply {
                    title = calendar.displayName
                    caldavDisplayName = calendar.displayName
                    caldavEmail = calendar.accountName
                    eventsHelper.insertOrUpdateEventTypeSync(this)
                }

                fetchEveCalCalendarEvents(calendar.id, localEventType.id!!, showToasts)
            }

            context.scheduleEveCalSync(true)
            callback()
        } finally {
            isUpdatingEveCal = false
        }
    }

    @SuppressLint("MissingPermission")
    fun getEveCalCalendars(ids: String, showToasts: Boolean): ArrayList<EveCalCalendar> {
        val calendars = ArrayList<EveCalCalendar>()
        if (!context.hasPermission(PERMISSION_WRITE_CALENDAR) || !context.hasPermission(PERMISSION_READ_CALENDAR)) {
            return calendars
        }

        val uri = Calendars.CONTENT_URI
        val projection = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
            Calendars.OWNER_ACCOUNT,
            Calendars.CALENDAR_COLOR,
            Calendars.CALENDAR_ACCESS_LEVEL)

        val selection = if (ids.trim().isNotEmpty()) "${Calendars._ID} IN ($ids)" else null
        context.queryCursor(uri, projection, selection, showErrors = showToasts) { cursor ->
            val id = cursor.getIntValue(Calendars._ID)
            val displayName = cursor.getStringValue(Calendars.CALENDAR_DISPLAY_NAME)
            val accountName = cursor.getStringValue(Calendars.ACCOUNT_NAME)
            val accountType = cursor.getStringValue(Calendars.ACCOUNT_TYPE)
            val ownerName = cursor.getStringValue(Calendars.OWNER_ACCOUNT)
            val color = cursor.getIntValue(Calendars.CALENDAR_COLOR)
            val accessLevel = cursor.getIntValue(Calendars.CALENDAR_ACCESS_LEVEL)
            val calendar = EveCalCalendar(id, displayName, accountName, accountType, ownerName, color, accessLevel)
            calendars.add(calendar)
        }

        return calendars
    }

    // check if the calendars color or title has changed
    fun updateEveCalCalendar(eventType: EventType) {
        val uri = Calendars.CONTENT_URI
        val newUri = ContentUris.withAppendedId(uri, eventType.caldavCalendarId.toLong())
        val projection = arrayOf(
            Calendars.CALENDAR_COLOR_KEY,
            Calendars.CALENDAR_COLOR,
            Calendars.CALENDAR_DISPLAY_NAME
        )

        context.queryCursor(newUri, projection) { cursor ->
            val properColorKey = cursor.getIntValue(Calendars.CALENDAR_COLOR_KEY)
            val properColor = cursor.getIntValue(Calendars.CALENDAR_COLOR)
            val displayName = cursor.getStringValue(Calendars.CALENDAR_DISPLAY_NAME)
            if (eventType.color != properColor || displayName != eventType.title) {
                val values = fillCalendarContentValues(properColorKey, displayName)
                try {
                    context.contentResolver.update(newUri, values, null, null)
                    eventType.color = properColor
                    eventType.title = displayName
                    context.eventTypesDB.insertOrUpdate(eventType)
                } catch (e: IllegalArgumentException) {
                }
            }
        }
    }

    private fun fillCalendarContentValues(colorKey: Int, title: String): ContentValues {
        return ContentValues().apply {
            put(Calendars.CALENDAR_COLOR_KEY, colorKey)
            put(Calendars.CALENDAR_DISPLAY_NAME, title)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchEveCalCalendarEvents(calendarId: Int, eventTypeId: Long, showToasts: Boolean) {
        val importIdsMap = HashMap<String, Event>()
        val fetchedEventIds = ArrayList<String>()
        val existingEvents = try {
            context.eventsDB.getEventsFromEveCalCalendar("$CALDAV-$calendarId")
        } catch (e: Exception) {
            ArrayList()
        }

        existingEvents.forEach {
            importIdsMap[it.importId] = it
        }

        val uri = Events.CONTENT_URI
        val projection = arrayOf(
            Events._ID,
            Events.TITLE,
            Events.DESCRIPTION,
            Events.DTSTART,
            Events.DTEND,
            Events.DURATION,
            Events.EXDATE,
            Events.ALL_DAY,
            Events.RRULE,
            Events.ORIGINAL_ID,
            Events.ORIGINAL_INSTANCE_TIME,
            Events.EVENT_LOCATION,
            Events.EVENT_TIMEZONE,
            Events.CALENDAR_TIME_ZONE,
            Events.DELETED,
            Events.AVAILABILITY
            )

        val selection = "${Events.CALENDAR_ID} = $calendarId"
        context.queryCursor(uri, projection, selection, showErrors = showToasts) { cursor ->
            val deleted = cursor.getIntValue(Events.DELETED)
            if (deleted == 1) {
                return@queryCursor
            }

            val id = cursor.getLongValue(Events._ID)
            val title = cursor.getStringValue(Events.TITLE)
            val description = cursor.getStringValue(Events.DESCRIPTION)
            val startTS = cursor.getLongValue(Events.DTSTART) / 1000L
            var endTS = cursor.getLongValue(Events.DTEND) / 1000L
            val allDay = cursor.getIntValue(Events.ALL_DAY)
            val rrule = cursor.getStringValue(Events.RRULE)
            val location = cursor.getStringValue(Events.EVENT_LOCATION)
            val originalId = cursor.getStringValue(Events.ORIGINAL_ID)
            val originalInstanceTime = cursor.getLongValue(Events.ORIGINAL_INSTANCE_TIME)
            val reminders = getEveCalEventReminders(id)
            val availability = cursor.getIntValue(Events.AVAILABILITY)

            if (endTS == 0L) {
                val duration = cursor.getStringValue(Events.DURATION)
                endTS = startTS + Parser().parseDurationSeconds(duration)
            }

            val reminder1 = reminders.getOrNull(0)
            val reminder2 = reminders.getOrNull(1)
            val reminder3 = reminders.getOrNull(2)
            val importId = getEveCalEventImportId(calendarId, id)
            val eventTimeZone = cursor.getStringValue(Events.EVENT_TIMEZONE)

            val source = "$CALDAV-$calendarId"
            val repeatRule = Parser().parseRepeatInterval(rrule, startTS)
            val event = Event(null, startTS, endTS, title, location, description, reminder1?.minutes ?: REMINDER_OFF,
                reminder2?.minutes ?: REMINDER_OFF, reminder3?.minutes ?: REMINDER_OFF, reminder1?.type
                ?: REMINDER_NOTIFICATION, reminder2?.type ?: REMINDER_NOTIFICATION, reminder3?.type
                ?: REMINDER_NOTIFICATION, repeatRule.repeatInterval, repeatRule.repeatRule,
                repeatRule.repeatLimit, ArrayList(), importId, eventTimeZone, allDay, eventTypeId, source = source, availability = availability)

            if (event.getIsAllDay()) {
                event.startTS = Formatter.getShiftedImportTimestamp(event.startTS)
                event.endTS = Formatter.getShiftedImportTimestamp(event.endTS)
                if (event.endTS > event.startTS) {
                    event.endTS -= DAY
                }
            }

            fetchedEventIds.add(importId)

            // if the event is an exception from another events repeat rule, find the original parent event
            if (originalInstanceTime != 0L) {
                val parentImportId = "$source-$originalId"
                val parentEvent = context.eventsDB.getEventWithImportId(parentImportId)
                val originalDayCode = Formatter.getDayCodeFromTS(originalInstanceTime / 1000L)
                if (parentEvent != null && !parentEvent.repetitionExceptions.contains(originalDayCode)) {
                    event.parentId = parentEvent.id!!
                    parentEvent.addRepetitionException(originalDayCode)
                    eventsHelper.insertEvent(parentEvent, addToEveCal = false, showToasts = false)

                    event.parentId = parentEvent.id!!
                    event.addRepetitionException(originalDayCode)
                    eventsHelper.insertEvent(event, addToEveCal = false, showToasts = false)
                    return@queryCursor
                }
            }

            // some calendars add repeatable event exceptions with using the "exDate" field, not by creating a child event that is an exception
            // exDate can be stored as "20190216T230000Z", but also as "Europe/Madrid;20201208T000000Z"
            val exDate = cursor.getStringValue(Events.EXDATE)
            if (exDate.length > 8) {
                val lines = exDate.split("\n")
                for (line in lines) {
                    val dates = line.split(",", ";")
                    dates.filter { it.isNotEmpty() && it[0].isDigit() }.forEach {
                        if (it.endsWith("Z")) {
                            // convert for example "20190216T230000Z" to "20190217000000" in Slovakia in a weird way
                            val formatter = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'")
                            val offset = DateTimeZone.getDefault().getOffset(System.currentTimeMillis())
                            val dt = formatter.parseDateTime(it).plusMillis(offset)
                            val dayCode = Formatter.getDayCodeFromDateTime(dt)
                            event.repetitionExceptions.add(dayCode)
                        } else {
                            val potentialTS = it.substring(0, 8)
                            if (potentialTS.areDigitsOnly()) {
                                event.repetitionExceptions.add(potentialTS)
                            }
                        }
                    }
                }
            }

            if (importIdsMap.containsKey(event.importId)) {
                val existingEvent = importIdsMap[importId]
                val originalEventId = existingEvent!!.id

                existingEvent.apply {
                    this.id = null
                    color = 0
                    lastUpdated = 0L
                    repetitionExceptions = ArrayList()
                }

                if (existingEvent.hashCode() != event.hashCode() && title.isNotEmpty()) {
                    event.id = originalEventId
                    eventsHelper.updateEvent(event, updateAtEveCal = false, showToasts = false)
                }
            } else {
                if (title.isNotEmpty()) {
                    importIdsMap[event.importId] = event
                    eventsHelper.insertEvent(event, addToEveCal = false, showToasts = false)
                }
            }
        }

        val eventIdsToDelete = ArrayList<Long>()
        importIdsMap.keys.filter { !fetchedEventIds.contains(it) }.forEach { it ->
            @Suppress("warnings")
            val caldavEventId = it
            existingEvents.forEach {
                if (it.importId == caldavEventId) {
                    eventIdsToDelete.add(it.id!!)
                }
            }
        }

        eventsHelper.deleteEvents(eventIdsToDelete.toMutableList(), false)
    }

    @SuppressLint("MissingPermission")
    fun insertEveCalEvent(event: Event) {
        val uri = Events.CONTENT_URI
        val values = fillEventContentValues(event)
        val newUri = context.contentResolver.insert(uri, values)

        val calendarId = event.getEveCalCalendarId()
        val eventRemoteID = java.lang.Long.parseLong(newUri!!.lastPathSegment!!)
        event.importId = getEveCalEventImportId(calendarId, eventRemoteID)

        setupEveCalEventReminders(event)
        setupEveCalEventImportId(event)
        refreshEveCalCalendar(event)
    }

    fun updateEveCalEvent(event: Event) {
        val uri = Events.CONTENT_URI
        val values = fillEventContentValues(event)
        val eventRemoteID = event.getEveCalEventId()
        event.importId = getEveCalEventImportId(event.getEveCalCalendarId(), eventRemoteID)

        val newUri = ContentUris.withAppendedId(uri, eventRemoteID)
        context.contentResolver.update(newUri, values, null, null)

        setupEveCalEventReminders(event)
        setupEveCalEventImportId(event)
        refreshEveCalCalendar(event)
    }

    private fun setupEveCalEventReminders(event: Event) {
        clearEventReminders(event)
        event.getReminders().forEach {
            val contentValues = ContentValues().apply {
                put(Reminders.MINUTES, it.minutes)
                put(Reminders.METHOD, if (it.type == REMINDER_EMAIL) Reminders.METHOD_EMAIL else Reminders.METHOD_ALERT)
                put(Reminders.EVENT_ID, event.getEveCalEventId())
            }

            try {
                context.contentResolver.insert(Reminders.CONTENT_URI, contentValues)
            } catch (e: Exception) {
                context.toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun setupEveCalEventImportId(event: Event) {
        context.eventsDB.updateEventImportIdAndSource(event.importId, "$CALDAV-${event.getEveCalCalendarId()}", event.id!!)
    }

    private fun fillEventContentValues(event: Event): ContentValues {
        return ContentValues().apply {
            put(Events.CALENDAR_ID, event.getEveCalCalendarId())
            put(Events.TITLE, event.title)
            put(Events.DESCRIPTION, event.description)
            put(Events.DTSTART, event.startTS * 1000L)
            put(Events.ALL_DAY, if (event.getIsAllDay()) 1 else 0)
            put(Events.EVENT_TIMEZONE, event.getTimeZoneString())
            put(Events.EVENT_LOCATION, event.location)
            put(Events.STATUS, Events.STATUS_CONFIRMED)
            put(Events.AVAILABILITY, event.availability)

            val repeatRule = Parser().getRepeatCode(event)
            if (repeatRule.isEmpty()) {
                putNull(Events.RRULE)
            } else {
                put(Events.RRULE, repeatRule)
            }

            if (event.getIsAllDay() && event.endTS >= event.startTS)
                event.endTS += DAY

            if (event.repeatInterval > 0) {
                put(Events.DURATION, getDurationCode(event))
                putNull(Events.DTEND)
            } else {
                put(Events.DTEND, event.endTS * 1000L)
                putNull(Events.DURATION)
            }
        }
    }

    private fun clearEventReminders(event: Event) {
        val selection = "${Reminders.EVENT_ID} = ?"
        val selectionArgs = arrayOf(event.getEveCalEventId().toString())
        context.contentResolver.delete(Reminders.CONTENT_URI, selection, selectionArgs)
    }

    private fun getDurationCode(event: Event): String {
        return if (event.getIsAllDay()) {
            val dur = max(1, (event.endTS - event.startTS) / DAY)
            "P${dur}D"
        } else {
            Parser().getDurationCode((event.endTS - event.startTS) / 60L)
        }
    }

    fun deleteEveCalEvent(event: Event) {
        val uri = Events.CONTENT_URI
        val contentUri = ContentUris.withAppendedId(uri, event.getEveCalEventId())
        try {
            context.contentResolver.delete(contentUri, null, null)
        } catch (ignored: Exception) {
        }
        refreshEveCalCalendar(event)
    }

    fun insertEventRepeatException(event: Event, occurrenceTS: Long) {
        val uri = Events.CONTENT_URI
        val values = fillEventRepeatExceptionValues(event, occurrenceTS)
        try {
            context.contentResolver.insert(uri, values)
            refreshEveCalCalendar(event)
        } catch (e: Exception) {
            context.showErrorToast(e)
        }
    }

    private fun fillEventRepeatExceptionValues(event: Event, occurrenceTS: Long): ContentValues {
        return ContentValues().apply {
            put(Events.CALENDAR_ID, event.getEveCalCalendarId())
            put(Events.DTSTART, occurrenceTS)
            put(Events.DTEND, occurrenceTS + (event.endTS - event.startTS))
            put(Events.ORIGINAL_ID, event.getEveCalEventId())
            put(Events.EVENT_TIMEZONE, TimeZone.getDefault().id.toString())
            put(Events.ORIGINAL_INSTANCE_TIME, occurrenceTS * 1000L)
            put(Events.EXDATE, Formatter.getDayCodeFromTS(occurrenceTS))
        }
    }

    private fun getEveCalEventReminders(eventId: Long): List<Reminder> {
        val reminders = ArrayList<Reminder>()
        val uri = Reminders.CONTENT_URI
        val projection = arrayOf(
            Reminders.MINUTES,
            Reminders.METHOD)
        val selection = "${Reminders.EVENT_ID} = $eventId"

        context.queryCursor(uri, projection, selection) { cursor ->
            val minutes = cursor.getIntValue(Reminders.MINUTES)
            val method = cursor.getIntValue(Reminders.METHOD)
            if (method == Reminders.METHOD_ALERT || method == Reminders.METHOD_EMAIL) {
                val type = if (method == Reminders.METHOD_EMAIL) REMINDER_EMAIL else REMINDER_NOTIFICATION
                val reminder = Reminder(minutes, type)
                reminders.add(reminder)
            }
        }

        return reminders.sortedBy { it.minutes }
    }

    private fun getEveCalEventImportId(calendarId: Int, eventId: Long) = "$CALDAV-$calendarId-$eventId"

    private fun refreshEveCalCalendar(event: Event) = context.refreshEveCalCalendars(event.getEveCalCalendarId().toString(), false)
}

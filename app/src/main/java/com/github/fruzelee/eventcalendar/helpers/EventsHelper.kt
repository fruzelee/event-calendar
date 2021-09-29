package com.github.fruzelee.eventcalendar.helpers

import android.app.Activity
import android.content.Context
import androidx.collection.LongSparseArray
import com.github.fruzelee.eventcalendar.extensions.*
import com.github.fruzelee.eventcalendar.models.Event
import com.github.fruzelee.eventcalendar.models.EventType

class EventsHelper(val context: Context) {
    private val config = context.config
    private val eventsDB = context.eventsDB
    private val eventTypesDB = context.eventTypesDB

    fun getEventTypes(
        activity: Activity,
        showWritableOnly: Boolean,
        callback: (notes: ArrayList<EventType>) -> Unit
    ) {
        ensureBackgroundThread {
            var eventTypes = ArrayList<EventType>()
            try {
                eventTypes = eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>
            } catch (ignored: Exception) {
            }

            if (showWritableOnly) {
                val caldavCalendars = activity.calDAVHelper.getEveCalCalendars("", true)
                eventTypes = eventTypes.filter { it ->
                    @Suppress("warnings")
                    val eventType = it
                    it.caldavCalendarId == 0 || caldavCalendars.firstOrNull { it.id == eventType.caldavCalendarId }
                        ?.canWrite() == true
                }.toMutableList() as ArrayList<EventType>
            }

            activity.runOnUiThread {
                callback(eventTypes)
            }
        }
    }

    private fun getEventTypesSync() =
        eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>

    fun insertOrUpdateEventTypeSync(eventType: EventType): Long {
        if (eventType.id != null && eventType.id!! > 0 && eventType.caldavCalendarId != 0) {
            context.calDAVHelper.updateEveCalCalendar(eventType)
        }

        val newId = eventTypesDB.insertOrUpdate(eventType)
        if (eventType.id == null) {
            config.addDisplayEventType(newId.toString())

            if (config.quickFilterEventTypes.isNotEmpty()) {
                config.addQuickFilterEventType(newId.toString())
            } else {
                val eventTypes = getEventTypesSync()
                if (eventTypes.size == 2) {
                    eventTypes.forEach {
                        config.addQuickFilterEventType(it.id.toString())
                    }
                }
            }
        }
        return newId
    }

    fun getEventTypeWithEveCalCalendarId(calendarId: Int) =
        eventTypesDB.getEventTypeWithEveCalCalendarId(calendarId)

    fun insertEvent(
        event: Event,
        addToEveCal: Boolean,
        showToasts: Boolean,
        callback: ((id: Long) -> Unit)? = null
    ) {
        if (event.startTS > event.endTS) {
            callback?.invoke(0)
            return
        }

        event.id = eventsDB.insertOrUpdate(event)

        context.scheduleNextEventReminder(event, showToasts)

        if (addToEveCal && event.source != SOURCE_SIMPLE_CALENDAR && config.caldavSync) {
            context.calDAVHelper.insertEveCalEvent(event)
        }

        callback?.invoke(event.id!!)
    }

    fun updateEvent(
        event: Event,
        updateAtEveCal: Boolean,
        showToasts: Boolean,
        callback: (() -> Unit)? = null
    ) {
        eventsDB.insertOrUpdate(event)

        context.scheduleNextEventReminder(event, showToasts)
        if (updateAtEveCal && event.source != SOURCE_SIMPLE_CALENDAR && config.caldavSync) {
            context.calDAVHelper.updateEveCalEvent(event)
        }
        callback?.invoke()
    }

    fun deleteEvent(id: Long, deleteFromEveCal: Boolean) =
        deleteEvents(arrayListOf(id), deleteFromEveCal)

    fun deleteEvents(ids: MutableList<Long>, deleteFromEveCal: Boolean) {
        if (ids.isEmpty()) {
            return
        }

        ids.chunked(CHOPPED_LIST_DEFAULT_SIZE).forEach { it ->
            val eventsWithImportId = eventsDB.getEventsByIdsWithImportIds(it)
            eventsDB.deleteEvents(it)

            it.forEach {
                context.cancelNotification(it)
                context.cancelPendingIntent(it)
            }

            if (deleteFromEveCal && config.caldavSync) {
                eventsWithImportId.forEach {
                    context.calDAVHelper.deleteEveCalEvent(it)
                }
            }

            deleteChildEvents(it as MutableList<Long>, deleteFromEveCal)
        }
    }

    private fun deleteChildEvents(ids: List<Long>, deleteFromEveCal: Boolean) {
        val childIds = eventsDB.getEventIdsWithParentIds(ids).toMutableList()
        if (childIds.isNotEmpty()) {
            deleteEvents(childIds, deleteFromEveCal)
        }
    }

    fun addEventRepeatLimit(eventId: Long, limitTS: Long) {
        val time = Formatter.getDateTimeFromTS(limitTS)
        eventsDB.updateEventRepetitionLimit(limitTS - time.hourOfDay, eventId)
        context.cancelNotification(eventId)
        context.cancelPendingIntent(eventId)
        if (config.caldavSync) {
            val event = eventsDB.getEventWithId(eventId)
            if (event?.getEveCalCalendarId() != 0) {
                context.calDAVHelper.updateEveCalEvent(event!!)
            }
        }
    }

    fun addEventRepetitionException(parentEventId: Long, occurrenceTS: Long, addToEveCal: Boolean) {
        ensureBackgroundThread {
            val parentEvent =
                eventsDB.getEventWithId(parentEventId) ?: return@ensureBackgroundThread
            var repetitionExceptions = parentEvent.repetitionExceptions
            repetitionExceptions.add(Formatter.getDayCodeFromTS(occurrenceTS))
            repetitionExceptions =
                repetitionExceptions.distinct().toMutableList() as ArrayList<String>

            eventsDB.updateEventRepetitionExceptions(repetitionExceptions.toString(), parentEventId)
            context.scheduleNextEventReminder(parentEvent, false)

            if (addToEveCal && config.caldavSync) {
                context.calDAVHelper.insertEventRepeatException(parentEvent, occurrenceTS)
            }
        }
    }

    fun getEvents(
        fromTS: Long,
        toTS: Long,
        eventId: Long = -1L,
        applyTypeFilter: Boolean = true,
        callback: (events: ArrayList<Event>) -> Unit
    ) {
        ensureBackgroundThread {
            getEventsSync(fromTS, toTS, eventId, applyTypeFilter, callback)
        }
    }

    private fun getEventsSync(
        fromTS: Long,
        toTS: Long,
        eventId: Long = -1L,
        applyTypeFilter: Boolean,
        callback: (events: ArrayList<Event>) -> Unit
    ) {

        var events = if (applyTypeFilter) {
            val displayEventTypes = context.config.displayEventTypes
            if (displayEventTypes.isEmpty()) {
                callback(ArrayList())
                return
            } else {
                try {
                    eventsDB.getOneTimeEventsFromToWithTypes(
                        toTS,
                        fromTS,
                        context.config.getDisplayEventTypesAsList()
                    ).toMutableList() as ArrayList<Event>
                } catch (e: Exception) {
                    ArrayList()
                }
            }
        } else {
            if (eventId == -1L) {
                eventsDB.getOneTimeEventsFromTo(toTS, fromTS).toMutableList() as ArrayList<Event>
            } else {
                eventsDB.getOneTimeEventFromToWithId(eventId, toTS, fromTS)
                    .toMutableList() as ArrayList<Event>
            }
        }

        events.addAll(getRepeatableEventsFor(fromTS, toTS, eventId, applyTypeFilter))

        events = events
            .asSequence()
            .distinct()
            .filterNot { it.repetitionExceptions.contains(Formatter.getDayCodeFromTS(it.startTS)) }
            .toMutableList() as ArrayList<Event>

        val eventTypeColors = LongSparseArray<Int>()
        context.eventTypesDB.getEventTypes().forEach {
            eventTypeColors.put(it.id!!, it.color)
        }

        events.forEach {
            it.updateIsPastEvent()
            val originalEvent = eventsDB.getEventWithId(it.id!!)
            if (originalEvent != null) {
                val eventStartDate = Formatter.getDateFromTS(it.startTS)
                val originalEventStartDate = Formatter.getDateFromTS(originalEvent.startTS)
                if (it.hasMissingYear().not()) {
                    val years = (eventStartDate.year - originalEventStartDate.year).coerceAtLeast(0)
                    if (years > 0) {
                        it.title = "${it.title} ($years)"
                    }
                }
            }
            it.color = eventTypeColors.get(it.eventType) ?: config.primaryColor
        }

        callback(events)
    }

    fun getRepeatableEventsFor(
        fromTS: Long,
        toTS: Long,
        eventId: Long = -1L,
        applyTypeFilter: Boolean = false
    ): List<Event> {
        val events = if (applyTypeFilter) {
            val displayEventTypes = context.config.displayEventTypes
            if (displayEventTypes.isEmpty()) {
                return ArrayList()
            } else {
                eventsDB.getRepeatableEventsFromToWithTypes(
                    toTS,
                    context.config.getDisplayEventTypesAsList()
                ).toMutableList() as ArrayList<Event>
            }
        } else {
            if (eventId == -1L) {
                eventsDB.getRepeatableEventsFromToWithTypes(toTS)
                    .toMutableList() as ArrayList<Event>
            } else {
                eventsDB.getRepeatableEventFromToWithId(eventId, toTS)
                    .toMutableList() as ArrayList<Event>
            }
        }

        val startTimes = LongSparseArray<Long>()
        val newEvents = ArrayList<Event>()
        events.forEach {
            startTimes.put(it.id!!, it.startTS)
            if (it.repeatLimit >= 0) {
                newEvents.addAll(getEventsRepeatingTillDateOrForever(fromTS, toTS, startTimes, it))
            } else {
                newEvents.addAll(getEventsRepeatingXTimes(fromTS, toTS, startTimes, it))
            }
        }

        return newEvents
    }

    private fun getEventsRepeatingXTimes(
        fromTS: Long,
        toTS: Long,
        startTimes: LongSparseArray<Long>,
        event: Event
    ): ArrayList<Event> {
        val original = event.copy()
        val events = ArrayList<Event>()
        while (event.repeatLimit < 0 && event.startTS <= toTS) {
            if (event.repeatInterval.isXWeeklyRepetition()) {
                if (event.startTS.isTsOnProperDay(event)) {
                    if (event.isOnProperWeek(startTimes)) {
                        if (event.endTS >= fromTS) {
                            event.copy().apply {
                                updateIsPastEvent()
                                color = event.color
                                events.add(this)
                            }
                        }
                        event.repeatLimit++
                    }
                }
            } else {
                if (event.endTS >= fromTS) {
                    event.copy().apply {
                        updateIsPastEvent()
                        color = event.color
                        events.add(this)
                    }
                } else if (event.getIsAllDay()) {
                    val dayCode = Formatter.getDayCodeFromTS(fromTS)
                    val endDayCode = Formatter.getDayCodeFromTS(event.endTS)
                    if (dayCode == endDayCode) {
                        event.copy().apply {
                            updateIsPastEvent()
                            color = event.color
                            events.add(this)
                        }
                    }
                }
                event.repeatLimit++
            }
            event.addIntervalTime(original)
        }
        return events
    }

    private fun getEventsRepeatingTillDateOrForever(
        fromTS: Long,
        toTS: Long,
        startTimes: LongSparseArray<Long>,
        event: Event
    ): ArrayList<Event> {
        val original = event.copy()
        val events = ArrayList<Event>()
        while (event.startTS <= toTS && (event.repeatLimit == 0L || event.repeatLimit >= event.startTS)) {
            if (event.endTS >= fromTS) {
                if (event.repeatInterval.isXWeeklyRepetition()) {
                    if (event.startTS.isTsOnProperDay(event)) {
                        if (event.isOnProperWeek(startTimes)) {
                            event.copy().apply {
                                updateIsPastEvent()
                                color = event.color
                                events.add(this)
                            }
                        }
                    }
                } else {
                    event.copy().apply {
                        updateIsPastEvent()
                        color = event.color
                        events.add(this)
                    }
                }
            }

            if (event.getIsAllDay()) {
                if (event.repeatInterval.isXWeeklyRepetition()) {
                    if (event.endTS >= toTS && event.startTS.isTsOnProperDay(event)) {
                        if (event.isOnProperWeek(startTimes)) {
                            event.copy().apply {
                                updateIsPastEvent()
                                color = event.color
                                events.add(this)
                            }
                        }
                    }
                } else {
                    val dayCode = Formatter.getDayCodeFromTS(fromTS)
                    val endDayCode = Formatter.getDayCodeFromTS(event.endTS)
                    if (dayCode == endDayCode) {
                        event.copy().apply {
                            updateIsPastEvent()
                            color = event.color
                            events.add(this)
                        }
                    }
                }
            }
            event.addIntervalTime(original)
        }
        return events
    }

    fun getRunningEvents(): List<Event> {
        val ts = getNowSeconds()
        val events = eventsDB.getOneTimeEventsFromTo(ts, ts).toMutableList() as ArrayList<Event>
        events.addAll(getRepeatableEventsFor(ts, ts))
        return events
    }

}

package com.github.fruzelee.eventcalendar.helpers

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import com.github.fruzelee.eventcalendar.extensions.config
import com.github.fruzelee.eventcalendar.extensions.scheduleEveCalSync
import java.util.*

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var startWeeklyAt: Int
        get() = prefs.getInt(START_WEEKLY_AT, 7)
        set(startWeeklyAt) = prefs.edit().putInt(START_WEEKLY_AT, startWeeklyAt).apply()

    var showMidnightSpanningEventsAtTop: Boolean
        get() = prefs.getBoolean(SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP, true)
        set(midnightSpanning) = prefs.edit().putBoolean(SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP, midnightSpanning).apply()

    var vibrateOnReminder: Boolean
        get() = prefs.getBoolean(VIBRATE, false)
        set(vibrate) = prefs.edit().putBoolean(VIBRATE, vibrate).apply()

    var reminderSoundUri: String
        get() = prefs.getString(REMINDER_SOUND_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())!!
        set(reminderSoundUri) = prefs.edit().putString(REMINDER_SOUND_URI, reminderSoundUri).apply()

    var lastSoundUri: String
        get() = prefs.getString(LAST_SOUND_URI, "")!!
        set(lastSoundUri) = prefs.edit().putString(LAST_SOUND_URI, lastSoundUri).apply()

    var lastReminderChannel: Long
        get() = prefs.getLong(LAST_REMINDER_CHANNEL_ID, 0L)
        set(lastReminderChannel) = prefs.edit().putLong(LAST_REMINDER_CHANNEL_ID, lastReminderChannel).apply()

    var storedView: Int
        get() = prefs.getInt(VIEW, WEEKLY_VIEW)
        set(view) = prefs.edit().putInt(VIEW, view).apply()

    var lastEventReminderMinutes1: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES, 10)
        set(lastEventReminderMinutes) = prefs.edit().putInt(LAST_EVENT_REMINDER_MINUTES, lastEventReminderMinutes).apply()

    var lastEventReminderMinutes2: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES_2, REMINDER_OFF)
        set(lastEventReminderMinutes2) = prefs.edit().putInt(LAST_EVENT_REMINDER_MINUTES_2, lastEventReminderMinutes2).apply()

    var lastEventReminderMinutes3: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES_3, REMINDER_OFF)
        set(lastEventReminderMinutes3) = prefs.edit().putInt(LAST_EVENT_REMINDER_MINUTES_3, lastEventReminderMinutes3).apply()

    var displayEventTypes: Set<String>
        get() = prefs.getStringSet(DISPLAY_EVENT_TYPES, HashSet())!!
        set(displayEventTypes) = prefs.edit().remove(DISPLAY_EVENT_TYPES).putStringSet(DISPLAY_EVENT_TYPES, displayEventTypes).apply()

    var quickFilterEventTypes: Set<String>
        get() = prefs.getStringSet(QUICK_FILTER_EVENT_TYPES, HashSet())!!
        set(quickFilterEventTypes) = prefs.edit().remove(QUICK_FILTER_EVENT_TYPES).putStringSet(QUICK_FILTER_EVENT_TYPES, quickFilterEventTypes).apply()

    fun addQuickFilterEventType(type: String) {
        val currQuickFilterEventTypes = HashSet(quickFilterEventTypes)
        currQuickFilterEventTypes.add(type)
        quickFilterEventTypes = currQuickFilterEventTypes
    }

    var caldavSync: Boolean
        get() = prefs.getBoolean(CALDAV_SYNC, false)
        set(caldavSync) {
            context.scheduleEveCalSync(caldavSync)
            prefs.edit().putBoolean(CALDAV_SYNC, caldavSync).apply()
        }

    var caldavSyncedCalendarIds: String
        get() = prefs.getString(CALDAV_SYNCED_CALENDAR_IDS, "")!!
        set(calendarIDs) = prefs.edit().putString(CALDAV_SYNCED_CALENDAR_IDS, calendarIDs).apply()

    var lastUsedCaldavCalendarId: Int
        get() = prefs.getInt(LAST_USED_CALDAV_CALENDAR, getSyncedCalendarIdsAsList().first().toInt())
        set(calendarId) = prefs.edit().putInt(LAST_USED_CALDAV_CALENDAR, calendarId).apply()

    var lastUsedLocalEventTypeId: Long
        get() = prefs.getLong(LAST_USED_LOCAL_EVENT_TYPE_ID, REGULAR_EVENT_TYPE_ID)
        set(lastUsedLocalEventTypeId) = prefs.edit().putLong(LAST_USED_LOCAL_EVENT_TYPE_ID, lastUsedLocalEventTypeId).apply()

    var reminderAudioStream: Int
        get() = prefs.getInt(REMINDER_AUDIO_STREAM, AudioManager.STREAM_ALARM)
        set(reminderAudioStream) = prefs.edit().putInt(REMINDER_AUDIO_STREAM, reminderAudioStream).apply()

    var replaceDescription: Boolean
        get() = prefs.getBoolean(REPLACE_DESCRIPTION, false)
        set(replaceDescription) = prefs.edit().putBoolean(REPLACE_DESCRIPTION, replaceDescription).apply()

    var loopReminders: Boolean
        get() = prefs.getBoolean(LOOP_REMINDERS, false)
        set(loopReminders) = prefs.edit().putBoolean(LOOP_REMINDERS, loopReminders).apply()

    var dimPastEvents: Boolean
        get() = prefs.getBoolean(DIM_PAST_EVENTS, true)
        set(dimPastEvents) = prefs.edit().putBoolean(DIM_PAST_EVENTS, dimPastEvents).apply()

    fun getSyncedCalendarIdsAsList() =
        caldavSyncedCalendarIds.split(",").filter { it.trim().isNotEmpty() }.map { Integer.parseInt(it) }.toMutableList() as ArrayList<Int>

    fun getDisplayEventTypesAsList() = displayEventTypes.map { it.toLong() }.toMutableList() as ArrayList<Long>

    fun addDisplayEventType(type: String) {
        addDisplayEventTypes(HashSet(listOf(type)))
    }

    private fun addDisplayEventTypes(types: Set<String>) {
        val currDisplayEventTypes = HashSet(displayEventTypes)
        currDisplayEventTypes.addAll(types)
        displayEventTypes = currDisplayEventTypes
    }

    var usePreviousEventReminders: Boolean
        get() = prefs.getBoolean(USE_PREVIOUS_EVENT_REMINDERS, true)
        set(usePreviousEventReminders) = prefs.edit().putBoolean(USE_PREVIOUS_EVENT_REMINDERS, usePreviousEventReminders).apply()

    var defaultReminder1: Int
        get() = prefs.getInt(DEFAULT_REMINDER_1, 10)
        set(defaultReminder1) = prefs.edit().putInt(DEFAULT_REMINDER_1, defaultReminder1).apply()

    var defaultReminder2: Int
        get() = prefs.getInt(DEFAULT_REMINDER_2, REMINDER_OFF)
        set(defaultReminder2) = prefs.edit().putInt(DEFAULT_REMINDER_2, defaultReminder2).apply()

    var defaultReminder3: Int
        get() = prefs.getInt(DEFAULT_REMINDER_3, REMINDER_OFF)
        set(defaultReminder3) = prefs.edit().putInt(DEFAULT_REMINDER_3, defaultReminder3).apply()

    var lastVibrateOnReminder: Boolean
        get() = prefs.getBoolean(LAST_VIBRATE_ON_REMINDER, context.config.vibrateOnReminder)
        set(lastVibrateOnReminder) = prefs.edit().putBoolean(LAST_VIBRATE_ON_REMINDER, lastVibrateOnReminder).apply()

    var defaultDuration: Int
        get() = prefs.getInt(DEFAULT_DURATION, 0)
        set(defaultDuration) = prefs.edit().putInt(DEFAULT_DURATION, defaultDuration).apply()

    var defaultEventTypeId: Long
        get() = prefs.getLong(DEFAULT_EVENT_TYPE_ID, -1L)
        set(defaultEventTypeId) = prefs.edit().putLong(DEFAULT_EVENT_TYPE_ID, defaultEventTypeId).apply()

    var allowChangingTimeZones: Boolean
        get() = prefs.getBoolean(ALLOW_CHANGING_TIME_ZONES, false)
        set(allowChangingTimeZones) = prefs.edit().putBoolean(ALLOW_CHANGING_TIME_ZONES, allowChangingTimeZones).apply()

    var weeklyViewItemHeightMultiplier: Float
        get() = prefs.getFloat(WEEKLY_VIEW_ITEM_HEIGHT_MULTIPLIER, 1f)
        set(weeklyViewItemHeightMultiplier) = prefs.edit().putFloat(WEEKLY_VIEW_ITEM_HEIGHT_MULTIPLIER, weeklyViewItemHeightMultiplier).apply()

    var weeklyViewDays: Int
        get() = prefs.getInt(WEEKLY_VIEW_DAYS, 7)
        set(weeklyViewDays) = prefs.edit().putInt(WEEKLY_VIEW_DAYS, weeklyViewDays).apply()

    var highlightWeekends: Boolean
        get() = prefs.getBoolean(HIGHLIGHT_WEEKENDS, false)
        set(highlightWeekends) = prefs.edit().putBoolean(HIGHLIGHT_WEEKENDS, highlightWeekends).apply()
}

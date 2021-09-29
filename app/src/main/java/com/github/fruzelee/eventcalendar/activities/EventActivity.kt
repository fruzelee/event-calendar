package com.github.fruzelee.eventcalendar.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract.Attendees
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.github.fruzelee.eventcalendar.R
import com.github.fruzelee.eventcalendar.databinding.ActivityEventBinding
import com.github.fruzelee.eventcalendar.dialogs.*
import com.github.fruzelee.eventcalendar.extensions.*
import com.github.fruzelee.eventcalendar.helpers.*
import com.github.fruzelee.eventcalendar.helpers.Formatter
import com.github.fruzelee.eventcalendar.models.*
import kotlinx.android.synthetic.main.activity_event.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class EventActivity : BaseActivity() {

    private lateinit var binding: ActivityEventBinding

    private var mIsAllDayEvent = false
    private var mReminder1Minutes = REMINDER_OFF
    private var mReminder2Minutes = REMINDER_OFF
    private var mReminder3Minutes = REMINDER_OFF
    private var mReminder1Type = REMINDER_NOTIFICATION
    private var mReminder2Type = REMINDER_NOTIFICATION
    private var mReminder3Type = REMINDER_NOTIFICATION
    private var mRepeatInterval = 0
    private var mRepeatLimit = 0L
    private var mRepeatRule = 0
    private var mEventTypeId = REGULAR_EVENT_TYPE_ID
    private var mDialogTheme = 0
    private var mEventOccurrenceTS = 0L
    private var mLastSavePromptTS = 0L
    private var mEventCalendarId = STORED_LOCALLY_ONLY
    private var mWasActivityInitialized = false
    private var mWasContactsPermissionChecked = false
    private var mWasCalendarChanged = false
    private var mAvailability = Attendees.AVAILABILITY_BUSY
    private var mStoredEventTypes = ArrayList<EventType>()
    private var mOriginalTimeZone = DateTimeZone.getDefault().id
    private var mOriginalStartTS = 0L
    private var mOriginalEndTS = 0L

    private lateinit var mEventStartDateTime: DateTime
    private lateinit var mEventEndDateTime: DateTime
    private lateinit var mEvent: Event

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_cross_vector)
        val intent = intent ?: return
        mDialogTheme = getDialogTheme()
        mWasContactsPermissionChecked = hasPermission(PERMISSION_READ_CONTACTS)

        val eventId = intent.getLongExtra(EVENT_ID, 0L)
        ensureBackgroundThread {
            mStoredEventTypes = eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>
            val event = eventsDB.getEventWithId(eventId)
            if (eventId != 0L && event == null) {
                finish()
                return@ensureBackgroundThread
            }

            val localEventType =
                mStoredEventTypes.firstOrNull { it.id == config.lastUsedLocalEventTypeId }
            runOnUiThread {
                if (!isDestroyed && !isFinishing) {
                    gotEvent(savedInstanceState, localEventType, event)
                }
            }
        }
    }

    private fun gotEvent(savedInstanceState: Bundle?, localEventType: EventType?, event: Event?) {
        if (localEventType == null || localEventType.caldavCalendarId != 0) {
            config.lastUsedLocalEventTypeId = REGULAR_EVENT_TYPE_ID
        }

        mEventTypeId =
            if (config.defaultEventTypeId == -1L) config.lastUsedLocalEventTypeId else config.defaultEventTypeId

        if (event != null) {
            mEvent = event
            mEventOccurrenceTS = intent.getLongExtra(EVENT_OCCURRENCE_TS, 0L)
            if (savedInstanceState == null) {
                setupEditEvent()
            }

            if (intent.getBooleanExtra(IS_DUPLICATE_INTENT, false)) {
                mEvent.id = null
                updateActionBarTitle(getString(R.string.new_event))
            } else {
                cancelNotification(mEvent.id!!)
            }
        } else {
            mEvent = Event(null)
            config.apply {
                mReminder1Minutes =
                    if (usePreviousEventReminders && lastEventReminderMinutes1 >= -1) lastEventReminderMinutes1 else defaultReminder1
                mReminder2Minutes =
                    if (usePreviousEventReminders && lastEventReminderMinutes2 >= -1) lastEventReminderMinutes2 else defaultReminder2
                mReminder3Minutes =
                    if (usePreviousEventReminders && lastEventReminderMinutes3 >= -1) lastEventReminderMinutes3 else defaultReminder3
            }

            if (savedInstanceState == null) {
                setupNewEvent()
            }
        }

        if (savedInstanceState == null) {
            updateTexts()
        }

        event_show_on_map.setOnClickListener { showOnMap() }
        event_start_date.setOnClickListener { setupStartDate() }
        event_start_time.setOnClickListener { setupStartTime() }
        event_end_date.setOnClickListener { setupEndDate() }
        event_end_time.setOnClickListener { setupEndTime() }
        event_time_zone.setOnClickListener { setupTimeZone() }

        event_all_day.setOnCheckedChangeListener { _, isChecked ->
            toggleAllDay(
                isChecked
            )
        }


        updateTextColors(event_scrollview)
        event_time_zone_image.beVisibleIf(config.allowChangingTimeZones)
        event_time_zone.beVisibleIf(config.allowChangingTimeZones)
        mWasActivityInitialized = true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_event, menu)
        if (mWasActivityInitialized) {
            menu.findItem(R.id.delete).isVisible = mEvent.id != null
            menu.findItem(R.id.duplicate).isVisible = mEvent.id != null
        }

        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save -> saveCurrentEvent()
            R.id.delete -> deleteEvent()
            R.id.duplicate -> duplicateEvent()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun getStartEndTimes(): Pair<Long, Long> {
        val offset = if (!config.allowChangingTimeZones || mEvent.getTimeZoneString()
                .equals(mOriginalTimeZone, true)
        ) {
            0
        } else {
            val original =
                if (mOriginalTimeZone.isEmpty()) DateTimeZone.getDefault().id else mOriginalTimeZone
            val millis = System.currentTimeMillis()
            (DateTimeZone.forID(mEvent.getTimeZoneString()).getOffset(millis) - DateTimeZone.forID(
                original
            ).getOffset(millis)) / 1000L
        }

        val newStartTS =
            mEventStartDateTime.withSecondOfMinute(0).withMillisOfSecond(0).seconds() - offset
        val newEndTS =
            mEventEndDateTime.withSecondOfMinute(0).withMillisOfSecond(0).seconds() - offset
        return Pair(newStartTS, newEndTS)
    }

    private fun getReminders(): ArrayList<Reminder> {
        var reminders = arrayListOf(
            Reminder(mReminder1Minutes, mReminder1Type),
            Reminder(mReminder2Minutes, mReminder2Type),
            Reminder(mReminder3Minutes, mReminder3Type)
        )
        reminders = reminders.filter { it.minutes != REMINDER_OFF }.sortedBy { it.minutes }
            .toMutableList() as ArrayList<Reminder>
        return reminders
    }

    private fun isEventChanged(): Boolean {
        var newStartTS: Long
        var newEndTS: Long
        getStartEndTimes().apply {
            newStartTS = first
            newEndTS = second
        }

        val hasTimeChanged = if (mOriginalStartTS == 0L) {
            mEvent.startTS != newStartTS || mEvent.endTS != newEndTS
        } else {
            mOriginalStartTS != newStartTS || mOriginalEndTS != newEndTS
        }

        val reminders = getReminders()
        if (event_title.text.toString() != mEvent.title ||
            event_location.text.toString() != mEvent.location ||
            event_description.text.toString() != mEvent.description ||
            event_time_zone.text != mEvent.getTimeZoneString() ||
            reminders != mEvent.getReminders() ||
            mRepeatInterval != mEvent.repeatInterval ||
            mRepeatRule != mEvent.repeatRule ||
            mEventTypeId != mEvent.eventType ||
            mWasCalendarChanged ||
            hasTimeChanged
        ) {
            return true
        }

        return false
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() - mLastSavePromptTS > SAVE_DISCARD_PROMPT_INTERVAL && isEventChanged()) {
            mLastSavePromptTS = System.currentTimeMillis()
            ConfirmationAdvancedDialog(
                this,
                "",
                R.string.save_before_closing,
                R.string.save,
                R.string.discard
            ) {
                if (it) {
                    saveCurrentEvent()
                } else {
                    super.onBackPressed()
                }
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!mWasActivityInitialized) {
            return
        }

        outState.apply {
            putSerializable(EVENT, mEvent)
            putLong(START_TS, mEventStartDateTime.seconds())
            putLong(END_TS, mEventEndDateTime.seconds())
            putString(TIME_ZONE, mEvent.timeZone)

            putInt(REMINDER_1_MINUTES, mReminder1Minutes)
            putInt(REMINDER_2_MINUTES, mReminder2Minutes)
            putInt(REMINDER_3_MINUTES, mReminder3Minutes)

            putInt(REMINDER_1_TYPE, mReminder1Type)
            putInt(REMINDER_2_TYPE, mReminder2Type)
            putInt(REMINDER_3_TYPE, mReminder3Type)

            putInt(REPEAT_INTERVAL, mRepeatInterval)
            putInt(REPEAT_RULE, mRepeatRule)
            putLong(REPEAT_LIMIT, mRepeatLimit)

            putInt(AVAILABILITY, mAvailability)

            putLong(EVENT_TYPE_ID, mEventTypeId)
            putInt(EVENT_CALENDAR_ID, mEventCalendarId)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (!savedInstanceState.containsKey(START_TS)) {
            finish()
            return
        }

        savedInstanceState.apply {
            mEvent = getSerializable(EVENT) as Event
            mEventStartDateTime = Formatter.getDateTimeFromTS(getLong(START_TS))
            mEventEndDateTime = Formatter.getDateTimeFromTS(getLong(END_TS))
            mEvent.timeZone = getString(TIME_ZONE) ?: TimeZone.getDefault().id

            mReminder1Minutes = getInt(REMINDER_1_MINUTES)
            mReminder2Minutes = getInt(REMINDER_2_MINUTES)
            mReminder3Minutes = getInt(REMINDER_3_MINUTES)

            mReminder1Type = getInt(REMINDER_1_TYPE)
            mReminder2Type = getInt(REMINDER_2_TYPE)
            mReminder3Type = getInt(REMINDER_3_TYPE)

            mAvailability = getInt(AVAILABILITY)

            mRepeatInterval = getInt(REPEAT_INTERVAL)
            mRepeatRule = getInt(REPEAT_RULE)
            mRepeatLimit = getLong(REPEAT_LIMIT)

            mEventTypeId = getLong(EVENT_TYPE_ID)
            mEventCalendarId = getInt(EVENT_CALENDAR_ID)
        }

        updateTexts()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == SELECT_TIME_ZONE_INTENT && resultCode == Activity.RESULT_OK && resultData?.hasExtra(
                TIME_ZONE
            ) == true
        ) {
            val timeZone = resultData.getSerializableExtra(TIME_ZONE) as MyTimeZone
            mEvent.timeZone = timeZone.zoneName
            updateTimeZoneText()
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun updateTexts() {
        updateStartTexts()
        updateEndTexts()
        updateTimeZoneText()
    }

    private fun setupEditEvent() {
        val realStart = if (mEventOccurrenceTS == 0L) mEvent.startTS else mEventOccurrenceTS
        val duration = mEvent.endTS - mEvent.startTS
        mOriginalStartTS = realStart
        mOriginalEndTS = realStart + duration

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        updateActionBarTitle(getString(R.string.edit_event))
        mOriginalTimeZone = mEvent.timeZone
        if (config.allowChangingTimeZones) {
            try {
                mEventStartDateTime = Formatter.getDateTimeFromTS(realStart)
                    .withZone(DateTimeZone.forID(mOriginalTimeZone))
                mEventEndDateTime = Formatter.getDateTimeFromTS(realStart + duration)
                    .withZone(DateTimeZone.forID(mOriginalTimeZone))
            } catch (e: Exception) {
                showErrorToast(e)
                mEventStartDateTime = Formatter.getDateTimeFromTS(realStart)
                mEventEndDateTime = Formatter.getDateTimeFromTS(realStart + duration)
            }
        } else {
            mEventStartDateTime = Formatter.getDateTimeFromTS(realStart)
            mEventEndDateTime = Formatter.getDateTimeFromTS(realStart + duration)
        }

        event_title.setText(mEvent.title)
        event_location.setText(mEvent.location)
        event_description.setText(mEvent.description)

        mReminder1Minutes = mEvent.reminder1Minutes
        mReminder2Minutes = mEvent.reminder2Minutes
        mReminder3Minutes = mEvent.reminder3Minutes
        mReminder1Type = mEvent.reminder1Type
        mReminder2Type = mEvent.reminder2Type
        mReminder3Type = mEvent.reminder3Type
        mRepeatInterval = mEvent.repeatInterval
        mRepeatLimit = mEvent.repeatLimit
        mRepeatRule = mEvent.repeatRule
        mEventTypeId = mEvent.eventType
        mEventCalendarId = mEvent.getEveCalCalendarId()
        mAvailability = mEvent.availability
    }

    private fun setupNewEvent() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        event_title.requestFocus()
        updateActionBarTitle(getString(R.string.new_event))
        if (config.defaultEventTypeId != -1L) {
            config.lastUsedCaldavCalendarId =
                mStoredEventTypes.firstOrNull { it.id == config.defaultEventTypeId }?.caldavCalendarId
                    ?: 0
        }

        val isLastCaldavCalendarOK = config.caldavSync && config.getSyncedCalendarIdsAsList()
            .contains(config.lastUsedCaldavCalendarId)
        mEventCalendarId =
            if (isLastCaldavCalendarOK) config.lastUsedCaldavCalendarId else STORED_LOCALLY_ONLY

        if (intent.action == Intent.ACTION_EDIT || intent.action == Intent.ACTION_INSERT) {
            val startTS = intent.getLongExtra("beginTime", System.currentTimeMillis()) / 1000L
            mEventStartDateTime = Formatter.getDateTimeFromTS(startTS)

            val endTS = intent.getLongExtra("endTime", System.currentTimeMillis()) / 1000L
            mEventEndDateTime = Formatter.getDateTimeFromTS(endTS)

            if (intent.getBooleanExtra("allDay", false)) {
                mEvent.flags = mEvent.flags or FLAG_ALL_DAY
                event_all_day.isChecked = true
                toggleAllDay(true)
            }

            event_title.setText(intent.getStringExtra("title"))
            event_location.setText(intent.getStringExtra("eventLocation"))
            event_description.setText(intent.getStringExtra("description"))
            if (event_description.value.isNotEmpty()) {
                event_description.movementMethod = LinkMovementMethod.getInstance()
            }
        } else {
            val startTS = intent.getLongExtra(NEW_EVENT_START_TS, 0L)
            val dateTime = Formatter.getDateTimeFromTS(startTS)
            mEventStartDateTime = dateTime

            val addMinutes = if (intent.getBooleanExtra(NEW_EVENT_SET_HOUR_DURATION, false)) {
                // if an event is created at 23:00 on the weekly view, make it end on 23:59 by default to avoid spanning across multiple days
                if (mEventStartDateTime.hourOfDay == 23) {
                    59
                } else {
                    60
                }
            } else {
                config.defaultDuration
            }
            mEventEndDateTime = mEventStartDateTime.plusMinutes(addMinutes)
        }
        addDefValuesToNewEvent()

    }

    private fun addDefValuesToNewEvent() {
        var newStartTS: Long
        var newEndTS: Long
        getStartEndTimes().apply {
            newStartTS = first
            newEndTS = second
        }

        mEvent.apply {
            startTS = newStartTS
            endTS = newEndTS
            reminder1Minutes = mReminder1Minutes
            reminder1Type = mReminder1Type
            reminder2Minutes = mReminder2Minutes
            reminder2Type = mReminder2Type
            reminder3Minutes = mReminder3Minutes
            reminder3Type = mReminder3Type
            eventType = mEventTypeId
        }
    }

    private fun resetTime() {
        if (mEventEndDateTime.isBefore(mEventStartDateTime) &&
            mEventStartDateTime.dayOfMonth() == mEventEndDateTime.dayOfMonth() &&
            mEventStartDateTime.monthOfYear() == mEventEndDateTime.monthOfYear()
        ) {

            mEventEndDateTime =
                mEventEndDateTime.withTime(
                    mEventStartDateTime.hourOfDay,
                    mEventStartDateTime.minuteOfHour,
                    mEventStartDateTime.secondOfMinute,
                    0
                )
            updateEndTimeText()
            checkStartEndValidity()
        }
    }

    private fun toggleAllDay(isChecked: Boolean) {
        mIsAllDayEvent = isChecked
        hideKeyboard()
        event_start_time.beGoneIf(isChecked)
        event_end_time.beGoneIf(isChecked)
        resetTime()
    }

    private fun deleteEvent() {
        if (mEvent.id == null) {
            return
        }

        DeleteEventDialog(this, arrayListOf(mEvent.id!!), mEvent.repeatInterval > 0) {
            ensureBackgroundThread {
                when (it) {
                    DELETE_SELECTED_OCCURRENCE -> eventsHelper.addEventRepetitionException(
                        mEvent.id!!,
                        mEventOccurrenceTS,
                        true
                    )
                    DELETE_FUTURE_OCCURRENCES -> eventsHelper.addEventRepeatLimit(
                        mEvent.id!!,
                        mEventOccurrenceTS
                    )
                    DELETE_ALL_OCCURRENCES -> eventsHelper.deleteEvent(mEvent.id!!, true)
                }

                runOnUiThread {
                    finish()
                }
            }
        }
    }

    private fun duplicateEvent() {
        // the activity has the singleTask launchMode to avoid some glitches, so finish it before relaunching
        finish()
        Intent(this, EventActivity::class.java).apply {
            putExtra(EVENT_ID, mEvent.id)
            putExtra(EVENT_OCCURRENCE_TS, mEventOccurrenceTS)
            putExtra(IS_DUPLICATE_INTENT, true)
            startActivity(this)
        }
    }

    private fun saveCurrentEvent() {
        if (config.wasAlarmWarningShown || (mReminder1Minutes == REMINDER_OFF && mReminder2Minutes == REMINDER_OFF && mReminder3Minutes == REMINDER_OFF)) {
            ensureBackgroundThread {
                saveEvent()
            }
        } else {
            ConfirmationDialog(
                this,
                messageId = R.string.reminder_warning,
                positive = R.string.ok,
                negative = 0
            ) {
                config.wasAlarmWarningShown = true
                ensureBackgroundThread {
                    saveEvent()
                }
            }
        }
    }

    private fun saveEvent() {
        val newTitle = event_title.value
        if (newTitle.isEmpty()) {
            toast(R.string.title_empty)
            runOnUiThread {
                event_title.requestFocus()
            }
            return
        }

        var newStartTS: Long
        var newEndTS: Long
        getStartEndTimes().apply {
            newStartTS = first
            newEndTS = second
        }

        if (newStartTS > newEndTS) {
            toast(R.string.end_before_start)
            return
        }

        val wasRepeatable = mEvent.repeatInterval > 0
        val oldSource = mEvent.source
        val newImportId = if (mEvent.id != null) {
            mEvent.importId
        } else {
            UUID.randomUUID().toString().replace("-", "") + System.currentTimeMillis().toString()
        }

        val newEventType =
            if (!config.caldavSync || config.lastUsedCaldavCalendarId == 0 || mEventCalendarId == STORED_LOCALLY_ONLY) {
                mEventTypeId
            } else {
                calDAVHelper.getEveCalCalendars("", true).firstOrNull { it.id == mEventCalendarId }
                    ?.apply {
                        if (!canWrite()) {
                            runOnUiThread {
                                toast(R.string.insufficient_permissions)
                            }
                            return
                        }
                    }

                eventsHelper.getEventTypeWithEveCalCalendarId(mEventCalendarId)?.id
                    ?: config.lastUsedLocalEventTypeId
            }

        val newSource = if (!config.caldavSync || mEventCalendarId == STORED_LOCALLY_ONLY) {
            config.lastUsedLocalEventTypeId = newEventType
            SOURCE_SIMPLE_CALENDAR
        } else {
            "$CALDAV-$mEventCalendarId"
        }

        val reminders = getReminders()
        if (!event_all_day.isChecked) {
            if (reminders.getOrNull(2)?.minutes ?: 0 < -1) {
                reminders.removeAt(2)
            }

            if (reminders.getOrNull(1)?.minutes ?: 0 < -1) {
                reminders.removeAt(1)
            }

            if (reminders.getOrNull(0)?.minutes ?: 0 < -1) {
                reminders.removeAt(0)
            }
        }

        val reminder1 = reminders.getOrNull(0) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)
        val reminder2 = reminders.getOrNull(1) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)
        val reminder3 = reminders.getOrNull(2) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)

        mReminder1Type =
            if (mEventCalendarId == STORED_LOCALLY_ONLY) REMINDER_NOTIFICATION else reminder1.type
        mReminder2Type =
            if (mEventCalendarId == STORED_LOCALLY_ONLY) REMINDER_NOTIFICATION else reminder2.type
        mReminder3Type =
            if (mEventCalendarId == STORED_LOCALLY_ONLY) REMINDER_NOTIFICATION else reminder3.type

        config.apply {
            if (usePreviousEventReminders) {
                lastEventReminderMinutes1 = reminder1.minutes
                lastEventReminderMinutes2 = reminder2.minutes
                lastEventReminderMinutes3 = reminder3.minutes
            }
        }

        mEvent.apply {
            startTS = newStartTS
            endTS = newEndTS
            title = newTitle
            description = event_description.value
            reminder1Minutes = reminder1.minutes
            reminder2Minutes = reminder2.minutes
            reminder3Minutes = reminder3.minutes
            reminder1Type = mReminder1Type
            reminder2Type = mReminder2Type
            reminder3Type = mReminder3Type
            repeatInterval = mRepeatInterval
            importId = newImportId
            timeZone = if (mEvent.timeZone.isEmpty()) TimeZone.getDefault().id else timeZone
            flags = mEvent.flags.addBitIf(event_all_day.isChecked, FLAG_ALL_DAY)
            repeatLimit = if (repeatInterval == 0) 0 else mRepeatLimit
            repeatRule = mRepeatRule
            eventType = newEventType
            lastUpdated = System.currentTimeMillis()
            source = newSource
            location = event_location.value
            availability = mAvailability
        }

        // recreate the event if it was moved in a different EveCal calendar
        if (mEvent.id != null && oldSource != newSource) {
            eventsHelper.deleteEvent(mEvent.id!!, true)
            mEvent.id = null
        }
        storeEvent(wasRepeatable)
    }

    private fun storeEvent(wasRepeatable: Boolean) {
        if (mEvent.id == null || mEvent.id == null) {
            eventsHelper.insertEvent(mEvent, addToEveCal = true, showToasts = true) {
                if (DateTime.now().isAfter(mEventStartDateTime.millis)) {
                    if (mEvent.repeatInterval == 0 && mEvent.getReminders()
                            .any { it.type == REMINDER_NOTIFICATION }
                    ) {
                        notifyEvent(mEvent)
                    }
                }

                finish()
            }
        } else {
            if (mRepeatInterval > 0 && wasRepeatable) {
                runOnUiThread {

                }
            } else {
                eventsHelper.updateEvent(mEvent, updateAtEveCal = true, showToasts = true) {
                    finish()
                }
            }
        }
    }


    private fun updateStartTexts() {
        updateStartDateText()
        updateStartTimeText()
    }

    private fun updateStartDateText() {
        event_start_date.text = Formatter.getDate(applicationContext, mEventStartDateTime)
        checkStartEndValidity()
    }

    private fun updateStartTimeText() {
        event_start_time.text = Formatter.getTime(this, mEventStartDateTime)
        checkStartEndValidity()
    }

    private fun updateEndTexts() {
        updateEndDateText()
        updateEndTimeText()
    }

    private fun updateEndDateText() {
        event_end_date.text = Formatter.getDate(applicationContext, mEventEndDateTime)
        checkStartEndValidity()
    }

    private fun updateEndTimeText() {
        event_end_time.text = Formatter.getTime(this, mEventEndDateTime)
        checkStartEndValidity()
    }

    private fun updateTimeZoneText() {
        event_time_zone.text = mEvent.getTimeZoneString()
    }

    private fun checkStartEndValidity() {
        val textColor =
            if (mEventStartDateTime.isAfter(mEventEndDateTime)) ContextCompat.getColor(
                this,
                R.color.red_text
            ) else config.textColor
        event_end_date.setTextColor(textColor)
        event_end_time.setTextColor(textColor)
    }

    private fun showOnMap() {
        if (event_location.value.isEmpty()) {
            toast(R.string.please_fill_location)
            return
        }

        val pattern = Pattern.compile(LAT_LON_PATTERN)
        val locationValue = event_location.value
        val uri = if (pattern.matcher(locationValue).find()) {
            val delimiter = if (locationValue.contains(';')) ";" else ","
            val parts = locationValue.split(delimiter)
            val latitude = parts.first()
            val longitude = parts.last()
            Uri.parse("geo:$latitude,$longitude")
        } else {
            val location = Uri.encode(locationValue)
            Uri.parse("geo:0,0?q=$location")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri)
        launchActivityIntent(intent)
    }

    private fun setupStartDate() {
        hideKeyboard()
        config.backgroundColor.getContrastColor()
        val datePicker = DatePickerDialog(
            this,
            mDialogTheme,
            startDateSetListener,
            mEventStartDateTime.year,
            mEventStartDateTime.monthOfYear - 1,
            mEventStartDateTime.dayOfMonth
        )

        datePicker.datePicker.firstDayOfWeek =
            if (config.isSundayFirst) Calendar.SUNDAY else Calendar.MONDAY
        datePicker.show()
    }

    private fun setupStartTime() {
        hideKeyboard()
        TimePickerDialog(
            this,
            mDialogTheme,
            startTimeSetListener,
            mEventStartDateTime.hourOfDay,
            mEventStartDateTime.minuteOfHour,
            config.use24HourFormat
        ).show()
    }

    private fun setupEndDate() {
        hideKeyboard()
        val datePicker = DatePickerDialog(
            this,
            mDialogTheme,
            endDateSetListener,
            mEventEndDateTime.year,
            mEventEndDateTime.monthOfYear - 1,
            mEventEndDateTime.dayOfMonth
        )

        datePicker.datePicker.firstDayOfWeek =
            if (config.isSundayFirst) Calendar.SUNDAY else Calendar.MONDAY
        datePicker.show()
    }

    private fun setupEndTime() {
        hideKeyboard()
        TimePickerDialog(
            this,
            mDialogTheme,
            endTimeSetListener,
            mEventEndDateTime.hourOfDay,
            mEventEndDateTime.minuteOfHour,
            config.use24HourFormat
        ).show()
    }

    private val startDateSetListener =
        DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            dateSet(year, monthOfYear, dayOfMonth, true)
        }

    private val startTimeSetListener =
        TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            timeSet(hourOfDay, minute, true)
        }

    private val endDateSetListener =
        DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            dateSet(
                year,
                monthOfYear,
                dayOfMonth,
                false
            )
        }

    private val endTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
        timeSet(
            hourOfDay,
            minute,
            false
        )
    }

    private fun dateSet(year: Int, month: Int, day: Int, isStart: Boolean) {
        if (isStart) {
            val diff = mEventEndDateTime.seconds() - mEventStartDateTime.seconds()

            mEventStartDateTime = mEventStartDateTime.withDate(year, month + 1, day)
            updateStartDateText()

            mEventEndDateTime = mEventStartDateTime.plusSeconds(diff.toInt())
            updateEndTexts()
        } else {
            mEventEndDateTime = mEventEndDateTime.withDate(year, month + 1, day)
            updateEndDateText()
        }
    }

    private fun timeSet(hours: Int, minutes: Int, isStart: Boolean) {
        try {
            if (isStart) {
                val diff = mEventEndDateTime.seconds() - mEventStartDateTime.seconds()

                mEventStartDateTime =
                    mEventStartDateTime.withHourOfDay(hours).withMinuteOfHour(minutes)
                updateStartTimeText()

                mEventEndDateTime = mEventStartDateTime.plusSeconds(diff.toInt())
                updateEndTexts()
            } else {
                mEventEndDateTime = mEventEndDateTime.withHourOfDay(hours).withMinuteOfHour(minutes)
                updateEndTimeText()
            }
        } catch (e: Exception) {
            timeSet(hours + 1, minutes, isStart)
            return
        }
    }

    @Suppress("DEPRECATION")
    private fun setupTimeZone() {
        Intent(this, SelectTimeZoneActivity::class.java).apply {
            putExtra(CURRENT_TIME_ZONE, mEvent.getTimeZoneString())
            startActivityForResult(this, SELECT_TIME_ZONE_INTENT)
        }
    }

    companion object {
        private const val EVENT = "EVENT"
        private const val START_TS = "START_TS"
        private const val END_TS = "END_TS"
        private const val REMINDER_1_MINUTES = "REMINDER_1_MINUTES"
        private const val REMINDER_2_MINUTES = "REMINDER_2_MINUTES"
        private const val REMINDER_3_MINUTES = "REMINDER_3_MINUTES"
        private const val REMINDER_1_TYPE = "REMINDER_1_TYPE"
        private const val REMINDER_2_TYPE = "REMINDER_2_TYPE"
        private const val REMINDER_3_TYPE = "REMINDER_3_TYPE"
        private const val REPEAT_INTERVAL = "REPEAT_INTERVAL"
        private const val REPEAT_LIMIT = "REPEAT_LIMIT"
        private const val REPEAT_RULE = "REPEAT_RULE"
        private const val AVAILABILITY = "AVAILABILITY"
        private const val EVENT_TYPE_ID = "EVENT_TYPE_ID"
        private const val EVENT_CALENDAR_ID = "EVENT_CALENDAR_ID"
        private const val SELECT_TIME_ZONE_INTENT = 1
        private const val LAT_LON_PATTERN =
            "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)([,;])\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$"
    }

}

package com.github.fruzelee.eventcalendar.helpers

import android.os.Build
import android.os.Looper

const val STORED_LOCALLY_ONLY = 0

const val DAY_CODE = "day_code"
const val EVENT_ID = "event_id"
const val IS_DUPLICATE_INTENT = "is_duplicate_intent"
const val EVENT_OCCURRENCE_TS = "event_occurrence_ts"
const val NEW_EVENT_START_TS = "new_event_start_ts"
const val WEEK_START_TIMESTAMP = "week_start_timestamp"
const val NEW_EVENT_SET_HOUR_DURATION = "new_event_set_hour_duration"
const val WEEK_START_DATE_TIME = "week_start_date_time"
const val CALDAV = "Caldav"
const val VIEW_TO_OPEN = "view_to_open"
const val REGULAR_EVENT_TYPE_ID = 1L
const val TIME_ZONE = "time_zone"
const val CURRENT_TIME_ZONE = "current_time_zone"

const val WEEKLY_VIEW = 1
const val LAST_VIEW = 2

const val REMINDER_OFF = -1

const val DAY = 86400
const val WEEK = 604800
const val MONTH =
    2592001    // exact value not taken into account, JoDa is used for adding months and years
const val YEAR = 31536000

// Shared Preferences
const val START_WEEKLY_AT = "start_weekly_at"
const val SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP = "show_midnight_spanning_events_at_top"
const val VIBRATE = "vibrate"
const val REMINDER_SOUND_URI = "reminder_sound_uri"
const val VIEW = "view"
const val LAST_EVENT_REMINDER_MINUTES = "reminder_minutes"
const val LAST_EVENT_REMINDER_MINUTES_2 = "reminder_minutes_2"
const val LAST_EVENT_REMINDER_MINUTES_3 = "reminder_minutes_3"
const val DISPLAY_EVENT_TYPES = "display_event_types"
const val QUICK_FILTER_EVENT_TYPES = "quick_filter_event_types"
const val CALDAV_SYNC = "caldav_sync"
const val CALDAV_SYNCED_CALENDAR_IDS = "caldav_synced_calendar_ids"
const val LAST_USED_CALDAV_CALENDAR = "last_used_caldav_calendar"
const val LAST_USED_LOCAL_EVENT_TYPE_ID = "last_used_local_event_type_id"
const val REPLACE_DESCRIPTION = "replace_description"
const val LOOP_REMINDERS = "loop_reminders"
const val DIM_PAST_EVENTS = "dim_past_events"
const val LAST_SOUND_URI = "last_sound_uri"
const val LAST_REMINDER_CHANNEL_ID = "last_reminder_channel_ID"
const val REMINDER_AUDIO_STREAM = "reminder_audio_stream"
const val USE_PREVIOUS_EVENT_REMINDERS = "use_previous_event_reminders"
const val DEFAULT_REMINDER_1 = "default_reminder_1"
const val DEFAULT_REMINDER_2 = "default_reminder_2"
const val DEFAULT_REMINDER_3 = "default_reminder_3"
const val LAST_VIBRATE_ON_REMINDER = "last_vibrate_on_reminder"
const val DEFAULT_DURATION = "default_duration"
const val DEFAULT_EVENT_TYPE_ID = "default_event_type_id"
const val ALLOW_CHANGING_TIME_ZONES = "allow_changing_time_zones"
const val WEEKLY_VIEW_ITEM_HEIGHT_MULTIPLIER = "weekly_view_item_height_multiplier"
const val WEEKLY_VIEW_DAYS = "weekly_view_days"
const val HIGHLIGHT_WEEKENDS = "highlight_weekends"

// repeat_rule for monthly and yearly repetition
const val REPEAT_SAME_DAY =
    1                           // i.e. 25th every month, or 3rd june (if yearly repetition)
const val REPEAT_ORDER_WEEKDAY_USE_LAST =
    2             // i.e. every last sunday. 4th if a month has 4 sundays, 5th if 5 (or last sunday in june, if yearly)
const val REPEAT_LAST_DAY = 3                           // i.e. every last day of the month
const val REPEAT_ORDER_WEEKDAY =
    4                      // i.e. every 4th sunday, even if a month has 4 sundays only (will stay 4th even at months with 5)

// special event flags
const val FLAG_ALL_DAY = 1
const val FLAG_IS_PAST_EVENT = 2
const val FLAG_MISSING_YEAR = 4

const val BY_DAY = "BY_DAY"
const val BY_MONTH_DAY = "BY_MONTH_DAY"
const val BY_MONTH = "BY_MONTH"

const val FREQ = "FREQ"
const val UNTIL = "UNTIL"
const val COUNT = "COUNT"
const val INTERVAL = "INTERVAL"

const val DAILY = "DAILY"
const val WEEKLY = "WEEKLY"
const val MONTHLY = "MONTHLY"
const val YEARLY = "YEARLY"

const val MO = "MO"
const val TU = "TU"
const val WE = "WE"
const val TH = "TH"
const val FR = "FR"
const val SA = "SA"
const val SU = "SU"

const val SOURCE_SIMPLE_CALENDAR = "event-calendar"

const val DELETE_SELECTED_OCCURRENCE = 0
const val DELETE_FUTURE_OCCURRENCES = 1
const val DELETE_ALL_OCCURRENCES = 2

const val REMINDER_NOTIFICATION = 0
const val REMINDER_EMAIL = 1

fun getNowSeconds() = System.currentTimeMillis() / 1000L

fun isWeekend(i: Int, isSundayFirst: Boolean): Boolean {
    return if (isSundayFirst) {
        i == 0 || i == 6 || i == 7 || i == 13
    } else {
        i == 5 || i == 6 || i == 12 || i == 13
    }
}

const val APP_ID = "app_id"
const val CHOPPED_LIST_DEFAULT_SIZE = 50
const val SAVE_DISCARD_PROMPT_INTERVAL = 1000L
const val DARK_GREY = 0xFF333333.toInt()

const val LOWER_ALPHA = 0.25f
const val MEDIUM_ALPHA = 0.5f
const val HIGHER_ALPHA = 0.75f

const val HOUR_MINUTES = 60
const val DAY_MINUTES = 24 * HOUR_MINUTES
const val WEEK_MINUTES = DAY_MINUTES * 7
const val MONTH_MINUTES = DAY_MINUTES * 30
const val YEAR_MINUTES = DAY_MINUTES * 365

const val MINUTE_SECONDS = 60
const val HOUR_SECONDS = HOUR_MINUTES * 60
const val DAY_SECONDS = DAY_MINUTES * 60
const val WEEK_SECONDS = WEEK_MINUTES * 60
const val MONTH_SECONDS = MONTH_MINUTES * 60
const val YEAR_SECONDS = YEAR_MINUTES * 60

// shared preferences
const val PREFS_KEY = "Prefs"
const val TREE_URI = "tree_uri_2"
const val OTG_TREE_URI = "otg_tree_uri_2"
const val OTG_REAL_PATH = "otg_real_path_2"
const val TEXT_COLOR = "text_color"
const val BACKGROUND_COLOR = "background_color"
const val PRIMARY_COLOR = "primary_color_2"
const val ACCENT_COLOR = "accent_color"
const val USE_ENGLISH = "use_english"
const val WAS_USE_ENGLISH_TOGGLED = "was_use_english_toggled"
const val USE_24_HOUR_FORMAT = "use_24_hour_format"
const val SUNDAY_FIRST = "sunday_first"
const val WAS_ALARM_WARNING_SHOWN = "was_alarm_warning_shown"
const val USE_SAME_SNOOZE = "use_same_snooze"
const val SNOOZE_TIME = "snooze_delay"
const val SILENT = "silent"
const val OTG_PARTITION = "otg_partition_2"
const val LAST_EXPORTED_SETTINGS_FILE = "last_exported_settings_file"

// permissions
const val PERMISSION_READ_STORAGE = 1
const val PERMISSION_WRITE_STORAGE = 2
const val PERMISSION_CAMERA = 3
const val PERMISSION_RECORD_AUDIO = 4
const val PERMISSION_READ_CONTACTS = 5
const val PERMISSION_WRITE_CONTACTS = 6
const val PERMISSION_READ_CALENDAR = 7
const val PERMISSION_WRITE_CALENDAR = 8
const val PERMISSION_CALL_PHONE = 9
const val PERMISSION_READ_CALL_LOG = 10
const val PERMISSION_WRITE_CALL_LOG = 11
const val PERMISSION_GET_ACCOUNTS = 12
const val PERMISSION_READ_SMS = 13
const val PERMISSION_SEND_SMS = 14
const val PERMISSION_READ_PHONE_STATE = 15

const val MONDAY_BIT = 1
const val TUESDAY_BIT = 2
const val WEDNESDAY_BIT = 4
const val THURSDAY_BIT = 8
const val FRIDAY_BIT = 16
const val SATURDAY_BIT = 32
const val SUNDAY_BIT = 64

fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()

fun ensureBackgroundThread(callback: () -> Unit) {
    if (isOnMainThread()) {
        Thread {
            callback()
        }.start()
    } else {
        callback()
    }
}

fun isNougatPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
fun isOreoPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
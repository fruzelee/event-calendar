package com.github.fruzelee.eventcalendar.extensions

import android.Manifest
import android.accounts.Account
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.Point
import android.media.*
import android.net.Uri
import android.os.*
import android.provider.*
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.github.fruzelee.eventcalendar.R
import com.github.fruzelee.eventcalendar.activities.EventActivity
import com.github.fruzelee.eventcalendar.activities.SnoozeReminderActivity
import com.github.fruzelee.eventcalendar.databases.EventsDatabase
import com.github.fruzelee.eventcalendar.helpers.*
import com.github.fruzelee.eventcalendar.helpers.Formatter
import com.github.fruzelee.eventcalendar.interfaces.EventTypesDao
import com.github.fruzelee.eventcalendar.interfaces.EventsDao
import com.github.fruzelee.eventcalendar.models.*
import com.github.fruzelee.eventcalendar.receivers.EveCalSyncReceiver
import com.github.fruzelee.eventcalendar.receivers.NotificationReceiver
import com.github.fruzelee.eventcalendar.services.SnoozeService
import com.github.fruzelee.eventcalendar.views.*
import org.joda.time.LocalDate
import java.util.*

val Context.config: Config get() = Config.newInstance(applicationContext)
val Context.eventsDB: EventsDao get() = EventsDatabase.getInstance(applicationContext).EventsDao()
val Context.eventTypesDB: EventTypesDao
    get() = EventsDatabase.getInstance(applicationContext).EventTypesDao()
val Context.eventsHelper: EventsHelper get() = EventsHelper(this)
val Context.calDAVHelper: EveCalHelper get() = EveCalHelper(this)

fun Context.scheduleAllEvents() {
    val events = eventsDB.getEventsAtReboot(getNowSeconds())
    events.forEach {
        scheduleNextEventReminder(it, false)
    }
}

fun Context.scheduleNextEventReminder(event: Event, showToasts: Boolean) {
    val validReminders = event.getReminders().filter { it.type == REMINDER_NOTIFICATION }
    if (validReminders.isEmpty()) {
        if (showToasts) {
            toast(R.string.saving)
        }
        return
    }

    val now = getNowSeconds()
    val reminderSeconds = validReminders.reversed().map { it.minutes * 60 }
    eventsHelper.getEvents(now, now + YEAR, event.id!!, false) {
        if (it.isNotEmpty()) {
            for (curEvent in it) {
                for (curReminder in reminderSeconds) {
                    if (curEvent.getEventStartTS() - curReminder > now) {
                        scheduleEventIn(
                            (curEvent.getEventStartTS() - curReminder) * 1000L,
                            curEvent,
                            showToasts
                        )
                        return@getEvents
                    }
                }
            }
        }

        if (showToasts) {
            toast(R.string.saving)
        }
    }
}

fun Context.scheduleEventIn(notifyTS: Long, event: Event, showToasts: Boolean) {
    if (notifyTS < System.currentTimeMillis()) {
        if (showToasts) {
            toast(R.string.saving)
        }
        return
    }

    val newNotifyTS = notifyTS + 1000
    if (showToasts) {
        val secondsTillNotification = (newNotifyTS - System.currentTimeMillis()) / 1000
        val msg = String.format(
            getString(R.string.reminder_triggers_in),
            formatSecondsToTimeString(secondsTillNotification.toInt())
        )
        toast(msg)
    }

    val pendingIntent = getNotificationIntent(event)
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    try {
        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            newNotifyTS,
            pendingIntent
        )
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

fun Context.formatSecondsToTimeString(totalSeconds: Int): String {
    val days = totalSeconds / DAY_SECONDS
    val hours = (totalSeconds % DAY_SECONDS) / HOUR_SECONDS
    val minutes = (totalSeconds % HOUR_SECONDS) / MINUTE_SECONDS
    val seconds = totalSeconds % MINUTE_SECONDS
    val timesString = StringBuilder()
    if (days > 0) {
        val daysString = String.format(resources.getQuantityString(R.plurals.days, days, days))
        timesString.append("$daysString, ")
    }

    if (hours > 0) {
        val hoursString = String.format(resources.getQuantityString(R.plurals.hours, hours, hours))
        timesString.append("$hoursString, ")
    }

    if (minutes > 0) {
        val minutesString =
            String.format(resources.getQuantityString(R.plurals.minutes, minutes, minutes))
        timesString.append("$minutesString, ")
    }

    if (seconds > 0) {
        val secondsString =
            String.format(resources.getQuantityString(R.plurals.seconds, seconds, seconds))
        timesString.append(secondsString)
    }

    var result = timesString.toString().trim().trimEnd(',')
    if (result.isEmpty()) {
        result = String.format(resources.getQuantityString(R.plurals.minutes, 0, 0))
    }
    return result
}

// hide the actual notification from the top bar
fun Context.cancelNotification(id: Long) {
    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id.toInt())
}

@SuppressLint("UnspecifiedImmutableFlag")
fun Context.getNotificationIntent(event: Event): PendingIntent {
    val intent = Intent(this, NotificationReceiver::class.java)
    intent.putExtra(EVENT_ID, event.id)
    intent.putExtra(EVENT_OCCURRENCE_TS, event.startTS)
    return PendingIntent.getBroadcast(
        this,
        event.id!!.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
}

@SuppressLint("UnspecifiedImmutableFlag")
fun Context.cancelPendingIntent(id: Long) {
    val intent = Intent(this, NotificationReceiver::class.java)
    PendingIntent.getBroadcast(this, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT).cancel()
}

fun Context.notifyRunningEvents() {
    eventsHelper.getRunningEvents()
        .filter { it -> it.getReminders().any { it.type == REMINDER_NOTIFICATION } }.forEach {
            notifyEvent(it)
        }
}

fun Context.notifyEvent(originalEvent: Event) {
    var event = originalEvent.copy()
    val currentSeconds = getNowSeconds()

    var eventStartTS =
        if (event.getIsAllDay()) Formatter.getDayStartTS(Formatter.getDayCodeFromTS(event.startTS)) else event.startTS
    // make sure refer to the proper repeatable event instance with "Tomorrow", or the specific date
    if (event.repeatInterval != 0 && eventStartTS - event.reminder1Minutes * 60 < currentSeconds) {
        val events = eventsHelper.getRepeatableEventsFor(
            currentSeconds - WEEK_SECONDS,
            currentSeconds + YEAR_SECONDS,
            event.id!!
        )
        for (currEvent in events) {
            eventStartTS = if (currEvent.getIsAllDay()) Formatter.getDayStartTS(
                Formatter.getDayCodeFromTS(currEvent.startTS)
            ) else currEvent.startTS
            if (eventStartTS - currEvent.reminder1Minutes * 60 > currentSeconds) {
                break
            }

            event = currEvent
        }
    }

    val pendingIntent = getPendingIntent(applicationContext, event)
    val startTime = Formatter.getTimeFromTS(applicationContext, event.startTS)
    val endTime = Formatter.getTimeFromTS(applicationContext, event.endTS)

    val displayedStartDate = when (Formatter.getDateFromTS(event.startTS)) {
        LocalDate.now() -> ""
        LocalDate.now().plusDays(1) -> getString(R.string.tomorrow)
        else -> "${Formatter.getDateFromCode(this, Formatter.getDayCodeFromTS(event.startTS))},"
    }

    val timeRange = if (event.getIsAllDay()) getString(R.string.all_day) else getFormattedEventTime(
        startTime,
        endTime
    )
    val descriptionOrLocation = if (config.replaceDescription) event.location else event.description
    val content = "$displayedStartDate $timeRange $descriptionOrLocation".trim()
    ensureBackgroundThread {
        val notification = getNotification(pendingIntent, event, content)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            if (notification != null) {
                notificationManager.notify(event.id!!.toInt(), notification)
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }
}

@SuppressLint("NewApi")
fun Context.getNotification(
    pendingIntent: PendingIntent,
    event: Event,
    content: String,
    publicVersion: Boolean = false
): Notification? {
    var soundUri = config.reminderSoundUri
    if (soundUri == SILENT) {
        soundUri = ""
    } else {
        grantReadUriPermission(soundUri)
    }

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    // create a new channel for every new sound uri as the new Android Oreo notification system is fundamentally broken
    if (soundUri != config.lastSoundUri || config.lastVibrateOnReminder != config.vibrateOnReminder) {
        if (!publicVersion) {
            if (isOreoPlus()) {
                val oldChannelId =
                    "simple_calendar_${config.lastReminderChannel}_${config.reminderAudioStream}_${event.eventType}"
                notificationManager.deleteNotificationChannel(oldChannelId)
            }
        }

        config.lastVibrateOnReminder = config.vibrateOnReminder
        config.lastReminderChannel = System.currentTimeMillis()
        config.lastSoundUri = soundUri
    }

    val channelId =
        "simple_calendar_${config.lastReminderChannel}_${config.reminderAudioStream}_${event.eventType}"
    if (isOreoPlus()) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setLegacyStreamType(config.reminderAudioStream)
            .build()

        val name = eventTypesDB.getEventTypeWithId(event.eventType)?.getDisplayTitle()
        val importance = NotificationManager.IMPORTANCE_HIGH
        NotificationChannel(channelId, name, importance).apply {
            enableLights(true)
            lightColor = event.color
            enableVibration(config.vibrateOnReminder)
            setSound(Uri.parse(soundUri), audioAttributes)
            try {
                notificationManager.createNotificationChannel(this)
            } catch (e: Exception) {
                showErrorToast(e)
                return null
            }
        }
    }

    val contentTitle = if (publicVersion) resources.getString(R.string.app_name) else event.title
    val contentText =
        if (publicVersion) resources.getString(R.string.public_event_notification_text) else content

    val builder = NotificationCompat.Builder(this, channelId)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setSmallIcon(R.drawable.ic_calendar_vector)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setDefaults(Notification.DEFAULT_LIGHTS)
        .setCategory(Notification.CATEGORY_EVENT)
        .setAutoCancel(true)
        .setSound(Uri.parse(soundUri), config.reminderAudioStream)
        .setChannelId(channelId)
        .addAction(
            R.drawable.ic_snooze_vector,
            getString(R.string.snooze),
            getSnoozePendingIntent(this, event)
        )

    if (config.vibrateOnReminder) {
        val vibrateArray = LongArray(2) { 500 }
        builder.setVibrate(vibrateArray)
    }

    if (!publicVersion) {
        val notification = getNotification(pendingIntent, event, content, true)
        if (notification != null) {
            builder.setPublicVersion(notification)
        }
    }

    val notification = builder.build()
    if (config.loopReminders) {
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
    }
    return notification
}

private fun getFormattedEventTime(startTime: String, endTime: String) =
    if (startTime == endTime) startTime else "$startTime \u2013 $endTime"

@SuppressLint("UnspecifiedImmutableFlag")
private fun getPendingIntent(context: Context, event: Event): PendingIntent {
    val intent = Intent(context, EventActivity::class.java)
    intent.putExtra(EVENT_ID, event.id)
    intent.putExtra(EVENT_OCCURRENCE_TS, event.startTS)
    return PendingIntent.getActivity(
        context,
        event.id!!.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
}

@SuppressLint("UnspecifiedImmutableFlag")
private fun getSnoozePendingIntent(context: Context, event: Event): PendingIntent {
    val snoozeClass =
        if (context.config.useSameSnooze) SnoozeService::class.java else SnoozeReminderActivity::class.java
    val intent = Intent(context, snoozeClass).setAction("Snooze")
    intent.putExtra(EVENT_ID, event.id)
    return if (context.config.useSameSnooze) {
        PendingIntent.getService(
            context,
            event.id!!.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    } else {
        PendingIntent.getActivity(
            context,
            event.id!!.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

fun Context.rescheduleReminder(event: Event?, minutes: Int) {
    if (event != null) {
        cancelPendingIntent(event.id!!)
        applicationContext.scheduleEventIn(
            System.currentTimeMillis() + minutes * 60000,
            event,
            false
        )
        cancelNotification(event.id!!)
    }
}

fun Context.recheckEveCalCalendars(callback: () -> Unit) {
    if (config.caldavSync) {
        ensureBackgroundThread {
            calDAVHelper.refreshCalendars(false, callback)
        }
    }
}

@SuppressLint("UnspecifiedImmutableFlag")
fun Context.scheduleEveCalSync(activate: Boolean) {
    val syncIntent = Intent(applicationContext, EveCalSyncReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        0,
        syncIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
    val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (activate) {
        val syncCheckInterval = 2 * AlarmManager.INTERVAL_HOUR
        try {
            alarm.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + syncCheckInterval,
                syncCheckInterval,
                pendingIntent
            )
        } catch (ignored: Exception) {
        }
    } else {
        alarm.cancel(pendingIntent)
    }
}

fun Context.refreshEveCalCalendars(ids: String, showToasts: Boolean) {
    val uri = CalendarContract.Calendars.CONTENT_URI
    val accounts = HashSet<Account>()
    val calendars = calDAVHelper.getEveCalCalendars(ids, showToasts)
    calendars.forEach {
        accounts.add(Account(it.accountName, it.accountType))
    }

    Bundle().apply {
        putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
        accounts.forEach {
            ContentResolver.requestSync(it, uri.authority, this)
        }
    }
}

fun Context.getWeeklyViewItemHeight(): Float {
    val defaultHeight = resources.getDimension(R.dimen.weekly_view_row_height)
    val multiplier = config.weeklyViewItemHeightMultiplier
    return defaultHeight * multiplier
}

fun Context.getSharedPrefs(): SharedPreferences =
    getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

fun Context.updateTextColors(viewGroup: ViewGroup, tmpTextColor: Int = 0, tmpAccentColor: Int = 0) {
    val textColor = if (tmpTextColor == 0) baseConfig.textColor else tmpTextColor
    baseConfig.backgroundColor
    val accentColor = if (tmpAccentColor == 0) {
        when {
            isWhiteTheme() || isBlackAndWhiteTheme() -> baseConfig.accentColor
            else -> baseConfig.primaryColor
        }
    } else {
        tmpAccentColor
    }

    val cnt = viewGroup.childCount
    (0 until cnt).map { viewGroup.getChildAt(it) }.forEach {
        when (it) {
            is MyTextView -> it.setColors(textColor, accentColor)
            is MySwitchCompat -> it.setColors(textColor, accentColor)
            is MyCompatRadioButton -> it.setColors(textColor, accentColor)
            is MyEditText -> it.setColors(textColor, accentColor)
            is MyFloatingActionButton -> it.setColors(accentColor)
            is ViewGroup -> updateTextColors(it, textColor, accentColor)
        }
    }
}

fun Context.isBlackAndWhiteTheme() =
    baseConfig.textColor == Color.WHITE && baseConfig.primaryColor == Color.BLACK && baseConfig.backgroundColor == Color.BLACK

fun Context.isWhiteTheme() =
    baseConfig.textColor == DARK_GREY && baseConfig.primaryColor == Color.WHITE && baseConfig.backgroundColor == Color.WHITE

fun Context.getAdjustedPrimaryColor() = when {
    isWhiteTheme() || isBlackAndWhiteTheme() -> baseConfig.accentColor
    else -> baseConfig.primaryColor
}

fun Context.toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
    toast(getString(id), length)
}

fun Context.toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
    try {
        if (isOnMainThread()) {
            doToast(this, msg, length)
        } else {
            Handler(Looper.getMainLooper()).post {
                doToast(this, msg, length)
            }
        }
    } catch (e: Exception) {
    }
}

private fun doToast(context: Context, message: String, length: Int) {
    if (context is Activity) {
        if (!context.isFinishing && !context.isDestroyed) {
            Toast.makeText(context, message, length).show()
        }
    } else {
        Toast.makeText(context, message, length).show()
    }
}

fun Context.showErrorToast(msg: String, length: Int = Toast.LENGTH_LONG) {
    toast(String.format(getString(R.string.an_error_occurred), msg), length)
}

fun Context.showErrorToast(exception: Exception, length: Int = Toast.LENGTH_LONG) {
    showErrorToast(exception.toString(), length)
}

val Context.baseConfig: BaseConfig get() = BaseConfig.newInstance(this)

fun Context.hasPermission(permId: Int) = ContextCompat.checkSelfPermission(
    this,
    getPermissionString(permId)
) == PackageManager.PERMISSION_GRANTED

fun getPermissionString(id: Int) = when (id) {
    PERMISSION_READ_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
    PERMISSION_WRITE_STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
    PERMISSION_CAMERA -> Manifest.permission.CAMERA
    PERMISSION_RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
    PERMISSION_READ_CONTACTS -> Manifest.permission.READ_CONTACTS
    PERMISSION_WRITE_CONTACTS -> Manifest.permission.WRITE_CONTACTS
    PERMISSION_READ_CALENDAR -> Manifest.permission.READ_CALENDAR
    PERMISSION_WRITE_CALENDAR -> Manifest.permission.WRITE_CALENDAR
    PERMISSION_CALL_PHONE -> Manifest.permission.CALL_PHONE
    PERMISSION_READ_CALL_LOG -> Manifest.permission.READ_CALL_LOG
    PERMISSION_WRITE_CALL_LOG -> Manifest.permission.WRITE_CALL_LOG
    PERMISSION_GET_ACCOUNTS -> Manifest.permission.GET_ACCOUNTS
    PERMISSION_READ_SMS -> Manifest.permission.READ_SMS
    PERMISSION_SEND_SMS -> Manifest.permission.SEND_SMS
    PERMISSION_READ_PHONE_STATE -> Manifest.permission.READ_PHONE_STATE
    else -> ""
}

fun Context.launchActivityIntent(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        toast(R.string.no_app_found)
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

fun Context.queryCursor(
    uri: Uri,
    projection: Array<String>,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    showErrors: Boolean = false,
    callback: (cursor: Cursor) -> Unit
) {
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    callback(cursor)
                } while (cursor.moveToNext())
            }
        }
    } catch (e: Exception) {
        if (showErrors) {
            showErrorToast(e)
        }
    }
}

fun Context.getDialogTheme() =
    if (baseConfig.backgroundColor.getContrastColor() == Color.WHITE) R.style.MyDialogTheme_Dark else R.style.MyDialogTheme

fun Context.grantReadUriPermission(uriString: String) {
    try {
        // ensure custom reminder sounds play well
        grantUriPermission(
            "com.android.systemui",
            Uri.parse(uriString),
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (ignored: Exception) {
        ignored.printStackTrace()
    }
}

val Context.windowManager: WindowManager get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

@Suppress("DEPRECATION")
val Context.usableScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        return size
    }

@Suppress("DEPRECATION")
val Context.realScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        return size
    }


fun Context.getOTGFastDocumentFile(path: String, otgPathToUse: String? = null): DocumentFile? {
    if (baseConfig.otgTreeUri.isEmpty()) {
        return null
    }

    val otgPath = otgPathToUse ?: baseConfig.otgPath
    if (baseConfig.otgPartition.isEmpty()) {
        baseConfig.otgPartition =
            baseConfig.otgTreeUri.removeSuffix("%3A").substringAfterLast('/').trimEnd('/')
        updateOTGPathFromPartition()
    }

    val relativePath = Uri.encode(path.substring(otgPath.length).trim('/'))
    val fullUri = "${baseConfig.otgTreeUri}/document/${baseConfig.otgPartition}%3A$relativePath"
    return DocumentFile.fromSingleUri(this, Uri.parse(fullUri))
}

fun Context.updateOTGPathFromPartition() {
    val otgPath = "/storage/${baseConfig.otgPartition}"
    baseConfig.otgPath = if (getOTGFastDocumentFile(otgPath, otgPath)?.exists() == true) {
        "/storage/${baseConfig.otgPartition}"
    } else {
        "/mnt/media_rw/${baseConfig.otgPartition}"
    }
}

fun Context.getFormattedSeconds(seconds: Int, showBefore: Boolean = true) = when (seconds) {
    -1 -> getString(R.string.no_reminder)
    0 -> getString(R.string.at_start)
    else -> {
        when {
            seconds < 0 && seconds > -60 * 60 * 24 -> {
                val minutes = -seconds / 60
                getString(R.string.during_day_at).format(minutes / 60, minutes % 60)
            }
            seconds % YEAR_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.years_before else R.plurals.by_years
                resources.getQuantityString(base, seconds / YEAR_SECONDS, seconds / YEAR_SECONDS)
            }
            seconds % MONTH_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.months_before else R.plurals.by_months
                resources.getQuantityString(base, seconds / MONTH_SECONDS, seconds / MONTH_SECONDS)
            }
            seconds % WEEK_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.weeks_before else R.plurals.by_weeks
                resources.getQuantityString(base, seconds / WEEK_SECONDS, seconds / WEEK_SECONDS)
            }
            seconds % DAY_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.days_before else R.plurals.by_days
                resources.getQuantityString(base, seconds / DAY_SECONDS, seconds / DAY_SECONDS)
            }
            seconds % HOUR_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.hours_before else R.plurals.by_hours
                resources.getQuantityString(base, seconds / HOUR_SECONDS, seconds / HOUR_SECONDS)
            }
            seconds % MINUTE_SECONDS == 0 -> {
                val base = if (showBefore) R.plurals.minutes_before else R.plurals.by_minutes
                resources.getQuantityString(
                    base,
                    seconds / MINUTE_SECONDS,
                    seconds / MINUTE_SECONDS
                )
            }
            else -> {
                val base = if (showBefore) R.plurals.seconds_before else R.plurals.by_seconds
                resources.getQuantityString(base, seconds, seconds)
            }
        }
    }
}
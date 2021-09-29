package com.github.fruzelee.eventcalendar.helpers

import android.content.Context
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import com.github.fruzelee.eventcalendar.R
import com.github.fruzelee.eventcalendar.extensions.getSharedPrefs
import java.util.*

/**
 * @author fazle
 * Created 9/25/2021 at 12:41 PM
 * github.com/fruzelee
 * web: fr.crevado.com
 */
open class BaseConfig(val context: Context) {
    protected val prefs = context.getSharedPrefs()

    companion object {
        fun newInstance(context: Context) = BaseConfig(context)
    }

    var treeUri: String
        get() = prefs.getString(TREE_URI, "")!!
        set(uri) = prefs.edit().putString(TREE_URI, uri).apply()

    var otgTreeUri: String
        get() = prefs.getString(OTG_TREE_URI, "")!!
        set(OTGTreeUri) = prefs.edit().putString(OTG_TREE_URI, OTGTreeUri).apply()

    var otgPartition: String
        get() = prefs.getString(OTG_PARTITION, "")!!
        set(OTGPartition) = prefs.edit().putString(OTG_PARTITION, OTGPartition).apply()

    var otgPath: String
        get() = prefs.getString(OTG_REAL_PATH, "")!!
        set(OTGPath) = prefs.edit().putString(OTG_REAL_PATH, OTGPath).apply()

    var textColor: Int
        get() = prefs.getInt(TEXT_COLOR, ContextCompat.getColor(context,R.color.default_text_color))
        set(textColor) = prefs.edit().putInt(TEXT_COLOR, textColor).apply()

    var backgroundColor: Int
        get() = prefs.getInt(
            BACKGROUND_COLOR,
            ContextCompat.getColor(context,R.color.default_background_color)
        )
        set(backgroundColor) = prefs.edit().putInt(BACKGROUND_COLOR, backgroundColor).apply()

    var primaryColor: Int
        get() = prefs.getInt(PRIMARY_COLOR, ContextCompat.getColor(context, R.color.color_primary))
        set(primaryColor) = prefs.edit().putInt(PRIMARY_COLOR, primaryColor).apply()

    var accentColor: Int
        get() = prefs.getInt(ACCENT_COLOR, ContextCompat.getColor(context,R.color.color_primary))
        set(accentColor) = prefs.edit().putInt(ACCENT_COLOR, accentColor).apply()

    var useEnglish: Boolean
        get() = prefs.getBoolean(USE_ENGLISH, false)
        set(useEnglish) {
            wasUseEnglishToggled = true
            prefs.edit().putBoolean(USE_ENGLISH, useEnglish).apply()
        }

    private var wasUseEnglishToggled: Boolean
        get() = prefs.getBoolean(WAS_USE_ENGLISH_TOGGLED, false)
        set(wasUseEnglishToggled) = prefs.edit()
            .putBoolean(WAS_USE_ENGLISH_TOGGLED, wasUseEnglishToggled).apply()

    var use24HourFormat: Boolean
        get() = prefs.getBoolean(USE_24_HOUR_FORMAT, DateFormat.is24HourFormat(context))
        set(use24HourFormat) = prefs.edit().putBoolean(USE_24_HOUR_FORMAT, use24HourFormat).apply()

    var isSundayFirst: Boolean
        get() {
            val isSundayFirst =
                Calendar.getInstance(Locale.getDefault()).firstDayOfWeek == Calendar.SUNDAY
            return prefs.getBoolean(SUNDAY_FIRST, isSundayFirst)
        }
        set(sundayFirst) = prefs.edit().putBoolean(SUNDAY_FIRST, sundayFirst).apply()

    var wasAlarmWarningShown: Boolean
        get() = prefs.getBoolean(WAS_ALARM_WARNING_SHOWN, false)
        set(wasAlarmWarningShown) = prefs.edit()
            .putBoolean(WAS_ALARM_WARNING_SHOWN, wasAlarmWarningShown).apply()


    var useSameSnooze: Boolean
        get() = prefs.getBoolean(USE_SAME_SNOOZE, true)
        set(useSameSnooze) = prefs.edit().putBoolean(USE_SAME_SNOOZE, useSameSnooze).apply()

    var snoozeTime: Int
        get() = prefs.getInt(SNOOZE_TIME, 10)
        set(snoozeDelay) = prefs.edit().putInt(SNOOZE_TIME, snoozeDelay).apply()

    var appId: String
        get() = prefs.getString(APP_ID, "")!!
        set(appId) = prefs.edit().putString(APP_ID, appId).apply()


    var lastExportedSettingsFile: String
        get() = prefs.getString(LAST_EXPORTED_SETTINGS_FILE, "")!!
        set(lastExportedSettingsFile) = prefs.edit()
            .putString(LAST_EXPORTED_SETTINGS_FILE, lastExportedSettingsFile).apply()
}
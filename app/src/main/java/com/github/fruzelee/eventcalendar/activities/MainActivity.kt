package com.github.fruzelee.eventcalendar.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.github.fruzelee.eventcalendar.R
import com.github.fruzelee.eventcalendar.databases.EventsDatabase
import com.github.fruzelee.eventcalendar.databinding.ActivityMainBinding
import com.github.fruzelee.eventcalendar.extensions.*
import com.github.fruzelee.eventcalendar.fragments.MyFragmentHolder
import com.github.fruzelee.eventcalendar.fragments.WeekFragmentsHolder
import com.github.fruzelee.eventcalendar.helpers.*
import com.github.fruzelee.eventcalendar.helpers.Formatter
import com.github.fruzelee.eventcalendar.jobs.EveCalUpdateListener
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentFragments = ArrayList<MyFragmentHolder>()
    private var mStoredTextColor = 0
    private var mStoredBackgroundColor = 0
    private var mStoredAdjustedPrimaryColor = 0
    private var mStoredDayCode = ""
    private var mStoredIsSundayFirst = false
    private var mStoredMidnightSpan = true
    private var mStoredUse24HourFormat = false
    private var mStoredDimPastEvents = true
    private var mStoredHighlightWeekends = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storeStateVariables()

        if (!hasPermission(PERMISSION_WRITE_CALENDAR) || !hasPermission(PERMISSION_READ_CALENDAR)) {
            config.caldavSync = false
        }

        checkIsViewIntent()

        if (!checkIsOpenIntent()) {
            updateViewPager()
        }

        if (savedInstanceState == null) {
            checkEveCalUpdateListener()
        }

    }


    override fun onResume() {
        super.onResume()
        if (mStoredTextColor != config.textColor || mStoredBackgroundColor != config.backgroundColor || mStoredAdjustedPrimaryColor != getAdjustedPrimaryColor()
            || mStoredDayCode != Formatter.getTodayCode() || mStoredDimPastEvents != config.dimPastEvents || mStoredHighlightWeekends != config.highlightWeekends
        ) {
            updateViewPager()
        }

        if (config.storedView == WEEKLY_VIEW) {
            if (mStoredIsSundayFirst != config.isSundayFirst || mStoredUse24HourFormat != config.use24HourFormat || mStoredMidnightSpan != config.showMidnightSpanningEventsAtTop) {
                updateViewPager()
            }
        }

        storeStateVariables()
        updateTextColors(binding.calendarCoordinator)

    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            EventsDatabase.destroyInstance()
            stopEveCalUpdateListener()
        }
    }


    override fun onBackPressed() {
        if (currentFragments.size > 1) {
            removeTopFragment()
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkIsOpenIntent()
        checkIsViewIntent()
    }

    private fun storeStateVariables() {
        config.apply {
            mStoredIsSundayFirst = isSundayFirst
            mStoredTextColor = textColor
            mStoredBackgroundColor = backgroundColor
            mStoredUse24HourFormat = use24HourFormat
            mStoredDimPastEvents = dimPastEvents
            mStoredHighlightWeekends = highlightWeekends
            mStoredMidnightSpan = showMidnightSpanningEventsAtTop
        }
        mStoredAdjustedPrimaryColor = getAdjustedPrimaryColor()
        mStoredDayCode = Formatter.getTodayCode()
    }

    private fun checkEveCalUpdateListener() {
        if (isNougatPlus()) {
            val updateListener = EveCalUpdateListener()
            if (config.caldavSync) {
                if (!updateListener.isScheduled(applicationContext)) {
                    updateListener.scheduleJob(applicationContext)
                }
            } else {
                updateListener.cancelJob(applicationContext)
            }
        }
    }

    private fun stopEveCalUpdateListener() {
        if (isNougatPlus()) {
            if (!config.caldavSync) {
                val updateListener = EveCalUpdateListener()
                updateListener.cancelJob(applicationContext)
            }
        }
    }

    private fun checkIsOpenIntent(): Boolean {
        val dayCodeToOpen = intent.getStringExtra(DAY_CODE) ?: ""
        val viewToOpen = intent.getIntExtra(VIEW_TO_OPEN, WEEKLY_VIEW)
        intent.removeExtra(VIEW_TO_OPEN)
        intent.removeExtra(DAY_CODE)
        if (dayCodeToOpen.isNotEmpty()) {
            if (viewToOpen != LAST_VIEW) {
                config.storedView = viewToOpen
            }
            updateViewPager()
            return true
        }

        val eventIdToOpen = intent.getLongExtra(EVENT_ID, 0L)
        val eventOccurrenceToOpen = intent.getLongExtra(EVENT_OCCURRENCE_TS, 0L)
        intent.removeExtra(EVENT_ID)
        intent.removeExtra(EVENT_OCCURRENCE_TS)
        if (eventIdToOpen != 0L && eventOccurrenceToOpen != 0L) {
            Intent(this, EventActivity::class.java).apply {
                putExtra(EVENT_ID, eventIdToOpen)
                putExtra(EVENT_OCCURRENCE_TS, eventOccurrenceToOpen)
                startActivity(this)
            }
        }

        return false
    }

    private fun checkIsViewIntent() {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            if (uri?.authority?.equals("com.android.calendar") == true || uri?.authority?.substringAfter(
                    "@"
                ) == "com.android.calendar"
            ) {
                if (uri.path!!.startsWith("/events")) {
                    ensureBackgroundThread {
                        // intents like content://com.android.calendar/events/1756
                        val eventId = uri.lastPathSegment
                        val id = eventsDB.getEventIdWithLastImportId("%-$eventId")
                        if (id != null) {
                            Intent(this, EventActivity::class.java).apply {
                                putExtra(EVENT_ID, id)
                                startActivity(this)
                            }
                        } else {
                            toast(R.string.caldav_event_not_found, Toast.LENGTH_LONG)
                        }
                    }
                } else if (uri.path!!.startsWith("/time") || intent?.extras?.getBoolean(
                        "DETAIL_VIEW",
                        false
                    ) == true
                ) {
                    // clicking date on a third party widget: content://com.android.calendar/time/1507309245683
                    // or content://0@com.android.calendar/time/1584958526435
                    val timestamp = uri.pathSegments.last()
                    if (timestamp.areDigitsOnly()) {
                        openDayAt()
                        return
                    }
                }
            }
        }
    }

    private fun updateViewPager() {
        val fragment = getFragmentsHolder()
        currentFragments.forEach {
            supportFragmentManager.beginTransaction().remove(it).commitNow()
        }
        currentFragments.clear()
        currentFragments.add(fragment)
        val bundle = Bundle()

        when (config.storedView) {
            WEEKLY_VIEW -> bundle.putString(WEEK_START_DATE_TIME, getThisWeekDateTime())
        }

        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
    }


    private fun getThisWeekDateTime(): String {
        val currentOffsetHours = TimeZone.getDefault().rawOffset / 1000 / 60 / 60

        // not great, not terrible
        val useHours = if (currentOffsetHours >= 10) 8 else 12
        var thisWeek =
            DateTime().withZone(DateTimeZone.UTC).withDayOfWeek(1).withHourOfDay(useHours)
                .minusDays(if (config.isSundayFirst) 1 else 0)
        if (DateTime().minusDays(7).seconds() > thisWeek.seconds()) {
            thisWeek = thisWeek.plusDays(7)
        }
        return thisWeek.toString()
    }

    private fun getFragmentsHolder() = when (config.storedView) {
        WEEKLY_VIEW -> WeekFragmentsHolder()
        else -> WeekFragmentsHolder()
    }

    private fun removeTopFragment() {
        supportFragmentManager.beginTransaction().remove(currentFragments.last()).commit()
        currentFragments.removeAt(currentFragments.size - 1)
        currentFragments.last().apply {
            refreshEvents()
            updateActionBarTitle()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(currentFragments.size > 1)
    }

    private fun openDayAt() {
        config.storedView = WEEKLY_VIEW
        updateViewPager()
    }

}

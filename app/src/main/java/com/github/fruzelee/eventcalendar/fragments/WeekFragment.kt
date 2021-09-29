package com.github.fruzelee.eventcalendar.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Range
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.collection.LongSparseArray
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.fruzelee.eventcalendar.R
import com.github.fruzelee.eventcalendar.activities.EventActivity
import com.github.fruzelee.eventcalendar.extensions.*
import com.github.fruzelee.eventcalendar.helpers.*
import com.github.fruzelee.eventcalendar.helpers.Formatter
import com.github.fruzelee.eventcalendar.interfaces.WeekFragmentListener
import com.github.fruzelee.eventcalendar.interfaces.WeeklyCalendar
import com.github.fruzelee.eventcalendar.models.Event
import com.github.fruzelee.eventcalendar.models.EventWeeklyView
import com.github.fruzelee.eventcalendar.views.MyScrollView
import com.github.fruzelee.eventcalendar.views.MyTextView
import kotlinx.android.synthetic.main.fragment_week.*
import kotlinx.android.synthetic.main.fragment_week.view.*
import org.joda.time.DateTime
import org.joda.time.Days
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class WeekFragment : Fragment(), WeeklyCalendar {

    var listener: WeekFragmentListener? = null
    private var weekTimestamp = 0L
    private var rowHeight = 0f
    private var todayColumnIndex = -1
    private var primaryColor = 0
    private var lastHash = 0
    private var prevScaleSpanY = 0f
    private var scaleCenterPercent = 0f
    private var defaultRowHeight = 0f
    private var screenHeight = 0
    private var rowHeightsAtScale = 0f
    private var prevScaleFactor = 0f
    private var mWasDestroyed = false
    private var isFragmentVisible = false
    private var wasFragmentInit = false
    private var wasExtraHeightAdded = false
    private var dimPastEvents = true
    private var highlightWeekends = false
    private var wasScaled = false
    private var isPrintVersion = false
    private var selectedGrid: View? = null
    private var currentTimeView: ImageView? = null
    private var fadeOutHandler = Handler(Looper.getMainLooper())
    private var allDayHolders = ArrayList<RelativeLayout>()
    private var allDayRows = ArrayList<HashSet<Int>>()
    private var currEvents = ArrayList<Event>()
    private var dayColumns = ArrayList<RelativeLayout>()
    private var eventTypeColors = LongSparseArray<Int>()
    private var eventTimeRanges = LinkedHashMap<String, ArrayList<EventWeeklyView>>()

    private lateinit var inflater: LayoutInflater
    private lateinit var mView: View
    private lateinit var scrollView: MyScrollView
    private lateinit var res: Resources
    private lateinit var config: Config

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        res = requireContext().resources
        config = requireContext().config
        rowHeight = requireContext().getWeeklyViewItemHeight()
        defaultRowHeight = res.getDimension(R.dimen.weekly_view_row_height)
        weekTimestamp = requireArguments().getLong(WEEK_START_TIMESTAMP)
        dimPastEvents = config.dimPastEvents
        highlightWeekends = config.highlightWeekends
        primaryColor = requireContext().getAdjustedPrimaryColor()
        allDayRows.add(HashSet())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        this.inflater = inflater

        val fullHeight = requireContext().getWeeklyViewItemHeight().toInt() * 24

        mView = inflater.inflate(R.layout.fragment_week, container, false).apply {
            scrollView = week_events_scrollview
            week_horizontal_grid_holder.layoutParams.height = fullHeight
            week_events_columns_holder.layoutParams.height = fullHeight

            val scaleDetector = getViewScaleDetector()
            scrollView.setOnTouchListener { _, motionEvent ->
                scaleDetector.onTouchEvent(motionEvent)
                if (motionEvent.action == MotionEvent.ACTION_UP && wasScaled) {
                    scrollView.isScrollable = true
                    wasScaled = false
                    true
                } else {
                    false
                }
            }
        }

        addDayColumns()
        scrollView.setOnScrollviewListener(object : MyScrollView.ScrollViewListener {
            override fun onScrollChanged(
                scrollView: MyScrollView,
                x: Int,
                y: Int,
                oldX: Int,
                oldY: Int
            ) {
                checkScrollLimits(y)
            }
        })

        scrollView.onGlobalLayout {
            if (fullHeight < scrollView.height) {
                scrollView.layoutParams.height =
                    fullHeight - res.getDimension(R.dimen.one_dp).toInt()
            }

            val initialScrollY = (rowHeight * config.startWeeklyAt).toInt()
            updateScrollY(max(listener?.getCurrScrollY() ?: 0, initialScrollY))
        }

        wasFragmentInit = true
        return mView
    }

    override fun onResume() {
        super.onResume()
        requireContext().eventsHelper.getEventTypes(requireActivity(), false) { it ->
            it.map {
                eventTypeColors.put(it.id!!, it.color)
            }
        }

        setupDayLabels()
        updateCalendar()

        if (rowHeight != 0f && mView.width != 0) {
            addCurrentTimeIndicator()
        }
    }

    override fun onPause() {
        super.onPause()
        wasExtraHeightAdded = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mWasDestroyed = true
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        isFragmentVisible = menuVisible
        if (isFragmentVisible && wasFragmentInit) {
            listener?.updateHoursTopMargin(mView.week_top_holder.height)
            checkScrollLimits(scrollView.scrollY)

            // fix some glitches like at swiping from a fully scaled out fragment with all-day events to an empty one
            val fullFragmentHeight =
                (listener?.getFullFragmentHeight() ?: 0) - mView.week_top_holder.height
            if (scrollView.height < fullFragmentHeight) {
                config.weeklyViewItemHeightMultiplier = fullFragmentHeight / 24 / defaultRowHeight
                updateViewScale()
                listener?.updateRowHeight(rowHeight.toInt())
            }
        }
    }

    fun updateCalendar() {
        if (context != null) {
            WeeklyCalendarImpl(this, requireContext()).updateWeeklyCalendar(weekTimestamp)
        }
    }

    private fun addDayColumns() {
        mView.week_events_columns_holder.removeAllViews()
        (0 until config.weeklyViewDays).forEach {
            val column = inflater.inflate(
                R.layout.weekly_view_day_column,
                mView.week_events_columns_holder,
                false
            ) as RelativeLayout
            column.tag = Formatter.getUTCDayCodeFromTS(weekTimestamp + it * DAY_SECONDS)
            mView.week_events_columns_holder.addView(column)
            dayColumns.add(column)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupDayLabels() {
        var curDay = Formatter.getUTCDateTimeFromTS(weekTimestamp)
        val todayCode = Formatter.getDayCodeFromDateTime(DateTime())
        val screenWidth = context?.usableScreenSize?.x ?: return
        val dayWidth = screenWidth / config.weeklyViewDays
        val useLongerDayLabels = dayWidth > res.getDimension(R.dimen.weekly_view_min_day_label)

        mView.week_letters_holder.removeAllViews()
        for (i in 0 until config.weeklyViewDays) {
            val dayCode = Formatter.getDayCodeFromDateTime(curDay)
            val labelIDs = if (useLongerDayLabels) {
                R.array.week_days_short
            } else {
                R.array.week_day_letters
            }

            val dayLetters = res.getStringArray(labelIDs).toMutableList() as ArrayList<String>
            val dayLetter = dayLetters[curDay.dayOfWeek - 1]

            val textColor = if (isPrintVersion) {
                ContextCompat.getColor(requireContext(), R.color.theme_light_text_color)
            } else if (todayCode == dayCode) {
                primaryColor
            } else if (highlightWeekends && isWeekend(curDay.dayOfWeek - 1, config.isSundayFirst)) {
                ContextCompat.getColor(requireContext(), R.color.red_text)
            } else {
                config.textColor
            }

            val label = inflater.inflate(
                R.layout.weekly_view_day_letter,
                mView.week_letters_holder,
                false
            ) as MyTextView
            label.text = "$dayLetter\n${curDay.dayOfMonth}"
            label.setTextColor(textColor)
            if (todayCode == dayCode) {
                todayColumnIndex = i
            }

            mView.week_letters_holder.addView(label)
            curDay = curDay.plusDays(1)
        }
    }

    private fun checkScrollLimits(y: Int) {
        if (isFragmentVisible) {
            listener?.scrollTo(y)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initGrid() {
        (0 until config.weeklyViewDays).mapNotNull { dayColumns.getOrNull(it) }
            .forEachIndexed { index, layout ->
                layout.removeAllViews()
                val gestureDetector = getViewGestureDetector(layout, index)

                layout.setOnTouchListener { _, motionEvent ->
                    gestureDetector.onTouchEvent(motionEvent)
                    true
                }
            }
    }

    private fun getViewGestureDetector(view: ViewGroup, index: Int): GestureDetector {
        return GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            @SuppressLint("InflateParams")
            override fun onSingleTapUp(event: MotionEvent): Boolean {
                selectedGrid?.animation?.cancel()
                selectedGrid?.beGone()

                val hour = (event.y / rowHeight).toInt()
                selectedGrid =
                    (inflater.inflate(R.layout.week_grid_item, null) as ImageView).apply {
                        view.addView(this)
                        background = ColorDrawable(primaryColor)
                        layoutParams.width = view.width
                        layoutParams.height = rowHeight.toInt()
                        y = hour * rowHeight
                        applyColorFilter(primaryColor.getContrastColor())

                        setOnClickListener {
                            val timestamp =
                                Formatter.getDateTimeFromTS(weekTimestamp + index * DAY_SECONDS)
                                    .withTime(hour, 0, 0, 0).seconds()
                            Intent(context, EventActivity::class.java).apply {
                                putExtra(NEW_EVENT_START_TS, timestamp)
                                putExtra(NEW_EVENT_SET_HOUR_DURATION, true)
                                startActivity(this)
                            }
                        }

                        // do not use setStartDelay, it will trigger instantly if the device has disabled animations
                        fadeOutHandler.removeCallbacksAndMessages(null)
                        fadeOutHandler.postDelayed({
                            animate().alpha(0f).withEndAction {
                                beGone()
                            }
                        }, PLUS_FADEOUT_DELAY)
                    }
                return super.onSingleTapUp(event)
            }
        })
    }

    private fun getViewScaleDetector(): ScaleGestureDetector {
        return ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val percent = (prevScaleSpanY - detector.currentSpanY) / screenHeight
                    prevScaleSpanY = detector.currentSpanY

                    val wantedFactor =
                        config.weeklyViewItemHeightMultiplier - (SCALE_RANGE * percent)
                    var newFactor = max(min(wantedFactor, MAX_SCALE_FACTOR),
                        MIN_SCALE_FACTOR
                    )
                    if (scrollView.height > defaultRowHeight * newFactor * 24) {
                        newFactor = scrollView.height / 24f / defaultRowHeight
                    }

                    if (abs(newFactor - prevScaleFactor) > MIN_SCALE_DIFFERENCE) {
                        prevScaleFactor = newFactor
                        config.weeklyViewItemHeightMultiplier = newFactor
                        updateViewScale()
                        listener?.updateRowHeight(rowHeight.toInt())

                        val targetY =
                            rowHeightsAtScale * rowHeight - scaleCenterPercent * getVisibleHeight()
                        scrollView.scrollTo(0, targetY.toInt())
                    }
                    return super.onScale(detector)
                }

                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    scaleCenterPercent = detector.focusY / scrollView.height
                    rowHeightsAtScale =
                        (scrollView.scrollY + scaleCenterPercent * getVisibleHeight()) / rowHeight
                    scrollView.isScrollable = false
                    prevScaleSpanY = detector.currentSpanY
                    prevScaleFactor = config.weeklyViewItemHeightMultiplier
                    wasScaled = true
                    screenHeight = context!!.realScreenSize.y
                    return super.onScaleBegin(detector)
                }
            })
    }

    private fun getVisibleHeight(): Float {
        val fullContentHeight = rowHeight * 24
        val visibleRatio = scrollView.height / fullContentHeight
        return fullContentHeight * visibleRatio
    }

    override fun updateWeeklyCalendar(events: ArrayList<Event>) {
        val newHash = events.hashCode()
        if (newHash == lastHash || mWasDestroyed || context == null) {
            return
        }

        lastHash = newHash

        requireActivity().runOnUiThread {
            if (context != null && activity != null && isAdded) {
                val replaceDescription = config.replaceDescription
                val sorted = events.sortedWith(
                    compareBy<Event> { it.startTS }.thenBy { it.endTS }.thenBy { it.title }
                        .thenBy { if (replaceDescription) it.location else it.description }
                ).toMutableList() as ArrayList<Event>

                currEvents = sorted
                addEvents(sorted)
            }
        }
    }

    private fun updateViewScale() {
        rowHeight = context?.getWeeklyViewItemHeight() ?: return

        val oneDp = res.getDimension(R.dimen.one_dp).toInt()
        val fullHeight = max(rowHeight.toInt() * 24, scrollView.height + oneDp)
        scrollView.layoutParams.height = fullHeight - oneDp
        mView.week_horizontal_grid_holder.layoutParams.height = fullHeight
        mView.week_events_columns_holder.layoutParams.height = fullHeight
        addEvents(currEvents)
    }

    @SuppressLint("InflateParams")
    private fun addEvents(events: ArrayList<Event>) {
        initGrid()
        allDayHolders.clear()
        allDayRows.clear()
        eventTimeRanges.clear()
        allDayRows.add(HashSet())
        week_all_day_holder?.removeAllViews()
        addNewLine()

        val minuteHeight = rowHeight / 60
        val minimalHeight = res.getDimension(R.dimen.weekly_view_minimal_event_height).toInt()
        val density = res.displayMetrics.density.roundToInt()

        for (event in events) {
            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val startDayCode = Formatter.getDayCodeFromDateTime(startDateTime)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
            val endDayCode = Formatter.getDayCodeFromDateTime(endDateTime)

            if (event.getIsAllDay() || ((startDayCode != endDayCode) && config.showMidnightSpanningEventsAtTop)) {
                continue
            }

            var currentDateTime = startDateTime
            var currentDayCode = Formatter.getDayCodeFromDateTime(currentDateTime)
            do {
                val startMinutes = when (currentDayCode == startDayCode) {
                    true -> (startDateTime.minuteOfDay)
                    else -> 0
                }
                val duration = when (currentDayCode == endDayCode) {
                    true -> (endDateTime.minuteOfDay - startMinutes)
                    else -> 1440
                }
                val range = Range(startMinutes, startMinutes + duration)
                val eventWeekly = EventWeeklyView(event.id!!, range)

                if (!eventTimeRanges.containsKey(currentDayCode)) {
                    eventTimeRanges[currentDayCode] = ArrayList()
                }
                eventTimeRanges[currentDayCode]?.add(eventWeekly)

                currentDateTime = currentDateTime.plusDays(1)
                currentDayCode = Formatter.getDayCodeFromDateTime(currentDateTime)
            } while (currentDayCode.toInt() <= endDayCode.toInt())
        }

        dayEvents@ for (event in events) {
            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val startDayCode = Formatter.getDayCodeFromDateTime(startDateTime)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
            val endDayCode = Formatter.getDayCodeFromDateTime(endDateTime)
            if (event.getIsAllDay() || ((startDayCode != endDayCode) && config.showMidnightSpanningEventsAtTop)) {
                addAllDayEvent(event)
            } else {
                var currentDateTime = startDateTime
                var currentDayCode = Formatter.getDayCodeFromDateTime(currentDateTime)
                do {
                    val dayOfWeek = dayColumns.indexOfFirst { it.tag == currentDayCode }
                    if (dayOfWeek == -1 || dayOfWeek >= config.weeklyViewDays) {
                        continue@dayEvents
                    }

                    val startMinutes = when (currentDayCode == startDayCode) {
                        true -> (startDateTime.minuteOfDay)
                        else -> 0
                    }
                    val duration = when (currentDayCode == endDayCode) {
                        true -> (endDateTime.minuteOfDay - startMinutes)
                        else -> 1440
                    }
                    val range = Range(startMinutes, startMinutes + duration)
                    var overlappingEvents = 0
                    var currentEventOverlapIndex = 0
                    var foundCurrentEvent = false

                    eventTimeRanges[currentDayCode]!!.forEachIndexed { _, eventWeeklyView ->
                        if (eventWeeklyView.range.touch(range)) {
                            overlappingEvents++

                            if (eventWeeklyView.id == event.id) {
                                foundCurrentEvent = true
                            }

                            if (!foundCurrentEvent) {
                                currentEventOverlapIndex++
                            }
                        }
                    }

                    val dayColumn = dayColumns[dayOfWeek]
                    (inflater.inflate(R.layout.week_event_marker, null, false) as TextView).apply {
                        var backgroundColor = eventTypeColors.get(event.eventType, primaryColor)
                        var textColor = backgroundColor.getContrastColor()
                        if (dimPastEvents && event.isPastEvent && !isPrintVersion) {
                            backgroundColor = backgroundColor.adjustAlpha(LOWER_ALPHA)
                            textColor = textColor.adjustAlpha(HIGHER_ALPHA)
                        }

                        background = ColorDrawable(backgroundColor)
                        setTextColor(textColor)
                        text = event.title
                        contentDescription = text
                        dayColumn.addView(this)
                        y = startMinutes * minuteHeight
                        (layoutParams as RelativeLayout.LayoutParams).apply {
                            width = dayColumn.width - 1
                            width /= max(overlappingEvents, 1)
                            if (overlappingEvents > 1) {
                                x = width * currentEventOverlapIndex.toFloat()
                                if (currentEventOverlapIndex != 0) {
                                    x += density
                                }

                                width -= density
                                if (currentEventOverlapIndex + 1 != overlappingEvents) {
                                    if (currentEventOverlapIndex != 0) {
                                        width -= density
                                    }
                                }
                            }

                            minHeight = if (event.startTS == event.endTS) {
                                minimalHeight
                            } else {
                                (duration * minuteHeight).toInt() - 1
                            }
                        }

                        setOnClickListener {
                            Intent(context, EventActivity::class.java).apply {
                                putExtra(EVENT_ID, event.id!!)
                                putExtra(EVENT_OCCURRENCE_TS, event.startTS)
                                startActivity(this)
                            }
                        }
                    }

                    currentDateTime = currentDateTime.plusDays(1)
                    currentDayCode = Formatter.getDayCodeFromDateTime(currentDateTime)
                } while (currentDayCode.toInt() <= endDayCode.toInt())
            }
        }

        checkTopHolderHeight()
        addCurrentTimeIndicator()
    }

    @SuppressLint("InflateParams")
    private fun addNewLine() {
        val allDaysLine =
            inflater.inflate(R.layout.all_day_events_holder_line, null, false) as RelativeLayout
        week_all_day_holder?.addView(allDaysLine)
        allDayHolders.add(allDaysLine)
    }

    @SuppressLint("InflateParams")
    private fun addCurrentTimeIndicator() {
        if (todayColumnIndex != -1) {
            val calendar = Calendar.getInstance()
            val minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            if (todayColumnIndex >= dayColumns.size) {
                currentTimeView?.alpha = 0f
                return
            }

            if (currentTimeView != null) {
                mView.week_events_holder.removeView(currentTimeView)
            }

            if (isPrintVersion) {
                return
            }

            val weeklyViewDays = config.weeklyViewDays
            currentTimeView =
                (inflater.inflate(R.layout.week_now_marker, null, false) as ImageView).apply {
                    applyColorFilter(primaryColor)
                    mView.week_events_holder.addView(this, 0)
                    val extraWidth = res.getDimension(R.dimen.activity_margin).toInt()
                    val markerHeight = res.getDimension(R.dimen.weekly_view_now_height).toInt()
                    val minuteHeight = rowHeight / 60
                    (layoutParams as RelativeLayout.LayoutParams).apply {
                        width = (mView.width / weeklyViewDays) + extraWidth
                        height = markerHeight
                    }

                    x = if (weeklyViewDays == 1) {
                        0f
                    } else {
                        (mView.width / weeklyViewDays * todayColumnIndex).toFloat() - extraWidth / 2f
                    }

                    y = minutes * minuteHeight - markerHeight / 2
                }
        }
    }

    private fun checkTopHolderHeight() {
        mView.week_top_holder.onGlobalLayout {
            if (isFragmentVisible && activity != null && !mWasDestroyed) {
                listener?.updateHoursTopMargin(mView.week_top_holder.height)
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun addAllDayEvent(event: Event) {
        (inflater.inflate(R.layout.week_all_day_event_marker, null, false) as TextView).apply {
            var backgroundColor = eventTypeColors.get(event.eventType, primaryColor)
            var textColor = backgroundColor.getContrastColor()
            if (dimPastEvents && event.isPastEvent && !isPrintVersion) {
                backgroundColor = backgroundColor.adjustAlpha(LOWER_ALPHA)
                textColor = textColor.adjustAlpha(HIGHER_ALPHA)
            }
            background = ColorDrawable(backgroundColor)

            setTextColor(textColor)
            text = event.title
            contentDescription = text

            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)

            val minTS = max(startDateTime.seconds(), weekTimestamp)
            val maxTS = min(endDateTime.seconds(), weekTimestamp + 2 * WEEK_SECONDS)

            // fix a visual glitch with all-day events or events lasting multiple days starting at midnight on monday, being shown the previous week too
            if (minTS == maxTS && (minTS - weekTimestamp == WEEK_SECONDS.toLong())) {
                return
            }

            val isStartTimeDay =
                Formatter.getDateTimeFromTS(maxTS) == Formatter.getDateTimeFromTS(maxTS)
                    .withTimeAtStartOfDay()
            val numDays = Days.daysBetween(
                Formatter.getDateTimeFromTS(minTS).toLocalDate(),
                Formatter.getDateTimeFromTS(maxTS).toLocalDate()
            ).days
            val daysCnt = if (numDays == 1 && isStartTimeDay) 0 else numDays
            val startDateTimeInWeek = Formatter.getDateTimeFromTS(minTS)
            val firstDayIndex =
                (startDateTimeInWeek.dayOfWeek - if (config.isSundayFirst) 0 else 1) % 7

            var doesEventFit: Boolean
            val cnt = allDayRows.size - 1
            var wasEventHandled = false
            var drawAtLine = 0
            for (index in 0..cnt) {
                doesEventFit = true
                drawAtLine = index
                val row = allDayRows[index]
                for (i in firstDayIndex..firstDayIndex + daysCnt) {
                    if (row.contains(i)) {
                        doesEventFit = false
                    }
                }

                for (dayIndex in firstDayIndex..firstDayIndex + daysCnt) {
                    if (doesEventFit) {
                        row.add(dayIndex)
                        wasEventHandled = true
                    } else if (index == cnt) {
                        if (allDayRows.size == index + 1) {
                            allDayRows.add(HashSet())
                            addNewLine()
                            drawAtLine++
                            wasEventHandled = true
                        }
                        allDayRows.last().add(dayIndex)
                    }
                }

                if (wasEventHandled) {
                    break
                }
            }

            val dayCodeStart = Formatter.getDayCodeFromDateTime(startDateTime).toInt()
            val dayCodeEnd = Formatter.getDayCodeFromDateTime(endDateTime).toInt()
            val dayOfWeek =
                dayColumns.indexOfFirst { it.tag.toInt() == dayCodeStart || (it.tag.toInt() in (dayCodeStart + 1)..dayCodeEnd) }
            if (dayOfWeek == -1) {
                return
            }

            allDayHolders[drawAtLine].addView(this)
            val dayWidth = mView.width / config.weeklyViewDays
            (layoutParams as RelativeLayout.LayoutParams).apply {
                leftMargin = dayOfWeek * dayWidth
                bottomMargin = 1
                width = (dayWidth) * (daysCnt + 1)
            }

            calculateExtraHeight()

            setOnClickListener {
                Intent(context, EventActivity::class.java).apply {
                    putExtra(EVENT_ID, event.id)
                    putExtra(EVENT_OCCURRENCE_TS, event.startTS)
                    startActivity(this)
                }
            }
        }
    }

    private fun calculateExtraHeight() {
        mView.week_top_holder.onGlobalLayout {
            if (activity != null && !mWasDestroyed) {
                if (isFragmentVisible) {
                    listener?.updateHoursTopMargin(mView.week_top_holder.height)
                }

                if (!wasExtraHeightAdded) {
                    wasExtraHeightAdded = true
                }
            }
        }
    }

    fun updateScrollY(y: Int) {
        if (wasFragmentInit) {
            scrollView.scrollY = y
        }
    }

    fun updateNotVisibleViewScaleLevel() {
        if (!isFragmentVisible) {
            updateViewScale()
        }
    }

    companion object {
        private const val PLUS_FADEOUT_DELAY = 5000L
        private const val MIN_SCALE_FACTOR = 0.3f
        private const val MAX_SCALE_FACTOR = 5f
        private const val MIN_SCALE_DIFFERENCE = 0.02f
        private const val SCALE_RANGE = MAX_SCALE_FACTOR - MIN_SCALE_FACTOR
    }

}
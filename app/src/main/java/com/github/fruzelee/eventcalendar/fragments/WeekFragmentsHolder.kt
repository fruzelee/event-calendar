package com.github.fruzelee.eventcalendar.fragments

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.github.fruzelee.eventcalendar.R
import com.github.fruzelee.eventcalendar.adapters.MyWeekPagerAdapter
import com.github.fruzelee.eventcalendar.extensions.*
import com.github.fruzelee.eventcalendar.helpers.Formatter
import com.github.fruzelee.eventcalendar.helpers.WEEK_SECONDS
import com.github.fruzelee.eventcalendar.helpers.WEEK_START_DATE_TIME
import com.github.fruzelee.eventcalendar.interfaces.WeekFragmentListener
import com.github.fruzelee.eventcalendar.views.MyScrollView
import com.github.fruzelee.eventcalendar.views.MyViewPager
import kotlinx.android.synthetic.main.fragment_week_holder.view.*
import org.joda.time.DateTime

class WeekFragmentsHolder : MyFragmentHolder(), WeekFragmentListener {

    private var viewPager: MyViewPager? = null
    private var weekHolder: ViewGroup? = null
    private var defaultWeeklyPage = 0
    private var thisWeekTS = 0L
    private var currentWeekTS = 0L
    private var isGoToTodayVisible = false
    private var weekScrollY = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dateTimeString = arguments?.getString(WEEK_START_DATE_TIME) ?: return
        currentWeekTS = (DateTime.parse(dateTimeString) ?: DateTime()).seconds()
        thisWeekTS = currentWeekTS
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        weekHolder = inflater.inflate(R.layout.fragment_week_holder, container, false) as ViewGroup
        weekHolder!!.background = ColorDrawable(requireContext().config.backgroundColor)

        val itemHeight = requireContext().getWeeklyViewItemHeight().toInt()
        weekHolder!!.week_view_hours_holder.setPadding(0, 0, 0, itemHeight)

        viewPager = weekHolder!!.week_view_view_pager
        viewPager!!.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        weekHolder!!.btnPrevious.setOnClickListener { attemptWeekViewPagerUpdate(true) }
        weekHolder!!.btnNext.setOnClickListener { attemptWeekViewPagerUpdate(false) }
        return weekHolder
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFragment() {
        addHours()
        setupWeeklyViewPager()

        weekHolder!!.week_view_hours_scrollview.setOnTouchListener { _, _ -> true }

        updateActionBarTitle()
    }

    private fun setupWeeklyViewPager() {
        val weekTSs = getWeekTimestamps(currentWeekTS)
        val weeklyAdapter =
            MyWeekPagerAdapter(requireActivity().supportFragmentManager, weekTSs, this)

        defaultWeeklyPage = weekTSs.size / 2

        viewPager!!.apply {
            adapter = weeklyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                }

                override fun onPageSelected(position: Int) {
                    currentWeekTS = weekTSs[position]
                    val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                    if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                        isGoToTodayVisible = shouldGoToTodayBeVisible
                    }

                    setupWeeklyActionbarTitle(weekTSs[position])
                }
            })
            currentItem = defaultWeeklyPage
        }

        weekHolder!!.week_view_hours_scrollview.setOnScrollviewListener(object :
            MyScrollView.ScrollViewListener {
            override fun onScrollChanged(
                scrollView: MyScrollView,
                x: Int,
                y: Int,
                oldX: Int,
                oldY: Int
            ) {
                weekScrollY = y
                weeklyAdapter.updateScrollY(viewPager!!.currentItem, y)
            }
        })
    }

    @SuppressLint("InflateParams")
    private fun addHours(textColor: Int = requireContext().config.textColor) {
        val itemHeight = requireContext().getWeeklyViewItemHeight().toInt()
        weekHolder!!.week_view_hours_holder.removeAllViews()
        val hourDateTime = DateTime().withDate(2000, 1, 1).withTime(0, 0, 0, 0)
        for (i in 1..23) {
            val formattedHours = Formatter.getHours(requireContext(), hourDateTime.withHourOfDay(i))
            (layoutInflater.inflate(
                R.layout.weekly_view_hour_textview,
                null,
                false
            ) as TextView).apply {
                text = formattedHours
                setTextColor(textColor)
                height = itemHeight
                weekHolder!!.week_view_hours_holder.addView(this)
            }
        }
    }

    private fun getWeekTimestamps(targetSeconds: Long): List<Long> {
        val weekTSs = ArrayList<Long>(PREFILLED_WEEKS)
        val dateTime = Formatter.getDateTimeFromTS(targetSeconds)
        val shownWeekDays = requireContext().config.weeklyViewDays
        var currentWeek = dateTime.minusDays(PREFILLED_WEEKS / 2 * shownWeekDays)
        for (i in 0 until PREFILLED_WEEKS) {
            weekTSs.add(currentWeek.seconds())
            currentWeek = currentWeek.plusDays(shownWeekDays)
        }
        return weekTSs
    }

    private fun setupWeeklyActionbarTitle(timestamp: Long) {
        val startDateTime = Formatter.getDateTimeFromTS(timestamp)
        val endDateTime = Formatter.getDateTimeFromTS(timestamp + WEEK_SECONDS)
        val startMonthName = Formatter.getMonthName(requireContext(), startDateTime.monthOfYear)
        if (startDateTime.monthOfYear == endDateTime.monthOfYear) {
            var newTitle = startMonthName
            if (startDateTime.year != DateTime().year) {
                newTitle += " - ${startDateTime.year}"
            }
            (activity as AppCompatActivity).updateActionBarTitle(newTitle)
        } else {
            val endMonthName = Formatter.getMonthName(requireContext(), endDateTime.monthOfYear)
            (activity as AppCompatActivity).updateActionBarTitle("$startMonthName - $endMonthName")
        }
        (activity as AppCompatActivity).updateActionBarSubtitle(
            "${getString(R.string.week)} ${
                startDateTime.plusDays(
                    3
                ).weekOfWeekyear
            }"
        )
    }

    override fun goToToday() {
        currentWeekTS = thisWeekTS
        setupFragment()
    }

    @SuppressLint("InflateParams")
    override fun showGoToDateDialog() {
        requireContext().setTheme(requireContext().getDialogTheme())
        val view = layoutInflater.inflate(R.layout.date_picker, null)
        val datePicker = view.findViewById<DatePicker>(R.id.date_picker)

        val dateTime = Formatter.getDateTimeFromTS(currentWeekTS)
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, dateTime.dayOfMonth, null)

        AlertDialog.Builder(requireContext())
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ -> dateSelected(dateTime, datePicker) }
            .create().apply {
                activity?.setupDialogStuff(view, this)
            }
    }

    private fun dateSelected(dateTime: DateTime, datePicker: DatePicker) {
        val isSundayFirst = requireContext().config.isSundayFirst
        val month = datePicker.month + 1
        val year = datePicker.year
        val day = datePicker.dayOfMonth
        var newDateTime = dateTime.withDate(year, month, day)

        if (isSundayFirst) {
            newDateTime = newDateTime.plusDays(1)
        }

        var selectedWeek = newDateTime.withDayOfWeek(1).withTimeAtStartOfDay()
            .minusDays(if (isSundayFirst) 1 else 0)
        if (newDateTime.minusDays(7).seconds() > selectedWeek.seconds()) {
            selectedWeek = selectedWeek.plusDays(7)
        }

        currentWeekTS = selectedWeek.seconds()
        setupFragment()
    }

    override fun refreshEvents() {
        (viewPager?.adapter as? MyWeekPagerAdapter)?.updateCalendars(viewPager!!.currentItem)
    }

    override fun shouldGoToTodayBeVisible() = currentWeekTS != thisWeekTS

    override fun updateActionBarTitle() {
        setupWeeklyActionbarTitle(currentWeekTS)
    }

    override fun getNewEventDayCode(): String {
        val currentTS = System.currentTimeMillis() / 1000
        return if (currentTS > currentWeekTS && currentTS < currentWeekTS + WEEK_SECONDS) {
            Formatter.getTodayCode()
        } else {
            Formatter.getDayCodeFromTS(currentWeekTS)
        }
    }

    override fun printView() {
        TODO("Not yet implemented")
    }

    override fun scrollTo(y: Int) {
        weekHolder!!.week_view_hours_scrollview.scrollY = y
        weekScrollY = y
    }

    override fun updateHoursTopMargin(margin: Int) {
        weekHolder?.apply {
            week_view_hours_divider?.layoutParams?.height = margin
            week_view_hours_scrollview?.requestLayout()
            week_view_hours_scrollview?.onGlobalLayout {
                week_view_hours_scrollview.scrollY = weekScrollY
            }
        }
    }

    override fun getCurrScrollY() = weekScrollY

    override fun updateRowHeight(rowHeight: Int) {
        val childCnt = weekHolder!!.week_view_hours_holder.childCount
        for (i in 0..childCnt) {
            val textView =
                weekHolder!!.week_view_hours_holder.getChildAt(i) as? TextView ?: continue
            textView.layoutParams.height = rowHeight
        }

        weekHolder!!.week_view_hours_holder.setPadding(0, 0, 0, rowHeight)
        (viewPager!!.adapter as? MyWeekPagerAdapter)?.updateNotVisibleScaleLevel(viewPager!!.currentItem)
    }

    override fun getFullFragmentHeight() = weekHolder!!.week_view_holder.height

    private fun attemptWeekViewPagerUpdate(previousButtonClicked: Boolean) {
        val weekTSs = if (previousButtonClicked) {
            getWeekTimestamps(currentWeekTS - 100000)
        } else {
            getWeekTimestamps(currentWeekTS + 100000)
        }
        val weeklyAdapter =
            MyWeekPagerAdapter(requireActivity().supportFragmentManager, weekTSs, this)

        defaultWeeklyPage = weekTSs.size / 2

        viewPager!!.apply {
            adapter = weeklyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                }

                override fun onPageSelected(position: Int) {
                    currentWeekTS = weekTSs[position]
                    val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                    if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                        isGoToTodayVisible = shouldGoToTodayBeVisible
                    }

                    setupWeeklyActionbarTitle(weekTSs[position])
                }
            })
            currentItem = defaultWeeklyPage
        }

        weekHolder!!.week_view_hours_scrollview.setOnScrollviewListener(object :
            MyScrollView.ScrollViewListener {
            override fun onScrollChanged(
                scrollView: MyScrollView,
                x: Int,
                y: Int,
                oldX: Int,
                oldY: Int
            ) {
                weekScrollY = y
                weeklyAdapter.updateScrollY(viewPager!!.currentItem, y)
            }
        })
    }

    companion object {
        private const val PREFILLED_WEEKS = 151
    }

}

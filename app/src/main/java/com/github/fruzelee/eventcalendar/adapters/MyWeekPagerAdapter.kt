package com.github.fruzelee.eventcalendar.adapters

import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.github.fruzelee.eventcalendar.fragments.WeekFragment
import com.github.fruzelee.eventcalendar.helpers.WEEK_START_TIMESTAMP
import com.github.fruzelee.eventcalendar.interfaces.WeekFragmentListener

@Suppress("DEPRECATION")
class MyWeekPagerAdapter(
    fm: FragmentManager,
    private val mWeekTimestamps: List<Long>,
    private val mListener: WeekFragmentListener
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val mFragments = SparseArray<WeekFragment>()

    override fun getCount() = mWeekTimestamps.size

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val weekTimestamp = mWeekTimestamps[position]
        bundle.putLong(WEEK_START_TIMESTAMP, weekTimestamp)

        val fragment = WeekFragment()
        fragment.arguments = bundle
        fragment.listener = mListener

        mFragments.put(position, fragment)
        return fragment
    }

    fun updateScrollY(pos: Int, y: Int) {
        mFragments[pos - 1]?.updateScrollY(y)
        mFragments[pos + 1]?.updateScrollY(y)
    }

    fun updateCalendars(pos: Int) {
        for (i in -1..1) {
            mFragments[pos + i]?.updateCalendar()
        }
    }

    fun updateNotVisibleScaleLevel(pos: Int) {
        mFragments[pos - 1]?.updateNotVisibleViewScaleLevel()
        mFragments[pos + 1]?.updateNotVisibleViewScaleLevel()
    }

}

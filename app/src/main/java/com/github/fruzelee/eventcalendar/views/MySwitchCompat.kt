package com.github.fruzelee.eventcalendar.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.github.fruzelee.eventcalendar.R
import com.github.fruzelee.eventcalendar.extensions.adjustAlpha

class MySwitchCompat : SwitchCompat {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setColors(textColor: Int, accentColor: Int) {
        setTextColor(textColor)
        val states = arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked))
        val thumbColors = intArrayOf(ContextCompat.getColor(context, R.color.thumb_deactivated), accentColor)
        val trackColors = intArrayOf(ContextCompat.getColor(context, R.color.track_deactivated), accentColor.adjustAlpha(0.3f))
        DrawableCompat.setTintList(DrawableCompat.wrap(thumbDrawable), ColorStateList(states, thumbColors))
        DrawableCompat.setTintList(DrawableCompat.wrap(trackDrawable), ColorStateList(states, trackColors))
    }
}

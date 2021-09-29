package com.github.fruzelee.eventcalendar.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import com.github.fruzelee.eventcalendar.R

class MyCompatRadioButton : AppCompatRadioButton {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    @SuppressLint("RestrictedApi")
    fun setColors(textColor: Int, accentColor: Int) {
        setTextColor(textColor)
        val colorStateList = ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_checked),
                        intArrayOf(android.R.attr.state_checked)
                ),
                intArrayOf(ContextCompat.getColor(context,R.color.radiobutton_disabled), accentColor)
        )
        supportButtonTintList = colorStateList
    }
}

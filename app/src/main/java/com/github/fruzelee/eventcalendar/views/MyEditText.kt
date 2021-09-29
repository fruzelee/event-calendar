package com.github.fruzelee.eventcalendar.views

import android.content.Context
import android.util.AttributeSet
import com.github.fruzelee.eventcalendar.extensions.adjustAlpha
import com.github.fruzelee.eventcalendar.extensions.applyColorFilter
import com.github.fruzelee.eventcalendar.helpers.MEDIUM_ALPHA

class MyEditText : androidx.appcompat.widget.AppCompatEditText {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setColors(textColor: Int, accentColor: Int) {
        background?.mutate()?.applyColorFilter(accentColor)

        // requires android:textCursorDrawable="@null" in xml to color the cursor too
        setTextColor(textColor)
        setHintTextColor(textColor.adjustAlpha(MEDIUM_ALPHA))
        setLinkTextColor(accentColor)
    }
}

package com.github.fruzelee.eventcalendar.extensions

import android.graphics.PorterDuff
import android.widget.ImageView

fun ImageView.applyColorFilter(color: Int) = setColorFilter(color, PorterDuff.Mode.SRC_IN)

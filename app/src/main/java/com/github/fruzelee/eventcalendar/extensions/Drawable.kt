package com.github.fruzelee.eventcalendar.extensions

import android.graphics.PorterDuff
import android.graphics.drawable.Drawable

@Suppress("DEPRECATION")
fun Drawable.applyColorFilter(color: Int) = mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN)


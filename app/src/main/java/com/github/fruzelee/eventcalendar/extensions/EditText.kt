package com.github.fruzelee.eventcalendar.extensions

import android.widget.EditText

val EditText.value: String get() = text.toString().trim()


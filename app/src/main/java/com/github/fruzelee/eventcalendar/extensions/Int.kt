package com.github.fruzelee.eventcalendar.extensions

import android.graphics.Color
import com.github.fruzelee.eventcalendar.helpers.DARK_GREY
import com.github.fruzelee.eventcalendar.helpers.MONTH
import com.github.fruzelee.eventcalendar.helpers.WEEK
import com.github.fruzelee.eventcalendar.helpers.YEAR
import java.util.*
import kotlin.math.roundToInt

fun Int.isXWeeklyRepetition() = this != 0 && this % WEEK == 0

fun Int.isXMonthlyRepetition() = this != 0 && this % MONTH == 0

fun Int.isXYearlyRepetition() = this != 0 && this % YEAR == 0

fun Int.getContrastColor(): Int {
    val y = (299 * Color.red(this) + 587 * Color.green(this) + 114 * Color.blue(this)) / 1000
    return if (y >= 149 && this != Color.BLACK) DARK_GREY else Color.WHITE
}

fun Int.toHex() = String.format("#%06X", 0xFFFFFF and this).uppercase(Locale.getDefault())

fun Int.adjustAlpha(factor: Float): Int {
    val alpha = (Color.alpha(this) * factor).roundToInt()
    val red = Color.red(this)
    val green = Color.green(this)
    val blue = Color.blue(this)
    return Color.argb(alpha, red, green, blue)
}

fun Int.addBitIf(add: Boolean, bit: Int) =
    if (add) {
        addBit(bit)
    } else {
        removeBit(bit)
    }

// TODO: how to do "bits & ~bit" in kotlin?
fun Int.removeBit(bit: Int) = addBit(bit) - bit

fun Int.addBit(bit: Int) = this or bit

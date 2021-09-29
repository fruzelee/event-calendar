package com.github.fruzelee.eventcalendar.helpers

/**
 * @author fazle
 * Created 9/25/2021 at 12:22 PM
 * github.com/fruzelee
 * web: fr.crevado.com
 */
import android.database.Cursor

fun Cursor.getStringValue(key: String): String = getString(getColumnIndex(key))

fun Cursor.getIntValue(key: String) = getInt(getColumnIndex(key))

fun Cursor.getLongValue(key: String) = getLong(getColumnIndex(key))
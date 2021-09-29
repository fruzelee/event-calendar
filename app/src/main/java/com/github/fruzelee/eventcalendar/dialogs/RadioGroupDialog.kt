package com.github.fruzelee.eventcalendar.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import com.github.fruzelee.eventcalendar.R

@SuppressLint("InflateParams")
class RadioGroupDialog(
    val activity: Activity,
    val callback: (newValue: Any) -> Unit
) {
    //private val dialog: AlertDialog
    private var wasInit = false

    init {
        activity.layoutInflater.inflate(R.layout.dialog_radio_group, null)
        wasInit = true
    }

}

package com.github.fruzelee.eventcalendar.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.github.fruzelee.eventcalendar.R
import com.github.fruzelee.eventcalendar.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_message.view.*

// similar fo ConfirmationDialog, but has a callback for negative button too
@SuppressLint("InflateParams")
class ConfirmationAdvancedDialog(activity: Activity, message: String = "", messageId: Int = R.string.proceed_with_deletion, positive: Int = R.string.yes,
                                 negative: Int, val callback: (result: Boolean) -> Unit) {
    private var dialog: AlertDialog

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_message, null)
        view.message.text = if (message.isEmpty()) activity.resources.getString(messageId) else message

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(positive) { _, _ -> positivePressed() }
                .setNegativeButton(negative) { _, _ -> negativePressed() }
                .create().apply {
                    activity.setupDialogStuff(view, this)
                }
    }

    private fun positivePressed() {
        dialog.dismiss()
        callback(true)
    }

    private fun negativePressed() {
        dialog.dismiss()
        callback(false)
    }
}

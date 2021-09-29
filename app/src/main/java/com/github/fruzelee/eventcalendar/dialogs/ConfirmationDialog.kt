package com.github.fruzelee.eventcalendar.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.github.fruzelee.eventcalendar.R
import com.github.fruzelee.eventcalendar.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_message.view.*

/**
 * A simple dialog without any view, just a messageId, a positive button and optionally a negative button
 *
 * @param activity has to be activity context to avoid some Theme.AppCompat issues
 * @param message the dialogs message, can be any String. If empty, messageId is used
 * @param messageId the dialogs messageId ID. Used only if message is empty
 * @param positive positive buttons text ID
 * @param negative negative buttons text ID (optional)
 * @param callback an anonymous function
 */
@SuppressLint("InflateParams")
class ConfirmationDialog(activity: Activity, message: String = "", messageId: Int = R.string.proceed_with_deletion, positive: Int = R.string.yes,
                         negative: Int = R.string.no, val callback: () -> Unit) {
    private var dialog: AlertDialog

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_message, null)
        view.message.text = if (message.isEmpty()) activity.resources.getString(messageId) else message

        val builder = AlertDialog.Builder(activity)
                .setPositiveButton(positive) { _, _ -> dialogConfirmed() }

        if (negative != 0)
            builder.setNegativeButton(negative, null)

        dialog = builder.create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    private fun dialogConfirmed() {
        dialog.dismiss()
        callback()
    }
}

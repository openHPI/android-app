package de.xikolo.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import de.xikolo.utils.NotificationUtil

class NotificationDeletedReceiver : BroadcastReceiver() {

    companion object {
        const val INTENT_ACTION_NOTIFICATION_DELETED = "de.xikolo.intent.action.NOTIFICATION_DELETED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (INTENT_ACTION_NOTIFICATION_DELETED == action) {
            NotificationUtil.getInstance(context).deleteDownloadNotificationsFromIntent(intent)
        }
    }

}

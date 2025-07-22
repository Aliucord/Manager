package com.aliucord.manager.ui.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.*
import com.aliucord.manager.*

object InstallNotifications {
    private const val CHANNEL_ID = "installation"

    /**
     * Creates or replaces a notification with id [id] that brings
     * up the existing [MainActivity] when clicked upon.
     *
     * @param id A unique notification ID for different notifications
     * @param title Main notification title
     * @param description Notification description
     */
    fun createNotification(
        context: Context,
        id: Int,
        @StringRes title: Int,
        @StringRes description: Int,
    ) {
        val manager = NotificationManagerCompat.from(context)

        // Create the target notification channel
        if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.notif_group_install_title))
                .setDescription(context.getString(R.string.notif_group_install_desc))
                .build()

            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_aliucord_logo)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(description))
            .setContentIntent(
                PendingIntent.getActivity(
                    /* context = */ context,
                    /* requestCode = */ 0,
                    /* intent = */
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT),
                    /* flags = */ PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()

        try {
            manager.notify(id, notification)
        } catch (e: SecurityException) {
            Log.w(BuildConfig.TAG, "Failed to send install notification", e)
        }
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 equwal
package dev.equwal.inkupdate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * One notification for each app that has an update.
 *
 * The id comes from the package name, so a later check replaces the old
 * notification instead of adding a second one. The caller tells a person about
 * a version once. Nothing else makes a notification.
 */
object Notify {

    private const val CHANNEL = "updates"

    /**
     * Tells the person about one update.
     *
     * @return true when the notification went out. It does not go out while
     *         the person keeps notifications off. The caller must then ask
     *         again after the next check, so the update is not lost.
     */
    fun update(context: Context, w: Watched, r: Result.Update): Boolean {
        val m = context.getSystemService(NotificationManager::class.java) ?: return false
        if (!m.areNotificationsEnabled()) return false
        channel(m, context)

        val b = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(w.title + " " + r.versionName + " is ready")
            .setContentText("From " + r.source)
            .setAutoCancel(true)
            .setContentIntent(open(context, r.page, w.pkg.hashCode()))

        if (r.apk != null) {
            b.addAction(
                Notification.Action.Builder(
                    null as android.graphics.drawable.Icon?,
                    "Download",
                    open(context, r.apk, w.pkg.hashCode() + 1)
                ).build()
            )
        }
        m.notify(w.pkg.hashCode(), b.build())
        return true
    }

    private fun channel(m: NotificationManager, context: Context) {
        m.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.channel_updates),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    /** Opens a page in whatever app handles it: F-Droid, Play, or a browser. */
    private fun open(context: Context, url: String, id: Int): PendingIntent {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context, id, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

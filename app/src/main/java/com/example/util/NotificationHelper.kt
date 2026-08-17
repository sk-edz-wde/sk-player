package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AppNotification

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val ADMIN_CHANNEL_ID = "sk_admin_broadcast_channel"
    private const val ADMIN_CHANNEL_NAME = "SK Admin Broadcasts & Announcements"
    private const val PREFS_NAME = "sk_notification_prefs"
    private const val KEY_FIRST_LAUNCH_TIME = "app_first_launch_time"
    private const val KEY_SEEN_NOTIFS = "seen_notif_ids"
    private const val KEY_DELETED_NOTIFS = "deleted_notif_ids"
    private const val KEY_POSTED_SYSTEM_NOTIFS = "posted_system_notif_ids"

    fun getFirstLaunchTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var launchTime = prefs.getLong(KEY_FIRST_LAUNCH_TIME, 0L)
        if (launchTime == 0L) {
            launchTime = System.currentTimeMillis()
            prefs.edit().putLong(KEY_FIRST_LAUNCH_TIME, launchTime).apply()
        }
        return launchTime
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ADMIN_CHANNEL_ID,
                ADMIN_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Broadcasts, alerts, and music updates from SK Edz Admin"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a system status bar notification for newly received announcements.
     * Guarantees 1-time delivery: Old notifications created before app install/first run
     * are strictly filtered out and never spammed on startup/reinstall.
     */
    fun showAdminNotification(context: Context, notif: AppNotification) {
        if (notif.id.isBlank() || isSystemNotificationPosted(context, notif.id) || isNotificationDeleted(context, notif.id)) {
            return
        }

        val firstLaunch = getFirstLaunchTime(context)
        // If notification timestamp is older than first launch (or older than 1 minute before first launch),
        // mark it silently as processed so it NEVER alerts the status bar.
        if (notif.timestamp > 0 && notif.timestamp < (firstLaunch - 60_000L)) {
            markSystemNotificationPosted(context, notif.id)
            return
        }

        try {
            createNotificationChannels(context)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("extra_notif_id", notif.id)
                putExtra("extra_song_id", notif.songId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notif.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val displayTitle = notif.title.ifBlank { "SK EDZ Notification" }
            val displayMessage = (notif.message.ifBlank { notif.body }).ifBlank { "New announcement from SK Admin" }

            val largeIcon = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
            } catch (e: Exception) {
                null
            }

            val builder = NotificationCompat.Builder(context, ADMIN_CHANNEL_ID)
                .setSmallIcon(R.drawable.app_logo)
                .setContentTitle(displayTitle)
                .setContentText(displayMessage)
                .setStyle(NotificationCompat.BigTextStyle().bigText(displayMessage))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon)
            }

            val notifId = (notif.id.hashCode() and 0x7FFFFFFF).coerceAtLeast(1000)
            manager.notify(notifId, builder.build())
            markSystemNotificationPosted(context, notif.id)
            Log.d(TAG, "Admin notification posted to status bar: $displayTitle (ID: $notifId)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show admin notification", e)
        }
    }

    private fun isSystemNotificationPosted(context: Context, notifId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val postedSet = prefs.getStringSet(KEY_POSTED_SYSTEM_NOTIFS, emptySet()) ?: emptySet()
        return postedSet.contains(notifId)
    }

    fun markSystemNotificationPosted(context: Context, notifId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val postedSet = (prefs.getStringSet(KEY_POSTED_SYSTEM_NOTIFS, emptySet()) ?: emptySet()).toMutableSet()
        postedSet.add(notifId)
        prefs.edit().putStringSet(KEY_POSTED_SYSTEM_NOTIFS, postedSet).apply()
    }

    fun isNotificationSeen(context: Context, notifId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seenSet = prefs.getStringSet(KEY_SEEN_NOTIFS, emptySet()) ?: emptySet()
        return seenSet.contains(notifId)
    }

    fun markNotificationSeen(context: Context, notifId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seenSet = (prefs.getStringSet(KEY_SEEN_NOTIFS, emptySet()) ?: emptySet()).toMutableSet()
        seenSet.add(notifId)
        prefs.edit().putStringSet(KEY_SEEN_NOTIFS, seenSet).apply()
    }

    fun markAllNotificationsSeen(context: Context, notifIds: List<String>) {
        if (notifIds.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seenSet = (prefs.getStringSet(KEY_SEEN_NOTIFS, emptySet()) ?: emptySet()).toMutableSet()
        seenSet.addAll(notifIds)
        prefs.edit().putStringSet(KEY_SEEN_NOTIFS, seenSet).apply()
    }

    fun isNotificationDeleted(context: Context, notifId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deletedSet = prefs.getStringSet(KEY_DELETED_NOTIFS, emptySet()) ?: emptySet()
        return deletedSet.contains(notifId)
    }

    fun deleteNotification(context: Context, notifId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deletedSet = (prefs.getStringSet(KEY_DELETED_NOTIFS, emptySet()) ?: emptySet()).toMutableSet()
        deletedSet.add(notifId)
        prefs.edit().putStringSet(KEY_DELETED_NOTIFS, deletedSet).apply()
    }

    fun clearAllNotifications(context: Context, notifIds: List<String>) {
        if (notifIds.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deletedSet = (prefs.getStringSet(KEY_DELETED_NOTIFS, emptySet()) ?: emptySet()).toMutableSet()
        deletedSet.addAll(notifIds)
        prefs.edit().putStringSet(KEY_DELETED_NOTIFS, deletedSet).apply()
    }
}


package com.example.boostx

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat.Action
import android.graphics.BitmapFactory


class BoostService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Create and activate media session for better TV integration
        mediaSession = MediaSessionCompat(this, "BoostXSession")
        mediaSession?.isActive = true

        val notification = buildNotification()
        startForeground(NOTIF_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle stop action from notification
        if (intent?.action == ACTION_STOP) {
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        // Service is sticky so system will try to keep it running
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, openIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Stop action
        val stopIntent = Intent(this, BoostService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val stopAction = NotificationCompat.Action.Builder(
            R.drawable.ic_close_black_24, // may fall back to default if resource missing
            "Stop",
            stopPending
        ).build()

        val style = NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BoostX running")
            .setContentText("Audio boost service is active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pending)
            .setOngoing(true)
            .addAction(stopAction)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BoostX Service",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Foreground service for BoostX"
            nm.createNotificationChannel(channel)
        }
    }

    // Utility: read saved boost level
    fun getSavedBoost(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_BOOST, 0)
    }

    private var mediaSession: MediaSessionCompat? = null

    companion object {
        const val CHANNEL_ID = "boostx_foreground"
        const val NOTIF_ID = 101
        const val PREFS_NAME = "boost_prefs"
        const val KEY_BOOST = "boost_level"
        const val ACTION_STOP = "com.example.boostx.action.STOP"
    }
}

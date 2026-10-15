package com.remixlab.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // کانال ضبط
            val recordingChannel = NotificationChannel(
                CHANNEL_RECORDING,
                "ضبط صدا",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "اطلاع‌رسانی هنگام ضبط صدا"
                setShowBadge(false)
            }

            // کانال پخش
            val playbackChannel = NotificationChannel(
                CHANNEL_PLAYBACK,
                "پخش موسیقی",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "کنترل پخش موسیقی"
                setShowBadge(false)
            }

            manager.createNotificationChannels(
                listOf(recordingChannel, playbackChannel)
            )

            Log.i("RemixLab", "Notification channels created")
        }
    }

    companion object {
        const val CHANNEL_RECORDING = "remixlab_recording"
        const val CHANNEL_PLAYBACK = "remixlab_playback"
    }
}

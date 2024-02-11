package com.example.mobile.misc

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mobile.MainActivity
import com.example.mobile.R

object NotificationHelper {
    private const val DEFAULT_CHANNEL = "DEFAULT_CHANNEL"
    private const val REQUEST_CODE = 0
    public const val extraEndpointId = "endpointId"

    private lateinit var notificationManager: NotificationManager
    private lateinit var appContext: Context

    fun init(appContext: Context) {
        notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        this.appContext = appContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Your Channel Name"
            val channelDescription = "Your Channel Description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(DEFAULT_CHANNEL, channelName, importance).apply {
                    description = channelDescription
                }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun generateNotificationId(): Int {
        // generates a unique ID using the current timestamp
        return System.currentTimeMillis().toInt()
    }

    private fun startNotificationIntent(title: String, content: String, intent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(appContext, DEFAULT_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(appContext)) {
            if (ActivityCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify( generateNotificationId(), notification)
            }
        }
    }

    fun sendNotification(title: String, content: String) {
        val intent = Intent(appContext, MainActivity::class.java)
        startNotificationIntent(title, content, intent)
    }

    fun sendProximityNotification(
        title: String,
        content: String,
        endpointId: String
    ) {
        val intent = Intent(appContext, MainActivity::class.java)
        intent.putExtra(extraEndpointId, endpointId)

        startNotificationIntent(title, content, intent)
    }
}
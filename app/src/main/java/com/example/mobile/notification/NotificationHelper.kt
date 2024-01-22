package com.example.mobile.notification

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
    private const val PROXIMITY_SHARE_CHANNEL = "PROXIMITY_SHARE_CHANNEL"
    private const val REQUEST_CODE = 0
    public const val extraEndpointId = "endpointId"

    private lateinit var notificationManager: NotificationManager

    fun init(applicationContext: Context) {
        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Your Channel Name"
            val channelDescription = "Your Channel Description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(PROXIMITY_SHARE_CHANNEL, channelName, importance).apply {
                    description = channelDescription
                }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun generateNotificationId(): Int {
        // generates a unique ID using the current timestamp
        return System.currentTimeMillis().toInt()
    }

    fun sendNotification(
        title: String,
        content: String,
        context: Context,
        endpointId: String
    ) {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(extraEndpointId, endpointId)
        val pendingIntent = PendingIntent.getActivity(
            context, REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, PROXIMITY_SHARE_CHANNEL)
            //TODO: check this icon
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify( generateNotificationId(), builder.build())
            }
        }
    }
}
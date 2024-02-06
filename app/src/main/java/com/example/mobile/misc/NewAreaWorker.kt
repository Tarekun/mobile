package com.example.mobile.misc

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.mobile.R
import com.example.mobile.database.MeasurementsUtils
import com.example.mobile.database.SettingsUtils

class NewAreaWorker(private val appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val currentLatitude = LocationManager.latitude
        val currentLongitude = LocationManager.longitude

        fun needToNotify(): Boolean {
            val settings = SettingsUtils.storedSettings
            //TODO: define conversion int[m] -> double[lat/long coordinate]
            val convertedLength: Double = settings.gridUnitLength.toDouble()
            val top = currentLatitude + convertedLength
            val bottom = currentLatitude - convertedLength
            val right = currentLongitude + convertedLength
            val left = currentLongitude - convertedLength

            //TODO: refine this logic to support filtering with the rest of the settings
            return MeasurementsUtils.isNewArea(top, bottom, left, right)
        }

        if (needToNotify()) {
            NotificationHelper.sendNotification(
                appContext.getString(R.string.notification_title_new_area),
                appContext.getString(R.string.notification_content_new_area),
            )
        }

        return Result.success()
    }
}
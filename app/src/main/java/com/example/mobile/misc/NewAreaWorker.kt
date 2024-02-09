package com.example.mobile.misc

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.mobile.R
import com.example.mobile.database.MeasurementsUtils
import com.example.mobile.database.SettingsUtils
import com.example.mobile.monitors.MonitorVariant
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

class NewAreaWorker(private val appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val currentLatitude = LocationManager.latitude
        val currentLongitude = LocationManager.longitude

        fun needToNotify(): Boolean {
            val settings = SettingsUtils.storedSettings
            // check if notifications are enabled
            if (!settings.notifyInNewArea) {
                return false
            }
            val notificationPeriod: Duration = settings.notificationPeriodMs.milliseconds
            // check if enought time has passed
            if (Clock.System.now() - settings.lastNotification < notificationPeriod) {
                return false
            }

            // define the area with the proper size
            //TODO: define conversion int[m] -> double[lat/long coordinate]
            val convertedLength: Double = settings.notifyOnlyAboveLength.toDouble()
            val top = currentLatitude + convertedLength
            val bottom = currentLatitude - convertedLength
            val right = currentLongitude + convertedLength
            val left = currentLongitude - convertedLength

            return if (settings.notifyOnlyAllMonitors) {
                MeasurementsUtils.isNewAreaForMonitor(top, bottom, left, right, MonitorVariant.AUDIO) &&
                MeasurementsUtils.isNewAreaForMonitor(top, bottom, left, right, MonitorVariant.WIFI) &&
                MeasurementsUtils.isNewAreaForMonitor(top, bottom, left, right, MonitorVariant.LTE)
            } else {
                MeasurementsUtils.isNewArea(top, bottom, left, right)
            }
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
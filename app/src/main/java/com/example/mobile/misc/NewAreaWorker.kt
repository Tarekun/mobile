package com.example.mobile.misc

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.mobile.R
import com.example.mobile.database.MeasurementsUtils
import com.example.mobile.database.SettingsUtils
import com.example.mobile.map.moveLatitude
import com.example.mobile.map.moveLongitude
import com.example.mobile.monitors.MonitorVariant
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

class NewAreaWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
    private val locationTracker: LocationTracker
): Worker(appContext, workerParams) {

    override fun doWork(): Result {
        if (locationTracker.isWorking()) {
            val currentLatitude = locationTracker.latitude
            val currentLongitude = locationTracker.longitude

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
                val top = moveLatitude(currentLatitude, settings.notifyOnlyAboveLength.toDouble())
                val bottom = moveLatitude(currentLatitude, -settings.notifyOnlyAboveLength.toDouble())
                val right = moveLongitude(
                    currentLongitude,
                    settings.notifyOnlyAboveLength.toDouble(),
                    currentLatitude
                )
                val left = moveLongitude(
                    currentLongitude,
                    -settings.notifyOnlyAboveLength.toDouble(),
                    currentLatitude
                )

                return if (settings.notifyOnlyAllMonitors) {
                    MeasurementsUtils.isNewAreaForMonitor(
                        top,
                        bottom,
                        left,
                        right,
                        MonitorVariant.AUDIO
                    ) &&
                            MeasurementsUtils.isNewAreaForMonitor(
                                top,
                                bottom,
                                left,
                                right,
                                MonitorVariant.WIFI
                            ) &&
                            MeasurementsUtils.isNewAreaForMonitor(
                                top,
                                bottom,
                                left,
                                right,
                                MonitorVariant.LTE
                            )
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
        }

        return Result.success()
    }
}
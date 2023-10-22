package com.example.mobile.FileSaving

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

enum class MonitorType {
    AUDIO, WIFI
}


class FileManager(private val context: Context) {

    private val audioFileName = "audio_data.txt"
    private val wifiFileName = "wifi_data.txt"

    fun saveData(monitorType: MonitorType, data: String) {
        val fileName = when (monitorType) {
            MonitorType.AUDIO -> audioFileName
            MonitorType.WIFI -> wifiFileName
        }

        val file = File(context.filesDir, fileName)

        FileOutputStream(file, true).use { fos ->
            PrintWriter(fos).use { pw ->
                pw.println(data)
            }
        }
    }

    fun classifyValue(value: Double, type: MonitorType): String {
        return when (type) {
            MonitorType.AUDIO -> {
                when {
                    value < -70 -> "quiet"
                    value < -30 -> "average"
                    else -> "loud"
                }
            }
            MonitorType.WIFI -> {
                when {
                    value < -85 -> "poor"
                    value < -65 -> "average"
                    else -> "good"
                }
            }
        }
    }

}

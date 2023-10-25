package com.example.mobile.monitors

import android.content.ContentValues
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.example.mobile.FileSaving.FileManager
import com.example.mobile.FileSaving.MonitorType
import kotlin.math.log10
import kotlin.math.sqrt
import database.DatabaseHelper


class AudioMonitor(private val context: Context): IMonitor {
    private var fileManager = FileManager(context)
    private val databaseHelper = DatabaseHelper(context)
    private val db = databaseHelper.writableDatabase
    private var audioRecorder: AudioRecord? = null
    private val sampleFrequency = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleFrequency,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    companion object {
        // periodo di esecuzione delle misurazioni suggerito
        const val defaultTimePeriodMs: Long = 1000
        // massima ampiezza possibile con un encoding a 16bit
        const val maxPossibleAmplitude: Double = 32767.0
    }

    @RequiresPermission(value = "android.permission.RECORD_AUDIO")
    override fun startMonitoring(onStart: () -> Unit) {
        audioRecorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleFrequency,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecorder?.startRecording()
        onStart()
    }

    override fun stopMonitoring() {
        //TODO: maybe protect this function so that it throws if start wasn't called yet
        audioRecorder?.stop()
        audioRecorder?.release()
        audioRecorder = null
    }

    override fun readValue(): Double {
        val buffer = ShortArray(bufferSize)
        audioRecorder?.read(buffer, 0, bufferSize)
        // i dB sono calcolati come dBFS dato che stiamo lavorando con segnali digitali
        // reference: https://en.m.wikipedia.org/wiki/DBFS
        val rms = rootMeanSquared(buffer)
        val decibelValue = decibelFromRms(rms)
        //TODO filemanager era la vecchia classe perscrivere in file( ora su db) ma il metodo classifyvalue è ancora li-> spostarlo
        val classification = fileManager.classifyValue(decibelValue,MonitorType.AUDIO)
        //fileManager.saveData(MonitorType.AUDIO, "$decibelValue ($classification)")
        val values = ContentValues().apply {
            put("valore", decibelValue) // inserisci il valore desiderato per il campo 'valore'
            put("classificazione", classification) // inserisci il valore desiderato per il campo 'classificazione'
        }

        val newRowId = db.insert("audio", null, values)

        db.close()
        return decibelValue
    }

    private fun rootMeanSquared(values: ShortArray): Double {
        var sum = 0.0
        for (value in values) {
            sum += value * value
        }
        return sqrt(sum / values.size)
    }

    private fun decibelFromRms(rms: Double): Double {
        // si moltiplica per 20 perché usiamo lo scarto quadratico medio di più misurazioni, invece
        // di una sola misurazione o una semplice media
        // reference: https://en.wikipedia.org/wiki/Decibel#Uses
        return 20 * log10(rms / maxPossibleAmplitude)
    }
}
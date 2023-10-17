package com.example.mobile.monitors

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlin.math.log10
import kotlin.math.sqrt

class AudioMonitor(): IMonitor {
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
    override fun startMonitoring() {
        audioRecorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleFrequency,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecorder?.startRecording()
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
        return decibelFromRms(rms)
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
package com.example.mobile.monitors

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.example.mobile.database.Classification
import kotlin.math.log10
import kotlin.math.sqrt

class AudioMonitor(context: Context): Monitor(context) {
    private var audioRecorder: AudioRecord? = null
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleFrequency,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    companion object {
        // massima ampiezza possibile con un encoding a 16bit
        const val maxPossibleAmplitude: Double = 32767.0
        const val sampleFrequency = 44100
    }

    @RequiresPermission(value = "android.permission.RECORD_AUDIO")
    override fun doStartMonitoring(onStart: () -> Unit): Boolean {
        return checkStateOrFail(
            MonitorState.CREATED,
            {
                audioRecorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleFrequency,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                audioRecorder?.startRecording()
                onStart()
                true
            }
        )
    }

    override fun doStopMonitoring(): Boolean {
        return checkStateOrFail(
            MonitorState.STARTED,
            {
                audioRecorder?.stop()
                audioRecorder?.release()
                audioRecorder = null
                true
            }
        )
    }

    override fun doReadValue(): Double {
        val buffer = ShortArray(bufferSize)
        audioRecorder?.read(buffer, 0, bufferSize)
        // i dB sono calcolati come dBFS dato che stiamo lavorando con segnali digitali
        // reference: https://en.m.wikipedia.org/wiki/DBFS
        val rms = rootMeanSquared(buffer)

        return decibelFromRms(rms)
    }

    override fun classifySignalStrength(dB: Double): Classification {
        return when(dB) {
            in -3.0..0.0 -> Classification.MAX
            in -24.0..-3.0 -> Classification.HIGH
            in -40.0..-24.0 -> Classification.MEDIUM
            in -60.0..-40.0 -> Classification.LOW
            in Double.NEGATIVE_INFINITY..-60.0 -> Classification.MIN
            else -> Classification.INVALID
        }
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
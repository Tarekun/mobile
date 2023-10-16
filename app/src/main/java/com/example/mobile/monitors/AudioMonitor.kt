package com.example.mobile.monitors

import android.content.Context
import android.media.MediaRecorder
import android.os.Build

class AudioMonitor(
    private val context: Context
): IMonitor {
    private var recorder: MediaRecorder? = null

    /**
     * Nasconde la chiamata al costruttore di MediaRecorder corrispondente alla versione del
     * SDK in uso sul dispositivo
     */
    private fun createRecorder(): MediaRecorder {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()
    }

    override fun startMonitoring() {
        createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            //TODO: ideally you want this to be /dev/null as we're not interested in keeping the data
            //it's too bad that /dev/null is not supported for MediaRecorder and the damned exception message doesn't tell you
            //post about the issue https://stackoverflow.com/questions/65810110/what-is-android-11s-equivalent-of-dev-null
            //also have a look into changing the class used from MediaRecorder to AudioRecord
            setOutputFile("${context.externalCacheDir?.absolutePath}/test.3gp")

            prepare()
            start()

            recorder = this
        }
    }

    override fun stopMonitoring() {
        //TODO: maybe protect this function so that it throws if start wasn't called yet
        recorder?.stop()
        recorder?.reset()
        recorder?.release()
        recorder = null
    }

    override fun readValue(): Int {
        //TODO: implement conversion to db
        return recorder?.getMaxAmplitude() ?: 0
    }
}
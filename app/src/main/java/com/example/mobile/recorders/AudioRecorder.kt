package com.example.mobile.recorders

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.FileOutputStream

class AudioRecorder(
    private val context: Context
) {
    private var recorder: MediaRecorder? = null

    private fun createRecorder(): MediaRecorder {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()
    }

    fun start(outputFile: File) {
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

    fun stop() {
        //notice that if you call stop() before having called start() an exception will be raised
        recorder?.stop()
        recorder?.reset()
        recorder = null
    }

    fun read(): Int {
//        recorder?.start()
        val value = recorder?.getMaxAmplitude() ?: 0
        recorder?.pause()
        return value
    }
}
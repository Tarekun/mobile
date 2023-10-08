package com.example.mobile.recorders

import android.media.MediaRecorder

class AudioRecorder {
    val maxVal: Int = MediaRecorder.getAudioSourceMax()
    val mediaRecorder: MediaRecorder

    init {
        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder?.setOutputFile("/dev/null");
        mediaRecorder?.prepare();
    }

    fun readValue(): Int {
        mediaRecorder.start()
        val value = mediaRecorder.getMaxAmplitude()
        mediaRecorder.pause()
        return value
    }
}
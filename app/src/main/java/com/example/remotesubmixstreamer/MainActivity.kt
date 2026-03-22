package com.example.remotesubmixstreamer

import android.media.*
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val TAG = "SubmixTest"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "Starting AudioRecord test...")

        val sampleRate = 48000
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        )

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.REMOTE_SUBMIX,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize * 2
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord init FAILED")
            return
        }

        audioRecord.startRecording()
        Log.d(TAG, "Recording started")

        thread {
            val buffer = ByteArray(bufferSize)

            while (true) {
                val read = audioRecord.read(buffer, 0, buffer.size)

                if (read > 0) {
                    // Check if buffer has non-zero data
                    var nonZero = false
                    for (i in 0 until read) {
                        if (buffer[i].toInt() != 0) {
                            nonZero = true
                            break
                        }
                    }

                    if (nonZero) {
                        Log.d(TAG, "🔥 AUDIO DATA FLOWING ($read bytes)")
                    } else {
                        Log.d(TAG, "…silence ($read bytes)")
                    }
                } else {
                    Log.e(TAG, "Read failed: $read")
                }
            }
        }
    }
}

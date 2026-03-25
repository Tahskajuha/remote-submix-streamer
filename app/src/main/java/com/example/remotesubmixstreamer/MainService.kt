package com.example.remotesubmixstreamer

import android.media.*
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.MutableStateFlow
import eu.buney.kopus.*
import android.app.Service
import android.util.Log
import android.content.Intent
import android.os.IBinder

class StreamerService : Service() {
	companion object {
		@Volatile private var running = false
		val isRunningFlow = MutableStateFlow(false)
	}
	override fun onBind(intent: Intent?): IBinder? = null
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (running) {
			return START_STICKY
		}

		val prefs = getSharedPreferences("config", MODE_PRIVATE)

		val ip = intent?.getStringExtra("ip")
		val port = intent?.getIntExtra("port", 0) ?: 0

		val finalIp: String?
		val finalPort: Int

		if (!ip.isNullOrBlank() && port != 0) {
			finalIp = ip
			finalPort = port

			prefs.edit().putString("ip", ip).putInt("port", port).apply()
		} else if (prefs.getString("ip", null) != null && prefs.getInt("port", 0) != 0){
			finalIp = prefs.getString("ip", null)
			finalPort = prefs.getInt("port", 0)
		} else {
			Log.e("StreamerService", "No valid configuration found. Stopping service.")
			stopSelf()
			return START_NOT_STICKY
		}
		Log.d("StreamerService", "Using data: $finalIp:$finalPort")

		val channels = 2
		val sampleRate = 48000
		val audioFormat = AudioFormat.ENCODING_PCM_16BIT
		val frameSize = 960
		val pcmBuffer = ShortArray(frameSize * channels)

		val rsubmixListener = AudioRecord(
			MediaRecorder.AudioSource.REMOTE_SUBMIX,
			sampleRate,
			AudioFormat.CHANNEL_IN_STEREO,
			audioFormat,
			AudioRecord.getMinBufferSize(
				sampleRate,
				AudioFormat.CHANNEL_IN_STEREO,
				audioFormat
			)
		)
		val encoder = OpusEncoder(sampleRate, channels)
		encoder.setApplication(OPUS_APPLICATION_AUDIO)

		if (rsubmixListener.state != AudioRecord.STATE_INITIALIZED) {
			Log.e("StreamerService", "AudioRecord failed to initialize")
			stopSelf()
			return START_NOT_STICKY
		}

		isRunningFlow.value = true
		running = true

		thread {
			rsubmixListener.startRecording()
			while (running) {
				val read = rsubmixListener.read(pcmBuffer, 0, pcmBuffer.size)
				var sum = 0
				if (read == pcmBuffer.size) {
					for (i in pcmBuffer) {
						sum += kotlin.math.abs(i.toInt())
					}
					Log.d("StreamerService", "PCM energy: $sum")
					val encoded = encoder.encode(pcmBuffer.copyOf(read))
					Log.d("StreamerService", "${encoded.size}")
				} else {
					Log.d("StreamerService", "Read failed")
				}
			}
			rsubmixListener.stop()
			rsubmixListener.release()

			isRunningFlow.value = false
		}

		return START_STICKY
	}
	override fun onDestroy() {
		super.onDestroy()
		running = false
	}
}

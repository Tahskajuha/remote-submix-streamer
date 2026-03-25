package com.example.remotesubmixstreamer

import android.media.*
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.MutableStateFlow
import eu.buney.kopus.*
import android.app.Service
import android.util.Log
import android.content.Intent
import android.os.IBinder
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import java.net.DatagramSocket
import java.net.DatagramPacket
import java.net.InetAddress

class StreamerService : Service() {
	companion object {
		@Volatile private var running = false
		val isRunningFlow = MutableStateFlow(false)
	}

	private fun createNotificationChannel() {
		val channel = NotificationChannel(
			"streamer_channel",
			"Streaming Service",
			NotificationManager.IMPORTANCE_LOW
		)
		val manager = getSystemService(NotificationManager::class.java)

		manager.createNotificationChannel(channel)
	}

	private fun buildNotification(): Notification {
		return Notification
			.Builder(this, "streamer_channel")
			.setContentTitle("Remote Submix Streaming")
			.setContentText("Streaming audio output")
			.setSmallIcon(android.R.drawable.ic_media_play)
			.setOngoing(true)
			.build()
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

		createNotificationChannel()
		startForeground(1, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)

		thread {
			try {
				rsubmixListener.startRecording()
				while (running) {
					val read = rsubmixListener.read(pcmBuffer, 0, pcmBuffer.size)
					if (read != pcmBuffer.size) {
						Log.d("StreamerService", "Read failed")
					}
					val encoded = encoder.encode(pcmBuffer.copyOf(read))
				}
			} finally {
				rsubmixListener.stop()
				rsubmixListener.release()
				isRunningFlow.value = false
			}
		}

		return START_STICKY
	}
	override fun onDestroy() {
		super.onDestroy()
		running = false
		stopForeground(STOP_FOREGROUND_REMOVE)
	}
}

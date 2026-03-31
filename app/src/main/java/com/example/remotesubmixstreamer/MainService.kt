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

data class RtpPacket(
	val sequenceNumber: Int,
	val timestamp: Int,
	val ssrc: Int,
	val payloadType: Int,
	val payload: ByteArray
) {
	fun toByteArray(): ByteArray {
		val buffer = ByteArray(12 + payload.size)

		// version 2, no padding, no header extension
		buffer[0] = 0x80.toByte()

		val marker = 0
		buffer[1] = ((marker shl 7) or (payloadType and 0x7F)).toByte()

		buffer[2] = (sequenceNumber shr 8).toByte()
		buffer[3] = (sequenceNumber and 0xFF).toByte()

		buffer[4] = ((timestamp shr 24) and 0xFF).toByte()
		buffer[5] = ((timestamp shr 16) and 0xFF).toByte()
		buffer[6] = ((timestamp shr 8) and 0xFF).toByte()
		buffer[7] = (timestamp and 0xFF).toByte()

		buffer[8] = ((ssrc shr 24) and 0xFF).toByte()
		buffer[9] = ((ssrc shr 16) and 0xFF).toByte()
		buffer[10] = ((ssrc shr 8) and 0xFF).toByte()
		buffer[11] = (ssrc and 0xFF).toByte()

		System.arraycopy(payload, 0, buffer, 12, payload.size)

		return buffer
	}
}

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
		val outputBuffer = ByteArray(1024)

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

		var seq = 0
		var timestamp = 0
		val ssrc = (0..Int.MAX_VALUE).random()

		val socket = DatagramSocket()
		val address = InetAddress.getByName(finalIp!!)

		thread {
			try {
				rsubmixListener.startRecording()
				while (running) {
					val read = rsubmixListener.read(pcmBuffer, 0, pcmBuffer.size)
					if (read != pcmBuffer.size) {
						Log.d("StreamerService", "Read failed")
						continue
					}
					val encoded = encoder.encode(
						pcmBuffer,
						0,
						frameSize,
						outputBuffer,
						0,
						outputBuffer.size
					)
					if (encoded <= 0) continue

					val payload = outputBuffer.copyOf(encoded)
					val packet = RtpPacket(
						sequenceNumber = seq,
						timestamp = timestamp,
						ssrc = ssrc,
						payloadType = 96,
						payload = payload
					)
					val bytes = packet.toByteArray()

					socket.send(
						DatagramPacket(bytes, bytes.size, address, finalPort)
					)
					seq = (seq + 1) and 0xFFFF
					timestamp += frameSize
				}
			} finally {
				socket.close()
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

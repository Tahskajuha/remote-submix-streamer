package com.example.remotesubmixstreamer

import android.media.*
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.MutableStateFlow
import android.app.Service
import android.util.Log
import android.content.Intent
import android.os.IBinder

class StreamerService : Service() {
	companion object {
		val isRunningFlow = MutableStateFlow(false)
	}
	override fun onBind(intent: Intent?): IBinder? = null
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		val prefs = getSharedPreferences("config", MODE_PRIVATE)

		val ip = intent?.getStringExtra("ip")
		val port = intent?.getIntExtra("port", -1) ?: -1

		val finalIp: String?
		val finalPort: Int

		if (ip != null && port != -1) {
			finalIp = ip
			finalPort = port

			prefs.edit().putString("ip", ip).putInt("port", port).apply()
		} else if (prefs.getString("ip", null) != null && prefs.getInt("port", -1) != -1){
			finalIp = prefs.getString("ip", null)
			finalPort = prefs.getInt("port", -1)
		} else {
			Log.e("StreamerService", "No valid configuration found. Stopping service.")
			isRunningFlow.value = false
			stopSelf()
			return START_NOT_STICKY
		}
		Log.d("StreamerService", "Using data: $finalIp:$finalPort")
		isRunningFlow.value = true
		return START_STICKY
	}
	override fun onDestroy() {
		super.onDestroy()
		isRunningFlow.value = false
	}
}

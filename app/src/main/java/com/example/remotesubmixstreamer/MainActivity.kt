package com.example.remotesubmixstreamer

import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import android.content.Intent

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val prefs = getSharedPreferences("config", MODE_PRIVATE);
		setContent {
			MainScreen(prefs)
		}
	}
}

@Composable
fun MainScreen(prefs: android.content.SharedPreferences) {
	var ip by remember {
		mutableStateOf(prefs.getString("ip", "") ?: "")
	}
	var portString by remember {
		mutableStateOf(prefs.getInt("port", 0).takeIf {
			it != 0
		}?.toString() ?: "")
	}
	val isRunning by StreamerService.isRunningFlow.collectAsState()
	val context = LocalContext.current
	val port = portString.toIntOrNull() ?: 0

	val Crust = Color(0xFF11111B)
	val Base = Color(0xFF1E1E2E)
	val Surface0 = Color(0xFF313244)
	val Surface1 = Color(0xFF45475A)
	val HalfSurface1 = Color(0x8045475A)
	val TextColor = Color(0xFFCDD6F4)
	val Subtext0 = Color(0xFFA6ADC8)
	val Sky = Color(0xFF89DCEB)
	val HalfSky = Color(0x8089DCEB)

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(Color(0xFF11111B))
			.padding(16.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				"RemoteSubmixStreamer",
				color = TextColor,
				style = MaterialTheme.typography.titleLarge
			)
			Switch(
				checked = isRunning,
				onCheckedChange = { enabled ->
					if (enabled) {
						val intent = Intent(context, StreamerService::class.java).apply {
							putExtra("ip", ip)
							putExtra("port", port)
						}
						context.startService(intent)
					} else {
						context.stopService(Intent(context, StreamerService::class.java))
					}
				},
				colors = SwitchDefaults.colors(
					checkedThumbColor = Sky,
					checkedTrackColor = HalfSky,
					uncheckedThumbColor = Surface1,
					uncheckedTrackColor = HalfSurface1,
					checkedBorderColor = Sky,
					uncheckedBorderColor = Surface1
				)
			)
		}

		Spacer(
			modifier = Modifier
				.weight(1f)
		)

		Row(
			modifier = Modifier
				.fillMaxWidth()
		) {
			OutlinedTextField(
				value = ip,
				onValueChange = { ip = it },
				placeholder = { Text("Target Device's IP") },
				singleLine = true,
				modifier = Modifier
					.weight(3f),
				colors = OutlinedTextFieldDefaults.colors(
					focusedBorderColor = Sky,
					unfocusedBorderColor = Surface1,
					focusedTextColor = TextColor,
					unfocusedTextColor = TextColor,
					cursorColor = Sky,
					focusedLabelColor = Sky,
					unfocusedLabelColor = Subtext0,
					focusedPlaceholderColor = Subtext0,
					unfocusedPlaceholderColor = Subtext0,
					focusedContainerColor = Surface0,
					unfocusedContainerColor = Surface0
				),
				textStyle = MaterialTheme.typography.bodyLarge
			)

			Spacer(
				modifier = Modifier
					.width(12.dp)
			)

			OutlinedTextField(
				value = portString,
				onValueChange = { newPortString ->
					if (newPortString.isEmpty() || newPortString.all { it.isDigit() }) {
						portString = newPortString
					}
				},
				placeholder = { Text("Target Port") },
				singleLine = true,
				modifier = Modifier
					.weight(1f),
				colors = OutlinedTextFieldDefaults.colors(
					focusedBorderColor = Sky,
					unfocusedBorderColor = Surface1,
					focusedTextColor = TextColor,
					unfocusedTextColor = TextColor,
					cursorColor = Sky,
					focusedLabelColor = Sky,
					unfocusedLabelColor = Subtext0,
					focusedPlaceholderColor = Subtext0,
					unfocusedPlaceholderColor = Subtext0,
					focusedContainerColor = Surface0,
					unfocusedContainerColor = Surface0
				),
				textStyle = MaterialTheme.typography.bodyLarge
			)
		}
	}
}

package com.joijukik.remixlab

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RemixLabApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemixLabApp(viewModel: PlayerViewModel = viewModel()) {
    val context = LocalContext.current

    val isSlowed by viewModel.isSlowed.collectAsState()
    val isReverbEnabled by viewModel.isReverbEnabled.collectAsState()
    val isBassBoostEnabled by viewModel.isBassBoostEnabled.collectAsState()
    val is8DEnabled by viewModel.is8DEnabled.collectAsState()
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val currentPitch by viewModel.currentPitch.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val isRecording by viewModel.isRecording.collectAsState()
    val recordingSeconds by viewModel.recordingSeconds.collectAsState()
    val lastSavedFile by viewModel.lastSavedFile.collectAsState()
    val recordingError by viewModel.recordingError.collectAsState()

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadSong(it) }
    }

    val projectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    // 🔑 نکته کلیدی: اول سرویس رو استارت کن، بعد درخواست MediaProjection بده
    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val projection = projectionManager.getMediaProjection(
                result.resultCode, result.data!!
            )
            viewModel.startRecording(projection)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "RemixLab",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "ریمیکس حرفه‌ای آهنگ‌های شما",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { audioPicker.launch("audio/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🎵 انتخاب آهنگ")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🎛️ COMA AUDIO LAB",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "پریست‌های آماده کمایی و تاریک",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                val presets = ComaPresets.all
                for (i in presets.indices step 2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PresetButton(
                            preset = presets[i],
                            onClick = { viewModel.applyComaPreset(presets[i]) },
                            modifier = Modifier.weight(1f)
                        )
                        if (i + 1 < presets.size) {
                            PresetButton(
                                preset = presets[i + 1],
                                onClick = { viewModel.applyComaPreset(presets[i + 1]) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🎚️ کنترل‌های دستی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "سرعت: ${"%.2f".format(currentSpeed)}x",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = currentSpeed,
                    onValueChange = { viewModel.setCustomSpeed(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "گام: ${"%.2f".format(currentPitch)}x",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = currentPitch,
                    onValueChange = { viewModel.setPitch(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "افکت‌ها",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = isSlowed,
                onClick = { viewModel.toggleSlowed() },
                label = { Text("Slowed") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = isReverbEnabled,
                onClick = { viewModel.toggleReverb() },
                label = { Text("Reverb") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = isBassBoostEnabled,
                onClick = { viewModel.toggleBassBoost() },
                label = { Text("Bass Boost") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = is8DEnabled,
                onClick = { viewModel.toggle8D() },
                label = { Text("8D Audio") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ============================================
        // بخش ضبط (Export)
        // ============================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "💾 ذخیره ریمیکس",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isRecording) {
                    Text(
                        text = "⏺️ در حال ضبط... ${formatTime(recordingSeconds)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else if (lastSavedFile != null) {
                    Text(
                        text = "✅ ذخیره شد!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lastSavedFile!!.name,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "برای ضبط، روی دکمه زیر بزن",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (recordingError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "❌ ${recordingError}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (isRecording) {
                            viewModel.stopRecording()
                        } else {
                            viewModel.clearRecordingError()

                            // 🔑 مراحل کلیدی برای اندروید ۱۴:
                            // ۱. اول سرویس رو استارت کن
                            val serviceIntent = Intent(context, MediaProjectionService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }

                            // ۲. بعد از یک تاخیر کوتاه، درخواست MediaProjection بده
                            // (کد درخواست در کالبک projectionLauncher انجام می‌شود)
                            projectionLauncher.launch(
                                projectionManager.createScreenCaptureIntent()
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isRecording) "⏹ توقف ضبط" else "⏺️ شروع ضبط",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.playPause() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (isPlaying) "⏸ توقف" else "▶️ پخش",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PresetButton(
    preset: ComaPreset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = preset.description,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

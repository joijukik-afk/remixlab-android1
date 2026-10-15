package com.joijukik.remixlab.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joijukik.remixlab.AiTuningAssistant
import com.joijukik.remixlab.ComaPreset
import com.joijukik.remixlab.ComaPresets
import com.joijukik.remixlab.MediaProjectionService
import com.joijukik.remixlab.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current

    val isSlowed by viewModel.isSlowed.collectAsState()
    val isReverbEnabled by viewModel.isReverbEnabled.collectAsState()
    val isBassBoostEnabled by viewModel.isBassBoostEnabled.collectAsState()
    val is8DEnabled by viewModel.is8DEnabled.collectAsState()
    val isDynamicsEnabled by viewModel.isDynamicsEnabled.collectAsState()
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val currentPitch by viewModel.currentPitch.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val songName by viewModel.songName.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val exportQuality by viewModel.exportQuality.collectAsState()

    val outputFileName by viewModel.outputFileName.collectAsState()
    val outputTitle by viewModel.outputTitle.collectAsState()
    val outputArtist by viewModel.outputArtist.collectAsState()

    val isRecording by viewModel.isRecording.collectAsState()
    val isRecordingPaused by viewModel.isRecordingPaused.collectAsState()
    val recordingSeconds by viewModel.recordingSeconds.collectAsState()
    val lastSavedUri by viewModel.lastSavedUri.collectAsState()
    val recordingError by viewModel.recordingError.collectAsState()

    var aiPrompt by remember { mutableStateOf("") }
    var aiResult by remember { mutableStateOf<AiTuningAssistant.TuningResult?>(null) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadSong(it) }
    }

    val projectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(context, MediaProjectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            Handler(Looper.getMainLooper()).postDelayed({
                val projection = projectionManager.getMediaProjection(
                    result.resultCode, result.data!!
                )
                viewModel.startRecording(projection)
            }, 500)
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

        // دکمه بازگشت
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onBack) {
                Text("← بازگشت")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "ویرایشگر ریمیکس", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // ─── انتخاب آهنگ ───
        Button(
            onClick = {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, permission
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    audioPicker.launch("audio/*")
                } else {
                    audioPermissionLauncher.launch(permission)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🎵 انتخاب آهنگ")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── اطلاعات آهنگ ───
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "🎶 در حال پخش:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = songName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Slider(
                    value = if (duration > 0) currentPosition.toFloat() else 0f,
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..(duration.coerceAtLeast(1L).toFloat()),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition / 1000),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = formatTime(duration / 1000),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── دستیار AI ───
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🤖 دستیار هوشمند",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = aiPrompt,
                    onValueChange = { aiPrompt = it },
                    label = { Text("مثلاً: کمایی و کند کن") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (aiPrompt.isNotBlank()) {
                            val result = AiTuningAssistant.analyzeRequest(aiPrompt)
                            aiResult = result
                            viewModel.applyAiTuning(aiPrompt)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✨ اعمال با AI")
                }

                if (aiResult != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "کلمات: ${aiResult!!.detectedKeywords.joinToString("، ")}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── پریست‌های COMA ───
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🎛️ COMA AUDIO LAB",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
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

        // ─── کنترل‌های دستی ───
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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

        // ─── افکت‌ها ───
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

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = isDynamicsEnabled,
                onClick = { viewModel.toggleDynamics() },
                label = { Text("🎚️ استودیویی") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ─── ضبط ───
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "💾 ذخیره ریمیکس (MP3)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = outputFileName,
                    onValueChange = { viewModel.setOutputFileName(it) },
                    label = { Text("نام فایل") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = outputTitle,
                    onValueChange = { viewModel.setOutputTitle(it) },
                    label = { Text("عنوان آهنگ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = outputArtist,
                    onValueChange = { viewModel.setOutputArtist(it) },
                    label = { Text("هنرمند") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "کیفیت: ${exportQuality} kbps",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(128, 192, 256, 320).forEach { kbps ->
                        FilterChip(
                            selected = exportQuality == kbps,
                            onClick = { viewModel.setExportQuality(kbps) },
                            label = { Text("$kbps") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isRecording) {
                    Text(
                        text = if (isRecordingPaused)
                            "⏸ متوقف شده (${formatTime(recordingSeconds)})"
                        else
                            "⏺️ ضبط... ${formatTime(recordingSeconds)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isRecordingPaused)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else if (lastSavedUri != null) {
                    Text(
                        text = "✅ ذخیره شد!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
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

                if (isRecording) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                if (isRecordingPaused) viewModel.resumeRecording()
                                else viewModel.pauseRecording()
                            },
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Text(if (isRecordingPaused) "▶️ ادامه" else "⏸ توقف موقت")
                        }
                        Button(
                            onClick = { viewModel.stopRecording() },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("⏹ توقف")
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.clearRecordingError()
                            projectionLauncher.launch(
                                projectionManager.createScreenCaptureIntent()
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⏺️ شروع ضبط")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.playPause() },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                text = if (isPlaying) "⏸ توقف موقت" else "▶️ پخش",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PresetButton(
    preset: ComaPreset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(onClick = onClick, modifier = modifier.height(64.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(text = preset.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatTime(seconds: Number): String {
    val total = seconds.toLong()
    val mins = total / 60
    val secs = total % 60
    return "%02d:%02d".format(mins, secs)
}

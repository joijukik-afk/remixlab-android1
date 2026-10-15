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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    val mixTracks by viewModel.mixTracks.collectAsState()
    val isSeparating by viewModel.isSeparating.collectAsState()
    val separationProgress by viewModel.separationProgress.collectAsState()
    val activeStemFilter by viewModel.activeStemFilter.collectAsState()

    var aiPrompt by remember { mutableStateOf("") }
    var aiResult by remember { mutableStateOf<AiTuningAssistant.TuningResult?>(null) }
    var eqBands by remember { mutableStateOf(List(10) { 0f }) }
    var compressorThreshold by remember { mutableStateOf(-20f) }
    var compressorRatio by remember { mutableStateOf(4f) }
    var limiterEnabled by remember { mutableStateOf(true) }
    var limiterThreshold by remember { mutableStateOf(-3f) }
    var inputGain by remember { mutableStateOf(0f) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.loadSong(it) } }

    val mixAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val trackId = "track_${mixTracks.size + 1}"
            viewModel.addTrackToMix(it, trackId)
        }
    }

    val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(context, MediaProjectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else context.startService(serviceIntent)
            Handler(Looper.getMainLooper()).postDelayed({
                val projection = projectionManager.getMediaProjection(result.resultCode, result.data!!)
                viewModel.startRecording(projection)
            }, 500)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) { Button(onClick = onBack) { Text("← بازگشت") } }
        Spacer(Modifier.height(16.dp))
        Text("ویرایشگر ریمیکس", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        // انتخاب آهنگ
        Button(
            onClick = {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    Manifest.permission.READ_MEDIA_AUDIO
                else Manifest.permission.READ_EXTERNAL_STORAGE
                val has = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                if (has) audioPicker.launch("audio/*")
                else audioPermissionLauncher.launch(permission)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("🎵 انتخاب آهنگ") }

        Spacer(Modifier.height(16.dp))

        // اطلاعات آهنگ
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🎶 در حال پخش:", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text(songName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() else 0f,
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..(duration.coerceAtLeast(1L).toFloat()),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(currentPosition / 1000), style = MaterialTheme.typography.bodySmall)
                    Text(formatTime(duration / 1000), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // دستیار AI
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🤖 دستیار هوشمند", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = aiPrompt,
                    onValueChange = { aiPrompt = it },
                    label = { Text("مثلاً: کمایی و کند کن") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (aiPrompt.isNotBlank()) {
                            val result = AiTuningAssistant.analyzeRequest(aiPrompt)
                            aiResult = result
                            viewModel.applyAiTuning(aiPrompt)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("✨ اعمال با AI") }
                if (aiResult != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("کلمات: ${aiResult!!.detectedKeywords.joinToString("، ")}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // میکس چند آهنگ
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🎛️ میکس چند آهنگ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { mixAudioPicker.launch("audio/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("➕ افزودن آهنگ به میکس")
                }
                Spacer(Modifier.height(12.dp))
                if (mixTracks.isEmpty()) {
                    Text("هنوز آهنگی به میکس اضافه نشده", style = MaterialTheme.typography.bodySmall)
                } else {
                    mixTracks.forEachIndexed { index, track ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("ترک ${index + 1}", modifier = Modifier.width(60.dp))
                            Slider(
                                value = track.volume,
                                onValueChange = { viewModel.setTrackVolume(track.id, it) },
                                valueRange = 0f..2f,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${(track.volume * 100).toInt()}%", modifier = Modifier.width(40.dp))
                            IconButton(onClick = { viewModel.removeTrackFromMix(track.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "حذف")
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // جداسازی صدا
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🎤 جداسازی صدا (AI)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.separateCurrentSong() },
                    enabled = !isSeparating,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isSeparating) "⏳ در حال پردازش..." else "🔍 تحلیل و جداسازی") }
                if (isSeparating) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { separationProgress }, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(12.dp))
                Text("فیلتر استم‌ها:", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = activeStemFilter == PlayerViewModel.StemFilter.ALL,
                        onClick = { viewModel.applyStemFilter(PlayerViewModel.StemFilter.ALL) },
                        label = { Text("همه") })
                    FilterChip(selected = activeStemFilter == PlayerViewModel.StemFilter.VOCALS_ONLY,
                        onClick = { viewModel.applyStemFilter(PlayerViewModel.StemFilter.VOCALS_ONLY) },
                        label = { Text("🎤 خواننده") })
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = activeStemFilter == PlayerViewModel.StemFilter.INSTRUMENTAL,
                        onClick = { viewModel.applyStemFilter(PlayerViewModel.StemFilter.INSTRUMENTAL) },
                        label = { Text("🎸 بیت") })
                    FilterChip(selected = activeStemFilter == PlayerViewModel.StemFilter.NO_DRUMS,
                        onClick = { viewModel.applyStemFilter(PlayerViewModel.StemFilter.NO_DRUMS) },
                        label = { Text("🥁 بدون درام") })
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = activeStemFilter == PlayerViewModel.StemFilter.NO_BASS,
                        onClick = { viewModel.applyStemFilter(PlayerViewModel.StemFilter.NO_BASS) },
                        label = { Text("🎵 بدون بیس") })
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // تنظیمات پیشرفته میکس
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🎚️ تنظیمات پیشرفته میکس", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text("اکولایزر ۱۰ باندی", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val bandLabels = listOf("31Hz","62Hz","125Hz","250Hz","500Hz","1kHz","2kHz","4kHz","8kHz","16kHz")
                bandLabels.forEachIndexed { index, label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(50.dp))
                        Slider(
                            value = eqBands[index],
                            onValueChange = { newVal ->
                                eqBands = eqBands.toMutableList().also { it[index] = newVal }
                                viewModel.setEqBand(index, newVal)
                            },
                            valueRange = -12f..12f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${eqBands[index].toInt()}dB", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(45.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("کمپرسور", fontWeight = FontWeight.Bold)
                Text("آستانه: ${compressorThreshold.toInt()} dB")
                Slider(value = compressorThreshold, onValueChange = {
                    compressorThreshold = it; viewModel.setCompressorThreshold(it)
                }, valueRange = -60f..0f)
                Text("نسبت: ${"%.1f".format(compressorRatio)}:1")
                Slider(value = compressorRatio, onValueChange = {
                    compressorRatio = it; viewModel.setCompressorRatio(it)
                }, valueRange = 1f..20f)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("لیمیتر", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = limiterEnabled, onCheckedChange = {
                        limiterEnabled = it; viewModel.setLimiterEnabled(it)
                    })
                }
                if (limiterEnabled) {
                    Text("آستانه: ${limiterThreshold.toInt()} dB")
                    Slider(value = limiterThreshold, onValueChange = {
                        limiterThreshold = it; viewModel.setLimiterThreshold(it)
                    }, valueRange = -12f..0f)
                }
                Spacer(Modifier.height(16.dp))
                Text("گین ورودی: ${inputGain.toInt()} dB")
                Slider(value = inputGain, onValueChange = {
                    inputGain = it; viewModel.setInputGain(it)
                }, valueRange = -12f..12f)
            }
        }

        Spacer(Modifier.height(24.dp))

        // COMA Presets
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🎛️ COMA AUDIO LAB", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                val presets = ComaPresets.all
                for (i in presets.indices step 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        PresetButton(preset = presets[i], onClick = { viewModel.applyComaPreset(presets[i]) }, modifier = Modifier.weight(1f))
                        if (i + 1 < presets.size) {
                            PresetButton(preset = presets[i + 1], onClick = { viewModel.applyComaPreset(presets[i + 1]) }, modifier = Modifier.weight(1f))
                        } else Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // کنترل‌های دستی
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🎚️ کنترل‌های دستی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text("سرعت: ${"%.2f".format(currentSpeed)}x")
                Slider(value = currentSpeed, onValueChange = { viewModel.setCustomSpeed(it) }, valueRange = 0.5f..2.0f, steps = 14)
                Spacer(Modifier.height(8.dp))
                Text("گام: ${"%.2f".format(currentPitch)}x")
                Slider(value = currentPitch, onValueChange = { viewModel.setPitch(it) }, valueRange = 0.5f..2.0f, steps = 14)
            }
        }

        Spacer(Modifier.height(24.dp))

        // افکت‌ها
        Text("افکت‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(selected = isSlowed, onClick = { viewModel.toggleSlowed() }, label = { Text("Slowed") }, modifier = Modifier.weight(1f))
            FilterChip(selected = isReverbEnabled, onClick = { viewModel.toggleReverb() }, label = { Text("Reverb") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(selected = isBassBoostEnabled, onClick = { viewModel.toggleBassBoost() }, label = { Text("Bass Boost") }, modifier = Modifier.weight(1f))
            FilterChip(selected = is8DEnabled, onClick = { viewModel.toggle8D() }, label = { Text("8D Audio") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(selected = isDynamicsEnabled, onClick = { viewModel.toggleDynamics() }, label = { Text("🎚️ استودیویی") }, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))

        // ضبط
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💾 ذخیره ریمیکس", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = outputFileName, onValueChange = { viewModel.setOutputFileName(it) }, label = { Text("نام فایل") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = outputTitle, onValueChange = { viewModel.setOutputTitle(it) }, label = { Text("عنوان") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = outputArtist, onValueChange = { viewModel.setOutputArtist(it) }, label = { Text("هنرمند") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(12.dp))
                Text("کیفیت: ${exportQuality} kbps", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(128, 192, 256, 320).forEach { kbps ->
                        FilterChip(selected = exportQuality == kbps, onClick = { viewModel.setExportQuality(kbps) }, label = { Text("$kbps") }, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (isRecording) {
                    Text(
                        if (isRecordingPaused) "⏸ متوقف (${formatTime(recordingSeconds)})"
                        else "⏺️ ضبط... ${formatTime(recordingSeconds)}",
                        color = if (isRecordingPaused) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else if (lastSavedUri != null) {
                    Text("✅ ذخیره شد!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                if (recordingError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("❌ ${recordingError}", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(12.dp))
                if (isRecording) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { if (isRecordingPaused) viewModel.resumeRecording() else viewModel.pauseRecording() }, modifier = Modifier.weight(1f).height(56.dp)) {
                            Text(if (isRecordingPaused) "▶️ ادامه" else "⏸ توقف موقت")
                        }
                        Button(onClick = { viewModel.stopRecording() }, modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("⏹ توقف")
                        }
                    }
                } else {
                    Button(onClick = {
                        viewModel.clearRecordingError()
                        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                    }, modifier = Modifier.fillMaxWidth()) { Text("⏺️ شروع ضبط") }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = { viewModel.playPause() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (isPlaying) "⏸ توقف موقت" else "▶️ پخش", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PresetButton(preset: ComaPreset, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.height(64.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(preset.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(preset.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatTime(seconds: Number): String {
    val total = seconds.toLong()
    return "%02d:%02d".format(total / 60, total % 60)
}

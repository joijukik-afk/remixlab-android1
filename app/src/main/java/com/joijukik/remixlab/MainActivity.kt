package com.remixlab.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remixlab.app.model.ComaPresets
import com.remixlab.app.service.MediaProjectionService
import com.remixlab.app.ui.AudioVisualizer
import com.remixlab.app.ui.SearchBar
import com.remixlab.app.ui.SongListScreen
import com.remixlab.app.ui.fakeAmplitudes
import com.remixlab.app.ui.theme.RemixLabTheme
import com.remixlab.app.ui.theme.RemixPrimary
import com.remixlab.app.ui.theme.RemixSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemixLabTheme {
                RemixLabApp(viewModel = viewModel)
            }
        }
        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) perms += Manifest.permission.READ_MEDIA_AUDIO
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) perms += Manifest.permission.POST_NOTIFICATIONS
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) perms += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (perms.isNotEmpty()) {
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
                .launch(perms.toTypedArray())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemixLabApp(viewModel: PlayerViewModel) {

    val context = LocalContext.current

    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val speed by viewModel.currentSpeed.collectAsStateWithLifecycle()
    val pitch by viewModel.currentPitch.collectAsStateWithLifecycle()
    val isSlowed by viewModel.isSlowed.collectAsStateWithLifecycle()
    val isReverb by viewModel.isReverbEnabled.collectAsStateWithLifecycle()
    val isBass by viewModel.isBassBoostEnabled.collectAsStateWithLifecycle()
    val is8D by viewModel.is8DEnabled.collectAsStateWithLifecycle()
    val isDyn by viewModel.isDynamicsEnabled.collectAsStateWithLifecycle()
    val ambient by viewModel.activeAmbient.collectAsStateWithLifecycle()
    val ambientVol by viewModel.ambientVolume.collectAsStateWithLifecycle()
    val songTitle by viewModel.currentSongTitle.collectAsStateWithLifecycle()
    val position by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recSec by viewModel.recordingSeconds.collectAsStateWithLifecycle()
    val savedPath by viewModel.lastSavedPath.collectAsStateWithLifecycle()
    val recErr by viewModel.recordingError.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val customPresets by viewModel.customPresets.collectAsStateWithLifecycle()
    val aiResult by viewModel.aiResult.collectAsStateWithLifecycle()
    val aiLoading by viewModel.aiLoading.collectAsStateWithLifecycle()

    var showSearch by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            val intent = Intent(context, MediaProjectionService::class.java).apply {
                action = MediaProjectionService.ACTION_START
                putExtra(MediaProjectionService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(MediaProjectionService.EXTRA_DATA, data)
            }
            ContextCompat.startForegroundService(context, intent)

            val mpm = context.getSystemService(MediaProjectionManager::class.java)
            val proj = mpm.getMediaProjection(result.resultCode, data)
            if (proj != null) viewModel.startRecording(proj)
        } else {
            Toast.makeText(context, "ضبط لغو شد", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RemixLab", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, contentDescription = "جستجو")
                    }
                    IconButton(onClick = { showAiDialog = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "دستیار")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            if (showSearch) {
                SearchBar(query = query, onQueryChange = viewModel::updateSearchQuery)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.height(220.dp)) {
                    SongListScreen(
                        songs = results,
                        onSongClick = { s ->
                            viewModel.loadSong(s.uri, s.title)
                            showSearch = false
                        }
                    )
                }
            }

            // ویژوالایزر
            AudioVisualizer(amplitudes = fakeAmplitudes(position))
            Spacer(Modifier.height(8.dp))

            // اسم آهنگ
            Text(
                songTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Spacer(Modifier.height(8.dp))

            // Progress
            if (duration > 0) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(position / 1000), style = MaterialTheme.typography.labelSmall)
                    Text(formatTime(duration / 1000), style = MaterialTheme.typography.labelSmall)
                }
                Slider(
                    value = if (duration > 0) position.toFloat() / duration else 0f,
                    onValueChange = { viewModel.seekTo((it * duration).toLong()) }
                )
            }

            // دکمه پخش
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = { viewModel.playPause() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // اسلایدرها
            SectionTitle("سرعت پخش (${"%.2f".format(speed)}x)")
            Slider(
                value = speed,
                onValueChange = viewModel::setCustomSpeed,
                valueRange = 0.5f..2.0f
            )

            SectionTitle("زیر و بمی صدا (${"%.2f".format(pitch)})")
            Slider(
                value = pitch,
                onValueChange = viewModel::setPitch,
                valueRange = 0.5f..2.0f
            )

            SectionTitle("صدای محیطی (${"%.0f".format(ambientVol * 100)}%)")
            Slider(
                value = ambientVol,
                onValueChange = viewModel::setAmbientVolume,
                valueRange = 0f..1f
            )

            Spacer(Modifier.height(8.dp))

            // افکت‌ها
            SectionTitle("افکت‌ها")
            FlowChips {
                FilterChip(
                    selected = isSlowed,
                    onClick = { viewModel.toggleSlowed() },
                    label = { Text("Slowed") }
                )
                FilterChip(
                    selected = isReverb,
                    onClick = { viewModel.toggleReverb() },
                    label = { Text("Reverb") }
                )
                FilterChip(
                    selected = isBass,
                    onClick = { viewModel.toggleBassBoost() },
                    label = { Text("Bass Boost") }
                )
                FilterChip(
                    selected = is8D,
                    onClick = { viewModel.toggle8D() },
                    label = { Text("8D") }
                )
                FilterChip(
                    selected = isDyn,
                    onClick = { viewModel.toggleDynamics() },
                    label = { Text("Dynamics") }
                )
            }

            Spacer(Modifier.height(16.dp))

            // پریست‌های COMA
            SectionTitle("پریست‌های COMA")
            FlowChips {
                ComaPresets.ALL.forEach { p ->
                    AssistChip(
                        onClick = { viewModel.applyComaPreset(p) },
                        label = { Text("${p.icon} ${p.name}") }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // پریست‌های سفارشی
            if (customPresets.isNotEmpty()) {
                SectionTitle("پریست‌های من")
                FlowChips {
                    customPresets.forEach { p ->
                        AssistChip(
                            onClick = { viewModel.applyComaPreset(p) },
                            label = { Text("${p.icon} ${p.name}") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showSavePresetDialog = true }) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("ذخیره پریست")
                }

                Button(
                    onClick = {
                        if (isRecording) {
                            viewModel.stopRecording()
                            val stop = Intent(context, MediaProjectionService::class.java).apply {
                                action = MediaProjectionService.ACTION_STOP
                            }
                            context.startService(stop)
                        } else {
                            val mpm = context.getSystemService(MediaProjectionManager::class.java)
                            projectionLauncher.launch(mpm.createScreenCaptureIntent())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color.Red else RemixPrimary
                    )
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isRecording) "توقف (${recSec}s)" else "ضبط MP3")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // دیالوگ ذخیره پریست
    if (showSavePresetDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("ذخیره پریست فعلی") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام پریست") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.saveCurrentAsPreset(name.trim())
                        showSavePresetDialog = false
                    }
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) { Text("انصراف") }
            }
        )
    }

    // دیالوگ AI
    if (showAiDialog) {
        var prompt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("🤖 دستیار هوشمند") },
            text = {
                Column {
                    Text("مثلاً: «صدای بارون با بیس قوی و Slowed»")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("حس مورد نظرت رو بنویس...") },
                        minLines = 2
                    )
                    if (aiLoading) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    aiResult?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it.explanation, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !aiLoading && prompt.isNotBlank(),
                    onClick = { viewModel.askAi(prompt) }
                ) { Text("اعمال") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.clearAiResult()
                    showAiDialog = false
                }) { Text("بستن") }
            }
        )
    }

    // پیام‌ها
    savedPath?.let {
        Toast.makeText(context, "ذخیره شد: $it", Toast.LENGTH_LONG).show()
        viewModel.clearLastSaved()
    }
    recErr?.let {
        Toast.makeText(context, "خطا: $it", Toast.LENGTH_LONG).show()
        viewModel.clearRecordingError()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = RemixSecondary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) { content() }
}

private fun formatTime(totalSec: Long): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

package com.joijukik.remixlab

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.joijukik.remixlab.ui.SongListScreen
import com.joijukik.remixlab.ui.SongItem

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RemixLabApp(viewModel)
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
    val songName by viewModel.songName.collectAsStateWithLifecycle()
    val position by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val aiExp by viewModel.aiExplanation.collectAsStateWithLifecycle()

    var showSearch by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RemixLab", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, contentDescription = "جستجو")
                    }
                    IconButton(onClick = { showAiDialog = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI")
                    }
                }
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
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("جستجوی آهنگ...") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Box(Modifier.height(200.dp)) {
                    SongListScreen(
                        songs = results.map {
                            SongItem(it.uri, it.title, it.artist, it.durationMs)
                        },
                        onSongClick = { s ->
                            viewModel.loadSong(s.uri, s.title)
                            showSearch = false
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(songName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (duration > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(position / 1000), style = MaterialTheme.typography.labelSmall)
                    Text(formatTime(duration / 1000), style = MaterialTheme.typography.labelSmall)
                }
                Slider(
                    value = if (duration > 0) position.toFloat() / duration else 0f,
                    onValueChange = { viewModel.seekTo((it * duration).toLong()) }
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
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

            Text("سرعت: ${"%.2f".format(speed)}x")
            Slider(value = speed, onValueChange = viewModel::setCustomSpeed, valueRange = 0.5f..2f)

            Text("پیچ: ${"%.2f".format(pitch)}")
            Slider(value = pitch, onValueChange = viewModel::setPitch, valueRange = 0.5f..2f)

            Spacer(Modifier.height(8.dp))

            Text("افکت‌ها", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = isSlowed, onClick = { viewModel.toggleSlowed() }, label = { Text("Slowed") })
                FilterChip(selected = isReverb, onClick = { viewModel.toggleReverb() }, label = { Text("Reverb") })
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = isBass, onClick = { viewModel.toggleBassBoost() }, label = { Text("Bass") })
                FilterChip(selected = is8D, onClick = { viewModel.toggle8D() }, label = { Text("8D") })
                FilterChip(selected = isDyn, onClick = { viewModel.toggleDynamics() }, label = { Text("Dyn") })
            }

            Spacer(Modifier.height(16.dp))

            aiExp?.let {
                Card(Modifier.fillMaxWidth()) {
                    Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAiDialog) {
        var prompt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("🤖 دستیار هوشمند") },
            text = {
                Column {
                    Text("مثلاً: «بارون با بیس قوی»")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.applyAiTuning(prompt)
                    showAiDialog = false
                }) { Text("اعمال") }
            },
            dismissButton = {
                TextButton(onClick = { showAiDialog = false }) { Text("بستن") }
            }
        )
    }
}

private fun formatTime(totalSec: Long): String {
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

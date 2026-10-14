package com.joijukik.remixlab

import android.media.audiofx.PresetReverb
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val isSlowed by viewModel.isSlowed.collectAsState()
    val isReverbEnabled by viewModel.isReverbEnabled.collectAsState()
    val isBassBoostEnabled by viewModel.isBassBoostEnabled.collectAsState()
    val is8DEnabled by viewModel.is8DEnabled.collectAsState()
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val currentPitch by viewModel.currentPitch.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadSong(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- عنوان ---
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

        // --- دکمه انتخاب آهنگ ---
        Button(
            onClick = { audioPicker.launch("audio/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🎵 انتخاب آهنگ")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ============================================
        // پنل COMA AUDIO LAB
        // ============================================
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

                // دکمه‌های پریست - ۲ تا در هر ردیف
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

        // ============================================
        // کنترل‌های دستی
        // ============================================
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

                // --- سرعت ---
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

                // --- گام ---
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

        // ============================================
        // افکت‌های پایه
        // ============================================
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

        // --- دکمه پخش/توقف ---
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

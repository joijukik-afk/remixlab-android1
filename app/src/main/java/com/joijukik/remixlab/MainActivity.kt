package com.joijukik.remixlab

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val isVirtualizerEnabled by viewModel.isVirtualizerEnabled.collectAsState()
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadSong(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "RemixLab",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "ریمیکس حرفه‌ای آهنگ‌های شما",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { audioPicker.launch("audio/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🎵 انتخاب آهنگ")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "سرعت: ${"%.2f".format(currentSpeed)}x",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = currentSpeed,
            onValueChange = { viewModel.setCustomSpeed(it) },
            valueRange = 0.5f..2.0f,
            steps = 14,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ردیف اول افکت‌ها
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

        // ردیف دوم افکت‌ها
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
                selected = isVirtualizerEnabled,
                onClick = { viewModel.toggleVirtualizer() },
                label = { Text("8D Audio") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

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
    }
}

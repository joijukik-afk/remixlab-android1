package com.remixlab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remixlab.app.SongInfo

@Composable
fun SongListScreen(
    songs: List<SongInfo>,
    onSongClick: (SongInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("آهنگی پیدا نشد. یک عبارت دیگر جستجو کن.")
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(songs, key = { it.uri.toString() }) { song ->
            ListItem(
                headlineContent = { Text(song.title, maxLines = 1) },
                supportingContent = { Text(song.artist, maxLines = 1) },
                trailingContent = {
                    Text(formatDuration(song.durationMs))
                },
                leadingContent = {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                },
                modifier = Modifier.clickable { onSongClick(song) }
            )
            HorizontalDivider()
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

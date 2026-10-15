package com.joijukik.remixlab.ui

import android.net.Uri
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

data class SongItem(
    val uri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long
)

@Composable
fun SongListScreen(
    songs: List<SongItem>,
    onSongClick: (SongItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("آهنگی پیدا نشد.")
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(songs, key = { it.uri.toString() }) { song ->
            ListItem(
                headlineContent = { Text(song.title, maxLines = 1) },
                supportingContent = { Text(song.artist, maxLines = 1) },
                trailingContent = {
                    val s = song.durationMs / 1000
                    Text("%d:%02d".format(s / 60, s % 60))
                },
                leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                modifier = Modifier.clickable { onSongClick(song) }
            )
            HorizontalDivider()
        }
    }
}

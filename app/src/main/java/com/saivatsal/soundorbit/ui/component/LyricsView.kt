package com.saivatsal.soundorbit.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saivatsal.soundorbit.core.lyrics.model.Lyrics
import com.saivatsal.soundorbit.ui.theme.CosmicTeal

@Composable
fun LyricsView(
    lyrics: Lyrics?,
    isLoading: Boolean,
    currentPositionMs: Long,
    modifier: Modifier = Modifier,
    onSeekTo: (Long) -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = CosmicTeal)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading lyrics...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            lyrics == null || (!lyrics.hasLyrics && !lyrics.isInstrumental) -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No lyrics available for this track",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            lyrics.isInstrumental -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = CosmicTeal,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Instrumental Track",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            lyrics.isSynced -> {
                val activeIndex = lyrics.activeLineIndex(currentPositionMs)
                val listState = rememberLazyListState()

                LaunchedEffect(activeIndex) {
                    if (activeIndex >= 0) {
                        val targetScrollIndex = (activeIndex - 2).coerceAtLeast(0)
                        listState.animateScrollToItem(targetScrollIndex)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    itemsIndexed(lyrics.syncedLyrics) { index, line ->
                        val isCurrent = index == activeIndex
                        val textColor by animateColorAsState(
                            targetValue = if (isCurrent) CosmicTeal else Color.White,
                            label = "lyricTextColor"
                        )
                        val textAlpha by animateFloatAsState(
                            targetValue = if (isCurrent) 1.0f else 0.4f,
                            label = "lyricAlpha"
                        )
                        val fontSize = if (isCurrent) 22.sp else 18.sp
                        val fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium

                        Text(
                            text = line.text.ifBlank { "♪" },
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            color = textColor,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(textAlpha)
                                .clickable { onSeekTo(line.timestampMs) }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
            lyrics.plainLyrics != null -> {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(vertical = 24.dp)
                ) {
                    Text(
                        text = lyrics.plainLyrics,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 28.sp
                    )
                }
            }
        }
    }
}

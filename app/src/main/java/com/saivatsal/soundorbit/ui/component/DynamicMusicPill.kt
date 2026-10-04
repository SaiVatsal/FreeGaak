package com.saivatsal.soundorbit.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.saivatsal.soundorbit.core.player.PlaybackStatus
import com.saivatsal.soundorbit.core.player.PlayerSnapshot
import com.saivatsal.soundorbit.ui.theme.DarkSurface
import com.saivatsal.soundorbit.ui.theme.EmeraldGreenBright
import com.saivatsal.soundorbit.ui.theme.ObsidianBlack
import com.saivatsal.soundorbit.ui.theme.TextMediumEmphasis

@Composable
fun DynamicMusicPill(
    playerState: PlayerSnapshot,
    onExpandNowPlaying: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return
    val isPlaying = playerState.status == PlaybackStatus.PLAYING

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(28.dp), ambientColor = EmeraldGreenBright)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            ObsidianBlack,
                            DarkSurface.copy(alpha = 0.95f),
                            ObsidianBlack
                        )
                    )
                )
                .border(1.2.dp, EmeraldGreenBright.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                .clickable { onExpandNowPlaying() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Album Artwork with Pulse
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Animated Equalizer Waveform
            MiniEqualizerBars(
                isPlaying = isPlaying,
                color = EmeraldGreenBright,
                modifier = Modifier.size(width = 16.dp, height = 18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Track metadata
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artistName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMediumEmphasis,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }

            // Quick Play/Pause Action
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = EmeraldGreenBright,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Quick Next Action
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun MiniEqualizerBars(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / 4f
        val gap = width / 8f

        val h1 = if (isPlaying) height * bar1Height else height * 0.3f
        val h2 = if (isPlaying) height * bar2Height else height * 0.6f
        val h3 = if (isPlaying) height * bar3Height else height * 0.4f

        // Bar 1
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, height - h1),
            size = Size(barWidth, h1),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )

        // Bar 2
        drawRoundRect(
            color = color,
            topLeft = Offset(barWidth + gap, height - h2),
            size = Size(barWidth, h2),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )

        // Bar 3
        drawRoundRect(
            color = color,
            topLeft = Offset((barWidth + gap) * 2, height - h3),
            size = Size(barWidth, h3),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}

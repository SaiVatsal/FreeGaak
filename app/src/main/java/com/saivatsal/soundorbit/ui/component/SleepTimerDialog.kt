package com.saivatsal.soundorbit.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saivatsal.soundorbit.core.player.SleepTimerState
import com.saivatsal.soundorbit.ui.theme.CosmicTeal
import com.saivatsal.soundorbit.ui.theme.DarkSurface

@Composable
fun SleepTimerDialog(
    state: SleepTimerState,
    onDismiss: () -> Unit,
    onStartTimer: (minutes: Int, fadeOut: Boolean) -> Unit,
    onStartEndOfTrack: () -> Unit,
    onCancelTimer: () -> Unit
) {
    var fadeOutEnabled by remember { mutableStateOf(state.fadeOutEnabled) }

    val presetOptions = listOf(
        5 to "5 minutes",
        10 to "10 minutes",
        15 to "15 minutes",
        30 to "30 minutes",
        45 to "45 minutes",
        60 to "60 minutes"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = CosmicTeal,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Sleep Timer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.isActive) {
                    val formattedTime = if (state.isEndOfTrack) {
                        "Pausing at the end of current track"
                    } else {
                        val minutes = state.remainingSeconds / 60
                        val seconds = state.remainingSeconds % 60
                        String.format("%02d:%02d remaining", minutes, seconds)
                    }

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.headlineSmall,
                        color = CosmicTeal,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Button(
                        onClick = {
                            onCancelTimer()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Turn Off Sleep Timer", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Fade out volume (final 30s)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = fadeOutEnabled,
                            onCheckedChange = { fadeOutEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = CosmicTeal
                            )
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(presetOptions) { (minutes, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onStartTimer(minutes, fadeOutEnabled)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onStartEndOfTrack()
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "End of track",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        }
    )
}

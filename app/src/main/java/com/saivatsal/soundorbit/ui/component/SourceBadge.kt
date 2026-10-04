package com.saivatsal.soundorbit.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.ui.theme.AudiusPurple
import com.saivatsal.soundorbit.ui.theme.DeezerOrange
import com.saivatsal.soundorbit.ui.theme.JamendoPink
import com.saivatsal.soundorbit.ui.theme.LocalGreen
import com.saivatsal.soundorbit.ui.theme.SpotifyGreen

@Composable
fun SourceBadge(
    sourceId: SourceId,
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeText) = when (sourceId) {
        SourceId.AUDIUS -> AudiusPurple.copy(alpha = 0.2f) to AudiusPurple
        SourceId.JAMENDO -> JamendoPink.copy(alpha = 0.2f) to JamendoPink
        SourceId.LOCAL -> LocalGreen.copy(alpha = 0.2f) to LocalGreen
        SourceId.SPOTIFY -> SpotifyGreen.copy(alpha = 0.2f) to SpotifyGreen
        SourceId.DEEZER -> DeezerOrange.copy(alpha = 0.2f) to DeezerOrange
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(badgeBg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = sourceId.name,
            color = badgeText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

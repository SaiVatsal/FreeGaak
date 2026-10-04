package com.saivatsal.soundorbit.ui.splash

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.saivatsal.soundorbit.R
import com.saivatsal.soundorbit.ui.theme.CosmicTeal
import com.saivatsal.soundorbit.ui.theme.DarkSurfaceVariant
import com.saivatsal.soundorbit.ui.theme.NebulaCoral
import com.saivatsal.soundorbit.ui.theme.OledBlack
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@OptIn(UnstableApi::class)
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
    splashDurationMs: Long = 2600L
) {
    val context = LocalContext.current
    val entranceAlpha = remember { Animatable(0f) }
    val entranceScale = remember { Animatable(0.85f) }
    var isVideoReady by remember { mutableStateOf(false) }

    // ExoPlayer dedicated for the splash video animation
    val splashPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            try {
                val rawUri = RawResourceDataSource.buildRawResourceUri(R.raw.splash_animation)
                val mediaItem = MediaItem.fromUri(rawUri)
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_OFF
                volume = 0f
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            isVideoReady = true
                        }
                    }
                })
            } catch (e: Exception) {
                // Fallback will render if video resource fails
            }
        }
    }

    DisposableEffect(splashPlayer) {
        onDispose {
            splashPlayer.stop()
            splashPlayer.release()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashAnimation")

    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DiscRotation"
    )

    val orbitPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbitPulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
        entranceScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        )
        delay(splashDurationMs)
        entranceAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutLinearInEasing)
        )
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .alpha(entranceAlpha.value)
            .scale(entranceScale.value),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Video Animation Container with Orbital Glow Backdrop
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                // Background Ambient Glow & Orbit Rings
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(orbitPulse)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.minDimension / 2f

                    // Outer orbit radial glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CosmicTeal.copy(alpha = glowAlpha * 0.45f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxRadius
                        ),
                        radius = maxRadius
                    )

                    // Planetary orbit line
                    drawCircle(
                        color = CosmicTeal.copy(alpha = 0.35f),
                        radius = maxRadius * 0.94f,
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))
                    )

                    // Secondary orbit line
                    drawCircle(
                        color = NebulaCoral.copy(alpha = 0.25f),
                        radius = maxRadius * 0.85f,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), 0f))
                    )
                }

                // Custom Video Animation Player
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = splashPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                )

                // Fallback Animated Spinning Vinyl CD if video is not yet ready
                if (!isVideoReady) {
                    Canvas(
                        modifier = Modifier.size(200.dp)
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val discRadius = size.minDimension / 2f

                        rotate(degrees = discRotation, pivot = center) {
                            drawVinylDisc(center = center, radius = discRadius)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Title: "Sound Orbit"
            Text(
                text = "Sound Orbit",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    letterSpacing = 2.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Glowing Feature Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CosmicTeal)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Private • Offline-First • Pure Audio",
                    style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.8.sp),
                    fontWeight = FontWeight.Medium,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Developer Credit Badge: "Developed by SaiVatsal"
            Surface(
                color = DarkSurfaceVariant.copy(alpha = 0.85f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                CosmicTeal.copy(alpha = 0.6f),
                                NebulaCoral.copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = CosmicTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Developed by ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Text(
                        text = "SaiVatsal",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun DrawScope.drawVinylDisc(center: Offset, radius: Float) {
    // 1. Vinyl Body base
    drawCircle(
        color = Color(0xFF14161A),
        radius = radius,
        center = center
    )

    // Outer rim highlight
    drawCircle(
        color = Color(0xFF2E333D),
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )

    // 2. Concentric Vinyl Grooves
    val grooveColor = Color(0xFF22262E)
    val grooveHighlight = Color(0xFF333842)
    val grooveRatios = floatArrayOf(
        0.92f, 0.88f, 0.84f, 0.80f, 0.76f, 0.72f, 0.68f, 0.64f, 0.60f, 0.56f, 0.52f, 0.48f, 0.44f
    )

    for ((index, ratio) in grooveRatios.withIndex()) {
        val strokeColor = if (index % 3 == 0) grooveHighlight.copy(alpha = 0.5f) else grooveColor.copy(alpha = 0.4f)
        val strokeWidth = if (index % 4 == 0) 1.5.dp.toPx() else 0.8.dp.toPx()
        drawCircle(
            color = strokeColor,
            radius = radius * ratio,
            center = center,
            style = Stroke(width = strokeWidth)
        )
    }

    // 3. Specular Light Shimmer / Vinyl Reflection Sheen
    val sheenBrush = Brush.sweepGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.08f),
            Color.Transparent,
            Color.Transparent,
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.08f),
            Color.Transparent
        ),
        center = center
    )
    drawCircle(
        brush = sheenBrush,
        radius = radius * 0.94f,
        center = center
    )

    // 4. Center Label Area
    val labelRadius = radius * 0.36f
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(
                CosmicTeal,
                Color(0xFF6200EA),
                NebulaCoral
            ),
            start = Offset(center.x - labelRadius, center.y - labelRadius),
            end = Offset(center.x + labelRadius, center.y + labelRadius)
        ),
        radius = labelRadius,
        center = center
    )

    // Inner label decorative ring
    drawCircle(
        color = Color.White.copy(alpha = 0.4f),
        radius = labelRadius * 0.78f,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    // Mini Orbit dots on label
    for (i in 0 until 4) {
        val angleRad = (i * 90.0) * (Math.PI / 180.0)
        val dotX = center.x + (labelRadius * 0.55f * cos(angleRad)).toFloat()
        val dotY = center.y + (labelRadius * 0.55f * sin(angleRad)).toFloat()
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = 2.dp.toPx(),
            center = Offset(dotX, dotY)
        )
    }

    // 5. Center Spindle Hole
    val spindleRadius = radius * 0.11f
    drawCircle(
        color = Color(0xFFD0D5DD),
        radius = spindleRadius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
    drawCircle(
        color = OledBlack,
        radius = spindleRadius - 1.dp.toPx(),
        center = center
    )
}

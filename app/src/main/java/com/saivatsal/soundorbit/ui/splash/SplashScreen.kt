package com.saivatsal.soundorbit.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saivatsal.soundorbit.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
    splashDurationMs: Long = 2400L
) {
    val entranceAlpha = remember { Animatable(0f) }
    val entranceScale = remember { Animatable(0.85f) }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashAnimation")

    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DiscRotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
        entranceScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
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
            .background(OledBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .alpha(entranceAlpha.value)
                .scale(entranceScale.value)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Animated Cosmic Orbital Vinyl Logo
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Background radial cosmic glow
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.width / 2f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                EmeraldGreenBright.copy(alpha = glowAlpha * 0.45f),
                                CosmicTeal.copy(alpha = glowAlpha * 0.2f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxRadius
                        ),
                        radius = maxRadius
                    )

                    // Outer orbit ring
                    drawCircle(
                        color = EmeraldGreenBright.copy(alpha = 0.4f),
                        radius = maxRadius * 0.92f,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f), 0f)
                        )
                    )

                    // Secondary orbit ring
                    drawCircle(
                        color = CosmicTeal.copy(alpha = 0.3f),
                        radius = maxRadius * 0.78f,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 14f), 0f)
                        )
                    )

                    // Orbiting satellite dots
                    val angleRad = Math.toRadians(discRotation.toDouble())
                    val orbitX = center.x + (maxRadius * 0.92f) * cos(angleRad).toFloat()
                    val orbitY = center.y + (maxRadius * 0.92f) * sin(angleRad).toFloat()
                    drawCircle(
                        color = EmeraldGreenBright,
                        radius = 4.dp.toPx(),
                        center = Offset(orbitX, orbitY)
                    )

                    val secAngleRad = Math.toRadians(-discRotation * 1.5.toDouble())
                    val secX = center.x + (maxRadius * 0.78f) * cos(secAngleRad).toFloat()
                    val secY = center.y + (maxRadius * 0.78f) * sin(secAngleRad).toFloat()
                    drawCircle(
                        color = CosmicTeal,
                        radius = 3.dp.toPx(),
                        center = Offset(secX, secY)
                    )
                }

                // Core Vinyl Icon
                Surface(
                    shape = CircleShape,
                    color = ObsidianBlack,
                    modifier = Modifier
                        .size(130.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    EmeraldGreenBright,
                                    CosmicTeal,
                                    EmeraldGreen,
                                    EmeraldGreenBright
                                )
                            ),
                            shape = CircleShape
                        ),
                    shadowElevation = 16.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Sound Orbit",
                            tint = EmeraldGreenBright,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Brand Name & Subtitle
            Text(
                text = "Sound Orbit",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your Private Universe of Sound",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMediumEmphasis
            )

            Spacer(modifier = Modifier.weight(1f))

            // Developer Credit Badge: "Developed by Grindokuu"
            Surface(
                color = DarkSurfaceVariant.copy(alpha = 0.9f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                EmeraldGreenBright.copy(alpha = 0.8f),
                                CosmicTeal.copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = EmeraldGreenBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Developed by ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Text(
                        text = "Grindokuu",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreenBright
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

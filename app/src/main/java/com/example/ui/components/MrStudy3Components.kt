package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.ai.LiveConnectionStatus
import com.example.data.model.SmartBoardElement
import com.example.data.model.SmartBoardElementType
import com.example.data.model.SmartBoardPayload
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.DeepNavyBar
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.GoldStreakAccent
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SpaceBlackBackground
import com.example.ui.theme.SpaceGlassSurface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

fun Modifier.pressableScale(
    interactionSource: MutableInteractionSource? = null,
    pressedScale: Float = 0.965f
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(stiffness = 520f, dampingRatio = 0.72f),
        label = "pressable_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun MrStudy3Backdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = MaterialTheme.colorScheme.background
    val isSpaceTheme = bg == SpaceBlackBackground || bg.red < 0.15f
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isSpaceTheme) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF070B14),
                            Color(0xFF0B1120),
                            Color(0xFF10182E),
                            Color(0xFF080D18)
                        )
                    )
                } else {
                    Brush.verticalGradient(listOf(bg, bg))
                }
            )
    ) {
        if (isSpaceTheme) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Top-left violet cosmic glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonPurplePrimary.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(size.width * 0.10f, size.height * 0.06f),
                        radius = size.maxDimension * 0.56f
                    ),
                    radius = size.maxDimension * 0.56f,
                    center = Offset(size.width * 0.10f, size.height * 0.06f)
                )
                // Mid-right electric blue aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ElectricBlueSecondary.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width * 0.94f, size.height * 0.30f),
                        radius = size.maxDimension * 0.52f
                    ),
                    radius = size.maxDimension * 0.52f,
                    center = Offset(size.width * 0.94f, size.height * 0.30f)
                )
                // Bottom cyan glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyberCyanTertiary.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.45f, size.height * 0.93f),
                        radius = size.maxDimension * 0.48f
                    ),
                    radius = size.maxDimension * 0.48f,
                    center = Offset(size.width * 0.45f, size.height * 0.93f)
                )
                // Subtle diagonal energy beams
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            ElectricBlueSecondary.copy(alpha = 0.08f),
                            NeonPurplePrimary.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, size.height * 0.16f),
                    end = Offset(size.width, size.height * 0.04f),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    accentBorder: Color = NeonPurplePrimary,
    secondaryBorder: Color = ElectricBlueSecondary,
    containerColor: Color = SpaceGlassSurface.copy(alpha = 0.80f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val interactionSource = remember { MutableInteractionSource() }
    val baseModifier = if (onClick != null) {
        modifier
            .pressableScale(interactionSource)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
    } else {
        modifier
    }

    Surface(
        modifier = baseModifier,
        shape = RoundedCornerShape(cornerRadius),
        color = if (isDark) containerColor else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(
            width = 1.15.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    accentBorder.copy(alpha = if (isDark) 0.72f else 0.34f),
                    secondaryBorder.copy(alpha = if (isDark) 0.42f else 0.18f),
                    accentBorder.copy(alpha = if (isDark) 0.28f else 0.10f)
                )
            )
        ),
        tonalElevation = if (isDark) 0.dp else 2.dp,
        shadowElevation = if (isDark) 6.dp else 2.dp
    ) {
        Box(
            modifier = Modifier.background(
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.055f),
                            Color.Transparent,
                            accentBorder.copy(alpha = 0.04f)
                        )
                    )
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun MrStudyLogoHeader(
    gradeLevel: String,
    onToggleGradeMode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Glowing 3D Cap Emblem
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF171E3A),
                border = BorderStroke(
                    1.2.dp,
                    Brush.linearGradient(listOf(NeonPurplePrimary, CyberCyanTertiary))
                ),
                shadowElevation = 8.dp,
                modifier = Modifier.size(42.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonPurplePrimary.copy(alpha = 0.45f),
                                ElectricBlueSecondary.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "MrStudy 4.0",
                        tint = CyberCyanTertiary,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "MrStudy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Surface(
                        color = ElectricBlueSecondary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            Brush.linearGradient(listOf(NeonPurplePrimary, CyberCyanTertiary))
                        )
                    ) {
                        Text(
                            text = "4.0",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyberCyanTertiary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "Daha Akıllı, Daha Güçlü Eğitim",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        GradeModeBadge(
            currentGrade = gradeLevel,
            onToggleGradeMode = onToggleGradeMode
        )
    }
}

@Composable
fun SubScreenTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    accentColor: Color = CyberCyanTertiary,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DeepNavyBar.copy(alpha = 0.94f),
        border = BorderStroke(0.9.dp, NeonPurplePrimary.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF172038),
                border = BorderStroke(1.dp, ElectricBlueSecondary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .size(44.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .testTag("subscreen_back_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri Dön",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!badgeText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun NeonGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    gradientColors: List<Color> = listOf(NeonPurplePrimary, ElectricBlueSecondary),
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val alpha = if (enabled) 1f else 0.5f
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f * alpha)),
        shadowElevation = if (enabled) 8.dp else 0.dp,
        modifier = modifier
            .heightIn(min = 48.dp)
            .pressableScale(interactionSource)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.horizontalGradient(
                    colors = gradientColors.map { it.copy(alpha = alpha) }
                )
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = Color.White),
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LiveSpectrumWaveBackground(
    modifier: Modifier = Modifier,
    accentPrimary: Color = NeonPurplePrimary,
    accentSecondary: Color = CyberCyanTertiary
) {
    val infinite = rememberInfiniteTransition(label = "spectrum")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Restart),
        label = "spectrum_phase"
    )
    Canvas(modifier = modifier) {
        val barCount = 34
        val gap = size.width / barCount
        for (i in 0 until barCount) {
            val wave = 0.35f + 0.65f * abs(sin(phase + i * 0.45f))
            val barH = size.height * wave
            val x = i * gap + gap * 0.25f
            val color = if (i % 3 == 0) accentSecondary else accentPrimary
            drawLine(
                color = color.copy(alpha = 0.32f),
                start = Offset(x, size.height),
                end = Offset(x, size.height - barH),
                strokeWidth = (gap * 0.45f).coerceAtLeast(2.dp.toPx()),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun GradeModeBadge(
    currentGrade: String,
    onToggleGradeMode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isYks = currentGrade.contains("YKS", ignoreCase = true) ||
        currentGrade.contains("12", ignoreCase = true) ||
        currentGrade.contains("11", ignoreCase = true) ||
        currentGrade.contains("Mezun", ignoreCase = true)

    val nextTarget = if (isYks) "6. Sınıf" else "YKS Hazırlık"
    val badgeBg by animateColorAsState(
        targetValue = if (isYks) ElectricBlueSecondary.copy(alpha = 0.24f) else NeonPurplePrimary.copy(alpha = 0.26f),
        label = "grade_badge_bg"
    )
    val badgeBorder by animateColorAsState(
        targetValue = if (isYks) ElectricBlueSecondary else NeonPurplePrimary,
        label = "grade_badge_border"
    )

    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = badgeBg,
        border = BorderStroke(
            1.1.dp,
            Brush.horizontalGradient(listOf(badgeBorder, CyberCyanTertiary.copy(alpha = 0.75f)))
        ),
        modifier = modifier
            .pressableScale(interactionSource)
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = androidx.compose.material3.ripple()) {
                onToggleGradeMode(nextTarget)
            }
            .testTag("grade_mode_switcher_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = badgeBorder,
                modifier = Modifier.size(15.dp)
            )
            AnimatedContent(targetState = currentGrade, label = "grade_text") { grade ->
                Text(
                    text = if (grade.contains("6")) "6. Sınıf • Ortaokul" else grade,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Seviye Modunu Değiştir",
                tint = CyberCyanTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 3D-shaded Volumetric MrStudy Robot Mascot:
 * - Spherical gradient armor (white glossy shell + obsidian cybernetic chassis)
 * - Floating graduation cap with golden tassel
 * - Glowing electric purple & cyber cyan headphones
 * - Digital black glass visor with glowing cyan expressive eyes + voice-sync waveform mouth
 */
@Composable
fun MrStudyRobotAvatar(
    status: LiveConnectionStatus,
    teacherAudioLevel: Float,
    userAudioLevel: Float,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 148.dp,
    showBook: Boolean = false
) {
    val activeAudio = when (status) {
        LiveConnectionStatus.TEACHER_SPEAKING -> teacherAudioLevel.coerceIn(0.18f, 1f)
        LiveConnectionStatus.LISTENING -> userAudioLevel.coerceIn(0f, 1f)
        else -> 0f
    }

    val smoothLevel by animateFloatAsState(
        targetValue = activeAudio,
        animationSpec = spring(stiffness = 320f, dampingRatio = 0.65f),
        label = "robot_voice_level"
    )

    val infinite = rememberInfiniteTransition(label = "robot_idle")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "robot_phase"
    )
    val blinkCycle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800),
            repeatMode = RepeatMode.Restart
        ),
        label = "robot_blink"
    )
    val isBlinking = blinkCycle > 0.93f

    Canvas(modifier = modifier.size(avatarSize)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val floatY = sin(phase) * (h * 0.022f)

        val isSpeaking = status == LiveConnectionStatus.TEACHER_SPEAKING
        val headphoneGlow = if (isSpeaking) 0.68f + smoothLevel * 0.32f else 0.48f

        val haloColor = when (status) {
            LiveConnectionStatus.TEACHER_SPEAKING -> NeonPurplePrimary
            LiveConnectionStatus.LISTENING -> CyberCyanTertiary
            LiveConnectionStatus.CONNECTING -> ElectricBlueSecondary
            else -> NeonPurplePrimary.copy(alpha = 0.65f)
        }

        // Cosmic multi-layer energy aura behind robot
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    haloColor.copy(alpha = 0.34f + smoothLevel * 0.25f),
                    ElectricBlueSecondary.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(cx, h * 0.54f + floatY),
                radius = w * (0.48f + smoothLevel * 0.08f)
            ),
            radius = w * (0.48f + smoothLevel * 0.08f),
            center = Offset(cx, h * 0.54f + floatY)
        )

        drawCircle(
            brush = Brush.linearGradient(listOf(CyberCyanTertiary.copy(alpha = 0.65f), NeonPurplePrimary.copy(alpha = 0.70f))),
            radius = w * (0.40f + smoothLevel * 0.05f),
            center = Offset(cx, h * 0.54f + floatY),
            style = Stroke(width = 1.6.dp.toPx())
        )

        // 3D-shaded white/silver chest & shoulders
        val bodyTop = h * 0.70f + floatY
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(cx - w * 0.24f, bodyTop),
            size = Size(w * 0.48f, h * 0.22f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFCBD5E1), Color(0xFF94A3B8))
            ),
            topLeft = Offset(cx - w * 0.21f, bodyTop + h * 0.02f),
            size = Size(w * 0.42f, h * 0.17f),
            cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
        )
        // Chest power core ("M" energy emblem)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(CyberCyanTertiary, NeonPurplePrimary)
            ),
            radius = w * (0.042f + smoothLevel * 0.015f),
            center = Offset(cx, bodyTop + h * 0.105f)
        )

        // Headphone Band (3D metallic + neon glow)
        drawArc(
            brush = Brush.linearGradient(listOf(NeonPurplePrimary.copy(alpha = headphoneGlow), CyberCyanTertiary.copy(alpha = headphoneGlow))),
            startAngle = 188f,
            sweepAngle = 164f,
            useCenter = false,
            topLeft = Offset(cx - w * 0.31f, h * 0.20f + floatY),
            size = Size(w * 0.62f, h * 0.52f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )

        // Left & Right 3D Glowing Earcups
        val earTop = h * 0.38f + floatY
        val earH = h * 0.23f
        val earW = w * 0.092f
        listOf(cx - w * 0.34f, cx + w * 0.34f - earW).forEachIndexed { idx, leftX ->
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
                topLeft = Offset(leftX, earTop),
                size = Size(earW, earH),
                cornerRadius = CornerRadius(earW * 0.45f, earW * 0.45f)
            )
            val glowOffset = if (idx == 0) leftX + earW * 0.22f else leftX + earW * 0.28f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        CyberCyanTertiary.copy(alpha = headphoneGlow),
                        NeonPurplePrimary.copy(alpha = headphoneGlow)
                    )
                ),
                topLeft = Offset(glowOffset, earTop + earH * 0.16f),
                size = Size(earW * 0.50f, earH * 0.68f),
                cornerRadius = CornerRadius(earW * 0.28f, earW * 0.28f)
            )
        }

        // 3D Spherical-Shaded White Futuristic Helmet
        val headLeft = cx - w * 0.27f
        val headTop = h * 0.25f + floatY
        val headW = w * 0.54f
        val headH = h * 0.45f
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFF94A3B8)),
                center = Offset(cx - headW * 0.18f, headTop + headH * 0.22f),
                radius = headW * 0.95f
            ),
            topLeft = Offset(headLeft, headTop),
            size = Size(headW, headH),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
        )
        drawRoundRect(
            color = CyberCyanTertiary.copy(alpha = 0.40f),
            topLeft = Offset(headLeft, headTop),
            size = Size(headW, headH),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
            style = Stroke(width = 1.4.dp.toPx())
        )

        // Deep Obsidian Glass Visor + Specular Top Reflection
        val visorLeft = headLeft + headW * 0.10f
        val visorTop = headTop + headH * 0.15f
        val visorW = headW * 0.80f
        val visorH = headH * 0.70f
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF0A1022), Color(0xFF030712))),
            topLeft = Offset(visorLeft, visorTop),
            size = Size(visorW, visorH),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
        )
        // Specular reflection arc on visor
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)
            ),
            topLeft = Offset(visorLeft + visorW * 0.08f, visorTop + visorH * 0.06f),
            size = Size(visorW * 0.84f, visorH * 0.24f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )

        // Expressive Cyber-Cyan Glowing Eyes (matching Görsel 1 & 2)
        val eyeY = visorTop + visorH * 0.41f
        val leftEyeX = cx - visorW * 0.21f
        val rightEyeX = cx + visorW * 0.21f
        val eyeW = visorW * 0.18f
        val eyeH = if (isBlinking) 3.dp.toPx() else visorH * (0.28f + smoothLevel * 0.04f)
        val eyeOuterGlow = if (isSpeaking) Color(0xFF38BDF8) else CyberCyanTertiary

        if (!isBlinking) {
            drawCircle(
                color = eyeOuterGlow.copy(alpha = 0.34f),
                radius = eyeW * 1.2f,
                center = Offset(leftEyeX, eyeY)
            )
            drawCircle(
                color = eyeOuterGlow.copy(alpha = 0.34f),
                radius = eyeW * 1.2f,
                center = Offset(rightEyeX, eyeY)
            )
        }
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFA5F3FC), CyberCyanTertiary)),
            topLeft = Offset(leftEyeX - eyeW / 2f, eyeY - eyeH / 2f),
            size = Size(eyeW, eyeH),
            cornerRadius = CornerRadius(eyeW / 2f, eyeW / 2f)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFA5F3FC), CyberCyanTertiary)),
            topLeft = Offset(rightEyeX - eyeW / 2f, eyeY - eyeH / 2f),
            size = Size(eyeW, eyeH),
            cornerRadius = CornerRadius(eyeW / 2f, eyeW / 2f)
        )

        // Synchronized Voice Waveform / Happy Smile inside Visor
        val mouthY = visorTop + visorH * 0.76f
        if (isSpeaking || status == LiveConnectionStatus.LISTENING) {
            val barCount = 5
            val spacing = visorW * 0.095f
            val startX = cx - ((barCount - 1) * spacing) / 2f
            for (i in 0 until barCount) {
                val barX = startX + i * spacing
                val waveOffset = sin(phase * 2.6f + i * 1.1f)
                val barH = (3.dp.toPx() + (smoothLevel * 12.dp.toPx()) * (0.55f + 0.45f * abs(waveOffset)))
                    .coerceAtMost(visorH * 0.26f)
                drawLine(
                    color = if (status == LiveConnectionStatus.LISTENING) CyberCyanTertiary else Color(0xFFC084FC),
                    start = Offset(barX, mouthY - barH / 2f),
                    end = Offset(barX, mouthY + barH / 2f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        } else {
            drawArc(
                color = Color(0xFF93C5FD),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(cx - visorW * 0.14f, mouthY - visorH * 0.10f),
                size = Size(visorW * 0.28f, visorH * 0.14f),
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Graduation Cap Floating Above Robot Head
        val capY = headTop - h * 0.01f
        val capPath = Path().apply {
            moveTo(cx, capY - h * 0.14f)
            lineTo(cx + w * 0.25f, capY - h * 0.06f)
            lineTo(cx, capY + h * 0.02f)
            lineTo(cx - w * 0.25f, capY - h * 0.06f)
            close()
        }
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(cx - w * 0.13f, capY - h * 0.05f),
            size = Size(w * 0.26f, h * 0.065f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        drawPath(
            path = capPath,
            brush = Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF312E81)))
        )
        drawPath(
            path = capPath,
            color = NeonPurplePrimary.copy(alpha = 0.90f),
            style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
        )
        val tasselEnd = Offset(cx + w * 0.23f, capY + h * 0.06f)
        drawLine(
            color = GoldStreakAccent,
            start = Offset(cx, capY - h * 0.06f),
            end = Offset(cx + w * 0.22f, capY - h * 0.05f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = GoldStreakAccent,
            start = Offset(cx + w * 0.22f, capY - h * 0.05f),
            end = tasselEnd,
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = GoldStreakAccent, radius = 3.2.dp.toPx(), center = tasselEnd)

        // Optional Mini Glowing Hologram Book (matches Görsel 2 mascot reading pose)
        if (showBook) {
            val bkW = w * 0.26f
            val bkH = h * 0.13f
            val bkX = cx + w * 0.15f
            val bkY = bodyTop + h * 0.04f
            drawRoundRect(
                brush = Brush.linearGradient(listOf(ElectricBlueSecondary.copy(alpha = 0.85f), CyberCyanTertiary.copy(alpha = 0.75f))),
                topLeft = Offset(bkX, bkY),
                size = Size(bkW, bkH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.65f),
                topLeft = Offset(bkX + 2.dp.toPx(), bkY + 2.dp.toPx()),
                size = Size(bkW - 4.dp.toPx(), bkH - 4.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

/**
 * Interactive Digital Smart Board Canvas:
 * Renders AI-generated JSON primitives (TRIANGLE, GRAPH, FORMULA, DIAGRAM) with animated draw progress.
 */
@Composable
fun SmartBoardCanvas(
    payload: SmartBoardPayload?,
    isGenerating: Boolean,
    subject: String,
    modifier: Modifier = Modifier,
    onCustomizeClick: (() -> Unit)? = null
) {
    val drawProgress = remember(payload) { Animatable(0f) }
    LaunchedEffect(payload) {
        drawProgress.snapTo(0f)
        drawProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        accentBorder = ElectricBlueSecondary,
        secondaryBorder = NeonPurplePrimary,
        containerColor = Color(0xFF0D1528).copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (isGenerating) CyberCyanTertiary else Color(0xFFF59E0B))
                    )
                    Text(
                        text = payload?.boardTitle?.ifBlank { "$subject • Dijital Akıllı Tahta" }
                            ?: "$subject • Dijital Akıllı Tahta",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = NeonPurplePrimary.copy(alpha = 0.24f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyberCyanTertiary.copy(alpha = 0.45f)),
                    modifier = if (onCustomizeClick != null) {
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onCustomizeClick)
                    } else Modifier
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyberCyanTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isGenerating) "Çiziliyor..." else if (onCustomizeClick != null) "Özelleştir" else "Akıllı Tahta",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            if (payload == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(114.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF060B16))
                ) {
                    SmartBoardGridBackground()
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Sleek Neon Smart Monitor Preview Box
                        Surface(
                            color = Color(0xFF0F1B33),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.2.dp,
                                Brush.linearGradient(listOf(CyberCyanTertiary, NeonPurplePrimary))
                            ),
                            modifier = Modifier.size(width = 88.dp, height = 74.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = CircleShape,
                                    color = ElectricBlueSecondary.copy(alpha = 0.30f),
                                    border = BorderStroke(1.dp, CyberCyanTertiary),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Dijital Akıllı Tahta Hazır",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberCyanTertiary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\"Tahtaya Çiz\" düğmesine bas veya ders anlatırken önemli üçgen, grafik ve formülleri buraya yansıt.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                val primaryElement = payload.elements.firstOrNull()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF060B16))
                ) {
                    SmartBoardGridBackground()
                    if (primaryElement != null) {
                        AnimatedSmartBoardElement(
                            element = primaryElement,
                            progress = drawProgress.value,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                        )
                    }
                }

                if (payload.formulas.isNotEmpty() || payload.summaryNote.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        payload.formulas.take(3).forEach { formula ->
                            Text(
                                text = "▸ $formula",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDDD6FE)
                            )
                        }
                        if (payload.summaryNote.isNotBlank()) {
                            Text(
                                text = payload.summaryNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartBoardGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 22.dp.toPx()
        var x = step
        while (x < size.width) {
            drawLine(
                color = Color(0xFF1E293B).copy(alpha = 0.45f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += step
        }
        var y = step
        while (y < size.height) {
            drawLine(
                color = Color(0xFF1E293B).copy(alpha = 0.45f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += step
        }
    }
}

@Composable
private fun AnimatedSmartBoardElement(
    element: SmartBoardElement,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .weight(1.1f)
                .height(110.dp)
        ) {
            val w = size.width
            val h = size.height
            when (element.type) {
                SmartBoardElementType.TRIANGLE -> {
                    val a = Offset(w * 0.22f, h * 0.85f)
                    val b = Offset(w * 0.84f, h * 0.85f)
                    val c = Offset(w * 0.36f, h * 0.16f)
                    val totalPts = listOf(a, b, c, a)
                    val segProgress = (progress * 3f).coerceIn(0f, 3f)
                    for (i in 0 until 3) {
                        val localP = (segProgress - i).coerceIn(0f, 1f)
                        if (localP > 0f) {
                            val p1 = totalPts[i]
                            val p2 = totalPts[i + 1]
                            val cur = Offset(
                                p1.x + (p2.x - p1.x) * localP,
                                p1.y + (p2.y - p1.y) * localP
                            )
                            drawLine(
                                color = CyberCyanTertiary,
                                start = p1,
                                end = cur,
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    if (progress > 0.7f) {
                        listOf(a, b, c).forEach { pt ->
                            drawCircle(
                                color = NeonPurplePrimary,
                                radius = 4.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
                SmartBoardElementType.GRAPH -> {
                    drawLine(
                        color = Color(0xFF64748B),
                        start = Offset(w * 0.10f, h * 0.12f),
                        end = Offset(w * 0.10f, h * 0.88f),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF64748B),
                        start = Offset(w * 0.10f, h * 0.88f),
                        end = Offset(w * 0.94f, h * 0.88f),
                        strokeWidth = 2.dp.toPx()
                    )
                    val ptsRaw = element.values.ifEmpty { listOf(2f, 5f, 3.5f, 7f, 9f) }
                    val minV = ptsRaw.minOrNull() ?: 0f
                    val maxV = (ptsRaw.maxOrNull() ?: 10f).let { if (it <= minV) minV + 1f else it }
                    val coords = ptsRaw.mapIndexed { idx, v ->
                        val px = w * (0.16f + (idx.toFloat() / (ptsRaw.size - 1).coerceAtLeast(1)) * 0.74f)
                        val normY = (v - minV) / (maxV - minV)
                        val py = h * (0.82f - normY * 0.62f)
                        Offset(px, py)
                    }
                    val activeSegs = ((coords.size - 1) * progress)
                    for (i in 0 until coords.size - 1) {
                        val lp = (activeSegs - i).coerceIn(0f, 1f)
                        if (lp > 0f) {
                            val p1 = coords[i]
                            val p2 = coords[i + 1]
                            drawLine(
                                color = NeonPurplePrimary,
                                start = p1,
                                end = Offset(p1.x + (p2.x - p1.x) * lp, p1.y + (p2.y - p1.y) * lp),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    coords.forEachIndexed { idx, pt ->
                        if (progress >= idx.toFloat() / coords.size) {
                            drawCircle(color = CyberCyanTertiary, radius = 4.dp.toPx(), center = pt)
                        }
                    }
                }
                SmartBoardElementType.DIAGRAM -> {
                    val nodes = 3
                    val centers = (0 until nodes).map { idx ->
                        Offset(w * (0.18f + idx * 0.32f), h * 0.50f)
                    }
                    for (i in 0 until nodes - 1) {
                        if (progress > i * 0.3f) {
                            drawLine(
                                color = ElectricBlueSecondary,
                                start = centers[i],
                                end = centers[i + 1],
                                strokeWidth = 2.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                            )
                        }
                    }
                    centers.forEachIndexed { idx, c ->
                        val alpha = ((progress * 3f) - idx * 0.5f).coerceIn(0f, 1f)
                        drawCircle(
                            color = NeonPurplePrimary.copy(alpha = alpha * 0.35f),
                            radius = 20.dp.toPx(),
                            center = c
                        )
                        drawCircle(
                            color = CyberCyanTertiary.copy(alpha = alpha),
                            radius = 12.dp.toPx(),
                            center = c,
                            style = Stroke(width = 2.2.dp.toPx())
                        )
                    }
                }
                SmartBoardElementType.FORMULA -> {
                    val frameW = w * 0.88f * progress.coerceAtLeast(0.15f)
                    drawRoundRect(
                        color = NeonPurplePrimary.copy(alpha = 0.16f),
                        topLeft = Offset(w * 0.06f, h * 0.18f),
                        size = Size(frameW, h * 0.64f),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    )
                    drawRoundRect(
                        color = CyberCyanTertiary.copy(alpha = 0.85f),
                        topLeft = Offset(w * 0.06f, h * 0.18f),
                        size = Size(frameW, h * 0.64f),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (element.title.isNotBlank()) {
                Text(
                    text = element.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyanTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (element.primaryText.isNotBlank()) {
                Text(
                    text = element.primaryText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (element.labels.isNotEmpty()) {
                Text(
                    text = element.labels.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFC4B5FD),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

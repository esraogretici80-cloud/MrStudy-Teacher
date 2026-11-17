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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SpaceBlackBackground
import com.example.ui.theme.SpaceGlassSurface
import kotlin.math.PI
import kotlin.math.sin

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
                            Color(0xFF0B0F19),
                            Color(0xFF10172A),
                            Color(0xFF090D16)
                        )
                    )
                } else {
                    Brush.verticalGradient(listOf(bg, bg))
                }
            )
    ) {
        if (isSpaceTheme) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonPurplePrimary.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.08f),
                        radius = size.maxDimension * 0.55f
                    ),
                    radius = size.maxDimension * 0.55f,
                    center = Offset(size.width * 0.12f, size.height * 0.08f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ElectricBlueSecondary.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.28f),
                        radius = size.maxDimension * 0.50f
                    ),
                    radius = size.maxDimension * 0.50f,
                    center = Offset(size.width * 0.92f, size.height * 0.28f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyberCyanTertiary.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.92f),
                        radius = size.maxDimension * 0.45f
                    ),
                    radius = size.maxDimension * 0.45f,
                    center = Offset(size.width * 0.5f, size.height * 0.92f)
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
    containerColor: Color = SpaceGlassSurface.copy(alpha = 0.78f),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = if (isDark) containerColor else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    accentBorder.copy(alpha = if (isDark) 0.52f else 0.28f),
                    ElectricBlueSecondary.copy(alpha = if (isDark) 0.24f else 0.12f)
                )
            )
        ),
        tonalElevation = if (isDark) 0.dp else 2.dp
    ) {
        Column(content = content)
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
        targetValue = if (isYks) ElectricBlueSecondary.copy(alpha = 0.22f) else NeonPurplePrimary.copy(alpha = 0.24f),
        label = "grade_badge_bg"
    )
    val badgeBorder by animateColorAsState(
        targetValue = if (isYks) ElectricBlueSecondary else NeonPurplePrimary,
        label = "grade_badge_border"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = badgeBg,
        border = BorderStroke(1.dp, badgeBorder.copy(alpha = 0.7f)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggleGradeMode(nextTarget) }
            .testTag("grade_mode_switcher_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
 * Interactive 2D/Canvas MrStudy Robot Mascot:
 * - Mezuniyet kepli (graduation cap + golden/cyan tassel)
 * - Mor ışıklı kulaklık (neon purple glowing headphones)
 * - Beyaz-siyah fütüristik gövde (white outer armor + obsidian chassis)
 * - Dijital ekran yüzünde mor parlayan ifadeli gözler + ses dalgasına senkron dijital ağız
 */
@Composable
fun MrStudyRobotAvatar(
    status: LiveConnectionStatus,
    teacherAudioLevel: Float,
    userAudioLevel: Float,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 148.dp
) {
    val activeAudio = if (status == LiveConnectionStatus.TEACHER_SPEAKING) {
        teacherAudioLevel.coerceIn(0.15f, 1f)
    } else if (status == LiveConnectionStatus.LISTENING) {
        userAudioLevel.coerceIn(0f, 1f)
    } else {
        0f
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
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "robot_phase"
    )
    val blinkCycle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600),
            repeatMode = RepeatMode.Restart
        ),
        label = "robot_blink"
    )
    val isBlinking = blinkCycle > 0.93f

    Canvas(modifier = modifier.size(avatarSize)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val floatY = sin(phase) * (h * 0.018f)

        val isSpeaking = status == LiveConnectionStatus.TEACHER_SPEAKING
        val headphoneGlow = if (isSpeaking) 0.55f + smoothLevel * 0.45f else 0.35f

        // Outer sound halo when talking/listening
        val haloColor = when (status) {
            LiveConnectionStatus.TEACHER_SPEAKING -> NeonPurplePrimary
            LiveConnectionStatus.LISTENING -> ElectricBlueSecondary
            LiveConnectionStatus.CONNECTING -> CyberCyanTertiary
            else -> NeonPurplePrimary.copy(alpha = 0.5f)
        }
        drawCircle(
            color = haloColor.copy(alpha = 0.14f + smoothLevel * 0.24f),
            radius = w * (0.42f + smoothLevel * 0.09f),
            center = Offset(cx, h * 0.54f + floatY)
        )
        drawCircle(
            color = haloColor.copy(alpha = 0.40f + smoothLevel * 0.35f),
            radius = w * (0.37f + smoothLevel * 0.05f),
            center = Offset(cx, h * 0.54f + floatY),
            style = Stroke(width = 2.dp.toPx())
        )

        // Futuristic white-black body / shoulders at bottom
        val bodyTop = h * 0.72f + floatY
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(cx - w * 0.23f, bodyTop),
            size = Size(w * 0.46f, h * 0.22f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
        )
        // White futuristic chest armor plates
        drawRoundRect(
            color = Color(0xFFF8FAFC),
            topLeft = Offset(cx - w * 0.20f, bodyTop + h * 0.02f),
            size = Size(w * 0.40f, h * 0.16f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        // Glowing blue/purple chest core indicator
        drawCircle(
            color = if (isSpeaking) NeonPurplePrimary else ElectricBlueSecondary,
            radius = w * (0.032f + smoothLevel * 0.012f),
            center = Offset(cx, bodyTop + h * 0.10f)
        )

        // Headphone Band (Neon Purple Glow)
        drawArc(
            color = NeonPurplePrimary.copy(alpha = headphoneGlow),
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(cx - w * 0.30f, h * 0.20f + floatY),
            size = Size(w * 0.60f, h * 0.52f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )

        // Left & Right Glowing Purple Headphones
        val earTop = h * 0.39f + floatY
        val earH = h * 0.22f
        val earW = w * 0.085f
        // Left earcup
        drawRoundRect(
            color = Color(0xFF1E1B4B),
            topLeft = Offset(cx - w * 0.33f, earTop),
            size = Size(earW, earH),
            cornerRadius = CornerRadius(earW * 0.45f, earW * 0.45f)
        )
        drawRoundRect(
            color = NeonPurplePrimary.copy(alpha = headphoneGlow),
            topLeft = Offset(cx - w * 0.32f, earTop + earH * 0.18f),
            size = Size(earW * 0.55f, earH * 0.64f),
            cornerRadius = CornerRadius(earW * 0.3f, earW * 0.3f)
        )
        // Right earcup
        drawRoundRect(
            color = Color(0xFF1E1B4B),
            topLeft = Offset(cx + w * 0.33f - earW, earTop),
            size = Size(earW, earH),
            cornerRadius = CornerRadius(earW * 0.45f, earW * 0.45f)
        )
        drawRoundRect(
            color = NeonPurplePrimary.copy(alpha = headphoneGlow),
            topLeft = Offset(cx + w * 0.32f - earW * 0.55f, earTop + earH * 0.18f),
            size = Size(earW * 0.55f, earH * 0.64f),
            cornerRadius = CornerRadius(earW * 0.3f, earW * 0.3f)
        )

        // White futuristic Robot Head Outer Shell
        val headLeft = cx - w * 0.26f
        val headTop = h * 0.25f + floatY
        val headW = w * 0.52f
        val headH = h * 0.44f
        drawRoundRect(
            color = Color(0xFFF8FAFC),
            topLeft = Offset(headLeft, headTop),
            size = Size(headW, headH),
            cornerRadius = CornerRadius(w * 0.13f, w * 0.13f)
        )
        drawRoundRect(
            color = Color(0xFFCBD5E1),
            topLeft = Offset(headLeft, headTop),
            size = Size(headW, headH),
            cornerRadius = CornerRadius(w * 0.13f, w * 0.13f),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Black Digital Face Visor Screen
        val visorLeft = headLeft + headW * 0.10f
        val visorTop = headTop + headH * 0.15f
        val visorW = headW * 0.80f
        val visorH = headH * 0.70f
        drawRoundRect(
            color = Color(0xFF070B14),
            topLeft = Offset(visorLeft, visorTop),
            size = Size(visorW, visorH),
            cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
        )

        // Expressive Glowing Purple Eyes
        val eyeY = visorTop + visorH * 0.38f
        val leftEyeX = cx - visorW * 0.22f
        val rightEyeX = cx + visorW * 0.22f
        val eyeW = visorW * 0.16f
        val eyeH = if (isBlinking) 3.dp.toPx() else visorH * (0.24f + smoothLevel * 0.04f)
        val eyeGlowColor = if (isSpeaking) Color(0xFFA855F7) else Color(0xFF8B5CF6)

        // Soft purple eye aura
        if (!isBlinking) {
            drawCircle(
                color = eyeGlowColor.copy(alpha = 0.28f),
                radius = eyeW * 1.15f,
                center = Offset(leftEyeX, eyeY)
            )
            drawCircle(
                color = eyeGlowColor.copy(alpha = 0.28f),
                radius = eyeW * 1.15f,
                center = Offset(rightEyeX, eyeY)
            )
        }
        drawRoundRect(
            color = Color(0xFFD8B4FE),
            topLeft = Offset(leftEyeX - eyeW / 2f, eyeY - eyeH / 2f),
            size = Size(eyeW, eyeH),
            cornerRadius = CornerRadius(eyeW / 2f, eyeW / 2f)
        )
        drawRoundRect(
            color = Color(0xFFD8B4FE),
            topLeft = Offset(rightEyeX - eyeW / 2f, eyeY - eyeH / 2f),
            size = Size(eyeW, eyeH),
            cornerRadius = CornerRadius(eyeW / 2f, eyeW / 2f)
        )

        // Synchronized Digital Mouth / Voice Waveform inside Visor
        val mouthY = visorTop + visorH * 0.75f
        val barCount = 5
        val spacing = visorW * 0.09f
        val startX = cx - ((barCount - 1) * spacing) / 2f
        for (i in 0 until barCount) {
            val barX = startX + i * spacing
            val waveOffset = sin(phase * 2.5f + i * 1.1f)
            val barH = if (isSpeaking || status == LiveConnectionStatus.LISTENING) {
                (3.dp.toPx() + (smoothLevel * 12.dp.toPx()) * (0.55f + 0.45f * kotlin.math.abs(waveOffset)))
                    .coerceAtMost(visorH * 0.26f)
            } else {
                if (i in 1..3) 3.dp.toPx() else 2.dp.toPx()
            }
            drawLine(
                color = if (status == LiveConnectionStatus.LISTENING) CyberCyanTertiary else Color(0xFFC084FC),
                start = Offset(barX, mouthY - barH / 2f),
                end = Offset(barX, mouthY + barH / 2f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Graduation Cap (Mezuniyet Kepi) on top of Robot Head
        val capY = headTop + h * 0.02f
        val capPath = Path().apply {
            moveTo(cx, capY - h * 0.14f)
            lineTo(cx + w * 0.25f, capY - h * 0.06f)
            lineTo(cx, capY + h * 0.02f)
            lineTo(cx - w * 0.25f, capY - h * 0.06f)
            close()
        }
        // Cap base crown
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(cx - w * 0.13f, capY - h * 0.05f),
            size = Size(w * 0.26f, h * 0.07f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        // Cap diamond mortarboard
        drawPath(path = capPath, color = Color(0xFF0F172A))
        drawPath(
            path = capPath,
            color = NeonPurplePrimary.copy(alpha = 0.85f),
            style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
        )
        // Golden/Cyan Tassel hanging on the right
        val tasselEnd = Offset(cx + w * 0.23f, capY + h * 0.06f)
        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(cx, capY - h * 0.06f),
            end = Offset(cx + w * 0.22f, capY - h * 0.05f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(cx + w * 0.22f, capY - h * 0.05f),
            end = tasselEnd,
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color(0xFFFBBF24),
            radius = 3.2.dp.toPx(),
            center = tasselEnd
        )
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
    modifier: Modifier = Modifier
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
        cornerRadius = 20.dp,
        accentBorder = CyberCyanTertiary,
        containerColor = Color(0xFF0E1628).copy(alpha = 0.90f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (isGenerating) Color(0xFFF59E0B) else CyberCyanTertiary)
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
                    color = NeonPurplePrimary.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyberCyanTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isGenerating) "Çiziliyor..." else "Akıllı Tahta",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyanTertiary
                        )
                    }
                }
            }

            if (payload == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(106.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF070D19)),
                    contentAlignment = Alignment.Center
                ) {
                    SmartBoardGridBackground()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Dijital Akıllı Tahta Hazır",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyberCyanTertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "\"Tahtaya Çiz\" düğmesine bas veya ders anlatırken önemli üçgen, grafik ve formülleri buraya yansıt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                val primaryElement = payload.elements.firstOrNull()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF070D19))
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
                        // Right-angle or vertex highlights
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
                    // X & Y Axes
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
                    // Glowing holographic formula frame + wave pulse
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

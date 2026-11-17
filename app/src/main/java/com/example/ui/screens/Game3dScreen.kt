package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Game3dQuestion
import com.example.ui.components.GlassCard
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Game3dScreen(
    subject: String,
    topic: String,
    questions: List<Game3dQuestion>,
    isLoading: Boolean,
    onReloadQuestions: () -> Unit,
    onGameCompleted: (correctCount: Int, totalCount: Int, score: Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        if (questions.isEmpty() && !isLoading) {
            onReloadQuestions()
        }
    }

    var currentIndex by remember(questions) { mutableIntStateOf(0) }
    var score by remember(questions) { mutableIntStateOf(0) }
    var correctCount by remember(questions) { mutableIntStateOf(0) }
    var streak by remember(questions) { mutableIntStateOf(0) }
    var gameFinished by remember(questions) { mutableStateOf(false) }
    var feedbackText by remember(questions) { mutableStateOf<String?>(null) }
    var feedbackPositive by remember(questions) { mutableStateOf(true) }

    // 3D world state: playerX in [-1.5f .. 1.5f] mapped to 4 lanes (-1.125, -0.375, 0.375, 1.125)
    var playerX by remember { mutableFloatStateOf(0f) }
    var gateDepthZ by remember(questions, currentIndex) { mutableFloatStateOf(1.0f) } // 1.0 = far horizon, 0.08 = hit plane
    var worldScroll by remember { mutableFloatStateOf(0f) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var isPausedForFeedback by remember { mutableStateOf(false) }

    val activeQuestion = questions.getOrNull(currentIndex)

    fun evaluateCollision(laneIndex: Int) {
        if (activeQuestion == null || isPausedForFeedback || gameFinished) return
        val isCorrect = laneIndex == activeQuestion.correctIndex
        if (isCorrect) {
            streak++
            correctCount++
            score += 100 + (streak - 1) * 25
            feedbackPositive = true
            feedbackText = "Harika! +${100 + (streak - 1) * 25} Puan • ${activeQuestion.explanation}"
        } else {
            streak = 0
            feedbackPositive = false
            val correctChar = ('A' + activeQuestion.correctIndex)
            feedbackText = "Doğru Şık $correctChar • ${activeQuestion.explanation}"
        }
        isPausedForFeedback = true
    }

    LaunchedEffect(questions, currentIndex, gameFinished, isPausedForFeedback) {
        if (questions.isEmpty() || gameFinished || isPausedForFeedback) return@LaunchedEffect
        var lastTimeNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastTimeNanos == 0L) {
                    lastTimeNanos = frameNanos
                    return@withFrameNanos
                }
                val dt = ((frameNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastTimeNanos = frameNanos
                worldScroll = (worldScroll + dt * 1.4f) % 1f
                rotationAngle += dt * 1.6f
                gateDepthZ -= dt * 0.135f // ~7.4 seconds per 3D flight approach
            }
            if (gateDepthZ <= 0.12f) {
                // Determine lane nearest to playerX
                val laneCenters = floatArrayOf(-1.125f, -0.375f, 0.375f, 1.125f)
                val hitLane = laneCenters.indices.minByOrNull { abs(playerX - laneCenters[it]) } ?: 0
                evaluateCollision(hitLane)
                break
            }
        }
    }

    LaunchedEffect(isPausedForFeedback) {
        if (isPausedForFeedback) {
            delay(1300L)
            feedbackText = null
            if (currentIndex + 1 >= questions.size) {
                gameFinished = true
                onGameCompleted(correctCount, questions.size, score)
            } else {
                currentIndex++
                gateDepthZ = 1.0f
            }
            isPausedForFeedback = false
        }
    }

    MrStudy3Backdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Game Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("game3d_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                    Column {
                        Text(
                            text = "MrStudy 3B Uzay Pisti",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$subject • ${topic.ifBlank { "Konu Pekiştirme" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyanTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NeonPurplePrimary.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, NeonPurplePrimary)
                ) {
                    Text(
                        text = "$score Puan",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassCard(modifier = Modifier.padding(24.dp)) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = CyberCyanTertiary)
                            Text(
                                text = "4 Hydrogen Konuya Özel 3B Sahne Hazırlıyor...",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (questions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassCard {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("3B oyun soruları henüz yüklenemedi.", color = Color.White)
                            Button(onClick = onReloadQuestions) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Soruları Üret")
                            }
                        }
                    }
                }
            } else if (gameFinished) {
                val successRate = (correctCount * 100) / questions.size.coerceAtLeast(1)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassCard(accentBorder = CyberCyanTertiary) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                color = NeonPurplePrimary.copy(alpha = 0.28f),
                                shape = CircleShape,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Text(
                                text = "3B Görev Tamamlandı!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${questions.size} soruda $correctCount doğru (%$successRate başarı) • Toplam $score Puan",
                                style = MaterialTheme.typography.titleSmall,
                                color = CyberCyanTertiary,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onBack,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Derse Dön")
                                }
                                Button(
                                    onClick = {
                                        currentIndex = 0
                                        score = 0
                                        correctCount = 0
                                        streak = 0
                                        gameFinished = false
                                        gateDepthZ = 1.0f
                                        onReloadQuestions()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tekrar Oyna")
                                }
                            }
                        }
                    }
                }
            } else if (activeQuestion != null) {
                // Question Banner
                GlassCard(accentBorder = NeonPurplePrimary) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Soru ${currentIndex + 1} / ${questions.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyanTertiary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Doğru cevabın 3B küpüne sürükle veya dokun!",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Text(
                            text = activeQuestion.prompt,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // 3D Perspective Viewport (60fps touch-steered canvas)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF060A14))
                        .pointerInput(currentIndex, isPausedForFeedback) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaNorm = (dragAmount.x / size.width.coerceAtLeast(1)) * 3.1f
                                playerX = (playerX + deltaNorm).coerceIn(-1.45f, 1.45f)
                            }
                        }
                        .pointerInput(currentIndex, isPausedForFeedback) {
                            detectTapGestures { offset ->
                                val normX = ((offset.x / size.width.coerceAtLeast(1)) * 3.0f) - 1.5f
                                playerX = normX.coerceIn(-1.45f, 1.45f)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val horizonY = h * 0.24f
                        val vanishX = w * 0.5f

                        // Sky / nebula gradient
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF0B0F19), Color(0xFF17153B), Color(0xFF080D1A))
                            )
                        )

                        // Perspective Cyber Floor Grid lines
                        val laneBounds = listOf(-1.5f, -0.75f, 0f, 0.75f, 1.5f)
                        laneBounds.forEach { lx ->
                            val topX = vanishX + lx * (w * 0.08f)
                            val bottomX = vanishX + lx * (w * 0.34f)
                            drawLine(
                                color = ElectricBlueSecondary.copy(alpha = 0.35f),
                                start = Offset(topX, horizonY),
                                end = Offset(bottomX, h),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }

                        for (step in 0..8) {
                            val z = ((step / 8f) + worldScroll * (1f / 8f)).coerceIn(0.02f, 1f)
                            val perspective = 1f - z
                            val y = horizonY + (h - horizonY) * (perspective * perspective)
                            val halfW = w * (0.12f + 0.40f * perspective)
                            drawLine(
                                color = NeonPurplePrimary.copy(alpha = 0.18f + 0.25f * perspective),
                                start = Offset(vanishX - halfW, y),
                                end = Offset(vanishX + halfW, y),
                                strokeWidth = 1.2.dp.toPx()
                            )
                        }

                        // 4 Approaching 3D Answer Monoliths (Projected Z -> Screen X, Y, Scale)
                        val laneCenters = listOf(-1.125f, -0.375f, 0.375f, 1.125f)
                        val laneColors = listOf(
                            Color(0xFF8B5CF6),
                            Color(0xFF3B82F6),
                            Color(0xFF22D3EE),
                            Color(0xFFA855F7)
                        )
                        val pScale = (1f - gateDepthZ).coerceIn(0.08f, 1f)
                        val cubeY = horizonY + (h * 0.80f - horizonY) * (pScale * pScale)
                        val cubeSize = (w * 0.165f) * (0.25f + 0.85f * pScale)

                        laneCenters.forEachIndexed { idx, lx ->
                            val spread = w * (0.09f + 0.25f * pScale)
                            val cx = vanishX + lx * spread
                            val baseColor = laneColors[idx]
                            val spinOffset = sin(rotationAngle + idx) * (cubeSize * 0.14f)

                            // 3D front face
                            val left = cx - cubeSize / 2f
                            val top = cubeY - cubeSize
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(baseColor.copy(alpha = 0.90f), baseColor.copy(alpha = 0.38f))
                                ),
                                topLeft = Offset(left, top),
                                size = Size(cubeSize, cubeSize),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                            // Isometric top cap
                            val topCap = Path().apply {
                                moveTo(left, top)
                                lineTo(left + spinOffset, top - cubeSize * 0.22f)
                                lineTo(left + cubeSize + spinOffset, top - cubeSize * 0.22f)
                                lineTo(left + cubeSize, top)
                                close()
                            }
                            drawPath(path = topCap, color = baseColor.copy(alpha = 0.55f))
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.7f),
                                topLeft = Offset(left, top),
                                size = Size(cubeSize, cubeSize),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                style = Stroke(width = 1.6.dp.toPx())
                            )
                        }

                        // Player 3D Spaceship / Hover-Orb at foreground
                        val shipX = vanishX + playerX * (w * 0.30f)
                        val shipY = h * 0.86f
                        val hover = cos(rotationAngle * 2f) * 4.dp.toPx()
                        drawCircle(
                            color = CyberCyanTertiary.copy(alpha = 0.28f),
                            radius = 28.dp.toPx(),
                            center = Offset(shipX, shipY + hover)
                        )
                        val shipPath = Path().apply {
                            moveTo(shipX, shipY - 22.dp.toPx() + hover)
                            lineTo(shipX + 20.dp.toPx(), shipY + 14.dp.toPx() + hover)
                            lineTo(shipX, shipY + 6.dp.toPx() + hover)
                            lineTo(shipX - 20.dp.toPx(), shipY + 14.dp.toPx() + hover)
                            close()
                        }
                        drawPath(path = shipPath, color = Color.White)
                        drawPath(
                            path = shipPath,
                            color = NeonPurplePrimary,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }

                    // Overlay Feedback Banner on hit
                    androidx.compose.animation.AnimatedVisibility(
                        visible = feedbackText != null,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp)
                    ) {
                        Surface(
                            color = (if (feedbackPositive) SuccessGreen else DangerRed).copy(alpha = 0.92f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = feedbackText.orEmpty(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Tappable A / B / C / D Answer Lane Cards (Accessibility + quick lane jump)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val laneCenters = listOf(-1.125f, -0.375f, 0.375f, 1.125f)
                    activeQuestion.options.take(4).forEachIndexed { idx, opt ->
                        val letter = ('A' + idx).toString()
                        val isTargeted = abs(playerX - laneCenters[idx]) < 0.38f
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isTargeted) NeonPurplePrimary.copy(alpha = 0.35f) else Color(0xFF141B2D),
                            border = BorderStroke(
                                1.5.dp,
                                if (isTargeted) CyberCyanTertiary else ElectricBlueSecondary.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !isPausedForFeedback) {
                                    playerX = laneCenters[idx]
                                    evaluateCollision(idx)
                                }
                                .testTag("game3d_option_$idx")
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyanTertiary
                                )
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

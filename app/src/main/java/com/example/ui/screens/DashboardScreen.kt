package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.LiveConnectionStatus
import com.example.data.model.DailyLearningTask
import com.example.ui.components.GlassCard
import com.example.ui.components.LiveSpectrumWaveBackground
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.MrStudyLogoHeader
import com.example.ui.components.MrStudyRobotAvatar
import com.example.ui.components.NeonGradientButton
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.GoldStreakAccent
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    state: MrStudyUiState,
    onOpenTeacher: () -> Unit,
    onOpenLiveLesson: () -> Unit,
    onOpenPdfStudio: () -> Unit,
    onOpenExamCenter: () -> Unit,
    onOpenStudyHub: () -> Unit,
    onOpen3dGame: () -> Unit = {},
    onOpenFlashcards: () -> Unit = {},
    onOpenGameStudio: () -> Unit = {},
    onOpenDiagramBoard: () -> Unit = {},
    onOpenScienceLab: () -> Unit = {},
    onOpenAiTools: () -> Unit = {},
    onOpenCodingStudio: () -> Unit = {},
    onOpenMathBoard: () -> Unit = {},
    onOpenAudioStudio: () -> Unit = {},
    onOpenDigitalLibrary: () -> Unit = {},
    onToggleLowPower3d: (Boolean) -> Unit = {},
    onMarkAllNotificationsRead: () -> Unit = {},
    onSetExamCountdown: (String, Long) -> Unit = { _, _ -> },
    onSwitchGradeMode: (String) -> Unit = {},
    onToggleDailyTask: (DailyLearningTask) -> Unit,
    onLoadMiniQuiz: (String) -> Unit,
    onMiniQuizCompleted: (Int) -> Unit,
    onOpenProfile: () -> Unit,
    onToggleTheme: (String) -> Unit,
    onOpenAppCheckSetup: () -> Unit = {}
) {
    val profile = state.profile
    val completedPlans = state.studyPlans.count { it.isCompleted }
    val avgScore = if (state.examHistory.isNotEmpty()) {
        state.examHistory.map { it.scorePercent }.average().toInt()
    } else 0

    val subjectAverages = remember(state.examHistory) {
        state.examHistory
            .groupBy { it.subject }
            .mapValues { (_, list) -> list.map { it.scorePercent }.average().toInt() }
    }
    val bestSubject = subjectAverages.maxByOrNull { it.value }
    val weakSubject = subjectAverages.minByOrNull { it.value }

    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showCountdownDialog by remember { mutableStateOf(false) }
    var robotTiltX by remember { mutableFloatStateOf(0f) }
    var robotTiltY by remember { mutableFloatStateOf(0f) }
    val unreadNotifCount = remember(state.studioState.notifications) {
        state.studioState.notifications.count { !it.isRead }
    }
    val daysToExam = remember(state.studioState.examTargetEpochMs) {
        ((state.studioState.examTargetEpochMs - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0L)
    }

    MrStudy3Backdrop {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Logo Header + Hero Profile GlassCard
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MrStudyLogoHeader(
                        gradeLevel = profile.gradeLevel,
                        onToggleGradeMode = onSwitchGradeMode
                    )

                    GlassCard(
                        accentBorder = NeonPurplePrimary,
                        secondaryBorder = CyberCyanTertiary,
                        cornerRadius = 24.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Image(
                                painter = painterResource(id = R.drawable.img_mrstudy4_hero),
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(24.dp)),
                                contentScale = ContentScale.Crop,
                                alpha = 0.24f
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        var robotClickCount by remember { mutableIntStateOf(0) }
                                        Box(
                                            contentAlignment = Alignment.TopStart,
                                            modifier = Modifier
                                                .pointerInput(Unit) {
                                                    detectDragGestures(
                                                        onDrag = { change, drag ->
                                                            change.consume()
                                                            robotTiltY = (robotTiltY + drag.x * 0.25f).coerceIn(-18f, 18f)
                                                            robotTiltX = (robotTiltX - drag.y * 0.25f).coerceIn(-18f, 18f)
                                                        },
                                                        onDragEnd = {
                                                            robotTiltX = 0f
                                                            robotTiltY = 0f
                                                        }
                                                    )
                                                }
                                                .clickable {
                                                    robotClickCount++
                                                    if (robotClickCount >= 3) {
                                                        robotClickCount = 0
                                                        onOpenAppCheckSetup()
                                                    } else {
                                                        robotTiltY = if (robotTiltY == 0f) 14f else 0f
                                                    }
                                                }
                                                .graphicsLayer {
                                                    rotationX = robotTiltX
                                                    rotationY = robotTiltY
                                                    cameraDistance = 12 * density
                                                }
                                        ) {
                                            MrStudyRobotAvatar(
                                                status = LiveConnectionStatus.IDLE,
                                                teacherAudioLevel = 0f,
                                                userAudioLevel = 0f,
                                                avatarSize = 84.dp
                                            )
                                            Surface(
                                                shape = CircleShape,
                                                color = NeonPurplePrimary,
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Merhaba, ${profile.displayName}!",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = NeonPurplePrimary.copy(alpha = 0.30f),
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Star,
                                                            contentDescription = null,
                                                            tint = Color(0xFFDDD6FE),
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "${profile.gradeLevel} • Seviye ${profile.level} (${profile.xp} XP)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    // Capsule Notifications + Theme + Profile Action Group
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF10192E).copy(alpha = 0.88f),
                                        border = BorderStroke(1.dp, ElectricBlueSecondary.copy(alpha = 0.55f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.height(42.dp)
                                        ) {
                                            IconButton(
                                                onClick = { showNotificationsDialog = true },
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .testTag("notifications_button")
                                            ) {
                                                BadgedBox(
                                                    badge = {
                                                        if (unreadNotifCount > 0) {
                                                            Badge { Text("$unreadNotifCount") }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Notifications,
                                                        contentDescription = "Bildirimler",
                                                        tint = GoldStreakAccent,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            VerticalDivider(
                                                modifier = Modifier.height(22.dp),
                                                color = Color.White.copy(alpha = 0.16f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    val next = if (profile.darkThemeMode == "light") "dark" else "light"
                                                    onToggleTheme(next)
                                                },
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .testTag("theme_toggle_button")
                                            ) {
                                                Icon(
                                                    imageVector = if (profile.darkThemeMode == "light") Icons.Default.DarkMode else Icons.Default.LightMode,
                                                    contentDescription = "Tema Değiştir",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            VerticalDivider(
                                                modifier = Modifier.height(22.dp),
                                                color = Color.White.copy(alpha = 0.16f)
                                            )
                                            IconButton(
                                                onClick = onOpenProfile,
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .testTag("open_profile_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = "Öğrenci Profili",
                                                    tint = CyberCyanTertiary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Exam Countdown + Low Power 3D + Golden Streak Pills
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = ElectricBlueSecondary.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, ElectricBlueSecondary.copy(alpha = 0.55f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable { showCountdownDialog = true }
                                    ) {
                                        Text(
                                            text = "⏳ ${state.studioState.examCountdownLabel}: $daysToExam gün",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyanTertiary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (state.studioState.lowPower3dMode) SuccessGreen.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, if (state.studioState.lowPower3dMode) SuccessGreen else Color.White.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable { onToggleLowPower3d(!state.studioState.lowPower3dMode) }
                                    ) {
                                        Text(
                                            text = if (state.studioState.lowPower3dMode) "⚡ 2D Güç Tasarrufu" else "🧊 3B Aktif",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = GoldStreakAccent.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, GoldStreakAccent.copy(alpha = 0.65f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EmojiEvents,
                                                contentDescription = null,
                                                tint = GoldStreakAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "${profile.streakDays}G 🔥",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Real XP Progress Bar
                                val xpInLevel = (profile.xp % 150L).coerceIn(0L, 150L)
                                val targetProgress = (xpInLevel / 150f).coerceIn(0.03f, 1f)
                                val animatedProgress by animateFloatAsState(
                                    targetValue = targetProgress,
                                    animationSpec = tween(700),
                                    label = "xp_bar_progress"
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFC4B5FD),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "Sonraki seviyeye ilerleme",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "$xpInLevel / 150 XP",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyanTertiary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color(0xFF0A0F1D))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(animatedProgress)
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            NeonPurplePrimary,
                                                            ElectricBlueSecondary,
                                                            CyberCyanTertiary
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Quick Start Section ("Hızlı Başlat")
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeading(
                        title = "Hızlı Başlat",
                        icon = Icons.Default.Star,
                        iconTint = CyberCyanTertiary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionTile(
                            title = "AI Öğretmen",
                            subtitle = "Ders & Görsel",
                            icon = Icons.Default.MenuBook,
                            accentColor = NeonPurplePrimary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_open_teacher"),
                            onClick = onOpenTeacher
                        )
                        QuickActionTile(
                            title = "PDF Stüdyo",
                            subtitle = "${state.pdfSources.size} Kaynak",
                            icon = Icons.Default.PictureAsPdf,
                            accentColor = ElectricBlueSecondary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_open_pdf"),
                            onClick = onOpenPdfStudio
                        )
                        QuickActionTile(
                            title = "Akıllı Sınav",
                            subtitle = "${state.examHistory.size} Sınav",
                            icon = Icons.Default.Quiz,
                            accentColor = CyberCyanTertiary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_open_exam"),
                            onClick = onOpenExamCenter
                        )
                    }

                    // Gemini Live Hero Card with Audio Spectrum Silhouette (matching Görsel 1)
                    GlassCard(
                        onClick = onOpenLiveLesson,
                        accentBorder = NeonPurplePrimary,
                        secondaryBorder = CyberCyanTertiary,
                        cornerRadius = 22.dp,
                        containerColor = Color(0xFF131432).copy(alpha = 0.88f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_open_live_lesson")
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            LiveSpectrumWaveBackground(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .align(Alignment.BottomCenter)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = NeonPurplePrimary.copy(alpha = 0.28f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, CyberCyanTertiary.copy(alpha = 0.55f)),
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.RecordVoiceOver,
                                                contentDescription = "Gemini Live",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Gemini Live • Canlı Sesli Ders",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Gerçek zamanlı karşılıklı sesli anlatım ve soru çözümü",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFCBD5E1),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                NeonGradientButton(
                                    text = "Başlat",
                                    icon = Icons.Default.PlayArrow,
                                    onClick = onOpenLiveLesson,
                                    gradientColors = listOf(NeonPurplePrimary, Color(0xFF6366F1)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    // 2-Column 3D Game + Notes & Plan Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryShortcutPill(
                            title = "3B Eğitim",
                            icon = Icons.Default.RocketLaunch,
                            accent = CyberCyanTertiary,
                            onClick = onOpen3dGame,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_open_3d_game")
                        )
                        SecondaryShortcutPill(
                            title = "Notlar & Plan",
                            icon = Icons.Default.AutoAwesome,
                            accent = NeonPurplePrimary,
                            onClick = onOpenStudyHub,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_open_study_hub")
                        )
                    }
                }
            }

            // 2B. MrStudy 4.0 Studios Hub (All 9 new interactive studios & libraries)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeading(
                        title = "MrStudy 4.0 Stüdyoları",
                        icon = Icons.Default.AutoAwesome,
                        iconTint = NeonPurplePrimary
                    )

                    FlowRow(
                        maxItemsInEachRow = 3,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StudioHubMiniTile(
                            title = "Bilgi Kartları",
                            subtitle = "${state.flashcards.size} Kart",
                            icon = Icons.Default.Style,
                            accent = CyberCyanTertiary,
                            tag = "quick_open_flashcards",
                            onClick = onOpenFlashcards,
                            modifier = Modifier.weight(1f)
                        )
                        StudioHubMiniTile(
                            title = "3B Oyun Yap",
                            subtitle = "${state.studioState.customGames.size} Oyun",
                            icon = Icons.Default.SportsEsports,
                            accent = NeonPurplePrimary,
                            tag = "quick_open_game_studio",
                            onClick = onOpenGameStudio,
                            modifier = Modifier.weight(1f)
                        )
                        StudioHubMiniTile(
                            title = "Diyagram",
                            subtitle = "${state.studioState.diagrams.size} Şema",
                            icon = Icons.Default.AccountTree,
                            accent = ElectricBlueSecondary,
                            tag = "quick_open_diagram_board",
                            onClick = onOpenDiagramBoard,
                            modifier = Modifier.weight(1f)
                        )
                        StudioHubMiniTile(
                            title = "3B Bilim Lab",
                            subtitle = "8 Deney",
                            icon = Icons.Default.Science,
                            accent = SuccessGreen,
                            tag = "quick_open_science_lab",
                            onClick = onOpenScienceLab,
                            modifier = Modifier.weight(1f)
                        )
                        StudioHubMiniTile(
                            title = "12 AI Aracı",
                            subtitle = "Ödev & Sunum",
                            icon = Icons.Default.Psychology,
                            accent = NeonPurplePrimary,
                            tag = "quick_open_ai_tools",
                            onClick = onOpenAiTools,
                            modifier = Modifier.weight(1f)
                        )
                        StudioHubMiniTile(
                            title = "Kodlama",
                            subtitle = "8 Dil + Web",
                            icon = Icons.Default.Code,
                            accent = CyberCyanTertiary,
                            tag = "quick_open_coding_studio",
                            onClick = onOpenCodingStudio,
                            modifier = Modifier.weight(1f)
                        )
                        StudioHubMiniTile(
                            title = "Mat Tahtası",
                            subtitle = "Çiz & Çöz",
                            icon = Icons.Default.Calculate,
                            accent = ElectricBlueSecondary,
                            tag = "quick_open_math_board",
                            onClick = onOpenMathBoard,
                            modifier = Modifier.weight(1f)
                        )
                        StudioHubMiniTile(
                            title = "Sesli Ders",
                            subtitle = "Podcast",
                            icon = Icons.Default.Headphones,
                            accent = GoldStreakAccent,
                            tag = "quick_open_audio_studio",
                            onClick = onOpenAudioStudio,
                            modifier = Modifier.weight(1f)
                        )
                        StudioHubMiniTile(
                            title = "Kütüphane",
                            subtitle = "Dijital Arşiv",
                            icon = Icons.Default.LocalLibrary,
                            accent = SuccessGreen,
                            tag = "quick_open_digital_library",
                            onClick = onOpenDigitalLibrary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Key Metrics Grid ("Öğrenci Gelişim Özeti") - 100% Real User Metrics
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeading(
                        title = "Öğrenci Gelişim Özeti",
                        icon = Icons.Default.BarChart,
                        iconTint = Color(0xFFC084FC)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricBox(
                                label = "Toplam Çalışma",
                                value = "${profile.totalStudyMinutes} dk",
                                detail = "Günlük hedef: ${profile.dailyGoalMinutes} dk",
                                icon = Icons.Default.TrackChanges,
                                accent = NeonPurplePrimary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricBox(
                                label = "Çözülen Soru",
                                value = "${profile.questionsSolved}",
                                detail = "Sınav + Öğretmen",
                                icon = Icons.Default.EmojiEvents,
                                accent = GoldStreakAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricBox(
                                label = "Tamamlanan Konu",
                                value = "$completedPlans / ${state.studyPlans.size}",
                                detail = "Plan hedeflerin",
                                icon = Icons.Default.Star,
                                accent = ElectricBlueSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricBox(
                                label = "Sınav Ortalaması",
                                value = if (state.examHistory.isEmpty()) "—" else "%$avgScore",
                                detail = "${state.notes.size} not • ${state.flashcards.size} kart",
                                icon = Icons.Default.CalendarMonth,
                                accent = CyberCyanTertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Visual Subject Performance Chart (GlassCard)
            item {
                GlassCard(
                    accentBorder = ElectricBlueSecondary,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = CyberCyanTertiary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ders Başarı & Çalışma Grafiği",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Planlarım →",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberCyanTertiary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onOpenStudyHub)
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (subjectAverages.isEmpty() && state.studyPlans.isEmpty()) {
                            Text(
                                text = "Henüz grafik verisi yok. İlk sınavını çöz veya çalışma planı ekle; ders bazlı başarı grafiğin burada otomatik oluşsun.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val chartData = if (subjectAverages.isNotEmpty()) {
                                subjectAverages.entries.take(5).map { it.key.take(6) to it.value.toFloat() }
                            } else {
                                listOf("Plan" to (completedPlans * 20f).coerceIn(10f, 100f))
                            }

                            val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(128.dp)
                                    .padding(horizontal = 8.dp)
                            ) {
                                val count = chartData.size.coerceAtLeast(1)
                                val slotWidth = size.width / count
                                val barWidth = (slotWidth * 0.45f).coerceAtMost(64f)
                                chartData.forEachIndexed { idx, (_, score) ->
                                    val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                                    drawRoundRect(
                                        color = trackColor,
                                        topLeft = Offset(x, 0f),
                                        size = Size(barWidth, size.height),
                                        cornerRadius = CornerRadius(14f, 14f)
                                    )
                                    val barHeight = (size.height * (score.coerceIn(5f, 100f) / 100f))
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            if (score >= 70f) listOf(CyberCyanTertiary, ElectricBlueSecondary)
                                            else listOf(NeonPurplePrimary, ElectricBlueSecondary)
                                        ),
                                        topLeft = Offset(x, size.height - barHeight),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(14f, 14f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                chartData.forEach { (label, score) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("%${score.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CyberCyanTertiary)
                                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            if (bestSubject != null || weakSubject != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (bestSubject != null) {
                                        Surface(
                                            color = SuccessGreen.copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("En Başarılı Ders", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                                                Text("${bestSubject.key} (%${bestSubject.value})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (weakSubject != null && weakSubject.key != bestSubject?.key) {
                                        Surface(
                                            color = WarningAmber.copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Geliştirilmeli", style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                                                Text("${weakSubject.key} (%${weakSubject.value})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Daily Learning Tasks & Badges
            item {
                GlassCard(
                    accentBorder = NeonPurplePrimary,
                    cornerRadius = 22.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = GoldStreakAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Günlük Öğrenme Görevleri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        state.dailyTasks.forEach { task ->
                            Surface(
                                color = if (task.isCompleted) {
                                    SuccessGreen.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (task.isCompleted) SuccessGreen.copy(alpha = 0.45f) else ElectricBlueSecondary.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onToggleDailyTask(task) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (task.isCompleted) SuccessGreen else CyberCyanTertiary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        if (task.description.isNotBlank()) {
                                            Text(
                                                text = task.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Surface(
                                        color = GoldStreakAccent.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "+${task.xpReward} XP",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = GoldStreakAccent,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (profile.badges.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldStreakAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Başarı Rozetlerin", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profile.badges.forEach { badge ->
                                    AssistChip(onClick = {}, label = { Text("🏅 $badge") })
                                }
                            }
                        }
                    }
                }
            }

            // 6. Interactive Mini Quiz Widget
            item {
                MiniQuizCard(
                    state = state,
                    onLoadMiniQuiz = onLoadMiniQuiz,
                    onMiniQuizCompleted = onMiniQuizCompleted
                )
            }

            // 7. Recent Exam History Preview
            if (state.examHistory.isNotEmpty()) {
                item {
                    GlassCard(
                        accentBorder = CyberCyanTertiary,
                        cornerRadius = 22.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Son Sınavlar & Çalışma Geçmişi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            state.examHistory.take(4).forEach { exam ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, ElectricBlueSecondary.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(exam.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                text = "${exam.subject} • ${exam.difficulty} • ${exam.correctCount}/${exam.questionCount} doğru",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "%${exam.scorePercent}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (exam.scorePercent >= 70) SuccessGreen else WarningAmber,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = {
                onMarkAllNotificationsRead()
                showNotificationsDialog = false
            },
            title = { Text("MrStudy 4.0 Bildirim Merkezi") },
            text = {
                if (state.studioState.notifications.isEmpty()) {
                    Text("Harika gidiyorsun! Şu an yeni bir bildirim bulunmuyor.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.studioState.notifications.take(8).forEach { notif ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("${notif.title} • ${notif.tag}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CyberCyanTertiary)
                                    Text(notif.body, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onMarkAllNotificationsRead()
                    showNotificationsDialog = false
                }) { Text("Tümünü Okundu Say") }
            }
        )
    }

    if (showCountdownDialog) {
        var examName by remember { mutableStateOf(state.studioState.examCountdownLabel) }
        var daysLeftText by remember { mutableStateOf(daysToExam.toString()) }
        AlertDialog(
            onDismissRequest = { showCountdownDialog = false },
            title = { Text("Sınav Geri Sayım Widget Ayarı") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = examName,
                        onValueChange = { examName = it },
                        label = { Text("Hedef Sınav (Örn: LGS / YKS / Yazılı)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = daysLeftText,
                        onValueChange = { daysLeftText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Kalan Gün Sayısı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val d = daysLeftText.toLongOrNull()?.coerceIn(0L, 1000L) ?: 30L
                    onSetExamCountdown(
                        examName.ifBlank { "Hedef Sınav" },
                        System.currentTimeMillis() + d * 86_400_000L
                    )
                    showCountdownDialog = false
                }) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { showCountdownDialog = false }) { Text("İptal") }
            }
        )
    }
}

@Composable
private fun StudioHubMiniTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        onClick = onClick,
        cornerRadius = 18.dp,
        accentBorder = accent,
        secondaryBorder = accent.copy(alpha = 0.4f),
        containerColor = Color(0xFF0E162B).copy(alpha = 0.88f),
        modifier = modifier
            .heightIn(min = 96.dp)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    icon: ImageVector,
    iconTint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.18f),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        onClick = onClick,
        cornerRadius = 20.dp,
        accentBorder = accentColor,
        secondaryBorder = accentColor.copy(alpha = 0.45f),
        containerColor = Color(0xFF0E162B).copy(alpha = 0.88f),
        modifier = modifier.heightIn(min = 128.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accentColor.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondaryShortcutPill(
    title: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        onClick = onClick,
        cornerRadius = 18.dp,
        accentBorder = accent,
        secondaryBorder = ElectricBlueSecondary,
        containerColor = Color(0xFF0E162B).copy(alpha = 0.88f),
        modifier = modifier.heightIn(min = 52.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.22f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    detail: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        cornerRadius = 20.dp,
        accentBorder = accent,
        secondaryBorder = accent.copy(alpha = 0.35f),
        containerColor = Color(0xFF0D1426).copy(alpha = 0.88f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MiniQuizCard(
    state: MrStudyUiState,
    onLoadMiniQuiz: (String) -> Unit,
    onMiniQuizCompleted: (Int) -> Unit
) {
    var selectedSubject by remember { mutableStateOf("Genel") }
    var currentIndex by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var finished by remember { mutableStateOf(false) }

    GlassCard(
        accentBorder = ElectricBlueSecondary,
        cornerRadius = 22.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberCyanTertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mini Bilgi Yarışması", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (state.isLoadingMiniQuiz) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CyberCyanTertiary)
                }
            }

            if (state.miniQuizQuestions.isEmpty()) {
                Text(
                    text = "Kısa ve keyifli 3 soruluk mini yarışmayla hem öğrendiklerini tazele hem XP kazan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Genel", "Matematik", "Fen Bilimleri", "Tarih").forEach { subj ->
                        AssistChip(
                            onClick = { selectedSubject = subj },
                            label = { Text(if (selectedSubject == subj) "✓ $subj" else subj) }
                        )
                    }
                }
                Button(
                    onClick = {
                        currentIndex = 0
                        correctCount = 0
                        selectedOption = null
                        finished = false
                        onLoadMiniQuiz(selectedSubject)
                    },
                    enabled = !state.isLoadingMiniQuiz,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("start_mini_quiz_button")
                ) {
                    Text("$selectedSubject Mini Yarışmasını Başlat")
                }
            } else if (finished) {
                Text(
                    text = "Tebrikler! 3 soruda $correctCount doğru yaptın (+${correctCount * 15 + 10} XP).",
                    style = MaterialTheme.typography.titleSmall,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = {
                        currentIndex = 0
                        correctCount = 0
                        selectedOption = null
                        finished = false
                        onLoadMiniQuiz(selectedSubject)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("Yeni Mini Yarışma Başlat")
                }
            } else {
                val q = state.miniQuizQuestions[currentIndex.coerceIn(0, state.miniQuizQuestions.lastIndex)]
                Text(
                    text = "Soru ${currentIndex + 1}/${state.miniQuizQuestions.size}: ${q.question}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                q.options.forEachIndexed { idx, opt ->
                    val isPicked = selectedOption == idx
                    val isCorrect = idx == q.correctIndex
                    val bg = when {
                        selectedOption == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        isCorrect -> SuccessGreen.copy(alpha = 0.22f)
                        isPicked -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                    }
                    Surface(
                        color = bg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            when {
                                selectedOption != null && isCorrect -> SuccessGreen
                                isPicked -> MaterialTheme.colorScheme.error
                                else -> ElectricBlueSecondary.copy(alpha = 0.28f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = selectedOption == null) {
                                selectedOption = idx
                                if (idx == q.correctIndex) correctCount++
                            }
                    ) {
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                if (selectedOption != null) {
                    Text(
                        text = q.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberCyanTertiary
                    )
                    FilledTonalButton(
                        onClick = {
                            if (currentIndex + 1 >= state.miniQuizQuestions.size) {
                                finished = true
                                onMiniQuizCompleted(correctCount)
                            } else {
                                currentIndex++
                                selectedOption = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(if (currentIndex + 1 >= state.miniQuizQuestions.size) "Sonucu Gör" else "Sonraki Soru")
                    }
                }
            }
        }
    }
}

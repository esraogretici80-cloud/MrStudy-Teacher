package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyLearningTask
import com.example.data.model.SubjectCatalog
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    state: MrStudyUiState,
    onOpenTeacher: () -> Unit,
    onOpenPdfStudio: () -> Unit,
    onOpenExamCenter: () -> Unit,
    onOpenStudyHub: () -> Unit,
    onToggleDailyTask: (DailyLearningTask) -> Unit,
    onLoadMiniQuiz: (String) -> Unit,
    onMiniQuizCompleted: (Int) -> Unit,
    onOpenProfile: () -> Unit,
    onToggleTheme: (String) -> Unit
) {
    val profile = state.profile
    val completedPlans = state.studyPlans.count { it.isCompleted }
    val avgScore = if (state.examHistory.isNotEmpty()) {
        state.examHistory.map { it.scorePercent }.average().toInt()
    } else 0

    // Group exam scores by subject for progress analytics
    val subjectAverages = remember(state.examHistory) {
        state.examHistory
            .groupBy { it.subject }
            .mapValues { (_, list) -> list.map { it.scorePercent }.average().toInt() }
    }
    val bestSubject = subjectAverages.maxByOrNull { it.value }
    val weakSubject = subjectAverages.minByOrNull { it.value }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header + Hero Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_academy),
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop,
                        alpha = 0.25f
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_teacher_avatar),
                                    contentDescription = "MrStudy Öğretmeni",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Merhaba, ${profile.displayName}!",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${profile.gradeLevel} • Seviye ${profile.level} (${profile.xp} XP)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        val next = if (profile.darkThemeMode == "dark") "light" else "dark"
                                        onToggleTheme(next)
                                    },
                                    modifier = Modifier.testTag("theme_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (profile.darkThemeMode == "dark") Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Tema Değiştir",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                IconButton(
                                    onClick = onOpenProfile,
                                    modifier = Modifier.testTag("open_profile_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Öğrenci Profili",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        val levelProgress = (profile.xp % 150L) / 150f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Sonraki seviyeye ilerleme",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "${profile.streakDays} Gün Seri 🔥",
                                style = MaterialTheme.typography.labelMedium,
                                color = WarningAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { levelProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Text(text = "Hızlı Başlat", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    title = "AI Öğretmen",
                    subtitle = "Ders & Görsel",
                    icon = Icons.Default.School,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_open_teacher"),
                    onClick = onOpenTeacher
                )
                QuickActionTile(
                    title = "PDF Stüdyo",
                    subtitle = "${state.pdfSources.size} Kaynak",
                    icon = Icons.Default.Description,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_open_pdf"),
                    onClick = onOpenPdfStudio
                )
                QuickActionTile(
                    title = "Akıllı Sınav",
                    subtitle = "${state.examHistory.size} Sınav",
                    icon = Icons.Default.Quiz,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_open_exam"),
                    onClick = onOpenExamCenter
                )
            }
        }

        // Key Metrics Grid
        item {
            Text(text = "Öğrenci Gelişim Özeti", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        label = "Toplam Çalışma",
                        value = "${profile.totalStudyMinutes} dk",
                        detail = "Günlük hedef: ${profile.dailyGoalMinutes} dk",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "Çözülen Soru",
                        value = "${profile.questionsSolved}",
                        detail = "Sınav + Öğretmen",
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
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "Sınav Ortalaması",
                        value = if (state.examHistory.isEmpty()) "—" else "%$avgScore",
                        detail = "${state.notes.size} not • ${state.flashcards.size} kart",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Visual Analytics Chart (Custom Canvas)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ders Başarı & Çalışma Grafiği", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "Planlarım →",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = onOpenStudyHub)
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

                        val barColor = MaterialTheme.colorScheme.primary
                        val accentColor = MaterialTheme.colorScheme.secondary
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
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
                                    color = if (score >= 70f) barColor else accentColor,
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
                                    Text("%${score.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                                        color = SuccessGreen.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("En Başarılı Ders", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                                            Text("${bestSubject.key} (%${bestSubject.value})", style = MaterialTheme.typography.titleSmall)
                                        }
                                    }
                                }
                                if (weakSubject != null && weakSubject.key != bestSubject?.key) {
                                    Surface(
                                        color = WarningAmber.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Geliştirilmeli", style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                                            Text("${weakSubject.key} (%${weakSubject.value})", style = MaterialTheme.typography.titleSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Learning Tasks & Badges
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Günlük Öğrenme Görevleri", style = MaterialTheme.typography.titleMedium)
                    }

                    state.dailyTasks.forEach { task ->
                        Surface(
                            color = if (task.isCompleted) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleDailyTask(task) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (task.isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, style = MaterialTheme.typography.titleSmall)
                                    if (task.description.isNotBlank()) {
                                        Text(
                                            text = task.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "+${task.xpReward} XP",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (profile.badges.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Başarı Rozetlerin", style = MaterialTheme.typography.labelLarge)
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

        // Interactive Mini Quiz Widget
        item {
            MiniQuizCard(
                state = state,
                onLoadMiniQuiz = onLoadMiniQuiz,
                onMiniQuizCompleted = onMiniQuizCompleted
            )
        }

        // Recent Exam History Preview
        if (state.examHistory.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Son Sınavlar & Çalışma Geçmişi", style = MaterialTheme.typography.titleMedium)
                        state.examHistory.take(4).forEach { exam ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exam.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .heightIn(min = 92.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = contentColor, maxLines = 1)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.8f), maxLines = 1)
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

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

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mini Bilgi Yarışması", style = MaterialTheme.typography.titleMedium)
                }
                if (state.isLoadingMiniQuiz) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            if (state.miniQuizQuestions.isEmpty()) {
                Text(
                    text = "Kısa ve keyifli 3 soruluk mini yarışmayla hem öğrendiklerini tazele hem XP kazan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    color = SuccessGreen
                )
                OutlinedButton(
                    onClick = {
                        currentIndex = 0
                        correctCount = 0
                        selectedOption = null
                        finished = false
                        onLoadMiniQuiz(selectedSubject)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Yeni Mini Yarışma Başlat")
                }
            } else {
                val q = state.miniQuizQuestions[currentIndex.coerceIn(0, state.miniQuizQuestions.lastIndex)]
                Text(
                    text = "Soru ${currentIndex + 1}/${state.miniQuizQuestions.size}: ${q.question}",
                    style = MaterialTheme.typography.titleSmall
                )
                q.options.forEachIndexed { idx, opt ->
                    val isPicked = selectedOption == idx
                    val isCorrect = idx == q.correctIndex
                    val bg = when {
                        selectedOption == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        isCorrect -> SuccessGreen.copy(alpha = 0.18f)
                        isPicked -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
                    Surface(
                        color = bg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (currentIndex + 1 >= state.miniQuizQuestions.size) "Sonucu Gör" else "Sonraki Soru")
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.LocalParentMetrics
import com.example.ui.components.GlassCard
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.pressableScale
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen

@Composable
fun ParentPanelScreen(
    state: MrStudyUiState,
    onUpdatePin: (String) -> Unit
) {
    val metrics = state.parentMetrics
    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    MrStudy3Backdrop {
        if (!isUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    accentBorder = NeonPurplePrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            color = NeonPurplePrimary.copy(alpha = 0.25f),
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Ebeveyn Koruması",
                                    tint = CyberCyanTertiary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Text(
                            text = "Ebeveyn Takip Paneli",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Öğrenci gelişim raporlarını görmek için 4 haneli PIN kodunu girin (Varsayılan: 1234).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )

                        // 4 PIN Dots with animated color
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            repeat(4) { idx ->
                                val filled = idx < enteredPin.length
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                pinError -> DangerRed
                                                filled -> CyberCyanTertiary
                                                else -> Color(0xFF1E293B)
                                            }
                                        )
                                )
                            }
                        }

                        if (pinError) {
                            Text(
                                text = "Hatalı PIN! Varsayılan şifre: 1234",
                                style = MaterialTheme.typography.labelMedium,
                                color = DangerRed
                            )
                        }

                        // Numeric Keypad with tactile pressableScale
                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("C", "0", "DEL")
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            rows.forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    row.forEach { key ->
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF1E293B).copy(alpha = 0.9f),
                                            border = BorderStroke(1.dp, NeonPurplePrimary.copy(alpha = 0.45f)),
                                            modifier = Modifier
                                                .size(64.dp)
                                                .pressableScale()
                                                .clip(CircleShape)
                                                .clickable {
                                                    pinError = false
                                                    when (key) {
                                                        "C" -> enteredPin = ""
                                                        "DEL" -> if (enteredPin.isNotEmpty()) {
                                                            enteredPin = enteredPin.dropLast(1)
                                                        }
                                                        else -> if (enteredPin.length < 4) {
                                                            val next = enteredPin + key
                                                            enteredPin = next
                                                            if (next.length == 4) {
                                                                if (next == metrics.parentPin) {
                                                                    isUnlocked = true
                                                                    enteredPin = ""
                                                                } else {
                                                                    pinError = true
                                                                    enteredPin = ""
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                .testTag("pin_key_$key")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (key == "DEL") {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.Backspace,
                                                        contentDescription = "Sil",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                } else {
                                                    Text(
                                                        text = key,
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
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
            }
        } else {
            UnlockedParentAnalytics(
                state = state,
                metrics = metrics,
                onLock = { isUnlocked = false },
                onOpenChangePin = { showChangePinDialog = true }
            )
        }
    }

    if (showChangePinDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Ebeveyn PIN Değiştir") },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it.filter(Char::isDigit).take(4) },
                    label = { Text("Yeni 4 Haneli PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length == 4) {
                            onUpdatePin(newPin)
                            showChangePinDialog = false
                        }
                    },
                    enabled = newPin.length == 4
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) { Text("İptal") }
            }
        )
    }
}

@Composable
private fun UnlockedParentAnalytics(
    state: MrStudyUiState,
    metrics: LocalParentMetrics,
    onLock: () -> Unit,
    onOpenChangePin: () -> Unit
) {
    val totalSolved = maxOf(metrics.totalQuestionsSolved.toLong(), state.profile.questionsSolved).toInt()
    val liveMinutes = metrics.totalLiveSeconds / 60
    val liveRemSec = metrics.totalLiveSeconds % 60
    val liveDurationText = if (liveMinutes > 0) "${liveMinutes} dk ${liveRemSec} sn" else "${liveRemSec} sn"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassCard(accentBorder = CyberCyanTertiary) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Ebeveyn Takip Raporu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${state.profile.displayName} • ${state.profile.gradeLevel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberCyanTertiary
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = onOpenChangePin, contentPadding = PaddingValues(horizontal = 10.dp)) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PIN", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(onClick = onLock, contentPadding = PaddingValues(horizontal = 10.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kilitle", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Primary 3 Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ParentStatCard(
                    title = "Çözülen Soru",
                    value = "$totalSolved",
                    subtitle = "Sohbet, Sınav & 3B",
                    icon = Icons.Default.Quiz,
                    accent = NeonPurplePrimary,
                    modifier = Modifier.weight(1f)
                )
                ParentStatCard(
                    title = "Canlı Ders Süresi",
                    value = liveDurationText,
                    subtitle = "${metrics.liveSessionsCount} canlı oturum",
                    icon = Icons.Default.RecordVoiceOver,
                    accent = ElectricBlueSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            GlassCard(accentBorder = CyberCyanTertiary) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            color = CyberCyanTertiary.copy(alpha = 0.20f),
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Games, contentDescription = null, tint = CyberCyanTertiary)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "3B Oyun Başarı Oranı",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${metrics.game3dPlayedCount} oyun • En yüksek: ${metrics.game3dBestScore} puan",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    Text(
                        text = if (metrics.game3dTotalQuestions > 0) "%${metrics.game3dSuccessRatePercent}" else "—",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyanTertiary
                    )
                }
            }
        }

        // Subject Progress Bars
        item {
            GlassCard(accentBorder = NeonPurplePrimary) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = NeonPurplePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ders Bazlı Gelişim Çubukları",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    val subjectsToShow = if (metrics.subjectProgress.isNotEmpty()) {
                        metrics.subjectProgress
                    } else {
                        mapOf("Matematik" to 15, "Fen Bilimleri" to 10, "Türkçe" to 10)
                    }

                    subjectsToShow.forEach { (subject, pct) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(subject, style = MaterialTheme.typography.labelLarge, color = Color.White)
                                Text("%$pct", style = MaterialTheme.typography.labelMedium, color = CyberCyanTertiary, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { (pct / 100f).coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (pct >= 70) SuccessGreen else NeonPurplePrimary,
                                trackColor = Color(0xFF1E293B)
                            )
                        }
                    }

                    if (metrics.lastLessonTopic.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Son Çalışılan Canlı Ders Konusu: ${metrics.lastLessonTopic}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, accentBorder = accent) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }
}

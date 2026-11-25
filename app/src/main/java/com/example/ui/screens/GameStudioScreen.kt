package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Custom3dGameConfig
import com.example.data.model.Game3dQuestion
import com.example.data.model.GameGenre
import com.example.data.model.SubjectCatalog
import com.example.ui.components.GlassCard
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepNavyBar
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary

@Composable
fun GameStudioScreen(
    state: MrStudyUiState,
    onBack: () -> Unit,
    onOpenClassic3dSpaceGame: () -> Unit,
    onGenerateAiGame: (genre: GameGenre, hero: String, env: String, topic: String, rules: String, subject: String) -> Unit,
    onSaveManualGame: (Custom3dGameConfig) -> Unit,
    onDeleteGame: (String) -> Unit,
    onRecordCustomGameScore: (gameId: String, score: Int, correct: Int, total: Int) -> Unit
) {
    BackHandler { onBack() }

    var playingGame by remember { mutableStateOf<Custom3dGameConfig?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var selectedGenre by remember { mutableStateOf(GameGenre.SPACE_PLATFORM) }
    var selectedSubject by remember { mutableStateOf(state.selectedSubject) }
    var topicInput by remember { mutableStateOf("") }
    var heroInput by remember { mutableStateOf(selectedGenre.defaultHero) }
    var envInput by remember { mutableStateOf(selectedGenre.defaultEnvironment) }
    var ruleInput by remember { mutableStateOf("Her doğru yanıt +100 puan ve kombo kazandırır.") }

    // Optional manual quick question fields
    var manualPrompt by remember { mutableStateOf("") }
    var manualOptA by remember { mutableStateOf("") }
    var manualOptB by remember { mutableStateOf("") }
    var manualCorrectIdx by remember { mutableIntStateOf(0) }
    var manualQuestions by remember { mutableStateOf<List<Game3dQuestion>>(emptyList()) }

    MrStudy3Backdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "3B Oyun Stüdyosu",
                subtitle = if (playingGame != null) playingGame!!.title else "Kendi Oyununu Tasarla & Oyna",
                onBack = {
                    if (playingGame != null) playingGame = null else onBack()
                },
                accentColor = CyberCyanTertiary,
                badgeText = "3.6 Flash AI"
            )

            val activePlay = playingGame
            if (activePlay != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        if (activePlay.genre == GameGenre.SPACE_PLATFORM && activePlay.questions.isNotEmpty() && activePlay.id == "__classic__") {
                            onOpenClassic3dSpaceGame()
                        } else {
                            CustomGamePlayRunner(
                                game = activePlay,
                                lowPowerMode = state.studioState.lowPower3dMode,
                                onCompleted = { sc, corr, tot -> onRecordCustomGameScore(activePlay.id, sc, corr, tot) },
                                onExitPlay = { playingGame = null }
                            )
                        }
                    }
                }
            } else {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DeepNavyBar.copy(alpha = 0.90f),
                    contentColor = CyberCyanTertiary
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Oyun Tasarla") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Oyunlarım (${state.studioState.customGames.size + 1})") })
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedTab == 0) {
                        item {
                            GlassCard(accentBorder = NeonPurplePrimary, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("1. Oyun Türünü Seç (8 Mod)", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        GameGenre.entries.forEach { g ->
                                            FilterChip(
                                                selected = selectedGenre == g,
                                                onClick = {
                                                    selectedGenre = g
                                                    heroInput = g.defaultHero
                                                    envInput = g.defaultEnvironment
                                                },
                                                label = { Text(g.label) }
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        SubjectCatalog.subjects.take(7).forEach { s ->
                                            FilterChip(selected = selectedSubject == s, onClick = { selectedSubject = s }, label = { Text(s) })
                                        }
                                    }
                                    OutlinedTextField(value = topicInput, onValueChange = { topicInput = it }, label = { Text("Oyun Konusu / Ünite") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = heroInput, onValueChange = { heroInput = it }, label = { Text("Karakter / Kahraman") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = envInput, onValueChange = { envInput = it }, label = { Text("3B Sahne Ortamı") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = ruleInput, onValueChange = { ruleInput = it }, label = { Text("Oyun Kuralları") }, modifier = Modifier.fillMaxWidth())

                                    NeonGradientButton(
                                        text = if (state.isGeneratingCustomGame) "3.6 Flash Oyunu Üretiyor..." else "AI ile Tam Oyun Üret",
                                        icon = Icons.Default.AutoAwesome,
                                        enabled = !state.isGeneratingCustomGame,
                                        onClick = {
                                            onGenerateAiGame(selectedGenre, heroInput, envInput, topicInput.ifBlank { selectedSubject }, ruleInput, selectedSubject)
                                            selectedTab = 1
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("game_studio_ai_generate_btn")
                                    )
                                    if (state.isGeneratingCustomGame) {
                                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp))
                                    }
                                }
                            }
                        }

                        item {
                            GlassCard(accentBorder = ElectricBlueSecondary, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("2. Veya Kendi Manuel Sorularını Ekle (${manualQuestions.size} soru)", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = manualPrompt, onValueChange = { manualPrompt = it }, label = { Text("Soru Metni") }, modifier = Modifier.fillMaxWidth())
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = manualOptA, onValueChange = { manualOptA = it }, label = { Text("A Şıkkı") }, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = manualOptB, onValueChange = { manualOptB = it }, label = { Text("B Şıkkı") }, modifier = Modifier.weight(1f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Doğru:", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelSmall)
                                        FilterChip(selected = manualCorrectIdx == 0, onClick = { manualCorrectIdx = 0 }, label = { Text("A") })
                                        FilterChip(selected = manualCorrectIdx == 1, onClick = { manualCorrectIdx = 1 }, label = { Text("B") })
                                        Spacer(Modifier.weight(1f))
                                        OutlinedButton(
                                            onClick = {
                                                if (manualPrompt.isNotBlank() && manualOptA.isNotBlank() && manualOptB.isNotBlank()) {
                                                    manualQuestions = manualQuestions + Game3dQuestion(
                                                        prompt = manualPrompt,
                                                        options = listOf(manualOptA, manualOptB, "C", "D"),
                                                        correctIndex = manualCorrectIdx,
                                                        explanation = "Öğrenci tasarımı soru"
                                                    )
                                                    manualPrompt = ""; manualOptA = ""; manualOptB = ""
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Soru Ekle")
                                        }
                                    }
                                    if (manualQuestions.isNotEmpty()) {
                                        Button(
                                            onClick = {
                                                onSaveManualGame(
                                                    Custom3dGameConfig(
                                                        title = "${topicInput.ifBlank { selectedSubject }} • Özel",
                                                        subject = selectedSubject,
                                                        topic = topicInput.ifBlank { selectedSubject },
                                                        genre = selectedGenre,
                                                        heroName = heroInput,
                                                        environmentName = envInput,
                                                        ruleSummary = ruleInput,
                                                        questions = manualQuestions
                                                    )
                                                )
                                                manualQuestions = emptyList()
                                                selectedTab = 1
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Manuel Oyunu Kaydet") }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            GlassCard(accentBorder = CyberCyanTertiary, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("3B Uzay Yörünge Koşucusu (Klasik)", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("Gerçek zamanlı 3B şerit değiştirme ve uzay kapıları", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                    }
                                    NeonGradientButton(
                                        text = "Oyna",
                                        icon = Icons.Default.RocketLaunch,
                                        onClick = onOpenClassic3dSpaceGame,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                        modifier = Modifier.testTag("play_classic_3d_btn")
                                    )
                                }
                            }
                        }
                        items(state.studioState.customGames, key = { it.id }) { game ->
                            GlassCard(accentBorder = NeonPurplePrimary, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(game.title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("${game.genre.label} • ${game.subject} • Rekor: ${game.highScore}", style = MaterialTheme.typography.labelSmall, color = CyberCyanTertiary)
                                        Text("${game.heroName} • ${game.environmentName}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                    }
                                    Row {
                                        IconButton(onClick = { onDeleteGame(game.id) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Sil", tint = DangerRed)
                                        }
                                        Button(onClick = { playingGame = game }) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Başlat")
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
}

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Custom3dGameConfig
import com.example.data.model.GameGenre
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonGradientButton
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomGamePlayRunner(
    game: Custom3dGameConfig,
    lowPowerMode: Boolean,
    onCompleted: (score: Int, correct: Int, total: Int) -> Unit,
    onExitPlay: () -> Unit
) {
    when (game.genre) {
        GameGenre.WORD_MATCH, GameGenre.MEMORY_PAIR -> PairMatchingGameBoard(
            game = game,
            isMemoryFlip = game.genre == GameGenre.MEMORY_PAIR,
            onCompleted = onCompleted,
            onExitPlay = onExitPlay
        )
        else -> MultiChoice3dArenaRunner(
            game = game,
            lowPowerMode = lowPowerMode,
            onCompleted = onCompleted,
            onExitPlay = onExitPlay
        )
    }
}

@Composable
private fun MultiChoice3dArenaRunner(
    game: Custom3dGameConfig,
    lowPowerMode: Boolean,
    onCompleted: (Int, Int, Int) -> Unit,
    onExitPlay: () -> Unit
) {
    val questions = game.questions
    if (questions.isEmpty()) {
        GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bu oyunda henüz soru yok.", color = Color.White, fontWeight = FontWeight.Bold)
                Button(onClick = onExitPlay) { Text("Stüdyoya Dön") }
            }
        }
        return
    }

    var idx by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }
    var pulse by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(lowPowerMode) {
        if (lowPowerMode) return@LaunchedEffect
        while (true) {
            delay(32L)
            pulse = (pulse + 0.08f) % 6.283f
        }
    }

    if (finished) {
        GlassCard(accentBorder = SuccessGreen, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${game.heroName} Görevi Tamamladı!", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                Text("Puan: $score • Doğru: $correct/${questions.size}", color = CyberCyanTertiary, fontWeight = FontWeight.Bold)
                NeonGradientButton(text = "Kütüphaneye Dön", onClick = onExitPlay, modifier = Modifier.fillMaxWidth())
            }
        }
        return
    }

    val q = questions[idx]
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassCard(accentBorder = NeonPurplePrimary, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${game.genre.label} • ${game.heroName}", style = MaterialTheme.typography.labelSmall, color = CyberCyanTertiary)
                    Text("Puan: $score (${idx + 1}/${questions.size})", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
                if (!lowPowerMode) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(92.dp)) {
                        val cy = size.height / 2f
                        drawLine(ElectricBlueSecondary.copy(alpha = 0.25f), Offset(0f, cy), Offset(size.width, cy), 3f)
                        val playerX = (size.width * ((idx + 0.5f) / questions.size.coerceAtLeast(1)))
                        drawCircle(CyberCyanTertiary.copy(alpha = 0.35f), radius = 26f + kotlin.math.sin(pulse) * 4f, center = Offset(playerX, cy))
                        drawCircle(NeonPurplePrimary, radius = 14f, center = Offset(playerX, cy))
                    }
                }
                Text(q.prompt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                q.options.forEachIndexed { oIdx, opt ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF172038),
                        border = BorderStroke(1.dp, ElectricBlueSecondary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth().clickable(enabled = feedback == null) {
                            val ok = oIdx == q.correctIndex
                            if (ok) {
                                correct++
                                score += 100
                                feedback = "✅ Doğru! +100 Puan. ${q.explanation}"
                            } else {
                                feedback = "❌ Doğru cevap: ${q.options.getOrNull(q.correctIndex)}. ${q.explanation}"
                            }
                        }
                    ) {
                        Text(
                            text = "${'A' + oIdx}) $opt",
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                if (feedback != null) {
                    Text(feedback.orEmpty(), color = CyberCyanTertiary, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        feedback = null
                        if (idx < questions.lastIndex) idx++ else {
                            finished = true
                            onCompleted(score, correct, questions.size)
                        }
                    }) {
                        Text(if (idx < questions.lastIndex) "Sonraki" else "Görevi Bitir")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PairMatchingGameBoard(
    game: Custom3dGameConfig,
    isMemoryFlip: Boolean,
    onCompleted: (Int, Int, Int) -> Unit,
    onExitPlay: () -> Unit
) {
    val pairs = remember(game) {
        game.wordPairs.ifEmpty {
            listOf("F=m·a" to "Newton 2. Yasası", "H₂O" to "Su Molekülü", "a²+b²=c²" to "Pisagor", "ATP" to "Hücre Enerjisi")
        }.take(6)
    }
    data class Tile(val id: String, val pairIndex: Int, val text: String)
    val tiles = remember(pairs) {
        pairs.flatMapIndexed { pIdx, (a, b) ->
            listOf(Tile("L_$pIdx", pIdx, a), Tile("R_$pIdx", pIdx, b))
        }.shuffled()
    }
    var matchedPairs by remember { mutableStateOf(setOf<Int>()) }
    var firstSelected by remember { mutableStateOf<Tile?>(null) }
    var secondSelected by remember { mutableStateOf<Tile?>(null) }
    var moves by remember { mutableIntStateOf(0) }

    LaunchedEffect(secondSelected) {
        val f = firstSelected
        val s = secondSelected
        if (f != null && s != null) {
            moves++
            delay(500L)
            if (f.pairIndex == s.pairIndex && f.id != s.id) {
                matchedPairs = matchedPairs + f.pairIndex
                if (matchedPairs.size == pairs.size) {
                    val finalScore = (pairs.size * 120 - moves * 10).coerceAtLeast(100)
                    onCompleted(finalScore, pairs.size, pairs.size)
                }
            }
            firstSelected = null
            secondSelected = null
        }
    }

    GlassCard(accentBorder = CyberCyanTertiary, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (isMemoryFlip) "Hafıza Kartlarını Aç" else "Doğru Çiftleri Eşleştir", color = CyberCyanTertiary, fontWeight = FontWeight.Bold)
                Text("${matchedPairs.size}/${pairs.size} • Hamle: $moves", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            if (matchedPairs.size == pairs.size) {
                Text("Tebrikler! Tüm eşleştirmeleri tamamladın.", color = SuccessGreen, fontWeight = FontWeight.Bold)
                Button(onClick = onExitPlay) { Text("Tamamla ve Dön") }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tiles.forEach { tile ->
                        val isMatched = tile.pairIndex in matchedPairs
                        val isOpen = isMatched || tile.id == firstSelected?.id || tile.id == secondSelected?.id || !isMemoryFlip
                        val isPicked = tile.id == firstSelected?.id || tile.id == secondSelected?.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                isMatched -> SuccessGreen.copy(alpha = 0.25f)
                                isPicked -> NeonPurplePrimary.copy(alpha = 0.35f)
                                else -> Color(0xFF172038)
                            },
                            border = BorderStroke(1.dp, if (isMatched) SuccessGreen else if (isPicked) CyberCyanTertiary else Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth(0.47f)
                                .clickable(enabled = !isMatched && secondSelected == null && tile.id != firstSelected?.id) {
                                    if (firstSelected == null) firstSelected = tile
                                    else secondSelected = tile
                                }
                        ) {
                            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isOpen) tile.text else "❓ Kart",
                                    color = if (isMatched) Color(0xFF86EFAC) else Color.White,
                                    style = MaterialTheme.typography.labelMedium,
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

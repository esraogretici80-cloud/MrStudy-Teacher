package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SavedMathBoard
import com.example.ui.components.GlassCard
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen

private data class MathStroke(
    val points: List<Offset>,
    val colorHex: Long,
    val widthPx: Float
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MathBoardScreen(
    state: MrStudyUiState,
    onBack: () -> Unit,
    onSolveMath: (String, Bitmap?) -> Unit,
    onSaveBoard: (SavedMathBoard) -> Unit,
    onDeleteBoard: (String) -> Unit,
    onExportBoard: (title: String, bitmap: Bitmap, asPdf: Boolean) -> Unit
) {
    BackHandler { onBack() }

    var expressionText by remember { mutableStateOf("2x² - 8 = 0") }
    var boardTitle by remember { mutableStateOf("Matematik Tahtam") }
    var penColorHex by remember { mutableStateOf(0xFF22D3EE) }
    var isEraser by remember { mutableStateOf(false) }

    val strokes = remember { mutableStateListOf<MathStroke>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    fun renderBoardToBitmap(): Bitmap {
        val w = 1000
        val h = 720
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        c.drawColor(android.graphics.Color.rgb(9, 13, 24))
        val gridPaint = Paint().apply {
            color = android.graphics.Color.argb(40, 148, 163, 184)
            strokeWidth = 1.5f
        }
        for (gx in 0..w step 50) c.drawLine(gx.toFloat(), 0f, gx.toFloat(), h.toFloat(), gridPaint)
        for (gy in 0..h step 50) c.drawLine(0f, gy.toFloat(), w.toFloat(), gy.toFloat(), gridPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 36f
        }
        c.drawText(expressionText, 32f, 54f, textPaint)

        strokes.forEach { st ->
            if (st.points.size >= 2) {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = st.colorHex.toInt()
                    strokeWidth = st.widthPx
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                }
                for (i in 0 until st.points.lastIndex) {
                    c.drawLine(st.points[i].x, st.points[i].y, st.points[i + 1].x, st.points[i + 1].y, p)
                }
            }
        }
        return bmp
    }

    MrStudy3Backdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "Matematik Çizim & Denklem Tahtası",
                subtitle = "El Çizimi + Sembol Tuş Takımı + AI Çözüm",
                onBack = onBack,
                accentColor = CyberCyanTertiary,
                badgeText = "${state.studioState.mathBoards.size} Kayıt"
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GlassCard(cornerRadius = 20.dp, accentBorder = ElectricBlueSecondary, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = boardTitle,
                                onValueChange = { boardTitle = it },
                                label = { Text("Çalışma Başlığı") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = expressionText,
                                onValueChange = { expressionText = it },
                                label = { Text("Matematik İfadesi / Problem") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("math_expression_input")
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("x²", "√(", "π", "∑", "∫", "÷", "×", "±", "≤", "≥", "≠", "α", "β", "θ", "°").forEach { sym ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { expressionText += sym },
                                        label = { Text(sym) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    0xFF22D3EE to "Camgöbeği",
                                    0xFFA855F7 to "Mor",
                                    0xFFFBBF24 to "Altın",
                                    0xFFF8FAFC to "Beyaz"
                                ).forEach { (hex, label) ->
                                    FilterChip(
                                        selected = !isEraser && penColorHex == hex,
                                        onClick = {
                                            isEraser = false
                                            penColorHex = hex
                                        },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                                FilterChip(
                                    selected = isEraser,
                                    onClick = { isEraser = true },
                                    label = { Text("Silgi", style = MaterialTheme.typography.labelSmall) }
                                )
                                OutlinedButton(onClick = { strokes.clear() }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Temizle")
                                }
                            }

                            // Interactive Drawing Canvas
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF090D18))
                                    .pointerInput(penColorHex, isEraser) {
                                        detectDragGestures(
                                            onDragStart = { start -> currentStroke = listOf(start) },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                currentStroke = currentStroke + change.position
                                            },
                                            onDragEnd = {
                                                if (currentStroke.size >= 2) {
                                                    strokes.add(
                                                        MathStroke(
                                                            points = currentStroke,
                                                            colorHex = if (isEraser) 0xFF090D18 else penColorHex,
                                                            widthPx = if (isEraser) 26f else 6f
                                                        )
                                                    )
                                                }
                                                currentStroke = emptyList()
                                            }
                                        )
                                    }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val step = 28.dp.toPx()
                                    var x = 0f
                                    while (x < size.width) {
                                        drawLine(Color.White.copy(alpha = 0.06f), Offset(x, 0f), Offset(x, size.height))
                                        x += step
                                    }
                                    var y = 0f
                                    while (y < size.height) {
                                        drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, y), Offset(size.width, y))
                                        y += step
                                    }

                                    (strokes + listOfNotNull(
                                        if (currentStroke.size >= 2) {
                                            MathStroke(
                                                currentStroke,
                                                if (isEraser) 0xFF090D18 else penColorHex,
                                                if (isEraser) 26f else 6f
                                            )
                                        } else null
                                    )).forEach { st ->
                                        val path = Path().apply {
                                            moveTo(st.points.first().x, st.points.first().y)
                                            st.points.drop(1).forEach { lineTo(it.x, it.y) }
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color(st.colorHex),
                                            style = Stroke(width = st.widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NeonGradientButton(
                                    text = if (state.isSolvingMath) "Çözülüyor..." else "Tahtayı AI ile Çöz",
                                    icon = Icons.Default.AutoAwesome,
                                    enabled = !state.isSolvingMath,
                                    onClick = { onSolveMath(expressionText, renderBoardToBitmap()) },
                                    modifier = Modifier.weight(1.3f).testTag("solve_math_board_button")
                                )
                                OutlinedButton(
                                    onClick = {
                                        val sol = state.mathSolutionResult?.finalAnswer.orEmpty()
                                        onSaveBoard(
                                            SavedMathBoard(
                                                title = boardTitle.ifBlank { "Matematik Tahtası" },
                                                expression = expressionText,
                                                solutionSummary = sol
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(0.9f).heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kaydet")
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onExportBoard(boardTitle, renderBoardToBitmap(), false) },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PNG Paylaş")
                                }
                                OutlinedButton(
                                    onClick = { onExportBoard(boardTitle, renderBoardToBitmap(), true) },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PDF Kaydet")
                                }
                            }
                        }
                    }
                }

                if (state.isSolvingMath) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CyberCyanTertiary)
                            Text("Denklem ve çizim analiz ediliyor...")
                        }
                    }
                }

                state.mathSolutionResult?.let { res ->
                    item {
                        GlassCard(accentBorder = SuccessGreen, cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Sonuç: ${res.finalAnswer}", style = MaterialTheme.typography.titleMedium, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                res.steps.forEachIndexed { idx, step ->
                                    Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                if (state.studioState.mathBoards.isNotEmpty()) {
                    item {
                        Text("Kayıtlı Matematik Tahtaları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(state.studioState.mathBoards, key = { it.id }) { b ->
                        GlassCard(
                            onClick = {
                                boardTitle = b.title
                                expressionText = b.expression
                            },
                            accentBorder = NeonPurplePrimary,
                            cornerRadius = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(b.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(b.expression, style = MaterialTheme.typography.bodySmall, color = CyberCyanTertiary)
                                }
                                IconButton(onClick = { onDeleteBoard(b.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Tahtayı Sil")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

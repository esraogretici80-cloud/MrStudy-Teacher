package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DiagramEdge
import com.example.data.model.DiagramNode
import com.example.data.model.DiagramProject
import com.example.data.model.DiagramType
import com.example.ui.components.GlassCard
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagramBoardScreen(
    state: MrStudyUiState,
    onBack: () -> Unit,
    onGenerateAiDiagram: (topic: String, type: DiagramType, subject: String) -> Unit,
    onSaveDiagram: (DiagramProject) -> Unit,
    onDeleteDiagram: (String) -> Unit,
    onExplainNode: (nodeLabel: String, diagramTopic: String, subject: String, actionType: String) -> Unit,
    onClearNodeExplanation: () -> Unit,
    onExportDiagram: (DiagramProject, Bitmap?, String) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var currentProject by remember(state.activeDiagram) {
        mutableStateOf(
            state.activeDiagram ?: DiagramProject(
                title = "Örnek Kavram Haritası",
                subject = state.selectedSubject,
                diagramType = DiagramType.CONCEPT_MAP,
                nodes = listOf(
                    DiagramNode(id = "n1", label = "Ana Kavram", note = "Merkez fikir", x = 0.5f, y = 0.24f, colorHex = 0xFF8B5CF6),
                    DiagramNode(id = "n2", label = "Alt Başlık A", note = "Temel kural", x = 0.25f, y = 0.65f, colorHex = 0xFF3B82F6),
                    DiagramNode(id = "n3", label = "Alt Başlık B", note = "Örnek uygulama", x = 0.75f, y = 0.65f, colorHex = 0xFF22D3EE)
                ),
                edges = listOf(
                    DiagramEdge(fromId = "n1", toId = "n2", label = "içerir"),
                    DiagramEdge(fromId = "n1", toId = "n3", label = "uygular")
                )
            )
        )
    }

    var selectedType by remember { mutableStateOf(currentProject.diagramType) }
    var aiTopic by remember { mutableStateOf("") }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var connectSourceId by remember { mutableStateOf<String?>(null) }
    var canvasZoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showAddNodeDialog by remember { mutableStateOf(false) }

    val selectedNode = currentProject.nodes.firstOrNull { it.id == selectedNodeId }

    MrStudy3Backdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "Akıllı Diyagram Tahtası",
                subtitle = "Sürükle-Bırak • 8 Şablon • AI Düğüm Asistanı",
                onBack = onBack,
                accentColor = CyberCyanTertiary,
                badgeText = "${currentProject.nodes.size} Kutu"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GlassCard(accentBorder = NeonPurplePrimary, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DiagramType.entries.forEach { t ->
                                    FilterChip(
                                        selected = selectedType == t,
                                        onClick = {
                                            selectedType = t
                                            currentProject = currentProject.copy(diagramType = t)
                                        },
                                        label = { Text(t.label) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = aiTopic,
                                    onValueChange = { aiTopic = it },
                                    label = { Text("AI Diyagram Konusu") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                NeonGradientButton(
                                    text = if (state.isGeneratingDiagram) "..." else "AI Üret",
                                    icon = Icons.Default.AutoAwesome,
                                    enabled = !state.isGeneratingDiagram,
                                    onClick = { onGenerateAiDiagram(aiTopic.ifBlank { state.selectedSubject }, selectedType, state.selectedSubject) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.testTag("diagram_ai_generate_btn")
                                )
                            }
                        }
                    }
                }

                // Interactive Canvas
                item {
                    GlassCard(accentBorder = ElectricBlueSecondary, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(onClick = { showAddNodeDialog = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Kutu Ekle")
                                }
                                OutlinedButton(
                                    onClick = { connectSourceId = selectedNodeId },
                                    enabled = selectedNodeId != null,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (connectSourceId != null) "Hedef Kutuya Dokun" else "Bağla")
                                }
                                IconButton(onClick = { canvasZoom = (canvasZoom + 0.15f).coerceAtMost(2f) }) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Yakınlaştır", tint = Color.White)
                                }
                                IconButton(onClick = { canvasZoom = (canvasZoom - 0.15f).coerceAtLeast(0.65f) }) {
                                    Icon(Icons.Default.ZoomOut, contentDescription = "Uzaklaştır", tint = Color.White)
                                }
                                IconButton(onClick = { onSaveDiagram(currentProject) }) {
                                    Icon(Icons.Default.Save, contentDescription = "Kaydet", tint = CyberCyanTertiary)
                                }
                            }

                            var draggedNodeId by remember { mutableStateOf<String?>(null) }
                            var canvasSize by remember { mutableStateOf(Size(800f, 520f)) }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(currentProject.nodes, canvasZoom, panOffset) {
                                            detectTapGestures { tap ->
                                                canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                                                val hit = currentProject.nodes.lastOrNull { n ->
                                                    val nx = (n.x * size.width - size.width / 2f) * canvasZoom + size.width / 2f + panOffset.x
                                                    val ny = (n.y * size.height - size.height / 2f) * canvasZoom + size.height / 2f + panOffset.y
                                                    kotlin.math.abs(tap.x - nx) <= 95f && kotlin.math.abs(tap.y - ny) <= 46f
                                                }
                                                if (hit != null && connectSourceId != null && connectSourceId != hit.id) {
                                                    val newEdge = DiagramEdge(fromId = connectSourceId!!, toId = hit.id, label = "ilişkili")
                                                    currentProject = currentProject.copy(edges = currentProject.edges + newEdge)
                                                    connectSourceId = null
                                                } else {
                                                    selectedNodeId = hit?.id
                                                    connectSourceId = null
                                                }
                                            }
                                        }
                                        .pointerInput(currentProject.nodes, canvasZoom) {
                                            detectDragGestures(
                                                onDragStart = { start ->
                                                    val hit = currentProject.nodes.lastOrNull { n ->
                                                        val nx = (n.x * size.width - size.width / 2f) * canvasZoom + size.width / 2f + panOffset.x
                                                        val ny = (n.y * size.height - size.height / 2f) * canvasZoom + size.height / 2f + panOffset.y
                                                        kotlin.math.abs(start.x - nx) <= 95f && kotlin.math.abs(start.y - ny) <= 46f
                                                    }
                                                    draggedNodeId = hit?.id
                                                },
                                                onDragEnd = { draggedNodeId = null },
                                                onDrag = { change, drag ->
                                                    change.consume()
                                                    val targetId = draggedNodeId
                                                    if (targetId != null) {
                                                        currentProject = currentProject.copy(
                                                            nodes = currentProject.nodes.map { n ->
                                                                if (n.id == targetId) {
                                                                    val dx = drag.x / (size.width * canvasZoom).coerceAtLeast(1f)
                                                                    val dy = drag.y / (size.height * canvasZoom).coerceAtLeast(1f)
                                                                    n.copy(x = (n.x + dx).coerceIn(0.08f, 0.92f), y = (n.y + dy).coerceIn(0.08f, 0.92f))
                                                                } else n
                                                            }
                                                        )
                                                    } else {
                                                        panOffset = Offset(
                                                            (panOffset.x + drag.x).coerceIn(-300f, 300f),
                                                            (panOffset.y + drag.y).coerceIn(-220f, 220f)
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    canvasSize = size
                                    fun toScreen(nx: Float, ny: Float) = Offset(
                                        (nx * size.width - size.width / 2f) * canvasZoom + size.width / 2f + panOffset.x,
                                        (ny * size.height - size.height / 2f) * canvasZoom + size.height / 2f + panOffset.y
                                    )

                                    val nodeMap = currentProject.nodes.associateBy { it.id }
                                    currentProject.edges.forEach { edge ->
                                        val from = nodeMap[edge.fromId] ?: return@forEach
                                        val to = nodeMap[edge.toId] ?: return@forEach
                                        val p1 = toScreen(from.x, from.y)
                                        val p2 = toScreen(to.x, to.y)
                                        drawLine(CyberCyanTertiary.copy(alpha = 0.72f), p1, p2, strokeWidth = 4f)
                                        if (edge.label.isNotBlank()) {
                                            drawContext.canvas.nativeCanvas.drawText(
                                                edge.label.take(14),
                                                (p1.x + p2.x) / 2f,
                                                (p1.y + p2.y) / 2f - 8f,
                                                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                    color = android.graphics.Color.rgb(148, 163, 184)
                                                    textSize = 24f
                                                    textAlign = Paint.Align.CENTER
                                                }
                                            )
                                        }
                                    }

                                    currentProject.nodes.forEach { node ->
                                        val c = toScreen(node.x, node.y)
                                        val w = 178f
                                        val h = 76f
                                        val col = Color(node.colorHex)
                                        val isSel = node.id == selectedNodeId
                                        drawRoundRect(
                                            color = col.copy(alpha = 0.28f),
                                            topLeft = Offset(c.x - w / 2f, c.y - h / 2f),
                                            size = Size(w, h),
                                            cornerRadius = CornerRadius(24f, 24f)
                                        )
                                        drawRoundRect(
                                            color = if (isSel) Color.White else col,
                                            topLeft = Offset(c.x - w / 2f, c.y - h / 2f),
                                            size = Size(w, h),
                                            cornerRadius = CornerRadius(24f, 24f),
                                            style = Stroke(width = if (isSel) 5f else 2.5f)
                                        )
                                        drawContext.canvas.nativeCanvas.drawText(
                                            node.label.take(18),
                                            c.x,
                                            c.y + 8f,
                                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                color = android.graphics.Color.WHITE
                                                textSize = 28f
                                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                                textAlign = Paint.Align.CENTER
                                            }
                                        )
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onExportDiagram(currentProject, renderDiagramBitmap(currentProject), "PNG") }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("PNG")
                                }
                                OutlinedButton(onClick = { onExportDiagram(currentProject, renderDiagramBitmap(currentProject), "PDF") }, modifier = Modifier.weight(1f)) {
                                    Text("PDF")
                                }
                                OutlinedButton(onClick = { onExportDiagram(currentProject, null, "JSON") }, modifier = Modifier.weight(1f)) {
                                    Text("JSON")
                                }
                            }
                        }
                    }
                }

                // Node Tap AI Assistant
                if (selectedNode != null) {
                    item {
                        GlassCard(accentBorder = CyberCyanTertiary, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Seçili Kutu: ${selectedNode.label}", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                        if (selectedNode.note.isNotBlank()) {
                                            Text(selectedNode.note, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                        }
                                    }
                                    IconButton(onClick = {
                                        currentProject = currentProject.copy(
                                            nodes = currentProject.nodes.filterNot { it.id == selectedNode.id },
                                            edges = currentProject.edges.filterNot { it.fromId == selectedNode.id || it.toId == selectedNode.id }
                                        )
                                        selectedNodeId = null
                                        onClearNodeExplanation()
                                    }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Kutuyu Sil", tint = DangerRed)
                                    }
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Kısa Açıkla", "Günlük Örnek Ver", "Mini Soru Sor").forEach { act ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = NeonPurplePrimary.copy(alpha = 0.25f),
                                            border = BorderStroke(1.dp, NeonPurplePrimary),
                                            modifier = Modifier.clickable {
                                                onExplainNode(selectedNode.label, currentProject.title, currentProject.subject, act)
                                            }
                                        ) {
                                            Text(act, color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                                        }
                                    }
                                }
                                if (state.isExplainingDiagramNode) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                } else if (!state.diagramNodeExplanation.isNullOrBlank()) {
                                    Text(state.diagramNodeExplanation, color = Color(0xFFE2E8F0), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                if (state.studioState.diagrams.isNotEmpty()) {
                    item {
                        Text("Kayıtlı Diyagramlar", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    items(state.studioState.diagrams, key = { it.id }) { diag ->
                        GlassCard(
                            onClick = { currentProject = diag; selectedType = diag.diagramType },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(diag.title, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("${diag.diagramType.label} • ${diag.nodes.size} kutu", color = CyberCyanTertiary, style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { onDeleteDiagram(diag.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Sil", tint = DangerRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddNodeDialog) {
        var label by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var chosenColor by remember { mutableStateOf(0xFF8B5CF6L) }
        AlertDialog(
            onDismissRequest = { showAddNodeDialog = false },
            title = { Text("Diyagram Kutusu Ekle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Kutu Başlığı") })
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Kısa Not (İsteğe bağlı)") })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Mor" to 0xFF8B5CF6L, "Mavi" to 0xFF3B82F6L, "Camgöbeği" to 0xFF22D3EEL, "Yeşil" to 0xFF22C55EL).forEach { (name, hex) ->
                            FilterChip(selected = chosenColor == hex, onClick = { chosenColor = hex }, label = { Text(name) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val node = DiagramNode(
                            id = UUID.randomUUID().toString().take(8),
                            label = label.trim(),
                            note = note.trim(),
                            x = 0.5f,
                            y = 0.5f,
                            colorHex = chosenColor
                        )
                        currentProject = currentProject.copy(nodes = currentProject.nodes + node)
                        showAddNodeDialog = false
                    },
                    enabled = label.isNotBlank()
                ) { Text("Ekle") }
            },
            dismissButton = { TextButton(onClick = { showAddNodeDialog = false }) { Text("İptal") } }
        )
    }
}

private fun renderDiagramBitmap(project: DiagramProject): Bitmap {
    val w = 1200
    val h = 800
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    c.drawColor(android.graphics.Color.rgb(11, 15, 25))
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    c.drawText(project.title, 44f, 58f, titlePaint)
    val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(34, 211, 238)
        strokeWidth = 5f
    }
    val map = project.nodes.associateBy { it.id }
    project.edges.forEach { e ->
        val a = map[e.fromId] ?: return@forEach
        val b = map[e.toId] ?: return@forEach
        c.drawLine(a.x * w, a.y * h, b.x * w, b.y * h, edgePaint)
    }
    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 26f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    project.nodes.forEach { n ->
        val cx = n.x * w
        val cy = n.y * h
        boxPaint.color = android.graphics.Color.rgb(26, 36, 68)
        c.drawRoundRect(RectF(cx - 110f, cy - 42f, cx + 110f, cy + 42f), 20f, 20f, boxPaint)
        c.drawText(n.label.take(18), cx, cy + 9f, textPaint)
    }
    return bmp
}

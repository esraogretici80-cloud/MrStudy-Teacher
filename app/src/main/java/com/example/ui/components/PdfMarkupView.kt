package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen
import com.example.util.PdfHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sqrt

/**
 * Normalized 0f..1f coordinate on the PDF page so annotations scale
 * smoothly across any device resolution and export at full quality.
 */
data class PdfMarkupPoint(val xRatio: Float, val yRatio: Float)

data class PdfMarkupStroke(
    val points: List<PdfMarkupPoint>,
    val color: Color,
    val strokeWidthDp: Float,
    val isHighlighter: Boolean
)

enum class PdfMarkupTool(val label: String) {
    PEN("İnce Kalem"),
    HIGHLIGHTER("Fosforlu Vurgulayıcı"),
    ERASER("Silgi")
}

val DefaultMarkupColors = listOf(
    Color(0xFFFFEA00) to "Fosforlu Sarı",
    Color(0xFF00E676) to "Fosforlu Yeşil",
    Color(0xFF00E5FF) to "Neon Mavi",
    Color(0xFFFF1744) to "Mercan Kırmızı",
    Color(0xFFD500F9) to "Neon Mor",
    Color(0xFF1E293B) to "Mürekkep Koyu"
)

/**
 * Interactive Markup Overlay with full Pen, Highlighter, Eraser, Undo, Clear, and Export.
 */
@Composable
fun PdfMarkupOverlay(
    modifier: Modifier = Modifier,
    strokes: List<PdfMarkupStroke>,
    isMarkupEnabled: Boolean,
    currentTool: PdfMarkupTool,
    selectedColor: Color,
    onStrokesChanged: (List<PdfMarkupStroke>) -> Unit
) {
    var inProgressPoints by remember { mutableStateOf<List<PdfMarkupPoint>>(emptyList()) }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isMarkupEnabled) {
                        Modifier.pointerInput(isMarkupEnabled, currentTool, selectedColor, strokes) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val startPoint = PdfMarkupPoint(
                                        xRatio = (startOffset.x / widthPx).coerceIn(0f, 1f),
                                        yRatio = (startOffset.y / heightPx).coerceIn(0f, 1f)
                                    )
                                    if (currentTool == PdfMarkupTool.ERASER) {
                                        val filtered = eraseStrokesNear(strokes, startPoint, 0.05f)
                                        if (filtered.size != strokes.size) {
                                            onStrokesChanged(filtered)
                                        }
                                    } else {
                                        inProgressPoints = listOf(startPoint)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val currentPoint = PdfMarkupPoint(
                                        xRatio = (change.position.x / widthPx).coerceIn(0f, 1f),
                                        yRatio = (change.position.y / heightPx).coerceIn(0f, 1f)
                                    )
                                    if (currentTool == PdfMarkupTool.ERASER) {
                                        val filtered = eraseStrokesNear(strokes, currentPoint, 0.05f)
                                        if (filtered.size != strokes.size) {
                                            onStrokesChanged(filtered)
                                        }
                                    } else {
                                        inProgressPoints = inProgressPoints + currentPoint
                                    }
                                },
                                onDragEnd = {
                                    if (currentTool != PdfMarkupTool.ERASER && inProgressPoints.size >= 2) {
                                        val newStroke = PdfMarkupStroke(
                                            points = inProgressPoints,
                                            color = selectedColor,
                                            strokeWidthDp = if (currentTool == PdfMarkupTool.HIGHLIGHTER) 24f else 3.5f,
                                            isHighlighter = currentTool == PdfMarkupTool.HIGHLIGHTER
                                        )
                                        onStrokesChanged(strokes + newStroke)
                                    }
                                    inProgressPoints = emptyList()
                                },
                                onDragCancel = {
                                    inProgressPoints = emptyList()
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            // Draw completed strokes
            for (stroke in strokes) {
                drawMarkupStroke(stroke, size.width, size.height)
            }

            // Draw current active stroke
            if (inProgressPoints.size >= 2) {
                val previewStroke = PdfMarkupStroke(
                    points = inProgressPoints,
                    color = selectedColor,
                    strokeWidthDp = if (currentTool == PdfMarkupTool.HIGHLIGHTER) 24f else 3.5f,
                    isHighlighter = currentTool == PdfMarkupTool.HIGHLIGHTER
                )
                drawMarkupStroke(previewStroke, size.width, size.height)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMarkupStroke(
    stroke: PdfMarkupStroke,
    widthPx: Float,
    heightPx: Float
) {
    if (stroke.points.size < 2) return
    val strokeColor = if (stroke.isHighlighter) {
        stroke.color.copy(alpha = 0.38f)
    } else {
        stroke.color
    }
    val strokePx = (stroke.strokeWidthDp * density).coerceAtLeast(2f)

    for (i in 0 until stroke.points.size - 1) {
        val p1 = stroke.points[i]
        val p2 = stroke.points[i + 1]
        drawLine(
            color = strokeColor,
            start = Offset(p1.xRatio * widthPx, p1.yRatio * heightPx),
            end = Offset(p2.xRatio * widthPx, p2.yRatio * heightPx),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
            blendMode = if (stroke.isHighlighter) BlendMode.SrcOver else BlendMode.SrcOver
        )
    }
}

private fun eraseStrokesNear(
    strokes: List<PdfMarkupStroke>,
    touchPoint: PdfMarkupPoint,
    radiusRatio: Float
): List<PdfMarkupStroke> {
    return strokes.filterNot { stroke ->
        stroke.points.any { pt ->
            val dx = pt.xRatio - touchPoint.xRatio
            val dy = pt.yRatio - touchPoint.yRatio
            sqrt((dx * dx + dy * dy).toDouble()) < radiusRatio
        }
    }
}

/**
 * Modern Floating Markup Toolbar with Tool Selector, Palette, Undo, Clear and Share.
 */
@Composable
fun PdfMarkupToolbar(
    isMarkupEnabled: Boolean,
    onToggleMarkup: () -> Unit,
    strokeCount: Int,
    currentTool: PdfMarkupTool,
    onSelectTool: (PdfMarkupTool) -> Unit,
    selectedColor: Color,
    onSelectColor: (Color) -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onShareAnnotated: () -> Unit,
    isSharing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Primary Toggle Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isMarkupEnabled) NeonPurplePrimary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(
                    1.dp,
                    if (isMarkupEnabled) NeonPurplePrimary else Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleMarkup() }
                    .testTag("toggle_markup_mode_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isMarkupEnabled) Icons.Default.Brush else Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (isMarkupEnabled) CyberCyanTertiary else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isMarkupEnabled) "İşaretleme Modu Açık" else "Sayfayı İşaretle / Vurgula",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (strokeCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricBlueSecondary,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = strokeCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            if (strokeCount > 0) {
                AssistChip(
                    onClick = onShareAnnotated,
                    enabled = !isSharing,
                    leadingIcon = {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp), tint = SuccessGreen)
                    },
                    label = { Text("İşaretli Sayfayı Paylaş", style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = SuccessGreen.copy(alpha = 0.18f)),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("share_annotated_page_button")
                )
            }
        }

        // Expanded Markup Controls
        AnimatedVisibility(
            visible = isMarkupEnabled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF10172A).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, CyberCyanTertiary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tool Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PdfMarkupTool.values().forEach { tool ->
                            val isSelected = currentTool == tool
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectTool(tool) },
                                label = { Text(tool.label, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    val icon = when (tool) {
                                        PdfMarkupTool.PEN -> Icons.Default.Edit
                                        PdfMarkupTool.HIGHLIGHTER -> Icons.Default.Highlight
                                        PdfMarkupTool.ERASER -> Icons.Default.Delete
                                    }
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlueSecondary.copy(alpha = 0.30f),
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    selectedBorderColor = CyberCyanTertiary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Undo
                        IconButton(
                            onClick = onUndo,
                            enabled = canUndo,
                            modifier = Modifier.size(36.dp).testTag("undo_markup_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Geri Al",
                                tint = if (canUndo) Color.White else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Clear
                        IconButton(
                            onClick = onClear,
                            enabled = strokeCount > 0,
                            modifier = Modifier.size(36.dp).testTag("clear_markup_button")
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Tümünü Temizle",
                                tint = if (strokeCount > 0) DangerRed else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Color Palette Row (Hidden for Eraser)
                    if (currentTool != PdfMarkupTool.ERASER) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (currentTool == PdfMarkupTool.HIGHLIGHTER) "Fosfor Rengi:" else "Mürekkep:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )

                            DefaultMarkupColors.forEach { (color, label) ->
                                val isSelected = selectedColor == color
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable { onSelectColor(color) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creates an annotated PNG bitmap combining original PDF page + drawn strokes
 * and triggers system share intent or file saving.
 */
suspend fun exportAnnotatedPdfBitmap(
    context: Context,
    baseBitmap: Bitmap,
    strokes: List<PdfMarkupStroke>,
    pageNumber: Int,
    pdfTitle: String
): File = withContext(Dispatchers.IO) {
    val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(mutableBitmap)
    val widthF = mutableBitmap.width.toFloat()
    val heightF = mutableBitmap.height.toFloat()

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }

    val path = Path()

    for (stroke in strokes) {
        if (stroke.points.size < 2) continue
        paint.strokeWidth = (stroke.strokeWidthDp * (widthF / 400f)).coerceIn(4f, 60f)
        if (stroke.isHighlighter) {
            paint.color = stroke.color.toArgb()
            paint.alpha = 110 // ~43% opacity for highlighter
        } else {
            paint.color = stroke.color.toArgb()
            paint.alpha = 255
        }

        path.reset()
        val p0 = stroke.points[0]
        path.moveTo(p0.xRatio * widthF, p0.yRatio * heightF)
        for (i in 1 until stroke.points.size) {
            val pt = stroke.points[i]
            path.lineTo(pt.xRatio * widthF, pt.yRatio * heightF)
        }
        canvas.drawPath(path, paint)
    }

    val safeTitle = pdfTitle.replace(Regex("[^a-zA-Z0-9çğıöşüÇĞİÖŞÜ_-]"), "_").take(20)
    PdfHelper.exportBitmapAsPng(context, mutableBitmap, "Isaretli_${safeTitle}_s$pageNumber")
}

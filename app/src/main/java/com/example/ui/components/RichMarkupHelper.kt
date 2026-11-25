package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

/**
 * Parses markdown markup (headings, bold, italics, code blocks, formulas, highlights)
 * into rich AnnotatedString with modern styling.
 */
fun parseRichMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()
        lines.forEachIndexed { lineIdx, line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("### ") -> {
                    val content = trimmed.removePrefix("### ")
                    pushStyle(SpanStyle(color = CyberCyanTertiary, fontWeight = FontWeight.Bold, fontSize = 16.sp))
                    append(content)
                    pop()
                }
                trimmed.startsWith("## ") -> {
                    val content = trimmed.removePrefix("## ")
                    pushStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp))
                    append(content)
                    pop()
                }
                trimmed.startsWith("# ") -> {
                    val content = trimmed.removePrefix("# ")
                    pushStyle(SpanStyle(color = ElectricBlueSecondary, fontWeight = FontWeight.Black, fontSize = 19.sp))
                    append(content)
                    pop()
                }
                trimmed.startsWith("📐") || trimmed.contains("Formül / Kural", ignoreCase = true) -> {
                    pushStyle(SpanStyle(color = CyberCyanTertiary, fontWeight = FontWeight.Bold))
                    formatInlineMarkup(line)
                    pop()
                }
                trimmed.startsWith("💡") || trimmed.contains("Püf Noktası", ignoreCase = true) -> {
                    pushStyle(SpanStyle(color = WarningAmber, fontWeight = FontWeight.Bold))
                    formatInlineMarkup(line)
                    pop()
                }
                trimmed.startsWith("✅") || trimmed.contains("Doğru Cevap", ignoreCase = true) -> {
                    pushStyle(SpanStyle(color = SuccessGreen, fontWeight = FontWeight.ExtraBold))
                    formatInlineMarkup(line)
                    pop()
                }
                else -> {
                    formatInlineMarkup(line)
                }
            }
            if (lineIdx < lines.lastIndex) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.formatInlineMarkup(line: String) {
    // Regex for bold **...**, inline code `...`, italics *...*
    var cursor = 0
    val pattern = Regex("(\\*\\*([^*]+)\\*\\*)|(`([^`]+)`)|(\\*([^*]+)\\*)")
    pattern.findAll(line).forEach { match ->
        if (match.range.first > cursor) {
            append(line.substring(cursor, match.range.first))
        }
        val full = match.value
        when {
            full.startsWith("**") && full.endsWith("**") -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White))
                append(full.removeSurrounding("**"))
                pop()
            }
            full.startsWith("`") && full.endsWith("`") -> {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyanTertiary,
                        background = Color(0x3300E5FF)
                    )
                )
                append(" " + full.removeSurrounding("`") + " ")
                pop()
            }
            full.startsWith("*") && full.endsWith("*") -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFFE2E8F0)))
                append(full.removeSurrounding("*"))
                pop()
            }
            else -> {
                append(full)
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < line.length) {
        append(line.substring(cursor))
    }
}

@Composable
fun RichMarkdownText(
    text: String = "",
    markdown: String = text,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    val source = if (markdown.isNotBlank()) markdown else text
    val annotated = parseRichMarkdown(source)
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        color = color,
        modifier = modifier
    )
}

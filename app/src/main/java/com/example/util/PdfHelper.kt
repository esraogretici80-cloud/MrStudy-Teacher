package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.StudyNote
import com.example.data.model.WorksheetData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ImportedPdfInfo(
    val localPath: String,
    val fileName: String,
    val pageCount: Int
)

object PdfHelper {

    fun tryTakePersistablePermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some document providers do not support persistable grants; local copy covers access.
        } catch (_: UnsupportedOperationException) {
        }
    }

    fun isLocalPdfAvailable(localPath: String): Boolean {
        if (localPath.isBlank()) return false
        val file = File(localPath)
        return file.exists() && file.isFile && file.length() > 0L
    }

    fun getLocalPdfSizeBytes(localPath: String): Long {
        if (localPath.isBlank()) return 0L
        val file = File(localPath)
        return if (file.exists() && file.isFile) file.length() else 0L
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = bytes / 1024.0
        return if (kb < 1024.0) {
            String.format(java.util.Locale.US, "%.0f KB", kb.coerceAtLeast(1.0))
        } else {
            String.format(java.util.Locale.US, "%.1f MB", kb / 1024.0)
        }
    }

    fun deleteLocalPdfIfOwned(context: Context, localPath: String) {
        if (localPath.isBlank()) return
        try {
            val pdfDir = File(context.filesDir, "pdfs").canonicalFile
            val target = File(localPath).canonicalFile
            if (target.path.startsWith(pdfDir.path) && target.exists()) {
                target.delete()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun importPdfToAppStorage(context: Context, sourceUri: Uri, suggestedName: String): ImportedPdfInfo =
        withContext(Dispatchers.IO) {
            tryTakePersistablePermission(context, sourceUri)
            val pdfDir = File(context.filesDir, "pdfs").apply { mkdirs() }
            val safeFileName = "${UUID.randomUUID()}.pdf"
            val destFile = File(pdfDir, safeFileName)

            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalStateException("PDF dosyası açılamadı. Lütfen dosyanın erişilebilir olduğundan emin olun.")

            inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val copied = input.copyTo(output)
                    if (copied <= 0L) {
                        throw IllegalStateException("Seçilen PDF dosyası boş görünüyor.")
                    }
                }
            }

            val pageCount = try {
                ParcelFileDescriptor.open(destFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val count = renderer.pageCount
                        if (count <= 0) throw IllegalStateException("PDF içinde geçerli sayfa bulunamadı.")
                        count
                    }
                }
            } catch (e: Exception) {
                destFile.delete()
                throw IllegalStateException(
                    "PDF dosyası bozuk, şifreli veya desteklenmeyen biçimde: ${e.localizedMessage ?: "Bilinmeyen hata"}"
                )
            }

            ImportedPdfInfo(
                localPath = destFile.absolutePath,
                fileName = suggestedName.removeSuffix(".pdf").ifBlank { "PDF Kaynak" },
                pageCount = pageCount
            )
        }

    suspend fun renderPdfPage(
        localPath: String,
        pageIndexZeroBased: Int,
        maxWidthPx: Int = 1400
    ): Bitmap = withContext(Dispatchers.IO) {
        val file = File(localPath)
        if (!file.exists()) {
            throw IllegalStateException("PDF dosyası cihazda bulunamadı. Lütfen kaynağı yeniden yükleyin.")
        }
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val safeIndex = pageIndexZeroBased.coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0))
                renderer.openPage(safeIndex).use { page ->
                    val scale = maxWidthPx.toFloat() / page.width.coerceAtLeast(1)
                    val targetWidth = (page.width * scale).toInt().coerceAtLeast(1)
                    val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    }

    suspend fun renderPdfSamplePages(
        localPath: String,
        focusPageOneBased: Int,
        maxPages: Int = 4,
        targetWidthPx: Int = 860
    ): List<Pair<Int, Bitmap>> = withContext(Dispatchers.IO) {
        val file = File(localPath)
        if (!file.exists()) return@withContext emptyList()
        val result = mutableListOf<Pair<Int, Bitmap>>()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val total = renderer.pageCount
                if (total <= 0) return@use
                val focusIdx = (focusPageOneBased - 1).coerceIn(0, total - 1)
                val indices = linkedSetOf(focusIdx)
                if (focusIdx + 1 < total) indices.add(focusIdx + 1)
                if (focusIdx - 1 >= 0) indices.add(focusIdx - 1)
                var i = 0
                while (indices.size < maxPages && i < total) {
                    indices.add(i)
                    i++
                }
                for (idx in indices.sorted().take(maxPages)) {
                    renderer.openPage(idx).use { page ->
                        val scale = targetWidthPx.toFloat() / page.width.coerceAtLeast(1)
                        val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(targetWidthPx, targetHeight, Bitmap.Config.ARGB_8888)
                        Canvas(bitmap).drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        result.add((idx + 1) to bitmap)
                    }
                }
            }
        }
        result
    }

    suspend fun decodeScaledBitmap(context: Context, uri: Uri, maxDim: Int = 1280): Bitmap =
        withContext(Dispatchers.IO) {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Görsel okunamadı.")
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, opts)
            var sampleSize = 1
            while (opts.outWidth / sampleSize > maxDim || opts.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                ?: throw IllegalStateException("Görsel çözümlenemedi.")
        }

    suspend fun exportNoteAsPdf(context: Context, note: StudyNote): File =
        withContext(Dispatchers.IO) {
            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val safeName = note.title.replace(Regex("[^a-zA-Z0-9çğıöşüÇĞİÖŞÜ_-]"), "_").take(40)
            val outFile = File(exportDir, "MrStudy_${safeName}_${System.currentTimeMillis()}.pdf")

            val doc = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 44f
            val usableWidth = pageWidth - margin * 2

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(18, 49, 77)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(217, 119, 6)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 35, 42)
                textSize = 11.5f
            }

            var pageNum = 1
            var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            var canvas = page.canvas
            var y = margin + 16f

            fun newPageIfNeeded(lineHeight: Float = 18f) {
                if (y + lineHeight > pageHeight - margin) {
                    doc.finishPage(page)
                    pageNum++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    canvas = page.canvas
                    y = margin + 16f
                }
            }

            fun drawWrappedText(text: String, paint: Paint, lineHeight: Float = 17f) {
                text.split("\n").forEach { paragraph ->
                    if (paragraph.isBlank()) {
                        y += lineHeight * 0.6f
                        return@forEach
                    }
                    val words = paragraph.split(" ")
                    var line = ""
                    for (word in words) {
                        val candidate = if (line.isEmpty()) word else "$line $word"
                        if (paint.measureText(candidate) > usableWidth) {
                            newPageIfNeeded(lineHeight)
                            canvas.drawText(line, margin, y, paint)
                            y += lineHeight
                            line = word
                        } else {
                            line = candidate
                        }
                    }
                    if (line.isNotEmpty()) {
                        newPageIfNeeded(lineHeight)
                        canvas.drawText(line, margin, y, paint)
                        y += lineHeight
                    }
                }
            }

            drawWrappedText(note.title, titlePaint, 24f)
            drawWrappedText("Ders: ${note.subject}" + if (note.sourcePdfTitle.isNotBlank()) "  •  Kaynak: ${note.sourcePdfTitle}" else "", subPaint, 20f)
            y += 10f
            drawWrappedText(note.content, bodyPaint)

            if (note.keyConcepts.isNotEmpty()) {
                y += 12f
                drawWrappedText("Önemli Kavramlar:", subPaint, 20f)
                note.keyConcepts.forEach { drawWrappedText("• $it", bodyPaint) }
            }
            if (note.formulas.isNotEmpty()) {
                y += 12f
                drawWrappedText("Formüller:", subPaint, 20f)
                note.formulas.forEach { drawWrappedText("• $it", bodyPaint) }
            }
            if (note.definitions.isNotEmpty()) {
                y += 12f
                drawWrappedText("Tanımlar:", subPaint, 20f)
                note.definitions.forEach { drawWrappedText("• $it", bodyPaint) }
            }

            doc.finishPage(page)
            FileOutputStream(outFile).use { doc.writeTo(it) }
            doc.close()
            outFile
        }

    suspend fun exportWorksheetPdf(context: Context, worksheet: WorksheetData): File =
        withContext(Dispatchers.IO) {
            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val safeName = worksheet.title.replace(Regex("[^a-zA-Z0-9çğıöşüÇĞİÖŞÜ_-]"), "_").take(36)
            val fileName = "MrStudy4_CalismaKagidi_${safeName}_${System.currentTimeMillis()}.pdf"
            val outFile = File(exportDir, fileName)

            val doc = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40f
            val usableWidth = pageWidth - margin * 2

            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val brandSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(196, 181, 253)
                textSize = 10.5f
            }
            val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
            }
            val accentStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(139, 92, 246)
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(88, 28, 135)
                textSize = 12.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 14.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 10.5f
            }
            val formulaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 58, 138)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 1f
            }
            val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(245, 243, 255)
            }

            var pageNum = 1
            var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            var canvas = page.canvas
            var y = margin

            fun drawHeaderBanner() {
                canvas.drawRoundRect(
                    RectF(margin, y, pageWidth - margin, y + 60f),
                    12f,
                    12f,
                    headerBgPaint
                )
                canvas.drawRoundRect(
                    RectF(margin, y, margin + 8f, y + 60f),
                    4f,
                    4f,
                    accentStripePaint
                )
                canvas.drawText("MrStudy 4.0  •  Çalışma Kağıdı", margin + 20f, y + 26f, brandPaint)
                canvas.drawText(
                    "Ders: ${worksheet.subject}   •   Seviye: ${worksheet.gradeLevel}",
                    margin + 20f,
                    y + 45f,
                    brandSubPaint
                )
                y += 78f
            }

            fun newPageIfNeeded(requiredHeight: Float = 20f) {
                if (y + requiredHeight > pageHeight - margin) {
                    doc.finishPage(page)
                    pageNum++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    canvas = page.canvas
                    y = margin + 12f
                }
            }

            fun drawWrappedText(text: String, paint: Paint, x: Float = margin, maxW: Float = usableWidth, lineHeight: Float = 15.5f) {
                text.split("\n").forEach { paragraph ->
                    if (paragraph.isBlank()) {
                        y += lineHeight * 0.45f
                        return@forEach
                    }
                    val words = paragraph.split(" ")
                    var line = ""
                    for (word in words) {
                        val candidate = if (line.isEmpty()) word else "$line $word"
                        if (paint.measureText(candidate) > maxW) {
                            newPageIfNeeded(lineHeight)
                            canvas.drawText(line, x, y, paint)
                            y += lineHeight
                            line = word
                        } else {
                            line = candidate
                        }
                    }
                    if (line.isNotEmpty()) {
                        newPageIfNeeded(lineHeight)
                        canvas.drawText(line, x, y, paint)
                        y += lineHeight
                    }
                }
            }

            drawHeaderBanner()
            drawWrappedText(worksheet.title, titlePaint, lineHeight = 20f)
            y += 6f

            if (worksheet.summary.isNotBlank()) {
                drawWrappedText("1. Ders Özeti", sectionPaint, lineHeight = 18f)
                drawWrappedText(worksheet.summary, bodyPaint)
                y += 10f
            }

            if (worksheet.formulas.isNotEmpty()) {
                newPageIfNeeded(44f)
                drawWrappedText("2. Akıllı Tahta Formülleri ve Kurallar", sectionPaint, lineHeight = 18f)
                val boxStart = y - 8f
                val boxHeight = (worksheet.formulas.size * 18f + 16f).coerceAtMost(200f)
                canvas.drawRoundRect(
                    RectF(margin, boxStart, pageWidth - margin, boxStart + boxHeight),
                    8f,
                    8f,
                    boxFillPaint
                )
                y += 8f
                worksheet.formulas.forEach { formula ->
                    drawWrappedText("• $formula", formulaPaint, x = margin + 10f, maxW = usableWidth - 20f, lineHeight = 16f)
                }
                y += 12f
            }

            if (worksheet.questions.isNotEmpty()) {
                newPageIfNeeded(32f)
                drawWrappedText("3. Pratik Sorular (5 Soru)", sectionPaint, lineHeight = 18f)
                y += 4f
                worksheet.questions.forEachIndexed { idx, q ->
                    newPageIfNeeded(48f)
                    val qNum = if (q.number > 0) q.number else idx + 1
                    drawWrappedText("$qNum) ${q.question}", titlePaint.apply { textSize = 11f }, lineHeight = 16f)
                    q.options.forEachIndexed { oIdx, opt ->
                        val prefix = ('A' + oIdx)
                        val cleanOpt = opt.replace(Regex("^[A-Da-d][).:]\\s*"), "")
                        drawWrappedText("    $prefix) $cleanOpt", bodyPaint, lineHeight = 14.5f)
                    }
                    y += 8f
                }

                // Bottom Answer Key Section
                newPageIfNeeded(60f)
                canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                y += 16f
                drawWrappedText("CEVAP ANAHTARI", sectionPaint, lineHeight = 18f)
                worksheet.questions.forEachIndexed { idx, q ->
                    val qNum = if (q.number > 0) q.number else idx + 1
                    drawWrappedText("$qNum: ${q.answerKey}", bodyPaint, lineHeight = 14.5f)
                }
            }

            doc.finishPage(page)
            FileOutputStream(outFile).use { doc.writeTo(it) }
            doc.close()

            saveCopyToPublicDownloads(context, outFile, fileName)
            outFile
        }

    private fun saveCopyToPublicDownloads(context: Context, sourceFile: File, displayName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/MrStudy")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }

    fun sharePdfFile(context: Context, file: File) {
        shareFile(context, file, "application/pdf", "MrStudy 4.0 PDF Olarak Paylaş / Kaydet")
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String = "MrStudy 4.0 Dosya Paylaş") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    suspend fun exportBitmapAsPng(context: Context, bitmap: Bitmap, prefix: String): File =
        withContext(Dispatchers.IO) {
            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val safe = prefix.replace(Regex("[^a-zA-Z0-9çğıöşüÇĞİÖŞÜ_-]"), "_").take(30)
            val outFile = File(exportDir, "MrStudy4_${safe}_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 96, it) }
            outFile
        }

    suspend fun exportBitmapAsPdf(context: Context, bitmap: Bitmap, title: String): File =
        withContext(Dispatchers.IO) {
            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val safe = title.replace(Regex("[^a-zA-Z0-9çğıöşüÇĞİÖŞÜ_-]"), "_").take(30)
            val fileName = "MrStudy4_${safe}_${System.currentTimeMillis()}.pdf"
            val outFile = File(exportDir, fileName)
            val doc = PdfDocument()
            val pageW = 595
            val pageH = 842
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
            val c = page.canvas
            c.drawColor(Color.rgb(11, 15, 25))
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            c.drawText("MrStudy 4.0  •  $title", 32f, 40f, titlePaint)
            val availW = pageW - 64f
            val availH = pageH - 96f
            val scale = minOf(availW / bitmap.width.coerceAtLeast(1), availH / bitmap.height.coerceAtLeast(1))
            val drawW = bitmap.width * scale
            val drawH = bitmap.height * scale
            val left = (pageW - drawW) / 2f
            val top = 64f
            c.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), null)
            doc.finishPage(page)
            FileOutputStream(outFile).use { doc.writeTo(it) }
            doc.close()
            saveCopyToPublicDownloads(context, outFile, fileName)
            outFile
        }

    suspend fun exportPresentationPdf(
        context: Context,
        title: String,
        subject: String,
        slides: List<Pair<String, List<String>>>
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safe = title.replace(Regex("[^a-zA-Z0-9çğıöşüÇĞİÖŞÜ_-]"), "_").take(30)
        val fileName = "MrStudy4_Sunum_${safe}_${System.currentTimeMillis()}.pdf"
        val outFile = File(exportDir, fileName)
        val doc = PdfDocument()
        val pageW = 842
        val pageH = 595
        val bgPaint = Paint().apply { color = Color.rgb(12, 18, 34) }
        val accentPaint = Paint().apply { color = Color.rgb(139, 92, 246) }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(34, 211, 238)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            textSize = 15f
        }
        slides.forEachIndexed { idx, (slideTitle, bullets) ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, idx + 1).create())
            val c = page.canvas
            c.drawRect(0f, 0f, pageW.toFloat(), pageH.toFloat(), bgPaint)
            c.drawRect(0f, 0f, 14f, pageH.toFloat(), accentPaint)
            c.drawText("MrStudy 4.0 • $subject • Slayt ${idx + 1}/${slides.size}", 44f, 48f, subPaint)
            c.drawText(slideTitle.take(64), 44f, 94f, titlePaint)
            var y = 148f
            bullets.forEach { bullet ->
                val words = bullet.split(" ")
                var line = "• "
                for (w in words) {
                    val cand = if (line == "• ") "• $w" else "$line $w"
                    if (bulletPaint.measureText(cand) > pageW - 96f) {
                        c.drawText(line, 48f, y, bulletPaint)
                        y += 26f
                        line = "  $w"
                    } else {
                        line = cand
                    }
                }
                if (line.isNotBlank()) {
                    c.drawText(line, 48f, y, bulletPaint)
                    y += 34f
                }
            }
            doc.finishPage(page)
        }
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        saveCopyToPublicDownloads(context, outFile, fileName)
        outFile
    }

    suspend fun exportTextFile(context: Context, fileName: String, content: String): File =
        withContext(Dispatchers.IO) {
            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val outFile = File(exportDir, fileName)
            outFile.writeText(content)
            outFile
        }
}

package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.example.data.model.StudyNote
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

    fun sharePdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Notu PDF Olarak Paylaş / Kaydet"))
    }
}

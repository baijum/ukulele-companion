package com.baijum.ukufretboard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File

/**
 * Platform-specific sharing and clipboard utilities.
 *
 * Extracted from [com.baijum.ukufretboard.domain.ChordSheetFormatter]
 * so that the domain package remains free of Android framework imports.
 */
object ShareUtils {

    /**
     * Shares text content via Android's share sheet.
     *
     * @param context Android context.
     * @param title Title for the share chooser.
     * @param text The text content to share.
     */
    fun shareText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }

    /**
     * Copies text to the Android clipboard.
     *
     * @param context Android context.
     * @param label Label for the clipboard entry.
     * @param text The text to copy.
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
            as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * Generates a PDF from formatted text and shares it via the system share sheet.
     *
     * @param context Android context.
     * @param title Title for the PDF and share chooser.
     * @param formattedText Chords-above-lyrics formatted text.
     */
    fun sharePdf(context: Context, title: String, formattedText: String) {
        val pageWidth = 595 // A4 in points
        val pageHeight = 842
        val margin = 50f
        val lineHeight = 16f
        val fontSize = 11f

        val paint = Paint().apply {
            typeface = Typeface.MONOSPACE
            textSize = fontSize
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            isAntiAlias = true
        }

        val lines = formattedText.lines()
        val usableHeight = pageHeight - 2 * margin
        val linesPerPage = (usableHeight / lineHeight).toInt()

        val document = PdfDocument()
        var pageNum = 0
        var lineIdx = 0

        while (lineIdx < lines.size) {
            pageNum++
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            var y = margin
            if (pageNum == 1 && title.isNotEmpty()) {
                canvas.drawText(title, margin, y + 18f, titlePaint)
                y += 30f
            }

            var linesOnPage = 0
            while (lineIdx < lines.size && linesOnPage < linesPerPage) {
                canvas.drawText(lines[lineIdx], margin, y + lineHeight, paint)
                y += lineHeight
                lineIdx++
                linesOnPage++
            }

            document.finishPage(page)
        }

        val pdfFile = File(context.cacheDir, "${sanitizeFilename(title)}.pdf")
        pdfFile.outputStream().use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    }

    private fun sanitizeFilename(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(100)
}

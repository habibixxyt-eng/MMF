package com.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File

object PdfTools {
    fun count(file: File): Int = PDDocument.load(file).use { it.numberOfPages }

    fun preview(file: File, page: Int): Bitmap {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                require(page in 0 until renderer.pageCount) { "Invalid page index" }
                renderer.openPage(page).use { p ->
                    val scale = minOf(2f, 1600f / maxOf(p.width, p.height))
                    val bitmap = Bitmap.createBitmap(
                        (p.width * scale).toInt().coerceAtLeast(1),
                        (p.height * scale).toInt().coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(Color.WHITE)
                    p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bitmap
                }
            }
        }
    }

    fun pages(text: String, total: Int): List<Int> {
        val result = mutableListOf<Int>()
        text.split(',').forEach { segment ->
            val parts = segment.trim().split('-')
            require(parts.size in 1..2) { "Use pages like 1,3,5-7" }
            val first = parts[0].trim().toInt()
            val last = if (parts.size == 2) parts[1].trim().toInt() else first
            require(first in 1..total && last in first..total) { "Page outside document (1..$total)" }
            result.addAll((first..last).map { it - 1 })
        }
        require(result.isNotEmpty()) { "No valid pages specified" }
        return result
    }

    fun select(input: File, output: File, selection: String) {
        PDDocument.load(input).use { src ->
            PDDocument().use { dst ->
                pages(selection, src.numberOfPages).forEach { dst.importPage(src.getPage(it)) }
                dst.save(output)
            }
        }
    }

    fun rotate(input: File, output: File, selection: String) {
        PDDocument.load(input).use { doc ->
            pages(selection, doc.numberOfPages).forEach {
                val p = doc.getPage(it)
                p.rotation = (p.rotation + 90) % 360
            }
            doc.save(output)
        }
    }

    fun delete(input: File, output: File, selection: String) {
        PDDocument.load(input).use { doc ->
            val indices = pages(selection, doc.numberOfPages).distinct().sortedDescending()
            require(indices.size < doc.numberOfPages) { "Keep at least one page" }
            indices.forEach { doc.removePage(it) }
            doc.save(output)
        }
    }

    fun text(input: File, output: File, page: Int, text: String, x: Float, y: Float) {
        require(text.isNotBlank()) { "Text cannot be blank" }
        PDDocument.load(input).use { doc ->
            require(page in 1..doc.numberOfPages) { "Page $page out of range (1..${doc.numberOfPages})" }
            PDPageContentStream(
                doc,
                doc.getPage(page - 1),
                PDPageContentStream.AppendMode.APPEND,
                true,
                true
            ).use { stream ->
                stream.beginText()
                stream.setFont(PDType1Font.HELVETICA, 14f)
                stream.newLineAtOffset(x, y)
                stream.showText(text)
                stream.endText()
            }
            doc.save(output)
        }
    }

    fun merge(inputs: List<File>, output: File) {
        require(inputs.size >= 2) { "Choose at least two PDFs" }
        PDDocument().use { dst ->
            inputs.forEach { file ->
                PDDocument.load(file).use { src ->
                    for (i in 0 until src.numberOfPages) {
                        dst.importPage(src.getPage(i))
                    }
                }
            }
            dst.save(output)
        }
    }

    fun images(inputs: List<File>, output: File) {
        require(inputs.isNotEmpty()) { "No images to convert" }
        val doc = PdfDocument()
        try {
            inputs.forEachIndexed { index, file ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image: ${file.name}" }

                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2400) sample *= 2
                val bitmap = BitmapFactory.decodeFile(
                    file.path,
                    BitmapFactory.Options().apply { inSampleSize = sample }
                ) ?: error("Cannot decode image ${file.name}")

                try {
                    val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                    val scale = minOf(555f / bitmap.width, 802f / bitmap.height)
                    val w = bitmap.width * scale
                    val h = bitmap.height * scale
                    page.canvas.drawColor(Color.WHITE)
                    page.canvas.drawBitmap(
                        bitmap,
                        null,
                        RectF((595f - w) / 2f, (842f - h) / 2f, (595f + w) / 2f, (842f + h) / 2f),
                        Paint(Paint.FILTER_BITMAP_FLAG)
                    )
                    doc.finishPage(page)
                } finally {
                    bitmap.recycle()
                }
            }
            output.outputStream().use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }
}

package com.starry.myne.PDFExport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.starry.myne.database.note.Note
import com.starry.myne.ui.screens.reader.main.viewmodel.ReaderFont
import com.starry.myne.ui.screens.reader.main.viewmodel.ReaderFont.Cursive.toTypeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object PdfExporter {
    suspend fun exportNoteAsPdf(note: Note, filePath: String, context: Context): Boolean {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 宽度 (8.5 x 11 inches, 72 PPI)
        val pageHeight = 842 // A4 高度
        val margin = 40 // 边距

        val paint = Paint()
        paint.textSize = note.fontSize.toFloat()
        var yPosition = margin.toFloat()

        // 加载背景图资源
        val backgroundBitmap = BitmapFactory.decodeResource(context.resources, note.background)

        // 创建页面
        var currentPage = createNewPage(pdfDocument, pageWidth, pageHeight)
        var canvas = currentPage.canvas

        // 绘制背景图
        canvas.drawBitmap(
            Bitmap.createScaledBitmap(backgroundBitmap, pageWidth, pageHeight, true),
            0f, 0f, null
        )

        val fontFamily = ReaderFont.getFontById(note.font).fontFamily
        fontFamily?.let {
            paint.typeface = toTypeface(context)
        }

        // 绘制标题
        paint.textSize = (note.fontSize * 1.5).toFloat() // 标题比正文字体稍大
        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText(note.title, margin.toFloat(), yPosition, paint)
        yPosition += 40f

        // 绘制内容
        paint.textSize = note.fontSize.toFloat() // 设置正文字体大小
        paint.isFakeBoldText = false

        for (entry in note.entries) {
            val contentLines = splitTextIntoLines(entry.text, pageWidth - 2 * margin, paint)
            val thoughtLines = splitTextIntoLines(entry.thoughts, pageWidth - 2 * margin, paint)

            // 绘制 Content
            paint.color = Color.BLACK
            for (line in contentLines) {
                if (yPosition + 24 > pageHeight - margin) {
                    // 换页逻辑
                    pdfDocument.finishPage(currentPage)
                    currentPage = createNewPage(pdfDocument, pageWidth, pageHeight)
                    canvas = currentPage.canvas
                    yPosition = margin.toFloat()

                    // 重新绘制背景图
                    canvas.drawBitmap(
                        Bitmap.createScaledBitmap(backgroundBitmap, pageWidth, pageHeight, true),
                        0f, 0f, null
                    )
                }
                canvas.drawText(line, margin.toFloat(), yPosition, paint)
                yPosition += 24f
            }

            // 绘制 Thoughts
            paint.color = 0xFF9C27B0.toInt() // 紫色
            for (line in thoughtLines) {
                if (yPosition + 24 > pageHeight - margin) {
                    // 换页逻辑
                    pdfDocument.finishPage(currentPage)
                    currentPage = createNewPage(pdfDocument, pageWidth, pageHeight)
                    canvas = currentPage.canvas
                    yPosition = margin.toFloat()

                    // 重新绘制背景图
                    canvas.drawBitmap(
                        Bitmap.createScaledBitmap(backgroundBitmap, pageWidth, pageHeight, true),
                        0f, 0f, null
                    )
                }
                canvas.drawText(line, margin.toFloat(), yPosition, paint)
                yPosition += 24f
            }

            // 绘制图片
            entry.imageUrl?.let { imageUrl ->
                val bitmap = loadBitmapFromUrl(imageUrl)
                bitmap?.let {
                    if (yPosition + 220 > pageHeight - margin) {
                        pdfDocument.finishPage(currentPage)
                        currentPage = createNewPage(pdfDocument, pageWidth, pageHeight)
                        canvas = currentPage.canvas
                        yPosition = margin.toFloat()

                        canvas.drawBitmap(
                            Bitmap.createScaledBitmap(backgroundBitmap, pageWidth, pageHeight, true),
                            0f, 0f, null
                        )
                    }
                    val scaledBitmap = Bitmap.createScaledBitmap(it, 200, 200, true)
                    canvas.drawBitmap(scaledBitmap, margin.toFloat(), yPosition, paint)
                    yPosition += 220f
                }
            }
        }

        // 绘制摘要 (Summary)
        note.summary?.let { summary ->
            if (yPosition + 80 > pageHeight - margin) {
                pdfDocument.finishPage(currentPage)
                currentPage = createNewPage(pdfDocument, pageWidth, pageHeight)
                canvas = currentPage.canvas
                yPosition = margin.toFloat()
                canvas.drawBitmap(
                    Bitmap.createScaledBitmap(backgroundBitmap, pageWidth, pageHeight, true),
                    0f, 0f, null
                )
            }

            paint.textSize = (note.fontSize * 1.1).toFloat() // 摘要字体稍大
            paint.color = Color.BLUE
            paint.isFakeBoldText = true
            canvas.drawText("Summary:", margin.toFloat(), yPosition, paint)
            yPosition += 24f

            paint.textSize = note.fontSize.toFloat()
            paint.isFakeBoldText = false
            paint.color = Color.BLACK
            val summaryLines = splitTextIntoLines(summary, pageWidth - 2 * margin, paint)
            summaryLines.forEach { line ->
                if (yPosition + 24 > pageHeight - margin) {
                    pdfDocument.finishPage(currentPage)
                    currentPage = createNewPage(pdfDocument, pageWidth, pageHeight)
                    canvas = currentPage.canvas
                    yPosition = margin.toFloat()
                    canvas.drawBitmap(
                        Bitmap.createScaledBitmap(backgroundBitmap, pageWidth, pageHeight, true),
                        0f, 0f, null
                    )
                }
                canvas.drawText(line, margin.toFloat(), yPosition, paint)
                yPosition += 24f
            }
            yPosition += 40f
        }

        // 结束当前页面
        pdfDocument.finishPage(currentPage)

        // 保存 PDF 到文件
        return try {
            val file = File(filePath)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            false
        }
    }

    private fun createNewPage(pdfDocument: PdfDocument, pageWidth: Int, pageHeight: Int): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.pages.size + 1).create()
        return pdfDocument.startPage(pageInfo)
    }

    private fun splitTextIntoLines(text: String, maxWidth: Int, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) > maxWidth) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }
    private suspend fun loadBitmapFromUrl(imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                BitmapFactory.decodeStream(url.openStream())
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}



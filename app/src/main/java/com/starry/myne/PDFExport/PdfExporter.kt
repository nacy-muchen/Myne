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
    // Suspend function to export a note as a PDF to the specified file path
    suspend fun exportNoteAsPdf(note: Note, filePath: String, context: Context): Boolean {
        val pdfDocument = PdfDocument()
        // Define A4 paper size (in points, 72 PPI)
        val pageWidth = 595
        val pageHeight = 842 // A4 height
        val margin = 40

        // Initialize Paint object for text drawing
        val paint = Paint()
        paint.textSize = note.fontSize.toFloat()
        var yPosition = margin.toFloat()

        // Load background image for the PDF
        val backgroundBitmap = BitmapFactory.decodeResource(context.resources, note.background)

        // Create a new page in the PDF document
        var currentPage = createNewPage(pdfDocument, pageWidth, pageHeight)
        var canvas = currentPage.canvas

        // Draw the background image onto the page
        canvas.drawBitmap(
            Bitmap.createScaledBitmap(backgroundBitmap, pageWidth, pageHeight, true),
            0f, 0f, null
        )

        // Set the font type based on the note's font settings
        val fontFamily = ReaderFont.getFontById(note.font).fontFamily
        fontFamily?.let {
            paint.typeface = toTypeface(context)
        }

        // Draw the title of the note, making it slightly larger than the body text
        paint.textSize = (note.fontSize * 1.5).toFloat()
        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText(note.title, margin.toFloat(), yPosition, paint)
        yPosition += 40f

        // Draw the body content of the note
        paint.textSize = note.fontSize.toFloat() // 设置正文字体大小
        paint.isFakeBoldText = false

        for (entry in note.entries) {
            val contentLines = splitTextIntoLines(entry.text, pageWidth - 2 * margin, paint)
            val thoughtLines = splitTextIntoLines(entry.thoughts, pageWidth - 2 * margin, paint)

            // Loop through each line of content
            paint.color = Color.BLACK
            for (line in contentLines) {
                if (yPosition + 24 > pageHeight - margin) {
                    // If the line doesn't fit, create a new page
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

            // Draw Thoughts (lines of text)
            paint.color = 0xFF9C27B0.toInt()
            for (line in thoughtLines) {
                // Check if the next line of text will overflow the current page
                if (yPosition + 24 > pageHeight - margin) {
                    // If so, finish the current page and create a new page
                    pdfDocument.finishPage(currentPage)
                    currentPage = createNewPage(pdfDocument, pageWidth, pageHeight)
                    canvas = currentPage.canvas
                    yPosition = margin.toFloat()

                    // Redraw the background image on the new page
                    canvas.drawBitmap(
                        Bitmap.createScaledBitmap(backgroundBitmap, pageWidth, pageHeight, true),
                        0f, 0f, null
                    )
                }
                canvas.drawText(line, margin.toFloat(), yPosition, paint)
                yPosition += 24f
            }

            // Draw image (if present)
            entry.imageUrl?.let { imageUrl ->
                // Load the image from the URL
                val bitmap = loadBitmapFromUrl(imageUrl)
                bitmap?.let {
                    // Check if the image will fit within the current page
                    if (yPosition + 220 > pageHeight - margin) {
                        pdfDocument.finishPage(currentPage)
                        currentPage = createNewPage(pdfDocument, pageWidth, pageHeight)
                        canvas = currentPage.canvas
                        yPosition = margin.toFloat()

                        // Redraw the background image on the new page
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

        // Draw Summary
        note.summary?.let { summary ->
            // Check if the summary will fit on the current page
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

            paint.textSize = (note.fontSize * 1.1).toFloat()
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

        // end current page
        pdfDocument.finishPage(currentPage)

        // save PDF to files
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

    // Function to create a new page in the PDF document
    private fun createNewPage(pdfDocument: PdfDocument, pageWidth: Int, pageHeight: Int): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.pages.size + 1).create()
        return pdfDocument.startPage(pageInfo)
    }

    // split text into lines
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

    // Function to load an image from a URL
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



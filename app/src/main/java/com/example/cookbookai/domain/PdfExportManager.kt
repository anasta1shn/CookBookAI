package com.example.cookbookai.domain

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.cookbookai.data.model.AnalysisResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExportManager {

    fun exportHistory(
        context: Context,
        history: List<AnalysisResult>
    ): File {

        val pdfDocument =
            PdfDocument()

        var pageNumber = 1

        var page =
            createPage(pdfDocument, pageNumber)

        var canvas =
            page.canvas

        val titlePaint =
            Paint().apply {
                textSize = 34f
                typeface = Typeface.DEFAULT_BOLD
            }

        val headerPaint =
            Paint().apply {
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
            }

        val textPaint =
            Paint().apply {
                textSize = 24f
            }

        val smallPaint =
            Paint().apply {
                textSize = 22f
            }

        var y = 80f

        canvas.drawText(
            "CookBook AI — отчёт анализа питания",
            60f,
            y,
            titlePaint
        )

        y += 60f

        canvas.drawText(
            "Всего анализов: ${history.size}",
            60f,
            y,
            textPaint
        )

        y += 45f

        val totalCalories =
            history.sumOf { it.calories }

        canvas.drawText(
            "Общая калорийность: $totalCalories ккал",
            60f,
            y,
            textPaint
        )

        y += 60f

        history.forEachIndexed { index, item ->

            if (y > 1650f) {

                pdfDocument.finishPage(page)

                pageNumber++

                page =
                    createPage(pdfDocument, pageNumber)

                canvas =
                    page.canvas

                y = 80f
            }

            val formatter =
                SimpleDateFormat(
                    "dd.MM.yyyy HH:mm",
                    Locale.getDefault()
                )

            canvas.drawText(
                "${index + 1}. ${item.name}",
                60f,
                y,
                headerPaint
            )

            y += 45f

            val lines =
                mutableListOf<String>()

            lines.add("Дата: ${formatter.format(Date(item.date))}")
            lines.add("Калории: ${item.calories} ккал")
            lines.add("Белки: ${item.proteins}")
            lines.add("Жиры: ${item.fats}")
            lines.add("Углеводы: ${item.carbs}")
            lines.add("Вес: ${item.weight} г")
            lines.add("AI confidence: ${(item.confidence * 100).toInt()}%")

            if (item.aiSummary.isNotBlank()) {
                lines.add("AI Summary: ${item.aiSummary}")
            }

            lines.forEach { line ->

                val wrappedLines =
                    wrapText(
                        line,
                        maxChars = 55
                    )

                wrappedLines.forEach { wrappedLine ->

                    canvas.drawText(
                        wrappedLine,
                        80f,
                        y,
                        textPaint
                    )

                    y += 34f
                }
            }

            if (item.topPredictions.isNotEmpty()) {

                y += 10f

                canvas.drawText(
                    "Top-3 predictions:",
                    80f,
                    y,
                    textPaint
                )

                y += 34f

                item.topPredictions.forEach { prediction ->

                    canvas.drawText(
                        "• $prediction",
                        100f,
                        y,
                        smallPaint
                    )

                    y += 32f
                }
            }

            y += 30f

            canvas.drawLine(
                60f,
                y,
                1020f,
                y,
                textPaint
            )

            y += 45f
        }

        pdfDocument.finishPage(page)

        val folder =
            context.getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS
            )

        if (folder?.exists() == false) {
            folder.mkdirs()
        }

        val file =
            File(
                folder,
                "cookbook_ai_report.pdf"
            )

        pdfDocument.writeTo(
            FileOutputStream(file)
        )

        pdfDocument.close()

        return file
    }

    private fun createPage(
        pdfDocument: PdfDocument,
        pageNumber: Int
    ): PdfDocument.Page {

        val pageInfo =
            PdfDocument.PageInfo.Builder(
                1080,
                1920,
                pageNumber
            ).create()

        return pdfDocument.startPage(pageInfo)
    }

    private fun wrapText(
        text: String,
        maxChars: Int
    ): List<String> {

        if (text.length <= maxChars) {
            return listOf(text)
        }

        val words =
            text.split(" ")

        val lines =
            mutableListOf<String>()

        var currentLine = ""

        words.forEach { word ->

            if ((currentLine + word).length > maxChars) {

                lines.add(currentLine.trim())

                currentLine = "$word "

            } else {

                currentLine += "$word "
            }
        }

        if (currentLine.isNotBlank()) {
            lines.add(currentLine.trim())
        }

        return lines
    }
}
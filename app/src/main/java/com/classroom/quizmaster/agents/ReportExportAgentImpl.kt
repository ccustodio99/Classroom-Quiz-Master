package com.classroom.quizmaster.agents

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.classroom.quizmaster.domain.model.ClassReport
import com.classroom.quizmaster.domain.model.StudentReport
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.Locale
import kotlin.text.Charsets

class ReportExportAgentImpl(private val context: Context) : ReportExportAgent {
    override suspend fun exportClassPdf(report: ClassReport): FileRef {
        val file = File(context.filesDir, "class-report-${report.moduleId}.pdf")
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint().apply { textSize = 14f }
            var y = 40f
            canvas.drawText("Class Report: ${report.topic}", 20f, y, paint)
            y += 24f
            canvas.drawText(
                String.format(Locale.US, "Pre Avg: %.1f%%  Post Avg: %.1f%%", report.preAverage, report.postAverage),
                20f,
                y,
                paint
            )
            y += 24f
            report.objectiveMastery.values.forEach { mastery ->
                canvas.drawText(
                    String.format(
                        Locale.US,
                        "%s -> Pre %.1f%% Post %.1f%%",
                        mastery.objective,
                        mastery.pre,
                        mastery.post
                    ),
                    20f,
                    y,
                    paint
                )
                y += 20f
            }
            document.finishPage(page)
            FileOutputStream(file).use { output ->
                document.writeTo(output)
            }
        } finally {
            document.close()
        }
        return FileRef(file.absolutePath)
    }

    override suspend fun exportStudentPdf(report: StudentReport): FileRef {
        val file = File(context.filesDir, "student-report-${report.student.id}.pdf")
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint().apply { textSize = 14f }
            var y = 40f
            canvas.drawText("Student: ${report.student.displayName}", 20f, y, paint)
            y += 24f
            canvas.drawText(
                String.format(Locale.US, "Pre %.1f%% -> Post %.1f%%", report.preScore, report.postScore),
                20f,
                y,
                paint
            )
            y += 24f
            report.mastery.values.forEach { mastery ->
                canvas.drawText(
                    String.format(
                        Locale.US,
                        "%s: Pre %.1f%% Post %.1f%%",
                        mastery.objective,
                        mastery.pre,
                        mastery.post
                    ),
                    20f,
                    y,
                    paint
                )
                y += 20f
            }
            document.finishPage(page)
            FileOutputStream(file).use { output -> document.writeTo(output) }
        } finally {
            document.close()
        }
        return FileRef(file.absolutePath)
    }

    override suspend fun exportCsv(rows: List<CsvRow>): FileRef {
        val file = File(context.filesDir, "quizmaster-export.csv")
        OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8).use { writer ->
            rows.forEach { row ->
                val line = row.cells.joinToString(",") { cell -> escapeCsvCell(cell) }
                writer.append(line)
                writer.append('\n')
            }
        }
        return FileRef(file.absolutePath)
    }

    private fun escapeCsvCell(value: String): String {
        val normalized = value.replace("\r\n", "\n").replace("\r", "\n")
        val escaped = normalized.replace("\"", "\"\"")
        val needsQuoting = escaped.any { it == ',' || it == '\n' || it == '"' }
        return if (needsQuoting) "\"$escaped\"" else escaped
    }
}

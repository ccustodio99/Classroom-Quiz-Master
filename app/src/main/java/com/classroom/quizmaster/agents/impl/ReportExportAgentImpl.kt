package com.classroom.quizmaster.agents.impl

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.classroom.quizmaster.agents.ReportExportAgent
import com.classroom.quizmaster.data.model.FileRef
import com.classroom.quizmaster.data.repo.ClassReportData
import com.classroom.quizmaster.data.repo.LearningGainRow
import com.classroom.quizmaster.data.repo.ReportRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportExportAgentImpl @Inject constructor(
    private val reportRepo: ReportRepo,
    @param:ApplicationContext private val context: Context // Fixed: explicit use-site target
) : ReportExportAgent {

    override suspend fun exportClassSummaryPdf(classId: String): Result<FileRef> = runCatching {
        val reportData = reportRepo.getClassReportData(classId)
        val learningGain = reportRepo.getLearningGainRows(classId)
        val file = writeClassSummaryPdf(reportData, learningGain)
        file.toFileRef()
    }

    override suspend fun exportLearningGainCsv(classId: String): Result<FileRef> = runCatching {
        val rows = reportRepo.getLearningGainRows(classId)
        val file = writeLearningGainCsv(rows)
        file.toFileRef()
    }

    private fun writeClassSummaryPdf(
        data: ClassReportData,
        learningGain: List<LearningGainRow>
    ): File {
        val file = createOutputFile(prefix = "class_summary", extension = "pdf")
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 22f
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 16f
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            textSize = 14f
            isAntiAlias = true
        }

        var y = 60f
        canvas.drawText(
            "Class Summary • ${data.classInfo.subject} ${data.classInfo.section}".trim(),
            48f,
            y,
            titlePaint
        )

        y += 40f
        canvas.drawText("Class Details", 48f, y, subtitlePaint)
        y += 24f
        canvas.drawText("Class code: ${data.classInfo.code}", 48f, y, bodyPaint)
        y += 20f
        canvas.drawText("Owner: ${data.classInfo.ownerId}", 48f, y, bodyPaint)
        y += 20f
        val teachersCount = data.roster.count { it.role.name.contains("TEACHER") } // Pre-calculate
        canvas.drawText(
            "Members: ${data.roster.size} • Teachers: $teachersCount", // Fixed string interpolation
            48f,
            y,
            bodyPaint
        )

        y += 32f
        canvas.drawText("Classwork Overview", 48f, y, subtitlePaint)
        y += 24f
        if (data.classwork.isEmpty()) {
            canvas.drawText("No classwork published yet.", 48f, y, bodyPaint)
            y += 20f
        } else {
            data.classwork.sortedBy { classworkItem -> classworkItem.dueAt ?: Long.MAX_VALUE } // Named lambda param
                .take(MAX_ROWS_SECTION)
                .forEach { classwork ->
                    val dueText = classwork.dueAt?.let { timestamp -> "Due: ${formatTimestamp(timestamp)}" } ?: "No due date" // Named lambda param
                    canvas.drawText(
                        "• ${classwork.title} (${classwork.type}) — $dueText",
                        48f,
                        y,
                        bodyPaint
                    )
                    y += 20f
                }
        }

        y += 24f
        canvas.drawText("Learning Gain Snapshot", 48f, y, subtitlePaint)
        y += 24f
        if (learningGain.isEmpty()) {
            canvas.drawText("No learning gain data yet.", 48f, y, bodyPaint)
        } else {
            canvas.drawText(
                String.format(Locale.US, "%-12s %8s %8s %10s", "Learner", "Pre", "Post", "Δ"),
                48f,
                y,
                bodyPaint
            )
            y += 20f
            learningGain.take(MAX_ROWS_SECTION).forEach { row ->
                canvas.drawText(
                    String.format(
                        Locale.US,
                        "%-12s %8s %8s %10s",
                        row.userId.take(12),
                        row.preScore.format(),
                        row.postScore.format(),
                        row.gain.formatPercent()
                    ),
                    48f,
                    y,
                    bodyPaint
                )
                y += 18f
            }
        }

        document.finishPage(page)
        document.writeTo(FileOutputStream(file))
        document.close()
        return file // Fixed: explicit return
    }

    private fun writeLearningGainCsv(rows: List<LearningGainRow>): File {
        val file = createOutputFile(prefix = "learning_gain", extension = "csv")
        file.bufferedWriter().use { writer ->
            writer.appendLine("user_id,pre_score,post_score,gain")
            rows.forEach { row ->
                writer.appendLine(
                    listOf(
                        row.userId,
                        row.preScore.format(),
                        row.postScore.format(),
                        row.gain.format()
                    ).joinToString(separator = ",")
                )
            }
        }
        return file
    }

    private fun createOutputFile(prefix: String, extension: String): File {
        val reportsDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "reports"
        )
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        val fileName = "${prefix}_${System.currentTimeMillis()}.$extension"
        return File(reportsDir, fileName)
    }

    private fun File.toFileRef(): FileRef = FileRef(
        id = absolutePath,
        name = name,
        downloadUrl = toURI().toString()
    )

    private fun Double.format(): String = String.format(Locale.US, "%.2f", this)

    private fun Double.formatPercent(): String = String.format(Locale.US, "%.1f%%", this * 100)

    private fun formatTimestamp(timestamp: Long): String =
        dateFormatter.format(Instant.ofEpochMilli(timestamp))

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MAX_ROWS_SECTION = 10
        private val dateFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.getDefault())
                .withZone(ZoneId.systemDefault())
    }
}
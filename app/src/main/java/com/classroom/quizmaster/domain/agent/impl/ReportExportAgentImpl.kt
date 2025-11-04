package com.classroom.quizmaster.domain.agent.impl

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.classroom.quizmaster.data.local.BlueprintLocalDataSource
import com.classroom.quizmaster.domain.agent.ReportExportAgent
import com.classroom.quizmaster.domain.model.FileRef
import java.io.File

class ReportExportAgentImpl(
    private val context: Context,
    private val localData: BlueprintLocalDataSource
) : ReportExportAgent {

    override suspend fun exportClassSummaryPdf(classId: String): Result<FileRef> = runCatching {
        val classroom = localData.findClassById(classId)
            ?: throw IllegalArgumentException("Class not found.")
        val roster = localData.rosterFor(classId)
        val assignments = localData.classworkFor(classId)

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(792, 1120, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 32f
        }

        var y = 80f
        canvas.drawText("Class Summary", 72f, y, paint)
        paint.textSize = 18f
        y += 48f
        canvas.drawText("Subject: ${classroom.subject}", 72f, y, paint)
        y += 28f
        canvas.drawText("Section: ${classroom.section}", 72f, y, paint)
        y += 28f
        canvas.drawText("Join Code: ${classroom.code}", 72f, y, paint)
        y += 40f
        paint.textSize = 20f
        canvas.drawText("Roster (${roster.size})", 72f, y, paint)
        paint.textSize = 16f
        roster.forEach { entry ->
            y += 24f
            canvas.drawText("- ${entry.role.name.lowercase().replaceFirstChar { it.uppercase() }} · ${entry.userId}", 96f, y, paint)
        }
        y += 36f
        paint.textSize = 20f
        canvas.drawText("Classwork (${assignments.size})", 72f, y, paint)
        paint.textSize = 16f
        assignments.forEach { bundle ->
            y += 24f
            val dueText = bundle.item.dueAt?.let { "Due ${java.util.Date(it)}" } ?: "No due date"
            canvas.drawText("- ${bundle.item.title} · ${bundle.item.type.name.lowercase()} · $dueText", 96f, y, paint)
        }

        pdf.finishPage(page)

        val outFile = File(context.cacheDir, "class-summary-$classId.pdf")
        outFile.outputStream().use { pdf.writeTo(it) }
        pdf.close()

        FileRef(path = outFile.absolutePath, mimeType = "application/pdf")
    }

    override suspend fun exportLearningGainCsv(classId: String): Result<FileRef> = runCatching {
        val classwork = localData.classworkFor(classId)
        val rows = buildList {
            add("classwork_id,user_id,grade")
            classwork.forEach { bundle ->
                val submissions = localData.submissionsFor(bundle.item.id)
                submissions.forEach { submission ->
                    val grade = submission.grade?.toString() ?: ""
                    add("${bundle.item.id},${submission.userId},$grade")
                }
            }
        }
        val outFile = File(context.cacheDir, "learning-gain-$classId.csv")
        outFile.writeText(rows.joinToString(separator = "\n"))
        FileRef(path = outFile.absolutePath, mimeType = "text/csv")
    }
}

package com.classroom.quizmaster.ui.materials

import com.classroom.quizmaster.domain.model.LearningMaterial
import com.classroom.quizmaster.domain.model.MaterialAttachment
import com.classroom.quizmaster.domain.model.MaterialAttachmentType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

data class MaterialSummaryUi(
    val id: String,
    val title: String,
    val description: String,
    val classroomName: String,
    val topicName: String,
    val updatedRelative: String,
    val attachmentCount: Int,
    val attachmentTypes: List<MaterialAttachmentType>
)

fun LearningMaterial.toSummaryUi(): MaterialSummaryUi =
    MaterialSummaryUi(
        id = id,
        title = title.ifBlank { "Untitled material" },
        description = description,
        classroomName = classroomName.ifBlank { "Unknown class" },
        topicName = topicName,
        updatedRelative = formatMaterialRelativeTime(updatedAt),
        attachmentCount = attachments.size,
        attachmentTypes = attachments.map(MaterialAttachment::type)
    )

fun formatMaterialRelativeTime(updatedAt: Instant): String {
    val now = Clock.System.now()
    val duration = (now - updatedAt).coerceAtLeast(Duration.ZERO)
    return when {
        duration < 1.minutes -> "Just now"
        duration < 1.hours -> "${duration.inWholeMinutes} min ago"
        duration < 24.hours -> "${duration.inWholeHours} hr ago"
        duration < 7.days -> "${duration.inWholeDays} d ago"
        else -> "${duration.inWholeDays / 7} wk ago"
    }
}

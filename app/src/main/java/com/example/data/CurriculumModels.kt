package com.example.data

import androidx.compose.ui.graphics.Color

enum class StandardGrade(
    val id: String,
    val shortName: String,
    val title: String,
    val subtitle: String,
    val hasStreams: Boolean,
    val nextStandardId: String?
) {
    CLASS_9(
        id = "class_9",
        shortName = "Class 9",
        title = "Class 9 Foundation",
        subtitle = "Fundamental Sciences, Mathematics & Logic",
        hasStreams = false,
        nextStandardId = "class_10"
    ),
    CLASS_10(
        id = "class_10",
        shortName = "Class 10",
        title = "Class 10 Board Foundation",
        subtitle = "Board Exam Prep & Applied Sciences",
        hasStreams = false,
        nextStandardId = "class_11"
    ),
    CLASS_11(
        id = "class_11",
        shortName = "Class 11",
        title = "Class 11 Senior Secondary",
        subtitle = "Calculus, Electromagnetism & Deep Stream Specialization",
        hasStreams = true,
        nextStandardId = "class_12"
    ),
    CLASS_12(
        id = "class_12",
        shortName = "Class 12",
        title = "Class 12 Senior & Competitive",
        subtitle = "Board Mastery, Entrance Prep & Advanced Synthesis",
        hasStreams = true,
        nextStandardId = null
    );

    companion object {
        fun fromId(id: String): StandardGrade {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CLASS_11
        }
    }
}

enum class AcademicStream(
    val id: String,
    val title: String,
    val tagLabel: String,
    val description: String,
    val applicableStandards: Set<String>
) {
    GENERAL_FOUNDATION(
        id = "general",
        title = "Integrated Foundation",
        tagLabel = "CORE",
        description = "Core integrated Mathematics, Sciences, Social Studies & Coding",
        applicableStandards = setOf("class_9", "class_10")
    ),
    SCIENCE_PCM(
        id = "science_pcm",
        title = "Science (PCM)",
        tagLabel = "PCM",
        description = "Physics, Chemistry, Advanced Mathematics & AI/Computing",
        applicableStandards = setOf("class_11", "class_12")
    ),
    SCIENCE_PCB(
        id = "science_pcb",
        title = "Science (PCB)",
        tagLabel = "PCB",
        description = "Physics, Chemistry, Biology, Genetics & Physiology",
        applicableStandards = setOf("class_11", "class_12")
    ),
    COMMERCE(
        id = "commerce",
        title = "Commerce & Finance",
        tagLabel = "COMMERCE",
        description = "Accountancy, Business Studies, Micro/Macro Economics & Applied Math",
        applicableStandards = setOf("class_11", "class_12")
    ),
    ARTS_HUMANITIES(
        id = "arts",
        title = "Arts & Humanities",
        tagLabel = "HUMANITIES",
        description = "History, Political Science, Economics, Psychology & Rhetoric",
        applicableStandards = setOf("class_11", "class_12")
    );

    companion object {
        fun fromId(id: String): AcademicStream {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SCIENCE_PCM
        }

        fun streamsForStandard(standardId: String): List<AcademicStream> {
            val std = StandardGrade.fromId(standardId)
            return if (!std.hasStreams) {
                listOf(GENERAL_FOUNDATION)
            } else {
                listOf(SCIENCE_PCM, SCIENCE_PCB, COMMERCE, ARTS_HUMANITIES)
            }
        }
    }
}

enum class BatchType(
    val id: String,
    val title: String,
    val badgeLabel: String,
    val description: String
) {
    LIVE_INTERACTIVE(
        id = "live",
        title = "Live Interactive Batch",
        badgeLabel = "LIVE",
        description = "Scheduled live lectures, real-time doubt solving, and live-to-recorded playback"
    ),
    ANIMATED_VISUAL(
        id = "animated",
        title = "Animated Visual Batch",
        badgeLabel = "3D/ANIMATED",
        description = "High-retention visual concept maps, 3D simulations, and geometric breakdowns"
    ),
    RECORDED_DEEP(
        id = "recorded",
        title = "Recorded Masterclass & Notes",
        badgeLabel = "SELF-PACED",
        description = "Exhaustive chapter scrolls, deep formula sheets, and self-paced mastery"
    );

    companion object {
        fun fromId(id: String): BatchType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LIVE_INTERACTIVE
        }
    }
}

data class LevelPlaybackProgress(
    val levelId: String,
    val vidyaId: String,
    val lastPositionSec: Int = 0,
    val totalDurationSec: Int = 0,
    val completed: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

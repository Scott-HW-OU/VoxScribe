package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ActionItem(
    val task: String,
    val owner: String = "Unassigned",
    val priority: String = "Medium",
    val completed: Boolean = false
)

enum class SummaryLengthOption(val id: String, val label: String, val promptInstruction: String) {
    SHORT("SHORT", "Short", "Keep the executiveSummary very concise (2 to 3 high-impact sentences maximum)."),
    MEDIUM("MEDIUM", "Medium", "Write a balanced, structured 1-paragraph executiveSummary (4 to 6 sentences)."),
    DETAILED("DETAILED", "Detailed", "Write a thorough, comprehensive multi-paragraph executiveSummary covering context, nuances, and outcomes.")
}

enum class SummaryFocusAspect(val id: String, val label: String) {
    ACTION_ITEMS("ACTION_ITEMS", "Action Items"),
    DECISIONS_MADE("DECISIONS_MADE", "Decisions Made"),
    IMPORTANT_DATES("IMPORTANT_DATES", "Important Dates"),
    BUDGET_PRICING("BUDGET_PRICING", "Budget & Metrics"),
    RISKS_COMPLIANCE("RISKS_COMPLIANCE", "Risks & Compliance")
}

@Serializable
data class CallAnalysisResult(
    val transcript: String,
    val executiveSummary: String,
    val keyPoints: List<String>,
    val decisionsMade: List<String> = emptyList(),
    val importantDates: List<String> = emptyList(),
    val actionItems: List<ActionItem>,
    val keywords: List<String>,
    val sentiment: String
)

@Serializable
data class LiveInCallInsight(
    val rollingSummary: String = "",
    val liveTakeaways: List<String> = emptyList(),
    val liveKeywords: List<String> = emptyList(),
    val detectedDates: List<String> = emptyList(),
    val lastUpdatedSecond: Int = 0,
    val poweredByAccount: String = ""
)

@Entity(tableName = "call_recordings")
data class CallRecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactName: String,
    val phoneNumber: String,
    val callDirection: String, // "INCOMING", "OUTGOING", "CONFERENCE"
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int,
    val audioFilePath: String?,
    val consentJurisdiction: String,
    val audibleNoticePlayed: Boolean,
    val consentScriptUsed: String,
    val consentTimestamp: Long,
    val transcript: String,
    val executiveSummary: String,
    val keyPointsJson: String,
    val decisionsMadeJson: String = "[]",
    val importantDatesJson: String = "[]",
    val actionItemsJson: String,
    val keywordTagsJson: String,
    val sentiment: String = "Neutral",
    val summaryStyle: String = "Executive Brief",
    val summaryLength: String = "MEDIUM", // "SHORT", "MEDIUM", "DETAILED"
    val focusAspectsJson: String = "[\"ACTION_ITEMS\",\"DECISIONS_MADE\",\"IMPORTANT_DATES\"]",
    val geminiAccountEmail: String = "",
    val cloudSyncStatus: String = "PENDING", // "SYNCED", "PENDING", "SYNCING", "FAILED"
    val cloudStorageUri: String? = null,
    val cloudChecksumSha256: String? = null,
    val lastSyncedAt: Long? = null,
    val exportedToNotion: Boolean = false,
    val exportedToEvernote: Boolean = false,
    val exportedToOneNote: Boolean = false,
    val exportedToKeep: Boolean = false,
    val isStarred: Boolean = false
) {
    fun keyPointsList(): List<String> = decodeList(keyPointsJson)
    fun decisionsMadeList(): List<String> = decodeList(decisionsMadeJson)
    fun importantDatesList(): List<String> = decodeList(importantDatesJson)
    fun keywordTagsList(): List<String> = decodeList(keywordTagsJson)
    fun focusAspectsList(): List<String> = decodeList(focusAspectsJson)

    fun actionItemsList(): List<ActionItem> = try {
        json.decodeFromString<List<ActionItem>>(actionItemsJson)
    } catch (_: Exception) {
        emptyList()
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        private fun decodeList(raw: String): List<String> = try {
            json.decodeFromString<List<String>>(raw)
        } catch (_: Exception) {
            emptyList()
        }

        fun encodeStringList(list: List<String>): String = json.encodeToString(list)
        fun encodeActionItems(list: List<ActionItem>): String = json.encodeToString(list)
    }
}

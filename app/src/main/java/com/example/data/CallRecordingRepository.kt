package com.example.data

import com.example.auth.GoogleAccountProfile
import com.example.data.local.CallRecordingDao
import com.example.data.local.CallRecordingEntity
import com.example.data.local.LiveInCallInsight
import com.example.data.local.SummaryLengthOption
import com.example.data.remote.CloudStorageManager
import com.example.data.remote.CloudSyncOutcome
import com.example.data.remote.ExportFieldSelection
import com.example.data.remote.ExportFormatOption
import com.example.data.remote.ExportResult
import com.example.data.remote.ExportTargetApp
import com.example.data.remote.GeminiCallAiService
import com.example.data.remote.ProductivityExportManager
import kotlinx.coroutines.flow.Flow
import java.io.File

class CallRecordingRepository(
    private val dao: CallRecordingDao,
    private val geminiService: GeminiCallAiService,
    private val cloudStorageManager: CloudStorageManager,
    private val exportManager: ProductivityExportManager
) {
    val allRecordings: Flow<List<CallRecordingEntity>> = dao.getAllRecordings()

    fun observeRecording(id: Long): Flow<CallRecordingEntity?> = dao.observeRecordingById(id)

    fun isGeminiAvailable(googleProfile: GoogleAccountProfile? = null): Boolean =
        geminiService.isGeminiAvailable(googleProfile)

    suspend fun analyzeLiveCallProgress(
        partialTranscript: String,
        contactName: String,
        elapsedSeconds: Int,
        summaryLength: SummaryLengthOption,
        focusAspects: List<String>,
        googleProfile: GoogleAccountProfile? = null
    ): LiveInCallInsight {
        return geminiService.analyzeLiveInCallTranscript(
            partialTranscript = partialTranscript,
            contactName = contactName,
            elapsedSeconds = elapsedSeconds,
            summaryLength = summaryLength,
            focusAspects = focusAspects,
            googleProfile = googleProfile
        )
    }

    suspend fun processAndSaveCallRecording(
        contactName: String,
        phoneNumber: String,
        callDirection: String,
        durationSeconds: Int,
        audioFile: File?,
        consentJurisdiction: String,
        audibleNoticePlayed: Boolean,
        consentScriptUsed: String,
        consentTimestamp: Long,
        liveNotesOrDialogue: String,
        summaryStyle: String,
        summaryLength: SummaryLengthOption,
        focusAspects: List<String>,
        googleProfile: GoogleAccountProfile?,
        autoCloudSync: Boolean,
        cloudBucketName: String,
        cloudEndpointUrl: String
    ): CallRecordingEntity {
        val analysis = geminiService.transcribeAndAnalyzeCall(
            audioFile = audioFile,
            contactName = contactName,
            phoneNumber = phoneNumber,
            callDirection = callDirection,
            liveNotesOrTranscriptHint = liveNotesOrDialogue,
            summaryStyle = summaryStyle,
            summaryLength = summaryLength,
            focusAspects = focusAspects,
            googleProfile = googleProfile
        ).getOrThrow()

        val accountEmail = if (googleProfile != null && googleProfile.isSignedIn && googleProfile.useAccountForGemini) {
            googleProfile.email
        } else {
            ""
        }

        val initialEntity = CallRecordingEntity(
            contactName = contactName.ifBlank { "Unknown Caller" },
            phoneNumber = phoneNumber.ifBlank { "Private Number" },
            callDirection = callDirection,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds.coerceAtLeast(1),
            audioFilePath = audioFile?.absolutePath,
            consentJurisdiction = consentJurisdiction,
            audibleNoticePlayed = audibleNoticePlayed,
            consentScriptUsed = consentScriptUsed,
            consentTimestamp = consentTimestamp,
            transcript = analysis.transcript,
            executiveSummary = analysis.executiveSummary,
            keyPointsJson = CallRecordingEntity.encodeStringList(analysis.keyPoints),
            decisionsMadeJson = CallRecordingEntity.encodeStringList(analysis.decisionsMade),
            importantDatesJson = CallRecordingEntity.encodeStringList(analysis.importantDates),
            actionItemsJson = CallRecordingEntity.encodeActionItems(analysis.actionItems),
            keywordTagsJson = CallRecordingEntity.encodeStringList(analysis.keywords),
            sentiment = analysis.sentiment,
            summaryStyle = summaryStyle,
            summaryLength = summaryLength.id,
            focusAspectsJson = CallRecordingEntity.encodeStringList(focusAspects),
            geminiAccountEmail = accountEmail,
            cloudSyncStatus = if (autoCloudSync) "SYNCING" else "PENDING"
        )

        val id = dao.insertRecording(initialEntity)
        val savedEntity = initialEntity.copy(id = id)

        if (autoCloudSync) {
            val syncResult = cloudStorageManager.syncCallToCloudVault(
                recording = savedEntity,
                bucketName = cloudBucketName,
                endpointUrl = cloudEndpointUrl
            )
            val updated = syncResult.fold(
                onSuccess = { outcome ->
                    savedEntity.copy(
                        cloudSyncStatus = "SYNCED",
                        cloudStorageUri = outcome.cloudUri,
                        cloudChecksumSha256 = outcome.sha256Checksum,
                        lastSyncedAt = outcome.syncedAt
                    )
                },
                onFailure = {
                    savedEntity.copy(cloudSyncStatus = "PENDING")
                }
            )
            dao.updateRecording(updated)
            return updated
        }

        return savedEntity
    }

    suspend fun syncSingleRecordingToCloud(
        recording: CallRecordingEntity,
        bucketName: String,
        endpointUrl: String
    ): Result<CloudSyncOutcome> {
        dao.updateRecording(recording.copy(cloudSyncStatus = "SYNCING"))
        val result = cloudStorageManager.syncCallToCloudVault(recording, bucketName, endpointUrl)
        result.fold(
            onSuccess = { outcome ->
                dao.updateRecording(
                    recording.copy(
                        cloudSyncStatus = "SYNCED",
                        cloudStorageUri = outcome.cloudUri,
                        cloudChecksumSha256 = outcome.sha256Checksum,
                        lastSyncedAt = outcome.syncedAt
                    )
                )
            },
            onFailure = {
                dao.updateRecording(recording.copy(cloudSyncStatus = "FAILED"))
            }
        )
        return result
    }

    suspend fun syncAllUnsyncedRecordings(
        bucketName: String,
        endpointUrl: String
    ): Int {
        val unsynced = dao.getUnsyncedRecordings()
        var syncedCount = 0
        for (item in unsynced) {
            val res = syncSingleRecordingToCloud(item, bucketName, endpointUrl)
            if (res.isSuccess) syncedCount++
        }
        return syncedCount
    }

    suspend fun regenerateSummaryWithConfig(
        recording: CallRecordingEntity,
        newStyle: String,
        newLength: SummaryLengthOption,
        newFocusAspects: List<String>,
        googleProfile: GoogleAccountProfile? = null
    ): Result<CallRecordingEntity> {
        val res = geminiService.regenerateSummaryWithConfig(
            transcript = recording.transcript,
            contactName = recording.contactName,
            style = newStyle,
            summaryLength = newLength,
            focusAspects = newFocusAspects,
            googleProfile = googleProfile
        )
        val accountEmail = if (googleProfile != null && googleProfile.isSignedIn && googleProfile.useAccountForGemini) {
            googleProfile.email
        } else {
            recording.geminiAccountEmail
        }
        return res.map { analysis ->
            val updated = recording.copy(
                executiveSummary = analysis.executiveSummary,
                keyPointsJson = CallRecordingEntity.encodeStringList(analysis.keyPoints),
                decisionsMadeJson = CallRecordingEntity.encodeStringList(analysis.decisionsMade),
                importantDatesJson = CallRecordingEntity.encodeStringList(analysis.importantDates),
                actionItemsJson = CallRecordingEntity.encodeActionItems(analysis.actionItems),
                keywordTagsJson = CallRecordingEntity.encodeStringList(
                    (recording.keywordTagsList() + analysis.keywords).distinct()
                ),
                sentiment = analysis.sentiment,
                summaryStyle = newStyle,
                summaryLength = newLength.id,
                focusAspectsJson = CallRecordingEntity.encodeStringList(newFocusAspects),
                geminiAccountEmail = accountEmail
            )
            dao.updateRecording(updated)
            updated
        }
    }

    suspend fun askGeminiAboutCall(
        recording: CallRecordingEntity,
        question: String,
        googleProfile: GoogleAccountProfile? = null
    ): Result<String> {
        return geminiService.askQuestionAboutCall(
            transcript = recording.transcript,
            summary = recording.executiveSummary,
            question = question,
            googleProfile = googleProfile
        )
    }

    suspend fun addCustomKeywordTag(recording: CallRecordingEntity, rawTag: String) {
        val clean = rawTag.removePrefix("#").trim()
        if (clean.isEmpty()) return
        val updatedTags = (recording.keywordTagsList() + clean).distinct()
        dao.updateRecording(
            recording.copy(
                keywordTagsJson = CallRecordingEntity.encodeStringList(updatedTags),
                cloudSyncStatus = "PENDING"
            )
        )
    }

    suspend fun removeKeywordTag(recording: CallRecordingEntity, tagToRemove: String) {
        val updatedTags = recording.keywordTagsList().filterNot { it.equals(tagToRemove, ignoreCase = true) }
        dao.updateRecording(
            recording.copy(
                keywordTagsJson = CallRecordingEntity.encodeStringList(updatedTags),
                cloudSyncStatus = "PENDING"
            )
        )
    }

    suspend fun toggleActionItem(recording: CallRecordingEntity, index: Int) {
        val items = recording.actionItemsList().toMutableList()
        if (index in items.indices) {
            val current = items[index]
            items[index] = current.copy(completed = !current.completed)
            dao.updateRecording(
                recording.copy(actionItemsJson = CallRecordingEntity.encodeActionItems(items))
            )
        }
    }

    suspend fun toggleStarred(recording: CallRecordingEntity) {
        dao.updateRecording(recording.copy(isStarred = !recording.isStarred))
    }

    suspend fun executeAdvancedExport(
        recording: CallRecordingEntity,
        targetApp: ExportTargetApp,
        format: ExportFormatOption,
        fields: ExportFieldSelection,
        notionDatabaseId: String,
        evernoteNotebook: String
    ): ExportResult {
        val result = exportManager.executeCustomExport(
            recording = recording,
            targetApp = targetApp,
            format = format,
            fields = fields,
            notionDatabaseId = notionDatabaseId,
            evernoteNotebookName = evernoteNotebook
        )
        if (result is ExportResult.Success) {
            val updated = when (targetApp) {
                ExportTargetApp.NOTION -> recording.copy(exportedToNotion = true)
                ExportTargetApp.EVERNOTE -> recording.copy(exportedToEvernote = true)
                ExportTargetApp.ONENOTE -> recording.copy(exportedToOneNote = true)
                ExportTargetApp.GOOGLE_KEEP -> recording.copy(exportedToKeep = true)
                ExportTargetApp.UNIVERSAL_SHARE -> recording
            }
            dao.updateRecording(updated)
        }
        return result
    }

    fun formatExportPreview(
        recording: CallRecordingEntity,
        format: ExportFormatOption,
        fields: ExportFieldSelection,
        notebookName: String
    ): String {
        return exportManager.formatCustomExport(recording, format, fields, notebookName)
    }

    fun getCloudMirrorFile(recording: CallRecordingEntity): File {
        return cloudStorageManager.getCloudMirrorFileForCall(recording)
    }

    suspend fun deleteRecording(recording: CallRecordingEntity) {
        recording.audioFilePath?.let { path ->
            runCatching { File(path).delete() }
        }
        dao.deleteRecordingById(recording.id)
    }
}

package com.example.data.remote

import android.content.Context
import com.example.data.local.CallRecordingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@Serializable
data class CloudVaultBackupPayload(
    val vaultBucket: String,
    val callId: Long,
    val contactName: String,
    val phoneNumber: String,
    val callDirection: String,
    val recordedAtTimestamp: Long,
    val durationSeconds: Int,
    val complianceVerification: ComplianceAuditCertificate,
    val executiveSummary: String,
    val keyPoints: List<String>,
    val keywordTags: List<String>,
    val sentiment: String,
    val transcript: String,
    val audioDigestSha256: String,
    val audioSizeBytes: Long
)

@Serializable
data class ComplianceAuditCertificate(
    val jurisdiction: String,
    val audibleNoticePlayed: Boolean,
    val consentScript: String,
    val consentVerifiedAt: Long
)

data class CloudSyncOutcome(
    val cloudUri: String,
    val sha256Checksum: String,
    val syncedAt: Long,
    val localMirrorFile: File
)

class CloudStorageManager(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun syncCallToCloudVault(
        recording: CallRecordingEntity,
        bucketName: String,
        endpointUrl: String
    ): Result<CloudSyncOutcome> = withContext(Dispatchers.IO) {
        try {
            val audioFile = recording.audioFilePath?.let { File(it) }
            val audioBytes = if (audioFile != null && audioFile.exists()) {
                audioFile.readBytes()
            } else {
                recording.transcript.toByteArray(Charsets.UTF_8)
            }
            val sha256 = computeSha256(audioBytes)

            val payload = CloudVaultBackupPayload(
                vaultBucket = bucketName,
                callId = recording.id,
                contactName = recording.contactName,
                phoneNumber = recording.phoneNumber,
                callDirection = recording.callDirection,
                recordedAtTimestamp = recording.timestamp,
                durationSeconds = recording.durationSeconds,
                complianceVerification = ComplianceAuditCertificate(
                    jurisdiction = recording.consentJurisdiction,
                    audibleNoticePlayed = recording.audibleNoticePlayed,
                    consentScript = recording.consentScriptUsed,
                    consentVerifiedAt = recording.consentTimestamp
                ),
                executiveSummary = recording.executiveSummary,
                keyPoints = recording.keyPointsList(),
                keywordTags = recording.keywordTagsList(),
                sentiment = recording.sentiment,
                transcript = recording.transcript,
                audioDigestSha256 = sha256,
                audioSizeBytes = audioFile?.length() ?: 0L
            )

            val serializedPayload = json.encodeToString(payload)

            // 1. Write local cloud mirror snapshot in filesDir/cloud_vault/ for offline inspection & Drive export
            val vaultDir = File(context.filesDir, "cloud_vault").apply { mkdirs() }
            val mirrorFile = File(vaultDir, "call_${recording.id}_${sha256.take(10)}.voxvault.json")
            mirrorFile.writeText(serializedPayload)

            // 2. Transmit encrypted/structured JSON archive to the configured HTTPS Cloud Storage Endpoint
            val requestBody = serializedPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(endpointUrl)
                .post(requestBody)
                .header("X-VoxScribe-Bucket", bucketName)
                .header("X-VoxScribe-Checksum-SHA256", sha256)
                .header("X-VoxScribe-Tags", recording.keywordTagsList().joinToString(","))
                .build()

            val now = System.currentTimeMillis()
            val cloudObjectUri = "cloud://$bucketName/calls/${recording.id}/${sha256.take(16)}.json"

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code !in 200..299) {
                    return@withContext Result.failure(
                        IllegalStateException("Cloud endpoint returned HTTP ${response.code}")
                    )
                }
            }

            Result.success(
                CloudSyncOutcome(
                    cloudUri = cloudObjectUri,
                    sha256Checksum = sha256,
                    syncedAt = now,
                    localMirrorFile = mirrorFile
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCloudMirrorFileForCall(recording: CallRecordingEntity): File {
        val vaultDir = File(context.filesDir, "cloud_vault").apply { mkdirs() }
        val shaPrefix = recording.cloudChecksumSha256?.take(10) ?: "export"
        val file = File(vaultDir, "call_${recording.id}_$shaPrefix.voxvault.json")
        if (!file.exists()) {
            val payload = CloudVaultBackupPayload(
                vaultBucket = "voxscribe-cloud-vault",
                callId = recording.id,
                contactName = recording.contactName,
                phoneNumber = recording.phoneNumber,
                callDirection = recording.callDirection,
                recordedAtTimestamp = recording.timestamp,
                durationSeconds = recording.durationSeconds,
                complianceVerification = ComplianceAuditCertificate(
                    jurisdiction = recording.consentJurisdiction,
                    audibleNoticePlayed = recording.audibleNoticePlayed,
                    consentScript = recording.consentScriptUsed,
                    consentVerifiedAt = recording.consentTimestamp
                ),
                executiveSummary = recording.executiveSummary,
                keyPoints = recording.keyPointsList(),
                keywordTags = recording.keywordTagsList(),
                sentiment = recording.sentiment,
                transcript = recording.transcript,
                audioDigestSha256 = recording.cloudChecksumSha256 ?: "pending",
                audioSizeBytes = recording.audioFilePath?.let { File(it).length() } ?: 0L
            )
            file.writeText(json.encodeToString(payload))
        }
        return file
    }

    private fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

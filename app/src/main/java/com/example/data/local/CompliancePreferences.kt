package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "voxscribe_settings")

data class ComplianceAndCloudSettings(
    val jurisdictionMode: String = "TWO_PARTY", // "TWO_PARTY", "GDPR_EU_UK", "ONE_PARTY"
    val requireConsentModalBeforeRecord: Boolean = true,
    val autoPlayAudibleTtsAlert: Boolean = true,
    val routeAlertToSpeakerphone: Boolean = true,
    val customAnnouncementScript: String = DEFAULT_TWO_PARTY_SCRIPT,
    val autoCloudSync: Boolean = true,
    val cloudBucketName: String = "voxscribe-cloud-vault-prod",
    val cloudEndpointUrl: String = "https://postman-echo.com/post",
    val notionDatabaseId: String = "",
    val evernoteNotebookName: String = "VoxScribe Call Summaries"
) {
    val jurisdictionDisplayName: String
        get() = when (jurisdictionMode) {
            "TWO_PARTY" -> "Two-Party / All-Party Consent (Strict)"
            "GDPR_EU_UK" -> "GDPR / UK Business Transparency Notice"
            else -> "One-Party Consent (Self-Consent + Optional Alert)"
        }

    companion object {
        const val DEFAULT_TWO_PARTY_SCRIPT =
            "Attention: This call is being recorded and transcribed by VoxScribe for note-taking and compliance purposes. By continuing on the line, all parties consent to this recording."
        const val DEFAULT_GDPR_SCRIPT =
            "Privacy Notice: This call is being recorded and processed using automated AI transcription in accordance with data protection regulations. Please state now if you object to recording."
        const val DEFAULT_ONE_PARTY_SCRIPT =
            "Notice: Call recording is now active for personal note-taking and summary generation."
    }
}

class CompliancePreferencesRepository(private val context: Context) {
    private object Keys {
        val JURISDICTION = stringPreferencesKey("jurisdiction_mode")
        val REQUIRE_CONSENT_MODAL = booleanPreferencesKey("require_consent_modal")
        val AUTO_PLAY_TTS = booleanPreferencesKey("auto_play_tts")
        val SPEAKERPHONE_ALERT = booleanPreferencesKey("speakerphone_alert")
        val CUSTOM_SCRIPT = stringPreferencesKey("custom_announcement_script")
        val AUTO_CLOUD_SYNC = booleanPreferencesKey("auto_cloud_sync")
        val CLOUD_BUCKET = stringPreferencesKey("cloud_bucket_name")
        val CLOUD_ENDPOINT = stringPreferencesKey("cloud_endpoint_url")
        val NOTION_DB_ID = stringPreferencesKey("notion_database_id")
        val EVERNOTE_NOTEBOOK = stringPreferencesKey("evernote_notebook_name")
    }

    val settingsFlow: Flow<ComplianceAndCloudSettings> = context.dataStore.data.map { prefs ->
        ComplianceAndCloudSettings(
            jurisdictionMode = prefs[Keys.JURISDICTION] ?: "TWO_PARTY",
            requireConsentModalBeforeRecord = prefs[Keys.REQUIRE_CONSENT_MODAL] ?: true,
            autoPlayAudibleTtsAlert = prefs[Keys.AUTO_PLAY_TTS] ?: true,
            routeAlertToSpeakerphone = prefs[Keys.SPEAKERPHONE_ALERT] ?: true,
            customAnnouncementScript = prefs[Keys.CUSTOM_SCRIPT]
                ?: ComplianceAndCloudSettings.DEFAULT_TWO_PARTY_SCRIPT,
            autoCloudSync = prefs[Keys.AUTO_CLOUD_SYNC] ?: true,
            cloudBucketName = prefs[Keys.CLOUD_BUCKET] ?: "voxscribe-cloud-vault-prod",
            cloudEndpointUrl = prefs[Keys.CLOUD_ENDPOINT] ?: "https://postman-echo.com/post",
            notionDatabaseId = prefs[Keys.NOTION_DB_ID] ?: "",
            evernoteNotebookName = prefs[Keys.EVERNOTE_NOTEBOOK] ?: "VoxScribe Call Summaries"
        )
    }

    suspend fun updateJurisdiction(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.JURISDICTION] = mode
            prefs[Keys.CUSTOM_SCRIPT] = when (mode) {
                "TWO_PARTY" -> ComplianceAndCloudSettings.DEFAULT_TWO_PARTY_SCRIPT
                "GDPR_EU_UK" -> ComplianceAndCloudSettings.DEFAULT_GDPR_SCRIPT
                else -> ComplianceAndCloudSettings.DEFAULT_ONE_PARTY_SCRIPT
            }
        }
    }

    suspend fun updateRequireConsentModal(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REQUIRE_CONSENT_MODAL] = enabled }
    }

    suspend fun updateAutoPlayTts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY_TTS] = enabled }
    }

    suspend fun updateRouteToSpeakerphone(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SPEAKERPHONE_ALERT] = enabled }
    }

    suspend fun updateCustomScript(script: String) {
        context.dataStore.edit { it[Keys.CUSTOM_SCRIPT] = script }
    }

    suspend fun updateAutoCloudSync(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_CLOUD_SYNC] = enabled }
    }

    suspend fun updateCloudConfig(bucketName: String, endpointUrl: String) {
        context.dataStore.edit {
            it[Keys.CLOUD_BUCKET] = bucketName.trim().ifEmpty { "voxscribe-cloud-vault-prod" }
            it[Keys.CLOUD_ENDPOINT] = endpointUrl.trim().ifEmpty { "https://postman-echo.com/post" }
        }
    }

    suspend fun updateExportTargets(notionDbId: String, evernoteNotebook: String) {
        context.dataStore.edit {
            it[Keys.NOTION_DB_ID] = notionDbId.trim()
            it[Keys.EVERNOTE_NOTEBOOK] = evernoteNotebook.trim().ifEmpty { "VoxScribe Call Summaries" }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.auth.GoogleAccountProfile
import com.example.data.local.ComplianceAndCloudSettings
import com.example.ui.theme.ComplianceAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSynced

@Composable
fun ComplianceAndSettingsScreen(
    settings: ComplianceAndCloudSettings,
    googleAccount: GoogleAccountProfile,
    isSpeakingNotice: Boolean,
    onUpdateJurisdiction: (String) -> Unit,
    onUpdateRequireModal: (Boolean) -> Unit,
    onUpdateAutoPlayTts: (Boolean) -> Unit,
    onUpdateSpeakerphoneRoute: (Boolean) -> Unit,
    onSaveCustomScript: (String) -> Unit,
    onTestPlayTtsScript: () -> Unit,
    onSaveExportTargets: (notionDbId: String, evernoteNotebook: String) -> Unit,
    onOpenGoogleAccountSheet: () -> Unit
) {
    var scriptInput by remember(settings.customAnnouncementScript) {
        mutableStateOf(settings.customAnnouncementScript)
    }
    var notionDbInput by remember(settings.notionDatabaseId) {
        mutableStateOf(settings.notionDatabaseId)
    }
    var evernoteNotebookInput by remember(settings.evernoteNotebookName) {
        mutableStateOf(settings.evernoteNotebookName)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("compliance_settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Google Account & Associated Gemini AI Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Account & Associated Gemini AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (googleAccount.isSignedIn) {
                                    "Signed in as ${googleAccount.displayName} (${googleAccount.email})"
                                } else {
                                    "Sign in with your Google Account to use your account's associated Google Gemini for AI transcription & summaries"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (googleAccount.isSignedIn) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Signed In",
                                tint = EmeraldSynced
                            )
                        }
                    }

                    Button(
                        onClick = onOpenGoogleAccountSheet,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF00262D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compliance_open_google_account_button")
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (googleAccount.isSignedIn) {
                                "Manage Google Account & Gemini Settings (${googleAccount.email})"
                            } else {
                                "Sign in with Google Account for Gemini AI"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Local Privacy Laws & Pre-Recording Compliance Alert Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = ComplianceAmber,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "Privacy Laws & Pre-Call Alert",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Configure consent alerts & spoken TTS disclosure before call recording",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Legal Jurisdiction Preset:",
                        style = MaterialTheme.typography.labelLarge,
                        color = ComplianceAmber
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "TWO_PARTY" to "All-Party (Strict)",
                            "GDPR_EU_UK" to "GDPR / UK",
                            "ONE_PARTY" to "One-Party"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = settings.jurisdictionMode == code,
                                onClick = { onUpdateJurisdiction(code) },
                                label = { Text(label) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("jurisdiction_chip_${code.lowercase()}")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Require Pre-Call Consent Alert Dialog",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Prompts you to verify participant notification before recording starts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.requireConsentModalBeforeRecord,
                            onCheckedChange = onUpdateRequireModal,
                            modifier = Modifier.testTag("switch_require_consent_modal")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Play Spoken TTS Alert on Call Start",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Automatically speaks the disclosure script aloud when recording begins",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoPlayAudibleTtsAlert,
                            onCheckedChange = onUpdateAutoPlayTts,
                            modifier = Modifier.testTag("switch_auto_play_tts")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Route Audible Alert to Speakerphone",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Ensures remote party hears the spoken compliance announcement",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.routeAlertToSpeakerphone,
                            onCheckedChange = onUpdateSpeakerphoneRoute
                        )
                    }

                    OutlinedTextField(
                        value = scriptInput,
                        onValueChange = { scriptInput = it },
                        label = { Text("Spoken TTS Compliance Disclosure Script") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compliance_script_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTestPlayTtsScript,
                            enabled = !isSpeakingNotice,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_play_tts_script_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isSpeakingNotice) "Speaking…" else "Test Audio")
                        }

                        Button(
                            onClick = { onSaveCustomScript(scriptInput) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_compliance_script_button")
                        ) {
                            Text("Save Script")
                        }
                    }
                }
            }
        }

        // 3. Productivity & Note-Taking Integrations (Notion, Evernote, OneNote, Google Keep)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = null, tint = ElectricCyan)
                        Text(
                            text = "Note-Taking & Productivity Export Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Configure Notion Database ID and Evernote Notebook for 1-click exports alongside Microsoft OneNote, Google Keep, CSV, Markdown, and Plain Text.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = notionDbInput,
                        onValueChange = { notionDbInput = it },
                        label = { Text("Notion Database ID (Optional for direct API export)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notion_db_id_input")
                    )

                    OutlinedTextField(
                        value = evernoteNotebookInput,
                        onValueChange = { evernoteNotebookInput = it },
                        label = { Text("Default Evernote / OneNote Notebook Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("evernote_notebook_input")
                    )

                    Button(
                        onClick = { onSaveExportTargets(notionDbInput, evernoteNotebookInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_export_settings_button")
                    ) {
                        Text("Save Productivity Export Preferences")
                    }
                }
            }
        }
    }
}

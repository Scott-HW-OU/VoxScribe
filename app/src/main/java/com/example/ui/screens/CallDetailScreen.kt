package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.auth.GoogleAccountProfile
import com.example.data.local.CallRecordingEntity
import com.example.data.local.SummaryFocusAspect
import com.example.data.local.SummaryLengthOption
import com.example.data.remote.ExportFormatOption
import com.example.data.remote.ExportTargetApp
import com.example.ui.theme.ComplianceAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSynced
import com.example.ui.theme.EvernoteGreen
import com.example.ui.theme.RecordingCrimson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CallDetailScreen(
    recording: CallRecordingEntity,
    googleAccount: GoogleAccountProfile,
    isPlayingAudio: Boolean,
    audioPositionMs: Int,
    audioDurationMs: Int,
    playbackSpeed: Float,
    isRegeneratingSummary: Boolean,
    isAskingQuestion: Boolean,
    callQuestionAnswer: String?,
    isSyncingCloud: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekAudio: (Float) -> Unit,
    onCyclePlaybackSpeed: () -> Unit,
    onRegenerateSummary: (style: String, length: SummaryLengthOption, focusAspects: List<String>) -> Unit,
    onAskQuestion: (String) -> Unit,
    onToggleActionItem: (Int) -> Unit,
    onAddCustomTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onQuickExport: (ExportTargetApp) -> Unit,
    onOpenAdvancedExportSheet: (ExportTargetApp, ExportFormatOption) -> Unit,
    onSyncToCloud: () -> Unit,
    onShareCloudVaultJson: () -> Unit,
    onDeleteCall: () -> Unit
) {
    BackHandler { onBack() }

    var selectedLength by remember(recording.id, recording.summaryLength) {
        mutableStateOf(
            SummaryLengthOption.entries.find { it.id == recording.summaryLength }
                ?: SummaryLengthOption.MEDIUM
        )
    }
    var selectedStyle by remember(recording.id, recording.summaryStyle) {
        mutableStateOf(recording.summaryStyle)
    }
    var selectedFocusSet by remember(recording.id, recording.focusAspectsJson) {
        val current = recording.focusAspectsList().toSet()
        mutableStateOf(
            current.ifEmpty {
                setOf(
                    SummaryFocusAspect.ACTION_ITEMS.id,
                    SummaryFocusAspect.DECISIONS_MADE.id,
                    SummaryFocusAspect.IMPORTANT_DATES.id
                )
            }
        )
    }

    var newTagInput by remember { mutableStateOf("") }
    var questionInput by remember { mutableStateOf("") }

    val dateStr = remember(recording.timestamp) {
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(recording.timestamp))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("call_detail_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Navigation Bar inside Detail
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("detail_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("All Calls")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onOpenAdvancedExportSheet(ExportTargetApp.NOTION, ExportFormatOption.MARKDOWN)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF00262D)
                        ),
                        modifier = Modifier.testTag("open_advanced_export_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Advanced Export", fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onDeleteCall,
                        modifier = Modifier.testTag("delete_call_button")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Call",
                            tint = RecordingCrimson
                        )
                    }
                }
            }
        }

        // 1. Call Overview & Compliance Audit Card
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
                    Text(
                        text = recording.contactName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${recording.phoneNumber} • ${recording.callDirection} • $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = ComplianceAmber.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = ComplianceAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${recording.consentJurisdiction} (${if (recording.audibleNoticePlayed) "Audible TTS Alert Played" else "Verified"})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ComplianceAmber
                                )
                            }
                        }

                        Surface(
                            color = ElectricCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Sentiment: ${recording.sentiment}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        val acctLabel = recording.geminiAccountEmail.ifBlank {
                            googleAccount.takeIf { it.isSignedIn }?.email.orEmpty()
                        }
                        if (acctLabel.isNotBlank()) {
                            Surface(
                                color = EmeraldSynced.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Gemini Account: $acctLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSynced,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Audio Player Strip
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier.testTag("audio_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingAudio) "Pause" else "Play",
                                    tint = ElectricCyan
                                )
                            }

                            val progress = if (audioDurationMs > 0) {
                                (audioPositionMs.toFloat() / audioDurationMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            Slider(
                                value = progress,
                                onValueChange = onSeekAudio,
                                modifier = Modifier.weight(1f)
                            )

                            AssistChip(
                                onClick = onCyclePlaybackSpeed,
                                label = { Text("${playbackSpeed}x") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Speed,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. Multi-App Productivity Export Strip (Notion, Evernote, OneNote, Google Keep, CSV/MD/TXT)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Export to Note-Taking & Productivity Apps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "1-tap export to Notion, Evernote, OneNote, Google Keep, or custom CSV/MD/TXT",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onQuickExport(ExportTargetApp.NOTION) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("quick_export_notion_button")
                        ) {
                            Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (recording.exportedToNotion) "Notion ✓" else "Notion")
                        }

                        Button(
                            onClick = { onQuickExport(ExportTargetApp.EVERNOTE) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EvernoteGreen.copy(alpha = 0.22f),
                                contentColor = EmeraldSynced
                            ),
                            modifier = Modifier.testTag("quick_export_evernote_button")
                        ) {
                            Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (recording.exportedToEvernote) "Evernote (.enex) ✓" else "Evernote (.enex)")
                        }

                        Button(
                            onClick = { onQuickExport(ExportTargetApp.ONENOTE) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7719AA).copy(alpha = 0.25f),
                                contentColor = Color(0xFFE0B3FF)
                            ),
                            modifier = Modifier.testTag("quick_export_onenote_button")
                        ) {
                            Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (recording.exportedToOneNote) "OneNote ✓" else "OneNote")
                        }

                        Button(
                            onClick = { onQuickExport(ExportTargetApp.GOOGLE_KEEP) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ComplianceAmber.copy(alpha = 0.22f),
                                contentColor = ComplianceAmber
                            ),
                            modifier = Modifier.testTag("quick_export_keep_button")
                        ) {
                            Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (recording.exportedToKeep) "Google Keep ✓" else "Google Keep")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onOpenAdvancedExportSheet(ExportTargetApp.UNIVERSAL_SHARE, ExportFormatOption.CSV)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_format_fields_export_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configure Format (MD / TXT / CSV / ENEX) & Select Fields…")
                    }
                }
            }
        }

        // 3. Configurable AI Executive Summary Card (Length: Short/Medium/Detailed + Focus Aspects)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Executive Summary (${recording.summaryLength})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (googleAccount.isSignedIn && googleAccount.useAccountForGemini) {
                                    "Powered by Google Gemini (${googleAccount.email})"
                                } else {
                                    "Customize length & focus aspects below"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = recording.executiveSummary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("call_executive_summary_text")
                    )

                    val keyPoints = recording.keyPointsList()
                    if (keyPoints.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        Text(
                            text = "Key Takeaways",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        keyPoints.forEach { point ->
                            Text(
                                text = "• $point",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    // Interactive Summary Length & Focus Customization inside Detail
                    Text(
                        text = "Customize Summary Length & Focus Aspects",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryLengthOption.entries.forEach { len ->
                            val isSelected = selectedLength == len
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedLength = len },
                                label = { Text(len.label) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_length_${len.id.lowercase()}")
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SummaryFocusAspect.entries.forEach { aspect ->
                            val isSelected = selectedFocusSet.contains(aspect.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val mutable = selectedFocusSet.toMutableSet()
                                    if (mutable.contains(aspect.id)) {
                                        if (mutable.size > 1) mutable.remove(aspect.id)
                                    } else {
                                        mutable.add(aspect.id)
                                    }
                                    selectedFocusSet = mutable
                                },
                                label = { Text(aspect.label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                ),
                                modifier = Modifier.testTag("detail_focus_${aspect.id.lowercase()}")
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onRegenerateSummary(
                                selectedStyle,
                                selectedLength,
                                selectedFocusSet.toList()
                            )
                        },
                        enabled = !isRegeneratingSummary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("regenerate_summary_button")
                    ) {
                        if (isRegeneratingSummary) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Re-Summarizing with Gemini…")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply ${selectedLength.label} Summary & Selected Focus")
                        }
                    }
                }
            }
        }

        // 4. Decisions Made & Important Dates Card
        val decisions = recording.decisionsMadeList()
        val importantDates = recording.importantDatesList()
        if (decisions.isNotEmpty() || importantDates.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (decisions.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Gavel,
                                    contentDescription = null,
                                    tint = EmeraldSynced,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Decisions Made",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            decisions.forEach { decision ->
                                Text(
                                    text = "✓ $decision",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        if (importantDates.isNotEmpty()) {
                            if (decisions.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = ComplianceAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Important Dates & Deadlines Mentioned",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            importantDates.forEach { dateItem ->
                                Text(
                                    text = "📅 $dateItem",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ComplianceAmber
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Action Items Checklist Card
        item {
            val actions = recording.actionItemsList()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Action Items (${actions.count { it.completed }}/${actions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (actions.isEmpty()) {
                        Text(
                            text = "No action items extracted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        actions.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.completed,
                                    onCheckedChange = { onToggleActionItem(index) },
                                    modifier = Modifier.testTag("action_item_checkbox_$index")
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.task,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    Text(
                                        text = "Owner: ${item.owner} • Priority: ${item.priority}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Automatic Keyword Tags & Cloud Vault Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Automatic Keyword Tags & Cloud Vault",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        recording.keywordTagsList().forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = {},
                                label = { Text("#$tag") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { onRemoveTag(tag) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove $tag",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTagInput,
                            onValueChange = { newTagInput = it },
                            placeholder = { Text("Add custom #tag…") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_custom_tag_input")
                        )
                        Button(
                            onClick = {
                                if (newTagInput.isNotBlank()) {
                                    onAddCustomTag(newTagInput)
                                    newTagInput = ""
                                }
                            },
                            modifier = Modifier.testTag("add_custom_tag_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Tag")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSyncToCloud,
                            enabled = !isSyncingCloud,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sync_single_call_cloud_button")
                        ) {
                            Icon(
                                imageVector = if (recording.cloudSyncStatus == "SYNCED") Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (recording.cloudSyncStatus == "SYNCED") "Re-Sync Cloud" else "Sync to Cloud")
                        }

                        OutlinedButton(
                            onClick = onShareCloudVaultJson,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cloud JSON")
                        }
                    }
                }
            }
        }

        // 7. Ask Gemini About This Call
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Ask Google Gemini About This Call",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = questionInput,
                            onValueChange = { questionInput = it },
                            placeholder = { Text("e.g., What pricing discount or deadline was agreed?") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ask_call_question_input")
                        )
                        IconButton(
                            onClick = {
                                if (questionInput.isNotBlank()) {
                                    onAskQuestion(questionInput)
                                }
                            },
                            enabled = !isAskingQuestion,
                            modifier = Modifier.testTag("ask_call_question_button")
                        ) {
                            if (isAskingQuestion) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Ask", tint = ElectricCyan)
                            }
                        }
                    }
                    if (!callQuestionAnswer.isNullOrBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = callQuestionAnswer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // 8. Full Speaker-Labeled Call Transcript
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Full Call Transcript",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = recording.transcript,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.testTag("call_transcript_text")
                    )
                }
            }
        }
    }
}

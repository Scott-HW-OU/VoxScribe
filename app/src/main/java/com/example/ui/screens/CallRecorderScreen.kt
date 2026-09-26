package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.auth.GoogleAccountProfile
import com.example.data.local.ComplianceAndCloudSettings
import com.example.data.local.SummaryFocusAspect
import com.example.data.local.SummaryLengthOption
import com.example.ui.RecorderFormState
import com.example.ui.theme.ComplianceAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSynced
import com.example.ui.theme.RecordingCrimson

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CallRecorderScreen(
    formState: RecorderFormState,
    settings: ComplianceAndCloudSettings,
    googleAccount: GoogleAccountProfile,
    isRecording: Boolean,
    isPaused: Boolean,
    elapsedSeconds: Int,
    waveformAmplitudes: List<Float>,
    isSpeakingNotice: Boolean,
    isSimulatingCall: Boolean,
    partialSpeechText: String,
    hasMicPermission: Boolean,
    onRequestMicPermissionAndRecord: () -> Unit,
    onUpdateContactName: (String) -> Unit,
    onUpdatePhoneNumber: (String) -> Unit,
    onUpdateCallDirection: (String) -> Unit,
    onUpdateScenario: (String) -> Unit,
    onUpdateSummaryStyle: (String) -> Unit,
    onUpdateSummaryLength: (SummaryLengthOption) -> Unit,
    onToggleFocusAspect: (String) -> Unit,
    onUpdateLiveNotes: (String) -> Unit,
    onToggleLiveSpeechRecognition: () -> Unit,
    onAddQuickUtterance: (speaker: String, text: String) -> Unit,
    onRefreshLiveAiInsight: () -> Unit,
    onPlayAudibleComplianceNotice: () -> Unit,
    onPauseResumeRecording: () -> Unit,
    onStartSimulatedDialogue: () -> Unit,
    onStopAndTranscribe: () -> Unit,
    onOpenGoogleAccountSheet: () -> Unit
) {
    var quickSpeakerLine by remember { mutableStateOf("") }
    var activeSpeakerToggle by remember { mutableStateOf("Speaker 2 (Caller)") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("call_recorder_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Google Account Gemini AI Active Context Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenGoogleAccountSheet() }
                    .testTag("recorder_google_account_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (googleAccount.isSignedIn) EmeraldSynced else ElectricCyan
                        )
                        Column {
                            Text(
                                text = if (googleAccount.isSignedIn && googleAccount.useAccountForGemini) {
                                    "Gemini AI • ${googleAccount.email}"
                                } else {
                                    "Google Account Gemini AI"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (googleAccount.isSignedIn && googleAccount.useAccountForGemini) {
                                    "Using Google Gemini associated with ${googleAccount.displayName} (${googleAccount.associatedProjectId})"
                                } else {
                                    "Tap to sign in with your Google Account to power AI transcription & summaries"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    AssistChip(
                        onClick = onOpenGoogleAccountSheet,
                        label = { Text(if (googleAccount.isSignedIn) "Manage" else "Sign In") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        // 2. Live Waveform & Call Recording Studio Console
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (isRecording) RecordingCrimson.copy(alpha = 0.2f) else ComplianceAmber.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Mic else Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isRecording) RecordingCrimson else ComplianceAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isRecording) {
                                        if (isPaused) "RECORDING PAUSED" else "LIVE CALL RECORDING"
                                    } else {
                                        settings.jurisdictionDisplayName
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isRecording) RecordingCrimson else ComplianceAmber
                                )
                            }
                        }

                        val timerFormatted = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
                        Text(
                            text = timerFormatted,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = if (isRecording) RecordingCrimson else ElectricCyan,
                            modifier = Modifier.testTag("recording_timer_text")
                        )
                    }

                    // Waveform Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .background(
                                MaterialTheme.colorScheme.background,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barCount = waveformAmplitudes.size.coerceAtLeast(1)
                            val totalSpacing = 4.dp.toPx() * (barCount - 1)
                            val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(3f)
                            waveformAmplitudes.forEachIndexed { index, amp ->
                                val barHeight = (size.height * amp.coerceIn(0.08f, 1f)).coerceAtLeast(6f)
                                val x = index * (barWidth + 4.dp.toPx())
                                val y = (size.height - barHeight) / 2f
                                val barColor = when {
                                    isRecording && !isPaused -> if (index % 3 == 0) RecordingCrimson else ElectricCyan
                                    else -> ElectricCyan.copy(alpha = 0.45f)
                                }
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                        }
                    }

                    // Primary Record / Stop Controls
                    if (formState.isAnalyzingWithAi) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(28.dp))
                            Column {
                                Text(
                                    text = "Transcribing & Generating ${formState.summaryLength.label} AI Summary…",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (googleAccount.isSignedIn && googleAccount.useAccountForGemini) {
                                        "Using Google Gemini (${googleAccount.email}) + Cloud Vault Sync"
                                    } else {
                                        "Extracting focus aspects, keyword tags & syncing to Cloud Vault"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (!isRecording) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPlayAudibleComplianceNotice,
                                enabled = !isSpeakingNotice,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pre_call_tts_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isSpeakingNotice) "Speaking…" else "Test TTS Alert")
                            }

                            Button(
                                onClick = onRequestMicPermissionAndRecord,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RecordingCrimson,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("start_call_recording_button")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Call Recording", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPauseResumeRecording,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pause_resume_recording_button")
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPaused) "Resume" else "Pause")
                            }

                            Button(
                                onClick = onStopAndTranscribe,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldSynced,
                                    contentColor = Color(0xFF042217)
                                ),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(48.dp)
                                    .testTag("stop_and_transcribe_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Finish & Transcribe", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Real-Time In-Call Transcription & On-The-Fly AI Analysis Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = ElectricCyan
                            )
                            Column {
                                Text(
                                    text = "Real-Time Call Transcript & Live AI Analysis",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Live dialogue stream with on-the-fly Gemini takeaways & auto-tags",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = onRefreshLiveAiInsight,
                            modifier = Modifier.testTag("refresh_live_ai_insight_button")
                        ) {
                            if (formState.isUpdatingLiveInsight) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = ElectricCyan
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh Live AI Insight")
                            }
                        }
                    }

                    // Live STT & Simulated Multi-Speaker Call Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onStartSimulatedDialogue,
                            enabled = !isSimulatingCall,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simulate_live_call_dialogue_button")
                        ) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSimulatingCall) "Speaking Live Call…" else "Play Live Call Dialogue",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        FilterChip(
                            selected = formState.liveSpeechRecognitionEnabled,
                            onClick = onToggleLiveSpeechRecognition,
                            label = { Text("Mic Live STT") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("toggle_live_stt_chip")
                        )
                    }

                    if (partialSpeechText.isNotBlank()) {
                        Text(
                            text = "Listening: \"$partialSpeechText…\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricCyan
                        )
                    }

                    // Quick Live Utterance Adder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {
                                activeSpeakerToggle = if (activeSpeakerToggle.startsWith("Speaker 1")) {
                                    "Speaker 2 (${formState.contactName.substringBefore(" ")})"
                                } else {
                                    "Speaker 1 (You)"
                                }
                            },
                            label = { Text(activeSpeakerToggle, style = MaterialTheme.typography.labelSmall) }
                        )
                        OutlinedTextField(
                            value = quickSpeakerLine,
                            onValueChange = { quickSpeakerLine = it },
                            placeholder = { Text("Add live spoken line or note…") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("live_utterance_input")
                        )
                        IconButton(
                            onClick = {
                                if (quickSpeakerLine.isNotBlank()) {
                                    onAddQuickUtterance(activeSpeakerToggle, quickSpeakerLine)
                                    quickSpeakerLine = ""
                                }
                            },
                            modifier = Modifier.testTag("add_live_utterance_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add Line", tint = ElectricCyan)
                        }
                    }

                    OutlinedTextField(
                        value = formState.liveTranscriptAndNotes,
                        onValueChange = onUpdateLiveNotes,
                        label = { Text("Live Call Transcript Stream (Editable)") },
                        placeholder = {
                            Text("Tap 'Play Live Call Dialogue', speak with Mic Live STT, or type live call notes…")
                        },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("live_transcript_editor")
                    )

                    // On-The-Fly AI Summary & Live Takeaways Box
                    val liveInsight = formState.liveInCallInsight
                    if (liveInsight.rollingSummary.isNotBlank() || liveInsight.liveTakeaways.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("live_ai_insight_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "On-the-Fly Gemini AI Analysis",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ElectricCyan
                                        )
                                    }
                                    if (liveInsight.poweredByAccount.isNotBlank()) {
                                        Text(
                                            text = liveInsight.poweredByAccount,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldSynced
                                        )
                                    }
                                }

                                if (liveInsight.rollingSummary.isNotBlank()) {
                                    Text(
                                        text = liveInsight.rollingSummary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                if (liveInsight.liveTakeaways.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Live Takeaways Detected:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ComplianceAmber
                                        )
                                        liveInsight.liveTakeaways.forEach { takeaway ->
                                            Text(
                                                text = "• $takeaway",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                if (liveInsight.detectedDates.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = ComplianceAmber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Dates Mentioned: ${liveInsight.detectedDates.joinToString(", ")}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ComplianceAmber
                                        )
                                    }
                                }

                                if (liveInsight.liveKeywords.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        liveInsight.liveKeywords.forEach { kw ->
                                            Surface(
                                                color = ElectricCyan.copy(alpha = 0.16f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "#$kw",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ElectricCyan,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Configurable AI Summarization Settings (Summary Length, Focus Aspects, Style)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "AI Call Summarization Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Desired Summary Length
                    Text(
                        text = "Desired Summary Length",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryLengthOption.entries.forEach { option ->
                            val selected = formState.summaryLength == option
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateSummaryLength(option) },
                                label = { Text(option.label) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("summary_length_${option.id.lowercase()}")
                            )
                        }
                    }

                    // Key Aspects to Focus On
                    Text(
                        text = "Key Aspects to Focus On",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SummaryFocusAspect.entries.forEach { aspect ->
                            val selected = formState.selectedFocusAspects.contains(aspect.id)
                            FilterChip(
                                selected = selected,
                                onClick = { onToggleFocusAspect(aspect.id) },
                                label = { Text(aspect.label) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                ),
                                modifier = Modifier.testTag("focus_aspect_${aspect.id.lowercase()}")
                            )
                        }
                    }

                    // Summary Style
                    Text(
                        text = "Summary Format Style",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                    val styles = listOf(
                        "Executive Brief",
                        "Sales & Deal Desk",
                        "Action-First Checklist",
                        "Legal & Compliance Audit"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        styles.forEach { style ->
                            FilterChip(
                                selected = formState.summaryStyle == style,
                                onClick = { onUpdateSummaryStyle(style) },
                                label = { Text(style) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Call Participant & Dialogue Scenario Metadata
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Call Participant & Scenario Metadata",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = formState.contactName,
                        onValueChange = onUpdateContactName,
                        label = { Text("Participant / Contact Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_name_input")
                    )

                    OutlinedTextField(
                        value = formState.phoneNumber,
                        onValueChange = onUpdatePhoneNumber,
                        label = { Text("Phone Number / Conference Bridge") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_number_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("INCOMING", "OUTGOING", "CONFERENCE").forEach { dir ->
                            FilterChip(
                                selected = formState.callDirection == dir,
                                onClick = { onUpdateCallDirection(dir) },
                                label = { Text(dir) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Text(
                        text = "Simulated Call Dialogue Topic (for TTS testing):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Sales & Contract Renewal",
                            "Product Roadmap & Engineering",
                            "Client Onboarding & Budget"
                        ).forEach { topic ->
                            FilterChip(
                                selected = formState.selectedScenario == topic,
                                onClick = { onUpdateScenario(topic) },
                                label = { Text(topic) }
                            )
                        }
                    }
                }
            }
        }
    }
}

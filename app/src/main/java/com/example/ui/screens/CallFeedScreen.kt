package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.auth.GoogleAccountProfile
import com.example.data.local.CallRecordingEntity
import com.example.ui.theme.ComplianceAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSynced
import com.example.ui.theme.RecordingCrimson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CallFeedScreen(
    recordings: List<CallRecordingEntity>,
    allRecordingsCount: Int,
    searchQuery: String,
    selectedTagFilter: String?,
    onlyStarredFilter: Boolean,
    topTags: List<Pair<String, Int>>,
    googleAccount: GoogleAccountProfile,
    onSearchChange: (String) -> Unit,
    onSelectTagFilter: (String?) -> Unit,
    onToggleStarredFilter: () -> Unit,
    onOpenCallDetail: (Long) -> Unit,
    onToggleStarCall: (CallRecordingEntity) -> Unit,
    onNavigateToRecorder: () -> Unit,
    onOpenGoogleAccountSheet: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("call_feed_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Banner + Google Account Gemini Status Strip
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(186.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_waveform_1790408245164),
                        contentDescription = "VoxScribe AI Call Recorder Hero",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x8809111E),
                                        Color(0xEB09111E)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = ElectricCyan.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clickable { onOpenGoogleAccountSheet() }
                                    .testTag("hero_google_account_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (googleAccount.isSignedIn && googleAccount.useAccountForGemini) {
                                            "Gemini AI • ${googleAccount.email}"
                                        } else {
                                            "Sign in with Google for Gemini AI"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan
                                    )
                                }
                            }

                            Surface(
                                color = ComplianceAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = ComplianceAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "$allRecordingsCount Calls",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ComplianceAmber
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Live Call Scribe & Cloud Vault",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Real-time transcription, custom-length AI summaries, compliance alerts, and Notion / Evernote / OneNote / Keep exports.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD0E2FF),
                                maxLines = 2
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = onNavigateToRecorder,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RecordingCrimson,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("hero_record_call_button")
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Record Call", style = MaterialTheme.typography.labelLarge)
                                }

                                if (!googleAccount.isSignedIn) {
                                    AssistChip(
                                        onClick = onOpenGoogleAccountSheet,
                                        label = { Text("Connect Google Account") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.AccountCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.height(36.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search & Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text("Search transcripts, summaries, #tags, or contacts…")
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_calls_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = onlyStarredFilter,
                        onClick = onToggleStarredFilter,
                        label = { Text("Starred") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (onlyStarredFilter) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("filter_starred_chip")
                    )

                    topTags.take(10).forEach { (tag, count) ->
                        val isSelected = selectedTagFilter.equals(tag, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectTagFilter(tag) },
                            label = { Text("#$tag ($count)") },
                            modifier = Modifier.testTag("filter_tag_$tag")
                        )
                    }
                }
            }
        }

        if (recordings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = if (allRecordingsCount == 0) {
                                "No Call Recordings Yet"
                            } else {
                                "No Matching Call Recordings"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Start a compliant call recording with real-time transcription, custom AI summary length & focus areas, automatic keyword tags, and Cloud Vault sync.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onNavigateToRecorder,
                            modifier = Modifier.testTag("empty_state_record_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Call Recorder Studio")
                        }
                    }
                }
            }
        } else {
            items(recordings, key = { it.id }) { recording ->
                CallRecordingCard(
                    recording = recording,
                    onClick = { onOpenCallDetail(recording.id) },
                    onToggleStar = { onToggleStarCall(recording) },
                    onTagClick = { tag -> onSelectTagFilter(tag) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CallRecordingCard(
    recording: CallRecordingEntity,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onTagClick: (String) -> Unit
) {
    val dateFmt = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault())
    val formattedDate = dateFmt.format(Date(recording.timestamp))
    val durationStr = "%02d:%02d".format(recording.durationSeconds / 60, recording.durationSeconds % 60)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("call_item_card_${recording.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (recording.callDirection) {
                            "OUTGOING" -> Icons.AutoMirrored.Filled.CallMade
                            "CONFERENCE" -> Icons.Default.Groups
                            else -> Icons.AutoMirrored.Filled.CallReceived
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = recording.callDirection,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recording.contactName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${recording.phoneNumber} • $formattedDate • $durationStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.testTag("star_button_${recording.id}")
                ) {
                    Icon(
                        imageVector = if (recording.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star Call",
                        tint = if (recording.isStarred) ComplianceAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status Badges Row (Compliance, Cloud Sync, Summary Length, Google Account)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusPill(
                    text = if (recording.audibleNoticePlayed) "TTS Alert Verified" else "Consent Verified",
                    color = ComplianceAmber
                )
                StatusPill(
                    text = if (recording.cloudSyncStatus == "SYNCED") "Cloud Synced" else "Cloud Pending",
                    color = if (recording.cloudSyncStatus == "SYNCED") EmeraldSynced else ElectricCyan
                )
                StatusPill(
                    text = "${recording.summaryLength} Summary",
                    color = ElectricCyan
                )
                if (recording.geminiAccountEmail.isNotBlank()) {
                    StatusPill(
                        text = "Gemini • ${recording.geminiAccountEmail}",
                        color = EmeraldSynced
                    )
                }
            }

            // Executive Summary Preview
            Text(
                text = recording.executiveSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Automatic Keyword Tags
            val tags = recording.keywordTagsList()
            if (tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.take(6).forEach { tag ->
                        AssistChip(
                            onClick = { onTagClick(tag) },
                            label = {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            // Exported Badges Row
            val exportedTargets = buildList {
                if (recording.exportedToNotion) add("Notion")
                if (recording.exportedToEvernote) add("Evernote")
                if (recording.exportedToOneNote) add("OneNote")
                if (recording.exportedToKeep) add("Google Keep")
            }
            if (exportedTargets.isNotEmpty()) {
                Text(
                    text = "Exported to: ${exportedTargets.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldSynced
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

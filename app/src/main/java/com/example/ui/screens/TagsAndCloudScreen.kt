package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CallRecordingEntity
import com.example.data.local.ComplianceAndCloudSettings
import com.example.ui.theme.ComplianceAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSynced

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsAndCloudScreen(
    recordings: List<CallRecordingEntity>,
    tagFrequencies: List<Pair<String, Int>>,
    selectedTagFilter: String?,
    settings: ComplianceAndCloudSettings,
    isSyncingCloud: Boolean,
    onSelectTag: (String?) -> Unit,
    onOpenCallDetail: (Long) -> Unit,
    onSyncAllToCloud: () -> Unit,
    onSyncSingleCall: (CallRecordingEntity) -> Unit,
    onShareCloudArchive: (CallRecordingEntity) -> Unit,
    onToggleAutoCloudSync: (Boolean) -> Unit,
    onUpdateCloudConfig: (bucket: String, endpoint: String) -> Unit
) {
    var bucketInput by remember(settings.cloudBucketName) {
        mutableStateOf(settings.cloudBucketName)
    }
    var endpointInput by remember(settings.cloudEndpointUrl) {
        mutableStateOf(settings.cloudEndpointUrl)
    }

    val syncedCount = recordings.count { it.cloudSyncStatus == "SYNCED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tags_and_cloud_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Cloud Storage Vault Overview Card
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Cloud Storage Vault & Auto-Tag Index",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$syncedCount of ${recordings.size} calls synced with SHA-256 verification",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatic Cloud Backup After Call",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Uploads transcript, AI summary, keyword tags, and SHA-256 audio digest to Cloud Vault",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoCloudSync,
                            onCheckedChange = onToggleAutoCloudSync,
                            modifier = Modifier.testTag("auto_cloud_sync_switch")
                        )
                    }

                    Button(
                        onClick = onSyncAllToCloud,
                        enabled = !isSyncingCloud,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSynced,
                            contentColor = Color(0xFF042217)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_all_cloud_button")
                    ) {
                        if (isSyncingCloud) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF042217)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing Calls to Cloud Storage…", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync All Pending Calls to Cloud Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Automatic Keyword Tag Cloud Explorer
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Tag, contentDescription = null, tint = ElectricCyan)
                        Text(
                            text = "Automatic Keyword Tag Cloud (${tagFrequencies.size} Unique Tags)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (tagFrequencies.isEmpty()) {
                        Text(
                            text = "Record a call to automatically generate AI keyword tags.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tagFrequencies.forEach { (tag, count) ->
                                val selected = selectedTagFilter.equals(tag, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectTag(tag) },
                                    label = { Text("#$tag ($count)") }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Cloud Bucket & Endpoint Configuration
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
                        text = "Cloud Vault Bucket & HTTPS Endpoint",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = bucketInput,
                        onValueChange = { bucketInput = it },
                        label = { Text("Cloud Storage Bucket Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = endpointInput,
                        onValueChange = { endpointInput = it },
                        label = { Text("Cloud Vault HTTPS Endpoint") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { onUpdateCloudConfig(bucketInput, endpointInput) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Cloud Storage Configuration")
                    }
                }
            }
        }

        // 4. Individual Cloud Vault Archive Items
        val displayedRecordings = if (selectedTagFilter.isNullOrBlank()) {
            recordings
        } else {
            recordings.filter { rec ->
                rec.keywordTagsList().any { it.equals(selectedTagFilter, ignoreCase = true) }
            }
        }

        items(displayedRecordings, key = { it.id }) { rec ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenCallDetail(rec.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rec.contactName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = rec.cloudStorageUri ?: "Pending Cloud Upload",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (rec.cloudSyncStatus == "SYNCED") EmeraldSynced else ComplianceAmber
                            )
                        }

                        Row {
                            IconButton(onClick = { onSyncSingleCall(rec) }) {
                                Icon(
                                    imageVector = if (rec.cloudSyncStatus == "SYNCED") Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                    contentDescription = "Sync Call",
                                    tint = if (rec.cloudSyncStatus == "SYNCED") EmeraldSynced else ElectricCyan
                                )
                            }
                            IconButton(onClick = { onShareCloudArchive(rec) }) {
                                Icon(
                                    imageVector = Icons.Default.IosShare,
                                    contentDescription = "Export Cloud JSON",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rec.keywordTagsList().forEach { tag ->
                            Surface(
                                color = ElectricCyan.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "#$tag",
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

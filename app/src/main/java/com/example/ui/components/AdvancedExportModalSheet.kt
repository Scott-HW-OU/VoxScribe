package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.ExportFieldSelection
import com.example.data.remote.ExportFormatOption
import com.example.data.remote.ExportTargetApp
import com.example.ui.ExportConfiguratorState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSynced

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedExportModalSheet(
    state: ExportConfiguratorState,
    previewText: String,
    onDismiss: () -> Unit,
    onSelectTargetApp: (ExportTargetApp) -> Unit,
    onSelectFormat: (ExportFormatOption) -> Unit,
    onUpdateFields: (ExportFieldSelection) -> Unit,
    onExecuteExport: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fields = state.fields

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Advanced Call Export Builder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose destination app, file format, and exact data fields",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close Export Sheet")
                }
            }

            // 1. Target Productivity App
            Text(
                text = "1. Destination Note / Productivity App",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExportTargetApp.entries.forEach { app ->
                    val selected = state.selectedTargetApp == app
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectTargetApp(app) },
                        label = { Text(app.label) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("export_target_${app.id.lowercase()}")
                    )
                }
            }

            // 2. Export Format
            Text(
                text = "2. Export Format",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExportFormatOption.entries.forEach { fmt ->
                    val selected = state.selectedFormat == fmt
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFormat(fmt) },
                        label = { Text(fmt.label) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("export_format_${fmt.id.lowercase()}")
                    )
                }
            }

            // 3. Data Fields to Include
            Text(
                text = "3. Select Data Fields to Include",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FieldToggleChip(
                    label = "Date & Time",
                    checked = fields.includeDateTime,
                    tag = "field_date_time",
                    onToggle = { onUpdateFields(fields.copy(includeDateTime = !fields.includeDateTime)) }
                )
                FieldToggleChip(
                    label = "Participants",
                    checked = fields.includeParticipants,
                    tag = "field_participants",
                    onToggle = { onUpdateFields(fields.copy(includeParticipants = !fields.includeParticipants)) }
                )
                FieldToggleChip(
                    label = "Privacy Compliance",
                    checked = fields.includeCompliance,
                    tag = "field_compliance",
                    onToggle = { onUpdateFields(fields.copy(includeCompliance = !fields.includeCompliance)) }
                )
                FieldToggleChip(
                    label = "AI Summary",
                    checked = fields.includeSummary,
                    tag = "field_summary",
                    onToggle = { onUpdateFields(fields.copy(includeSummary = !fields.includeSummary)) }
                )
                FieldToggleChip(
                    label = "Decisions & Dates",
                    checked = fields.includeDecisionsAndDates,
                    tag = "field_decisions_dates",
                    onToggle = {
                        onUpdateFields(fields.copy(includeDecisionsAndDates = !fields.includeDecisionsAndDates))
                    }
                )
                FieldToggleChip(
                    label = "Action Items",
                    checked = fields.includeActionItems,
                    tag = "field_action_items",
                    onToggle = { onUpdateFields(fields.copy(includeActionItems = !fields.includeActionItems)) }
                )
                FieldToggleChip(
                    label = "Keyword Tags",
                    checked = fields.includeTags,
                    tag = "field_tags",
                    onToggle = { onUpdateFields(fields.copy(includeTags = !fields.includeTags)) }
                )
                FieldToggleChip(
                    label = "Full Transcript",
                    checked = fields.includeTranscript,
                    tag = "field_transcript",
                    onToggle = { onUpdateFields(fields.copy(includeTranscript = !fields.includeTranscript)) }
                )
            }

            // 4. Live Export Preview
            Text(
                text = "Live Output Preview (${state.selectedFormat.label})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = previewText.take(800) + if (previewText.length > 800) "\n… [truncated in preview]" else "",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Button(
                onClick = onExecuteExport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("execute_advanced_export_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldSynced,
                    contentColor = Color(0xFF042217)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.IosShare, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export ${state.selectedFormat.label} to ${state.selectedTargetApp.label}",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FieldToggleChip(
    label: String,
    checked: Boolean,
    tag: String,
    onToggle: () -> Unit
) {
    FilterChip(
        selected = checked,
        onClick = onToggle,
        label = { Text(label) },
        leadingIcon = if (checked) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
            selectedLabelColor = ElectricCyan
        ),
        modifier = Modifier.testTag(tag)
    )
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ComplianceAndCloudSettings
import com.example.ui.theme.ComplianceAmber
import com.example.ui.theme.EmeraldSynced
import com.example.ui.theme.RecordingCrimson

@Composable
fun ConsentVerificationDialog(
    settings: ComplianceAndCloudSettings,
    contactName: String,
    isSpeakingNotice: Boolean,
    hasPlayedAudibleAlert: Boolean,
    partyConsentConfirmed: Boolean,
    onPlayAudibleNotice: () -> Unit,
    onToggleConsentConfirmed: (Boolean) -> Unit,
    onConfirmStartRecording: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Gavel,
                contentDescription = "Privacy Compliance Alert",
                tint = ComplianceAmber,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Pre-Call Privacy & Consent Alert",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Local & international wiretapping / data privacy laws (${settings.jurisdictionDisplayName}) require notifying call participants ($contactName) before recording begins.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = ComplianceAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Audible TTS Disclosure Script:",
                                style = MaterialTheme.typography.labelLarge,
                                color = ComplianceAmber
                            )
                        }
                        Text(
                            text = "\"${settings.customAnnouncementScript}\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                OutlinedButton(
                    onClick = onPlayAudibleNotice,
                    enabled = !isSpeakingNotice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("play_tts_disclosure_button")
                ) {
                    if (isSpeakingNotice) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Speaking Audible Alert on Line…")
                    } else if (hasPlayedAudibleAlert) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSynced
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Audible Notice Played (Tap to Replay)")
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play Audible TTS Disclosure Now")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = partyConsentConfirmed,
                        onCheckedChange = onToggleConsentConfirmed,
                        modifier = Modifier.testTag("consent_checkbox")
                    )
                    Text(
                        text = "I confirm all required parties have been notified and consented to recording.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmStartRecording,
                enabled = partyConsentConfirmed || !settings.requireConsentModalBeforeRecord,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RecordingCrimson,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_consent_start_record_button")
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Verify & Start Recording", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_consent_dialog_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

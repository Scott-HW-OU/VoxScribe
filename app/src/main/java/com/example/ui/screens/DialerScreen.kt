package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telephony.ActivePhoneCallInfo
import com.example.telephony.CallAudioOutputRoute
import com.example.telephony.SystemCallLogEntry
import com.example.telephony.SystemCallPhase
import com.example.ui.theme.ComplianceAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSynced
import com.example.ui.theme.RecordingCrimson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class KeypadButtonSpec(
    val digit: Char,
    val letters: String,
    val testTagSuffix: String
)

private val KEYPAD_ROWS = listOf(
    listOf(
        KeypadButtonSpec('1', "OO", "1"),
        KeypadButtonSpec('2', "ABC", "2"),
        KeypadButtonSpec('3', "DEF", "3")
    ),
    listOf(
        KeypadButtonSpec('4', "GHI", "4"),
        KeypadButtonSpec('5', "JKL", "5"),
        KeypadButtonSpec('6', "MNO", "6")
    ),
    listOf(
        KeypadButtonSpec('7', "PQRS", "7"),
        KeypadButtonSpec('8', "TUV", "8"),
        KeypadButtonSpec('9', "WXYZ", "9")
    ),
    listOf(
        KeypadButtonSpec('*', "PAUSE", "star"),
        KeypadButtonSpec('0', "+", "0"),
        KeypadButtonSpec('#', "SEND", "pound")
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialerScreen(
    phoneNumber: String,
    contactName: String,
    autoRecordOnCall: Boolean,
    activePhoneCallInfo: ActivePhoneCallInfo,
    systemCallLog: List<SystemCallLogEntry>,
    isDefaultDialer: Boolean,
    isCallScreeningEnabled: Boolean,
    hasPhonePermissions: Boolean,
    isRecording: Boolean,
    onKeypadDigitPressed: (Char) -> Unit,
    onBackspacePressed: () -> Unit,
    onClearNumberPressed: () -> Unit,
    onAppendPlusSign: () -> Unit,
    onUpdatePhoneNumber: (String) -> Unit,
    onUpdateContactName: (String) -> Unit,
    onSelectAudioOutputRoute: (CallAudioOutputRoute) -> Unit,
    onToggleAutoRecordOnCall: (Boolean) -> Unit,
    onDialOnly: () -> Unit,
    onDialAndRecord: () -> Unit,
    onAnswerIncomingCall: () -> Unit,
    onEndActiveCall: () -> Unit,
    onToggleCallHold: () -> Unit,
    onToggleCallMute: () -> Unit,
    onSelectCallLogEntry: (SystemCallLogEntry) -> Unit,
    onRequestPhoneAndBluetoothPermissions: () -> Unit,
    onRequestDefaultDialerRole: () -> Unit,
    onRequestCallScreeningRole: () -> Unit,
    onRefreshTelephonyState: () -> Unit,
    onNavigateToRecorderStudio: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dialer_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 3-Way Audio Output Route Selector Card (Bluetooth / Phone Speaker / Loudspeaker)
        item {
            AudioOutputRouteSelectorCard(
                activePhoneCallInfo = activePhoneCallInfo,
                onSelectRoute = onSelectAudioOutputRoute,
                onRefreshDevices = onRefreshTelephonyState
            )
        }

        // 2. Live Active Call Control Banner (if a phone call is currently ringing, dialing, active, or on hold)
        if (activePhoneCallInfo.phase != SystemCallPhase.IDLE &&
            activePhoneCallInfo.phase != SystemCallPhase.DISCONNECTED
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, EmeraldSynced, RoundedCornerShape(20.dp))
                        .testTag("dialer_active_call_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldSynced.copy(alpha = 0.14f)
                    ),
                    shape = RoundedCornerShape(20.dp)
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activePhoneCallInfo.contactName.ifBlank {
                                        activePhoneCallInfo.phoneNumber.ifBlank { "Active Phone Call" }
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldSynced
                                )
                                Text(
                                    text = "${activePhoneCallInfo.phoneNumber} • ${activePhoneCallInfo.phase.displayLabel}" +
                                        if (activePhoneCallInfo.lastDtmfSequence.isNotBlank()) {
                                            " • Keypad: ${activePhoneCallInfo.lastDtmfSequence}"
                                        } else {
                                            ""
                                        },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            AssistChip(
                                onClick = onNavigateToRecorderStudio,
                                label = {
                                    Text(if (isRecording) "Recording Live" else "Open Scribe")
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (isRecording) RecordingCrimson else ElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }

                        if (activePhoneCallInfo.phase == SystemCallPhase.RINGING) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onAnswerIncomingCall,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldSynced,
                                        contentColor = Color(0xFF00210B)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dialer_answer_call_button")
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Answer & Record", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onEndActiveCall,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RecordingCrimson,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dialer_decline_call_button")
                                ) {
                                    Icon(Icons.Default.CallEnd, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Decline", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = activePhoneCallInfo.isMicrophoneMuted,
                                    onClick = onToggleCallMute,
                                    label = { Text(if (activePhoneCallInfo.isMicrophoneMuted) "Muted" else "Mute") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.MicOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = activePhoneCallInfo.isOnHold,
                                    onClick = onToggleCallHold,
                                    label = { Text(if (activePhoneCallInfo.isOnHold) "On Hold" else "Hold") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Pause,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = onEndActiveCall,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RecordingCrimson,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("dialer_hangup_button")
                                ) {
                                    Icon(Icons.Default.CallEnd, contentDescription = "End Call")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("End")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Phone Number Readout & Full Interactive 12-Key Keypad Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(ElectricCyan.copy(alpha = 0.6f), EmeraldSynced.copy(alpha = 0.6f))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Dialer Header Badge
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
                                imageVector = Icons.Default.Dialpad,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Phone Dialer & DTMF Keypad",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                        }
                        Surface(
                            color = EmeraldSynced.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Audio: ${activePhoneCallInfo.audioRoute.shortLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSynced,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Dialed Number Display Box with + and Backspace/Clear
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                ElectricCyan.copy(alpha = 0.35f),
                                RoundedCornerShape(18.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = onAppendPlusSign,
                                    modifier = Modifier.testTag("dialer_plus_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Insert +",
                                        tint = ElectricCyan
                                    )
                                }

                                Text(
                                    text = phoneNumber.ifBlank { "Enter Number" },
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.5.sp
                                    ),
                                    color = if (phoneNumber.isBlank()) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    } else {
                                        EmeraldSynced
                                    },
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dialer_number_display")
                                )

                                Row {
                                    if (phoneNumber.isNotEmpty()) {
                                        IconButton(
                                            onClick = onBackspacePressed,
                                            modifier = Modifier.testTag("dialer_backspace_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Backspace",
                                                tint = ElectricCyan
                                            )
                                        }
                                        IconButton(
                                            onClick = onClearNumberPressed,
                                            modifier = Modifier.testTag("dialer_clear_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear Number",
                                                tint = RecordingCrimson
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(48.dp))
                                    }
                                }
                            }

                            // Contact Name Input / Lookup Preview
                            OutlinedTextField(
                                value = contactName,
                                onValueChange = onUpdateContactName,
                                label = { Text("Caller / Contact Label (Auto-resolves from Contacts)") },
                                placeholder = { Text("Optional contact name for AI transcript") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dialer_contact_name_input")
                            )
                        }
                    }

                    // 12-Key Telephone Keypad Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KEYPAD_ROWS.forEach { rowSpecs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowSpecs.forEach { spec ->
                                    DialerKeypadKey(
                                        digit = spec.digit,
                                        letters = spec.letters,
                                        onClick = { onKeypadDigitPressed(spec.digit) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("dialer_key_${spec.testTagSuffix}")
                                    )
                                }
                            }
                        }
                    }

                    // Primary Call & Auto-Record Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDialOnly,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("dialer_call_only_button")
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Dial Call",
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onDialAndRecord,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldSynced,
                                contentColor = Color(0xFF00210B)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1.35f)
                                .height(54.dp)
                                .testTag("dialer_call_and_record_button")
                        ) {
                            Icon(
                                Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Call & Auto-Record",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Auto-Record Toggle Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Record & Transcribe When Connected",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Plays privacy compliance alert & starts live Gemini AI transcription on call connect",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoRecordOnCall,
                            onCheckedChange = onToggleAutoRecordOnCall,
                            modifier = Modifier.testTag("dialer_auto_record_switch")
                        )
                    }
                }
            }
        }

        // 4. Recent Device Call Log & System Role Bindings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = ElectricCyan
                            )
                            Text(
                                text = "Recent Calls & Phone System Roles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onRefreshTelephonyState) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Call Log")
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = onRequestPhoneAndBluetoothPermissions,
                            label = {
                                Text(
                                    text = if (hasPhonePermissions) {
                                        "Phone & Bluetooth Ready"
                                    } else {
                                        "Grant Phone & Bluetooth Permissions"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (hasPhonePermissions) Icons.Default.Check else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (hasPhonePermissions) EmeraldSynced else ComplianceAmber,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        )

                        AssistChip(
                            onClick = onRequestDefaultDialerRole,
                            label = {
                                Text(
                                    text = if (isDefaultDialer || activePhoneCallInfo.isInCallServiceBound) {
                                        "Default Phone Dialer Active"
                                    } else {
                                        "Set as Default Phone Dialer"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PhoneInTalk,
                                    contentDescription = null,
                                    tint = if (isDefaultDialer || activePhoneCallInfo.isInCallServiceBound) EmeraldSynced else ElectricCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        )

                        AssistChip(
                            onClick = onRequestCallScreeningRole,
                            label = {
                                Text(
                                    text = if (isCallScreeningEnabled) {
                                        "Caller ID Screening Active"
                                    } else {
                                        "Enable Caller ID Screening"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isCallScreeningEnabled) EmeraldSynced else ElectricCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        )
                    }

                    if (systemCallLog.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        Text(
                            text = "Tap a recent call to load into the keypad:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val timeFmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            systemCallLog.forEach { entry ->
                                AssistChip(
                                    onClick = { onSelectCallLogEntry(entry) },
                                    label = {
                                        Text(
                                            text = "${entry.cachedName} (${entry.phoneNumber}) • ${timeFmt.format(Date(entry.timestamp))}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = {
                                        val icon = when (entry.callType) {
                                            "OUTGOING" -> Icons.AutoMirrored.Filled.CallMade
                                            "MISSED" -> Icons.AutoMirrored.Filled.CallMissed
                                            else -> Icons.AutoMirrored.Filled.CallReceived
                                        }
                                        Icon(
                                            icon,
                                            contentDescription = entry.callType,
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialerKeypadKey(
    digit: Char,
    letters: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .height(66.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = ElectricCyan.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color(0xFFF6EEFF)
            )
            Text(
                text = letters,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = EmeraldSynced
            )
        }
    }
}

/**
 * Reusable 3-Way Call Audio Output Switcher Card (`Bluetooth` / `Phone Speaker` / `Loudspeaker`)
 * displayed on both the dedicated Dialer Screen and the Call Recorder Screen.
 */
@Composable
fun AudioOutputRouteSelectorCard(
    activePhoneCallInfo: ActivePhoneCallInfo,
    onSelectRoute: (CallAudioOutputRoute) -> Unit,
    onRefreshDevices: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_output_route_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val activeIcon = when (activePhoneCallInfo.audioRoute) {
                        CallAudioOutputRoute.BLUETOOTH -> Icons.Default.BluetoothAudio
                        CallAudioOutputRoute.EARPIECE -> Icons.Default.PhoneInTalk
                        CallAudioOutputRoute.LOUDSPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = activeIcon,
                            contentDescription = null,
                            tint = EmeraldSynced,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Call Audio Output Speaker",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        val subtitle = when (activePhoneCallInfo.audioRoute) {
                            CallAudioOutputRoute.BLUETOOTH -> {
                                if (activePhoneCallInfo.connectedBluetoothName.isNotBlank()) {
                                    "Bluetooth Connected: ${activePhoneCallInfo.connectedBluetoothName}"
                                } else {
                                    "Routed to Bluetooth Speaker / Headset (SCO)"
                                }
                            }
                            CallAudioOutputRoute.EARPIECE ->
                                "Routed to Built-in Phone Speaker (Handset Earpiece)"
                            CallAudioOutputRoute.LOUDSPEAKER ->
                                "Routed to Loudspeaker (Best for 2-Way Call Recording)"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldSynced
                        )
                    }
                }
                IconButton(
                    onClick = onRefreshDevices,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Scan Audio Devices",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CallAudioOutputRoute.entries.forEach { route ->
                    val selected = activePhoneCallInfo.audioRoute == route
                    val icon = when (route) {
                        CallAudioOutputRoute.BLUETOOTH -> Icons.Default.BluetoothAudio
                        CallAudioOutputRoute.EARPIECE -> Icons.Default.PhoneInTalk
                        CallAudioOutputRoute.LOUDSPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectRoute(route) },
                        label = {
                            Text(
                                text = route.shortLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = route.label,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSynced.copy(alpha = 0.22f),
                            selectedLabelColor = EmeraldSynced,
                            selectedLeadingIconColor = EmeraldSynced
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("audio_route_chip_${route.id.lowercase()}")
                    )
                }
            }
        }
    }
}

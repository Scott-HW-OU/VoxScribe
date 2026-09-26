package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AdvancedExportModalSheet
import com.example.ui.components.ConsentVerificationDialog
import com.example.ui.components.GoogleAccountModalSheet
import com.example.ui.screens.CallDetailScreen
import com.example.ui.screens.CallFeedScreen
import com.example.ui.screens.CallRecorderScreen
import com.example.ui.screens.ComplianceAndSettingsScreen
import com.example.ui.screens.DialerScreen
import com.example.ui.screens.TagsAndCloudScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSynced

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoxScribeApp(
    initialDialNumber: String? = null,
    openRecorderInitially: Boolean = false,
    viewModel: VoxScribeViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val allRecordings by viewModel.allRecordings.collectAsStateWithLifecycle()
    val filteredRecordings by viewModel.filteredRecordings.collectAsStateWithLifecycle()
    val selectedRecording by viewModel.selectedRecording.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsStateWithLifecycle()
    val onlyStarredFilter by viewModel.onlyStarredFilter.collectAsStateWithLifecycle()
    val tagFrequencies by viewModel.tagFrequencyList.collectAsStateWithLifecycle()
    val recorderForm by viewModel.recorderForm.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val exportConfig by viewModel.exportConfig.collectAsStateWithLifecycle()

    val activePhoneCallInfo by viewModel.activePhoneCallInfo.collectAsStateWithLifecycle()
    val systemCallLog by viewModel.systemCallLog.collectAsStateWithLifecycle()
    val isDefaultDialer by viewModel.isDefaultDialer.collectAsStateWithLifecycle()
    val isCallScreeningEnabled by viewModel.isCallScreeningEnabled.collectAsStateWithLifecycle()

    val googleAccount by viewModel.googleAccount.collectAsStateWithLifecycle()
    val showGoogleAccountSheet by viewModel.showGoogleAccountSheet.collectAsStateWithLifecycle()
    val isGoogleSigningIn by viewModel.isGoogleSigningIn.collectAsStateWithLifecycle()
    val googleAuthHintMessage by viewModel.googleAuthHintMessage.collectAsStateWithLifecycle()
    val pendingOAuthIntentSender by viewModel.pendingOAuthIntentSender.collectAsStateWithLifecycle()

    val isRecording by viewModel.audioRecorder.isRecording.collectAsStateWithLifecycle()
    val isPaused by viewModel.audioRecorder.isPaused.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.audioRecorder.elapsedSeconds.collectAsStateWithLifecycle()
    val waveformAmplitudes by viewModel.audioRecorder.waveformAmplitudes.collectAsStateWithLifecycle()
    val activeAudioSourceLabel by viewModel.audioRecorder.activeAudioSourceLabel.collectAsStateWithLifecycle()

    val isPlayingAudio by viewModel.audioPlayer.isPlaying.collectAsStateWithLifecycle()
    val audioPositionMs by viewModel.audioPlayer.currentPositionMs.collectAsStateWithLifecycle()
    val audioDurationMs by viewModel.audioPlayer.durationMs.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.audioPlayer.playbackSpeed.collectAsStateWithLifecycle()

    val isSpeakingNotice by viewModel.ttsAnnouncer.isSpeakingNotice.collectAsStateWithLifecycle()
    val partialSpeechText by viewModel.liveSpeechTranscriber.partialSpeech.collectAsStateWithLifecycle()

    val isRegeneratingSummary by viewModel.isRegeneratingSummary.collectAsStateWithLifecycle()
    val isAskingQuestion by viewModel.isAskingCallQuestion.collectAsStateWithLifecycle()
    val callQuestionAnswer by viewModel.callQuestionAnswer.collectAsStateWithLifecycle()
    val isSyncingCloud by viewModel.isSyncingCloud.collectAsStateWithLifecycle()

    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val pendingShareIntent by viewModel.pendingShareIntent.collectAsStateWithLifecycle()

    LaunchedEffect(initialDialNumber, openRecorderInitially) {
        if (!initialDialNumber.isNullOrBlank()) {
            viewModel.updatePhoneNumber(initialDialNumber)
            viewModel.selectTab(AppTab.DIALER)
        } else if (openRecorderInitially) {
            viewModel.selectTab(AppTab.RECORD)
        }
    }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasPhonePermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        viewModel.requestStartCallRecording()
    }

    val phoneAndMicPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasMicPermission = grants[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        hasPhonePermissions = grants[Manifest.permission.READ_PHONE_STATE] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        viewModel.refreshTelephonyBindingsAndCallLog()
    }

    val requestAllTelephonyAndAudioPermissions: () -> Unit = {
        val perms = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                add(Manifest.permission.ANSWER_PHONE_CALLS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.toTypedArray()
        phoneAndMicPermissionsLauncher.launch(perms)
    }

    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshTelephonyBindingsAndCallLog()
    }

    val oauthConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.handleGoogleOAuthConsentResult(result.data)
    }

    LaunchedEffect(pendingOAuthIntentSender) {
        val sender = pendingOAuthIntentSender
        if (sender != null) {
            runCatching {
                oauthConsentLauncher.launch(IntentSenderRequest.Builder(sender).build())
            }.onFailure {
                viewModel.consumePendingOAuthIntentSender()
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    LaunchedEffect(pendingShareIntent) {
        val intent = pendingShareIntent
        if (intent != null) {
            runCatching { context.startActivity(intent) }
            viewModel.consumePendingShareIntent()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = EmeraldSynced,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "VoxScribe",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = ElectricCyan
                            )
                        }
                    },
                    actions = {
                        // Personal Google Account Sign-In Pill in TopAppBar
                        Surface(
                            color = if (googleAccount.isSignedIn) {
                                EmeraldSynced.copy(alpha = 0.18f)
                            } else {
                                ElectricCyan.copy(alpha = 0.18f)
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { viewModel.setShowGoogleAccountSheet(true) }
                                .testTag("top_bar_google_account_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (googleAccount.isSignedIn) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldSynced),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = googleAccount.displayName.firstOrNull()?.uppercase() ?: "G",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00210B)
                                        )
                                    }
                                    Text(
                                        text = googleAccount.email.substringBefore("@"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EmeraldSynced,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Sign in with Google",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Sign In",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini AI",
                                    tint = if (googleAccount.isSignedIn) EmeraldSynced else ElectricCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                if (selectedRecording == null && currentTab == AppTab.CALLS) {
                    FloatingActionButton(
                        onClick = { viewModel.selectTab(AppTab.DIALER) },
                        containerColor = EmeraldSynced,
                        contentColor = Color(0xFF00210B),
                        modifier = Modifier.testTag("fab_open_dialer")
                    ) {
                        Icon(Icons.Default.Dialpad, contentDescription = "Open Phone Dialer")
                    }
                }
            },
            bottomBar = {
                if (!isExpandedScreen && selectedRecording == null) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.CALLS,
                            onClick = { viewModel.selectTab(AppTab.CALLS) },
                            icon = { Icon(Icons.Default.PhoneInTalk, contentDescription = "Calls") },
                            label = { Text("Calls") },
                            modifier = Modifier.testTag("nav_tab_calls")
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.DIALER,
                            onClick = { viewModel.selectTab(AppTab.DIALER) },
                            icon = { Icon(Icons.Default.Dialpad, contentDescription = "Dialer") },
                            label = { Text("Dialer") },
                            modifier = Modifier.testTag("nav_tab_dialer")
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.RECORD,
                            onClick = { viewModel.selectTab(AppTab.RECORD) },
                            icon = { Icon(Icons.Default.Mic, contentDescription = "Record") },
                            label = { Text("Record") },
                            modifier = Modifier.testTag("nav_tab_record")
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.TAGS_CLOUD,
                            onClick = { viewModel.selectTab(AppTab.TAGS_CLOUD) },
                            icon = { Icon(Icons.Default.CloudSync, contentDescription = "Cloud") },
                            label = { Text("Cloud") },
                            modifier = Modifier.testTag("nav_tab_tags_cloud")
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.COMPLIANCE,
                            onClick = { viewModel.selectTab(AppTab.COMPLIANCE) },
                            icon = { Icon(Icons.Default.Gavel, contentDescription = "Compliance") },
                            label = { Text("Compliance") },
                            modifier = Modifier.testTag("nav_tab_compliance")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NavigationRailItem(
                            selected = currentTab == AppTab.CALLS,
                            onClick = {
                                viewModel.closeCallDetail()
                                viewModel.selectTab(AppTab.CALLS)
                            },
                            icon = { Icon(Icons.Default.PhoneInTalk, contentDescription = "Calls") },
                            label = { Text("Calls") }
                        )
                        NavigationRailItem(
                            selected = currentTab == AppTab.DIALER,
                            onClick = {
                                viewModel.closeCallDetail()
                                viewModel.selectTab(AppTab.DIALER)
                            },
                            icon = { Icon(Icons.Default.Dialpad, contentDescription = "Dialer") },
                            label = { Text("Dialer") }
                        )
                        NavigationRailItem(
                            selected = currentTab == AppTab.RECORD,
                            onClick = {
                                viewModel.closeCallDetail()
                                viewModel.selectTab(AppTab.RECORD)
                            },
                            icon = { Icon(Icons.Default.Mic, contentDescription = "Record") },
                            label = { Text("Record") }
                        )
                        NavigationRailItem(
                            selected = currentTab == AppTab.TAGS_CLOUD,
                            onClick = {
                                viewModel.closeCallDetail()
                                viewModel.selectTab(AppTab.TAGS_CLOUD)
                            },
                            icon = { Icon(Icons.Default.CloudSync, contentDescription = "Tags & Cloud") },
                            label = { Text("Cloud") }
                        )
                        NavigationRailItem(
                            selected = currentTab == AppTab.COMPLIANCE,
                            onClick = {
                                viewModel.closeCallDetail()
                                viewModel.selectTab(AppTab.COMPLIANCE)
                            },
                            icon = { Icon(Icons.Default.Gavel, contentDescription = "Compliance") },
                            label = { Text("Compliance") }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    val activeDetail = selectedRecording
                    if (activeDetail != null) {
                        CallDetailScreen(
                            recording = activeDetail,
                            googleAccount = googleAccount,
                            isPlayingAudio = isPlayingAudio,
                            audioPositionMs = audioPositionMs,
                            audioDurationMs = audioDurationMs,
                            playbackSpeed = playbackSpeed,
                            isRegeneratingSummary = isRegeneratingSummary,
                            isAskingQuestion = isAskingQuestion,
                            callQuestionAnswer = callQuestionAnswer,
                            isSyncingCloud = isSyncingCloud,
                            onBack = { viewModel.closeCallDetail() },
                            onTogglePlayPause = {
                                viewModel.audioPlayer.togglePlayPause(activeDetail.audioFilePath)
                            },
                            onSeekAudio = { viewModel.audioPlayer.seekToFraction(it) },
                            onCyclePlaybackSpeed = { viewModel.audioPlayer.cyclePlaybackSpeed() },
                            onRegenerateSummary = { style, len, focus ->
                                viewModel.regenerateSummaryWithCustomConfig(activeDetail, style, len, focus)
                            },
                            onAskQuestion = { q -> viewModel.askQuestionAboutCall(activeDetail, q) },
                            onToggleActionItem = { idx -> viewModel.toggleActionItem(activeDetail, idx) },
                            onAddCustomTag = { tag -> viewModel.addCustomTag(activeDetail, tag) },
                            onRemoveTag = { tag -> viewModel.removeTag(activeDetail, tag) },
                            onQuickExport = { app -> viewModel.quickExportToTarget(activeDetail, app) },
                            onOpenAdvancedExportSheet = { target, fmt ->
                                viewModel.openExportConfigurator(target, fmt)
                            },
                            onSyncToCloud = { viewModel.syncSingleCallToCloud(activeDetail) },
                            onShareCloudVaultJson = { viewModel.shareCloudVaultArchiveFile(activeDetail) },
                            onDeleteCall = { viewModel.deleteRecording(activeDetail) }
                        )
                    } else {
                        when (currentTab) {
                            AppTab.CALLS -> CallFeedScreen(
                                recordings = filteredRecordings,
                                allRecordingsCount = allRecordings.size,
                                searchQuery = searchQuery,
                                selectedTagFilter = selectedTagFilter,
                                onlyStarredFilter = onlyStarredFilter,
                                topTags = tagFrequencies,
                                googleAccount = googleAccount,
                                onSearchChange = viewModel::updateSearchQuery,
                                onSelectTagFilter = viewModel::selectTagFilter,
                                onToggleStarredFilter = viewModel::toggleStarredFilter,
                                onOpenCallDetail = viewModel::openCallDetail,
                                onToggleStarCall = viewModel::toggleStarred,
                                onNavigateToDialer = { viewModel.selectTab(AppTab.DIALER) },
                                onNavigateToRecorder = { viewModel.selectTab(AppTab.RECORD) },
                                onOpenGoogleAccountSheet = { viewModel.setShowGoogleAccountSheet(true) }
                            )

                            AppTab.DIALER -> DialerScreen(
                                phoneNumber = recorderForm.phoneNumber,
                                contactName = recorderForm.contactName,
                                autoRecordOnCall = recorderForm.autoRecordOnPhoneCallActive,
                                activePhoneCallInfo = activePhoneCallInfo,
                                systemCallLog = systemCallLog,
                                isDefaultDialer = isDefaultDialer,
                                isCallScreeningEnabled = isCallScreeningEnabled,
                                hasPhonePermissions = hasPhonePermissions,
                                isRecording = isRecording,
                                onKeypadDigitPressed = viewModel::pressKeypadDigit,
                                onBackspacePressed = viewModel::backspaceDialerNumber,
                                onClearNumberPressed = viewModel::clearDialerNumber,
                                onAppendPlusSign = viewModel::appendDialerPlusSign,
                                onUpdatePhoneNumber = viewModel::updatePhoneNumber,
                                onUpdateContactName = viewModel::updateContactName,
                                onSelectAudioOutputRoute = viewModel::selectCallAudioOutputRoute,
                                onToggleAutoRecordOnCall = viewModel::toggleAutoRecordOnPhoneCall,
                                onDialOnly = {
                                    viewModel.dialAndRecordPhoneCall(
                                        recorderForm.phoneNumber,
                                        recorderForm.contactName
                                    )
                                },
                                onDialAndRecord = viewModel::dialAndAutoRecordFromDialer,
                                onAnswerIncomingCall = viewModel::answerIncomingSystemCall,
                                onEndActiveCall = viewModel::endActiveSystemCall,
                                onToggleCallHold = viewModel::toggleCallHold,
                                onToggleCallMute = viewModel::toggleCallMute,
                                onSelectCallLogEntry = viewModel::selectSystemCallLogEntry,
                                onRequestPhoneAndBluetoothPermissions = requestAllTelephonyAndAudioPermissions,
                                onRequestDefaultDialerRole = {
                                    val intent = viewModel.getRequestDefaultDialerIntent()
                                    if (intent != null) {
                                        runCatching { roleRequestLauncher.launch(intent) }
                                    }
                                },
                                onRequestCallScreeningRole = {
                                    val intent = viewModel.getRequestCallScreeningIntent()
                                    if (intent != null) {
                                        runCatching { roleRequestLauncher.launch(intent) }
                                    }
                                },
                                onRefreshTelephonyState = viewModel::refreshTelephonyBindingsAndCallLog,
                                onNavigateToRecorderStudio = { viewModel.selectTab(AppTab.RECORD) }
                            )

                            AppTab.RECORD -> CallRecorderScreen(
                                formState = recorderForm,
                                settings = settings,
                                googleAccount = googleAccount,
                                activePhoneCallInfo = activePhoneCallInfo,
                                systemCallLog = systemCallLog,
                                isDefaultDialer = isDefaultDialer,
                                isCallScreeningEnabled = isCallScreeningEnabled,
                                hasPhonePermissions = hasPhonePermissions,
                                hasMicPermission = hasMicPermission,
                                isRecording = isRecording,
                                isPaused = isPaused,
                                elapsedSeconds = elapsedSeconds,
                                waveformAmplitudes = waveformAmplitudes,
                                activeAudioSourceLabel = activeAudioSourceLabel,
                                isSpeakingNotice = isSpeakingNotice,
                                partialSpeechText = partialSpeechText,
                                onRequestPhoneAndMicPermissions = requestAllTelephonyAndAudioPermissions,
                                onRequestDefaultDialerRole = {
                                    val intent = viewModel.getRequestDefaultDialerIntent()
                                    if (intent != null) {
                                        runCatching { roleRequestLauncher.launch(intent) }
                                    }
                                },
                                onRequestCallScreeningRole = {
                                    val intent = viewModel.getRequestCallScreeningIntent()
                                    if (intent != null) {
                                        runCatching { roleRequestLauncher.launch(intent) }
                                    }
                                },
                                onRefreshTelephonyState = viewModel::refreshTelephonyBindingsAndCallLog,
                                onDialAndRecordCall = viewModel::dialAndRecordPhoneCall,
                                onAnswerIncomingCall = viewModel::answerIncomingSystemCall,
                                onEndActiveCall = viewModel::endActiveSystemCall,
                                onToggleCallHold = viewModel::toggleCallHold,
                                onToggleCallSpeakerphone = viewModel::toggleCallSpeakerphone,
                                onToggleCallMute = viewModel::toggleCallMute,
                                onToggleAutoRecordOnCall = viewModel::toggleAutoRecordOnPhoneCall,
                                onSelectCallLogEntry = viewModel::selectSystemCallLogEntry,
                                onRequestMicPermissionAndRecord = {
                                    if (hasMicPermission) {
                                        viewModel.requestStartCallRecording()
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                onUpdateContactName = viewModel::updateContactName,
                                onUpdatePhoneNumber = viewModel::updatePhoneNumber,
                                onUpdateCallDirection = viewModel::updateCallDirection,
                                onUpdateSummaryStyle = viewModel::updateRecorderSummaryStyle,
                                onUpdateSummaryLength = viewModel::updateRecorderSummaryLength,
                                onToggleFocusAspect = viewModel::toggleRecorderFocusAspect,
                                onUpdateLiveNotes = viewModel::updateLiveTranscriptNotes,
                                onToggleLiveSpeechRecognition = viewModel::toggleLiveSpeechRecognition,
                                onRefreshLiveAiInsight = viewModel::triggerLiveInCallAiUpdate,
                                onPlayAudibleComplianceNotice = { viewModel.playComplianceNoticeNow() },
                                onPauseResumeRecording = {
                                    if (isPaused) {
                                        viewModel.audioRecorder.resumeRecording()
                                    } else {
                                        viewModel.audioRecorder.pauseRecording()
                                    }
                                },
                                onStopAndTranscribe = viewModel::stopRecordingAndTranscribe,
                                onSelectAudioOutputRoute = viewModel::selectCallAudioOutputRoute,
                                onOpenDialerScreen = { viewModel.selectTab(AppTab.DIALER) },
                                onOpenGoogleAccountSheet = { viewModel.setShowGoogleAccountSheet(true) }
                            )

                            AppTab.TAGS_CLOUD -> TagsAndCloudScreen(
                                recordings = allRecordings,
                                tagFrequencies = tagFrequencies,
                                selectedTagFilter = selectedTagFilter,
                                settings = settings,
                                isSyncingCloud = isSyncingCloud,
                                onSelectTag = viewModel::selectTagFilter,
                                onOpenCallDetail = viewModel::openCallDetail,
                                onSyncAllToCloud = viewModel::syncAllPendingToCloud,
                                onSyncSingleCall = viewModel::syncSingleCallToCloud,
                                onShareCloudArchive = viewModel::shareCloudVaultArchiveFile,
                                onToggleAutoCloudSync = viewModel::updateAutoCloudSync,
                                onUpdateCloudConfig = viewModel::updateCloudConfig
                            )

                            AppTab.COMPLIANCE -> ComplianceAndSettingsScreen(
                                settings = settings,
                                googleAccount = googleAccount,
                                isSpeakingNotice = isSpeakingNotice,
                                onUpdateJurisdiction = viewModel::updateJurisdiction,
                                onUpdateRequireModal = viewModel::updateRequireConsentModal,
                                onUpdateAutoPlayTts = viewModel::updateAutoPlayTts,
                                onUpdateSpeakerphoneRoute = viewModel::updateRouteToSpeakerphone,
                                onSaveCustomScript = viewModel::updateCustomScript,
                                onTestPlayTtsScript = { viewModel.playComplianceNoticeNow() },
                                onSaveExportTargets = viewModel::updateExportTargets,
                                onOpenGoogleAccountSheet = { viewModel.setShowGoogleAccountSheet(true) }
                            )
                        }
                    }
                }
            }
        }

        // Pre-Call Privacy & Compliance Consent Alert Dialog
        if (recorderForm.showConsentDialog) {
            ConsentVerificationDialog(
                settings = settings,
                contactName = recorderForm.contactName.ifBlank {
                    recorderForm.phoneNumber.ifBlank { "Call Participant" }
                },
                isSpeakingNotice = isSpeakingNotice,
                hasPlayedAudibleAlert = recorderForm.hasPlayedAudibleAlertForSession,
                partyConsentConfirmed = recorderForm.partyConsentConfirmed,
                onPlayAudibleNotice = { viewModel.playComplianceNoticeNow() },
                onToggleConsentConfirmed = viewModel::setPartyConsentConfirmed,
                onConfirmStartRecording = viewModel::confirmConsentAndStartRecording,
                onDismiss = viewModel::dismissConsentDialog
            )
        }

        // Personal Google Account Sign-In & Associated Gemini AI Modal Sheet
        if (showGoogleAccountSheet) {
            GoogleAccountModalSheet(
                account = googleAccount,
                isSigningIn = isGoogleSigningIn,
                authHintMessage = googleAuthHintMessage,
                onDismiss = { viewModel.setShowGoogleAccountSheet(false) },
                onSignInWithCredentialManager = {
                    viewModel.signInWithGoogleCredentialManager(context)
                },
                onSignInWithLinkedAccount = { email, name, token, projectId, key ->
                    viewModel.signInWithLinkedGoogleAccount(email, name, token, projectId, key)
                },
                onToggleUseAccountForGemini = viewModel::toggleUseGoogleAccountForGemini,
                onOpenDeviceAccountSettings = viewModel::openSystemAddGoogleAccountSettings,
                onSignOut = viewModel::signOutGoogleAccount
            )
        }

        // Advanced Export Builder Modal Sheet
        val currentDetailForExport = selectedRecording
        if (exportConfig.isVisible && currentDetailForExport != null) {
            val previewText = remember(
                currentDetailForExport,
                exportConfig.selectedFormat,
                exportConfig.fields
            ) {
                viewModel.getExportPreviewText(currentDetailForExport)
            }
            AdvancedExportModalSheet(
                state = exportConfig,
                previewText = previewText,
                onDismiss = viewModel::closeExportConfigurator,
                onSelectTargetApp = viewModel::updateExportTargetApp,
                onSelectFormat = viewModel::updateExportFormat,
                onUpdateFields = viewModel::updateExportFields,
                onExecuteExport = { viewModel.executeConfiguredExport(currentDetailForExport) }
            )
        }
    }
}

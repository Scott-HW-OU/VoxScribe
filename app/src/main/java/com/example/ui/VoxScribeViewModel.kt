package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerController
import com.example.audio.CallAudioRecorder
import com.example.audio.ComplianceTtsAnnouncer
import com.example.audio.LiveSpeechTranscriber
import com.example.auth.GoogleAccountProfile
import com.example.auth.GoogleAccountSessionManager
import com.example.auth.GoogleSignInOutcome
import com.example.data.CallRecordingRepository
import com.example.data.local.CallRecordingEntity
import com.example.data.local.ComplianceAndCloudSettings
import com.example.data.local.CompliancePreferencesRepository
import com.example.data.local.LiveInCallInsight
import com.example.data.local.SummaryFocusAspect
import com.example.data.local.SummaryLengthOption
import com.example.data.local.VoxScribeDatabase
import com.example.data.remote.CloudStorageManager
import com.example.data.remote.ExportFieldSelection
import com.example.data.remote.ExportFormatOption
import com.example.data.remote.ExportResult
import com.example.data.remote.ExportTargetApp
import com.example.data.remote.GeminiCallAiService
import com.example.data.remote.ProductivityExportManager
import com.example.telephony.ActivePhoneCallInfo
import com.example.telephony.CallAudioOutputRoute
import com.example.telephony.PhoneCallStateBus
import com.example.telephony.SystemCallLogEntry
import com.example.telephony.SystemCallPhase
import com.example.telephony.TelephonyCallMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    CALLS,
    DIALER,
    RECORD,
    TAGS_CLOUD,
    COMPLIANCE
}

data class RecorderFormState(
    val contactName: String = "",
    val phoneNumber: String = "",
    val callDirection: String = "OUTGOING",
    val liveTranscriptAndNotes: String = "",
    val summaryStyle: String = "Executive Brief",
    val summaryLength: SummaryLengthOption = SummaryLengthOption.MEDIUM,
    val selectedFocusAspects: Set<String> = setOf(
        SummaryFocusAspect.ACTION_ITEMS.id,
        SummaryFocusAspect.DECISIONS_MADE.id,
        SummaryFocusAspect.IMPORTANT_DATES.id
    ),
    val autoRecordOnPhoneCallActive: Boolean = true,
    val showConsentDialog: Boolean = false,
    val hasPlayedAudibleAlertForSession: Boolean = false,
    val partyConsentConfirmed: Boolean = false,
    val lastConsentTimestamp: Long = 0L,
    val isAnalyzingWithAi: Boolean = false,
    val isUpdatingLiveInsight: Boolean = false,
    val liveSpeechRecognitionEnabled: Boolean = false,
    val liveInCallInsight: LiveInCallInsight = LiveInCallInsight()
)

data class ExportConfiguratorState(
    val isVisible: Boolean = false,
    val selectedFormat: ExportFormatOption = ExportFormatOption.MARKDOWN,
    val selectedTargetApp: ExportTargetApp = ExportTargetApp.NOTION,
    val fields: ExportFieldSelection = ExportFieldSelection()
)

class VoxScribeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = VoxScribeDatabase.getInstance(application)
    private val geminiService = GeminiCallAiService()
    private val cloudStorageManager = CloudStorageManager(application)
    private val exportManager = ProductivityExportManager(application)
    private val repository = CallRecordingRepository(
        dao = database.callRecordingDao(),
        geminiService = geminiService,
        cloudStorageManager = cloudStorageManager,
        exportManager = exportManager
    )
    private val prefsRepository = CompliancePreferencesRepository(application)
    private val googleAuthManager = GoogleAccountSessionManager(application)

    val audioRecorder = CallAudioRecorder(application, viewModelScope)
    val audioPlayer = AudioPlayerController(viewModelScope)
    val ttsAnnouncer = ComplianceTtsAnnouncer(application, viewModelScope)
    val liveSpeechTranscriber = LiveSpeechTranscriber(application) { recognizedSentence ->
        appendSpokenMicLineToLiveTranscript(recognizedSentence)
    }

    val telephonyMonitor = TelephonyCallMonitor(application) { newPhase, phone, contact, direction ->
        handleSystemTelephonyTransition(newPhase, phone, contact, direction)
    }

    private var liveAnalysisDebounceJob: Job? = null
    private var wasRecordingTriggeredByPhoneCall = false

    private val _currentTab = MutableStateFlow(AppTab.CALLS)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _selectedCallId = MutableStateFlow<Long?>(null)
    val selectedCallId: StateFlow<Long?> = _selectedCallId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    private val _onlyStarredFilter = MutableStateFlow(false)
    val onlyStarredFilter: StateFlow<Boolean> = _onlyStarredFilter.asStateFlow()

    private val _recorderForm = MutableStateFlow(RecorderFormState())
    val recorderForm: StateFlow<RecorderFormState> = _recorderForm.asStateFlow()

    private val _exportConfig = MutableStateFlow(ExportConfiguratorState())
    val exportConfig: StateFlow<ExportConfiguratorState> = _exportConfig.asStateFlow()

    private val _systemCallLog = MutableStateFlow<List<SystemCallLogEntry>>(emptyList())
    val systemCallLog: StateFlow<List<SystemCallLogEntry>> = _systemCallLog.asStateFlow()

    private val _isDefaultDialer = MutableStateFlow(false)
    val isDefaultDialer: StateFlow<Boolean> = _isDefaultDialer.asStateFlow()

    private val _isCallScreeningEnabled = MutableStateFlow(false)
    val isCallScreeningEnabled: StateFlow<Boolean> = _isCallScreeningEnabled.asStateFlow()

    private val _showGoogleAccountSheet = MutableStateFlow(false)
    val showGoogleAccountSheet: StateFlow<Boolean> = _showGoogleAccountSheet.asStateFlow()

    private val _isGoogleSigningIn = MutableStateFlow(false)
    val isGoogleSigningIn: StateFlow<Boolean> = _isGoogleSigningIn.asStateFlow()

    private val _googleAuthHintMessage = MutableStateFlow<String?>(null)
    val googleAuthHintMessage: StateFlow<String?> = _googleAuthHintMessage.asStateFlow()

    private val _pendingOAuthIntentSender = MutableStateFlow<IntentSender?>(null)
    val pendingOAuthIntentSender: StateFlow<IntentSender?> = _pendingOAuthIntentSender.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _pendingShareIntent = MutableStateFlow<Intent?>(null)
    val pendingShareIntent: StateFlow<Intent?> = _pendingShareIntent.asStateFlow()

    private val _isRegeneratingSummary = MutableStateFlow(false)
    val isRegeneratingSummary: StateFlow<Boolean> = _isRegeneratingSummary.asStateFlow()

    private val _callQuestionAnswer = MutableStateFlow<String?>(null)
    val callQuestionAnswer: StateFlow<String?> = _callQuestionAnswer.asStateFlow()

    private val _isAskingCallQuestion = MutableStateFlow(false)
    val isAskingCallQuestion: StateFlow<Boolean> = _isAskingCallQuestion.asStateFlow()

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    val activePhoneCallInfo: StateFlow<ActivePhoneCallInfo> = PhoneCallStateBus.callInfo

    val googleAccount: StateFlow<GoogleAccountProfile> = googleAuthManager.accountFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoogleAccountProfile()
    )

    val settings: StateFlow<ComplianceAndCloudSettings> = prefsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ComplianceAndCloudSettings()
    )

    val allRecordings: StateFlow<List<CallRecordingEntity>> = repository.allRecordings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredRecordings: StateFlow<List<CallRecordingEntity>> = combine(
        allRecordings,
        _searchQuery,
        _selectedTagFilter,
        _onlyStarredFilter
    ) { list, query, tag, starredOnly ->
        list.filter { item ->
            val matchesStar = !starredOnly || item.isStarred
            val matchesTag = tag.isNullOrBlank() || item.keywordTagsList().any {
                it.equals(tag, ignoreCase = true)
            }
            val q = query.trim()
            val matchesQuery = q.isEmpty() ||
                item.contactName.contains(q, ignoreCase = true) ||
                item.phoneNumber.contains(q, ignoreCase = true) ||
                item.executiveSummary.contains(q, ignoreCase = true) ||
                item.transcript.contains(q, ignoreCase = true) ||
                item.keywordTagsList().any { it.contains(q.removePrefix("#"), ignoreCase = true) }
            matchesStar && matchesTag && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val selectedRecording: StateFlow<CallRecordingEntity?> = combine(
        allRecordings,
        _selectedCallId
    ) { list, id ->
        list.find { it.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val tagFrequencyList: StateFlow<List<Pair<String, Int>>> = allRecordings.combine(_searchQuery) { list, _ ->
        val counts = mutableMapOf<String, Int>()
        list.forEach { rec ->
            rec.keywordTagsList().forEach { tag ->
                counts[tag] = (counts[tag] ?: 0) + 1
            }
        }
        counts.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // 1. Delete any legacy example call recordings ("Elena Vance" etc.) on startup
        viewModelScope.launch {
            repository.purgeExampleRecordings()
        }

        // 2. Start real Android TelephonyManager & InCallService state monitoring
        refreshTelephonyBindingsAndCallLog()

        // 3. Observe InCallService / CallScreeningService state changes from PhoneCallStateBus
        viewModelScope.launch {
            PhoneCallStateBus.callInfo.collect { callInfo ->
                if (callInfo.phoneNumber.isNotBlank() || callInfo.contactName.isNotBlank()) {
                    _recorderForm.update { current ->
                        current.copy(
                            phoneNumber = callInfo.phoneNumber.ifBlank { current.phoneNumber },
                            contactName = callInfo.contactName.ifBlank { current.contactName },
                            callDirection = callInfo.callDirection
                        )
                    }
                }

                if (callInfo.phase == SystemCallPhase.ACTIVE_IN_CALL &&
                    _recorderForm.value.autoRecordOnPhoneCallActive &&
                    !audioRecorder.isRecording.value
                ) {
                    wasRecordingTriggeredByPhoneCall = true
                    _currentTab.value = AppTab.RECORD
                    startCallRecordingForActiveTelephonyCall()
                } else if ((callInfo.phase == SystemCallPhase.DISCONNECTED || callInfo.phase == SystemCallPhase.IDLE) &&
                    wasRecordingTriggeredByPhoneCall &&
                    audioRecorder.isRecording.value
                ) {
                    wasRecordingTriggeredByPhoneCall = false
                    stopRecordingAndTranscribe()
                }
            }
        }
    }

    fun refreshTelephonyBindingsAndCallLog() {
        telephonyMonitor.startMonitoring()
        telephonyMonitor.refreshConnectedBluetoothDevices()
        _isDefaultDialer.value = telephonyMonitor.isDefaultDialerApp()
        _isCallScreeningEnabled.value = telephonyMonitor.isCallScreeningRoleHeld()
        viewModelScope.launch {
            _systemCallLog.value = telephonyMonitor.loadRecentSystemCallLog(limit = 12)
        }
    }

    private fun handleSystemTelephonyTransition(
        newPhase: SystemCallPhase,
        phone: String,
        contact: String,
        direction: String
    ) {
        if (phone.isNotBlank() || contact.isNotBlank()) {
            _recorderForm.update { current ->
                current.copy(
                    phoneNumber = phone.ifBlank { current.phoneNumber },
                    contactName = contact.ifBlank { current.contactName },
                    callDirection = direction.ifBlank { current.callDirection }
                )
            }
        }

        when (newPhase) {
            SystemCallPhase.RINGING -> {
                _currentTab.value = AppTab.RECORD
                _snackbarMessage.value =
                    "Incoming Phone Call detected (${phone.ifBlank { "Caller" }}) — Ready to record."
            }
            SystemCallPhase.ACTIVE_IN_CALL -> {
                if (_recorderForm.value.autoRecordOnPhoneCallActive && !audioRecorder.isRecording.value) {
                    wasRecordingTriggeredByPhoneCall = true
                    _currentTab.value = AppTab.RECORD
                    startCallRecordingForActiveTelephonyCall()
                }
            }
            SystemCallPhase.IDLE, SystemCallPhase.DISCONNECTED -> {
                if (wasRecordingTriggeredByPhoneCall && audioRecorder.isRecording.value) {
                    wasRecordingTriggeredByPhoneCall = false
                    stopRecordingAndTranscribe()
                }
                refreshTelephonyBindingsAndCallLog()
            }
            else -> {}
        }
    }

    private fun startCallRecordingForActiveTelephonyCall() {
        val currentSettings = settings.value
        if (currentSettings.autoPlayAudibleTtsAlert) {
            playComplianceNoticeNow {
                beginActualRecording(audiblePlayed = true)
            }
        } else {
            beginActualRecording(audiblePlayed = false)
        }
    }

    // --- Real Phone Call Dialer, Keypad & Audio Output Routing Controls ---

    fun pressKeypadDigit(digit: Char) {
        telephonyMonitor.playKeypadDtmfTone(digit)
        val nextPhone = _recorderForm.value.phoneNumber + digit
        val lookedUp = telephonyMonitor.lookupContactName(nextPhone)
        _recorderForm.update { current ->
            current.copy(
                phoneNumber = nextPhone,
                contactName = lookedUp.ifBlank { current.contactName }
            )
        }
    }

    fun backspaceDialerNumber() {
        val currentPhone = _recorderForm.value.phoneNumber
        if (currentPhone.isEmpty()) return
        val nextPhone = currentPhone.dropLast(1)
        val lookedUp = telephonyMonitor.lookupContactName(nextPhone)
        _recorderForm.update { current ->
            current.copy(
                phoneNumber = nextPhone,
                contactName = if (nextPhone.isEmpty()) "" else lookedUp.ifBlank { current.contactName }
            )
        }
    }

    fun clearDialerNumber() {
        _recorderForm.update {
            it.copy(phoneNumber = "", contactName = "")
        }
    }

    fun appendDialerPlusSign() {
        telephonyMonitor.playKeypadDtmfTone('0')
        val currentPhone = _recorderForm.value.phoneNumber
        val nextPhone = if (currentPhone.isEmpty()) "+" else "$currentPhone+"
        _recorderForm.update { it.copy(phoneNumber = nextPhone) }
    }

    fun selectCallAudioOutputRoute(route: CallAudioOutputRoute) {
        val statusMsg = telephonyMonitor.setCallAudioOutputRoute(route)
        _snackbarMessage.value = statusMsg
    }

    fun dialAndAutoRecordFromDialer() {
        val targetNumber = _recorderForm.value.phoneNumber.trim()
        if (targetNumber.isBlank()) {
            _snackbarMessage.value = "Enter a phone number on the keypad to call & record."
            return
        }
        _recorderForm.update { it.copy(autoRecordOnPhoneCallActive = true) }
        dialAndRecordPhoneCall(targetNumber, _recorderForm.value.contactName)
        _currentTab.value = AppTab.RECORD
    }

    fun dialAndRecordPhoneCall(phoneNumber: String, contactName: String) {
        val targetNumber = phoneNumber.trim().ifBlank { _recorderForm.value.phoneNumber.trim() }
        if (targetNumber.isBlank()) {
            _snackbarMessage.value = "Please enter a phone number to place a call."
            return
        }
        val resolvedContact = contactName.trim().ifBlank {
            telephonyMonitor.lookupContactName(targetNumber)
        }
        _recorderForm.update {
            it.copy(
                phoneNumber = targetNumber,
                contactName = resolvedContact.ifBlank { targetNumber },
                callDirection = "OUTGOING"
            )
        }

        val res = telephonyMonitor.placeRealPhoneCall(targetNumber)
        res.fold(
            onSuccess = { msg ->
                _snackbarMessage.value = msg
            },
            onFailure = { err ->
                _snackbarMessage.value = "Failed to place call: ${err.localizedMessage}"
            }
        )
    }

    fun answerIncomingSystemCall() {
        val answered = telephonyMonitor.answerRingingCall()
        if (answered) {
            _snackbarMessage.value = "Answered incoming phone call."
        } else {
            _snackbarMessage.value = "Use system phone notification or grant Answer Phone Calls permission."
        }
    }

    fun endActiveSystemCall() {
        val ended = telephonyMonitor.endActiveCall()
        if (audioRecorder.isRecording.value) {
            wasRecordingTriggeredByPhoneCall = false
            stopRecordingAndTranscribe()
        } else if (ended) {
            _snackbarMessage.value = "Phone call ended."
        }
    }

    fun toggleCallHold() {
        PhoneCallStateBus.toggleHoldTelecomCall()
    }

    fun toggleCallSpeakerphone() {
        val currentRoute = activePhoneCallInfo.value.audioRoute
        val nextRoute = if (currentRoute == CallAudioOutputRoute.LOUDSPEAKER) {
            CallAudioOutputRoute.EARPIECE
        } else {
            CallAudioOutputRoute.LOUDSPEAKER
        }
        selectCallAudioOutputRoute(nextRoute)
    }

    fun toggleCallMute() {
        val next = !activePhoneCallInfo.value.isMicrophoneMuted
        telephonyMonitor.setMicrophoneMuted(next)
    }

    fun toggleAutoRecordOnPhoneCall(enabled: Boolean) {
        _recorderForm.update { it.copy(autoRecordOnPhoneCallActive = enabled) }
        _snackbarMessage.value = if (enabled) {
            "Auto-record enabled: VoxScribe will automatically record when a phone call connects."
        } else {
            "Auto-record disabled: Tap 'Start Call Recording' manually during calls."
        }
    }

    fun selectSystemCallLogEntry(entry: SystemCallLogEntry) {
        _recorderForm.update {
            it.copy(
                phoneNumber = entry.phoneNumber,
                contactName = entry.cachedName,
                callDirection = if (entry.callType == "MISSED") "INCOMING" else entry.callType
            )
        }
        _snackbarMessage.value = "Loaded ${entry.cachedName} (${entry.phoneNumber}) from device Call Log."
    }

    fun getRequestDefaultDialerIntent(): Intent? = telephonyMonitor.createRequestDialerRoleIntent()
    fun getRequestCallScreeningIntent(): Intent? = telephonyMonitor.createRequestCallScreeningRoleIntent()

    // --- Google Account Sign-In & Associated Gemini Management ---

    fun setShowGoogleAccountSheet(visible: Boolean) {
        _showGoogleAccountSheet.value = visible
        if (!visible) {
            _googleAuthHintMessage.value = null
        }
    }

    fun signInWithGoogleCredentialManager(activityContext: Context) {
        viewModelScope.launch {
            _isGoogleSigningIn.value = true
            _googleAuthHintMessage.value = null
            when (val outcome = googleAuthManager.signInWithGoogle(activityContext)) {
                is GoogleSignInOutcome.SignedIn -> {
                    _isGoogleSigningIn.value = false
                    if (outcome.pendingConsentIntentSender != null) {
                        _pendingOAuthIntentSender.value = outcome.pendingConsentIntentSender
                    } else {
                        _snackbarMessage.value =
                            "Signed in as ${outcome.profile.email} — Gemini AI linked to your Google Account!"
                    }
                }

                is GoogleSignInOutcome.NeedsOAuthConsentResolution -> {
                    _isGoogleSigningIn.value = false
                    _pendingOAuthIntentSender.value = outcome.intentSender
                }

                is GoogleSignInOutcome.NoGoogleAccountOnDevice -> {
                    _isGoogleSigningIn.value = false
                    _googleAuthHintMessage.value = outcome.message
                }

                is GoogleSignInOutcome.Cancelled -> {
                    _isGoogleSigningIn.value = false
                    _snackbarMessage.value = outcome.message
                }

                is GoogleSignInOutcome.Error -> {
                    _isGoogleSigningIn.value = false
                    _googleAuthHintMessage.value = outcome.message
                }
            }
        }
    }

    fun handleGoogleOAuthConsentResult(data: Intent?) {
        viewModelScope.launch {
            _pendingOAuthIntentSender.value = null
            val res = googleAuthManager.handleOAuthAuthorizationResult(data, googleAccount.value)
            res.fold(
                onSuccess = { profile ->
                    _snackbarMessage.value =
                        "Authorized Google Gemini OAuth scopes for ${profile.email}!"
                },
                onFailure = { err ->
                    _snackbarMessage.value =
                        "Google Sign-In active (${googleAccount.value.email.ifBlank { "Account linked" }}). ${err.localizedMessage ?: ""}"
                }
            )
        }
    }

    fun consumePendingOAuthIntentSender() {
        _pendingOAuthIntentSender.value = null
    }

    fun signInWithLinkedGoogleAccount(
        email: String,
        displayName: String,
        oauthAccessToken: String,
        associatedProjectId: String,
        accountGeminiApiKey: String
    ) {
        viewModelScope.launch {
            _isGoogleSigningIn.value = true
            val res = googleAuthManager.signInWithLinkedGoogleAccount(
                email = email,
                displayName = displayName,
                oauthAccessToken = oauthAccessToken,
                associatedProjectId = associatedProjectId,
                accountGeminiApiKey = accountGeminiApiKey
            )
            _isGoogleSigningIn.value = false
            res.fold(
                onSuccess = { profile ->
                    _googleAuthHintMessage.value = null
                    _snackbarMessage.value =
                        "Signed in as ${profile.displayName} (${profile.email}) — Google Gemini AI active!"
                },
                onFailure = { err ->
                    _googleAuthHintMessage.value = err.localizedMessage
                }
            )
        }
    }

    fun toggleUseGoogleAccountForGemini(enabled: Boolean) {
        viewModelScope.launch {
            googleAuthManager.setUseAccountForGemini(enabled)
            _snackbarMessage.value = if (enabled) {
                "Now using Gemini AI associated with ${googleAccount.value.email}"
            } else {
                "Switched to default app Gemini configuration"
            }
        }
    }

    fun signOutGoogleAccount() {
        viewModelScope.launch {
            googleAuthManager.signOut()
            _snackbarMessage.value = "Signed out of Google Account."
        }
    }

    fun openSystemAddGoogleAccountSettings() {
        _pendingShareIntent.value = googleAuthManager.createAddGoogleAccountSettingsIntent()
    }

    // --- Navigation & Filtering ---

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
        if (tab == AppTab.RECORD || tab == AppTab.DIALER) {
            refreshTelephonyBindingsAndCallLog()
        }
    }

    fun openCallDetail(callId: Long) {
        audioPlayer.stopAndRelease()
        _callQuestionAnswer.value = null
        _selectedCallId.value = callId
    }

    fun closeCallDetail() {
        audioPlayer.stopAndRelease()
        _callQuestionAnswer.value = null
        _exportConfig.update { it.copy(isVisible = false) }
        _selectedCallId.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTagFilter(tag: String?) {
        _selectedTagFilter.value = if (_selectedTagFilter.value == tag) null else tag
    }

    fun toggleStarredFilter() {
        _onlyStarredFilter.value = !_onlyStarredFilter.value
    }

    // --- Recorder & AI Summary Config ---

    fun updateContactName(name: String) {
        _recorderForm.update { it.copy(contactName = name) }
    }

    fun updatePhoneNumber(phone: String) {
        val lookedUp = telephonyMonitor.lookupContactName(phone)
        _recorderForm.update {
            it.copy(
                phoneNumber = phone,
                contactName = if (it.contactName.isBlank() && lookedUp.isNotBlank()) lookedUp else it.contactName
            )
        }
    }

    fun updateCallDirection(direction: String) {
        _recorderForm.update { it.copy(callDirection = direction) }
    }

    fun updateRecorderSummaryStyle(style: String) {
        _recorderForm.update { it.copy(summaryStyle = style) }
    }

    fun updateRecorderSummaryLength(length: SummaryLengthOption) {
        _recorderForm.update { it.copy(summaryLength = length) }
        if (audioRecorder.isRecording.value) {
            triggerLiveInCallAiUpdate()
        }
    }

    fun toggleRecorderFocusAspect(aspectId: String) {
        _recorderForm.update { current ->
            val updated = current.selectedFocusAspects.toMutableSet()
            if (updated.contains(aspectId)) {
                if (updated.size > 1) updated.remove(aspectId)
            } else {
                updated.add(aspectId)
            }
            current.copy(selectedFocusAspects = updated)
        }
        if (audioRecorder.isRecording.value) {
            triggerLiveInCallAiUpdate()
        }
    }

    fun updateLiveTranscriptNotes(notes: String) {
        _recorderForm.update { it.copy(liveTranscriptAndNotes = notes) }
        if (audioRecorder.isRecording.value) {
            scheduleDebouncedLiveInsight()
        }
    }

    fun toggleLiveSpeechRecognition() {
        val currentlyEnabled = _recorderForm.value.liveSpeechRecognitionEnabled
        if (currentlyEnabled) {
            liveSpeechTranscriber.stopListening()
            _recorderForm.update { it.copy(liveSpeechRecognitionEnabled = false) }
        } else {
            liveSpeechTranscriber.startListening()
            _recorderForm.update { it.copy(liveSpeechRecognitionEnabled = true) }
            _snackbarMessage.value = "Live microphone speech-to-text active for call."
        }
    }

    private fun appendSpokenMicLineToLiveTranscript(sentence: String) {
        val sec = audioRecorder.elapsedSeconds.value
        val timeTag = "[%02d:%02d]".format(sec / 60, sec % 60)
        val speakerName = googleAccount.value.takeIf { it.isSignedIn }?.displayName?.ifBlank { "You" } ?: "You"
        val formattedLine = "$timeTag $speakerName: $sentence"
        _recorderForm.update { current ->
            val merged = if (current.liveTranscriptAndNotes.isBlank()) {
                formattedLine
            } else {
                current.liveTranscriptAndNotes + "\n" + formattedLine
            }
            current.copy(liveTranscriptAndNotes = merged)
        }
        triggerLiveInCallAiUpdate()
    }

    fun requestStartCallRecording() {
        val currentSettings = settings.value
        if (currentSettings.requireConsentModalBeforeRecord) {
            _recorderForm.update {
                it.copy(
                    showConsentDialog = true,
                    hasPlayedAudibleAlertForSession = false,
                    partyConsentConfirmed = false
                )
            }
        } else {
            beginActualRecording(audiblePlayed = false)
        }
    }

    fun dismissConsentDialog() {
        ttsAnnouncer.stopAll()
        _recorderForm.update { it.copy(showConsentDialog = false) }
    }

    fun setPartyConsentConfirmed(confirmed: Boolean) {
        _recorderForm.update { it.copy(partyConsentConfirmed = confirmed) }
    }

    fun playComplianceNoticeNow(onFinished: () -> Unit = {}) {
        val currentSettings = settings.value
        ttsAnnouncer.speakComplianceNotice(
            script = currentSettings.customAnnouncementScript,
            routeToSpeakerphone = currentSettings.routeAlertToSpeakerphone
        ) {
            _recorderForm.update {
                it.copy(
                    hasPlayedAudibleAlertForSession = true,
                    partyConsentConfirmed = true,
                    lastConsentTimestamp = System.currentTimeMillis()
                )
            }
            onFinished()
        }
    }

    fun confirmConsentAndStartRecording() {
        val currentSettings = settings.value
        val form = _recorderForm.value
        _recorderForm.update {
            it.copy(
                showConsentDialog = false,
                lastConsentTimestamp = System.currentTimeMillis()
            )
        }

        if (currentSettings.autoPlayAudibleTtsAlert && !form.hasPlayedAudibleAlertForSession) {
            playComplianceNoticeNow {
                beginActualRecording(audiblePlayed = true)
            }
        } else {
            beginActualRecording(audiblePlayed = form.hasPlayedAudibleAlertForSession)
        }
    }

    private fun beginActualRecording(audiblePlayed: Boolean) {
        val routeSpeakerphone = settings.value.routeAlertToSpeakerphone
        val result = audioRecorder.startRecording(routeSpeakerphoneForCallCapture = routeSpeakerphone)
        result.fold(
            onSuccess = {
                liveSpeechTranscriber.startListening()
                val accountBadge = googleAccount.value.takeIf { it.isSignedIn }?.email ?: "Default Gemini"
                val audioSrc = audioRecorder.activeAudioSourceLabel.value
                _recorderForm.update {
                    it.copy(
                        hasPlayedAudibleAlertForSession = audiblePlayed,
                        liveSpeechRecognitionEnabled = true,
                        lastConsentTimestamp = System.currentTimeMillis(),
                        liveInCallInsight = LiveInCallInsight(
                            rollingSummary = "Recording active on $audioSrc. Speak during the phone call to stream real-time transcription & Gemini AI takeaways…",
                            lastUpdatedSecond = 0,
                            poweredByAccount = accountBadge
                        )
                    )
                }
                _snackbarMessage.value = "Call recording active ($audioSrc)."
            },
            onFailure = { err ->
                _snackbarMessage.value = "Could not start call audio recorder: ${err.localizedMessage}"
            }
        )
    }

    private fun scheduleDebouncedLiveInsight() {
        liveAnalysisDebounceJob?.cancel()
        liveAnalysisDebounceJob = viewModelScope.launch {
            delay(1200L)
            triggerLiveInCallAiUpdate()
        }
    }

    fun triggerLiveInCallAiUpdate() {
        val form = _recorderForm.value
        if (form.liveTranscriptAndNotes.isBlank()) return
        viewModelScope.launch {
            _recorderForm.update { it.copy(isUpdatingLiveInsight = true) }
            val insight = repository.analyzeLiveCallProgress(
                partialTranscript = form.liveTranscriptAndNotes,
                contactName = form.contactName.ifBlank { form.phoneNumber.ifBlank { "Caller" } },
                elapsedSeconds = audioRecorder.elapsedSeconds.value,
                summaryLength = form.summaryLength,
                focusAspects = form.selectedFocusAspects.toList(),
                googleProfile = googleAccount.value
            )
            _recorderForm.update {
                it.copy(
                    liveInCallInsight = insight,
                    isUpdatingLiveInsight = false
                )
            }
        }
    }

    fun stopRecordingAndTranscribe() {
        ttsAnnouncer.stopAll()
        liveSpeechTranscriber.stopListening()
        liveAnalysisDebounceJob?.cancel()
        val durationSec = audioRecorder.elapsedSeconds.value.coerceAtLeast(1)
        val audioFile = audioRecorder.stopRecording()
        val form = _recorderForm.value
        val callInfo = activePhoneCallInfo.value
        val currentSettings = settings.value

        val finalPhone = form.phoneNumber.ifBlank { callInfo.phoneNumber.ifBlank { "Phone Call" } }
        val finalContact = form.contactName.ifBlank {
            callInfo.contactName.ifBlank {
                telephonyMonitor.lookupContactName(finalPhone).ifBlank { finalPhone }
            }
        }
        val finalDirection = form.callDirection.ifBlank { callInfo.callDirection }

        viewModelScope.launch {
            _recorderForm.update { it.copy(isAnalyzingWithAi = true, liveSpeechRecognitionEnabled = false) }
            try {
                val saved = repository.processAndSaveCallRecording(
                    contactName = finalContact,
                    phoneNumber = finalPhone,
                    callDirection = finalDirection,
                    durationSeconds = durationSec,
                    audioFile = audioFile,
                    consentJurisdiction = currentSettings.jurisdictionDisplayName,
                    audibleNoticePlayed = form.hasPlayedAudibleAlertForSession,
                    consentScriptUsed = currentSettings.customAnnouncementScript,
                    consentTimestamp = form.lastConsentTimestamp.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    liveNotesOrDialogue = form.liveTranscriptAndNotes,
                    summaryStyle = form.summaryStyle,
                    summaryLength = form.summaryLength,
                    focusAspects = form.selectedFocusAspects.toList(),
                    googleProfile = googleAccount.value,
                    autoCloudSync = currentSettings.autoCloudSync,
                    cloudBucketName = currentSettings.cloudBucketName,
                    cloudEndpointUrl = currentSettings.cloudEndpointUrl
                )
                _recorderForm.update {
                    it.copy(
                        isAnalyzingWithAi = false,
                        liveTranscriptAndNotes = "",
                        liveInCallInsight = LiveInCallInsight()
                    )
                }
                _selectedCallId.value = saved.id
                _currentTab.value = AppTab.CALLS
                val accountNote = if (saved.geminiAccountEmail.isNotBlank()) {
                    " via ${saved.geminiAccountEmail}"
                } else {
                    ""
                }
                _snackbarMessage.value =
                    "Call transcribed & summarized (${form.summaryLength.label})$accountNote with ${saved.keywordTagsList().size} tags!"
            } catch (e: Exception) {
                _recorderForm.update { it.copy(isAnalyzingWithAi = false) }
                _snackbarMessage.value = "Failed to save call recording: ${e.localizedMessage}"
            }
        }
    }

    fun regenerateSummaryWithCustomConfig(
        recording: CallRecordingEntity,
        style: String,
        length: SummaryLengthOption,
        focusAspects: List<String>
    ) {
        viewModelScope.launch {
            _isRegeneratingSummary.value = true
            val res = repository.regenerateSummaryWithConfig(
                recording = recording,
                newStyle = style,
                newLength = length,
                newFocusAspects = focusAspects,
                googleProfile = googleAccount.value
            )
            _isRegeneratingSummary.value = false
            res.fold(
                onSuccess = {
                    val acct = googleAccount.value.takeIf { it.isSignedIn }?.email
                    _snackbarMessage.value =
                        "Summary updated ($style · ${length.label})${if (acct != null) " via $acct" else ""}!"
                },
                onFailure = { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not regenerate summary."
                }
            )
        }
    }

    fun askQuestionAboutCall(recording: CallRecordingEntity, question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _isAskingCallQuestion.value = true
            val res = repository.askGeminiAboutCall(
                recording = recording,
                question = question,
                googleProfile = googleAccount.value
            )
            _isAskingCallQuestion.value = false
            res.fold(
                onSuccess = { answer ->
                    _callQuestionAnswer.value = answer
                },
                onFailure = { err ->
                    _callQuestionAnswer.value = err.localizedMessage ?: "Unable to answer question."
                }
            )
        }
    }

    fun addCustomTag(recording: CallRecordingEntity, tag: String) {
        viewModelScope.launch {
            repository.addCustomKeywordTag(recording, tag)
            _snackbarMessage.value = "Added tag #${tag.removePrefix("#").trim()}"
        }
    }

    fun removeTag(recording: CallRecordingEntity, tag: String) {
        viewModelScope.launch {
            repository.removeKeywordTag(recording, tag)
        }
    }

    fun toggleActionItem(recording: CallRecordingEntity, index: Int) {
        viewModelScope.launch {
            repository.toggleActionItem(recording, index)
        }
    }

    fun toggleStarred(recording: CallRecordingEntity) {
        viewModelScope.launch {
            repository.toggleStarred(recording)
        }
    }

    fun deleteRecording(recording: CallRecordingEntity) {
        viewModelScope.launch {
            audioPlayer.stopAndRelease()
            repository.deleteRecording(recording)
            if (_selectedCallId.value == recording.id) {
                _selectedCallId.value = null
            }
            _snackbarMessage.value = "Call recording deleted."
        }
    }

    // --- Advanced Export Configurator Actions ---

    fun openExportConfigurator(
        defaultTarget: ExportTargetApp = ExportTargetApp.NOTION,
        defaultFormat: ExportFormatOption = ExportFormatOption.MARKDOWN
    ) {
        _exportConfig.update {
            it.copy(
                isVisible = true,
                selectedTargetApp = defaultTarget,
                selectedFormat = defaultFormat
            )
        }
    }

    fun closeExportConfigurator() {
        _exportConfig.update { it.copy(isVisible = false) }
    }

    fun updateExportFormat(format: ExportFormatOption) {
        _exportConfig.update { it.copy(selectedFormat = format) }
    }

    fun updateExportTargetApp(targetApp: ExportTargetApp) {
        val suggestedFormat = when (targetApp) {
            ExportTargetApp.EVERNOTE -> ExportFormatOption.ENEX
            ExportTargetApp.GOOGLE_KEEP -> ExportFormatOption.PLAIN_TEXT
            else -> _exportConfig.value.selectedFormat
        }
        _exportConfig.update {
            it.copy(
                selectedTargetApp = targetApp,
                selectedFormat = suggestedFormat
            )
        }
    }

    fun updateExportFields(fields: ExportFieldSelection) {
        _exportConfig.update { it.copy(fields = fields) }
    }

    fun getExportPreviewText(recording: CallRecordingEntity): String {
        val cfg = _exportConfig.value
        return repository.formatExportPreview(
            recording = recording,
            format = cfg.selectedFormat,
            fields = cfg.fields,
            notebookName = settings.value.evernoteNotebookName
        )
    }

    fun executeConfiguredExport(recording: CallRecordingEntity) {
        val cfg = _exportConfig.value
        viewModelScope.launch {
            val result = repository.executeAdvancedExport(
                recording = recording,
                targetApp = cfg.selectedTargetApp,
                format = cfg.selectedFormat,
                fields = cfg.fields,
                notionDatabaseId = settings.value.notionDatabaseId,
                evernoteNotebook = settings.value.evernoteNotebookName
            )
            when (result) {
                is ExportResult.Success -> {
                    _snackbarMessage.value = result.message
                    result.shareIntent?.let { _pendingShareIntent.value = it }
                }
                is ExportResult.Error -> {
                    _snackbarMessage.value = result.errorMessage
                }
            }
        }
    }

    fun quickExportToTarget(recording: CallRecordingEntity, targetApp: ExportTargetApp) {
        val defaultFormat = when (targetApp) {
            ExportTargetApp.EVERNOTE -> ExportFormatOption.ENEX
            ExportTargetApp.GOOGLE_KEEP -> ExportFormatOption.PLAIN_TEXT
            else -> ExportFormatOption.MARKDOWN
        }
        viewModelScope.launch {
            val result = repository.executeAdvancedExport(
                recording = recording,
                targetApp = targetApp,
                format = defaultFormat,
                fields = ExportFieldSelection(),
                notionDatabaseId = settings.value.notionDatabaseId,
                evernoteNotebook = settings.value.evernoteNotebookName
            )
            when (result) {
                is ExportResult.Success -> {
                    _snackbarMessage.value = result.message
                    result.shareIntent?.let { _pendingShareIntent.value = it }
                }
                is ExportResult.Error -> {
                    _snackbarMessage.value = result.errorMessage
                }
            }
        }
    }

    fun syncSingleCallToCloud(recording: CallRecordingEntity) {
        viewModelScope.launch {
            _isSyncingCloud.value = true
            val currentSettings = settings.value
            val res = repository.syncSingleRecordingToCloud(
                recording = recording,
                bucketName = currentSettings.cloudBucketName,
                endpointUrl = currentSettings.cloudEndpointUrl
            )
            _isSyncingCloud.value = false
            res.fold(
                onSuccess = { outcome ->
                    _snackbarMessage.value = "Synced to Cloud Vault (${outcome.sha256Checksum.take(10)}…)"
                },
                onFailure = { err ->
                    _snackbarMessage.value = "Cloud sync error: ${err.localizedMessage}"
                }
            )
        }
    }

    fun syncAllPendingToCloud() {
        viewModelScope.launch {
            _isSyncingCloud.value = true
            val currentSettings = settings.value
            val count = repository.syncAllUnsyncedRecordings(
                bucketName = currentSettings.cloudBucketName,
                endpointUrl = currentSettings.cloudEndpointUrl
            )
            _isSyncingCloud.value = false
            _snackbarMessage.value = if (count > 0) {
                "Synced $count call archive(s) to Cloud Vault!"
            } else {
                "All call recordings are already synced to Cloud Vault."
            }
        }
    }

    fun shareCloudVaultArchiveFile(recording: CallRecordingEntity) {
        viewModelScope.launch {
            try {
                val mirrorFile = repository.getCloudMirrorFile(recording)
                val app = getApplication<Application>()
                val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", mirrorFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, "VoxScribe Cloud Vault Archive - ${recording.contactName}")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                _pendingShareIntent.value = Intent.createChooser(intent, "Save / Share Cloud Vault JSON Archive").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "Unable to share archive: ${e.localizedMessage}"
            }
        }
    }

    // --- Compliance & Cloud Settings Updates ---

    fun updateJurisdiction(mode: String) {
        viewModelScope.launch {
            prefsRepository.updateJurisdiction(mode)
            _snackbarMessage.value = "Updated compliance jurisdiction & announcement script."
        }
    }

    fun updateRequireConsentModal(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.updateRequireConsentModal(enabled) }
    }

    fun updateAutoPlayTts(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.updateAutoPlayTts(enabled) }
    }

    fun updateRouteToSpeakerphone(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.updateRouteToSpeakerphone(enabled) }
    }

    fun updateCustomScript(script: String) {
        viewModelScope.launch {
            prefsRepository.updateCustomScript(script)
            _snackbarMessage.value = "Saved custom compliance announcement script."
        }
    }

    fun updateAutoCloudSync(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.updateAutoCloudSync(enabled) }
    }

    fun updateCloudConfig(bucketName: String, endpointUrl: String) {
        viewModelScope.launch {
            prefsRepository.updateCloudConfig(bucketName, endpointUrl)
            _snackbarMessage.value = "Saved Cloud Vault bucket & endpoint settings."
        }
    }

    fun updateExportTargets(notionDbId: String, evernoteNotebook: String) {
        viewModelScope.launch {
            prefsRepository.updateExportTargets(notionDbId, evernoteNotebook)
            _snackbarMessage.value = "Saved Notion & Evernote export preferences."
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun consumePendingShareIntent() {
        _pendingShareIntent.value = null
    }

    override fun onCleared() {
        super.onCleared()
        telephonyMonitor.stopMonitoring()
        audioPlayer.stopAndRelease()
        ttsAnnouncer.shutdown()
        liveSpeechTranscriber.stopListening()
    }
}

package com.example.telephony

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SystemCallPhase(val displayLabel: String) {
    IDLE("Idle (Ready to Dial or Record)"),
    RINGING("Incoming Call Ringing"),
    DIALING("Dialing Outgoing Call"),
    ACTIVE_IN_CALL("Connected — Active Phone Call"),
    HOLDING("Call On Hold"),
    DISCONNECTED("Call Ended")
}

/**
 * 3-Way Call Audio Output Route:
 * - `BLUETOOTH`: Routes call & recording audio through connected Bluetooth headset/speaker (`ROUTE_BLUETOOTH` / `BluetoothSco`).
 * - `EARPIECE`: Routes call audio through built-in Phone Speaker / Handset Earpiece (`ROUTE_WIRED_OR_EARPIECE`).
 * - `LOUDSPEAKER`: Routes call audio through high-gain Loudspeaker (`ROUTE_SPEAKER`) for two-way acoustic/line capture.
 */
enum class CallAudioOutputRoute(
    val id: String,
    val label: String,
    val shortLabel: String,
    val telecomRouteMask: Int
) {
    BLUETOOTH(
        id = "BLUETOOTH",
        label = "Bluetooth Speaker",
        shortLabel = "Bluetooth",
        telecomRouteMask = CallAudioState.ROUTE_BLUETOOTH
    ),
    EARPIECE(
        id = "EARPIECE",
        label = "Phone Speaker",
        shortLabel = "Phone Speaker",
        telecomRouteMask = CallAudioState.ROUTE_WIRED_OR_EARPIECE
    ),
    LOUDSPEAKER(
        id = "LOUDSPEAKER",
        label = "Loudspeaker",
        shortLabel = "Loudspeaker",
        telecomRouteMask = CallAudioState.ROUTE_SPEAKER
    )
}

data class ActivePhoneCallInfo(
    val phase: SystemCallPhase = SystemCallPhase.IDLE,
    val phoneNumber: String = "",
    val contactName: String = "",
    val callDirection: String = "OUTGOING", // "INCOMING" or "OUTGOING"
    val isInCallServiceBound: Boolean = false,
    val isOnHold: Boolean = false,
    val audioRoute: CallAudioOutputRoute = CallAudioOutputRoute.LOUDSPEAKER,
    val isSpeakerphoneOn: Boolean = true,
    val isBluetoothAvailable: Boolean = false,
    val connectedBluetoothName: String = "",
    val isMicrophoneMuted: Boolean = false,
    val networkOperatorName: String = "",
    val audioCaptureSourceLabel: String = "VOICE_COMMUNICATION",
    val lastDtmfSequence: String = ""
)

data class SystemCallLogEntry(
    val id: Long,
    val phoneNumber: String,
    val cachedName: String,
    val callType: String, // "INCOMING", "OUTGOING", "MISSED"
    val timestamp: Long,
    val durationSeconds: Int
)

/**
 * Process-wide reactive bridge connecting Android's `InCallService`, `CallScreeningService`,
 * and `TelephonyManager` callbacks with the VoxScribe Dialer, Keypad, Audio Router, and Recorder.
 */
object PhoneCallStateBus {
    private var currentTelecomCall: Call? = null
    private var activeInCallService: InCallService? = null

    private val _callInfo = MutableStateFlow(ActivePhoneCallInfo())
    val callInfo: StateFlow<ActivePhoneCallInfo> = _callInfo.asStateFlow()

    fun attachInCallService(service: InCallService) {
        activeInCallService = service
        _callInfo.update { it.copy(isInCallServiceBound = true) }
    }

    fun detachInCallService(service: InCallService) {
        if (activeInCallService == service) {
            activeInCallService = null
        }
    }

    fun onTelecomAudioStateChanged(audioState: CallAudioState?) {
        if (audioState == null) return
        val mappedRoute = when (audioState.route) {
            CallAudioState.ROUTE_BLUETOOTH -> CallAudioOutputRoute.BLUETOOTH
            CallAudioState.ROUTE_SPEAKER -> CallAudioOutputRoute.LOUDSPEAKER
            else -> CallAudioOutputRoute.EARPIECE
        }
        val btSupported = (audioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH) != 0
        _callInfo.update { current ->
            current.copy(
                audioRoute = mappedRoute,
                isSpeakerphoneOn = mappedRoute == CallAudioOutputRoute.LOUDSPEAKER,
                isBluetoothAvailable = btSupported || current.isBluetoothAvailable,
                isMicrophoneMuted = audioState.isMuted
            )
        }
    }

    fun requestTelecomAudioRoute(route: CallAudioOutputRoute): Boolean {
        val service = activeInCallService ?: return false
        return runCatching {
            service.setAudioRoute(route.telecomRouteMask)
            true
        }.getOrDefault(false)
    }

    fun onTelecomCallAttached(call: Call, resolvedContactName: String?) {
        currentTelecomCall = call
        val number = call.details?.handle?.schemeSpecificPart.orEmpty()
        val callerDisplay = call.details?.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: resolvedContactName?.takeIf { it.isNotBlank() }
            ?: ""
        val direction = if (call.details?.callDirection == Call.Details.DIRECTION_INCOMING) {
            "INCOMING"
        } else {
            "OUTGOING"
        }
        val mappedPhase = mapTelecomStateToPhase(call.state)
        _callInfo.update { current ->
            current.copy(
                phase = mappedPhase,
                phoneNumber = number.ifBlank { current.phoneNumber },
                contactName = callerDisplay.ifBlank { current.contactName },
                callDirection = direction,
                isInCallServiceBound = true,
                isOnHold = call.state == Call.STATE_HOLDING
            )
        }
    }

    fun onTelecomCallStateChanged(call: Call, state: Int, resolvedContactName: String?) {
        val number = call.details?.handle?.schemeSpecificPart.orEmpty()
        val callerDisplay = call.details?.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: resolvedContactName?.takeIf { it.isNotBlank() }
            ?: ""
        val mappedPhase = mapTelecomStateToPhase(state)
        _callInfo.update { current ->
            current.copy(
                phase = mappedPhase,
                phoneNumber = number.ifBlank { current.phoneNumber },
                contactName = callerDisplay.ifBlank { current.contactName },
                isInCallServiceBound = true,
                isOnHold = state == Call.STATE_HOLDING
            )
        }
    }

    fun onTelecomCallRemoved(call: Call) {
        if (currentTelecomCall == call) {
            currentTelecomCall = null
        }
        _callInfo.update { current ->
            current.copy(
                phase = SystemCallPhase.IDLE,
                isInCallServiceBound = activeInCallService != null,
                isOnHold = false,
                lastDtmfSequence = ""
            )
        }
    }

    fun onScreenedCallDetected(phoneNumber: String, direction: String, resolvedContactName: String) {
        _callInfo.update { current ->
            current.copy(
                phoneNumber = phoneNumber.ifBlank { current.phoneNumber },
                contactName = resolvedContactName.ifBlank { current.contactName },
                callDirection = direction
            )
        }
    }

    fun updateFromTelephonyManager(
        phase: SystemCallPhase,
        phoneNumber: String?,
        contactName: String?,
        callDirection: String?,
        networkOperator: String?
    ) {
        _callInfo.update { current ->
            val nextPhase = if (current.isInCallServiceBound && phase == SystemCallPhase.ACTIVE_IN_CALL) {
                current.phase
            } else {
                phase
            }
            current.copy(
                phase = nextPhase,
                phoneNumber = phoneNumber?.takeIf { it.isNotBlank() } ?: current.phoneNumber,
                contactName = contactName?.takeIf { it.isNotBlank() } ?: current.contactName,
                callDirection = callDirection?.takeIf { it.isNotBlank() } ?: current.callDirection,
                networkOperatorName = networkOperator?.takeIf { it.isNotBlank() } ?: current.networkOperatorName
            )
        }
    }

    fun updateAudioState(
        audioRoute: CallAudioOutputRoute? = null,
        isSpeakerphoneOn: Boolean? = null,
        isBluetoothAvailable: Boolean? = null,
        connectedBluetoothName: String? = null,
        isMuted: Boolean? = null,
        audioSourceLabel: String? = null
    ) {
        _callInfo.update { current ->
            val resolvedRoute = audioRoute ?: when {
                isSpeakerphoneOn == true -> CallAudioOutputRoute.LOUDSPEAKER
                isSpeakerphoneOn == false && current.audioRoute == CallAudioOutputRoute.LOUDSPEAKER -> CallAudioOutputRoute.EARPIECE
                else -> current.audioRoute
            }
            current.copy(
                audioRoute = resolvedRoute,
                isSpeakerphoneOn = isSpeakerphoneOn ?: (resolvedRoute == CallAudioOutputRoute.LOUDSPEAKER),
                isBluetoothAvailable = isBluetoothAvailable ?: current.isBluetoothAvailable,
                connectedBluetoothName = connectedBluetoothName ?: current.connectedBluetoothName,
                isMicrophoneMuted = isMuted ?: current.isMicrophoneMuted,
                audioCaptureSourceLabel = audioSourceLabel ?: current.audioCaptureSourceLabel
            )
        }
    }

    fun sendTelecomDtmfTone(digit: Char): Boolean {
        _callInfo.update { current ->
            current.copy(lastDtmfSequence = (current.lastDtmfSequence + digit).takeLast(16))
        }
        val call = currentTelecomCall ?: return false
        return runCatching {
            call.playDtmfTone(digit)
            call.stopDtmfTone()
            true
        }.getOrDefault(false)
    }

    fun answerIncomingTelecomCall(): Boolean {
        val call = currentTelecomCall ?: return false
        return runCatching {
            call.answer(VideoProfile.STATE_AUDIO_ONLY)
            true
        }.getOrDefault(false)
    }

    fun disconnectActiveTelecomCall(): Boolean {
        val call = currentTelecomCall ?: return false
        return runCatching {
            call.disconnect()
            true
        }.getOrDefault(false)
    }

    fun toggleHoldTelecomCall(): Boolean {
        val call = currentTelecomCall ?: return false
        return runCatching {
            if (call.state == Call.STATE_HOLDING) {
                call.unhold()
            } else {
                call.hold()
            }
            true
        }.getOrDefault(false)
    }

    private fun mapTelecomStateToPhase(state: Int): SystemCallPhase {
        return when (state) {
            Call.STATE_RINGING -> SystemCallPhase.RINGING
            Call.STATE_DIALING, Call.STATE_CONNECTING -> SystemCallPhase.DIALING
            Call.STATE_ACTIVE -> SystemCallPhase.ACTIVE_IN_CALL
            Call.STATE_HOLDING -> SystemCallPhase.HOLDING
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> SystemCallPhase.DISCONNECTED
            else -> SystemCallPhase.IDLE
        }
    }
}

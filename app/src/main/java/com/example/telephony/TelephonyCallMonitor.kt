package com.example.telephony

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Real Android `TelephonyManager`, `TelecomManager`, `AudioManager`, `ToneGenerator`, `CallLog`, and `RoleManager` controller.
 * - Registers live OS `TelephonyCallback` / `PhoneStateListener` to detect `CALL_STATE_RINGING`,
 *   `CALL_STATE_OFFHOOK` (active phone call), and `CALL_STATE_IDLE` (call ended).
 * - Places real outgoing phone calls via `TelecomManager.placeCall` / `Intent.ACTION_CALL` / `Intent.ACTION_DIAL`.
 * - Switches call audio output between **Bluetooth Speaker**, **Phone Speaker (Earpiece)**, and **Loudspeaker**.
 * - Generates real DTMF Keypad tones (`ToneGenerator`) and sends Telecom DTMF tones during active calls.
 */
class TelephonyCallMonitor(
    private val context: Context,
    private val onCallStateChanged: (
        newPhase: SystemCallPhase,
        phoneNumber: String,
        contactName: String,
        direction: String
    ) -> Unit
) {
    private val telephonyManager: TelephonyManager? =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val telecomManager: TelecomManager? =
        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var toneGenerator: ToneGenerator? = null
    private var isMonitoring = false
    private var lastTelephonyState = TelephonyManager.CALL_STATE_IDLE
    private var currentDirection = "OUTGOING"
    private var currentDetectedNumber = ""
    private var currentDetectedContact = ""

    private var telephonyCallbackApi31: Any? = null

    @Suppress("DEPRECATION")
    private var legacyPhoneStateListener: PhoneStateListener? = null

    fun startMonitoring() {
        refreshConnectedBluetoothDevices()
        if (isMonitoring) return
        val hasPhoneStatePerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val operatorName = runCatching {
            telephonyManager?.networkOperatorName?.takeIf { it.isNotBlank() } ?: "Cellular / VoIP Network"
        }.getOrDefault("Cellular / VoIP Network")

        PhoneCallStateBus.updateFromTelephonyManager(
            phase = SystemCallPhase.IDLE,
            phoneNumber = null,
            contactName = null,
            callDirection = null,
            networkOperator = operatorName
        )

        if (!hasPhoneStatePerm || telephonyManager == null) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleRawTelephonyState(state, null)
                    }
                }
                telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
                telephonyCallbackApi31 = callback
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleRawTelephonyState(state, phoneNumber)
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                legacyPhoneStateListener = listener
            }
            isMonitoring = true
        } catch (_: SecurityException) {
            isMonitoring = false
        }
    }

    fun stopMonitoring() {
        runCatching { toneGenerator?.release() }
        toneGenerator = null
        val tm = telephonyManager ?: return
        if (!isMonitoring) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (telephonyCallbackApi31 as? TelephonyCallback)?.let {
                    tm.unregisterTelephonyCallback(it)
                }
            } else {
                @Suppress("DEPRECATION")
                legacyPhoneStateListener?.let {
                    tm.listen(it, PhoneStateListener.LISTEN_NONE)
                }
            }
        }
        isMonitoring = false
    }

    private fun handleRawTelephonyState(state: Int, incomingNumberHint: String?) {
        val busState = PhoneCallStateBus.callInfo.value
        val numberFromHint = incomingNumberHint?.takeIf { it.isNotBlank() }
            ?: busState.phoneNumber.takeIf { it.isNotBlank() }
            ?: queryLatestCallLogNumber()
            ?: currentDetectedNumber

        if (numberFromHint.isNotBlank()) {
            currentDetectedNumber = numberFromHint
            val lookedUp = lookupContactName(numberFromHint)
            if (lookedUp.isNotBlank()) {
                currentDetectedContact = lookedUp
            }
        }

        val phase = when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                currentDirection = "INCOMING"
                SystemCallPhase.RINGING
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (lastTelephonyState != TelephonyManager.CALL_STATE_RINGING && busState.callDirection != "INCOMING") {
                    currentDirection = "OUTGOING"
                }
                SystemCallPhase.ACTIVE_IN_CALL
            }
            else -> SystemCallPhase.IDLE
        }

        lastTelephonyState = state

        val operatorName = runCatching {
            telephonyManager?.networkOperatorName?.takeIf { it.isNotBlank() } ?: "Cellular Network"
        }.getOrDefault("Cellular Network")

        PhoneCallStateBus.updateFromTelephonyManager(
            phase = phase,
            phoneNumber = currentDetectedNumber,
            contactName = currentDetectedContact,
            callDirection = currentDirection,
            networkOperator = operatorName
        )

        onCallStateChanged(
            phase,
            currentDetectedNumber,
            currentDetectedContact,
            currentDirection
        )
    }

    /**
     * Switches the phone call audio route between:
     * - `CallAudioOutputRoute.BLUETOOTH` (Bluetooth Headset / Car / Speaker)
     * - `CallAudioOutputRoute.EARPIECE` (Built-in Phone Handset Speaker)
     * - `CallAudioOutputRoute.LOUDSPEAKER` (Loudspeaker / Speakerphone)
     */
    fun setCallAudioOutputRoute(route: CallAudioOutputRoute): String {
        refreshConnectedBluetoothDevices()
        val am = audioManager
        runCatching {
            // 1. Forward to InCallService if bound as Default Dialer
            PhoneCallStateBus.requestTelecomAudioRoute(route)

            if (am != null) {
                am.mode = AudioManager.MODE_IN_COMMUNICATION

                // 2. Modern Android 12+ (API 31+) Communication Device Routing
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val commDevices = am.availableCommunicationDevices
                    val targetDevice = when (route) {
                        CallAudioOutputRoute.BLUETOOTH -> commDevices.firstOrNull {
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                                it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        }
                        CallAudioOutputRoute.LOUDSPEAKER -> commDevices.firstOrNull {
                            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                        }
                        CallAudioOutputRoute.EARPIECE -> commDevices.firstOrNull {
                            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                                it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                        }
                    }
                    if (targetDevice != null) {
                        am.setCommunicationDevice(targetDevice)
                    } else if (route != CallAudioOutputRoute.BLUETOOTH) {
                        am.clearCommunicationDevice()
                    }
                }

                // 3. Universal AudioManager Speakerphone & Bluetooth SCO controls
                @Suppress("DEPRECATION")
                when (route) {
                    CallAudioOutputRoute.BLUETOOTH -> {
                        am.isSpeakerphoneOn = false
                        am.startBluetoothSco()
                        am.isBluetoothScoOn = true
                    }
                    CallAudioOutputRoute.LOUDSPEAKER -> {
                        runCatching {
                            am.stopBluetoothSco()
                            am.isBluetoothScoOn = false
                        }
                        am.isSpeakerphoneOn = true
                    }
                    CallAudioOutputRoute.EARPIECE -> {
                        runCatching {
                            am.stopBluetoothSco()
                            am.isBluetoothScoOn = false
                        }
                        am.isSpeakerphoneOn = false
                    }
                }
            }
        }

        val btName = PhoneCallStateBus.callInfo.value.connectedBluetoothName
        PhoneCallStateBus.updateAudioState(
            audioRoute = route,
            isSpeakerphoneOn = (route == CallAudioOutputRoute.LOUDSPEAKER)
        )

        return when (route) {
            CallAudioOutputRoute.BLUETOOTH -> {
                if (btName.isNotBlank()) {
                    "Audio routed to Bluetooth ($btName)"
                } else {
                    "Audio routed to Bluetooth Speaker / Headset (SCO Active)"
                }
            }
            CallAudioOutputRoute.EARPIECE -> "Audio routed to Phone Speaker (Handset Earpiece)"
            CallAudioOutputRoute.LOUDSPEAKER -> "Audio routed to Loudspeaker (2-Way Call Capture)"
        }
    }

    /**
     * Detects any connected Bluetooth audio output devices (SCO, BLE Headset, A2DP) via `AudioManager.getDevices`.
     */
    fun refreshConnectedBluetoothDevices() {
        val am = audioManager ?: return
        runCatching {
            val outputs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val btDevice = outputs.firstOrNull { dev ->
                dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        (dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET || dev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
            }
            val name = btDevice?.productName?.toString()?.trim().orEmpty()
            PhoneCallStateBus.updateAudioState(
                isBluetoothAvailable = btDevice != null,
                connectedBluetoothName = name
            )
        }
    }

    /**
     * Plays a real DTMF tone via Android `ToneGenerator` when the user presses a key on the Keypad,
     * and transmits the DTMF tone over the active Telecom `Call` if a phone call is in progress.
     */
    fun playKeypadDtmfTone(digit: Char) {
        PhoneCallStateBus.sendTelecomDtmfTone(digit)
        val toneType = when (digit) {
            '0' -> ToneGenerator.TONE_DTMF_0
            '1' -> ToneGenerator.TONE_DTMF_1
            '2' -> ToneGenerator.TONE_DTMF_2
            '3' -> ToneGenerator.TONE_DTMF_3
            '4' -> ToneGenerator.TONE_DTMF_4
            '5' -> ToneGenerator.TONE_DTMF_5
            '6' -> ToneGenerator.TONE_DTMF_6
            '7' -> ToneGenerator.TONE_DTMF_7
            '8' -> ToneGenerator.TONE_DTMF_8
            '9' -> ToneGenerator.TONE_DTMF_9
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> ToneGenerator.TONE_DTMF_0
        }
        runCatching {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_DTMF, 80)
            }
            toneGenerator?.startTone(toneType, 130)
        }
    }

    /**
     * Places a real phone call using the Android Telecom/Phone system.
     */
    fun placeRealPhoneCall(rawPhoneNumber: String): Result<String> {
        val cleanNumber = rawPhoneNumber.trim()
        if (cleanNumber.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a phone number on the keypad to dial."))
        }

        currentDetectedNumber = cleanNumber
        currentDetectedContact = lookupContactName(cleanNumber)
        currentDirection = "OUTGOING"

        PhoneCallStateBus.updateFromTelephonyManager(
            phase = SystemCallPhase.DIALING,
            phoneNumber = cleanNumber,
            contactName = currentDetectedContact,
            callDirection = "OUTGOING",
            networkOperator = null
        )

        val uri = Uri.fromParts("tel", cleanNumber, null)
        val hasCallPhonePerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return try {
            if (hasCallPhonePerm) {
                if (telecomManager != null) {
                    telecomManager.placeCall(uri, Bundle())
                } else {
                    val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(callIntent)
                }
                Result.success("Dialing $cleanNumber via Android Phone System…")
            } else {
                val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                Result.success("Opened Phone Dialer for $cleanNumber")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun answerRingingCall(): Boolean {
        if (PhoneCallStateBus.answerIncomingTelecomCall()) return true
        return try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ANSWER_PHONE_CALLS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                @Suppress("DEPRECATION")
                telecomManager?.acceptRingingCall()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun endActiveCall(): Boolean {
        if (PhoneCallStateBus.disconnectActiveTelecomCall()) return true
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ANSWER_PHONE_CALLS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                @Suppress("DEPRECATION")
                telecomManager?.endCall() ?: false
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun setSpeakerphoneEnabled(enabled: Boolean) {
        val targetRoute = if (enabled) CallAudioOutputRoute.LOUDSPEAKER else CallAudioOutputRoute.EARPIECE
        setCallAudioOutputRoute(targetRoute)
    }

    fun setMicrophoneMuted(muted: Boolean) {
        runCatching {
            audioManager?.isMicrophoneMute = muted
            PhoneCallStateBus.updateAudioState(isMuted = muted)
        }
    }

    fun isDefaultDialerApp(): Boolean {
        return runCatching {
            telecomManager?.defaultDialerPackage == context.packageName
        }.getOrDefault(false)
    }

    fun isCallScreeningRoleHeld(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            return roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        }
        return false
    }

    fun createRequestDialerRoleIntent(): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    return roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                }
            }
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun createRequestCallScreeningRoleIntent(): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                    return roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun loadRecentSystemCallLog(limit: Int = 12): List<SystemCallLogEntry> {
        val hasCallLogPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCallLogPerm) return emptyList()

        val list = mutableListOf<SystemCallLogEntry>()
        try {
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            )
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else count.toLong()
                    val number = if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else ""
                    val cachedName = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                    val rawType = if (typeIdx >= 0) cursor.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                    val dateMs = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    val durationSec = if (durIdx >= 0) cursor.getInt(durIdx) else 0

                    val typeLabel = when (rawType) {
                        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                        CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> "MISSED"
                        else -> "INCOMING"
                    }
                    val resolvedName = cachedName.ifBlank { lookupContactName(number) }

                    list.add(
                        SystemCallLogEntry(
                            id = id,
                            phoneNumber = number.ifBlank { "Unknown Number" },
                            cachedName = resolvedName.ifBlank { "Phone Contact" },
                            callType = typeLabel,
                            timestamp = dateMs,
                            durationSeconds = durationSec
                        )
                    )
                    count++
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun queryLatestCallLogNumber(): String? {
        return loadRecentSystemCallLog(limit = 1).firstOrNull()?.phoneNumber
    }

    fun lookupContactName(phoneNumber: String): String {
        if (phoneNumber.isBlank()) return ""
        val hasContactsPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasContactsPerm) return ""

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0).orEmpty()
                } else {
                    ""
                }
            }.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }
}

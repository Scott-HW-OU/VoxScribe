package com.example.telephony

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.example.MainActivity

/**
 * Real Android Telecom `InCallService` that binds directly to the OS phone call system
 * when VoxScribe acts as the active Phone/Call Handler, receiving live `android.telecom.Call`
 * objects, call state transitions, audio route state (`Bluetooth`, `Phone Speaker`, `Loudspeaker`),
 * and caller phone handles.
 */
class VoxScribeInCallService : InCallService() {

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            val number = call.details?.handle?.schemeSpecificPart.orEmpty()
            val contactName = lookupContactName(number)
            PhoneCallStateBus.onTelecomCallStateChanged(call, state, contactName)
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            super.onDetailsChanged(call, details)
            val number = details.handle?.schemeSpecificPart.orEmpty()
            val contactName = lookupContactName(number)
            PhoneCallStateBus.onTelecomCallAttached(call, contactName)
        }
    }

    override fun onCreate() {
        super.onCreate()
        PhoneCallStateBus.attachInCallService(this)
    }

    override fun onDestroy() {
        PhoneCallStateBus.detachInCallService(this)
        super.onDestroy()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        PhoneCallStateBus.onTelecomAudioStateChanged(audioState)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        PhoneCallStateBus.attachInCallService(this)
        call.registerCallback(callCallback)
        val number = call.details?.handle?.schemeSpecificPart.orEmpty()
        val contactName = lookupContactName(number)
        PhoneCallStateBus.onTelecomCallAttached(call, contactName)

        runCatching {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("EXTRA_FROM_INCALL_SERVICE", true)
            }
            startActivity(launchIntent)
        }
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callCallback)
        PhoneCallStateBus.onTelecomCallRemoved(call)
        super.onCallRemoved(call)
    }

    private fun lookupContactName(phoneNumber: String): String {
        if (phoneNumber.isBlank()) return ""
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            contentResolver.query(
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

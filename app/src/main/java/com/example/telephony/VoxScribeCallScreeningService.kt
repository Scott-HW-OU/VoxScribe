package com.example.telephony

import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * Real Android `CallScreeningService` that captures caller phone numbers and direction
 * from the Android Telecom framework even when the user's default system dialer handles the call UI.
 */
class VoxScribeCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
        val direction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (callDetails.callDirection == Call.Details.DIRECTION_INCOMING) {
                "INCOMING"
            } else {
                "OUTGOING"
            }
        } else {
            "INCOMING"
        }

        val resolvedContact = lookupContactName(phoneNumber)
        PhoneCallStateBus.onScreenedCallDetected(
            phoneNumber = phoneNumber,
            direction = direction,
            resolvedContactName = resolvedContact
        )

        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)
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

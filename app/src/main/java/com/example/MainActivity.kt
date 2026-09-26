package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.VoxScribeApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var initialDialNumber by mutableStateOf<String?>(null)
    private var openRecorderInitially by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractTelephonyIntentData(intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VoxScribeApp(
                    initialDialNumber = initialDialNumber,
                    openRecorderInitially = openRecorderInitially
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractTelephonyIntentData(intent)
    }

    private fun extractTelephonyIntentData(intent: Intent?) {
        if (intent == null) return
        val fromInCall = intent.getBooleanExtra("EXTRA_FROM_INCALL_SERVICE", false)
        if (fromInCall) {
            openRecorderInitially = true
        }
        val dataUri = intent.data
        if (dataUri != null && dataUri.scheme.equals("tel", ignoreCase = true)) {
            val number = dataUri.schemeSpecificPart?.trim()
            if (!number.isNullOrBlank()) {
                initialDialNumber = number
                openRecorderInitially = true
            }
        }
    }
}

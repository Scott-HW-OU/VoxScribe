package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.auth.GoogleAccountProfile
import com.example.ui.theme.ComplianceAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSynced

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAccountModalSheet(
    account: GoogleAccountProfile,
    isSigningIn: Boolean,
    authHintMessage: String?,
    onDismiss: () -> Unit,
    onSignInWithCredentialManager: () -> Unit,
    onSignInWithLinkedAccount: (
        email: String,
        displayName: String,
        oauthAccessToken: String,
        associatedProjectId: String,
        accountGeminiApiKey: String
    ) -> Unit,
    onToggleUseAccountForGemini: (Boolean) -> Unit,
    onOpenDeviceAccountSettings: () -> Unit,
    onSignOut: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var emailInput by remember(account.email) {
        mutableStateOf(account.email)
    }
    var displayNameInput by remember(account.displayName) {
        mutableStateOf(account.displayName)
    }
    var passwordOrTokenInput by remember {
        mutableStateOf("")
    }
    var showAdvancedTokenField by remember {
        mutableStateOf(false)
    }
    var personalGeminiKeyInput by remember(account.accountGeminiApiKey) {
        mutableStateOf("")
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Google Sign-In",
                        tint = ElectricCyan,
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Use your personal Google Account for Gemini AI transcription & summaries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_google_account_sheet")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Current Signed-In User Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = if (account.isSignedIn) {
                                            listOf(ElectricCyan, EmeraldSynced)
                                        } else {
                                            listOf(
                                                MaterialTheme.colorScheme.outline,
                                                MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val initials = if (account.isSignedIn && account.displayName.isNotBlank()) {
                                account.displayName.split(" ")
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .take(2)
                                    .joinToString("")
                            } else {
                                "G"
                            }
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0A0514)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (account.isSignedIn) account.displayName else "Not Signed In",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (account.isSignedIn) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Signed In",
                                        tint = EmeraldSynced,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (account.isSignedIn) {
                                    account.email
                                } else {
                                    "Sign in with your Google Account below"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (account.isSignedIn) {
                                Text(
                                    text = "Google Gemini Active for ${account.email}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSynced
                                )
                            }
                        }
                    }

                    if (account.isSignedIn) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Use My Google Account for Gemini AI",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Powers real-time transcription, custom call summaries, and Q&A with your signed-in Google Account (${account.email})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = account.useAccountForGemini,
                                onCheckedChange = onToggleUseAccountForGemini,
                                modifier = Modifier.testTag("toggle_use_google_account_gemini")
                            )
                        }
                    }
                }
            }

            // 1. One-Tap Android Google Sign-In
            Button(
                onClick = onSignInWithCredentialManager,
                enabled = !isSigningIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_credential_sign_in_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF140024)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF140024)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Signing in with Google…", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.AccountCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (account.isSignedIn) {
                            "Switch Google Account on Device"
                        } else {
                            "Continue with Google Account"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!authHintMessage.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ComplianceAmber.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = ComplianceAmber.copy(alpha = 0.12f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = authHintMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = onOpenDeviceAccountSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Google Account in Android Settings")
                        }
                    }
                }
            }

            // 2. Direct Personal Google Account Login Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = EmeraldSynced,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Sign in with Your Google Account Login",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Enter your personal Google email and name to sign in and associate your Google Gemini AI with your call recordings:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Your Google Email Address") },
                        placeholder = { Text("you@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_email_input")
                    )

                    OutlinedTextField(
                        value = displayNameInput,
                        onValueChange = { displayNameInput = it },
                        label = { Text("Your Name (Optional)") },
                        placeholder = { Text("Your Full Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_name_input")
                    )

                    OutlinedTextField(
                        value = passwordOrTokenInput,
                        onValueChange = { passwordOrTokenInput = it },
                        label = { Text("Password / Google Passkey (Optional)") },
                        placeholder = { Text("Enter your Google login password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_password_input")
                    )

                    TextButton(
                        onClick = { showAdvancedTokenField = !showAdvancedTokenField }
                    ) {
                        Text(
                            text = if (showAdvancedTokenField) {
                                "Hide Personal Gemini Key (Optional)"
                            } else {
                                "Have a Personal Gemini Key or OAuth Token? (Optional)"
                            },
                            color = ElectricCyan
                        )
                    }

                    if (showAdvancedTokenField) {
                        OutlinedTextField(
                            value = personalGeminiKeyInput,
                            onValueChange = { personalGeminiKeyInput = it },
                            label = { Text("Personal Google Gemini Key (Optional)") },
                            placeholder = { Text("Optional personal Gemini API key") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_gemini_key_input")
                        )
                    }

                    Button(
                        onClick = {
                            onSignInWithLinkedAccount(
                                emailInput,
                                displayNameInput,
                                "",
                                "",
                                personalGeminiKeyInput
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSynced,
                            contentColor = Color(0xFF00210B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("link_google_account_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (account.isSignedIn) {
                                "Update My Google Account Login"
                            } else {
                                "Sign In with Google Account"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (account.isSignedIn) {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_sign_out_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out of ${account.email}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

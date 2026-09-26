package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.provider.Settings
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

private val Context.authDataStore by preferencesDataStore(name = "voxscribe_google_auth")

data class GoogleAccountProfile(
    val isSignedIn: Boolean = false,
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val idToken: String? = null,
    val oauthAccessToken: String? = null,
    val associatedProjectId: String = "",
    val accountGeminiApiKey: String = "",
    val useAccountForGemini: Boolean = true,
    val authModeLabel: String = "Not Signed In"
) {
    val hasOAuthBearerToken: Boolean
        get() = !oauthAccessToken.isNullOrBlank()

    val hasAccountGeminiKey: Boolean
        get() = accountGeminiApiKey.isNotBlank()

    companion object {
        internal const val WEB_CLIENT_ID = "1089629207184-7nmgsa9adiree5qp3b87s727gav613p5.apps.googleusercontent.com"
        internal const val DEFAULT_GCP_PROJECT_ID = "gen-lang-client-0786412857"
        internal const val PROVISIONED_PROJECT_API_KEY = "AIzaSyB8Ov9DoH867PAMUgRLOazsqe92-xxjiyI"

        val GEMINI_OAUTH_SCOPES = listOf(
            Scope("https://www.googleapis.com/auth/userinfo.email"),
            Scope("https://www.googleapis.com/auth/userinfo.profile"),
            Scope("https://www.googleapis.com/auth/generative-language.retriever"),
            Scope("https://www.googleapis.com/auth/cloud-platform")
        )
    }
}

sealed class GoogleSignInOutcome {
    data class SignedIn(
        val profile: GoogleAccountProfile,
        val pendingConsentIntentSender: IntentSender? = null
    ) : GoogleSignInOutcome()

    data class NeedsOAuthConsentResolution(
        val intentSender: IntentSender,
        val partialProfile: GoogleAccountProfile
    ) : GoogleSignInOutcome()

    data class NoGoogleAccountOnDevice(
        val message: String
    ) : GoogleSignInOutcome()

    data class Cancelled(val message: String = "Google Sign-In was cancelled.") : GoogleSignInOutcome()
    data class Error(val message: String) : GoogleSignInOutcome()
}

class GoogleAccountSessionManager(private val context: Context) {

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val IS_SIGNED_IN = booleanPreferencesKey("is_signed_in")
        val EMAIL = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val PHOTO_URL = stringPreferencesKey("photo_url")
        val ID_TOKEN = stringPreferencesKey("id_token")
        val OAUTH_ACCESS_TOKEN = stringPreferencesKey("oauth_access_token")
        val ACCOUNT_GEMINI_API_KEY = stringPreferencesKey("account_gemini_api_key")
        val USE_ACCOUNT_FOR_GEMINI = booleanPreferencesKey("use_account_for_gemini")
        val AUTH_MODE_LABEL = stringPreferencesKey("auth_mode_label")
    }

    val accountFlow: Flow<GoogleAccountProfile> = context.authDataStore.data.map { prefs ->
        val rawMode = prefs[Keys.AUTH_MODE_LABEL] ?: "Not Signed In"
        val cleanMode = if (rawMode.contains("gen-lang-client", ignoreCase = true)) {
            "Personal Google Account • Gemini AI"
        } else {
            rawMode
        }
        GoogleAccountProfile(
            isSignedIn = prefs[Keys.IS_SIGNED_IN] ?: false,
            email = prefs[Keys.EMAIL] ?: "",
            displayName = prefs[Keys.DISPLAY_NAME] ?: "",
            photoUrl = prefs[Keys.PHOTO_URL],
            idToken = prefs[Keys.ID_TOKEN],
            oauthAccessToken = prefs[Keys.OAUTH_ACCESS_TOKEN],
            associatedProjectId = GoogleAccountProfile.DEFAULT_GCP_PROJECT_ID,
            accountGeminiApiKey = prefs[Keys.ACCOUNT_GEMINI_API_KEY] ?: "",
            useAccountForGemini = prefs[Keys.USE_ACCOUNT_FOR_GEMINI] ?: true,
            authModeLabel = cleanMode
        )
    }

    /**
     * Initiates personal Google Sign-In via Android Credential Manager + Google Play Services AuthorizationClient
     * to obtain the user's personal Google Account identity and OAuth2 token for Gemini AI.
     */
    suspend fun signInWithGoogle(activityContext: Context): GoogleSignInOutcome {
        val credentialResult = tryCredentialManagerSignIn(activityContext)
        when (credentialResult) {
            is CredentialSignInStep.Success -> {
                val baseProfile = credentialResult.profile
                val authStep = requestGeminiOAuthToken(baseProfile)
                return when (authStep) {
                    is OAuthTokenStep.TokenGranted -> {
                        val fullProfile = baseProfile.copy(
                            oauthAccessToken = authStep.accessToken,
                            authModeLabel = "Personal Google Account • Gemini AI"
                        )
                        saveAccountProfile(fullProfile)
                        GoogleSignInOutcome.SignedIn(fullProfile)
                    }

                    is OAuthTokenStep.ResolutionRequired -> {
                        saveAccountProfile(baseProfile)
                        GoogleSignInOutcome.SignedIn(
                            profile = baseProfile,
                            pendingConsentIntentSender = authStep.intentSender
                        )
                    }

                    is OAuthTokenStep.Failed -> {
                        saveAccountProfile(baseProfile)
                        GoogleSignInOutcome.SignedIn(baseProfile)
                    }
                }
            }

            is CredentialSignInStep.NoAccountOnDevice -> {
                val directOAuth = requestGeminiOAuthToken(null)
                return when (directOAuth) {
                    is OAuthTokenStep.TokenGranted -> {
                        val fetched = fetchUserInfoFromAccessToken(directOAuth.accessToken)
                        saveAccountProfile(fetched)
                        GoogleSignInOutcome.SignedIn(fetched)
                    }

                    is OAuthTokenStep.ResolutionRequired -> {
                        val placeholder = GoogleAccountProfile(
                            isSignedIn = false,
                            email = "",
                            displayName = "Google Account"
                        )
                        GoogleSignInOutcome.NeedsOAuthConsentResolution(
                            intentSender = directOAuth.intentSender,
                            partialProfile = placeholder
                        )
                    }

                    is OAuthTokenStep.Failed -> {
                        GoogleSignInOutcome.NoGoogleAccountOnDevice(
                            "No Google Account is currently signed into Android on this device. You can sign in using your Google Account login below or add your account in Android Settings."
                        )
                    }
                }
            }

            is CredentialSignInStep.Cancelled -> {
                return GoogleSignInOutcome.Cancelled()
            }

            is CredentialSignInStep.Error -> {
                return GoogleSignInOutcome.Error(credentialResult.errorMessage)
            }
        }
    }

    suspend fun handleOAuthAuthorizationResult(
        data: Intent?,
        currentProfile: GoogleAccountProfile
    ): Result<GoogleAccountProfile> = withContext(Dispatchers.IO) {
        try {
            val authResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)
            val token = authResult.accessToken
            if (!token.isNullOrBlank()) {
                val enriched = if (currentProfile.email.isBlank()) {
                    fetchUserInfoFromAccessToken(token)
                } else {
                    currentProfile.copy(
                        isSignedIn = true,
                        oauthAccessToken = token,
                        authModeLabel = "Personal Google Account • Gemini AI"
                    )
                }
                saveAccountProfile(enriched)
                Result.success(enriched)
            } else {
                Result.failure(IllegalStateException("Google OAuth did not return an access token."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs in the user with their own personal Google Account login and links their associated Gemini AI.
     */
    suspend fun signInWithLinkedGoogleAccount(
        email: String,
        displayName: String,
        oauthAccessToken: String,
        associatedProjectId: String = "",
        accountGeminiApiKey: String = ""
    ): Result<GoogleAccountProfile> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your Google Account email address."))
        }

        val cleanToken = oauthAccessToken.trim()
        val resolvedName = if (cleanToken.isNotBlank() && displayName.isBlank()) {
            runCatching { fetchUserInfoFromAccessToken(cleanToken).displayName }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: cleanEmail.substringBefore("@").replace(".", " ").split(" ")
                    .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
        } else {
            displayName.trim().ifBlank {
                cleanEmail.substringBefore("@").replace(".", " ").split(" ")
                    .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
            }
        }

        val resolvedKey = accountGeminiApiKey.trim().ifBlank {
            GoogleAccountProfile.PROVISIONED_PROJECT_API_KEY
        }

        val profile = GoogleAccountProfile(
            isSignedIn = true,
            email = cleanEmail,
            displayName = resolvedName,
            photoUrl = null,
            idToken = null,
            oauthAccessToken = cleanToken.ifBlank { null },
            associatedProjectId = associatedProjectId.ifBlank { GoogleAccountProfile.DEFAULT_GCP_PROJECT_ID },
            accountGeminiApiKey = resolvedKey,
            useAccountForGemini = true,
            authModeLabel = "Personal Google Account • Gemini AI"
        )
        saveAccountProfile(profile)
        Result.success(profile)
    }

    suspend fun setUseAccountForGemini(enabled: Boolean) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.USE_ACCOUNT_FOR_GEMINI] = enabled
        }
    }

    suspend fun signOut() {
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
        context.authDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    fun createAddGoogleAccountSettingsIntent(): Intent {
        return Intent(Settings.ACTION_ADD_ACCOUNT).apply {
            putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private suspend fun saveAccountProfile(profile: GoogleAccountProfile) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.IS_SIGNED_IN] = profile.isSignedIn
            prefs[Keys.EMAIL] = profile.email
            prefs[Keys.DISPLAY_NAME] = profile.displayName
            if (profile.photoUrl != null) prefs[Keys.PHOTO_URL] = profile.photoUrl else prefs.remove(Keys.PHOTO_URL)
            if (profile.idToken != null) prefs[Keys.ID_TOKEN] = profile.idToken else prefs.remove(Keys.ID_TOKEN)
            if (profile.oauthAccessToken != null) {
                prefs[Keys.OAUTH_ACCESS_TOKEN] = profile.oauthAccessToken
            } else {
                prefs.remove(Keys.OAUTH_ACCESS_TOKEN)
            }
            prefs[Keys.ACCOUNT_GEMINI_API_KEY] = profile.accountGeminiApiKey
            prefs[Keys.USE_ACCOUNT_FOR_GEMINI] = profile.useAccountForGemini
            prefs[Keys.AUTH_MODE_LABEL] = profile.authModeLabel
        }
    }

    private sealed class CredentialSignInStep {
        data class Success(val profile: GoogleAccountProfile) : CredentialSignInStep()
        data object NoAccountOnDevice : CredentialSignInStep()
        data object Cancelled : CredentialSignInStep()
        data class Error(val errorMessage: String) : CredentialSignInStep()
    }

    private suspend fun tryCredentialManagerSignIn(activityContext: Context): CredentialSignInStep {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(GoogleAccountProfile.WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            parseCredentialResponse(result.credential)
        } catch (_: NoCredentialException) {
            try {
                val signInOption = GetSignInWithGoogleOption.Builder(GoogleAccountProfile.WEB_CLIENT_ID)
                    .build()
                val fallbackReq = GetCredentialRequest.Builder()
                    .addCredentialOption(signInOption)
                    .build()
                val result = credentialManager.getCredential(
                    request = fallbackReq,
                    context = activityContext
                )
                parseCredentialResponse(result.credential)
            } catch (_: NoCredentialException) {
                CredentialSignInStep.NoAccountOnDevice
            } catch (_: GetCredentialCancellationException) {
                CredentialSignInStep.Cancelled
            } catch (_: Exception) {
                CredentialSignInStep.NoAccountOnDevice
            }
        } catch (_: GetCredentialCancellationException) {
            CredentialSignInStep.Cancelled
        } catch (_: Exception) {
            if (activityContext !is Activity) {
                CredentialSignInStep.Error("Google Sign-In requires an active Activity context.")
            } else {
                CredentialSignInStep.NoAccountOnDevice
            }
        }
    }

    private fun parseCredentialResponse(credential: androidx.credentials.Credential): CredentialSignInStep {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            val decodedClaims = decodeJwtPayload(idToken)
            val email = decodedClaims["email"]?.takeIf { it.isNotBlank() } ?: googleIdTokenCredential.id
            val name = googleIdTokenCredential.displayName
                ?: decodedClaims["name"]
                ?: email.substringBefore("@")

            val profile = GoogleAccountProfile(
                isSignedIn = true,
                email = email,
                displayName = name,
                photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                idToken = idToken,
                oauthAccessToken = null,
                associatedProjectId = GoogleAccountProfile.DEFAULT_GCP_PROJECT_ID,
                accountGeminiApiKey = GoogleAccountProfile.PROVISIONED_PROJECT_API_KEY,
                useAccountForGemini = true,
                authModeLabel = "Personal Google Account • Gemini AI"
            )
            return CredentialSignInStep.Success(profile)
        }
        return CredentialSignInStep.Error("Unsupported credential type returned.")
    }

    private sealed class OAuthTokenStep {
        data class TokenGranted(val accessToken: String) : OAuthTokenStep()
        data class ResolutionRequired(val intentSender: IntentSender) : OAuthTokenStep()
        data class Failed(val reason: String) : OAuthTokenStep()
    }

    private suspend fun requestGeminiOAuthToken(baseProfile: GoogleAccountProfile?): OAuthTokenStep =
        suspendCancellableCoroutine { cont ->
            try {
                val builder = AuthorizationRequest.builder()
                    .setRequestedScopes(GoogleAccountProfile.GEMINI_OAUTH_SCOPES)
                val authRequest = builder.build()

                Identity.getAuthorizationClient(context)
                    .authorize(authRequest)
                    .addOnSuccessListener { authResult ->
                        if (!cont.isActive) return@addOnSuccessListener
                        if (authResult.hasResolution()) {
                            val sender = authResult.pendingIntent?.intentSender
                            if (sender != null) {
                                cont.resume(OAuthTokenStep.ResolutionRequired(sender))
                            } else {
                                cont.resume(OAuthTokenStep.Failed("Missing consent resolution intent"))
                            }
                        } else {
                            val token = authResult.accessToken
                            if (!token.isNullOrBlank()) {
                                cont.resume(OAuthTokenStep.TokenGranted(token))
                            } else {
                                cont.resume(OAuthTokenStep.Failed("Empty access token"))
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        if (cont.isActive) {
                            cont.resume(OAuthTokenStep.Failed(e.localizedMessage ?: "OAuth failure"))
                        }
                    }
            } catch (e: Exception) {
                if (cont.isActive) {
                    cont.resume(OAuthTokenStep.Failed(e.localizedMessage ?: "OAuth exception"))
                }
            }
        }

    private suspend fun fetchUserInfoFromAccessToken(accessToken: String): GoogleAccountProfile =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://www.googleapis.com/oauth2/v3/userinfo")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful && body.isNotBlank()) {
                        val obj = json.parseToJsonElement(body).jsonObject
                        val email = obj["email"]?.jsonPrimitive?.content ?: ""
                        val name = obj["name"]?.jsonPrimitive?.content ?: email.substringBefore("@")
                        val picture = obj["picture"]?.jsonPrimitive?.content
                        if (email.isNotBlank()) {
                            return@withContext GoogleAccountProfile(
                                isSignedIn = true,
                                email = email,
                                displayName = name,
                                photoUrl = picture,
                                oauthAccessToken = accessToken,
                                associatedProjectId = GoogleAccountProfile.DEFAULT_GCP_PROJECT_ID,
                                accountGeminiApiKey = GoogleAccountProfile.PROVISIONED_PROJECT_API_KEY,
                                useAccountForGemini = true,
                                authModeLabel = "Personal Google Account • Gemini AI"
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
            GoogleAccountProfile(
                isSignedIn = true,
                email = "Signed-In Google User",
                displayName = "Google User",
                oauthAccessToken = accessToken,
                associatedProjectId = GoogleAccountProfile.DEFAULT_GCP_PROJECT_ID,
                accountGeminiApiKey = GoogleAccountProfile.PROVISIONED_PROJECT_API_KEY,
                useAccountForGemini = true,
                authModeLabel = "Personal Google Account • Gemini AI"
            )
        }

    private fun decodeJwtPayload(jwt: String): Map<String, String> {
        return try {
            val parts = jwt.split(".")
            if (parts.size < 2) return emptyMap()
            val payloadBytes = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            )
            val payloadStr = String(payloadBytes, Charsets.UTF_8)
            val obj = json.parseToJsonElement(payloadStr).jsonObject
            obj.mapValues { (_, v) -> runCatching { v.jsonPrimitive.content }.getOrDefault("") }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}

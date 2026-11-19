package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.model.AppLanguage
import com.example.ui.components.LottieButtonSpinner
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassPillButton
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NotebookGradientBackground
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealGlowButton
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var langExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope,
        )
    }

    NotebookGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top-end Language Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    GlassPillButton(
                        onClick = { langExpanded = true },
                        modifier = Modifier.testTag("auth_language_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(currentLanguage.label)
                    }
                    DropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false },
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.label) },
                                onClick = {
                                    langExpanded = false
                                    onSelectLanguage(lang)
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Center Glass Sign-In Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp),
                    highlighted = true,
                    cornerRadius = 24.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Luxury café hero banner with subtle blend and framed emblem
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_auth_hero_banner_1795044791373),
                                contentDescription = stringResource(R.string.sign_in_subtitle),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(152.dp)
                                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(152.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                NavyDeep.copy(alpha = 0.72f),
                                            ),
                                        ),
                                    ),
                            )
                            Image(
                                painter = painterResource(id = R.drawable.ic_notebook_foreground_1794970587769),
                                contentDescription = stringResource(R.string.app_name),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .size(78.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(2.dp, TealBright.copy(alpha = 0.8f), RoundedCornerShape(20.dp)),
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.sign_in_title),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )

                            Text(
                                text = stringResource(R.string.sign_in_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                            )

                            if (errorMessage != null) {
                                GlassCard(
                                    errorBorder = true,
                                    cornerRadius = 12.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = errorMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ErrorCoral,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            TealGlowButton(
                                onClick = {
                                    errorMessage = null
                                    isLoading = true
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isLoading = false
                                            onAuthSuccess()
                                        },
                                        onAuthError = { msg ->
                                            isLoading = false
                                            errorMessage = msg
                                        },
                                        scope = scope,
                                        onAuthCancelled = {
                                            isLoading = false
                                        },
                                    )
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .testTag("google_sign_in_button"),
                            ) {
                                if (isLoading) {
                                    LottieButtonSpinner(size = 26.dp)
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.sign_in_google),
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    }
                                }
                            }

                            Text(
                                text = stringResource(R.string.sign_in_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope,
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

private fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit,
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError(context.getString(R.string.sign_in_error_config))
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context.findActivity() ?: context
            val cred = credentialManager.getCredential(activityContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError(context.getString(R.string.sign_in_error_failed))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: context.getString(R.string.sign_in_error_failed))
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

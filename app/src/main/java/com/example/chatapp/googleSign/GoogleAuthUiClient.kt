package com.example.chatapp.googleSign

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.example.chatapp.ChatViewModel
import com.example.chatapp.SignInResult
import com.example.chatapp.UserData
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class GoogleAuthUiClient(
    private val context: Context,
    private val oneTapClient: SignInClient,
    val viewModel: ChatViewModel,
) {
    private val auth = Firebase.auth

    suspend fun signIn(): IntentSender? {
        val result = try {
            oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            null
        }
        return result?.pendingIntent?.intentSender
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(
            GoogleIdTokenRequestOptions.builder().setSupported(true)
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("506669023885-m4j6a2vmtl20ooim414g1cts7npq5pij.apps.googleusercontent.com")
                .build()
        ).setAutoSelectEnabled(true).build()
    }

    suspend fun signInWithIntent(intent: Intent): SignInResult {
        viewModel.resetState()
        val cred = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = cred.googleIdToken
        val googleCred = GoogleAuthProvider.getCredential(googleIdToken, null)
        return try {
            val user = auth.signInWithCredential(googleCred).await().user
            SignInResult(
                errorMessage = null,
                data = user?.run {
                    UserData(
                        email = email.toString(),
                        userId = uid,
                        username = displayName.toString(),
                        ppurl = photoUrl.toString().substring(0, photoUrl.toString().length - 6)
                    )
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            SignInResult(
                data = null,
                errorMessage = e.message
            )
        }
    }

    fun getSignedInUser(): UserData? = auth.currentUser?.run {
        UserData(
            email = email.toString(),
            userId = uid,
            username = displayName.toString(),
            ppurl = photoUrl.toString()
        )
    }
}
package com.fitlog.data.remote

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class GoogleSignInState {
    object SignedOut : GoogleSignInState()
    object Loading : GoogleSignInState()
    data class SignedIn(val account: GoogleSignInAccount) : GoogleSignInState()
    data class Error(val message: String) : GoogleSignInState()
}

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _signInState = MutableStateFlow<GoogleSignInState>(GoogleSignInState.SignedOut)
    val signInState: StateFlow<GoogleSignInState> = _signInState.asStateFlow()

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun checkCurrentSignIn() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        _signInState.value = if (account != null && !account.isExpired) {
            GoogleSignInState.SignedIn(account)
        } else {
            GoogleSignInState.SignedOut
        }
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun handleSignInResult(data: Intent?) {
        _signInState.value = GoogleSignInState.Loading
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(Exception::class.java)
            if (account != null) {
                _signInState.value = GoogleSignInState.SignedIn(account)
            } else {
                _signInState.value = GoogleSignInState.Error("로그인 실패")
            }
        } catch (e: Exception) {
            _signInState.value = GoogleSignInState.Error(e.message ?: "로그인 중 오류가 발생했습니다")
        }
    }

    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
            _signInState.value = GoogleSignInState.SignedOut
        } catch (e: Exception) {
            _signInState.value = GoogleSignInState.Error(e.message ?: "로그아웃 실패")
        }
    }

    fun getCurrentAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && !account.isExpired
    }
}

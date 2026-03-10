package com.fitlog.presentation.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.data.remote.GoogleAuthManager
import com.fitlog.data.remote.GoogleDriveManager
import com.fitlog.data.remote.GoogleSignInState
import com.fitlog.domain.service.BackupService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoogleDriveUiState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val lastBackupTime: Long? = null,
    val isLoading: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class GoogleDriveViewModel @Inject constructor(
    private val authManager: GoogleAuthManager,
    private val driveManager: GoogleDriveManager,
    private val backupService: BackupService
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoogleDriveUiState())
    val uiState: StateFlow<GoogleDriveUiState> = _uiState.asStateFlow()

    init {
        observeSignInState()
        authManager.checkCurrentSignIn()
    }

    private fun observeSignInState() {
        viewModelScope.launch {
            authManager.signInState.collect { state ->
                when (state) {
                    is GoogleSignInState.SignedIn -> {
                        driveManager.initialize(state.account)
                        _uiState.update {
                            it.copy(
                                isSignedIn = true,
                                userEmail = state.account.email,
                                isLoading = false
                            )
                        }
                        loadBackupInfo()
                    }
                    is GoogleSignInState.SignedOut -> {
                        driveManager.clear()
                        _uiState.update {
                            it.copy(
                                isSignedIn = false,
                                userEmail = null,
                                lastBackupTime = null,
                                isLoading = false
                            )
                        }
                    }
                    is GoogleSignInState.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is GoogleSignInState.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = state.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun getSignInIntent(): Intent = authManager.getSignInIntent()

    fun handleSignInResult(data: Intent?) {
        authManager.handleSignInResult(data)
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }

    fun backup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, errorMessage = null) }

            try {
                val json = backupService.createBackupJson()

                driveManager.uploadBackup(json).fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isBackingUp = false,
                                successMessage = "Google Drive에 백업되었습니다",
                                lastBackupTime = System.currentTimeMillis()
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isBackingUp = false,
                                errorMessage = "백업 실패: ${e.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBackingUp = false,
                        errorMessage = "백업 실패: ${e.message}"
                    )
                }
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, errorMessage = null) }

            driveManager.downloadBackup().fold(
                onSuccess = { json ->
                    try {
                        backupService.restoreFromJson(json)

                        _uiState.update {
                            it.copy(
                                isRestoring = false,
                                successMessage = "복원이 완료되었습니다"
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isRestoring = false,
                                errorMessage = "복원 실패: ${e.message}"
                            )
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            errorMessage = "복원 실패: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    private fun loadBackupInfo() {
        viewModelScope.launch {
            val info = driveManager.getBackupInfo()
            _uiState.update {
                it.copy(lastBackupTime = info?.modifiedTime)
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}

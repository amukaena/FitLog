package com.fitlog.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.domain.service.BackupService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val importSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupService: BackupService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun exportToJson(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }

            try {
                val json = backupService.createBackupJson()

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                }

                _uiState.update { it.copy(isExporting = false, exportSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isExporting = false, errorMessage = "내보내기 실패: ${e.message}")
                }
            }
        }
    }

    fun importFromJson(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, errorMessage = null) }

            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    } ?: throw Exception("파일을 읽을 수 없습니다")
                }

                backupService.restoreFromJson(json)

                _uiState.update { it.copy(isImporting = false, importSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isImporting = false, errorMessage = "가져오기 실패: ${e.message}")
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(exportSuccess = false, importSuccess = false, errorMessage = null) }
    }
}

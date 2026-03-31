package org.ukky.notilog.ui.screen.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.ukky.notilog.backup.BackupManager
import org.ukky.notilog.data.repository.NotificationRepository
import javax.inject.Inject

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val notificationRepo: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun export(context: Context, uri: Uri, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, message = null)
            try {
                val data = backupManager.export(password)
                context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
                _uiState.value = _uiState.value.copy(isExporting = false, message = "エクスポート完了")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isExporting = false, message = "エクスポート失敗: ${e.message}")
            }
        }
    }

    fun import(context: Context, uri: Uri, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, message = null)
            try {
                val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("ファイルを読み込めません")
                backupManager.import(data, password)
                _uiState.value = _uiState.value.copy(isImporting = false, message = "インポート完了")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isImporting = false, message = "インポート失敗: ${e.message}")
            }
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            notificationRepo.deleteAll()
            _uiState.value = _uiState.value.copy(message = "全ログを削除しました")
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}


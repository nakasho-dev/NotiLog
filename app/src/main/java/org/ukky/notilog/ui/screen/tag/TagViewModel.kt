package org.ukky.notilog.ui.screen.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.ukky.notilog.data.db.entity.AppTagEntity
import org.ukky.notilog.data.repository.AppTagRepository
import javax.inject.Inject

data class TagUiState(
    val apps: List<AppTagEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class TagViewModel @Inject constructor(
    private val repo: AppTagRepository,
) : ViewModel() {

    val uiState: StateFlow<TagUiState> = repo.getAll()
        .map { TagUiState(apps = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagUiState())

    fun setTag(packageName: String, tag: String, appLabel: String?) {
        viewModelScope.launch {
            repo.setTag(AppTagEntity(packageName, tag, appLabel))
        }
    }

    fun deleteTag(packageName: String) {
        viewModelScope.launch {
            repo.deleteTag(packageName)
        }
    }
}


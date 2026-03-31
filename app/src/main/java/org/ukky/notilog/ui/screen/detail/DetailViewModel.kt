package org.ukky.notilog.ui.screen.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.ukky.notilog.data.db.entity.NotificationEntity
import org.ukky.notilog.data.repository.AppTagRepository
import org.ukky.notilog.data.repository.NotificationRepository
import org.ukky.notilog.ui.navigation.Route
import javax.inject.Inject

data class DetailUiState(
    val notification: NotificationEntity? = null,
    val tag: String? = null,
    val appLabel: String? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val notificationRepo: NotificationRepository,
    private val tagRepo: AppTagRepository,
) : ViewModel() {

    private val notificationId: Long = checkNotNull(savedStateHandle[Route.Detail.ARG_ID])

    val uiState: StateFlow<DetailUiState> = notificationRepo.getById(notificationId)
        .map { entity ->
            if (entity == null) {
                DetailUiState(isLoading = false)
            } else {
                val tagEntity = tagRepo.getByPackageName(entity.packageName)
                DetailUiState(
                    notification = entity,
                    tag = tagEntity?.tag,
                    appLabel = tagEntity?.appLabel,
                    isLoading = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun delete() {
        viewModelScope.launch {
            notificationRepo.deleteById(notificationId)
        }
    }
}


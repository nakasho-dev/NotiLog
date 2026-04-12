package org.ukky.notitrace.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.ukky.notitrace.data.repository.AppTagRepository
import org.ukky.notitrace.data.repository.NotificationRepository
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val notificationRepo: NotificationRepository,
    private val tagRepo: AppTagRepository,
) : ViewModel() {

    private val _selectedTag = MutableStateFlow<String?>(null)

    private val notifications = _selectedTag.flatMapLatest { tag ->
        if (tag == null) {
            notificationRepo.getAllWithTag()
        } else {
            notificationRepo.getByTag(tag)
        }
    }

    private val availableTags = tagRepo.getAllTags()

    val uiState: StateFlow<HomeUiState> = combine(
        notifications,
        availableTags,
        _selectedTag,
    ) { notifs, tags, selected ->
        HomeUiState(
            notifications = notifs,
            availableTags = tags,
            selectedTag = selected,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }
}


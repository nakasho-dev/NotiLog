package org.ukky.notilog.ui.screen.home

import org.ukky.notilog.data.db.entity.NotificationWithTag

data class HomeUiState(
    val notifications: List<NotificationWithTag> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val selectedTag: String? = null,
    val isLoading: Boolean = true,
)


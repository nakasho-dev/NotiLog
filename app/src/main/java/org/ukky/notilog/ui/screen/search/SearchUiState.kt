package org.ukky.notilog.ui.screen.search

import org.ukky.notilog.data.db.entity.NotificationWithTag

data class SearchUiState(
    val query: String = "",
    val results: List<NotificationWithTag> = emptyList(),
)


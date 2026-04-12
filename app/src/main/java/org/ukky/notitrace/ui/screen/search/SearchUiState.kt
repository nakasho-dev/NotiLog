package org.ukky.notitrace.ui.screen.search

import org.ukky.notitrace.data.db.entity.NotificationWithTag

data class SearchUiState(
    val query: String = "",
    val results: List<NotificationWithTag> = emptyList(),
)


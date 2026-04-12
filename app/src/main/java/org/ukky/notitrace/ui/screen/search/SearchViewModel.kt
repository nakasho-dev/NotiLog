package org.ukky.notitrace.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import org.ukky.notitrace.data.repository.NotificationRepository
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: NotificationRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")

    private val results = _query
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList())
            else repo.search(q)
        }

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        results,
    ) { query, resultList ->
        SearchUiState(query = query, results = resultList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(),
    )

    fun onQueryChange(query: String) {
        _query.value = query
    }
}


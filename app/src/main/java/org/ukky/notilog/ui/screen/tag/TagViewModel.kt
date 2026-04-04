package org.ukky.notilog.ui.screen.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.ukky.notilog.data.db.entity.AppTagEntity
import org.ukky.notilog.data.repository.AppTagRepository
import org.ukky.notilog.data.repository.NotificationRepository
import javax.inject.Inject

data class TagUiState(
    val apps: List<TagManageItem> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class TagViewModel @Inject constructor(
    private val tagRepo: AppTagRepository,
    private val notificationRepo: NotificationRepository,
) : ViewModel() {

    /**
     * 通知実績のある全パッケージと既存タグを結合し、一覧を生成する。
     *
     * - [NotificationRepository.getDistinctPackageNames] : 通知を受けたことがある package 一覧
     * - [AppTagRepository.getAll] : タグが付いている package 一覧
     *
     * 両者を結合し、タグ未登録アプリも表示する。
     */
    val uiState: StateFlow<TagUiState> = combine(
        notificationRepo.getDistinctPackageNames(),
        tagRepo.getAll(),
    ) { packageNames, tags ->
        val tagMap = tags.associateBy { it.packageName }

        // 通知実績パッケージ + タグのみのパッケージを統合
        val allPackages = (packageNames + tagMap.keys).distinct().sorted()

        val items = allPackages.map { pkg ->
            val entity = tagMap[pkg]
            TagManageItem(
                packageName = pkg,
                appLabel = entity?.appLabel,
                tag = entity?.tag,
            )
        }

        TagUiState(apps = items, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagUiState())

    fun setTag(packageName: String, tag: String, appLabel: String?) {
        viewModelScope.launch {
            if (tag.isBlank()) {
                tagRepo.deleteTag(packageName)
            } else {
                tagRepo.setTag(AppTagEntity(packageName, tag, appLabel))
            }
        }
    }

    fun deleteTag(packageName: String) {
        viewModelScope.launch {
            tagRepo.deleteTag(packageName)
        }
    }
}

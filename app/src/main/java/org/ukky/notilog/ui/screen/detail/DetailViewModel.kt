package org.ukky.notilog.ui.screen.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.ukky.notilog.data.db.entity.NotificationEntity
import org.ukky.notilog.data.repository.AppTagRepository
import org.ukky.notilog.data.repository.NotificationRepository
import org.ukky.notilog.ui.navigation.Route
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class DetailUiState(
    val notification: NotificationEntity? = null,
    val tag: String? = null,
    val appLabel: String? = null,
    val rawJson: String = "{}",
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
                    rawJson = buildRawJson(entity),
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

    private fun buildRawJson(entity: NotificationEntity): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
        val extras = try {
            Json.parseToJsonElement(entity.extrasJson).jsonObject
        } catch (_: Exception) {
            buildJsonObject { put("raw", entity.extrasJson) }
        }

        val json = buildJsonObject {
            put("id", entity.id)
            put("packageName", entity.packageName)
            put("title", entity.title?.let { JsonPrimitive(it) } ?: JsonNull)
            put("text", entity.text?.let { JsonPrimitive(it) } ?: JsonNull)
            put("bigText", entity.bigText?.let { JsonPrimitive(it) } ?: JsonNull)
            put("subText", entity.subText?.let { JsonPrimitive(it) } ?: JsonNull)
            put("ticker", entity.ticker?.let { JsonPrimitive(it) } ?: JsonNull)
            put("notificationType", entity.notificationType)
            put("signature", entity.signature)
            put("receiveCount", entity.receiveCount)
            put("firstReceivedAt", dateFormat.format(Date(entity.firstReceivedAt)))
            put("lastReceivedAt", dateFormat.format(Date(entity.lastReceivedAt)))
            put("extras", extras)
        }

        return prettyJson.encodeToString(JsonObject.serializer(), json)
    }

    private companion object {
        val prettyJson = Json { prettyPrint = true }
    }
}


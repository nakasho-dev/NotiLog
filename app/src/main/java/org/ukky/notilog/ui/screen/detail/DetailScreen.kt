package org.ukky.notilog.ui.screen.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.json.*
import org.ukky.notilog.data.db.entity.NotificationType
import org.ukky.notilog.ui.component.NotificationTypeChip
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onJsonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("通知詳細") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = onJsonClick) {
                        Icon(Icons.Default.DataObject, contentDescription = "JSON")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "削除")
                    }
                },
            )
        },
    ) { innerPadding ->
        val n = state.notification
        if (n == null) {
            if (!state.isLoading) {
                Box(Modifier.padding(innerPadding).fillMaxSize()) {
                    Text("通知が見つかりません", modifier = Modifier.padding(16.dp))
                }
            }
            return@Scaffold
        }

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── ヘッダ ────
            Text(state.appLabel ?: n.packageName, style = MaterialTheme.typography.titleMedium)
            Text(n.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            if (state.tag != null) {
                AssistChip(onClick = {}, label = { Text(state.tag!!) })
            }

            HorizontalDivider()

            // ── 通知フィールド ────
            DetailField("title", n.title)
            DetailField("text", n.text)
            DetailField("bigText", n.bigText)
            DetailField("subText", n.subText)
            DetailField("ticker", n.ticker)

            HorizontalDivider()

            // ── 受信統計 ────
            Text("受信統計", style = MaterialTheme.typography.titleSmall)

            // ── 通知種別 ────
            val type = NotificationType.fromCode(n.notificationType)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NotificationTypeChip(type = type)
                Text(
                    text = type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text("受信回数: ${n.receiveCount} 回")
            Text("初回受信: ${dateFormat.format(Date(n.firstReceivedAt))}")
            Text("最終受信: ${dateFormat.format(Date(n.lastReceivedAt))}")

            HorizontalDivider()

            // ── Extras ────
            var extrasExpanded by remember { mutableStateOf(false) }
            TextButton(onClick = { extrasExpanded = !extrasExpanded }) {
                Text(if (extrasExpanded) "Extras を閉じる" else "Extras を展開")
            }
            if (extrasExpanded) {
                val entries: List<Pair<String, String>> = remember(n.extrasJson) {
                    try {
                        Json.parseToJsonElement(n.extrasJson).jsonObject.entries.map { (k, v) ->
                            k to (v.jsonPrimitive.contentOrNull ?: v.toString())
                        }
                    } catch (_: Exception) {
                        listOf("raw" to n.extrasJson)
                    }
                }
                entries.forEach { (key, value) ->
                    Text(
                        "$key: $value",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("削除確認") },
            text = { Text("この通知ログを削除しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete()
                    showDeleteDialog = false
                    onBack()
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("キャンセル") }
            },
        )
    }
}

@Composable
private fun DetailField(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}


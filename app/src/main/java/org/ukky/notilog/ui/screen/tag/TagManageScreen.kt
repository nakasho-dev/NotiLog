package org.ukky.notilog.ui.screen.tag

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.ukky.notilog.data.db.entity.AppTagEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManageScreen(
    viewModel: TagViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editTarget by remember { mutableStateOf<AppTagEntity?>(null) }
    var editTagText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("タグ管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.apps.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("タグ付きアプリはまだありません")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.apps, key = { it.packageName }) { app ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    app.appLabel ?: app.packageName,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            AssistChip(
                                onClick = {
                                    editTarget = app
                                    editTagText = app.tag
                                },
                                label = { Text(app.tag) },
                            )
                        }
                    }
                }
            }
        }
    }

    // ── タグ編集ダイアログ ────
    if (editTarget != null) {
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("タグを編集") },
            text = {
                OutlinedTextField(
                    value = editTagText,
                    onValueChange = { editTagText = it },
                    label = { Text("タグ") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editTarget?.let {
                        viewModel.setTag(it.packageName, editTagText, it.appLabel)
                    }
                    editTarget = null
                }) { Text("保存") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        editTarget?.let { viewModel.deleteTag(it.packageName) }
                        editTarget = null
                    }) { Text("削除") }
                    TextButton(onClick = { editTarget = null }) { Text("キャンセル") }
                }
            },
        )
    }
}


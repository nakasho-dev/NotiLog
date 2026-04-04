package org.ukky.notilog.ui.screen.settings

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.provider.Settings.Secure
import org.ukky.notilog.service.NotiLogListenerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOssLicensesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showPasswordDialog by remember { mutableStateOf<PasswordDialogMode?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // SAF launchers
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) showPasswordDialog = PasswordDialogMode.Export(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) showPasswordDialog = PasswordDialogMode.Import(uri)
    }

    // 通知リスナー権限の確認
    val listenerEnabled = remember(Unit) {
        val flat = Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
        val cn = ComponentName(context, NotiLogListenerService::class.java).flattenToString()
        flat.contains(cn)
    }

    // メッセージ表示
    LaunchedEffect(state.message) {
        // Snackbar等で表示（簡易実装）
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 通知リスナー権限 ────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("通知リスナー権限", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (listenerEnabled) "✅ 有効" else "❌ 無効",
                        color = if (listenerEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    )
                    if (!listenerEnabled) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }) {
                            Text("設定を開く")
                        }
                    }
                }
            }

            // ── バックアップ ────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("バックアップ", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { exportLauncher.launch("notilog_backup.bin") },
                            enabled = !state.isExporting,
                        ) {
                            Text(if (state.isExporting) "エクスポート中…" else "エクスポート")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("*/*")) },
                            enabled = !state.isImporting,
                        ) {
                            Text(if (state.isImporting) "インポート中…" else "インポート")
                        }
                    }
                }
            }

            // ── データ削除 ────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("データ管理", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("全ログ削除")
                    }
                }
            }

            // ── バージョン ────
            Text(
                "NotiLog v1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )

            // ── OSS ライセンス ────
            OutlinedButton(
                onClick = onOssLicensesClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("OSSライセンス")
            }

            // ── メッセージ ────
            state.message?.let {
                Snackbar(modifier = Modifier.padding(8.dp)) { Text(it) }
                LaunchedEffect(it) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearMessage()
                }
            }
        }
    }

    // ── 全削除確認ダイアログ ────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("全ログ削除") },
            text = { Text("すべての通知ログを削除しますか？\nこの操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAll()
                    showDeleteDialog = false
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("キャンセル") }
            },
        )
    }

    // ── パスワード入力ダイアログ ────
    showPasswordDialog?.let { mode ->
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = null },
            title = { Text(if (mode is PasswordDialogMode.Export) "エクスポート" else "インポート") },
            text = {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("パスワード") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (mode) {
                        is PasswordDialogMode.Export -> viewModel.export(context, mode.uri, password)
                        is PasswordDialogMode.Import -> viewModel.import(context, mode.uri, password)
                    }
                    showPasswordDialog = null
                }) { Text("実行") }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = null }) { Text("キャンセル") }
            },
        )
    }
}

private sealed interface PasswordDialogMode {
    data class Export(val uri: android.net.Uri) : PasswordDialogMode
    data class Import(val uri: android.net.Uri) : PasswordDialogMode
}


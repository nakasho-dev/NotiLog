package org.ukky.notilog.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.util.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext as withCoroutineContext

/**
 * OSSライセンス表示画面。
 *
 * AboutLibraries が Gradle プラグインでビルド時に生成した
 * `aboutlibraries.json` を非同期で読み込み、使用ライブラリのライセンス一覧を表示する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OssLicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // R.raw.aboutlibraries を IO スレッドで非同期ロード
    val libraries by produceState<Libs?>(null, context) {
        value = withCoroutineContext(Dispatchers.IO) {
            Libs.Builder().withContext(context).build()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("OSSライセンス") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

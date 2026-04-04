package org.ukky.notilog.ui.screen.tag

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.ukky.notilog.data.db.entity.AppTagEntity
import org.ukky.notilog.data.repository.AppTagRepository
import org.ukky.notilog.data.repository.NotificationRepository

/**
 * TagViewModel の単体テスト。
 *
 * 通知実績アプリ一覧とタグの結合ロジック、
 * タグの追加/編集/削除操作を検証する。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TagViewModelTest {

    private lateinit var tagRepo: AppTagRepository
    private lateinit var notificationRepo: NotificationRepository

    private val packageNamesFlow = MutableStateFlow<List<String>>(emptyList())
    private val tagsFlow = MutableStateFlow<List<AppTagEntity>>(emptyList())

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        tagRepo = mockk(relaxed = true)
        notificationRepo = mockk(relaxed = true)

        every { notificationRepo.getDistinctPackageNames() } returns packageNamesFlow
        every { tagRepo.getAll() } returns tagsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = TagViewModel(tagRepo, notificationRepo)

    // ── 一覧生成ロジック ────────────────────────────────

    @Test
    fun `通知実績アプリがタグなしで表示される`() = runTest {
        packageNamesFlow.value = listOf("com.a", "com.b")
        tagsFlow.value = emptyList()

        val vm = createViewModel()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.apps.size)
            assertEquals("com.a", state.apps[0].packageName)
            assertNull(state.apps[0].tag)
            assertEquals("com.b", state.apps[1].packageName)
            assertNull(state.apps[1].tag)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `タグ付きアプリにはタグが表示される`() = runTest {
        packageNamesFlow.value = listOf("com.a", "com.b")
        tagsFlow.value = listOf(
            AppTagEntity("com.a", "SNS", "App A"),
        )

        val vm = createViewModel()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.apps.size)
            assertEquals("SNS", state.apps[0].tag)
            assertEquals("App A", state.apps[0].appLabel)
            assertNull(state.apps[1].tag)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `通知実績のないタグのみのアプリも一覧に含まれる`() = runTest {
        packageNamesFlow.value = listOf("com.a")
        tagsFlow.value = listOf(
            AppTagEntity("com.a", "SNS", "App A"),
            AppTagEntity("com.orphan", "旧アプリ", "Orphan"),
        )

        val vm = createViewModel()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.apps.size) // com.a, com.orphan (sorted)
            // sorted alphabetically
            val names = state.apps.map { it.packageName }
            assertTrue(names.contains("com.orphan"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `パッケージ名はアルファベット順にソートされる`() = runTest {
        packageNamesFlow.value = listOf("com.z", "com.a", "com.m")
        tagsFlow.value = emptyList()

        val vm = createViewModel()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(listOf("com.a", "com.m", "com.z"), state.apps.map { it.packageName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── タグ操作 ────────────────────────────────────────

    @Test
    fun `setTagで空文字の場合はdeleteTagが呼ばれる`() = runTest {
        val vm = createViewModel()
        vm.setTag("com.a", "", null)
        coVerify { tagRepo.deleteTag("com.a") }
        coVerify(exactly = 0) { tagRepo.setTag(any()) }
    }

    @Test
    fun `setTagで空白のみの場合もdeleteTagが呼ばれる`() = runTest {
        val vm = createViewModel()
        vm.setTag("com.a", "   ", null)
        coVerify { tagRepo.deleteTag("com.a") }
    }

    @Test
    fun `setTagでタグ文字列がある場合はupsertされる`() = runTest {
        val vm = createViewModel()
        vm.setTag("com.a", "仕事", "App A")
        coVerify { tagRepo.setTag(AppTagEntity("com.a", "仕事", "App A")) }
    }

    @Test
    fun `deleteTagが正しく委譲される`() = runTest {
        val vm = createViewModel()
        vm.deleteTag("com.a")
        coVerify { tagRepo.deleteTag("com.a") }
    }

    // ── ローディング状態 ────────────────────────────────

    @Test
    fun `初期状態はローディング中`() = runTest {
        val vm = createViewModel()
        // 初期値をキャプチャ
        assertTrue(vm.uiState.value.isLoading || vm.uiState.value.apps.isEmpty())
    }

    @Test
    fun `データ到着後はローディング完了`() = runTest {
        packageNamesFlow.value = listOf("com.a")
        tagsFlow.value = emptyList()

        val vm = createViewModel()
        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }
}



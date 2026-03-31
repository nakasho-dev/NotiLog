package org.ukky.notilog.ui.screen.home

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.ukky.notilog.data.db.entity.NotificationEntity
import org.ukky.notilog.data.db.entity.NotificationWithTag
import org.ukky.notilog.data.repository.AppTagRepository
import org.ukky.notilog.data.repository.NotificationRepository

/**
 * HomeViewModel の単体テスト
 *
 * RED → GREEN: UIState の生成・タグフィルタ切り替えを検証する
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var notificationRepo: NotificationRepository
    private lateinit var tagRepo: AppTagRepository
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        notificationRepo = mockk(relaxed = true)
        tagRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初期状態ではフィルタなしで全通知を取得する`() = runTest {
        val items = listOf(
            NotificationWithTag(createEntity("s1", "通知A"), "SNS", "App"),
            NotificationWithTag(createEntity("s2", "通知B"), null, null),
        )
        every { notificationRepo.getAllWithTag() } returns flowOf(items)
        every { tagRepo.getAllTags() } returns flowOf(listOf("SNS"))
        every { notificationRepo.getByTag(any()) } returns flowOf(emptyList())

        val vm = HomeViewModel(notificationRepo, tagRepo)

        vm.uiState.test {
            // initialValue (isLoading = true)
            awaitItem()
            // 上流 Flow を処理させる
            advanceUntilIdle()
            val state = awaitItem()
            assertEquals(2, state.notifications.size)
            assertEquals("通知A", state.notifications[0].notification.title)
            assertNull(state.selectedTag)
            assertEquals(listOf("SNS"), state.availableTags)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `タグフィルタを切り替えると対応する通知だけ表示される`() = runTest {
        every { notificationRepo.getAllWithTag() } returns flowOf(emptyList())
        every { tagRepo.getAllTags() } returns flowOf(listOf("仕事", "SNS"))

        val filtered = listOf(
            NotificationWithTag(createEntity("f1", "Slack通知"), "仕事", "Slack"),
        )
        every { notificationRepo.getByTag("仕事") } returns flowOf(filtered)

        val vm = HomeViewModel(notificationRepo, tagRepo)

        vm.uiState.test {
            awaitItem() // initialValue
            advanceUntilIdle()
            awaitItem() // 初期データ（全件=空）

            vm.selectTag("仕事")
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(1, state.notifications.size)
            assertEquals("仕事", state.selectedTag)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `タグフィルタを解除すると全件に戻る`() = runTest {
        val all = listOf(
            NotificationWithTag(createEntity("a1", "全件A"), null, null),
        )
        every { notificationRepo.getAllWithTag() } returns flowOf(all)
        every { tagRepo.getAllTags() } returns flowOf(emptyList())

        val vm = HomeViewModel(notificationRepo, tagRepo)

        vm.uiState.test {
            awaitItem() // initialValue
            advanceUntilIdle()
            val state = awaitItem()
            assertNull(state.selectedTag)
            assertEquals(1, state.notifications.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createEntity(signature: String, title: String) = NotificationEntity(
        id = 0, packageName = "com.test", title = title, text = null,
        bigText = null, subText = null, ticker = null, extrasJson = "{}",
        signature = signature, receiveCount = 1,
        firstReceivedAt = 1000L, lastReceivedAt = 1000L,
    )
}


package org.ukky.notitrace.ui.screen.search

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
import org.ukky.notitrace.data.db.entity.NotificationEntity
import org.ukky.notitrace.data.db.entity.NotificationWithTag
import org.ukky.notitrace.data.repository.NotificationRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private lateinit var repo: NotificationRepository
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `検索クエリが空の場合は結果が空`() = runTest {
        val vm = SearchViewModel(repo)

        vm.uiState.test {
            val state = awaitItem() // initialValue
            assertTrue(state.results.isEmpty())
            assertEquals("", state.query)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `検索クエリを入力すると結果が返る`() = runTest {
        val results = listOf(
            NotificationWithTag(createEntity("東京の天気"), null, null),
        )
        every { repo.search("東京") } returns flowOf(results)

        val vm = SearchViewModel(repo)

        vm.uiState.test {
            awaitItem() // initialValue

            vm.onQueryChange("東京")
            // debounce(300ms) を進める
            advanceTimeBy(400)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals("東京", state.query)
            assertEquals(1, state.results.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createEntity(title: String) = NotificationEntity(
        id = 0, packageName = "com.test", title = title, text = null,
        bigText = null, subText = null, ticker = null, extrasJson = "{}",
        signature = "sig", receiveCount = 1,
        firstReceivedAt = 1000L, lastReceivedAt = 1000L,
    )
}


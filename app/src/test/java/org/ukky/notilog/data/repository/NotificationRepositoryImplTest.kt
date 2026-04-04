package org.ukky.notilog.data.repository

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.ukky.notilog.data.db.dao.NotificationDao
import org.ukky.notilog.data.db.entity.NotificationEntity
import org.ukky.notilog.data.db.entity.NotificationWithTag

/**
 * NotificationRepositoryImpl の単体テスト（MockK で DAO をモック）
 *
 * RED → GREEN: Repository の upsert ロジック（新規 / 重複分岐）を検証する
 */
class NotificationRepositoryImplTest {

    private lateinit var dao: NotificationDao
    private lateinit var repository: NotificationRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = NotificationRepositoryImpl(dao)
    }

    // ── upsert: 新規通知 ──────────────────────────────

    @Test
    fun `新規signatureの場合INSERTされる`() = runTest {
        coEvery { dao.findBySignature("new_sig") } returns null
        coEvery { dao.insert(any()) } returns 1L

        val entity = createEntity(signature = "new_sig")
        repository.upsert(entity)

        coVerify(exactly = 1) { dao.insert(entity) }
        coVerify(exactly = 0) { dao.incrementCount(any(), any()) }
    }

    // ── upsert: 重複通知 ──────────────────────────────

    @Test
    fun `既存signatureの場合receiveCountがインクリメントされる`() = runTest {
        val existing = createEntity(signature = "dup_sig", receiveCount = 3)
        coEvery { dao.findBySignature("dup_sig") } returns existing

        val newEntity = createEntity(signature = "dup_sig", lastReceivedAt = 5000L)
        repository.upsert(newEntity)

        coVerify(exactly = 0) { dao.insert(any()) }
        coVerify(exactly = 1) { dao.incrementCount("dup_sig", 5000L) }
    }

    // ── 一覧取得 ──────────────────────────────────────

    @Test
    fun `全通知をFlowで取得できる`() = runTest {
        val mockList = listOf(
            NotificationWithTag(createEntity(signature = "s1", title = "通知1"), "SNS", "App1"),
            NotificationWithTag(createEntity(signature = "s2", title = "通知2"), null, null),
        )
        every { dao.getAllWithTag() } returns flowOf(mockList)

        repository.getAllWithTag().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("通知1", result[0].notification.title)
            assertEquals("SNS", result[0].tag)
            assertNull(result[1].tag)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── タグフィルタ ──────────────────────────────────

    @Test
    fun `タグで絞り込める`() = runTest {
        val filtered = listOf(
            NotificationWithTag(createEntity(signature = "f1"), "仕事", "SlackApp"),
        )
        every { dao.getByTag("仕事") } returns flowOf(filtered)

        repository.getByTag("仕事").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("仕事", result[0].tag)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── 検索 ──────────────────────────────────────────

    @Test
    fun `全文検索でマッチした通知を取得できる`() = runTest {
        val searchResult = listOf(
            NotificationWithTag(createEntity(signature = "sr1", title = "東京天気"), null, null),
        )
        every { dao.search("東京") } returns flowOf(searchResult)

        repository.search("東京").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("東京天気", result[0].notification.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── 削除 ──────────────────────────────────────────

    @Test
    fun `IDで通知を削除する`() = runTest {
        repository.deleteById(42L)
        coVerify { dao.deleteById(42L) }
    }

    @Test
    fun `全件削除する`() = runTest {
        repository.deleteAll()
        coVerify { dao.deleteAll() }
    }

    // ── 通知実績パッケージ一覧 ──────────────────────────

    @Test
    fun `通知実績のある全パッケージ名をFlowで取得できる`() = runTest {
        every { dao.getDistinctPackageNames() } returns flowOf(listOf("com.a", "com.b"))

        repository.getDistinctPackageNames().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("com.a", result[0])
            assertEquals("com.b", result[1])
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── ヘルパー ──────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun createEntity(
        signature: String,
        title: String? = "Title",
        receiveCount: Int = 1,
        lastReceivedAt: Long = 1000L,
        notificationType: String = "local",
    ) = NotificationEntity(
        id = 0,
        packageName = "com.test",
        title = title,
        text = "text",
        bigText = null,
        subText = null,
        ticker = null,
        extrasJson = "{}",
        rawJson = "{}",
        signature = signature,
        notificationType = notificationType,
        isRemote = notificationType == "remote_push" || notificationType == "remote_silent",
        receiveCount = receiveCount,
        firstReceivedAt = 1000L,
        lastReceivedAt = lastReceivedAt,
    )
}


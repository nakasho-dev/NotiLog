package org.ukky.notitrace.data.repository

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.ukky.notitrace.data.db.dao.NotificationDao
import org.ukky.notitrace.data.db.dao.NotificationRawLogDao
import org.ukky.notitrace.data.db.entity.NotificationEntity
import org.ukky.notitrace.data.db.entity.NotificationRawLogEntity
import org.ukky.notitrace.data.db.entity.NotificationWithTag
import org.ukky.notitrace.data.db.entity.RawLogWithTag

/**
 * NotificationRepositoryImpl の単体テスト（MockK で DAO をモック）
 */
class NotificationRepositoryImplTest {

    private lateinit var dao: NotificationDao
    private lateinit var rawLogDao: NotificationRawLogDao
    private lateinit var repository: NotificationRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        rawLogDao = mockk(relaxed = true)
        repository = NotificationRepositoryImpl(dao, rawLogDao)
    }

    // ── upsert: 新規通知 ──────────────────────────────

    @Test
    fun `新規signatureの場合INSERTされる`() = runTest {
        coEvery { dao.findBySignature("new_sig") } returns null
        coEvery { dao.insert(any()) } returns 1L
        coEvery { rawLogDao.insert(any()) } returns 1L

        val entity = createEntity(signature = "new_sig")
        repository.upsert(entity)

        coVerify(exactly = 1) { dao.insert(entity) }
        coVerify(exactly = 0) { dao.incrementCount(any(), any()) }
    }

    @Test
    fun `新規通知のupsertでrawLogも記録される`() = runTest {
        coEvery { dao.findBySignature("new_sig") } returns null
        coEvery { dao.insert(any()) } returns 42L
        coEvery { rawLogDao.insert(any()) } returns 1L

        val entity = createEntity(signature = "new_sig", rawJson = """{"test":"data"}""")
        repository.upsert(entity)

        coVerify(exactly = 1) {
            rawLogDao.insert(match {
                it.notificationId == 42L &&
                    it.rawJson == """{"test":"data"}""" &&
                    it.receivedAt == entity.firstReceivedAt
            })
        }
    }

    // ── upsert: 重複通知 ──────────────────────────────

    @Test
    fun `既存signatureの場合receiveCountがインクリメントされる`() = runTest {
        val existing = createEntity(id = 10L, signature = "dup_sig", receiveCount = 3)
        coEvery { dao.findBySignature("dup_sig") } returns existing
        coEvery { rawLogDao.insert(any()) } returns 1L

        val newEntity = createEntity(signature = "dup_sig", lastReceivedAt = 5000L)
        repository.upsert(newEntity)

        coVerify(exactly = 0) { dao.insert(any()) }
        coVerify(exactly = 1) { dao.incrementCount("dup_sig", 5000L) }
    }

    @Test
    fun `重複通知のupsertでもrawLogが記録される`() = runTest {
        val existing = createEntity(id = 10L, signature = "dup_sig", receiveCount = 3)
        coEvery { dao.findBySignature("dup_sig") } returns existing
        coEvery { rawLogDao.insert(any()) } returns 1L

        val newEntity = createEntity(
            signature = "dup_sig",
            lastReceivedAt = 5000L,
            rawJson = """{"dup":"data"}""",
        )
        repository.upsert(newEntity)

        coVerify(exactly = 1) {
            rawLogDao.insert(match {
                it.notificationId == 10L &&
                    it.rawJson == """{"dup":"data"}""" &&
                    it.receivedAt == 5000L
            })
        }
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
    fun `FTS検索でマッチした通知を取得できる`() = runTest {
        val searchResult = listOf(
            NotificationWithTag(createEntity(signature = "sr1", title = "東京天気"), null, null),
        )
        every { dao.searchFts("東京") } returns flowOf(searchResult)

        repository.search("東京").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("東京天気", result[0].notification.title)
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { dao.searchFts("東京") }
        verify(exactly = 0) { dao.searchPartial(any()) }
    }

    @Test
    fun `FTS検索が0件なら部分一致検索にフォールバックする`() = runTest {
        val fallbackResult = listOf(
            NotificationWithTag(createEntity(signature = "sr2", title = "東京都"), null, null),
        )
        every { dao.searchFts("京") } returns flowOf(emptyList())
        every { dao.searchPartial("%京%") } returns flowOf(fallbackResult)

        repository.search("京").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("東京都", result[0].notification.title)
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { dao.searchFts("京") }
        verify(exactly = 1) { dao.searchPartial("%京%") }
    }

    @Test
    fun `FTS検索が失敗しても部分一致検索にフォールバックする`() = runTest {
        val fallbackResult = listOf(
            NotificationWithTag(createEntity(signature = "sr3", title = "100%完了"), null, null),
        )
        every { dao.searchFts("100%") } returns flow { error("bad MATCH query") }
        every { dao.searchPartial("%100\\%%") } returns flowOf(fallbackResult)

        repository.search("100%").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("100%完了", result[0].notification.title)
            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { dao.searchFts("100%") }
        verify(exactly = 1) { dao.searchPartial("%100\\%%") }
    }

    // ── 削除 ──────────────────────────────────────────

    @Test
    fun `IDで通知を削除する`() = runTest {
        repository.deleteById(42L)
        coVerify { dao.deleteById(42L) }
    }

    @Test
    fun `全件削除でrawLogも一括削除される`() = runTest {
        repository.deleteAll()
        coVerify(exactly = 1) { rawLogDao.deleteAll() }
        coVerify(exactly = 1) { dao.deleteAll() }
    }

    // ── JSONL エクスポート用一括取得 ─────────────────────

    @Test
    fun `getForExport_tagNull_全件をAllWithTagListから取得する`() = runTest {
        val allItems = listOf(
            NotificationWithTag(createEntity(signature = "e1"), "SNS", "App1"),
            NotificationWithTag(createEntity(signature = "e2"), null, null),
        )
        coEvery { dao.getAllWithTagList() } returns allItems

        val result = repository.getForExport(null)
        assertEquals(2, result.size)
        coVerify(exactly = 1) { dao.getAllWithTagList() }
        coVerify(exactly = 0) { dao.getByTagList(any()) }
    }

    @Test
    fun `getForExport_tag指定_指定タグでフィルタした一覧を取得する`() = runTest {
        val filtered = listOf(
            NotificationWithTag(createEntity(signature = "e3"), "仕事", "SlackApp"),
        )
        coEvery { dao.getByTagList("仕事") } returns filtered

        val result = repository.getForExport("仕事")
        assertEquals(1, result.size)
        assertEquals("仕事", result[0].tag)
        coVerify(exactly = 0) { dao.getAllWithTagList() }
        coVerify(exactly = 1) { dao.getByTagList("仕事") }
    }

    @Test
    fun `getForExport_存在しないタグ_空リストが返る`() = runTest {
        coEvery { dao.getByTagList("存在しないタグ") } returns emptyList()

        val result = repository.getForExport("存在しないタグ")
        assertTrue(result.isEmpty())
        coVerify(exactly = 1) { dao.getByTagList("存在しないタグ") }
    }

    // ── 生データ JSONL エクスポート用一括取得 ──────────────

    @Test
    fun `getForRawExport_tagNull_全件を受信順で取得する`() = runTest {
        val rawItems = listOf(
            RawLogWithTag("""{"a":1}""", 1000L, "com.a", "local", "SNS", "App1"),
            RawLogWithTag("""{"b":2}""", 2000L, "com.b", "remote_push", null, null),
        )
        coEvery { rawLogDao.getAllWithTagOrderByReceivedAt() } returns rawItems

        val result = repository.getForRawExport(null)
        assertEquals(2, result.size)
        assertEquals(1000L, result[0].receivedAt)
        assertEquals(2000L, result[1].receivedAt)
    }

    @Test
    fun `getForRawExport_tag指定_指定タグでフィルタする`() = runTest {
        val rawItems = listOf(
            RawLogWithTag("""{"c":3}""", 3000L, "com.c", "local", "仕事", "Slack"),
        )
        coEvery { rawLogDao.getByTagOrderByReceivedAt("仕事") } returns rawItems

        val result = repository.getForRawExport("仕事")
        assertEquals(1, result.size)
        assertEquals("仕事", result[0].tag)
    }

    // ── rawLog 保持期間クリーンアップ ─────────────────────

    @Test
    fun `cleanupOldRawLogs_指定時刻より古いrawLogを削除する`() = runTest {
        coEvery { rawLogDao.deleteOlderThan(5000L) } returns 3

        val deleted = repository.cleanupOldRawLogs(5000L)
        assertEquals(3, deleted)
        coVerify(exactly = 1) { rawLogDao.deleteOlderThan(5000L) }
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
        id: Long = 0,
        signature: String,
        title: String? = "Title",
        receiveCount: Int = 1,
        lastReceivedAt: Long = 1000L,
        notificationType: String = "local",
        rawJson: String = "{}",
    ) = NotificationEntity(
        id = id,
        packageName = "com.test",
        title = title,
        text = "text",
        bigText = null,
        subText = null,
        ticker = null,
        extrasJson = "{}",
        rawJson = rawJson,
        signature = signature,
        notificationType = notificationType,
        isRemote = notificationType == "remote_push" || notificationType == "remote_silent",
        receiveCount = receiveCount,
        firstReceivedAt = 1000L,
        lastReceivedAt = lastReceivedAt,
    )
}

package org.ukky.notitrace.data.repository

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.ukky.notitrace.data.db.dao.AppTagDao
import org.ukky.notitrace.data.db.entity.AppTagEntity

/**
 * AppTagRepositoryImpl の単体テスト
 */
class AppTagRepositoryImplTest {

    private lateinit var dao: AppTagDao
    private lateinit var repository: AppTagRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = AppTagRepositoryImpl(dao)
    }

    @Test
    fun `タグを設定するとupsertが呼ばれる`() = runTest {
        val entity = AppTagEntity("com.example", "SNS", "Example")
        repository.setTag(entity)
        coVerify { dao.upsert(entity) }
    }

    @Test
    fun `全タグ名をFlowで取得できる`() = runTest {
        every { dao.getAllTags() } returns flowOf(listOf("SNS", "仕事"))

        repository.getAllTags().test {
            val tags = awaitItem()
            assertEquals(listOf("SNS", "仕事"), tags)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `パッケージ名でタグ情報を取得できる`() = runTest {
        coEvery { dao.getByPackageName("com.example") } returns
                AppTagEntity("com.example", "SNS", "Example")

        val result = repository.getByPackageName("com.example")
        assertNotNull(result)
        assertEquals("SNS", result!!.tag)
    }

    @Test
    fun `タグを削除できる`() = runTest {
        repository.deleteTag("com.example")
        coVerify { dao.deleteByPackageName("com.example") }
    }

    @Test
    fun `全タグ付きアプリ一覧をFlowで取得`() = runTest {
        val list = listOf(
            AppTagEntity("a", "tag1", "App A"),
            AppTagEntity("b", "tag2", "App B"),
        )
        every { dao.getAll() } returns flowOf(list)

        repository.getAll().test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}


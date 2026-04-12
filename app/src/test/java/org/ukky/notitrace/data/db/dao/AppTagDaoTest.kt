package org.ukky.notitrace.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.ukky.notitrace.data.db.NotiTraceDatabase
import org.ukky.notitrace.data.db.entity.AppTagEntity

/**
 * AppTagDao の単体テスト（Robolectric + in-memory DB）
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppTagDaoTest {

    private lateinit var db: NotiTraceDatabase
    private lateinit var dao: AppTagDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NotiTraceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.appTagDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `タグを挿入して取得できる`() = runTest {
        dao.upsert(AppTagEntity("com.example.app", "SNS", "Example App"))

        val result = dao.getByPackageName("com.example.app")
        assertNotNull(result)
        assertEquals("SNS", result!!.tag)
        assertEquals("Example App", result.appLabel)
    }

    @Test
    fun `同一パッケージのタグをupsertで更新できる`() = runTest {
        dao.upsert(AppTagEntity("com.example.app", "SNS", "App"))
        dao.upsert(AppTagEntity("com.example.app", "メッセンジャー", "App"))

        val result = dao.getByPackageName("com.example.app")!!
        assertEquals("メッセンジャー", result.tag)
    }

    @Test
    fun `全タグ一覧をFlowで取得できる`() = runTest {
        dao.upsert(AppTagEntity("a", "仕事", null))
        dao.upsert(AppTagEntity("b", "プライベート", null))
        dao.upsert(AppTagEntity("c", "仕事", null))

        dao.getAllTags().test {
            val tags = awaitItem()
            // DISTINCT + ORDER BY tag
            assertEquals(listOf("プライベート", "仕事"), tags)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `パッケージ名でタグを削除できる`() = runTest {
        dao.upsert(AppTagEntity("com.delete.me", "削除対象", null))
        dao.deleteByPackageName("com.delete.me")

        val result = dao.getByPackageName("com.delete.me")
        assertNull(result)
    }

    @Test
    fun `全タグをバックアップ用に取得できる`() = runTest {
        dao.upsert(AppTagEntity("a", "tagA", "AppA"))
        dao.upsert(AppTagEntity("b", "tagB", "AppB"))

        val all = dao.getAllForBackup()
        assertEquals(2, all.size)
    }
}


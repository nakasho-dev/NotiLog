package org.ukky.notilog.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.ukky.notilog.data.db.dao.NotificationDao
import org.ukky.notilog.data.db.entity.NotificationEntity
import org.ukky.notilog.data.db.entity.NotificationWithTag
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val dao: NotificationDao,
) : NotificationRepository {

    override fun getAllWithTag(): Flow<List<NotificationWithTag>> =
        dao.getAllWithTag()

    override fun getByTag(tag: String): Flow<List<NotificationWithTag>> =
        dao.getByTag(tag)

    override fun search(query: String): Flow<List<NotificationWithTag>> {
        val pattern = query.toLikePattern()
        return dao.searchFts(query)
            .catch { emit(emptyList()) }
            .flatMapLatest { ftsResults ->
                if (ftsResults.isNotEmpty()) {
                    flowOf(ftsResults)
                } else {
                    dao.searchPartial(pattern)
                }
            }
    }

    override fun getById(id: Long): Flow<NotificationEntity?> =
        dao.getById(id)

    /**
     * 重複判定付き保存（upsert）。
     *
     * 1. signature で既存チェック
     * 2. 既存あり → receiveCount++ & lastReceivedAt 更新
     * 3. 既存なし → INSERT
     */
    override suspend fun upsert(entity: NotificationEntity) {
        val existing = dao.findBySignature(entity.signature)
        if (existing != null) {
            dao.incrementCount(entity.signature, entity.lastReceivedAt)
        } else {
            dao.insert(entity)
        }
    }

    override suspend fun deleteById(id: Long) = dao.deleteById(id)

    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun getAllForBackup(): List<NotificationEntity> =
        dao.getAllForBackup()

    override suspend fun getForExport(tag: String?): List<NotificationWithTag> =
        if (tag == null) dao.getAllWithTagList()
        else dao.getByTagList(tag)

    override fun getDistinctPackageNames(): Flow<List<String>> =
        dao.getDistinctPackageNames()

    private fun String.toLikePattern(): String = buildString(length + 2) {
        append('%')
        for (ch in this@toLikePattern) {
            when (ch) {
                '\\', '%', '_' -> append('\\')
            }
            append(ch)
        }
        append('%')
    }
}


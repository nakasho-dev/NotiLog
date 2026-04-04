package org.ukky.notilog.data.repository

import kotlinx.coroutines.flow.Flow
import org.ukky.notilog.data.db.dao.NotificationDao
import org.ukky.notilog.data.db.entity.NotificationEntity
import org.ukky.notilog.data.db.entity.NotificationWithTag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val dao: NotificationDao,
) : NotificationRepository {

    override fun getAllWithTag(): Flow<List<NotificationWithTag>> =
        dao.getAllWithTag()

    override fun getByTag(tag: String): Flow<List<NotificationWithTag>> =
        dao.getByTag(tag)

    override fun search(query: String): Flow<List<NotificationWithTag>> =
        dao.search(query)

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

    override fun getDistinctPackageNames(): Flow<List<String>> =
        dao.getDistinctPackageNames()
}


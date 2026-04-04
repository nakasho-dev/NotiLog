package org.ukky.notilog.data.repository

import kotlinx.coroutines.flow.Flow
import org.ukky.notilog.data.db.entity.NotificationEntity
import org.ukky.notilog.data.db.entity.NotificationWithTag

/**
 * 通知データへのアクセスを抽象化するインターフェース。
 */
interface NotificationRepository {
    fun getAllWithTag(): Flow<List<NotificationWithTag>>
    fun getByTag(tag: String): Flow<List<NotificationWithTag>>
    fun search(query: String): Flow<List<NotificationWithTag>>
    fun getById(id: Long): Flow<NotificationEntity?>
    suspend fun upsert(entity: NotificationEntity)
    suspend fun deleteById(id: Long)
    suspend fun deleteAll()
    suspend fun getAllForBackup(): List<NotificationEntity>
    fun getDistinctPackageNames(): Flow<List<String>>
}


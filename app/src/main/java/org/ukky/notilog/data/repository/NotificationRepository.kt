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
    /**
     * 通知を検索する。
     *
     * まず FTS4 で全文検索し、結果が 0 件または MATCH クエリが解釈できない場合は
     * title / text / bigText / subText に対する部分一致検索へフォールバックする。
     */
    fun search(query: String): Flow<List<NotificationWithTag>>
    fun getById(id: Long): Flow<NotificationEntity?>
    suspend fun upsert(entity: NotificationEntity)
    suspend fun deleteById(id: Long)
    suspend fun deleteAll()
    suspend fun getAllForBackup(): List<NotificationEntity>
    fun getDistinctPackageNames(): Flow<List<String>>
}


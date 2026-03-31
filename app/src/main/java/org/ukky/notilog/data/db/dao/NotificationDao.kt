package org.ukky.notilog.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.ukky.notilog.data.db.entity.NotificationEntity
import org.ukky.notilog.data.db.entity.NotificationWithTag

@Dao
interface NotificationDao {

    // ── 一覧取得（Flow / リアルタイム更新） ────────────

    @Query(
        """
        SELECT n.*, a.tag, a.app_label
        FROM notifications n
        LEFT JOIN app_tags a ON n.package_name = a.package_name
        ORDER BY n.last_received_at DESC
        """
    )
    fun getAllWithTag(): Flow<List<NotificationWithTag>>

    @Query(
        """
        SELECT n.*, a.tag, a.app_label
        FROM notifications n
        INNER JOIN app_tags a ON n.package_name = a.package_name
        WHERE a.tag = :tag
        ORDER BY n.last_received_at DESC
        """
    )
    fun getByTag(tag: String): Flow<List<NotificationWithTag>>

    // ── FTS 全文検索 ──────────────────────────────────

    @Query(
        """
        SELECT n.*, a.tag, a.app_label
        FROM notifications n
        INNER JOIN notifications_fts fts ON n.id = fts.rowid
        LEFT JOIN app_tags a ON n.package_name = a.package_name
        WHERE notifications_fts MATCH :query
        ORDER BY n.last_received_at DESC
        """
    )
    fun search(query: String): Flow<List<NotificationWithTag>>

    // ── 個別取得 ──────────────────────────────────────

    @Query("SELECT * FROM notifications WHERE id = :id")
    fun getById(id: Long): Flow<NotificationEntity?>

    @Query("SELECT * FROM notifications WHERE signature = :signature LIMIT 1")
    suspend fun findBySignature(signature: String): NotificationEntity?

    // ── 書き込み ──────────────────────────────────────

    @Insert
    suspend fun insert(entity: NotificationEntity): Long

    @Query(
        """
        UPDATE notifications
        SET receive_count = receive_count + 1,
            last_received_at = :timestamp
        WHERE signature = :signature
        """
    )
    suspend fun incrementCount(signature: String, timestamp: Long)

    // ── 削除 ──────────────────────────────────────────

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    // ── バックアップ用（非Flow / 一括取得） ────────────

    @Query("SELECT * FROM notifications ORDER BY last_received_at DESC")
    suspend fun getAllForBackup(): List<NotificationEntity>
}


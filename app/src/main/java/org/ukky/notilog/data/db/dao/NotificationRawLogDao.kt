package org.ukky.notilog.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import org.ukky.notilog.data.db.entity.NotificationRawLogEntity
import org.ukky.notilog.data.db.entity.RawLogWithTag

@Dao
interface NotificationRawLogDao {

    @Insert
    suspend fun insert(entity: NotificationRawLogEntity): Long

    // ── 保持期間による削除 ──────────────────────────────
    /**
     * 指定した時刻より古い rawLog を一括削除する。
     *
     * @param cutoffMillis この時刻（Unix millis）より前のレコードを削除
     * @return 削除した行数
     */
    @Query("DELETE FROM notification_raw_logs WHERE received_at < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int

    // ── 全削除（notifications ON DELETE CASCADE でも消えるが明示用） ────
    @Query("DELETE FROM notification_raw_logs")
    suspend fun deleteAll()

    // ── JSONL 生データエクスポート用（受信順 ASC） ────
    @Query(
        """
        SELECT r.raw_json, r.received_at, n.package_name, n.notification_type,
               a.tag, a.app_label
        FROM notification_raw_logs r
        INNER JOIN notifications n ON r.notification_id = n.id
        LEFT JOIN app_tags a ON n.package_name = a.package_name
        ORDER BY r.received_at ASC
        """
    )
    suspend fun getAllWithTagOrderByReceivedAt(): List<RawLogWithTag>

    @Query(
        """
        SELECT r.raw_json, r.received_at, n.package_name, n.notification_type,
               a.tag, a.app_label
        FROM notification_raw_logs r
        INNER JOIN notifications n ON r.notification_id = n.id
        INNER JOIN app_tags a ON n.package_name = a.package_name
        WHERE a.tag = :tag
        ORDER BY r.received_at ASC
        """
    )
    suspend fun getByTagOrderByReceivedAt(tag: String): List<RawLogWithTag>

    // ── バックアップ用 ──────────────────────────────
    @Query("SELECT * FROM notification_raw_logs ORDER BY received_at ASC")
    suspend fun getAllForBackup(): List<NotificationRawLogEntity>
}


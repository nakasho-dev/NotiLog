package org.ukky.notilog.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 通知受信ごとの生データ JSON を保持する子テーブル。
 *
 * - 親テーブル `notifications` の `id` と外部キーで結合する
 * - 親レコードが削除されると CASCADE で自動削除される
 * - 重複通知（signature 一致）でも受信ごとに 1 行ずつ記録する
 * - JSONL 生データエクスポートでは、このテーブルを受信順に出力する
 */
@Entity(
    tableName = "notification_raw_logs",
    foreignKeys = [
        ForeignKey(
            entity = NotificationEntity::class,
            parentColumns = ["id"],
            childColumns = ["notification_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["notification_id"]),
        Index(value = ["received_at"]),
    ],
)
data class NotificationRawLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "notification_id")
    val notificationId: Long,

    @ColumnInfo(name = "raw_json")
    val rawJson: String,

    @ColumnInfo(name = "received_at")
    val receivedAt: Long,
)


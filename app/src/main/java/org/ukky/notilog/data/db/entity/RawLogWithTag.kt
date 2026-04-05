package org.ukky.notilog.data.db.entity

import androidx.room.ColumnInfo

/**
 * notification_raw_logs LEFT JOIN notifications LEFT JOIN app_tags の結果を受ける POJO。
 *
 * JSONL 生データエクスポートで使用する。
 */
data class RawLogWithTag(
    @ColumnInfo(name = "raw_json") val rawJson: String,
    @ColumnInfo(name = "received_at") val receivedAt: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "notification_type") val notificationType: String,
    @ColumnInfo(name = "tag") val tag: String?,
    @ColumnInfo(name = "app_label") val appLabel: String?,
)


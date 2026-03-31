package org.ukky.notilog.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * notifications LEFT JOIN app_tags の結果を受けるPOJO。
 * DAO の @Query で使用する。
 */
data class NotificationWithTag(
    @Embedded val notification: NotificationEntity,
    @ColumnInfo(name = "tag") val tag: String?,
    @ColumnInfo(name = "app_label") val appLabel: String?,
)


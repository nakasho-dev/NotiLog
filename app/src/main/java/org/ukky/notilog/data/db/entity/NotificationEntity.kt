package org.ukky.notilog.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 通知ログのメインテーブル。
 *
 * - signature (SHA-256) で重複判定を行う
 * - 同一 signature の通知は receiveCount / lastReceivedAt のみ更新する
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["signature"], unique = true),
        Index(value = ["package_name"]),
        Index(value = ["last_received_at"]),
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    val title: String?,

    val text: String?,

    @ColumnInfo(name = "big_text")
    val bigText: String?,

    @ColumnInfo(name = "sub_text")
    val subText: String?,

    val ticker: String?,

    @ColumnInfo(name = "extras_json", defaultValue = "{}")
    val extrasJson: String = "{}",

    val signature: String,

    /**
     * 通知の種別コード（[NotificationType.code] を TEXT で保存）。
     *
     * @see NotificationType
     */
    @ColumnInfo(name = "notification_type", defaultValue = "local")
    val notificationType: String = NotificationType.LOCAL.code,

    /**
     * リモートプッシュ通知かどうか（v2 互換用 — 非推奨）。
     * 新しいコードでは [notificationType] を使用すること。
     */
    @Deprecated("Use notificationType instead")
    @ColumnInfo(name = "is_remote", defaultValue = "0")
    val isRemote: Boolean = false,

    @ColumnInfo(name = "receive_count", defaultValue = "1")
    val receiveCount: Int = 1,

    @ColumnInfo(name = "first_received_at")
    val firstReceivedAt: Long,

    @ColumnInfo(name = "last_received_at")
    val lastReceivedAt: Long,
)


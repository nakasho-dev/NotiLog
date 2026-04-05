package org.ukky.notilog.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 2,
    val exportedAt: Long,
    val notifications: List<NotificationBackupItem>,
    val tags: List<TagBackupItem>,
    val rawLogs: List<RawLogBackupItem> = emptyList(),
)

@Serializable
data class NotificationBackupItem(
    val packageName: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val ticker: String?,
    val extrasJson: String,
    val rawJson: String = "{}",
    val signature: String,
    val receiveCount: Int,
    val firstReceivedAt: Long,
    val lastReceivedAt: Long,
)

@Serializable
data class TagBackupItem(
    val packageName: String,
    val tag: String,
    val appLabel: String?,
)

/**
 * 受信ごとの生データ JSON のバックアップ用モデル。
 *
 * [notificationSignature] で親 notification と紐付ける（復元時に id は変わるため）。
 */
@Serializable
data class RawLogBackupItem(
    val notificationSignature: String,
    val rawJson: String,
    val receivedAt: Long,
)

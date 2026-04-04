package org.ukky.notilog.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long,
    val notifications: List<NotificationBackupItem>,
    val tags: List<TagBackupItem>,
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


package org.ukky.notilog.backup

import kotlinx.serialization.json.Json
import org.ukky.notilog.data.db.entity.AppTagEntity
import org.ukky.notilog.data.db.entity.NotificationEntity
import org.ukky.notilog.data.repository.AppTagRepository
import org.ukky.notilog.data.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * バックアップのエクスポート / インポートを管理する。
 *
 * フロー:
 *   Entity → BackupItem → JSON → AES-GCM 暗号化 → ByteArray（ファイル出力は呼び出し側）
 */
@Singleton
class BackupManager @Inject constructor(
    private val notificationRepo: NotificationRepository,
    private val tagRepo: AppTagRepository,
) {
    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    /**
     * 暗号化バックアップデータを生成する。
     */
    suspend fun export(password: String): ByteArray {
        val notifications = notificationRepo.getAllForBackup().map { it.toBackupItem() }
        val tags = tagRepo.getAllForBackup().map { it.toBackupItem() }

        val backup = BackupData(
            exportedAt = System.currentTimeMillis(),
            notifications = notifications,
            tags = tags,
        )

        val jsonBytes = json.encodeToString(BackupData.serializer(), backup)
            .toByteArray(Charsets.UTF_8)

        return BackupCrypto.encrypt(jsonBytes, password)
    }

    /**
     * 暗号化バックアップからデータを復元する。
     * signature ベースで重複を排除してマージする。
     */
    suspend fun import(encryptedData: ByteArray, password: String) {
        val jsonBytes = BackupCrypto.decrypt(encryptedData, password)
        val backup = json.decodeFromString(BackupData.serializer(), String(jsonBytes, Charsets.UTF_8))

        backup.notifications.forEach { item ->
            val entity = item.toEntity()
            notificationRepo.upsert(entity)
        }

        backup.tags.forEach { item ->
            tagRepo.setTag(item.toEntity())
        }
    }

    // ── マッピング ────────────────────────────────────

    private fun NotificationEntity.toBackupItem() = NotificationBackupItem(
        packageName = packageName,
        title = title,
        text = text,
        bigText = bigText,
        subText = subText,
        ticker = ticker,
        extrasJson = extrasJson,
        rawJson = rawJson,
        signature = signature,
        receiveCount = receiveCount,
        firstReceivedAt = firstReceivedAt,
        lastReceivedAt = lastReceivedAt,
    )

    private fun NotificationBackupItem.toEntity() = NotificationEntity(
        packageName = packageName,
        title = title,
        text = text,
        bigText = bigText,
        subText = subText,
        ticker = ticker,
        extrasJson = extrasJson,
        rawJson = rawJson,
        signature = signature,
        receiveCount = receiveCount,
        firstReceivedAt = firstReceivedAt,
        lastReceivedAt = lastReceivedAt,
    )

    private fun AppTagEntity.toBackupItem() = TagBackupItem(
        packageName = packageName,
        tag = tag,
        appLabel = appLabel,
    )

    private fun TagBackupItem.toEntity() = AppTagEntity(
        packageName = packageName,
        tag = tag,
        appLabel = appLabel,
    )
}


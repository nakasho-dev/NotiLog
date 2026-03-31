package org.ukky.notilog.util

import android.app.Notification
import android.service.notification.StatusBarNotification
import org.ukky.notilog.data.db.entity.NotificationEntity
import org.ukky.notilog.data.db.entity.NotificationType
import kotlinx.serialization.json.*

/**
 * StatusBarNotification → NotificationEntity への変換ユーティリティ。
 */
object NotificationExtractor {

    /**
     * FCM / GCM 経由のリモートプッシュ通知に含まれる典型的な extras キー。
     * いずれかが存在すればリモートプッシュと推定する。
     */
    private val REMOTE_PUSH_MARKER_KEYS = setOf(
        "google.message_id",
        "google.sent_time",
        "google.delivered_priority",
        "google.original_priority",
        "google.c.a.e",          // FCM Analytics
        "google.c.sender.id",
        "gcm.n.e",               // GCM notification key
        "com.google.firebase.messaging.default_notification_channel_id",
    )

    fun extract(sbn: StatusBarNotification): NotificationEntity {
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val ticker = sbn.notification.tickerText?.toString()

        val extrasJson = buildExtrasJson(sbn)
        val type = classifyNotification(sbn)

        val now = System.currentTimeMillis()
        val signature = SignatureGenerator.generate(
            packageName = sbn.packageName,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
        )

        @Suppress("DEPRECATION")
        return NotificationEntity(
            packageName = sbn.packageName,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            ticker = ticker,
            extrasJson = extrasJson,
            signature = signature,
            notificationType = type.code,
            isRemote = type == NotificationType.REMOTE_PUSH || type == NotificationType.REMOTE_SILENT,
            receiveCount = 1,
            firstReceivedAt = now,
            lastReceivedAt = now,
        )
    }

    // ──────────────────────────────────────────────
    //  通知種別の判定（ヒューリスティクス）
    // ──────────────────────────────────────────────

    /**
     * 通知を 7 種別に分類する。
     *
     * 判定優先度:
     *  1. FLAG_FOREGROUND_SERVICE → [NotificationType.FOREGROUND_SERVICE]
     *  2. FLAG_ONGOING_EVENT     → [NotificationType.ONGOING]
     *  3. FLAG_GROUP_SUMMARY     → [NotificationType.GROUP_SUMMARY]
     *  4. FCM マーカー + 低優先度 → [NotificationType.REMOTE_SILENT]
     *  5. FCM マーカー           → [NotificationType.REMOTE_PUSH]
     *  6. 低優先度               → [NotificationType.LOCAL_SILENT]
     *  7. default                → [NotificationType.LOCAL]
     */
    internal fun classifyNotification(sbn: StatusBarNotification): NotificationType {
        val flags = sbn.notification.flags

        // ── 1. フォアグラウンドサービス ────
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) {
            return NotificationType.FOREGROUND_SERVICE
        }

        // ── 2. 進行中（FS 以外） ────
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) {
            return NotificationType.ONGOING
        }

        // ── 3. グループサマリー ────
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            return NotificationType.GROUP_SUMMARY
        }

        val isRemote = hasRemotePushMarkers(sbn)
        val isSilent = isSilentPriority(sbn)

        // ── 4 / 5. リモート（静音 or 通常） ────
        if (isRemote) {
            return if (isSilent) NotificationType.REMOTE_SILENT
            else NotificationType.REMOTE_PUSH
        }

        // ── 6. ローカル静音 ────
        if (isSilent) {
            return NotificationType.LOCAL_SILENT
        }

        // ── 7. 通常ローカル ────
        return NotificationType.LOCAL
    }

    /**
     * FCM / GCM 由来のキーが extras に含まれるかを判定する。
     * FLAG_LOCAL_ONLY が立っている場合はリモートとみなさない。
     */
    internal fun hasRemotePushMarkers(sbn: StatusBarNotification): Boolean {
        if (sbn.notification.flags and Notification.FLAG_LOCAL_ONLY != 0) return false

        val extras = sbn.notification.extras
        for (key in extras.keySet()) {
            if (key in REMOTE_PUSH_MARKER_KEYS) return true
        }
        return false
    }

    /**
     * 低優先度（サイレント）通知かどうかを判定する。
     *
     * - priority が LOW(-1) または MIN(-2)
     */
    internal fun isSilentPriority(sbn: StatusBarNotification): Boolean {
        @Suppress("DEPRECATION")
        val priority = sbn.notification.priority
        return priority <= Notification.PRIORITY_LOW
    }

    // ──────────────────────────────────────────────
    //  Extras JSON 生成
    // ──────────────────────────────────────────────

    private fun buildExtrasJson(sbn: StatusBarNotification): String {
        val extras = sbn.notification.extras
        val map = buildJsonObject {
            for (key in extras.keySet()) {
                val value = try {
                    extras.get(key)?.toString() ?: "null"
                } catch (_: Exception) {
                    "<unreadable>"
                }
                put(key, value)
            }
        }
        return map.toString()
    }
}


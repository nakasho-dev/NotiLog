package org.ukky.notilog.util

import android.app.Notification
import android.service.notification.StatusBarNotification
import org.ukky.notilog.data.db.entity.NotificationEntity
import org.ukky.notilog.data.db.entity.NotificationType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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

        val rawJson = buildRawJson(sbn)

        @Suppress("DEPRECATION")
        return NotificationEntity(
            packageName = sbn.packageName,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            ticker = ticker,
            extrasJson = extrasJson,
            rawJson = rawJson,
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

    // ──────────────────────────────────────────────
    //  通知受信時の生データ JSON 生成
    // ──────────────────────────────────────────────

    private val prettyJson = Json { prettyPrint = true }

    /**
     * StatusBarNotification の全フィールドを、加工・変換せずに
     * 整形 JSON 文字列としてダンプする。
     *
     * アプリ独自の加工データ（notificationType / signature / capturedAt 等）は
     * 含めず、Android OS から受け取った通知データのみを忠実に保持する。
     * この JSON は DB の raw_json カラムに保存され、JSON ビューア画面で
     * デバッグ用の生データとしてそのまま表示・コピーされる。
     */
    internal fun buildRawJson(sbn: StatusBarNotification): String {
        val notification = sbn.notification

        val extrasObj = buildJsonObject {
            val extras = notification.extras
            for (key in extras.keySet()) {
                val value = try {
                    extras.get(key)?.toString() ?: "null"
                } catch (_: Exception) {
                    "<unreadable>"
                }
                put(key, value)
            }
        }

        val json = buildJsonObject {
            // ── StatusBarNotification のフィールド ────
            put("packageName", sbn.packageName)
            put("id", sbn.id)
            put("key", sbn.key)
            put("postTime", sbn.postTime)
            put("tag", sbn.tag?.let { JsonPrimitive(it) } ?: JsonNull)
            put("groupKey", sbn.groupKey?.let { JsonPrimitive(it) } ?: JsonNull)
            put("isOngoing", sbn.isOngoing)
            put("isClearable", sbn.isClearable)

            // ── Notification のフィールド ────
            put("flags", notification.flags)
            @Suppress("DEPRECATION")
            put("priority", notification.priority)
            put("tickerText", notification.tickerText?.toString()
                ?.let { JsonPrimitive(it) } ?: JsonNull)
            put("category", notification.category
                ?.let { JsonPrimitive(it) } ?: JsonNull)
            put("channelId", notification.channelId
                ?.let { JsonPrimitive(it) } ?: JsonNull)
            put("group", notification.group
                ?.let { JsonPrimitive(it) } ?: JsonNull)
            put("sortKey", notification.sortKey
                ?.let { JsonPrimitive(it) } ?: JsonNull)
            put("when", notification.`when`)
            put("number", notification.number)

            // ── Extras（全 key-value） ────
            put("extras", extrasObj)
        }

        return prettyJson.encodeToString(JsonObject.serializer(), json)
    }
}


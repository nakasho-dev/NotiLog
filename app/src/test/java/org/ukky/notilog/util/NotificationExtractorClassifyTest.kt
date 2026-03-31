package org.ukky.notilog.util

import android.app.Notification
import android.os.BaseBundle
import android.os.Bundle
import android.service.notification.StatusBarNotification
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.ukky.notilog.data.db.entity.NotificationType

/**
 * NotificationExtractor の通知分類ロジックのテスト。
 *
 * Robolectric を使い StatusBarNotification / Notification をモックして
 * 7 種別の判定ロジックを検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationExtractorClassifyTest {

    // ══════════════════════════════════════════════
    //  1. FOREGROUND_SERVICE
    // ══════════════════════════════════════════════

    @Test
    fun `FLAG_FOREGROUND_SERVICEが立っている場合はFOREGROUND_SERVICE`() {
        val sbn = buildSbn(flags = Notification.FLAG_FOREGROUND_SERVICE)
        assertEquals(NotificationType.FOREGROUND_SERVICE, NotificationExtractor.classifyNotification(sbn))
    }

    @Test
    fun `FLAG_FOREGROUND_SERVICEとFCMマーカーが同時に存在する場合はFOREGROUND_SERVICEが優先`() {
        val sbn = buildSbn(
            flags = Notification.FLAG_FOREGROUND_SERVICE,
            extrasKeys = setOf("google.message_id"),
        )
        assertEquals(NotificationType.FOREGROUND_SERVICE, NotificationExtractor.classifyNotification(sbn))
    }

    @Test
    fun `FLAG_FOREGROUND_SERVICEとFLAG_ONGOING_EVENTが同時に立つ場合はFOREGROUND_SERVICE優先`() {
        val sbn = buildSbn(
            flags = Notification.FLAG_FOREGROUND_SERVICE or Notification.FLAG_ONGOING_EVENT,
        )
        assertEquals(NotificationType.FOREGROUND_SERVICE, NotificationExtractor.classifyNotification(sbn))
    }

    // ══════════════════════════════════════════════
    //  2. ONGOING
    // ══════════════════════════════════════════════

    @Test
    fun `FLAG_ONGOING_EVENTが立っている場合はONGOING`() {
        val sbn = buildSbn(flags = Notification.FLAG_ONGOING_EVENT)
        assertEquals(NotificationType.ONGOING, NotificationExtractor.classifyNotification(sbn))
    }

    @Test
    fun `FLAG_ONGOING_EVENTとFCMマーカーが同時に存在する場合はONGOINGが優先`() {
        val sbn = buildSbn(
            flags = Notification.FLAG_ONGOING_EVENT,
            extrasKeys = setOf("google.sent_time"),
        )
        assertEquals(NotificationType.ONGOING, NotificationExtractor.classifyNotification(sbn))
    }

    // ══════════════════════════════════════════════
    //  3. GROUP_SUMMARY
    // ══════════════════════════════════════════════

    @Test
    fun `FLAG_GROUP_SUMMARYが立っている場合はGROUP_SUMMARY`() {
        val sbn = buildSbn(flags = Notification.FLAG_GROUP_SUMMARY)
        assertEquals(NotificationType.GROUP_SUMMARY, NotificationExtractor.classifyNotification(sbn))
    }

    // ══════════════════════════════════════════════
    //  4. REMOTE_SILENT
    // ══════════════════════════════════════════════

    @Test
    fun `FCMマーカーがありPRIORITY_LOWならREMOTE_SILENT`() {
        val sbn = buildSbn(
            extrasKeys = setOf("google.message_id"),
            priority = Notification.PRIORITY_LOW,
        )
        assertEquals(NotificationType.REMOTE_SILENT, NotificationExtractor.classifyNotification(sbn))
    }

    @Test
    fun `FCMマーカーがありPRIORITY_MINならREMOTE_SILENT`() {
        val sbn = buildSbn(
            extrasKeys = setOf("google.c.sender.id"),
            priority = Notification.PRIORITY_MIN,
        )
        assertEquals(NotificationType.REMOTE_SILENT, NotificationExtractor.classifyNotification(sbn))
    }

    // ══════════════════════════════════════════════
    //  5. REMOTE_PUSH
    // ══════════════════════════════════════════════

    @Test
    fun `FCMマーカーがあり通常優先度ならREMOTE_PUSH`() {
        val sbn = buildSbn(
            extrasKeys = setOf("google.message_id"),
            priority = Notification.PRIORITY_DEFAULT,
        )
        assertEquals(NotificationType.REMOTE_PUSH, NotificationExtractor.classifyNotification(sbn))
    }

    @Test
    fun `FCMマーカーがあり高優先度ならREMOTE_PUSH`() {
        val sbn = buildSbn(
            extrasKeys = setOf("google.sent_time"),
            priority = Notification.PRIORITY_HIGH,
        )
        assertEquals(NotificationType.REMOTE_PUSH, NotificationExtractor.classifyNotification(sbn))
    }

    @Test
    fun `複数のFCMマーカーが存在する場合もREMOTE_PUSHと判定される`() {
        val sbn = buildSbn(
            extrasKeys = setOf("google.message_id", "google.sent_time", "google.c.a.e"),
            priority = Notification.PRIORITY_DEFAULT,
        )
        assertEquals(NotificationType.REMOTE_PUSH, NotificationExtractor.classifyNotification(sbn))
    }

    // ══════════════════════════════════════════════
    //  5b. FLAG_LOCAL_ONLY でリモート判定が無効になる
    // ══════════════════════════════════════════════

    @Test
    fun `FLAG_LOCAL_ONLYが立っていればFCMマーカーがあってもリモートとみなさない`() {
        val sbn = buildSbn(
            flags = Notification.FLAG_LOCAL_ONLY,
            extrasKeys = setOf("google.message_id"),
            priority = Notification.PRIORITY_DEFAULT,
        )
        // FLAG_LOCAL_ONLY → リモート判定無効 → 通常ローカル
        assertEquals(NotificationType.LOCAL, NotificationExtractor.classifyNotification(sbn))
    }

    @Test
    fun `FLAG_LOCAL_ONLYとPRIORITY_LOWでFCMマーカーがある場合はLOCAL_SILENT`() {
        val sbn = buildSbn(
            flags = Notification.FLAG_LOCAL_ONLY,
            extrasKeys = setOf("google.message_id"),
            priority = Notification.PRIORITY_LOW,
        )
        assertEquals(NotificationType.LOCAL_SILENT, NotificationExtractor.classifyNotification(sbn))
    }

    // ══════════════════════════════════════════════
    //  6. LOCAL_SILENT
    // ══════════════════════════════════════════════

    @Test
    fun `FCMマーカーなしでPRIORITY_LOWならLOCAL_SILENT`() {
        val sbn = buildSbn(priority = Notification.PRIORITY_LOW)
        assertEquals(NotificationType.LOCAL_SILENT, NotificationExtractor.classifyNotification(sbn))
    }

    @Test
    fun `FCMマーカーなしでPRIORITY_MINならLOCAL_SILENT`() {
        val sbn = buildSbn(priority = Notification.PRIORITY_MIN)
        assertEquals(NotificationType.LOCAL_SILENT, NotificationExtractor.classifyNotification(sbn))
    }

    // ══════════════════════════════════════════════
    //  7. LOCAL (デフォルト)
    // ══════════════════════════════════════════════

    @Test
    fun `フラグなしFCMマーカーなし通常優先度はLOCAL`() {
        val sbn = buildSbn()
        assertEquals(NotificationType.LOCAL, NotificationExtractor.classifyNotification(sbn))
    }

    @Test
    fun `PRIORITY_HIGHでFCMマーカーなしはLOCAL`() {
        val sbn = buildSbn(priority = Notification.PRIORITY_HIGH)
        assertEquals(NotificationType.LOCAL, NotificationExtractor.classifyNotification(sbn))
    }

    @Test
    fun `PRIORITY_MAXでFCMマーカーなしはLOCAL`() {
        val sbn = buildSbn(priority = Notification.PRIORITY_MAX)
        assertEquals(NotificationType.LOCAL, NotificationExtractor.classifyNotification(sbn))
    }

    // ══════════════════════════════════════════════
    //  ヘルパー関数テスト
    // ══════════════════════════════════════════════

    @Test
    fun `hasRemotePushMarkers_FCMキーなし_false`() {
        val sbn = buildSbn()
        assertFalse(NotificationExtractor.hasRemotePushMarkers(sbn))
    }

    @Test
    fun `hasRemotePushMarkers_FCMキーあり_true`() {
        val sbn = buildSbn(extrasKeys = setOf("google.message_id"))
        assertTrue(NotificationExtractor.hasRemotePushMarkers(sbn))
    }

    @Test
    fun `hasRemotePushMarkers_FLAG_LOCAL_ONLYがありFCMキーあり_false`() {
        val sbn = buildSbn(
            flags = Notification.FLAG_LOCAL_ONLY,
            extrasKeys = setOf("google.message_id"),
        )
        assertFalse(NotificationExtractor.hasRemotePushMarkers(sbn))
    }

    @Test
    fun `isSilentPriority_DEFAULT_false`() {
        val sbn = buildSbn(priority = Notification.PRIORITY_DEFAULT)
        assertFalse(NotificationExtractor.isSilentPriority(sbn))
    }

    @Test
    fun `isSilentPriority_LOW_true`() {
        val sbn = buildSbn(priority = Notification.PRIORITY_LOW)
        assertTrue(NotificationExtractor.isSilentPriority(sbn))
    }

    @Test
    fun `isSilentPriority_MIN_true`() {
        val sbn = buildSbn(priority = Notification.PRIORITY_MIN)
        assertTrue(NotificationExtractor.isSilentPriority(sbn))
    }

    @Test
    fun `isSilentPriority_HIGH_false`() {
        val sbn = buildSbn(priority = Notification.PRIORITY_HIGH)
        assertFalse(NotificationExtractor.isSilentPriority(sbn))
    }

    // ══════════════════════════════════════════════
    //  エッジケース
    // ══════════════════════════════════════════════

    @Test
    fun `全フラグが同時に立つ場合はFOREGROUND_SERVICEが最優先`() {
        val allFlags = Notification.FLAG_FOREGROUND_SERVICE or
            Notification.FLAG_ONGOING_EVENT or
            Notification.FLAG_GROUP_SUMMARY
        val sbn = buildSbn(
            flags = allFlags,
            extrasKeys = setOf("google.message_id"),
            priority = Notification.PRIORITY_LOW,
        )
        assertEquals(NotificationType.FOREGROUND_SERVICE, NotificationExtractor.classifyNotification(sbn))
    }

    @Test
    fun `FLAG_ONGOING_EVENTとFLAG_GROUP_SUMMARYが同時に立つ場合はONGOINGが優先`() {
        val sbn = buildSbn(
            flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_GROUP_SUMMARY,
        )
        assertEquals(NotificationType.ONGOING, NotificationExtractor.classifyNotification(sbn))
    }

    @Test
    fun `GCMレガシーキーでもリモート判定される`() {
        val sbn = buildSbn(
            extrasKeys = setOf("gcm.n.e"),
            priority = Notification.PRIORITY_DEFAULT,
        )
        assertEquals(NotificationType.REMOTE_PUSH, NotificationExtractor.classifyNotification(sbn))
    }

    @Test
    fun `Firebaseチャネルキーでもリモート判定される`() {
        val sbn = buildSbn(
            extrasKeys = setOf("com.google.firebase.messaging.default_notification_channel_id"),
            priority = Notification.PRIORITY_DEFAULT,
        )
        assertEquals(NotificationType.REMOTE_PUSH, NotificationExtractor.classifyNotification(sbn))
    }

    // ══════════════════════════════════════════════
    //  テストヘルパー
    // ══════════════════════════════════════════════

    /**
     * テスト用の StatusBarNotification を構築する。
     * Robolectric 環境で実際の Bundle を使用する。
     */
    @Suppress("DEPRECATION")
    private fun buildSbn(
        packageName: String = "com.example.test",
        flags: Int = 0,
        priority: Int = Notification.PRIORITY_DEFAULT,
        extrasKeys: Set<String> = emptySet(),
    ): StatusBarNotification {
        val extras = Bundle().apply {
            // 標準 extras
            putString(Notification.EXTRA_TITLE, "Test Title")
            putString(Notification.EXTRA_TEXT, "Test Text")
            // FCM/GCM マーカーキー
            for (key in extrasKeys) {
                putString(key, "marker_value")
            }
        }

        val notification = Notification().apply {
            this.flags = flags
            this.priority = priority
            this.extras = extras
        }

        return mockk<StatusBarNotification>(relaxed = true) {
            every { this@mockk.packageName } returns packageName
            every { this@mockk.notification } returns notification
        }
    }
}



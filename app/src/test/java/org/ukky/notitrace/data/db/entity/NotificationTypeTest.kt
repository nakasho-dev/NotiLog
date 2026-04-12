package org.ukky.notitrace.data.db.entity

import org.junit.Assert.*
import org.junit.Test

/**
 * NotificationType 列挙型の単体テスト。
 *
 * - code ↔ enum の相互変換
 * - 全エントリの網羅性
 * - 不明コードのフォールバック
 */
class NotificationTypeTest {

    // ── fromCode: 正常系 ────────────────────────────

    @Test
    fun `各codeから正しいenumに変換される`() {
        assertEquals(NotificationType.FOREGROUND_SERVICE, NotificationType.fromCode("foreground_service"))
        assertEquals(NotificationType.ONGOING, NotificationType.fromCode("ongoing"))
        assertEquals(NotificationType.GROUP_SUMMARY, NotificationType.fromCode("group_summary"))
        assertEquals(NotificationType.REMOTE_PUSH, NotificationType.fromCode("remote_push"))
        assertEquals(NotificationType.REMOTE_SILENT, NotificationType.fromCode("remote_silent"))
        assertEquals(NotificationType.LOCAL, NotificationType.fromCode("local"))
        assertEquals(NotificationType.LOCAL_SILENT, NotificationType.fromCode("local_silent"))
    }

    // ── fromCode: 異常系 ────────────────────────────

    @Test
    fun `不明なcodeはLOCALにフォールバックする`() {
        assertEquals(NotificationType.LOCAL, NotificationType.fromCode("unknown_type"))
    }

    @Test
    fun `空文字列はLOCALにフォールバックする`() {
        assertEquals(NotificationType.LOCAL, NotificationType.fromCode(""))
    }

    // ── code の一意性・網羅性 ────────────────────────

    @Test
    fun `全エントリのcodeが一意である`() {
        val codes = NotificationType.entries.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `全7種別が定義されている`() {
        assertEquals(7, NotificationType.entries.size)
    }

    // ── label / description の非空チェック ─────────

    @Test
    fun `全エントリのlabelが空でない`() {
        NotificationType.entries.forEach { type ->
            assertTrue("${type.name} の label が空", type.label.isNotBlank())
        }
    }

    @Test
    fun `全エントリのdescriptionが空でない`() {
        NotificationType.entries.forEach { type ->
            assertTrue("${type.name} の description が空", type.description.isNotBlank())
        }
    }

    // ── code → enum → code のラウンドトリップ ───────

    @Test
    fun `code変換のラウンドトリップが成立する`() {
        NotificationType.entries.forEach { original ->
            val restored = NotificationType.fromCode(original.code)
            assertEquals(original, restored)
        }
    }
}


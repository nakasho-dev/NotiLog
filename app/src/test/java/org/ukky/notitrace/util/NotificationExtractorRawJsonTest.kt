package org.ukky.notitrace.util

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * NotificationExtractor.buildRawJson() のテスト。
 *
 * buildRawJson は StatusBarNotification から直接全フィールドを読み取り、
 * アプリ独自の加工データ（notificationType / signature / capturedAt）を含めず
 * Android OS 由来の生データのみを JSON ダンプすることを検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationExtractorRawJsonTest {

    // ══════════════════════════════════════════════
    //  生データ JSON に OS 由来のフィールドが含まれること
    // ══════════════════════════════════════════════

    @Test
    fun `rawJsonにpackageNameが含まれる`() {
        val sbn = buildSbn(packageName = "com.example.app")
        val json = parseRawJson(sbn)
        assertEquals("com.example.app", json["packageName"]?.jsonPrimitive?.content)
    }

    @Test
    fun `rawJsonにidが含まれる`() {
        val sbn = buildSbn(id = 42)
        val json = parseRawJson(sbn)
        assertEquals(42, json["id"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `rawJsonにkeyが含まれる`() {
        val sbn = buildSbn(key = "0|com.example|42|null|10001")
        val json = parseRawJson(sbn)
        assertEquals("0|com.example|42|null|10001", json["key"]?.jsonPrimitive?.content)
    }

    @Test
    fun `rawJsonにpostTimeが含まれる`() {
        val sbn = buildSbn(postTime = 1700000000000L)
        val json = parseRawJson(sbn)
        assertEquals(1700000000000L, json["postTime"]?.jsonPrimitive?.content?.toLong())
    }

    @Test
    fun `rawJsonにtagがnullの場合JsonNullになる`() {
        val sbn = buildSbn(tag = null)
        val json = parseRawJson(sbn)
        assertEquals(JsonNull, json["tag"])
    }

    @Test
    fun `rawJsonにtagが存在する場合その値が入る`() {
        val sbn = buildSbn(tag = "my_tag")
        val json = parseRawJson(sbn)
        assertEquals("my_tag", json["tag"]?.jsonPrimitive?.content)
    }

    @Test
    fun `rawJsonにflagsが含まれる`() {
        val sbn = buildSbn(flags = Notification.FLAG_ONGOING_EVENT)
        val json = parseRawJson(sbn)
        assertEquals(Notification.FLAG_ONGOING_EVENT, json["flags"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `rawJsonにpriorityが含まれる`() {
        val sbn = buildSbn(priority = Notification.PRIORITY_HIGH)
        val json = parseRawJson(sbn)
        assertEquals(Notification.PRIORITY_HIGH, json["priority"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `rawJsonにisOngoingが含まれる`() {
        val sbn = buildSbn(flags = Notification.FLAG_ONGOING_EVENT)
        val json = parseRawJson(sbn)
        assertTrue(json.containsKey("isOngoing"))
    }

    @Test
    fun `rawJsonにisClearableが含まれる`() {
        val sbn = buildSbn()
        val json = parseRawJson(sbn)
        assertTrue(json.containsKey("isClearable"))
    }

    @Test
    fun `rawJsonにwhenが含まれる`() {
        val sbn = buildSbn(whenTime = 1700000000000L)
        val json = parseRawJson(sbn)
        assertEquals(1700000000000L, json["when"]?.jsonPrimitive?.content?.toLong())
    }

    @Test
    fun `rawJsonにnumberが含まれる`() {
        val sbn = buildSbn(number = 5)
        val json = parseRawJson(sbn)
        assertEquals(5, json["number"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `rawJsonにchannelIdが含まれる`() {
        val sbn = buildSbn(channelId = "my_channel")
        val json = parseRawJson(sbn)
        assertEquals("my_channel", json["channelId"]?.jsonPrimitive?.content)
    }

    @Test
    fun `rawJsonにcategoryがnullの場合JsonNullになる`() {
        val sbn = buildSbn(category = null)
        val json = parseRawJson(sbn)
        assertEquals(JsonNull, json["category"])
    }

    @Test
    fun `rawJsonにgroupKeyが含まれる`() {
        val sbn = buildSbn(groupKey = "group_123")
        val json = parseRawJson(sbn)
        assertEquals("group_123", json["groupKey"]?.jsonPrimitive?.content)
    }

    // ══════════════════════════════════════════════
    //  extras が生データとして含まれること
    // ══════════════════════════════════════════════

    @Test
    fun `rawJsonのextrasにBundle全キーが含まれる`() {
        val sbn = buildSbn(
            extrasKeys = mapOf(
                Notification.EXTRA_TITLE to "通知タイトル",
                Notification.EXTRA_TEXT to "通知テキスト",
                "google.message_id" to "fcm_123",
            ),
        )
        val json = parseRawJson(sbn)
        val extras = json["extras"]!!.jsonObject
        assertEquals("通知タイトル", extras[Notification.EXTRA_TITLE]?.jsonPrimitive?.content)
        assertEquals("通知テキスト", extras[Notification.EXTRA_TEXT]?.jsonPrimitive?.content)
        assertEquals("fcm_123", extras["google.message_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `rawJsonのextrasが空の場合は空オブジェクトになる`() {
        val sbn = buildSbn(extrasKeys = emptyMap())
        val json = parseRawJson(sbn)
        val extras = json["extras"]!!.jsonObject
        assertTrue(extras.isEmpty())
    }

    // ══════════════════════════════════════════════
    //  アプリ独自の加工データが含まれないこと
    // ══════════════════════════════════════════════

    @Test
    fun `rawJsonにnotificationTypeが含まれない`() {
        val sbn = buildSbn()
        val json = parseRawJson(sbn)
        assertFalse("rawJson should not contain notificationType", json.containsKey("notificationType"))
    }

    @Test
    fun `rawJsonにsignatureが含まれない`() {
        val sbn = buildSbn()
        val json = parseRawJson(sbn)
        assertFalse("rawJson should not contain signature", json.containsKey("signature"))
    }

    @Test
    fun `rawJsonにcapturedAtが含まれない`() {
        val sbn = buildSbn()
        val json = parseRawJson(sbn)
        assertFalse("rawJson should not contain capturedAt", json.containsKey("capturedAt"))
    }

    // ══════════════════════════════════════════════
    //  整形出力
    // ══════════════════════════════════════════════

    @Test
    fun `rawJsonはprettyPrint整形されている`() {
        val sbn = buildSbn()
        val rawJson = NotificationExtractor.buildRawJson(sbn)
        assertTrue("rawJson should contain newlines (prettyPrint)", rawJson.contains("\n"))
        assertTrue("rawJson should contain indentation", rawJson.contains("    "))
    }

    @Test
    fun `rawJsonは有効なJSONである`() {
        val sbn = buildSbn(
            extrasKeys = mapOf(
                Notification.EXTRA_TITLE to "テスト",
                "google.message_id" to "msg_1",
            ),
        )
        val rawJson = NotificationExtractor.buildRawJson(sbn)
        // パースに成功すれば有効な JSON
        val parsed = Json.parseToJsonElement(rawJson)
        assertNotNull(parsed)
    }

    // ══════════════════════════════════════════════
    //  テストヘルパー
    // ══════════════════════════════════════════════

    private fun parseRawJson(sbn: StatusBarNotification) =
        Json.parseToJsonElement(NotificationExtractor.buildRawJson(sbn)).jsonObject

    @Suppress("DEPRECATION")
    private fun buildSbn(
        packageName: String = "com.example.test",
        id: Int = 0,
        key: String = "0|com.example.test|0|null|10001",
        postTime: Long = 0L,
        tag: String? = null,
        groupKey: String? = null,
        flags: Int = 0,
        priority: Int = Notification.PRIORITY_DEFAULT,
        channelId: String? = null,
        category: String? = null,
        group: String? = null,
        sortKey: String? = null,
        whenTime: Long = 0L,
        number: Int = 0,
        extrasKeys: Map<String, String> = mapOf(
            Notification.EXTRA_TITLE to "Test Title",
            Notification.EXTRA_TEXT to "Test Text",
        ),
    ): StatusBarNotification {
        val extras = Bundle().apply {
            for ((k, v) in extrasKeys) {
                putString(k, v)
            }
        }

        val notification = mockk<Notification>(relaxed = true) {
            every { this@mockk.flags } returns flags
            every { this@mockk.priority } returns priority
            every { this@mockk.extras } returns extras
            every { this@mockk.channelId } returns channelId
            every { this@mockk.category } returns category
            every { this@mockk.group } returns group
            every { this@mockk.sortKey } returns sortKey
            every { this@mockk.`when` } returns whenTime
            every { this@mockk.number } returns number
            every { this@mockk.tickerText } returns null
        }

        return mockk<StatusBarNotification>(relaxed = true) {
            every { this@mockk.packageName } returns packageName
            every { this@mockk.id } returns id
            every { this@mockk.key } returns key
            every { this@mockk.postTime } returns postTime
            every { this@mockk.tag } returns tag
            every { this@mockk.groupKey } returns groupKey
            every { this@mockk.notification } returns notification
            every { this@mockk.isOngoing } returns (flags and Notification.FLAG_ONGOING_EVENT != 0)
            every { this@mockk.isClearable } returns (flags and Notification.FLAG_ONGOING_EVENT == 0)
        }
    }
}



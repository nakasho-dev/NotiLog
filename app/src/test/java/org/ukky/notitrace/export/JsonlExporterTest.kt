package org.ukky.notitrace.export

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.ukky.notitrace.data.db.entity.NotificationEntity
import org.ukky.notitrace.data.db.entity.NotificationWithTag
import org.ukky.notitrace.data.db.entity.RawLogWithTag
import java.io.ByteArrayOutputStream

/**
 * JsonlExporter の単体テスト。
 *
 * - 集約済みエクスポート（export）と生データエクスポート（exportRawLogs）の両方をテスト
 */
class JsonlExporterTest {

    private lateinit var exporter: JsonlExporter
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        exporter = JsonlExporter()
    }

    // ══════════════════════════════════════════════════
    //  集約済みエクスポート（export）
    // ══════════════════════════════════════════════════

    @Test
    fun `空リストを渡すと空ファイルが出力される`() {
        val out = ByteArrayOutputStream()
        exporter.export(emptyList(), out)
        assertEquals("", out.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun `1件の通知が1行のJSONとして出力される`() {
        val item = notificationWithTag(
            id = 1L,
            packageName = "com.example",
            title = "タイトル",
            text = "本文",
            tag = "SNS",
        )
        val out = ByteArrayOutputStream()
        exporter.export(listOf(item), out)

        val lines = out.toString(Charsets.UTF_8.name()).lines().filter { it.isNotEmpty() }
        assertEquals(1, lines.size)
    }

    @Test
    fun `2件の通知が2行のJSONとして出力される`() {
        val items = listOf(
            notificationWithTag(id = 1L, packageName = "com.a", title = "A", text = null, tag = "仕事"),
            notificationWithTag(id = 2L, packageName = "com.b", title = "B", text = "テキスト", tag = null),
        )
        val out = ByteArrayOutputStream()
        exporter.export(items, out)

        val lines = out.toString(Charsets.UTF_8.name()).lines().filter { it.isNotEmpty() }
        assertEquals(2, lines.size)
    }

    @Test
    fun `各フィールドが正しくシリアライズされる`() {
        val item = notificationWithTag(
            id = 42L,
            packageName = "com.test.app",
            title = "テストタイトル",
            text = "テスト本文",
            bigText = "拡張テキスト",
            subText = "サブテキスト",
            ticker = "ティッカー",
            tag = "テストタグ",
            appLabel = "テストアプリ",
            notificationType = "remote_push",
            receiveCount = 5,
            firstReceivedAt = 1_000_000L,
            lastReceivedAt = 2_000_000L,
        )
        val out = ByteArrayOutputStream()
        exporter.export(listOf(item), out)

        val line = out.toString(Charsets.UTF_8.name()).trim()
        val exported = json.decodeFromString(JsonlExportItem.serializer(), line)

        assertEquals(42L, exported.id)
        assertEquals("com.test.app", exported.packageName)
        assertEquals("テストタイトル", exported.title)
        assertEquals("テスト本文", exported.text)
        assertEquals("拡張テキスト", exported.bigText)
        assertEquals("サブテキスト", exported.subText)
        assertEquals("ティッカー", exported.ticker)
        assertEquals("テストタグ", exported.tag)
        assertEquals("テストアプリ", exported.appLabel)
        assertEquals("remote_push", exported.notificationType)
        assertEquals(5, exported.receiveCount)
        assertEquals(1_000_000L, exported.firstReceivedAt)
        assertEquals(2_000_000L, exported.lastReceivedAt)
    }

    @Test
    fun `タグなし通知のtagとappLabelはnullになる`() {
        val item = notificationWithTag(
            id = 1L,
            packageName = "com.noTag",
            title = "タイトル",
            text = null,
            tag = null,
            appLabel = null,
        )
        val out = ByteArrayOutputStream()
        exporter.export(listOf(item), out)

        val line = out.toString(Charsets.UTF_8.name()).trim()
        val exported = json.decodeFromString(JsonlExportItem.serializer(), line)

        assertNull(exported.tag)
        assertNull(exported.appLabel)
    }

    @Test
    fun `nullフィールドを含む通知が正しくシリアライズされる`() {
        val item = notificationWithTag(
            id = 3L,
            packageName = "com.minimal",
            title = null,
            text = null,
            bigText = null,
            subText = null,
            ticker = null,
            tag = null,
        )
        val out = ByteArrayOutputStream()
        exporter.export(listOf(item), out)

        val line = out.toString(Charsets.UTF_8.name()).trim()
        val exported = json.decodeFromString(JsonlExportItem.serializer(), line)
        assertNull(exported.title)
        assertNull(exported.text)
    }

    @Test
    fun `UTF-8でエンコードされている`() {
        val item = notificationWithTag(
            id = 1L,
            packageName = "com.jp",
            title = "日本語通知タイトル",
            text = "日本語テキスト",
            tag = "日本語タグ",
        )
        val out = ByteArrayOutputStream()
        exporter.export(listOf(item), out)

        val content = out.toString(Charsets.UTF_8.name())
        assertTrue(content.contains("日本語通知タイトル"))
        assertTrue(content.contains("日本語テキスト"))
        assertTrue(content.contains("日本語タグ"))
    }

    // ══════════════════════════════════════════════════
    //  生データエクスポート（exportRawLogs）
    // ══════════════════════════════════════════════════

    @Test
    fun `rawLog空リストを渡すと空ファイルが出力される`() {
        val out = ByteArrayOutputStream()
        exporter.exportRawLogs(emptyList(), out)
        assertEquals("", out.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun `rawLog_1件が1行のJSONとして出力される`() {
        val item = RawLogWithTag(
            rawJson = """{"packageName":"com.example","id":1}""",
            receivedAt = 1000L,
            packageName = "com.example",
            notificationType = "local",
            tag = "SNS",
            appLabel = "ExampleApp",
        )
        val out = ByteArrayOutputStream()
        exporter.exportRawLogs(listOf(item), out)

        val lines = out.toString(Charsets.UTF_8.name()).lines().filter { it.isNotEmpty() }
        assertEquals(1, lines.size)
    }

    @Test
    fun `rawLog_各フィールドが正しくシリアライズされる`() {
        val item = RawLogWithTag(
            rawJson = """{"packageName":"com.test","id":42}""",
            receivedAt = 1_234_567L,
            packageName = "com.test",
            notificationType = "remote_push",
            tag = "テスト",
            appLabel = "テストアプリ",
        )
        val out = ByteArrayOutputStream()
        exporter.exportRawLogs(listOf(item), out)

        val line = out.toString(Charsets.UTF_8.name()).trim()
        val exported = json.decodeFromString(JsonlRawExportItem.serializer(), line)

        assertEquals("""{"packageName":"com.test","id":42}""", exported.rawJson)
        assertEquals(1_234_567L, exported.receivedAt)
        assertEquals("com.test", exported.packageName)
        assertEquals("remote_push", exported.notificationType)
        assertEquals("テスト", exported.tag)
        assertEquals("テストアプリ", exported.appLabel)
    }

    @Test
    fun `rawLog_重複通知の受信順に出力される`() {
        val items = listOf(
            RawLogWithTag("""{"n":1}""", 1000L, "com.a", "local", null, null),
            RawLogWithTag("""{"n":2}""", 2000L, "com.a", "local", null, null),
            RawLogWithTag("""{"n":3}""", 3000L, "com.a", "local", null, null),
        )
        val out = ByteArrayOutputStream()
        exporter.exportRawLogs(items, out)

        val lines = out.toString(Charsets.UTF_8.name()).lines().filter { it.isNotEmpty() }
        assertEquals(3, lines.size)
        // 受信順の検証
        val first = json.decodeFromString(JsonlRawExportItem.serializer(), lines[0])
        val last = json.decodeFromString(JsonlRawExportItem.serializer(), lines[2])
        assertEquals(1000L, first.receivedAt)
        assertEquals(3000L, last.receivedAt)
    }

    @Test
    fun `rawLog_タグなしの場合tagとappLabelはnull`() {
        val item = RawLogWithTag(
            rawJson = """{}""",
            receivedAt = 1000L,
            packageName = "com.noTag",
            notificationType = "local",
            tag = null,
            appLabel = null,
        )
        val out = ByteArrayOutputStream()
        exporter.exportRawLogs(listOf(item), out)

        val line = out.toString(Charsets.UTF_8.name()).trim()
        val exported = json.decodeFromString(JsonlRawExportItem.serializer(), line)
        assertNull(exported.tag)
        assertNull(exported.appLabel)
    }

    // ── ヘルパー ──────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun notificationWithTag(
        id: Long = 0L,
        packageName: String = "com.test",
        title: String? = null,
        text: String? = null,
        bigText: String? = null,
        subText: String? = null,
        ticker: String? = null,
        tag: String? = null,
        appLabel: String? = null,
        notificationType: String = "local",
        receiveCount: Int = 1,
        firstReceivedAt: Long = 1000L,
        lastReceivedAt: Long = 1000L,
    ) = NotificationWithTag(
        notification = NotificationEntity(
            id = id,
            packageName = packageName,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            ticker = ticker,
            extrasJson = "{}",
            rawJson = "{}",
            signature = "sig_$id",
            notificationType = notificationType,
            isRemote = false,
            receiveCount = receiveCount,
            firstReceivedAt = firstReceivedAt,
            lastReceivedAt = lastReceivedAt,
        ),
        tag = tag,
        appLabel = appLabel,
    )
}

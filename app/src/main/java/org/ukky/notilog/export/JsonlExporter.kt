package org.ukky.notilog.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ukky.notilog.data.db.entity.NotificationWithTag
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知データを JSONL（JSON Lines）形式でエクスポートする。
 *
 * 各通知を 1 行の JSON オブジェクトとして出力する。
 * タグ情報（タグ名・アプリ表示名）も各行に含める。
 *
 * フォーマット例:
 * ```
 * {"id":1,"packageName":"com.example","title":"件名","text":"本文",...}
 * {"id":2,"packageName":"com.other","title":null,"text":"テキスト",...}
 * ```
 */
@Singleton
class JsonlExporter @Inject constructor() {

    private val json = Json { encodeDefaults = true }

    /**
     * 通知リストを JSONL 形式で [outputStream] に書き出す。
     *
     * @param items エクスポート対象の通知（タグ情報付き）
     * @param outputStream 出力先ストリーム（呼び出し側でクローズすること）
     */
    fun export(items: List<NotificationWithTag>, outputStream: OutputStream) {
        val writer = outputStream.bufferedWriter(Charsets.UTF_8)
        items.forEach { item ->
            writer.write(json.encodeToString(item.toExportItem()))
            writer.newLine()
        }
        writer.flush()
    }

    private fun NotificationWithTag.toExportItem() = JsonlExportItem(
        id = notification.id,
        packageName = notification.packageName,
        title = notification.title,
        text = notification.text,
        bigText = notification.bigText,
        subText = notification.subText,
        ticker = notification.ticker,
        tag = tag,
        appLabel = appLabel,
        notificationType = notification.notificationType,
        receiveCount = notification.receiveCount,
        firstReceivedAt = notification.firstReceivedAt,
        lastReceivedAt = notification.lastReceivedAt,
    )
}

/**
 * JSONL エクスポートの 1 行分のデータモデル。
 *
 * - [id] は DB の主キー（同一端末のみ有効）
 * - [tag] / [appLabel] は紐付きタグがない場合 null
 */
@Serializable
data class JsonlExportItem(
    val id: Long,
    val packageName: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val ticker: String?,
    val tag: String?,
    val appLabel: String?,
    val notificationType: String,
    val receiveCount: Int,
    val firstReceivedAt: Long,
    val lastReceivedAt: Long,
)


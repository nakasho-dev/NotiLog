package org.ukky.notilog.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

/**
 * 全文検索用 FTS4 仮想テーブル。
 *
 * contentEntity に NotificationEntity を指定し、Room が自動で同期トリガーを生成する。
 * unicode61 トークナイザで日本語を含む通知にも FTS4 を適用しやすくするが、
 * 短い語句や 1 文字検索は LIKE フォールバックで補完する前提とする。
 */
@Fts4(contentEntity = NotificationEntity::class, tokenizer = "unicode61")
@Entity(tableName = "notifications_fts")
data class NotificationFtsEntity(
    val title: String?,
    val text: String?,
    @ColumnInfo(name = "big_text")
    val bigText: String?,
    @ColumnInfo(name = "sub_text")
    val subText: String?,
)


package org.ukky.notitrace.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * パッケージ単位のタグテーブル。
 *
 * 通知テーブルとは JOIN で結合するため、
 * タグ変更は過去ログ全件に即時反映される。
 */
@Entity(
    tableName = "app_tags",
    indices = [Index(value = ["tag"])]
)
data class AppTagEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    val tag: String,

    @ColumnInfo(name = "app_label")
    val appLabel: String?,
)


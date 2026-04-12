package org.ukky.notitrace.ui.screen.tag

/**
 * タグ管理画面の1行分のUIモデル。
 *
 * 通知実績のある全パッケージを表示し、タグは未設定 (null) の場合もある。
 */
data class TagManageItem(
    val packageName: String,
    val appLabel: String?,
    val tag: String?,
)


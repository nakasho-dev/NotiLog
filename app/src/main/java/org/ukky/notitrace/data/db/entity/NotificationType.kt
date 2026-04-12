package org.ukky.notitrace.data.db.entity

/**
 * 通知の種別を網羅的に分類する列挙型。
 *
 * [StatusBarNotification] の flags / priority / extras から
 * ヒューリスティクスで判定し、DB には [code] を TEXT として保存する。
 *
 * 判定優先度（上が最優先）:
 *  1. FOREGROUND_SERVICE – FLAG_FOREGROUND_SERVICE
 *  2. ONGOING            – FLAG_ONGOING_EVENT（FS 以外）
 *  3. GROUP_SUMMARY      – FLAG_GROUP_SUMMARY
 *  4. REMOTE_SILENT      – FCM マーカー有 + 低優先度
 *  5. REMOTE_PUSH        – FCM マーカー有
 *  6. LOCAL_SILENT        – 低優先度（MIN / LOW）
 *  7. LOCAL              – 上記いずれにも該当しない
 */
enum class NotificationType(
    val code: String,
    val label: String,
    val description: String,
) {
    /** フォアグラウンドサービスの常駐通知 */
    FOREGROUND_SERVICE(
        code = "foreground_service",
        label = "常駐",
        description = "フォアグラウンドサービスの常駐通知",
    ),

    /** 進行中の通知（メディア再生、ダウンロード等） */
    ONGOING(
        code = "ongoing",
        label = "進行中",
        description = "スワイプで消せない進行中の通知",
    ),

    /** 通知グループのサマリー */
    GROUP_SUMMARY(
        code = "group_summary",
        label = "グループ",
        description = "通知グループのサマリー",
    ),

    /** FCM/GCM 経由のリモートプッシュ通知 */
    REMOTE_PUSH(
        code = "remote_push",
        label = "リモート",
        description = "サーバーからのプッシュ通知",
    ),

    /** FCM/GCM 経由だが低優先度のサイレントプッシュ */
    REMOTE_SILENT(
        code = "remote_silent",
        label = "リモート静音",
        description = "サーバーからの静音プッシュ通知",
    ),

    /** アプリが生成した通常のローカル通知 */
    LOCAL(
        code = "local",
        label = "ローカル",
        description = "アプリが生成した通知",
    ),

    /** アプリが生成した低優先度のサイレント通知 */
    LOCAL_SILENT(
        code = "local_silent",
        label = "サイレント",
        description = "アプリが生成した静音通知",
    );

    companion object {
        private val codeMap = entries.associateBy { it.code }

        /** DB に保存された code 文字列から enum に変換する。不明値は [LOCAL] にフォールバック。 */
        fun fromCode(code: String): NotificationType =
            codeMap[code] ?: LOCAL
    }
}


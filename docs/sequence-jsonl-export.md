# シーケンス図: JSONL エクスポートフロー

> **対象機能**: F-15 JSONL エクスポート（集約済み） / F-16 JSONL 生データエクスポート（受信順）  
> **最終更新**: 2026-04-05

---

## 1. 集約済み全件エクスポートフロー（F-15）

```mermaid
sequenceDiagram
    actor User
    participant SettingsScreen
    participant SettingsViewModel
    participant NotificationRepository
    participant NotificationDao
    participant JsonlExporter
    participant SAF as Storage Access Framework

    User->>SettingsScreen: タグ選択「すべて」のまま<br/>「JSONLエクスポート（集約済み）」ボタンタップ
    SettingsScreen->>SAF: CreateDocument("notitrace_export.jsonl")
    SAF-->>User: ファイル保存先選択ダイアログ表示
    User->>SAF: 保存先を選択して確定
    SAF-->>SettingsScreen: 保存先 URI を返却

    SettingsScreen->>SettingsViewModel: exportJsonl(context, uri, tag=null)
    SettingsViewModel->>SettingsViewModel: isJsonlExporting = true

    SettingsViewModel->>NotificationRepository: getForExport(tag=null)
    NotificationRepository->>NotificationDao: getAllWithTagList()
    NotificationDao-->>NotificationRepository: List<NotificationWithTag>（全件）
    NotificationRepository-->>SettingsViewModel: List<NotificationWithTag>

    SettingsViewModel->>SAF: openOutputStream(uri)
    SAF-->>SettingsViewModel: OutputStream

    SettingsViewModel->>JsonlExporter: export(items, outputStream)
    loop 各通知
        JsonlExporter->>JsonlExporter: toExportItem() → JsonlExportItem
        JsonlExporter->>JsonlExporter: json.encodeToString(item) + newLine()
    end
    JsonlExporter->>JsonlExporter: writer.flush()
    JsonlExporter-->>SettingsViewModel: 完了

    SettingsViewModel->>SettingsViewModel: isJsonlExporting = false<br/>message = "JSONLエクスポート完了（全件）: N件"
    SettingsScreen-->>User: Snackbar でメッセージ表示
```

---

## 2. 集約済みタグフィルタエクスポートフロー（F-15）

```mermaid
sequenceDiagram
    actor User
    participant SettingsScreen
    participant SettingsViewModel
    participant AppTagRepository
    participant NotificationRepository
    participant NotificationDao
    participant JsonlExporter
    participant SAF as Storage Access Framework

    Note over SettingsScreen,AppTagRepository: 画面表示時にタグ一覧を収集
    SettingsScreen->>SettingsViewModel: tags StateFlow 購読
    SettingsViewModel->>AppTagRepository: getAllTags()
    AppTagRepository-->>SettingsViewModel: Flow<List<String>>
    SettingsViewModel-->>SettingsScreen: tags（例: ["SNS", "仕事"]）

    User->>SettingsScreen: ドロップダウンで「SNS」を選択
    User->>SettingsScreen: 「JSONLエクスポート（集約済み）」ボタンタップ
    SettingsScreen->>SAF: CreateDocument("notitrace_export.jsonl")
    SAF-->>User: ファイル保存先選択ダイアログ表示
    User->>SAF: 保存先を選択して確定
    SAF-->>SettingsScreen: 保存先 URI を返却

    SettingsScreen->>SettingsViewModel: exportJsonl(context, uri, tag="SNS")
    SettingsViewModel->>SettingsViewModel: isJsonlExporting = true

    SettingsViewModel->>NotificationRepository: getForExport(tag="SNS")
    NotificationRepository->>NotificationDao: getByTagList("SNS")
    NotificationDao-->>NotificationRepository: List<NotificationWithTag>（"SNS"タグのみ）
    NotificationRepository-->>SettingsViewModel: List<NotificationWithTag>

    SettingsViewModel->>SAF: openOutputStream(uri)
    SAF-->>SettingsViewModel: OutputStream

    SettingsViewModel->>JsonlExporter: export(items, outputStream)
    JsonlExporter-->>SettingsViewModel: 完了

    SettingsViewModel->>SettingsViewModel: isJsonlExporting = false<br/>message = "JSONLエクスポート完了（タグ: SNS）: N件"
    SettingsScreen-->>User: Snackbar でメッセージ表示
```

---

## 3. 生データエクスポートフロー（F-16）

```mermaid
sequenceDiagram
    actor User
    participant SettingsScreen
    participant SettingsViewModel
    participant NotificationRepository
    participant RawLogDao as NotificationRawLogDao
    participant JsonlExporter
    participant SAF as Storage Access Framework

    User->>SettingsScreen: タグ選択（すべて or 特定タグ）<br/>「JSONLエクスポート（生データ・受信順）」ボタンタップ
    SettingsScreen->>SAF: CreateDocument("notitrace_raw_export.jsonl")
    SAF-->>User: ファイル保存先選択ダイアログ表示
    User->>SAF: 保存先を選択して確定
    SAF-->>SettingsScreen: 保存先 URI を返却

    SettingsScreen->>SettingsViewModel: exportRawJsonl(context, uri, tag)
    SettingsViewModel->>SettingsViewModel: isRawJsonlExporting = true

    SettingsViewModel->>NotificationRepository: getForRawExport(tag)

    alt tag == null（全件）
        NotificationRepository->>RawLogDao: getAllWithTagOrderByReceivedAt()
    else tag != null（タグフィルタ）
        NotificationRepository->>RawLogDao: getByTagOrderByReceivedAt(tag)
    end

    RawLogDao-->>NotificationRepository: List<RawLogWithTag>（受信時刻 ASC）
    NotificationRepository-->>SettingsViewModel: List<RawLogWithTag>

    SettingsViewModel->>SAF: openOutputStream(uri)
    SAF-->>SettingsViewModel: OutputStream

    SettingsViewModel->>JsonlExporter: exportRawLogs(items, outputStream)
    loop 各 rawLog
        JsonlExporter->>JsonlExporter: toRawExportItem() → JsonlRawExportItem
        JsonlExporter->>JsonlExporter: json.encodeToString(item) + newLine()
    end
    JsonlExporter->>JsonlExporter: writer.flush()
    JsonlExporter-->>SettingsViewModel: 完了

    SettingsViewModel->>SettingsViewModel: isRawJsonlExporting = false<br/>message = "生データJSONLエクスポート完了: N件"
    SettingsScreen-->>User: Snackbar でメッセージ表示
```

---

## 4. JSONL 出力フォーマット

### 4.1 集約済みエクスポート（F-15）

#### フォーマット概要

- ファイル拡張子: `.jsonl`
- エンコーディング: UTF-8
- 改行コード: LF (`\n`)
- 1 行 = 1 通知（JSON オブジェクト）— 重複集約済み
- 最後の行末に改行あり

#### フィールド仕様

| フィールド | 型 | 説明 |
|---|---|---|
| `id` | Long | DB 主キー（同一端末のみ有効） |
| `packageName` | String | 通知発行元パッケージ名 |
| `title` | String? | 通知タイトル（null 可） |
| `text` | String? | 通知テキスト（null 可） |
| `bigText` | String? | 拡張テキスト（null 可） |
| `subText` | String? | サブテキスト（null 可） |
| `ticker` | String? | ティッカーテキスト（null 可） |
| `tag` | String? | アプリに付与されたタグ（タグなしの場合 null） |
| `appLabel` | String? | アプリ表示名キャッシュ（null 可） |
| `notificationType` | String | 通知種別コード（`local`, `remote_push` 等） |
| `receiveCount` | Int | 同一通知の受信回数 |
| `firstReceivedAt` | Long | 初回受信時刻（Unix ミリ秒） |
| `lastReceivedAt` | Long | 最終受信時刻（Unix ミリ秒） |

#### 出力例

```jsonl
{"id":1,"packageName":"com.example.sns","title":"新しいメッセージ","text":"田中さんからメッセージが届きました","bigText":null,"subText":null,"ticker":null,"tag":"SNS","appLabel":"SNSアプリ","notificationType":"remote_push","receiveCount":1,"firstReceivedAt":1743811200000,"lastReceivedAt":1743811200000}
{"id":2,"packageName":"com.example.mail","title":null,"text":"メール受信","bigText":"本文テキスト","subText":"受信トレイ","ticker":"メール受信","tag":"仕事","appLabel":"メールアプリ","notificationType":"local","receiveCount":3,"firstReceivedAt":1743800000000,"lastReceivedAt":1743811000000}
```

### 4.2 生データエクスポート（F-16）

#### フォーマット概要

- ファイル拡張子: `.jsonl`
- エンコーディング: UTF-8
- 改行コード: LF (`\n`)
- 1 行 = 1 受信（JSON オブジェクト）— 重複集約前の個々の受信データ
- 出力順: 受信時刻昇順（ASC）
- 最後の行末に改行あり

#### フィールド仕様

| フィールド | 型 | 説明 |
|---|---|---|
| `rawJson` | String | 受信時に `StatusBarNotification` から直接ダンプした生データ JSON |
| `receivedAt` | Long | 受信時刻（Unix ミリ秒） |
| `packageName` | String | 通知発行元パッケージ名 |
| `notificationType` | String | 通知種別コード |
| `tag` | String? | アプリに付与されたタグ（null 可） |
| `appLabel` | String? | アプリ表示名（null 可） |

#### 出力例

```jsonl
{"rawJson":"{\"packageName\":\"com.example.sns\",\"id\":1,\"key\":\"0|com.example.sns|1|null|10001\",\"postTime\":1743811200000,...}","receivedAt":1743811200000,"packageName":"com.example.sns","notificationType":"remote_push","tag":"SNS","appLabel":"SNSアプリ"}
{"rawJson":"{\"packageName\":\"com.example.sns\",\"id\":1,\"key\":\"0|com.example.sns|1|null|10001\",\"postTime\":1743811500000,...}","receivedAt":1743811500000,"packageName":"com.example.sns","notificationType":"remote_push","tag":"SNS","appLabel":"SNSアプリ"}
```

---

## 5. 設計上の留意点

| 項目 | 詳細 |
|---|---|
| **暗号化なし** | JSONL エクスポートはプレーンテキストである。バックアップ用途には既存の暗号化バックアップ（`notitrace_backup.bin`）を使用すること |
| **SAF 経由** | ファイル保存はすべて Storage Access Framework 経由。アプリはファイルシステムへの直接アクセスを行わない |
| **集約済みエクスポートに extras_json / raw_json は含まない** | プライバシー上の配慮から、集約済み JSONL に生 extras データおよび raw_json は含めない |
| **生データエクスポートには rawJson を含む** | 生データエクスポートは受信ごとの `rawJson`（Android OS 由来の通知データ）を含む。デバッグ・分析用途を想定 |
| **id は端末固有** | `id` フィールドは DB の autoincrement 値のため、別端末へのインポート時には使用しないこと |
| **rawLog の保持期間** | `notification_raw_logs` テーブルには保持日数が設定可能。期限切れのデータは削除されるため、生データエクスポートの対象外となる |

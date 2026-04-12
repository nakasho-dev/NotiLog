# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ========== デバッグ用：クラッシュログにファイル名と行番号を残す ==========
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ========== Kotlin ==========
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ========== Room ==========
# Room のエンティティ・DAO はリフレクション経由で使われるため保持
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
# Room コード生成クラス (_Impl) を保持
-keep class **_Impl { *; }
-keep class **_Impl$* { *; }
-dontwarn androidx.room.**

# ========== Hilt / Dagger ==========
# Hilt が内包するコンシューマルールで概ねカバーされるが念のため
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-dontwarn dagger.**
-dontwarn javax.inject.**

# ========== Kotlinx Serialization ==========
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
# @Serializable アノテーション付きクラスを保持
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    static ** $serializer;
    static **[] $serializer;
    *** serializer(...);
    *** Companion;
}
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ========== SQLCipher ==========
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# ========== AboutLibraries ==========
-keep class com.mikepenz.aboutlibraries.** { *; }
-dontwarn com.mikepenz.aboutlibraries.**

# ========== Navigation Compose (Route sealed class / object) ==========
# シリアライゼーション経由で使われるルートクラスを保持
-keep class org.ukky.notitrace.ui.navigation.** { *; }

# ========== NotiTrace アプリ固有クラス ==========
# Room エンティティ（フィールド名がカラム名に対応）
-keep class org.ukky.notitrace.data.db.entity.** { *; }
# バックアップデータ（Kotlinx Serialization で直列化）
-keep class org.ukky.notitrace.backup.** { *; }
# 通知タイプ enum
-keepclassmembers enum org.ukky.notitrace.** { *; }

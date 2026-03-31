package org.ukky.notilog.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.ukky.notilog.data.db.dao.AppTagDao
import org.ukky.notilog.data.db.dao.NotificationDao
import org.ukky.notilog.data.db.entity.AppTagEntity
import org.ukky.notilog.data.db.entity.NotificationEntity
import org.ukky.notilog.data.db.entity.NotificationFtsEntity

@Database(
    entities = [
        NotificationEntity::class,
        NotificationFtsEntity::class,
        AppTagEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class NotiLogDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun appTagDao(): AppTagDao

    companion object {
        /** v1 → v2: リモート/ローカル通知判定用カラムを追加 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notifications ADD COLUMN is_remote INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /** v2 → v3: 通知種別カラム (notification_type) を追加し、is_remote からバックフィル */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notifications ADD COLUMN notification_type TEXT NOT NULL DEFAULT 'local'"
                )
                // 既存データ: is_remote=1 → 'remote_push' にバックフィル
                db.execSQL(
                    "UPDATE notifications SET notification_type = 'remote_push' WHERE is_remote = 1"
                )
            }
        }

        /** v1 → v3: v1 から直接 v3 へ移行（is_remote + notification_type を一括追加） */
        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notifications ADD COLUMN is_remote INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE notifications ADD COLUMN notification_type TEXT NOT NULL DEFAULT 'local'"
                )
            }
        }
    }
}


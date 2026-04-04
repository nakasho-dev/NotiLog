package org.ukky.notilog.data.db

import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.ukky.notilog.data.crypto.KeyStoreManager
import java.security.SecureRandom

/**
 * SQLCipher 暗号化付きの Room Database を提供する。
 *
 * 暗号鍵フロー:
 * 1. 初回: ランダムパスフレーズ生成 → Keystore で暗号化 → SharedPreferences に保存
 * 2. 2回目以降: SharedPreferences から取得 → Keystore で復号 → SQLCipher に渡す
 */
object DatabaseProvider {

    private const val PREFS_NAME = "notilog_db_prefs"
    private const val KEY_ENCRYPTED_PASSPHRASE = "encrypted_passphrase"
    private const val PASSPHRASE_LENGTH = 32

    @Volatile
    private var INSTANCE: NotiLogDatabase? = null

    fun getDatabase(context: Context): NotiLogDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }
    }

    private fun buildDatabase(context: Context): NotiLogDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = getOrCreatePassphrase(context)
        val factory: SupportSQLiteOpenHelper.Factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context.applicationContext,
            NotiLogDatabase::class.java,
            "notilog.db"
        )
            .openHelperFactory(factory)
            .addMigrations(
                NotiLogDatabase.MIGRATION_1_2,
                NotiLogDatabase.MIGRATION_2_3,
                NotiLogDatabase.MIGRATION_1_3,
                NotiLogDatabase.MIGRATION_3_4,
            )
            .build()
    }

    private fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)

        return if (stored != null) {
            val encrypted = Base64.decode(stored, Base64.NO_WRAP)
            KeyStoreManager.decrypt(encrypted)
        } else {
            val passphrase = ByteArray(PASSPHRASE_LENGTH).also {
                SecureRandom().nextBytes(it)
            }
            val encrypted = KeyStoreManager.encrypt(passphrase)
            prefs.edit()
                .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .apply()
            passphrase
        }
    }
}


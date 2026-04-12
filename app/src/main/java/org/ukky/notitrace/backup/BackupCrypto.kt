package org.ukky.notitrace.backup

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/**
 * バックアップファイルの暗号化・復号。
 *
 * ユーザーパスワードから PBKDF2 で鍵を導出し、AES-256-GCM で暗号化する。
 * ファイル構造: [salt(16)] [iv(12)] [ciphertext + GCM tag(16)]
 */
object BackupCrypto {

    private const val PBKDF2_ITERATIONS = 210_000
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128

    fun encrypt(plaintext: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv // 12 bytes auto-generated
        val ciphertext = cipher.doFinal(plaintext)

        return salt + iv + ciphertext
    }

    fun decrypt(data: ByteArray, password: String): ByteArray {
        val salt = data.sliceArray(0 until SALT_LENGTH)
        val iv = data.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
        val ciphertext = data.sliceArray(SALT_LENGTH + IV_LENGTH until data.size)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}


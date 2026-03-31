package org.ukky.notilog.util

import org.junit.Assert.*
import org.junit.Test

/**
 * SignatureGenerator の単体テスト（RED → GREEN）
 *
 * 要件:
 * - 同一内容 → 同一ハッシュ
 * - 異なる内容 → 異なるハッシュ
 * - null フィールドを安全に扱える
 * - 空文字列でもクラッシュしない
 */
class SignatureGeneratorTest {

    @Test
    fun `同一内容から同一のsignatureが生成される`() {
        val sig1 = SignatureGenerator.generate(
            packageName = "com.example.app",
            title = "タイトル",
            text = "本文テキスト",
            bigText = "拡張テキスト",
            subText = "サブテキスト"
        )
        val sig2 = SignatureGenerator.generate(
            packageName = "com.example.app",
            title = "タイトル",
            text = "本文テキスト",
            bigText = "拡張テキスト",
            subText = "サブテキスト"
        )
        assertEquals(sig1, sig2)
    }

    @Test
    fun `異なる内容から異なるsignatureが生成される`() {
        val sig1 = SignatureGenerator.generate(
            packageName = "com.example.app",
            title = "タイトルA",
            text = "本文",
            bigText = null,
            subText = null
        )
        val sig2 = SignatureGenerator.generate(
            packageName = "com.example.app",
            title = "タイトルB",
            text = "本文",
            bigText = null,
            subText = null
        )
        assertNotEquals(sig1, sig2)
    }

    @Test
    fun `パッケージ名が異なれば同じテキストでも異なるsignature`() {
        val sig1 = SignatureGenerator.generate("com.app.a", "t", "x", null, null)
        val sig2 = SignatureGenerator.generate("com.app.b", "t", "x", null, null)
        assertNotEquals(sig1, sig2)
    }

    @Test
    fun `全フィールドがnullでもクラッシュしない`() {
        val sig = SignatureGenerator.generate("com.example.app", null, null, null, null)
        assertNotNull(sig)
        assertTrue(sig.isNotEmpty())
    }

    @Test
    fun `signatureはSHA256の64文字hex文字列`() {
        val sig = SignatureGenerator.generate("pkg", "t", "x", null, null)
        assertEquals(64, sig.length)
        assertTrue(sig.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `nullと空文字列で異なるsignatureが生成される`() {
        val sigNull = SignatureGenerator.generate("pkg", null, "text", null, null)
        val sigEmpty = SignatureGenerator.generate("pkg", "", "text", null, null)
        assertNotEquals(sigNull, sigEmpty)
    }
}


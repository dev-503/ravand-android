package ir.ravand.app.data

import android.util.Base64
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * احراز هویت: هش رمز (PBKDF2-HMAC-SHA256) + TOTP مطابق RFC 6238
 * (HMAC-SHA1، ۶ رقم، قدم ۳۰ ثانیه، پنجرهٔ ±۱ قدم).
 *
 * نکته: نسخهٔ دسکتاپ از Argon2id استفاده میکند. Argon2 در کتابخانهٔ استاندارد
 * اندروید موجود نیست، بنابراین اینجا از PBKDF2WithHmacSHA256 با ۱۲۰٬۰۰۰ تکرار
 * و نمک تصادفی ۱۶ بایتی استفاده شده است — هیچ رمزی بهصورت متن ساده ذخیره نمیشود.
 */
object Crypto {

    private const val ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private val rng = SecureRandom()

    // ---------- رمز عبور ----------

    fun hashPassword(plain: String): String {
        val salt = ByteArray(SALT_BYTES).also { rng.nextBytes(it) }
        val hash = pbkdf2(plain.toCharArray(), salt, ITERATIONS, 256)
        return listOf("pbkdf2", ITERATIONS.toString(), b64(salt), b64(hash)).joinToString(separator = "\$")
    }

    fun verifyPassword(plain: String, stored: String): Boolean {
        val parts = stored.split('$')
        if (parts.size == 4 && parts[0] == "pbkdf2") {
            return try {
                val iterations = parts[1].toInt()
                val salt = unb64(parts[2])
                val expected = unb64(parts[3])
                val actual = pbkdf2(plain.toCharArray(), salt, iterations, expected.size * 8)
                MessageDigest.isEqual(expected, actual)
            } catch (_: Exception) {
                false
            }
        }
        // سازگاری با ذخیرهٔ قدیمی متن ساده
        return plain == stored
    }

    private fun pbkdf2(chars: CharArray, salt: ByteArray, iterations: Int, bits: Int): ByteArray {
        val spec = PBEKeySpec(chars, salt, iterations, bits)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun b64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun unb64(s: String): ByteArray =
        Base64.decode(s, Base64.NO_WRAP)

    // ---------- Base32 (RFC 4648) ----------

    private const val B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun base32Encode(bytes: ByteArray): String {
        val sb = StringBuilder()
        var buffer = 0
        var bits = 0
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toInt() and 0xff)
            bits += 8
            while (bits >= 5) {
                sb.append(B32[(buffer ushr (bits - 5)) and 0x1f])
                bits -= 5
                buffer = buffer and ((1 shl bits) - 1)
            }
        }
        if (bits > 0) sb.append(B32[(buffer shl (5 - bits)) and 0x1f])
        return sb.toString()
    }

    fun base32Decode(input: String): ByteArray {
        val clean = input.uppercase(Locale.ROOT).filter {
            it != ' ' && it != '-' && it != '\n' && it != '\r' && it != '='
        }
        val out = mutableListOf<Byte>()
        var buffer = 0
        var bits = 0
        for (ch in clean) {
            val idx = B32.indexOf(ch)
            if (idx < 0) throw IllegalArgumentException("داده‌ی base32 نامعتبر است")
            buffer = (buffer shl 5) or idx
            bits += 5
            if (bits >= 8) {
                out.add(((buffer ushr (bits - 8)) and 0xff).toByte())
                bits -= 8
                buffer = buffer and ((1 shl bits) - 1)
            }
        }
        return out.toByteArray()
    }

    // ---------- TOTP ----------

    const val TOTP_PERIOD = 30L
    const val TOTP_ISSUER = "Ravand"

    fun generateSecret(): String {
        val buf = ByteArray(20).also { rng.nextBytes(it) }
        return base32Encode(buf)
    }

    fun otpauthUri(username: String, secretB32: String): String {
        fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        return "otpauth://totp/${enc(TOTP_ISSUER)}:${enc(username)}" +
            "?secret=$secretB32&issuer=${enc(TOTP_ISSUER)}" +
            "&digits=6&period=30&algorithm=SHA1"
    }

    private fun hotp(secret: ByteArray, counter: Long): Int {
        val msg = ByteArray(8)
        var c = counter
        for (i in 7 downTo 0) {
            msg[i] = (c and 0xffL).toByte()
            c = c ushr 8
        }
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secret, "HmacSHA1"))
        val res = mac.doFinal(msg)
        val offset = res[19].toInt() and 0x0f
        val code = ((res[offset].toInt() and 0x7f) shl 24) or
            ((res[offset + 1].toInt() and 0xff) shl 16) or
            ((res[offset + 2].toInt() and 0xff) shl 8) or
            (res[offset + 3].toInt() and 0xff)
        return code % 1_000_000
    }

    fun totpCode(secretB32: String, unixSecs: Long = System.currentTimeMillis() / 1000): String {
        val bytes = base32Decode(secretB32)
        return hotp(bytes, unixSecs / TOTP_PERIOD).toString().padStart(6, '0')
    }

    fun verifyTotp(
        secretB32: String,
        code: String,
        nowSecs: Long = System.currentTimeMillis() / 1000,
    ): Boolean {
        val clean = cleanDigits(code).trim()
        if (clean.length != 6 || !clean.all { it in '0'..'9' }) return false
        val bytes = try {
            base32Decode(secretB32)
        } catch (_: Exception) {
            return false
        }
        val counter = nowSecs / TOTP_PERIOD
        val target = clean.toInt()
        return listOf(0L, -1L, 1L).any { delta -> hotp(bytes, counter + delta) == target }
    }
}

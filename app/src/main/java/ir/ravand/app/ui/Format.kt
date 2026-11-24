package ir.ravand.app.ui

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

private val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

fun toFaDigits(s: String): String {
    val sb = StringBuilder(s.length)
    for (ch in s) sb.append(if (ch in '0'..'9') faDigits[ch - '0'] else ch)
    return sb.toString()
}

/** قالبnumbers فارسی با جداکنندهٔ هزارها — معادل Intl.NumberFormat('fa-IR'). */
fun fa(value: Number, digits: Int = 1): String {
    val symbols = DecimalFormatSymbols(Locale.US)
    val df = DecimalFormat("#,##0${if (digits > 0) "." + "0".repeat(digits) else ""}", symbols)
    return toFaDigits(df.format(value))
}

private val persianMonths = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)
private val weekDays = listOf("شنبه", "یکشنبه", "دوشنبه", "سهشنبه", "چهارشنبه", "پنجشنبه", "جمعه")

/** تبدیل میلادی به خورشیدی (الگوریتم استاندارد Borkowski). */
private fun toJalali(gy0: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val g_d_m = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    var jy = 979
    var gy = gy0
    if (gy > 1600) gy -= 1600 else { jy = 0; gy -= 621 }
    val gy2 = if (gm > 2) gy + 1 else gy
    var days = 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 - 80 + gd + g_d_m[gm - 1]
    jy += 33 * (days / 12053)
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    jy += (days - 1) / 365
    if (days > 365) days = (days - 1) % 365
    val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
    val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
    return Triple(jy, jm, jd)
}

fun nowFmt(): String {
    val now = LocalDate.now()
    val (_, jm, jd) = toJalali(now.year, now.monthValue, now.dayOfMonth)
    val dayName = weekDays[(now.dayOfWeek.value + 1) % 7]
    return "$dayName، ${fa(jd, 0)} ${persianMonths[jm - 1]}"
}

fun timeFmt(): String {
    val t = LocalTime.now()
    return toFaDigits("%02d:%02d".format(Locale.US, t.hour, t.minute))
}

fun eventKindLabel(kind: String): String = when (kind) {
    "check_in" -> "↱ ورود"
    "check_out" -> "↰ خروج"
    "leave_start" -> "🌴 مرخصی"
    "leave_end" -> "↱ پایان مرخصی"
    "auto_checkout" -> "↰ خروج خودکار"
    else -> kind
}

/** ساخت QR code باینری otpauth URI با ZXing. */
fun qrBitmap(text: String, size: Int = 512): Bitmap? = try {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    bmp
} catch (_: Exception) {
    null
}

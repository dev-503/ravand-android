package ir.ravand.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import ir.ravand.app.R

// نکتهٔ مهم: Color(Int) در Compose قالب ARGB میخواهد (0xAARRGGBB).
// مقادیر ۶رقمی آلفای صفر دارند و کاملاً شفاف میشوند — حتماً با FF شروع شوند.
data class Accent(val primary: Int, val dark: Int, val light: Int, val onPrimary: Int = 0xFFFFFFFF.toInt())

fun accentFor(id: String): Accent = when (id) {
    // نارنجی روشن با متن سفید کنتراست کمی دارد، بنابراین متن تیره استفاده میشود
    "dark" -> Accent(0xFFEF8C4D.toInt(), 0xFFB85C18.toInt(), 0xFFFFF2E9.toInt(), 0xFF2A1B10.toInt())
    "blue" -> Accent(0xFF5179E5.toInt(), 0xFF3B61C7.toInt(), 0xFFECF1FF.toInt())
    "brown" -> Accent(0xFF997857.toInt(), 0xFF76573A.toInt(), 0xFFFFF5EF.toInt())
    "mono" -> Accent(0xFF424849.toInt(), 0xFF272C2D.toInt(), 0xFFEFF0F0.toInt())
    else -> Accent(0xFF21A87A.toInt(), 0xFF11845E.toInt(), 0xFFE8F7F1.toInt()) // mint
}

/** متنها را بهطور محسوس درشتتر میکند (فونت پیشفرض ۴۰۰ وزن متوسط است). */
private fun Typography.withFont(family: FontFamily): Typography {
    fun TextStyle.styled(bold: Boolean): TextStyle = copy(
        fontFamily = family,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
    )

    return copy(
        displayLarge = displayLarge.styled(true),
        displayMedium = displayMedium.styled(true),
        displaySmall = displaySmall.styled(true),
        headlineLarge = headlineLarge.styled(true),
        headlineMedium = headlineMedium.styled(true),
        headlineSmall = headlineSmall.styled(true),
        titleLarge = titleLarge.styled(true),
        titleMedium = titleMedium.styled(true),
        titleSmall = titleSmall.styled(true),
        bodyLarge = bodyLarge.styled(false),
        bodyMedium = bodyMedium.styled(false),
        bodySmall = bodySmall.styled(false),
        labelLarge = labelLarge.styled(true),
        labelMedium = labelMedium.styled(false),
        labelSmall = labelSmall.styled(false),
    )
}

@Composable
fun RavandTheme(themeId: String, fontId: String, content: @Composable () -> Unit) {
    val accent = accentFor(themeId)
    val colors = lightColorScheme(
        primary = Color(accent.primary),
        onPrimary = Color(accent.onPrimary),
        primaryContainer = Color(accent.light),
        onPrimaryContainer = Color(accent.dark),
        secondary = Color(accent.dark),
        onSecondary = Color.White,
        secondaryContainer = Color(accent.light),
        onSecondaryContainer = Color(accent.dark),
        background = Color(0xFFF5F7F6),
        onBackground = Color(0xFF1F2A27),
        surface = Color.White,
        onSurface = Color(0xFF1F2A27),
        surfaceVariant = Color(0xFFF1F4F3),
        onSurfaceVariant = Color(0xFF5D6B67),
        error = Color(0xFFC0392B),
        onError = Color.White,
        outline = Color(0xFFDDE3E1),
    )

    // فونت آراد نسخهٔ Variable آن دارای وزن پیشفرض ۱۰۰ (بسیار نازک) است.
    // بهجای تنظیم محور wght در زمان اجرا (که روی برخی دستگاهها پشتیبانی نمیشود)،
    // دو فایل استاتیک از همان فونت ساخته شد: arad.ttf (wght=600) و arad_bold.ttf (wght=800).
    val fontFamily = if (fontId == "arad") {
        FontFamily(
            Font(R.font.arad),
            Font(R.font.arad_bold, weight = FontWeight.Bold),
        )
    } else {
        FontFamily.SansSerif // وزیرمتن: در صورت وجود فایل فونت، جایگزین کنید
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography().withFont(fontFamily),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            content()
        }
    }
}

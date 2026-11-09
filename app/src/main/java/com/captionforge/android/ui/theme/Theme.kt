package com.captionforge.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.captionforge.android.R

object AppColors {
    val Background = Color(0xFF0D0B14)
    val Surface = Color(0xFF161224)
    val SurfaceElevated = Color(0xFF201B34)
    val SurfaceBorder = Color(0xFF2E264A)

    val PrimaryPurple = Color(0xFF8B5CF6)
    val PrimaryPurpleLight = Color(0xFFA78BFA)
    val PrimaryPurpleDark = Color(0xFF6D28D9)

    val AccentCyan = Color(0xFF06B6D4)
    val AccentGreen = Color(0xFF10B981)
    val AccentAmber = Color(0xFFF59E0B)
    val AccentCoral = Color(0xFFF43F5E)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)

    val TrackColors = listOf(
        Color(0xFF8B5CF6), // Track 1: Purple
        Color(0xFF06B6D4), // Track 2: Cyan
        Color(0xFFF59E0B), // Track 3: Amber
        Color(0xFF10B981), // Track 4: Emerald
        Color(0xFFEC4899)  // Track 5: Pink
    )
}

val InterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.Bold)
)

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn, FontWeight.Normal),
    Font(R.font.vazirmatn, FontWeight.Bold)
)

val NotoSansArabicFontFamily = FontFamily(
    Font(R.font.noto_sans_arabic, FontWeight.Normal),
    Font(R.font.noto_sans_arabic, FontWeight.Bold)
)

val NotoNastaliqUrduFontFamily = FontFamily(
    Font(R.font.noto_nastaliq_urdu, FontWeight.Normal),
    Font(R.font.noto_nastaliq_urdu, FontWeight.Bold)
)

val LalezarFontFamily = FontFamily(
    Font(R.font.lalezar, FontWeight.Normal),
    Font(R.font.lalezar, FontWeight.Bold)
)

val AmiriFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Bold)
)

val BebasNeueFontFamily = FontFamily(
    Font(R.font.bebas_neue, FontWeight.Normal),
    Font(R.font.bebas_neue, FontWeight.Bold)
)

object CustomFontRegistry {
    private val composeFonts = mutableMapOf<String, FontFamily>()
    private val fontFiles = mutableMapOf<String, java.io.File>()

    fun registerFont(name: String, file: java.io.File) {
        if (!file.exists() || file.length() < 256) return
        val key = name.lowercase().trim()
        runCatching {
            composeFonts[key] = FontFamily(Font(file))
            fontFiles[key] = file
        }
    }

    fun getComposeFont(name: String): FontFamily? = composeFonts[name.lowercase().trim()]

    fun getFontFile(name: String): java.io.File? = fontFiles[name.lowercase().trim()]
}

fun resolveSubtitleFontFamily(fontName: String): FontFamily {
    CustomFontRegistry.getComposeFont(fontName)?.let { return it }
    return when (fontName.lowercase().trim()) {
        "vazirmatn" -> VazirmatnFontFamily
        "noto sans arabic", "noto sans" -> NotoSansArabicFontFamily
        "noto nastaliq urdu" -> NotoNastaliqUrduFontFamily
        "lalezar" -> LalezarFontFamily
        "amiri" -> AmiriFontFamily
        "bebas neue", "impact" -> BebasNeueFontFamily
        "monospace" -> FontFamily.Monospace
        "serif" -> FontFamily.Serif
        else -> InterFontFamily
    }
}

fun parseHexColor(hex: String, opacityPercent: Int = 100): Color {
    val clean = hex.trim().removePrefix("#")
    val rgb = clean.toLongOrNull(16) ?: 0xFFFFFFL
    val baseColor = when (clean.length) {
        6 -> Color(0xFF000000L or rgb)
        8 -> Color(rgb)
        else -> Color.White
    }
    val alpha = (opacityPercent.coerceIn(0, 100) / 100f)
    return baseColor.copy(alpha = baseColor.alpha * alpha)
}

fun isRtlScript(text: String): Boolean {
    val rtlRegex = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")
    return rtlRegex.containsMatchIn(text)
}

private val CaptionForgeColorScheme = darkColorScheme(
    primary = AppColors.PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = AppColors.PrimaryPurpleDark,
    onPrimaryContainer = Color.White,
    secondary = AppColors.AccentCyan,
    onSecondary = Color.Black,
    secondaryContainer = AppColors.SurfaceElevated,
    onSecondaryContainer = AppColors.TextPrimary,
    tertiary = AppColors.AccentAmber,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceElevated,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.SurfaceBorder,
    error = AppColors.AccentCoral,
    onError = Color.White
)

private val CaptionForgeTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

@Composable
fun CaptionForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CaptionForgeColorScheme,
        typography = CaptionForgeTypography,
        content = content
    )
}

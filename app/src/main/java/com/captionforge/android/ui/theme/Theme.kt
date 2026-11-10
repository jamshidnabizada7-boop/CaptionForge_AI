package com.captionforge.android.ui.theme

import androidx.annotation.FontRes
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
import java.io.File

import androidx.compose.ui.graphics.Brush

object AppColors {
    // Deep Obsidian-Cosmos Studio Palette
    val Background = Color(0xFF07070D)
    val Surface = Color(0xFF10101B)
    val SurfaceElevated = Color(0xFF181829)
    val SurfaceGlass = Color(0xFF1E1E36)
    val SurfaceBorder = Color(0xFF2B2B4B)

    // Signature "Magic Editor" Electric Indigo & Neon Violet Accents
    val MagicIndigo = Color(0xFF4F46E5)
    val PrimaryPurple = Color(0xFF6366F1)
    val PrimaryPurpleLight = Color(0xFFA855F7)
    val PrimaryPurpleDark = Color(0xFF3B28CC)

    // High-Contrast Studio Accents
    val AccentCyan = Color(0xFF00E5FF)
    val AccentGreen = Color(0xFF10B981)
    val AccentAmber = Color(0xFFFBBF24)
    val AccentOrange = Color(0xFFFF6B00)
    val AccentCoral = Color(0xFFF43F5E)
    val AccentPink = Color(0xFFEC4899)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFFA1A9C4)
    val TextMuted = Color(0xFF64708D)

    val MagicBrandGradient = Brush.linearGradient(
        listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFEC4899))
    )

    val MagicButtonGradient = Brush.horizontalGradient(
        listOf(Color(0xFF4F46E5), Color(0xFF8B5CF6), Color(0xFFA855F7))
    )

    val SolarKineticGradient = Brush.linearGradient(
        listOf(Color(0xFFFF5500), Color(0xFFFF9500), Color(0xFFFFCC00))
    )

    val CyberHologramGradient = Brush.linearGradient(
        listOf(Color(0xFF00E5FF), Color(0xFF3B82F6), Color(0xFF8B5CF6))
    )

    val TrackColors = listOf(
        Color(0xFF6366F1), // Track 1: Electric Indigo
        Color(0xFF00E5FF), // Track 2: Cyber Cyan
        Color(0xFFFF6B00), // Track 3: Solar Orange
        Color(0xFF10B981), // Track 4: Emerald
        Color(0xFFEC4899)  // Track 5: Neon Pink
    )
}

data class FontCatalogItem(
    val name: String,
    @FontRes val fontResId: Int?,
    val category: String,
    val secondaryCategories: List<String> = emptyList(),
    val scripts: List<String>,
    val sampleText: String,
    val supportedWeights: List<Int> = listOf(400, 600, 700, 900),
    val license: String = "SIL Open Font License 1.1",
    val isRtlOptimized: Boolean = false,
    val isCustomUserFont: Boolean = false
) {
    fun matchesCategory(cat: String): Boolean {
        if (cat.equals("All", ignoreCase = true)) return true
        return category.equals(cat, ignoreCase = true) ||
            secondaryCategories.any { it.equals(cat, ignoreCase = true) }
    }

    fun matchesScript(scriptCode: String): Boolean {
        if (scriptCode.equals("all", ignoreCase = true)) return true
        return scripts.any { it.equals(scriptCode, ignoreCase = true) }
    }
}

object FontCatalog {
    val categories = listOf(
        "All",
        "Bold Cinematic",
        "Modern Sans-Serif",
        "Condensed Headlines",
        "Documentary",
        "Minimal Captions",
        "Elegant Persian",
        "Modern Dari/Persian",
        "Pashto Typography",
        "Urdu Nastaliq",
        "Arabic Calligraphy",
        "Neon/Glowing Titles",
        "Metallic Cinematic Titles",
        "Social-Media Captions"
    )

    val scriptFilters = listOf(
        "all" to "All Scripts",
        "fa" to "Persian / Dari (فارسی)",
        "ps" to "Pashto (پښتو)",
        "ur" to "Urdu (اردو)",
        "ar" to "Arabic (العربية)",
        "en" to "English / Latin"
    )

    val builtInFonts: List<FontCatalogItem> = listOf(
        // --- PERSIAN / DARI, PASHTO, URDU & ARABIC RTL FONTS (14 Verified Fonts) ---
        FontCatalogItem(
            name = "Vazirmatn",
            fontResId = R.font.vazirmatn,
            category = "Elegant Persian",
            secondaryCategories = listOf("Modern Dari/Persian", "Social-Media Captions", "Bold Cinematic"),
            scripts = listOf("fa", "ps", "ar", "ur"),
            sampleText = "ویزای تحصیلی فنلاند بدون مدرک زبان",
            supportedWeights = listOf(400, 500, 700, 800, 900),
            isRtlOptimized = true
        ),
        FontCatalogItem(
            name = "Lalezar",
            fontResId = R.font.lalezar,
            category = "Modern Dari/Persian",
            secondaryCategories = listOf("Bold Cinematic", "Social-Media Captions", "Elegant Persian"),
            scripts = listOf("fa", "ar", "ps"),
            sampleText = "بهترین فرصت مهاجرت و کار دانشجویی",
            supportedWeights = listOf(700, 900),
            isRtlOptimized = true
        ),
        FontCatalogItem(
            name = "Noto Sans Arabic",
            fontResId = R.font.noto_sans_arabic,
            category = "Pashto Typography",
            secondaryCategories = listOf("Modern Dari/Persian", "Arabic Calligraphy", "Documentary"),
            scripts = listOf("ps", "fa", "ar", "ur"),
            sampleText = "د مصنوعي ځیرکتیا سره د ویډیو ایډیټ او پښتو",
            supportedWeights = listOf(400, 500, 600, 700, 800, 900),
            isRtlOptimized = true
        ),
        FontCatalogItem(
            name = "Noto Nastaliq Urdu",
            fontResId = R.font.noto_nastaliq_urdu,
            category = "Urdu Nastaliq",
            secondaryCategories = listOf("Elegant Persian", "Documentary"),
            scripts = listOf("ur", "fa"),
            sampleText = "جدید موشن گرافکس اور اردو سب ٹائٹلز",
            supportedWeights = listOf(400, 700),
            isRtlOptimized = true
        ),
        FontCatalogItem(
            name = "Amiri",
            fontResId = R.font.amiri,
            category = "Arabic Calligraphy",
            secondaryCategories = listOf("Elegant Persian", "Documentary"),
            scripts = listOf("ar", "fa", "ps", "ur"),
            sampleText = "تصميم سينمائي احترافي باللغة العربية",
            supportedWeights = listOf(400, 700),
            isRtlOptimized = true
        ),
        FontCatalogItem(
            name = "Cairo",
            fontResId = R.font.cairo,
            category = "Arabic Calligraphy",
            secondaryCategories = listOf("Modern Dari/Persian", "Social-Media Captions", "Bold Cinematic"),
            scripts = listOf("ar", "fa", "ps"),
            sampleText = "صناعة محتوى احترافي وترجمة فورية",
            supportedWeights = listOf(400, 600, 700, 800, 900),
            isRtlOptimized = true
        ),
        FontCatalogItem(
            name = "Tajawal",
            fontResId = R.font.tajawal,
            category = "Modern Dari/Persian",
            secondaryCategories = listOf("Arabic Calligraphy", "Minimal Captions", "Social-Media Captions"),
            scripts = listOf("ar", "fa", "ps"),
            sampleText = "زیرنویس‌های هوشمند و انیمیشن سینمایی",
            supportedWeights = listOf(400, 500, 700, 800),
            isRtlOptimized = true
        ),
        FontCatalogItem(
            name = "IBM Plex Sans Arabic",
            fontResId = R.font.ibm_plex_sans_arabic,
            category = "Modern Dari/Persian",
            secondaryCategories = listOf("Documentary", "Minimal Captions", "Arabic Calligraphy"),
            scripts = listOf("ar", "fa", "ps"),
            sampleText = "کیفیت استودیویی برای ویدیوهای کوتاه و ریلز",
            supportedWeights = listOf(400, 500, 600, 700),
            isRtlOptimized = true
        ),
        FontCatalogItem(
            name = "Markazi Text",
            fontResId = R.font.markazi_text,
            category = "Elegant Persian",
            secondaryCategories = listOf("Documentary", "Pashto Typography"),
            scripts = listOf("fa", "ps", "ar", "ur"),
            sampleText = "روایت مستند با تایپوگرافی اصیل فارسی و دری",
            supportedWeights = listOf(400, 500, 600, 700),
            isRtlOptimized = true
        ),
        FontCatalogItem(
            name = "Reem Kufi",
            fontResId = R.font.reem_kufi,
            category = "Arabic Calligraphy",
            secondaryCategories = listOf("Metallic Cinematic Titles", "Neon/Glowing Titles", "Bold Cinematic"),
            scripts = listOf("ar", "fa"),
            sampleText = "عناوين كوفية هندسية فاخرة",
            supportedWeights = listOf(400, 600, 700),
            isRtlOptimized = true
        ),
        FontCatalogItem(
            name = "Lateef",
            fontResId = R.font.lateef,
            category = "Pashto Typography",
            secondaryCategories = listOf("Urdu Nastaliq", "Elegant Persian"),
            scripts = listOf("ps", "ur", "fa", "ar"),
            sampleText = "په پښتو او دري ژبو کې ښکلي سرلیکونه",
            supportedWeights = listOf(400, 600, 700),
            isRtlOptimized = true
        ),
        FontCatalogItem(
            name = "Mirza",
            fontResId = R.font.mirza,
            category = "Urdu Nastaliq",
            secondaryCategories = listOf("Elegant Persian", "Arabic Calligraphy"),
            scripts = listOf("ur", "fa", "ar", "ps"),
            sampleText = "خوبصورت اردو اور فارسی خطاطی اسٹائل",
            supportedWeights = listOf(400, 600, 700),
            isRtlOptimized = true
        ),
        FontCatalogItem(
            name = "Scheherazade New",
            fontResId = R.font.scheherazade_new,
            category = "Pashto Typography",
            secondaryCategories = listOf("Arabic Calligraphy", "Documentary"),
            scripts = listOf("ps", "ar", "fa", "ur"),
            sampleText = "د پښتو، دري، اردو او عربي بشپړ ملاتړ",
            supportedWeights = listOf(400, 700),
            isRtlOptimized = true
        ),
        FontCatalogItem(
            name = "Noto Naskh Arabic",
            fontResId = R.font.noto_naskh_arabic,
            category = "Arabic Calligraphy",
            secondaryCategories = listOf("Pashto Typography", "Documentary"),
            scripts = listOf("ar", "fa", "ps", "ur"),
            sampleText = "خط نسخ واضح للأفلام الوثائقية والأخبار",
            supportedWeights = listOf(400, 600, 700),
            isRtlOptimized = true
        ),

        // --- ENGLISH / LATIN STUDIO FONTS (12 Verified Fonts) ---
        FontCatalogItem(
            name = "Inter",
            fontResId = R.font.inter,
            category = "Modern Sans-Serif",
            secondaryCategories = listOf("Minimal Captions", "Documentary", "Social-Media Captions"),
            scripts = listOf("en"),
            sampleText = "Clean Studio Subtitle Precision",
            supportedWeights = listOf(400, 500, 600, 700, 800, 900)
        ),
        FontCatalogItem(
            name = "Bebas Neue",
            fontResId = R.font.bebas_neue,
            category = "Bold Cinematic",
            secondaryCategories = listOf("Condensed Headlines", "Metallic Cinematic Titles"),
            scripts = listOf("en"),
            sampleText = "FINLAND STUDY VISA REVEALED",
            supportedWeights = listOf(700, 900)
        ),
        FontCatalogItem(
            name = "Montserrat",
            fontResId = R.font.montserrat,
            category = "Modern Sans-Serif",
            secondaryCategories = listOf("Social-Media Captions", "Bold Cinematic"),
            scripts = listOf("en"),
            sampleText = "VIRAL CREATOR REELS & SHORTS",
            supportedWeights = listOf(400, 600, 700, 800, 900)
        ),
        FontCatalogItem(
            name = "Poppins",
            fontResId = R.font.poppins,
            category = "Social-Media Captions",
            secondaryCategories = listOf("Modern Sans-Serif", "Minimal Captions"),
            scripts = listOf("en"),
            sampleText = "Dynamic Word-by-Word Pop Captions",
            supportedWeights = listOf(400, 600, 700, 800)
        ),
        FontCatalogItem(
            name = "Oswald",
            fontResId = R.font.oswald,
            category = "Condensed Headlines",
            secondaryCategories = listOf("Bold Cinematic", "Documentary"),
            scripts = listOf("en"),
            sampleText = "HIGH-IMPACT DOCUMENTARY HEADLINE",
            supportedWeights = listOf(400, 600, 700)
        ),
        FontCatalogItem(
            name = "Anton",
            fontResId = R.font.anton,
            category = "Bold Cinematic",
            secondaryCategories = listOf("Social-Media Captions", "Condensed Headlines"),
            scripts = listOf("en"),
            sampleText = "UNSTOPPABLE KINETIC PUNCH",
            supportedWeights = listOf(700, 900)
        ),
        FontCatalogItem(
            name = "Archivo Black",
            fontResId = R.font.archivo_black,
            category = "Bold Cinematic",
            secondaryCategories = listOf("Social-Media Captions", "Metallic Cinematic Titles"),
            scripts = listOf("en"),
            sampleText = "HEAVY WEIGHT HERO TITLES",
            supportedWeights = listOf(800, 900)
        ),
        FontCatalogItem(
            name = "League Spartan",
            fontResId = R.font.league_spartan,
            category = "Condensed Headlines",
            secondaryCategories = listOf("Modern Sans-Serif", "Social-Media Captions"),
            scripts = listOf("en"),
            sampleText = "GEOMETRIC CREATOR TYPOGRAPHY",
            supportedWeights = listOf(400, 600, 700, 800, 900)
        ),
        FontCatalogItem(
            name = "Playfair Display",
            fontResId = R.font.playfair_display,
            category = "Documentary",
            secondaryCategories = listOf("Metallic Cinematic Titles"),
            scripts = listOf("en"),
            sampleText = "Luxury Editorial & Film Chronicle",
            supportedWeights = listOf(400, 600, 700, 800)
        ),
        FontCatalogItem(
            name = "Cinzel",
            fontResId = R.font.cinzel,
            category = "Metallic Cinematic Titles",
            secondaryCategories = listOf("Bold Cinematic", "Documentary"),
            scripts = listOf("en"),
            sampleText = "AUSTRALIA • CINEMATIC EPIC",
            supportedWeights = listOf(400, 700, 900)
        ),
        FontCatalogItem(
            name = "Orbitron",
            fontResId = R.font.orbitron,
            category = "Neon/Glowing Titles",
            secondaryCategories = listOf("Metallic Cinematic Titles", "Bold Cinematic"),
            scripts = listOf("en"),
            sampleText = "◈ CYBER HOLOGRAM 2026 ◈",
            supportedWeights = listOf(400, 700, 900)
        ),
        FontCatalogItem(
            name = "JetBrains Mono",
            fontResId = R.font.jetbrains_mono,
            category = "Minimal Captions",
            secondaryCategories = listOf("Neon/Glowing Titles", "Documentary"),
            scripts = listOf("en"),
            sampleText = "[00:01.42] TECH & GLITCH TELEMETRY",
            supportedWeights = listOf(400, 600, 700)
        )
    )

    fun allFonts(customUserFonts: List<String> = emptyList()): List<FontCatalogItem> {
        val userItems = customUserFonts.map { customName ->
            FontCatalogItem(
                name = customName,
                fontResId = null,
                category = "Bold Cinematic",
                secondaryCategories = listOf("Social-Media Captions", "Elegant Persian"),
                scripts = listOf("en", "fa", "ps", "ur", "ar"),
                sampleText = "$customName • Custom Studio Font",
                supportedWeights = listOf(400, 700),
                license = "User Imported Font (.TTF/.OTF)",
                isCustomUserFont = true
            )
        }
        return userItems + builtInFonts
    }

    fun findFontItem(name: String, customUserFonts: List<String> = emptyList()): FontCatalogItem {
        val clean = name.trim()
        return allFonts(customUserFonts).firstOrNull { it.name.equals(clean, ignoreCase = true) }
            ?: builtInFonts.first()
    }
}

private val composeFontFamilyCache = mutableMapOf<Int, FontFamily>()

private fun fontFamilyFromRes(@FontRes resId: Int): FontFamily {
    return composeFontFamilyCache.getOrPut(resId) {
        FontFamily(
            Font(resId, FontWeight.Normal),
            Font(resId, FontWeight.Medium),
            Font(resId, FontWeight.SemiBold),
            Font(resId, FontWeight.Bold),
            Font(resId, FontWeight.ExtraBold),
            Font(resId, FontWeight.Black)
        )
    }
}

val InterFontFamily = fontFamilyFromRes(R.font.inter)
val VazirmatnFontFamily = fontFamilyFromRes(R.font.vazirmatn)
val NotoSansArabicFontFamily = fontFamilyFromRes(R.font.noto_sans_arabic)
val NotoNastaliqUrduFontFamily = fontFamilyFromRes(R.font.noto_nastaliq_urdu)
val LalezarFontFamily = fontFamilyFromRes(R.font.lalezar)
val AmiriFontFamily = fontFamilyFromRes(R.font.amiri)
val BebasNeueFontFamily = fontFamilyFromRes(R.font.bebas_neue)

object CustomFontRegistry {
    private val composeFonts = mutableMapOf<String, FontFamily>()
    private val fontFiles = mutableMapOf<String, File>()

    fun registerFont(name: String, file: File) {
        if (!file.exists() || file.length() < 256) return
        val key = name.lowercase().trim()
        runCatching {
            composeFonts[key] = FontFamily(
                Font(file, FontWeight.Normal),
                Font(file, FontWeight.Bold)
            )
            fontFiles[key] = file
        }
    }

    fun getComposeFont(name: String): FontFamily? = composeFonts[name.lowercase().trim()]

    fun getFontFile(name: String): File? = fontFiles[name.lowercase().trim()]

    fun getRegisteredFontNames(): List<String> = fontFiles.keys.toList()
}

@FontRes
fun resolveBundledFontResId(fontName: String): Int {
    return when (fontName.lowercase().trim()) {
        "vazirmatn" -> R.font.vazirmatn
        "lalezar" -> R.font.lalezar
        "noto sans arabic", "noto sans" -> R.font.noto_sans_arabic
        "noto nastaliq urdu" -> R.font.noto_nastaliq_urdu
        "amiri" -> R.font.amiri
        "cairo" -> R.font.cairo
        "tajawal" -> R.font.tajawal
        "ibm plex sans arabic" -> R.font.ibm_plex_sans_arabic
        "markazi text" -> R.font.markazi_text
        "reem kufi" -> R.font.reem_kufi
        "lateef" -> R.font.lateef
        "mirza" -> R.font.mirza
        "scheherazade new" -> R.font.scheherazade_new
        "noto naskh arabic" -> R.font.noto_naskh_arabic
        "bebas neue", "impact" -> R.font.bebas_neue
        "montserrat" -> R.font.montserrat
        "poppins" -> R.font.poppins
        "oswald" -> R.font.oswald
        "anton" -> R.font.anton
        "archivo black" -> R.font.archivo_black
        "league spartan" -> R.font.league_spartan
        "playfair display", "serif" -> R.font.playfair_display
        "cinzel" -> R.font.cinzel
        "orbitron" -> R.font.orbitron
        "jetbrains mono", "monospace" -> R.font.jetbrains_mono
        else -> R.font.inter
    }
}

fun resolveSubtitleFontFamily(fontName: String): FontFamily {
    CustomFontRegistry.getComposeFont(fontName)?.let { return it }
    val resId = resolveBundledFontResId(fontName)
    return fontFamilyFromRes(resId)
}

fun resolveComposeFontWeight(weight: Int): FontWeight {
    return when {
        weight >= 850 -> FontWeight.Black
        weight >= 750 -> FontWeight.ExtraBold
        weight >= 650 -> FontWeight.Bold
        weight >= 550 -> FontWeight.SemiBold
        weight >= 450 -> FontWeight.Medium
        else -> FontWeight.Normal
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

package com.captionforge.android.model

import kotlinx.serialization.Serializable
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

@Serializable
data class WordTiming(
    val start: Double = 0.0,
    val end: Double = 0.0,
    val text: String = "",
    val customColor: String? = null,
    val scaleBoost: Double = 1.0,
    val animation: String = "None", // None, Pop, Bounce, Glow, Shake, Slide
    val sfx: String = "None",       // None or any SFX preset/category name
    val tier: Int = 0               // 0=Auto, 1=Top White Kicker, 2=Hero Orange Main, 3=Bottom Line
)

@Serializable
data class CaptionCue(
    val id: String = "c_${UUID.randomUUID().toString().take(8)}",
    val start: Double = 0.0,
    val end: Double = 1.5,
    val text: String = "",
    val words: List<WordTiming> = emptyList(),
    val tier1Text: String = "",
    val tier2Text: String = "",
    val tier3Text: String = "",
    val customStyle: SubtitleStyle? = null,
    val entranceSfx: String = "None",
    val exitSfx: String = "None",
    val keyframes: List<OverlayKeyframe> = emptyList(),
    val isDemoSubtitle: Boolean = false,
    val transcriptionSource: String = "manual" // "asr_faster_whisper", "asr_local_engine", "demo_template", "srt_import", "manual"
) {
    val duration: Double get() = max(0.05, end - start)

    /**
     * Resolves the 3-tier hierarchical text structure used by Reference Video 1's
     * Orange Persian Kinetic template (Tier 1: Top white supporting sub-headline,
     * Tier 2: Large bold orange-amber hero headline, Tier 3: Bottom white/gold sub-headline).
     * Applies RTL normalization and BiDi shaping preservation to each tier.
     */
    fun resolveThreeTiers(normalizeRtl: Boolean = true): Triple<String, String, String> {
        val cleanText = text.removePrefix("[DEMO] ").trim()
        val rawTriple = if (tier1Text.isNotBlank() || tier2Text.isNotBlank() || tier3Text.isNotBlank()) {
            val t2 = tier2Text.ifBlank { cleanText }
            Triple(tier1Text, t2, tier3Text)
        } else if (words.any { it.tier in 1..3 }) {
            val t1 = words.filter { it.tier == 1 }.joinToString(" ") { it.text }
            val t2 = words.filter { it.tier == 2 || it.tier == 0 }.joinToString(" ") { it.text }
            val t3 = words.filter { it.tier == 3 }.joinToString(" ") { it.text }
            Triple(t1, t2.ifBlank { cleanText }, t3)
        } else {
            val lines = cleanText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            when {
                lines.size >= 3 -> Triple(lines[0], lines[1], lines.drop(2).joinToString(" "))
                lines.size == 2 -> Triple(lines[0], lines[1], "")
                else -> {
                    val tokens = cleanText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                    when {
                        tokens.size >= 5 -> {
                            val t1 = tokens.take(2).joinToString(" ")
                            val t2 = tokens.drop(2).take(max(1, tokens.size - 4)).joinToString(" ")
                            val t3 = tokens.takeLast(2).joinToString(" ")
                            Triple(t1, t2, t3)
                        }
                        tokens.size == 4 -> Triple(
                            tokens.take(1).joinToString(" "),
                            tokens.drop(1).take(2).joinToString(" "),
                            tokens.takeLast(1).joinToString(" ")
                        )
                        tokens.size == 3 -> Triple(tokens[0], tokens[1], tokens[2])
                        tokens.size == 2 -> Triple(tokens[0], tokens[1], "")
                        else -> Triple("", text.trim(), "")
                    }
                }
            }
        }
        if (!normalizeRtl) return rawTriple
        return Triple(
            RtlTextShaper.normalizePersianText(rawTriple.first),
            RtlTextShaper.normalizePersianText(rawTriple.second),
            RtlTextShaper.normalizePersianText(rawTriple.third)
        )
    }
}

/**
 * Defines distinct layered text effects (stroke, drop-shadow, 3D extrusion, outer bloom,
 * and inner glow) for an individual tier in the 3-tier Persian cinematic captionLayer.
 */
@Serializable
data class CaptionTierStyle(
    val tierIndex: Int = 2, // 1 = Top Sub-Headline, 2 = Hero Center Headline, 3 = Bottom Sub-Headline
    val fontFamily: String = "Lalezar",
    val fontWeight: Int = 900,
    val fontScale: Double = 1.0,
    val fillColor: String = "#FF7A00",
    val secondaryColor: String = "#FFB800",
    val tertiaryColor: String = "#D93800",
    val useGradient: Boolean = true,
    val strokeWidth: Double = 4.5,
    val strokeColor: String = "#090612",
    val dropShadowDepth: Double = 6.0,
    val dropShadowBlur: Double = 10.0,
    val dropShadowColor: String = "#000000",
    val extrusion3D: Double = 5.5,
    val extrusionColor: String = "#3B1104",
    val outerGlowRadius: Double = 7.5,
    val outerGlowColor: String = "#FF7A00",
    val innerGlowRadius: Double = 4.5,
    val innerGlowColor: String = "#FFF3B0",
    val innerGlowAlpha: Double = 0.62
)

/**
 * Handles Persian/Dari/Arabic/Pashto/Urdu Unicode normalization, contextual joining
 * preservation (ZWNJ & Kashida tatweel), and BiDi directional isolation for both
 * Jetpack Compose live preview and Android Canvas / HarfBuzz export rendering.
 */
object RtlTextShaper {
    private val rtlCodePointRegex = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")

    fun containsRtl(text: String): Boolean = rtlCodePointRegex.containsMatchIn(text)

    /**
     * Normalizes Arabic Yeh/Kaf variants to standard Persian/Farsi codepoints (\u06CC, \u06A9)
     * while preserving ZWNJ (\u200C) half-spaces and Kashida (\u0640) decorative extensions
     * (e.g., "نازیــــه") so HarfBuzz ligatures and contextual forms shape without breaks.
     */
    fun normalizePersianText(input: String): String {
        if (input.isEmpty()) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '\u064A', '\u0649' -> sb.append('\u06CC') // Farsi Yeh
                '\u0643' -> sb.append('\u06A9')           // Farsi Keheh
                else -> sb.append(ch)
            }
        }
        return sb.toString().trim()
    }

    /**
     * Wraps an RTL line with Unicode Right-to-Left Isolate / Embedding markers when mixed
     * with Latin tokens, numbers, or punctuation so visual alignment never flips in LTR hosts.
     */
    fun shapeLineForRtlDisplay(input: String, forceRtl: Boolean = true): String {
        val normalized = normalizePersianText(input)
        if (normalized.isEmpty()) return normalized
        val isRtl = forceRtl || containsRtl(normalized)
        if (!isRtl) return normalized
        // Strip existing stray BiDi overrides before wrapping cleanly
        val clean = normalized.replace("\u200F", "").replace("\u202B", "").replace("\u202C", "")
        return "\u200F$clean"
    }

    /**
     * Orders word timings for horizontal rendering while preserving each word's internal
     * cursive glyph joining.
     */
    fun orderWordsForVisualLine(words: List<WordTiming>, isRtlLine: Boolean): List<WordTiming> {
        return if (isRtlLine) words.reversed() else words
    }
}

@Serializable
data class SubtitleStyle(
    val preset: String = "Bold",
    val font: String = "Vazirmatn",
    val fontWeight: Int = 700,
    val fontSize: Double = 26.0,
    val textColor: String = "#FFFFFF",
    val bgColor: String = "#181124",
    val bgEnabled: Boolean = true,
    val bgShape: String = "Rounded", // Rounded, Pill, Sharp, Banner, Glass, None
    val bgOpacity: Int = 82,
    val outline: Double = 2.5,
    val outlineColor: String = "#000000",
    val shadowDepth: Double = 3.0,
    val shadowBlur: Double = 5.0,
    val shadowColor: String = "#000000",
    val extrusion3D: Double = 0.0,
    val xPos: Double = 0.50,
    val yPos: Double = 0.76,
    val scale: Double = 1.0,
    val rotation: Double = 0.0,
    val align: String = "center",
    val textTransform: String = "None", // None, UPPERCASE, lowercase, Title Case
    val animation: String = "Pop",
    val exitAnimation: String = "Fade", // None, Fade, Scale Down, Slide Down, Blur Out
    val early: Double = 0.0,
    val oneLine: Boolean = false,
    val overlay: String = "None",
    val karaoke: Boolean = false,
    val followFace: Boolean = false,
    val opacity: Int = 100,
    val letterSpacing: Double = 0.0,
    val lineSpacing: Double = 1.08,
    val blur: Double = 0.0,
    val glow: Double = 0.0,
    val glowColor: String = "#00F0FF",
    val gradient: Boolean = false,
    val secondaryColor: String = "#FF6A00",
    val tertiaryColor: String = "#FF2E00",
    val gradientAngle: Double = 90.0,
    val threeTierLayout: Boolean = false,
    val tier1Color: String = "#FFFFFF",
    val tier2Color: String = "#FF7A00",
    val tier3Color: String = "#FACC15",
    // Distinct per-component layered text effect parameters for Headline (Tier 2) vs Sub-Headlines (Tiers 1 & 3)
    val headlineFont: String = "Lalezar",
    val subHeadlineFont: String = "Vazirmatn",
    val headlineStrokeWidth: Double = 5.0,
    val headlineStrokeColor: String = "#090612",
    val subHeadlineStrokeWidth: Double = 3.4,
    val subHeadlineStrokeColor: String = "#090612",
    val headlineShadowDepth: Double = 6.5,
    val headlineShadowBlur: Double = 12.0,
    val headlineShadowColor: String = "#000000",
    val subHeadlineShadowDepth: Double = 3.8,
    val subHeadlineShadowBlur: Double = 7.5,
    val subHeadlineShadowColor: String = "#000000",
    val headlineInnerGlow: Double = 4.5,
    val headlineInnerGlowColor: String = "#FFF3B0",
    val subHeadlineInnerGlow: Double = 1.8,
    val subHeadlineInnerGlowColor: String = "#E2E8F0"
) {
    /**
     * Resolves the layered effect configuration (distinct stroke, drop-shadow, 3D extrusion,
     * outer bloom, and inner glow) for a specific tier (1 = Top Sub-Headline, 2 = Hero Headline, 3 = Bottom Sub-Headline).
     */
    fun resolveTierLayerStyle(tierIndex: Int): CaptionTierStyle {
        return when (tierIndex) {
            2 -> CaptionTierStyle(
                tierIndex = 2,
                fontFamily = if (font.equals("Vazirmatn", ignoreCase = true) || font.equals("Lalezar", ignoreCase = true)) {
                    headlineFont.ifBlank { "Lalezar" }
                } else {
                    font
                },
                fontWeight = 900,
                fontScale = 1.98,
                fillColor = tier2Color.ifBlank { textColor },
                secondaryColor = secondaryColor.ifBlank { "#FFB800" },
                tertiaryColor = tertiaryColor.ifBlank { "#D93800" },
                useGradient = true,
                strokeWidth = max(headlineStrokeWidth, outline * 1.15),
                strokeColor = headlineStrokeColor.ifBlank { outlineColor },
                dropShadowDepth = max(headlineShadowDepth, shadowDepth * 1.15),
                dropShadowBlur = max(headlineShadowBlur, shadowBlur * 1.25),
                dropShadowColor = headlineShadowColor.ifBlank { shadowColor },
                extrusion3D = max(5.5, extrusion3D),
                extrusionColor = "#3B1104",
                outerGlowRadius = max(7.0, glow),
                outerGlowColor = glowColor.ifBlank { "#FF7A00" },
                innerGlowRadius = max(2.5, headlineInnerGlow),
                innerGlowColor = headlineInnerGlowColor.ifBlank { "#FFF3B0" },
                innerGlowAlpha = 0.65
            )
            1 -> CaptionTierStyle(
                tierIndex = 1,
                fontFamily = if (font.equals("Lalezar", ignoreCase = true)) {
                    subHeadlineFont.ifBlank { "Vazirmatn" }
                } else {
                    font
                },
                fontWeight = 800,
                fontScale = 0.88,
                fillColor = tier1Color.ifBlank { "#FFFFFF" },
                secondaryColor = secondaryColor,
                tertiaryColor = tertiaryColor,
                useGradient = false,
                strokeWidth = max(subHeadlineStrokeWidth, outline * 0.85),
                strokeColor = subHeadlineStrokeColor.ifBlank { outlineColor },
                dropShadowDepth = max(subHeadlineShadowDepth, shadowDepth * 0.85),
                dropShadowBlur = max(subHeadlineShadowBlur, shadowBlur * 0.90),
                dropShadowColor = subHeadlineShadowColor.ifBlank { shadowColor },
                extrusion3D = max(2.0, extrusion3D * 0.38),
                extrusionColor = "#110C1D",
                outerGlowRadius = 0.0,
                outerGlowColor = glowColor,
                innerGlowRadius = max(1.0, subHeadlineInnerGlow),
                innerGlowColor = subHeadlineInnerGlowColor.ifBlank { "#E2E8F0" },
                innerGlowAlpha = 0.42
            )
            else -> CaptionTierStyle(
                tierIndex = 3,
                fontFamily = if (font.equals("Lalezar", ignoreCase = true)) {
                    subHeadlineFont.ifBlank { "Vazirmatn" }
                } else {
                    font
                },
                fontWeight = 800,
                fontScale = 0.92,
                fillColor = tier3Color.ifBlank { "#FFFFFF" },
                secondaryColor = secondaryColor,
                tertiaryColor = tertiaryColor,
                useGradient = false,
                strokeWidth = max(subHeadlineStrokeWidth, outline * 0.85),
                strokeColor = subHeadlineStrokeColor.ifBlank { outlineColor },
                dropShadowDepth = max(subHeadlineShadowDepth, shadowDepth * 0.85),
                dropShadowBlur = max(subHeadlineShadowBlur, shadowBlur * 0.90),
                dropShadowColor = subHeadlineShadowColor.ifBlank { shadowColor },
                extrusion3D = max(2.0, extrusion3D * 0.38),
                extrusionColor = "#110C1D",
                outerGlowRadius = 0.0,
                outerGlowColor = glowColor,
                innerGlowRadius = max(1.0, subHeadlineInnerGlow),
                innerGlowColor = subHeadlineInnerGlowColor.ifBlank { "#E2E8F0" },
                innerGlowAlpha = 0.42
            )
        }
    }
}

data class StylePresetItem(
    val id: String,
    val name: String,
    val category: String,
    val badgeText: String,
    val previewSample: String,
    val description: String,
    val style: SubtitleStyle,
    val defaultSfx: String = "None"
)

object StylePresetCatalog {
    val presets: List<StylePresetItem> = listOf(
        StylePresetItem(
            id = "orange_persian_kinetic",
            name = "Cinematic Orange Persian",
            category = "3-Tier RTL",
            badgeText = "3-TIER REF #1",
            previewSample = "ویزای تحصیلی • فنلاند • بدون مدرک زبان",
            description = "Three-tier Persian kinetic layout from Reference Video 1: crisp white top sub-headline, massive orange-gold 3D hero headline with dark extrusion, inner glow & optical flare, and white bottom sub-headline.",
            defaultSfx = "Punchy Pop",
            style = SubtitleStyle(
                preset = "Cinematic Orange Persian",
                font = "Vazirmatn",
                fontWeight = 900,
                fontSize = 32.0,
                textColor = "#FF7A00",
                secondaryColor = "#FFB800",
                tertiaryColor = "#D93800",
                gradient = true,
                bgColor = "#140D22",
                bgEnabled = false,
                bgShape = "None",
                bgOpacity = 0,
                outline = 4.5,
                outlineColor = "#090612",
                shadowDepth = 6.0,
                shadowBlur = 10.0,
                shadowColor = "#000000",
                extrusion3D = 5.5,
                xPos = 0.50,
                yPos = 0.72,
                animation = "Pop",
                exitAnimation = "Scale Down",
                karaoke = true,
                glow = 7.5,
                glowColor = "#FF7A00",
                threeTierLayout = true,
                tier1Color = "#FFFFFF",
                tier2Color = "#FF7A00",
                tier3Color = "#FFFFFF",
                headlineFont = "Lalezar",
                subHeadlineFont = "Vazirmatn",
                headlineStrokeWidth = 5.2,
                headlineStrokeColor = "#090612",
                subHeadlineStrokeWidth = 3.5,
                subHeadlineStrokeColor = "#090612",
                headlineShadowDepth = 6.8,
                headlineShadowBlur = 12.0,
                headlineShadowColor = "#000000",
                subHeadlineShadowDepth = 4.0,
                subHeadlineShadowBlur = 7.5,
                subHeadlineShadowColor = "#000000",
                headlineInnerGlow = 4.8,
                headlineInnerGlowColor = "#FFF3B0",
                subHeadlineInnerGlow = 2.0,
                subHeadlineInnerGlowColor = "#E2E8F0"
            )
        ),
        StylePresetItem(
            id = "white_bold_colored_emphasis",
            name = "White Bold + Colored Emphasis",
            category = "Creator & Explainer",
            badgeText = "REF #1 EXPLAINER",
            previewSample = "بهترین فرصت مهاجرت",
            description = "Crisp white heavy Persian/Latin captions with vibrant orange/gold active word emphasis and tactile click SFX.",
            defaultSfx = "Text Click",
            style = SubtitleStyle(
                preset = "White Bold + Colored Emphasis",
                font = "Vazirmatn",
                fontWeight = 800,
                fontSize = 27.0,
                textColor = "#FFFFFF",
                secondaryColor = "#FF7A00",
                gradient = false,
                bgColor = "#161126",
                bgEnabled = true,
                bgShape = "Rounded",
                bgOpacity = 82,
                outline = 3.0,
                outlineColor = "#000000",
                shadowDepth = 3.5,
                shadowBlur = 5.0,
                yPos = 0.76,
                animation = "Pop",
                exitAnimation = "Fade",
                karaoke = true,
                glow = 2.0,
                glowColor = "#FF7A00",
                threeTierLayout = false
            )
        ),
        StylePresetItem(
            id = "bold_documentary",
            name = "Bold White Documentary",
            category = "Documentary",
            badgeText = "DOC STUDIO",
            previewSample = "THE UNTOLD CHRONICLE",
            description = "Condensed high-contrast documentary lower-third with warm amber accent bar and deep drop shadow.",
            defaultSfx = "Deep Whoosh",
            style = SubtitleStyle(
                preset = "Bold White Documentary",
                font = "Oswald",
                fontWeight = 700,
                fontSize = 27.0,
                textColor = "#F8FAFC",
                secondaryColor = "#F59E0B",
                bgColor = "#0F172A",
                bgEnabled = true,
                bgShape = "Sharp",
                bgOpacity = 88,
                outline = 2.5,
                outlineColor = "#000000",
                shadowDepth = 4.0,
                shadowBlur = 6.0,
                yPos = 0.80,
                textTransform = "UPPERCASE",
                animation = "Slide up",
                exitAnimation = "Fade",
                karaoke = true
            )
        ),
        StylePresetItem(
            id = "modern_minimal",
            name = "Minimal Clean",
            category = "Minimal Captions",
            badgeText = "CLEAN",
            previewSample = "Minimal editorial precision",
            description = "Refined sans-serif pill badge with soft translucent backdrop and subtle fade transitions.",
            defaultSfx = "Soft Transition",
            style = SubtitleStyle(
                preset = "Minimal Clean",
                font = "Inter",
                fontWeight = 600,
                fontSize = 22.0,
                textColor = "#F8FAFC",
                secondaryColor = "#38BDF8",
                bgColor = "#0F172A",
                bgEnabled = true,
                bgShape = "Pill",
                bgOpacity = 72,
                outline = 1.0,
                outlineColor = "#0F172A",
                shadowDepth = 2.0,
                shadowBlur = 4.0,
                yPos = 0.82,
                animation = "Fade",
                exitAnimation = "Fade",
                karaoke = false
            )
        ),
        StylePresetItem(
            id = "elegant_nastaliq",
            name = "Elegant Persian/Nastaliq",
            category = "Urdu & Poetic RTL",
            badgeText = "NASTALIQ",
            previewSample = "جدید موشن گرافکس اور اردو",
            description = "Authentic Urdu & poetic Persian Nastaliq calligraphy with warm gold highlights and generous line height.",
            defaultSfx = "Magical Shimmer",
            style = SubtitleStyle(
                preset = "Elegant Persian/Nastaliq",
                font = "Noto Nastaliq Urdu",
                fontWeight = 700,
                fontSize = 26.0,
                textColor = "#FEF08A",
                secondaryColor = "#F59E0B",
                gradient = true,
                bgColor = "#1C142B",
                bgEnabled = true,
                bgShape = "Rounded",
                bgOpacity = 80,
                outline = 2.5,
                outlineColor = "#090514",
                shadowDepth = 3.5,
                shadowBlur = 6.0,
                lineSpacing = 1.35,
                yPos = 0.76,
                animation = "Fade",
                exitAnimation = "Fade",
                karaoke = true,
                glow = 3.0,
                glowColor = "#F59E0B"
            )
        ),
        StylePresetItem(
            id = "neon_glow",
            name = "Neon Glow",
            category = "Cyber & Hologram",
            badgeText = "NEON",
            previewSample = "◈ CYBER PULSE 2026 ◈",
            description = "Electric cyan and magenta neon bloom matched to Reference Video 2 hologram scenes.",
            defaultSfx = "Electric Pulse",
            style = SubtitleStyle(
                preset = "Neon Glow",
                font = "Orbitron",
                fontWeight = 800,
                fontSize = 26.0,
                textColor = "#E0F2FE",
                secondaryColor = "#00F0FF",
                tertiaryColor = "#D946EF",
                gradient = true,
                bgColor = "#090D16",
                bgEnabled = true,
                bgShape = "Rounded",
                bgOpacity = 70,
                outline = 2.5,
                outlineColor = "#0284C7",
                shadowDepth = 0.0,
                shadowBlur = 12.0,
                yPos = 0.76,
                animation = "Bounce",
                exitAnimation = "Blur Out",
                karaoke = true,
                glow = 9.0,
                glowColor = "#00F0FF"
            )
        ),
        StylePresetItem(
            id = "karaoke_highlight",
            name = "Color Highlight Karaoke",
            category = "Social Media",
            badgeText = "KARAOKE",
            previewSample = "Sing & Follow Every Word",
            description = "Synchronized word-by-word fill sweep with emerald/cyan active badge and bouncy word scaling.",
            defaultSfx = "Soft Pop",
            style = SubtitleStyle(
                preset = "Color Highlight Karaoke",
                font = "Poppins",
                fontWeight = 800,
                fontSize = 27.0,
                textColor = "#FFFFFF",
                secondaryColor = "#10B981",
                bgColor = "#131C31",
                bgEnabled = true,
                bgShape = "Pill",
                bgOpacity = 85,
                outline = 3.0,
                outlineColor = "#000000",
                shadowDepth = 3.0,
                yPos = 0.75,
                animation = "Pop",
                exitAnimation = "Scale Down",
                karaoke = true,
                glow = 4.0,
                glowColor = "#10B981"
            )
        ),
        StylePresetItem(
            id = "metallic_titles",
            name = "Metallic Cinematic Title",
            category = "Reference Video 2 Title",
            badgeText = "3D METALLIC",
            previewSample = "AUSTRALIA",
            description = "Chrome-silver 3D extruded cinematic display title with dark bevel outline and specular sheen.",
            defaultSfx = "Metallic Impact",
            style = SubtitleStyle(
                preset = "Metallic Cinematic Title",
                font = "Cinzel",
                fontWeight = 900,
                fontSize = 34.0,
                textColor = "#F8FAFC",
                secondaryColor = "#94A3B8",
                tertiaryColor = "#E2E8F0",
                gradient = true,
                bgColor = "#0F172A",
                bgEnabled = false,
                bgShape = "None",
                outline = 4.0,
                outlineColor = "#1E293B",
                shadowDepth = 6.0,
                shadowBlur = 8.0,
                extrusion3D = 4.5,
                letterSpacing = 2.0,
                yPos = 0.56,
                textTransform = "UPPERCASE",
                animation = "Pop",
                exitAnimation = "Fade",
                karaoke = false,
                glow = 5.0,
                glowColor = "#38BDF8"
            )
        ),
        StylePresetItem(
            id = "cinematic_film",
            name = "Cinematic Film",
            category = "Film & Trailer",
            badgeText = "ANAMORPHIC",
            previewSample = "A FILM BY CAPTIONFORGE",
            description = "Classic widescreen golden-ivory cinema subtitles with crisp black outline and no box clutter.",
            defaultSfx = "Cinematic Boom",
            style = SubtitleStyle(
                preset = "Cinematic Film",
                font = "Playfair Display",
                fontWeight = 700,
                fontSize = 25.0,
                textColor = "#FEF9C3",
                secondaryColor = "#FDE047",
                bgEnabled = false,
                bgShape = "None",
                outline = 3.2,
                outlineColor = "#000000",
                shadowDepth = 4.0,
                shadowBlur = 7.0,
                letterSpacing = 0.6,
                yPos = 0.84,
                animation = "Fade",
                exitAnimation = "Fade",
                karaoke = false
            )
        ),
        StylePresetItem(
            id = "youtube_creator",
            name = "YouTube Creator",
            category = "Creator & Explainer",
            badgeText = "CREATOR",
            previewSample = "WAIT FOR THE PLOT TWIST!",
            description = "Ultra-bold punchy headline style with heavy black stroke, 3D pop shadow, and high-energy word bounce.",
            defaultSfx = "Fast Whoosh",
            style = SubtitleStyle(
                preset = "YouTube Creator",
                font = "Anton",
                fontWeight = 900,
                fontSize = 30.0,
                textColor = "#FACC15",
                secondaryColor = "#FF5500",
                gradient = true,
                bgEnabled = false,
                bgShape = "None",
                outline = 4.5,
                outlineColor = "#000000",
                shadowDepth = 5.0,
                shadowBlur = 4.0,
                extrusion3D = 3.0,
                yPos = 0.74,
                textTransform = "UPPERCASE",
                animation = "Bounce",
                exitAnimation = "Scale Down",
                karaoke = true,
                glow = 3.5,
                glowColor = "#F59E0B"
            )
        ),
        StylePresetItem(
            id = "social_media_reels",
            name = "Modern Social Reels",
            category = "Social Media",
            badgeText = "9:16 REELS",
            previewSample = "همین حالا ویدیو بسازید",
            description = "High-retention vertical Reels/TikTok caption style with purple-pink gradient and crisp word pop.",
            defaultSfx = "Punchy Pop",
            style = SubtitleStyle(
                preset = "Modern Social Reels",
                font = "Lalezar",
                fontWeight = 800,
                fontSize = 29.0,
                textColor = "#FFFFFF",
                secondaryColor = "#EC4899",
                tertiaryColor = "#8B5CF6",
                gradient = true,
                bgColor = "#1F1235",
                bgEnabled = true,
                bgShape = "Rounded",
                bgOpacity = 84,
                outline = 3.5,
                outlineColor = "#090414",
                shadowDepth = 4.0,
                yPos = 0.72,
                animation = "Pop",
                exitAnimation = "Scale Down",
                karaoke = true,
                glow = 5.0,
                glowColor = "#EC4899"
            )
        ),
        StylePresetItem(
            id = "word_by_word_pop",
            name = "Word-by-Word Pop",
            category = "Kinetic",
            badgeText = "WORD POP",
            previewSample = "ONE POWERFUL WORD",
            description = "Centers and scales the active spoken word with explosive elastic pop and punchy sound trigger.",
            defaultSfx = "Punchy Pop",
            style = SubtitleStyle(
                preset = "Word-by-Word Pop",
                font = "Archivo Black",
                fontWeight = 900,
                fontSize = 32.0,
                textColor = "#38BDF8",
                secondaryColor = "#F43F5E",
                gradient = true,
                bgColor = "#0F172A",
                bgEnabled = true,
                bgShape = "Pill",
                bgOpacity = 86,
                outline = 4.0,
                outlineColor = "#000000",
                extrusion3D = 2.5,
                oneLine = true,
                yPos = 0.68,
                animation = "Pop",
                exitAnimation = "Scale Down",
                karaoke = true,
                glow = 6.0,
                glowColor = "#38BDF8"
            )
        ),
        StylePresetItem(
            id = "glitch_and_digital",
            name = "Glitch and Digital",
            category = "Cyber & Hologram",
            badgeText = "GLITCH",
            previewSample = "SYS_OVERRIDE // 0x9F",
            description = "Monospace digital HUD typography with chromatic neon border and digital byte glitch SFX.",
            defaultSfx = "Glitch",
            style = SubtitleStyle(
                preset = "Glitch and Digital",
                font = "JetBrains Mono",
                fontWeight = 700,
                fontSize = 23.0,
                textColor = "#22D3EE",
                secondaryColor = "#F43F5E",
                gradient = true,
                bgColor = "#090D16",
                bgEnabled = true,
                bgShape = "Sharp",
                bgOpacity = 90,
                outline = 2.0,
                outlineColor = "#F43F5E",
                shadowDepth = 3.0,
                shadowColor = "#22D3EE",
                letterSpacing = 1.2,
                yPos = 0.78,
                animation = "Typewriter",
                exitAnimation = "Blur Out",
                karaoke = true,
                glow = 7.0,
                glowColor = "#22D3EE"
            )
        ),
        StylePresetItem(
            id = "luxury_editorial",
            name = "Luxury Editorial",
            category = "Luxury & Brand",
            badgeText = "EDITORIAL",
            previewSample = "Haute Couture & Design",
            description = "High-fashion serif & Arabic Kufi pairing with champagne gold accents and wide tracking.",
            defaultSfx = "Light Sweep",
            style = SubtitleStyle(
                preset = "Luxury Editorial",
                font = "Playfair Display",
                fontWeight = 600,
                fontSize = 25.0,
                textColor = "#FEF3C7",
                secondaryColor = "#D97706",
                gradient = true,
                bgColor = "#18131E",
                bgEnabled = true,
                bgShape = "Banner",
                bgOpacity = 76,
                outline = 1.5,
                outlineColor = "#271F30",
                shadowDepth = 3.0,
                letterSpacing = 1.5,
                yPos = 0.80,
                animation = "Fade",
                exitAnimation = "Fade",
                karaoke = false,
                glow = 3.0,
                glowColor = "#F59E0B"
            )
        ),
        StylePresetItem(
            id = "typewriter_cinema",
            name = "Typewriter",
            category = "Kinetic",
            badgeText = "TYPEWRITER",
            previewSample = "تایپ هوشمند زیرنویس_",
            description = "Crisp mechanical terminal & documentary typewriter reveal with synchronized tactile key clicks.",
            defaultSfx = "Text Click",
            style = SubtitleStyle(
                preset = "Typewriter",
                font = "JetBrains Mono",
                fontWeight = 700,
                fontSize = 24.0,
                textColor = "#38BDF8",
                secondaryColor = "#F8FAFC",
                bgColor = "#0B111E",
                bgEnabled = true,
                bgShape = "Sharp",
                bgOpacity = 88,
                outline = 2.0,
                outlineColor = "#0F172A",
                shadowDepth = 3.0,
                letterSpacing = 0.8,
                yPos = 0.78,
                animation = "Typewriter",
                exitAnimation = "Fade",
                karaoke = true
            )
        ),
        StylePresetItem(
            id = "pop_and_bounce",
            name = "Pop and Bounce",
            category = "Kinetic",
            badgeText = "BOUNCE",
            previewSample = "انرژی و حرکت سریع!",
            description = "Elastic spring pop-and-bounce caption energy with solar yellow & coral highlights and punchy pop SFX.",
            defaultSfx = "Punchy Pop",
            style = SubtitleStyle(
                preset = "Pop and Bounce",
                font = "Archivo Black",
                fontWeight = 900,
                fontSize = 29.0,
                textColor = "#FACC15",
                secondaryColor = "#F43F5E",
                tertiaryColor = "#FB923C",
                gradient = true,
                bgColor = "#1F122B",
                bgEnabled = true,
                bgShape = "Rounded",
                bgOpacity = 84,
                outline = 4.0,
                outlineColor = "#000000",
                shadowDepth = 4.5,
                extrusion3D = 2.5,
                yPos = 0.74,
                animation = "Bounce",
                exitAnimation = "Scale Down",
                karaoke = true,
                glow = 4.5,
                glowColor = "#F43F5E"
            )
        )
    )

    fun findByNameOrId(query: String): StylePresetItem? {
        val clean = query.trim()
        return presets.firstOrNull {
            it.id.equals(clean, ignoreCase = true) ||
                it.name.equals(clean, ignoreCase = true) ||
                (clean.equals("Orange Persian Kinetic", ignoreCase = true) && it.id == "orange_persian_kinetic")
        }
    }
}

@Serializable
data class Track(
    val id: String = "tr_${UUID.randomUUID().toString().take(8)}",
    val name: String = "Original",
    val language: String = "auto",
    val color: String = "#7352ff",
    val visible: Boolean = true,
    val locked: Boolean = false,
    val style: SubtitleStyle = SubtitleStyle(),
    val captions: List<CaptionCue> = emptyList(),
    val isDemoTrack: Boolean = false,
    val transcriptionSource: String = "manual"
)

@Serializable
data class VideoTrim(
    val start: Double = 0.0,
    val end: Double = 10.0
) {
    val duration: Double get() = max(0.1, end - start)
}

@Serializable
data class VideoClipSegment(
    val id: String = "clip_${UUID.randomUUID().toString().take(6)}",
    val label: String = "Clip 1",
    val sourceUri: String = "",
    val startInTimeline: Double = 0.0,
    val endInTimeline: Double = 5.0,
    val sourceTrimStart: Double = 0.0,
    val transition: String = "Cut" // Cut, Fade, Whip, Purple Flash
) {
    val duration: Double get() = max(0.1, endInTimeline - startInTimeline)
}

@Serializable
data class OverlayKeyframe(
    val time: Double = 0.0,
    val xPos: Double = 0.5,
    val yPos: Double = 0.5,
    val scale: Double = 1.0,
    val rotation: Double = 0.0,
    val opacity: Double = 1.0,
    val blur: Double = 0.0,
    val glow: Double = 0.0,
    val easing: String = "easeInOut",
    val color: String = "#FF7A00"
)

@Serializable
data class GraphicOverlay(
    val id: String = "ov_${UUID.randomUUID().toString().take(8)}",
    val name: String = "Overlay",
    val type: String = "template",
    val content: String = "",
    val start: Double = 0.0,
    val end: Double = 3.0,
    val xPos: Double = 0.5,
    val yPos: Double = 0.5,
    val scale: Double = 1.0,
    val opacity: Double = 1.0,
    val rotation: Double = 0.0,
    val blur: Double = 0.0,
    val glow: Double = 0.0,
    val glowColor: String = "#00F0FF",
    val blendMode: String = "normal",
    val color: String = "#FF7A00",
    val animation: String = "Fade",
    val visible: Boolean = true,
    val isBackground: Boolean = false,
    val depthLayer: Int = 2,
    val keyframes: List<OverlayKeyframe> = emptyList()
) {
    val duration: Double get() = max(0.1, end - start)
}

@Serializable
data class AudioVolumeKeyframe(
    val time: Double = 0.0,
    val volume: Double = 1.0
)

@Serializable
data class AudioClip(
    val id: String = "aud_${UUID.randomUUID().toString().take(8)}",
    val name: String = "Audio Clip",
    val category: String = "Soft Pop",
    val source: String = "",
    val start: Double = 0.0,
    val duration: Double = 1.2,
    val trimStart: Double = 0.0,
    val volume: Double = 1.0,
    val fadeIn: Double = 0.05,
    val fadeOut: Double = 0.10,
    val ducking: Boolean = true,
    val loop: Boolean = false,
    val isMusic: Boolean = false,
    val keyframes: List<AudioVolumeKeyframe> = emptyList()
)

@Serializable
data class VideoEffects(
    val brightness: Double = 0.0,
    val contrast: Double = 1.0,
    val saturation: Double = 1.0,
    val sharpness: Double = 0.0,
    val clearVoice: String = "Off",
    val sfx: String = "None",
    val sfxMode: String = "Sentence", // Sentence, Word, Both
    val sfxVolume: Int = 60,
    val musicVolume: Int = 35
)

@Serializable
data class Project(
    val id: String = "proj_${UUID.randomUUID().toString().replace("-", "").take(16)}",
    val name: String = "Untitled Project",
    val source: String = "asset:///sample_video.mp4",
    val duration: Double = 8.0,
    val width: Int = 720,
    val height: Int = 1280,
    val hasAudio: Boolean = true,
    val trim: VideoTrim = VideoTrim(0.0, 8.0),
    val resolution: Int = 720,
    val aspectRatio: String = "9:16",
    val effects: VideoEffects = VideoEffects(),
    val tracks: List<Track> = emptyList(),
    val overlays: List<GraphicOverlay> = emptyList(),
    val audioClips: List<AudioClip> = emptyList(),
    val videoClips: List<VideoClipSegment> = emptyList(),
    val customFonts: List<String> = emptyList(),
    val customStylePresets: List<SubtitleStyle> = emptyList(),
    val favoriteStylePresets: List<String> = emptyList(),
    val favoriteFonts: List<String> = emptyList(),
    val recentFonts: List<String> = emptyList(),
    val favoriteSfx: List<String> = emptyList(),
    val createdAt: String = "2026-11-09T10:00:00Z",
    val updatedAt: String = "2026-11-09T10:00:00Z",
    val isSyncedWithBackend: Boolean = false
)

@Serializable
data class ProjectSummary(
    val id: String = "",
    val name: String = "Untitled",
    val createdAt: String = "",
    val updatedAt: String = "",
    val duration: Double = 0.0,
    val source: String = "",
    val width: Int = 720,
    val height: Int = 1280
)

@Serializable
data class BackendConfig(
    val transcription: String = "offline",
    val translation: String = "offline",
    val render: Boolean = false,
    val faceTracking: Boolean = false,
    val sfxCategories: List<String> = emptyList(),
    val fontsCount: Int = 26
)

@Serializable
data class JobStartResponse(
    val jobId: String = ""
)

@Serializable
data class JobStatusResponse(
    val id: String = "",
    val type: String = "",
    val status: String = "",
    val message: String? = null
)

@Serializable
data class MotionPoint(
    val time: Double = 0.0,
    val x: Double = 0.5,
    val y: Double = 0.5,
    val scale: Double = 1.0
)

@Serializable
data class TrackMotionResponse(
    val trackType: String = "face",
    val start: Double = 0.0,
    val end: Double = 5.0,
    val points: List<MotionPoint> = emptyList()
)

@Serializable
data class TrackMotionRequest(
    val trackType: String = "face",
    val start: Double = 0.0,
    val end: Double = 5.0
)

@Serializable
data class TranscribeRequest(
    val language: String? = null,
    val model: String = "small"
)

@Serializable
data class TranslateRequest(
    val trackId: String,
    val language: String,
    val mode: String = "Simultaneous",
    val provider: String? = null
)

@Serializable
data class ExportRequest(
    val resolution: Int = 720,
    val aspectRatio: String? = null
)

@Serializable
data class ProjectUpdatePayload(
    val name: String,
    val trim: VideoTrim,
    val tracks: List<Track>,
    val overlays: List<GraphicOverlay>,
    val audioClips: List<AudioClip>,
    val customFonts: List<String>,
    val effects: VideoEffects,
    val resolution: Int,
    val aspectRatio: String
)

object SrtParser {
    private val cueRegex = Regex(
        """(\d\d:\d\d:\d\d[,.]\d+|\d\d:\d\d[,.]\d+)\s*-->\s*(\d\d:\d\d:\d\d[,.]\d+|\d\d:\d\d[,.]\d+)[^\n]*\n([^\n]+(?:\n(?!\s*\n|\d+\s*\n\d\d:|\d\d:\d\d:)[^\n]+)*)""",
        RegexOption.MULTILINE
    )

    private fun parseTimeSeconds(stamp: String): Double {
        val parts = stamp.trim().replace(',', '.').split(':')
        var total = 0.0
        var factor = 1.0
        for (i in parts.indices.reversed()) {
            total += (parts[i].toDoubleOrNull() ?: 0.0) * factor
            factor *= 60.0
        }
        return total
    }

    fun parseSrt(rawContent: String): List<CaptionCue> {
        val normalized = rawContent.replace("\r\n", "\n").replace("\r", "\n").replace("WEBVTT", "")
        val result = mutableListOf<CaptionCue>()
        cueRegex.findAll(normalized).forEachIndexed { idx, match ->
            val start = parseTimeSeconds(match.groupValues[1])
            val end = max(start + 0.1, parseTimeSeconds(match.groupValues[2]))
            val cleanText = match.groupValues[3].replace(Regex("<[^>]+>"), "").trim()
            if (cleanText.isNotEmpty()) {
                val words = buildWordTimings(cleanText, start, end)
                result.add(
                    CaptionCue(
                        id = "srt_${idx + 1}",
                        start = start,
                        end = end,
                        text = cleanText,
                        words = words
                    )
                )
            }
        }
        return result
    }

    fun buildWordTimings(
        text: String,
        start: Double,
        end: Double,
        existingWords: List<WordTiming> = emptyList()
    ): List<WordTiming> {
        val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        val step = max(0.05, (end - start) / tokens.size)
        return tokens.mapIndexed { index, word ->
            val wStart = start + index * step
            val wEnd = if (index == tokens.lastIndex) end else (wStart + step)
            val prev = existingWords.getOrNull(index)
            WordTiming(
                start = (wStart * 100.0).roundToInt() / 100.0,
                end = (wEnd * 100.0).roundToInt() / 100.0,
                text = word,
                customColor = prev?.customColor,
                scaleBoost = prev?.scaleBoost ?: 1.0,
                animation = prev?.animation ?: "None",
                sfx = prev?.sfx ?: "None",
                tier = prev?.tier ?: 0
            )
        }
    }

    fun exportSrt(captions: List<CaptionCue>): String {
        val sb = StringBuilder()
        captions.sortedBy { it.start }.forEachIndexed { index, cue ->
            sb.append(index + 1).append('\n')
            sb.append(formatTimestamp(cue.start)).append(" --> ").append(formatTimestamp(cue.end)).append('\n')
            sb.append(cue.text).append("\n\n")
        }
        return sb.toString()
    }

    private fun formatTimestamp(seconds: Double): String {
        val s = max(0.0, seconds)
        val hh = (s / 3600).toInt()
        val mm = ((s % 3600) / 60).toInt()
        val ss = (s % 60).toInt()
        val ms = ((s - s.toInt()) * 1000).roundToInt().coerceIn(0, 999)
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hh, mm, ss, ms)
    }
}

object SampleProjectsFactory {
    fun createVideo1ShowcaseProject(): Project {
        val duration = 12.0
        val flagshipStyle = StylePresetCatalog.findByNameOrId("orange_persian_kinetic")!!.style
        return Project(
            id = "sample_project_video1",
            name = "Video 1 • Persian Orange Kinetic",
            source = "asset:///sample_video.mp4",
            duration = duration,
            width = 720,
            height = 1280,
            hasAudio = true,
            trim = VideoTrim(0.0, duration),
            resolution = 1080,
            aspectRatio = "9:16",
            effects = VideoEffects(
                brightness = 0.02,
                contrast = 1.06,
                saturation = 1.12,
                clearVoice = "Balanced",
                sfx = "Punchy Pop",
                sfxMode = "Both",
                sfxVolume = 75
            ),
            tracks = listOf(
                Track(
                    id = "tr_fa_main",
                    name = "[DEMO] Persian 3-Tier Kinetic (FA)",
                    language = "fa",
                    color = "#FF7A00",
                    visible = true,
                    style = flagshipStyle,
                    isDemoTrack = true,
                    transcriptionSource = "demo_showcase",
                    captions = listOf(
                        CaptionCue(
                            id = "v1_c1",
                            start = 0.25,
                            end = 3.10,
                            text = "ویزای تحصیلی فنلاند بدون مدرک زبان",
                            tier1Text = "ویزای تحصیلی",
                            tier2Text = "فنلاند",
                            tier3Text = "بدون مدرک زبان",
                            entranceSfx = "Fast Whoosh",
                            isDemoSubtitle = true,
                            transcriptionSource = "demo_showcase",
                            words = listOf(
                                WordTiming(0.25, 0.70, "ویزای", customColor = "#FFFFFF", scaleBoost = 1.0, animation = "Slide", tier = 1),
                                WordTiming(0.70, 1.15, "تحصیلی", customColor = "#FFFFFF", scaleBoost = 1.0, animation = "Slide", tier = 1),
                                WordTiming(1.15, 2.05, "فنلاند", customColor = "#FF7A00", scaleBoost = 1.35, animation = "Pop", sfx = "Punchy Pop", tier = 2),
                                WordTiming(2.05, 2.40, "بدون", customColor = "#FFFFFF", scaleBoost = 1.08, animation = "Bounce", tier = 3),
                                WordTiming(2.40, 2.75, "مدرک", customColor = "#FFFFFF", scaleBoost = 1.0, animation = "Pop", tier = 3),
                                WordTiming(2.75, 3.10, "زبان", customColor = "#FFB800", scaleBoost = 1.18, animation = "Glow", sfx = "Text Click", tier = 3)
                            )
                        ),
                        CaptionCue(
                            id = "v1_c2",
                            start = 3.25,
                            end = 6.30,
                            text = "ویزه خانم نازیــــه است",
                            tier1Text = "ویزه خانم",
                            tier2Text = "نازیــــه",
                            tier3Text = "است",
                            entranceSfx = "Cinematic Swoosh",
                            isDemoSubtitle = true,
                            transcriptionSource = "demo_showcase",
                            words = listOf(
                                WordTiming(3.25, 3.75, "ویزه", customColor = "#FFFFFF", scaleBoost = 1.0, animation = "Slide", tier = 1),
                                WordTiming(3.75, 4.25, "خانم", customColor = "#FFFFFF", scaleBoost = 1.05, animation = "Slide", tier = 1),
                                WordTiming(4.25, 5.55, "نازیــــه", customColor = "#FF7A00", scaleBoost = 1.40, animation = "Pop", sfx = "Heavy Impact", tier = 2),
                                WordTiming(5.55, 6.30, "است", customColor = "#FFFFFF", scaleBoost = 1.10, animation = "Bounce", tier = 3)
                            )
                        ),
                        CaptionCue(
                            id = "v1_c3",
                            start = 6.45,
                            end = 9.30,
                            text = "بهترین فرصت مهاجرت و کار دانشجویی",
                            tier1Text = "بهترین فرصت",
                            tier2Text = "مهاجرت",
                            tier3Text = "و کار دانشجویی",
                            entranceSfx = "Transition Hit",
                            isDemoSubtitle = true,
                            transcriptionSource = "demo_showcase",
                            words = listOf(
                                WordTiming(6.45, 6.90, "بهترین", customColor = "#FFFFFF", scaleBoost = 1.0, animation = "Slide", tier = 1),
                                WordTiming(6.90, 7.35, "فرصت", customColor = "#FFFFFF", scaleBoost = 1.0, animation = "Slide", tier = 1),
                                WordTiming(7.35, 8.35, "مهاجرت", customColor = "#FF7A00", scaleBoost = 1.36, animation = "Pop", sfx = "Bass Drop", tier = 2),
                                WordTiming(8.35, 8.60, "و", customColor = "#FFFFFF", scaleBoost = 1.0, tier = 3),
                                WordTiming(8.60, 8.95, "کار", customColor = "#FFB800", scaleBoost = 1.14, animation = "Bounce", tier = 3),
                                WordTiming(8.95, 9.30, "دانشجویی", customColor = "#FFFFFF", scaleBoost = 1.08, animation = "Pop", tier = 3)
                            )
                        ),
                        CaptionCue(
                            id = "v1_c4",
                            start = 9.45,
                            end = 11.85,
                            text = "در کامنت فنلاند بنویسید",
                            tier1Text = "در کامنت",
                            tier2Text = "فنلاند",
                            tier3Text = "بنویسید",
                            entranceSfx = "Fast Whoosh",
                            isDemoSubtitle = true,
                            transcriptionSource = "demo_showcase",
                            words = listOf(
                                WordTiming(9.45, 9.85, "در", customColor = "#FFFFFF", tier = 1),
                                WordTiming(9.85, 10.30, "کامنت", customColor = "#FFFFFF", tier = 1),
                                WordTiming(10.30, 11.20, "فنلاند", customColor = "#FF7A00", scaleBoost = 1.38, animation = "Pop", sfx = "Punchy Pop", tier = 2),
                                WordTiming(11.20, 11.85, "بنویسید", customColor = "#FFFFFF", scaleBoost = 1.12, animation = "Glow", sfx = "Magical Shimmer", tier = 3)
                            )
                        )
                    )
                ),
                Track(
                    id = "tr_en_sub",
                    name = "[DEMO] English Bilingual (EN)",
                    language = "en",
                    color = "#06B6D4",
                    visible = false,
                    isDemoTrack = true,
                    transcriptionSource = "demo_showcase",
                    style = SubtitleStyle(
                        preset = "Modern Minimal",
                        font = "Inter",
                        fontWeight = 700,
                        fontSize = 18.0,
                        textColor = "#FFFFFF",
                        bgColor = "#181124",
                        bgEnabled = false,
                        bgShape = "None",
                        bgOpacity = 0,
                        outline = 2.5,
                        yPos = 0.91,
                        animation = "Fade",
                        karaoke = false
                    ),
                    captions = listOf(
                        CaptionCue(
                            id = "v1_en1",
                            start = 0.25,
                            end = 3.10,
                            text = "Finland Study Visa • No Language Certificate",
                            isDemoSubtitle = true,
                            transcriptionSource = "demo_showcase",
                            words = SrtParser.buildWordTimings("Finland Study Visa • No Language Certificate", 0.25, 3.10)
                        ),
                        CaptionCue(
                            id = "v1_en2",
                            start = 3.25,
                            end = 6.30,
                            text = "This is Ms. Nazia's approved visa",
                            isDemoSubtitle = true,
                            transcriptionSource = "demo_showcase",
                            words = SrtParser.buildWordTimings("This is Ms. Nazia's approved visa", 3.25, 6.30)
                        ),
                        CaptionCue(
                            id = "v1_en3",
                            start = 6.45,
                            end = 9.30,
                            text = "Best opportunity for migration & student work",
                            isDemoSubtitle = true,
                            transcriptionSource = "demo_showcase",
                            words = SrtParser.buildWordTimings("Best opportunity for migration & student work", 6.45, 9.30)
                        ),
                        CaptionCue(
                            id = "v1_en4",
                            start = 9.45,
                            end = 11.85,
                            text = "Comment FINLAND below right now",
                            isDemoSubtitle = true,
                            transcriptionSource = "demo_showcase",
                            words = SrtParser.buildWordTimings("Comment FINLAND below right now", 9.45, 11.85)
                        )
                    )
                )
            ),
            overlays = listOf(
                GraphicOverlay(
                    id = "ov_v1_flare_1",
                    name = "Optical Purple Lens Flare #1",
                    type = "light_leak",
                    content = "✦ OPTICAL FLARE ✦",
                    color = "#D946EF",
                    start = 1.10,
                    end = 2.35,
                    xPos = 0.50,
                    yPos = 0.72,
                    opacity = 0.90,
                    animation = "Fade"
                ),
                GraphicOverlay(
                    id = "ov_v1_flare_2",
                    name = "Optical Purple Lens Flare #2",
                    type = "light_leak",
                    content = "✦ OPTICAL FLARE ✦",
                    color = "#A855F7",
                    start = 4.20,
                    end = 5.65,
                    xPos = 0.50,
                    yPos = 0.72,
                    opacity = 0.92,
                    animation = "Fade"
                ),
                GraphicOverlay(
                    id = "ov_v1_flare_3",
                    name = "Optical Purple Lens Flare #3",
                    type = "light_leak",
                    content = "✦ OPTICAL FLARE ✦",
                    color = "#D946EF",
                    start = 10.25,
                    end = 11.45,
                    xPos = 0.50,
                    yPos = 0.72,
                    opacity = 0.88,
                    animation = "Fade"
                )
            ),
            audioClips = listOf(
                AudioClip(
                    id = "aud_v1_whoosh_1",
                    name = "Fast Whoosh Intro",
                    category = "Fast Whoosh",
                    source = "sfx/whoosh_fast_air.wav",
                    start = 0.25,
                    duration = 0.42,
                    volume = 0.90,
                    ducking = true
                ),
                AudioClip(
                    id = "aud_v1_pop_1",
                    name = "Punchy Pop Hero Hit",
                    category = "Punchy Pop",
                    source = "sfx/pop_crisp.wav",
                    start = 1.15,
                    duration = 0.35,
                    volume = 0.95,
                    ducking = true
                ),
                AudioClip(
                    id = "aud_v1_whoosh_2",
                    name = "Cinematic Whip Cut",
                    category = "Cinematic Swoosh",
                    source = "sfx/whoosh_cinematic_whip.wav",
                    start = 3.25,
                    duration = 0.50,
                    volume = 0.90,
                    ducking = true
                ),
                AudioClip(
                    id = "aud_v1_impact_2",
                    name = "Heavy Impact Accent",
                    category = "Heavy Impact",
                    source = "sfx/impact_heavy_slam.wav",
                    start = 4.25,
                    duration = 0.65,
                    volume = 0.88,
                    ducking = true
                ),
                AudioClip(
                    id = "aud_v1_bass_3",
                    name = "808 Sub Bass Drop",
                    category = "Bass Drop",
                    source = "sfx/bass_808_drop.wav",
                    start = 7.35,
                    duration = 0.85,
                    volume = 0.85,
                    ducking = true
                ),
                AudioClip(
                    id = "aud_v1_pop_4",
                    name = "Final Hero Pop",
                    category = "Punchy Pop",
                    source = "sfx/pop_bubble_snap.wav",
                    start = 10.30,
                    duration = 0.38,
                    volume = 0.94,
                    ducking = true
                )
            ),
            videoClips = listOf(
                VideoClipSegment(
                    id = "vc_1",
                    label = "Intro Hook",
                    sourceUri = "asset:///sample_video.mp4",
                    startInTimeline = 0.0,
                    endInTimeline = 3.20,
                    sourceTrimStart = 0.0,
                    transition = "Purple Flash"
                ),
                VideoClipSegment(
                    id = "vc_2",
                    label = "Visa Reveal",
                    sourceUri = "asset:///sample_video.mp4",
                    startInTimeline = 3.20,
                    endInTimeline = 6.40,
                    sourceTrimStart = 3.20,
                    transition = "Purple Flash"
                ),
                VideoClipSegment(
                    id = "vc_3",
                    label = "Call To Action",
                    sourceUri = "asset:///sample_video.mp4",
                    startInTimeline = 6.40,
                    endInTimeline = 12.0,
                    sourceTrimStart = 6.40,
                    transition = "Cut"
                )
            )
        )
    }

    fun createVideo2VfxShowcaseProject(): Project {
        val duration = 5.0
        val neonPreset = StylePresetCatalog.findByNameOrId("neon_glow")!!.style.copy(
            font = "Noto Sans Arabic",
            fontSize = 25.0,
            yPos = 0.78
        )
        return Project(
            id = "sample_project_video2",
            name = "Video 2 • Hologram, Flag & Australia",
            source = "asset:///sample_video.mp4",
            duration = duration,
            width = 720,
            height = 1280,
            hasAudio = true,
            trim = VideoTrim(0.0, duration),
            resolution = 1080,
            aspectRatio = "9:16",
            effects = VideoEffects(
                brightness = 0.0,
                contrast = 1.12,
                saturation = 1.2,
                clearVoice = "Strong",
                sfx = "Magical Shimmer",
                sfxMode = "Both",
                sfxVolume = 60
            ),
            tracks = listOf(
                Track(
                    id = "tr_v2_multi",
                    name = "Multilingual RTL (FA / PS / UR)",
                    language = "fa",
                    color = "#8B5CF6",
                    visible = true,
                    style = neonPreset,
                    captions = listOf(
                        CaptionCue(
                            id = "v2_c1",
                            start = 0.1,
                            end = 1.8,
                            text = "برج‌های دوقلو و معماری درخشان",
                            entranceSfx = "Magical Shimmer",
                            words = listOf(
                                WordTiming(0.10, 0.55, "برج‌های", customColor = "#38BDF8", scaleBoost = 1.2, animation = "Glow", sfx = "Sparkle"),
                                WordTiming(0.55, 0.95, "دوقلو", customColor = "#FACC15", scaleBoost = 1.25, animation = "Pop"),
                                WordTiming(0.95, 1.15, "و", customColor = "#FFFFFF"),
                                WordTiming(1.15, 1.50, "معماری", customColor = "#FFFFFF", animation = "Bounce"),
                                WordTiming(1.50, 1.80, "درخشان", customColor = "#00F0FF", scaleBoost = 1.2, animation = "Glow")
                            )
                        ),
                        CaptionCue(
                            id = "v2_c2",
                            start = 1.9,
                            end = 3.4,
                            text = "د مصنوعي ځیرکتیا سره د ویډیو ایډیټ",
                            entranceSfx = "Riser",
                            words = SrtParser.buildWordTimings("د مصنوعي ځیرکتیا سره د ویډیو ایډیټ", 1.9, 3.4)
                        ),
                        CaptionCue(
                            id = "v2_c3",
                            start = 3.5,
                            end = 4.9,
                            text = "جدید موشن گرافکس اور اردو سب ٹائٹلز",
                            customStyle = StylePresetCatalog.findByNameOrId("elegant_nastaliq")?.style,
                            entranceSfx = "Metallic Impact",
                            words = SrtParser.buildWordTimings("جدید موشن گرافکس اور اردو سب ٹائٹلز", 3.5, 4.9)
                        )
                    )
                )
            ),
            overlays = listOf(
                GraphicOverlay(
                    id = "ov_v2_bg_words",
                    name = "Soft Persian Background Words",
                    type = "soft_bg_text",
                    content = "برج‌های دوگانه بلندترین",
                    color = "#94A3B8",
                    start = 0.0,
                    end = 3.2,
                    xPos = 0.50,
                    yPos = 0.25,
                    scale = 1.0,
                    opacity = 0.45,
                    blur = 1.5,
                    isBackground = true,
                    depthLayer = 0
                ),
                GraphicOverlay(
                    id = "ov_v2_holo",
                    name = "Hand Glow Hologram",
                    type = "glow_hologram",
                    content = "◈ TWIN TOWERS HOLOGRAM ◈",
                    color = "#38BDF8",
                    start = 0.2,
                    end = 2.4,
                    xPos = 0.58,
                    yPos = 0.42,
                    scale = 1.1,
                    animation = "Pulse",
                    glow = 8.0,
                    keyframes = listOf(
                        OverlayKeyframe(time = 0.0, xPos = 0.54, yPos = 0.46, scale = 0.9, easing = "easeInOut"),
                        OverlayKeyframe(time = 1.1, xPos = 0.62, yPos = 0.38, scale = 1.15, easing = "easeInOut"),
                        OverlayKeyframe(time = 2.2, xPos = 0.56, yPos = 0.41, scale = 1.05, easing = "easeOut")
                    )
                ),
                GraphicOverlay(
                    id = "ov_v2_flag",
                    name = "Waving Flag Behind Subject",
                    type = "flag_graphic",
                    content = "⚑ MALAYSIA FLAG ⚑",
                    color = "#F59E0B",
                    start = 1.5,
                    end = 3.6,
                    xPos = 0.36,
                    yPos = 0.32,
                    scale = 1.05,
                    isBackground = true,
                    depthLayer = 0
                ),
                GraphicOverlay(
                    id = "ov_v2_aus",
                    name = "Metallic Australia Reveal",
                    type = "metallic_banner",
                    content = "AUSTRALIA",
                    color = "#E2E8F0",
                    start = 2.8,
                    end = 4.9,
                    xPos = 0.50,
                    yPos = 0.54,
                    scale = 1.0,
                    animation = "Pop"
                ),
                GraphicOverlay(
                    id = "ov_v2_end",
                    name = "End Studio Accent",
                    type = "end_accent",
                    content = "CaptionForge Studio",
                    color = "#EF4444",
                    start = 3.6,
                    end = 4.9,
                    xPos = 0.50,
                    yPos = 0.90,
                    scale = 1.0
                )
            ),
            audioClips = listOf(
                AudioClip(
                    id = "aud_v2_shimmer",
                    name = "Celestial Hologram Shimmer",
                    category = "Magical Shimmer",
                    source = "sfx/shimmer_celestial.wav",
                    start = 0.2,
                    duration = 0.8,
                    volume = 0.8,
                    ducking = true
                ),
                AudioClip(
                    id = "aud_v2_riser",
                    name = "Tension Riser Build",
                    category = "Riser",
                    source = "sfx/riser_tension.wav",
                    start = 2.2,
                    duration = 0.7,
                    volume = 0.85,
                    ducking = true
                ),
                AudioClip(
                    id = "aud_v2_glitch",
                    name = "Digital Byte Glitch",
                    category = "Glitch",
                    source = "sfx/glitch_digital_byte.wav",
                    start = 2.8,
                    duration = 0.5,
                    volume = 0.9,
                    ducking = true
                )
            ),
            videoClips = listOf(
                VideoClipSegment(
                    id = "vc_v2_1",
                    label = "Hologram Scene",
                    sourceUri = "asset:///sample_video.mp4",
                    startInTimeline = 0.0,
                    endInTimeline = 2.8,
                    sourceTrimStart = 0.0,
                    transition = "Whip"
                ),
                VideoClipSegment(
                    id = "vc_v2_2",
                    label = "Australia Reveal",
                    sourceUri = "asset:///sample_video.mp4",
                    startInTimeline = 2.8,
                    endInTimeline = 5.0,
                    sourceTrimStart = 2.8,
                    transition = "Cut"
                )
            )
        )
    }
}

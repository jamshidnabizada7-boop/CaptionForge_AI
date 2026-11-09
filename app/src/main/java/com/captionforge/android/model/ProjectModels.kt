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
    val text: String = ""
)

@Serializable
data class CaptionCue(
    val id: String = "c_${UUID.randomUUID().toString().take(8)}",
    val start: Double = 0.0,
    val end: Double = 1.5,
    val text: String = "",
    val words: List<WordTiming> = emptyList()
) {
    val duration: Double get() = max(0.05, end - start)
}

@Serializable
data class SubtitleStyle(
    val preset: String = "Bold",
    val font: String = "Noto Sans Arabic",
    val fontSize: Double = 25.0,
    val textColor: String = "#FFFFFF",
    val bgColor: String = "#181124",
    val bgEnabled: Boolean = true,
    val outline: Double = 2.0,
    val yPos: Double = 0.76,
    val align: String = "center",
    val animation: String = "Pop",
    val early: Double = 0.0,
    val oneLine: Boolean = false,
    val overlay: String = "None",
    val karaoke: Boolean = false,
    val followFace: Boolean = false,
    val opacity: Int = 100,
    val letterSpacing: Double = 0.0,
    val lineSpacing: Double = 1.0,
    val glow: Double = 0.0,
    val glowColor: String = "#00F0FF",
    val gradient: Boolean = false,
    val secondaryColor: String = "#FF6A00"
)

@Serializable
data class Track(
    val id: String = "tr_${UUID.randomUUID().toString().take(8)}",
    val name: String = "Original",
    val language: String = "auto",
    val color: String = "#7352ff",
    val visible: Boolean = true,
    val locked: Boolean = false,
    val style: SubtitleStyle = SubtitleStyle(),
    val captions: List<CaptionCue> = emptyList()
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
data class AudioClip(
    val id: String = "aud_${UUID.randomUUID().toString().take(8)}",
    val name: String = "Audio Clip",
    val category: String = "sfx",
    val source: String = "",
    val start: Double = 0.0,
    val duration: Double = 1.2,
    val trimStart: Double = 0.0,
    val volume: Double = 1.0,
    val fadeIn: Double = 0.0,
    val fadeOut: Double = 0.0,
    val ducking: Boolean = true
)

@Serializable
data class VideoEffects(
    val brightness: Double = 0.0,
    val contrast: Double = 1.0,
    val saturation: Double = 1.0,
    val sharpness: Double = 0.0,
    val clearVoice: String = "Off",
    val sfx: String = "None",
    val sfxMode: String = "Sentence",
    val sfxVolume: Int = 40
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
    val fontsCount: Int = 10
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

    fun buildWordTimings(text: String, start: Double, end: Double): List<WordTiming> {
        val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        val step = max(0.05, (end - start) / tokens.size)
        return tokens.mapIndexed { index, word ->
            val wStart = start + index * step
            val wEnd = if (index == tokens.lastIndex) end else (wStart + step)
            WordTiming(
                start = (wStart * 100.0).roundToInt() / 100.0,
                end = (wEnd * 100.0).roundToInt() / 100.0,
                text = word
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
        val duration = 5.0
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
                contrast = 1.08,
                saturation = 1.15,
                clearVoice = "Balanced",
                sfx = "Pop",
                sfxMode = "Sentence",
                sfxVolume = 60
            ),
            tracks = listOf(
                Track(
                    id = "tr_fa_main",
                    name = "Persian Hero (FA)",
                    language = "fa",
                    color = "#8B5CF6",
                    visible = true,
                    style = SubtitleStyle(
                        preset = "Orange Headline",
                        font = "Vazirmatn",
                        fontSize = 28.0,
                        textColor = "#FF7A00",
                        bgColor = "#140E24",
                        bgEnabled = true,
                        outline = 3.0,
                        yPos = 0.72,
                        animation = "Pop",
                        karaoke = true,
                        glow = 4.0,
                        glowColor = "#FF7A00"
                    ),
                    captions = listOf(
                        CaptionCue(
                            id = "v1_c1",
                            start = 0.1,
                            end = 1.7,
                            text = "ویزای تحصیلی فنلاند بدون مدرک زبان",
                            words = SrtParser.buildWordTimings("ویزای تحصیلی فنلاند بدون مدرک زبان", 0.1, 1.7)
                        ),
                        CaptionCue(
                            id = "v1_c2",
                            start = 1.8,
                            end = 3.4,
                            text = "بهترین فرصت مهاجرت و کار دانشجویی",
                            words = SrtParser.buildWordTimings("بهترین فرصت مهاجرت و کار دانشجویی", 1.8, 3.4)
                        ),
                        CaptionCue(
                            id = "v1_c3",
                            start = 3.5,
                            end = 4.9,
                            text = "همین حالا با کپشن‌فورج ویدیو بسازید",
                            words = SrtParser.buildWordTimings("همین حالا با کپشن‌فورج ویدیو بسازید", 3.5, 4.9)
                        )
                    )
                ),
                Track(
                    id = "tr_en_sub",
                    name = "English Bilingual (EN)",
                    language = "en",
                    color = "#06B6D4",
                    visible = true,
                    style = SubtitleStyle(
                        preset = "Bold",
                        font = "Inter",
                        fontSize = 20.0,
                        textColor = "#FFFFFF",
                        bgColor = "#181124",
                        bgEnabled = true,
                        outline = 2.0,
                        yPos = 0.84,
                        animation = "Fade",
                        karaoke = false
                    ),
                    captions = listOf(
                        CaptionCue(
                            id = "v1_en1",
                            start = 0.1,
                            end = 1.7,
                            text = "Finland Study Visa Options Explained",
                            words = SrtParser.buildWordTimings("Finland Study Visa Options Explained", 0.1, 1.7)
                        ),
                        CaptionCue(
                            id = "v1_en2",
                            start = 1.8,
                            end = 3.4,
                            text = "Top opportunity for international students",
                            words = SrtParser.buildWordTimings("Top opportunity for international students", 1.8, 3.4)
                        ),
                        CaptionCue(
                            id = "v1_en3",
                            start = 3.5,
                            end = 4.9,
                            text = "Create viral multilingual captions today",
                            words = SrtParser.buildWordTimings("Create viral multilingual captions today", 3.5, 4.9)
                        )
                    )
                )
            ),
            overlays = listOf(
                GraphicOverlay(
                    id = "ov_v1_hero",
                    name = "Persian Orange Headline",
                    type = "persian_orange_headline",
                    content = "فنلاند",
                    color = "#FF7A00",
                    start = 0.1,
                    end = 2.2,
                    xPos = 0.50,
                    yPos = 0.28,
                    scale = 1.05,
                    animation = "Pop",
                    glow = 6.0
                ),
                GraphicOverlay(
                    id = "ov_v1_flare",
                    name = "Purple Light Flare",
                    type = "light_leak",
                    content = "✦ PURPLE FLARE ✦",
                    color = "#D946EF",
                    start = 1.5,
                    end = 2.5,
                    opacity = 0.80,
                    animation = "Fade"
                )
            ),
            audioClips = listOf(
                AudioClip(
                    id = "aud_v1_pop",
                    name = "Crisp Caption Pop",
                    category = "Pop",
                    source = "sfx/pop_crisp.wav",
                    start = 0.1,
                    duration = 0.4,
                    volume = 0.9,
                    ducking = true
                ),
                AudioClip(
                    id = "aud_v1_whoosh",
                    name = "Cinematic Whip Transition",
                    category = "Whoosh",
                    source = "sfx/whoosh_cinematic_whip.wav",
                    start = 1.7,
                    duration = 0.6,
                    volume = 0.85,
                    ducking = true
                )
            ),
            videoClips = listOf(
                VideoClipSegment(
                    id = "vc_1",
                    label = "Intro Hook",
                    sourceUri = "asset:///sample_video.mp4",
                    startInTimeline = 0.0,
                    endInTimeline = 1.75,
                    sourceTrimStart = 0.0,
                    transition = "Purple Flash"
                ),
                VideoClipSegment(
                    id = "vc_2",
                    label = "Main Body",
                    sourceUri = "asset:///sample_video.mp4",
                    startInTimeline = 1.75,
                    endInTimeline = 5.0,
                    sourceTrimStart = 1.75,
                    transition = "Cut"
                )
            )
        )
    }

    fun createVideo2VfxShowcaseProject(): Project {
        val duration = 5.0
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
                sfx = "Shimmer",
                sfxMode = "Sentence",
                sfxVolume = 55
            ),
            tracks = listOf(
                Track(
                    id = "tr_v2_multi",
                    name = "Multilingual RTL (FA / PS / UR)",
                    language = "fa",
                    color = "#8B5CF6",
                    visible = true,
                    style = SubtitleStyle(
                        preset = "Neon",
                        font = "Noto Sans Arabic",
                        fontSize = 25.0,
                        textColor = "#FFFFFF",
                        bgColor = "#1E1B4B",
                        bgEnabled = true,
                        outline = 2.5,
                        yPos = 0.78,
                        animation = "Bounce",
                        karaoke = true,
                        glow = 5.0,
                        glowColor = "#06B6D4"
                    ),
                    captions = listOf(
                        CaptionCue(
                            id = "v2_c1",
                            start = 0.1,
                            end = 1.8,
                            text = "برج‌های دوقلو و معماری درخشان",
                            words = SrtParser.buildWordTimings("برج‌های دوقلو و معماری درخشان", 0.1, 1.8)
                        ),
                        CaptionCue(
                            id = "v2_c2",
                            start = 1.9,
                            end = 3.4,
                            text = "د مصنوعي ځیرکتیا سره د ویډیو ایډیټ",
                            words = SrtParser.buildWordTimings("د مصنوعي ځیرکتیا سره د ویډیو ایډیټ", 1.9, 3.4)
                        ),
                        CaptionCue(
                            id = "v2_c3",
                            start = 3.5,
                            end = 4.9,
                            text = "جدید موشن گرافکس اور اردو سب ٹائٹلز",
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
                    category = "Shimmer",
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

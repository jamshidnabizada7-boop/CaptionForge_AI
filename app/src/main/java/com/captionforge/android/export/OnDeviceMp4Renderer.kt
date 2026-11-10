package com.captionforge.android.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Contrast
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.captionforge.android.audio.SfxPlayerManager
import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.GraphicOverlay
import com.captionforge.android.model.Project
import com.captionforge.android.model.SubtitleStyle
import com.captionforge.android.model.Track
import com.captionforge.android.ui.theme.CustomFontRegistry
import com.captionforge.android.ui.theme.resolveBundledFontResId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Real On-Device Hardware-Accelerated MP4 Video Renderer powered by Jetpack Media3 Transformer
 * and OverlayEffect (BitmapOverlay).
 *
 * Renders all 26 bundled multi-script fonts (Latin, Persian/Dari, Pashto, Urdu Nastaliq, Arabic),
 * custom user TTF/OTF fonts, Reference Video 1's 3-Tier Orange Persian Kinetic layout (without boxy
 * clutter, with 8-step 3D dark extrusion, multi-stop orange-gold gradient, and optical purple lens
 * flare), and mixes synchronized SFX audio stems with the presenter's voice audio into the MP4.
 */
@OptIn(UnstableApi::class)
class OnDeviceMp4Renderer(private val context: Context) {

    suspend fun renderProjectToMp4(
        project: Project,
        resolution: Int,
        onProgress: (String) -> Unit
    ): Result<File> = withContext(Dispatchers.Main) {
        runCatching {
            onProgress("Preparing input video & mixed SFX stem for Media3 Transformer...")
            val inputUri = resolvePlayableFileUri(project.source)

            val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeName = project.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "captionforge" }
            val outFile = File(outDir, "${safeName}_${resolution}p.mp4")
            if (outFile.exists()) {
                outFile.delete()
            }

            val trimStartMs = (project.trim.start * 1000.0).roundToInt().toLong().coerceAtLeast(0L)
            val trimEndMs = (project.trim.end * 1000.0).roundToInt().toLong().coerceAtLeast(trimStartMs + 200L)

            val mediaItem = MediaItem.Builder()
                .setUri(inputUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(trimStartMs)
                        .setEndPositionMs(trimEndMs)
                        .build()
                )
                .build()

            val targetWidth = when (project.aspectRatio) {
                "16:9" -> (resolution * 16) / 9
                "1:1" -> resolution
                "4:5" -> (resolution * 4) / 5
                else -> (resolution * 9) / 16
            }.coerceAtLeast(320)
            val targetHeight = resolution.coerceAtLeast(320)

            val videoEffects = mutableListOf<Effect>()
            videoEffects.add(Presentation.createForHeight(targetHeight))

            val contrastVal = (project.effects.contrast - 1.0).toFloat().coerceIn(-0.8f, 0.8f)
            if (abs(contrastVal) > 0.02f) {
                videoEffects.add(Contrast(contrastVal))
            }

            val captionOverlay = CaptionAndVfxBitmapOverlay(
                context = context,
                project = project,
                frameWidth = targetWidth,
                frameHeight = targetHeight
            )
            videoEffects.add(OverlayEffect(listOf<TextureOverlay>(captionOverlay)))

            val editedVideoItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(Effects(emptyList(), videoEffects))
                .build()

            // Synthesize synchronized SFX stem WAV and combine with main video sequence
            val sfxStemFile = withContext(Dispatchers.IO) {
                val stemOut = File(context.cacheDir, "sfx_stem_${project.id}.wav")
                SfxPlayerManager(context).renderProjectSfxStemWav(project, stemOut)
            }

            val sequences = mutableListOf<EditedMediaItemSequence>()
            sequences.add(EditedMediaItemSequence(listOf(editedVideoItem)))

            if (sfxStemFile.exists() && sfxStemFile.length() > 100L) {
                val sfxMediaItem = MediaItem.fromUri(Uri.fromFile(sfxStemFile))
                val editedSfxItem = EditedMediaItem.Builder(sfxMediaItem).build()
                sequences.add(EditedMediaItemSequence(listOf(editedSfxItem)))
            }

            val composition = Composition.Builder(sequences).build()

            val completion = CompletableDeferred<ExportResult>()
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        completion.complete(exportResult)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        completion.completeExceptionally(exportException)
                    }
                })
                .build()

            onProgress("Encoding ${resolution}p MP4 on-device with Media3 Transformer (0%)...")
            transformer.start(composition, outFile.absolutePath)

            val progressHolder = ProgressHolder()
            while (!completion.isCompleted) {
                val state = transformer.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress("Encoding ${resolution}p MP4 on-device (${progressHolder.progress}%)...")
                }
                delay(150)
            }

            completion.await()
            captionOverlay.disposeBitmap()

            if (!outFile.exists() || outFile.length() == 0L) {
                error("Rendered MP4 file was empty")
            }
            outFile
        }
    }

    private suspend fun resolvePlayableFileUri(source: String): Uri = withContext(Dispatchers.IO) {
        if (source.startsWith("asset:///")) {
            val assetName = source.removePrefix("asset:///")
            val cachedVideo = File(context.cacheDir, "cached_$assetName")
            if (!cachedVideo.exists() || cachedVideo.length() < 1024L) {
                context.assets.open(assetName).use { input ->
                    cachedVideo.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            Uri.fromFile(cachedVideo)
        } else if (source.startsWith("file://") || source.startsWith("content://") || source.startsWith("http")) {
            Uri.parse(source)
        } else {
            val file = File(source)
            if (file.exists()) Uri.fromFile(file) else Uri.parse(source)
        }
    }

    fun renderOverlayFrameBitmap(
        project: Project,
        timelineSec: Double,
        width: Int = 720,
        height: Int = 1280
    ): Bitmap {
        val overlay = CaptionAndVfxBitmapOverlay(
            context = context,
            project = project,
            frameWidth = width,
            frameHeight = height
        )
        val presentationUs = ((timelineSec - project.trim.start).coerceAtLeast(0.0) * 1_000_000.0).roundToInt().toLong()
        return overlay.getBitmap(presentationUs)
    }
}

@OptIn(UnstableApi::class)
private class CaptionAndVfxBitmapOverlay(
    private val context: Context,
    private val project: Project,
    private val frameWidth: Int,
    private val frameHeight: Int
) : BitmapOverlay() {

    private val bitmap: Bitmap = Bitmap.createBitmap(
        frameWidth.coerceAtLeast(320),
        frameHeight.coerceAtLeast(320),
        Bitmap.Config.ARGB_8888
    )
    private val canvas = Canvas(bitmap)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val flarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val typefaceCache = mutableMapOf<String, Typeface>()

    private fun resolveTypeface(fontName: String): Typeface {
        val key = fontName.lowercase().trim()
        typefaceCache[key]?.let { return it }

        CustomFontRegistry.getFontFile(fontName)?.let { file ->
            runCatching {
                val tf = Typeface.createFromFile(file)
                typefaceCache[key] = tf
                return tf
            }
        }

        val resId = resolveBundledFontResId(fontName)
        val tf = runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        typefaceCache[key] = tf
        return tf
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val timelineSec = project.trim.start + (presentationTimeUs / 1_000_000.0)
        bitmap.eraseColor(Color.TRANSPARENT)

        // 1. Background Overlays (depthLayer <= 0 or isBackground)
        project.overlays.filter {
            it.visible && (it.isBackground || it.depthLayer <= 0) &&
                timelineSec >= it.start && timelineSec <= it.end
        }.forEach { ov ->
            drawGraphicOverlay(canvas, ov, timelineSec)
        }

        // 2. Visible Subtitle Tracks
        project.tracks.filter { it.visible }.forEach { track ->
            val earlySec = track.style.early
            val cue = track.captions.firstOrNull { c ->
                timelineSec >= (c.start - earlySec) && timelineSec <= c.end
            }
            if (cue != null) {
                drawSubtitleCue(canvas, track, cue, timelineSec)
            }
        }

        // 3. Foreground Overlays
        project.overlays.filter {
            it.visible && !it.isBackground && it.depthLayer > 0 &&
                timelineSec >= it.start && timelineSec <= it.end
        }.forEach { ov ->
            drawGraphicOverlay(canvas, ov, timelineSec)
        }

        return bitmap
    }

    private fun drawOpticalPurpleFlare(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        intensity: Float,
        scaleFactor: Float
    ) {
        val alphaGlow = (intensity.coerceIn(0f, 1f) * 215f).roundToInt()
        if (alphaGlow <= 5) return

        // 1. Outer purple-magenta radial bloom (Reference Video 1 @ 8.7s)
        val radius = 210f * scaleFactor
        flarePaint.reset()
        flarePaint.isAntiAlias = true
        flarePaint.shader = RadialGradient(
            cx,
            cy,
            radius,
            intArrayOf(
                Color.argb(alphaGlow, 240, 171, 252),
                Color.argb((alphaGlow * 0.72f).roundToInt(), 217, 70, 239),
                Color.argb((alphaGlow * 0.35f).roundToInt(), 124, 58, 237),
                Color.argb(0, 88, 28, 135)
            ),
            floatArrayOf(0f, 0.26f, 0.62f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, flarePaint)

        // 2. Horizontal anamorphic lens streak
        val streakHalfW = 290f * scaleFactor
        val streakHalfH = 11f * scaleFactor
        flarePaint.shader = LinearGradient(
            cx - streakHalfW,
            cy,
            cx + streakHalfW,
            cy,
            intArrayOf(
                Color.argb(0, 168, 85, 247),
                Color.argb((alphaGlow * 0.85f).roundToInt(), 232, 121, 249),
                Color.argb(min(255, (alphaGlow * 1.1f).roundToInt()), 255, 255, 255),
                Color.argb((alphaGlow * 0.85f).roundToInt(), 232, 121, 249),
                Color.argb(0, 168, 85, 247)
            ),
            floatArrayOf(0f, 0.32f, 0.5f, 0.68f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(
            RectF(cx - streakHalfW, cy - streakHalfH, cx + streakHalfW, cy + streakHalfH),
            flarePaint
        )

        // 3. Diagonal secondary glint streak
        canvas.save()
        canvas.rotate(-18f, cx, cy)
        val glintW = 135f * scaleFactor
        val glintH = 5.5f * scaleFactor
        canvas.drawOval(
            RectF(cx - glintW, cy - glintH, cx + glintW, cy + glintH),
            flarePaint
        )
        canvas.restore()
        flarePaint.shader = null
    }

    private fun drawGraphicOverlay(canvas: Canvas, ov: GraphicOverlay, timelineSec: Double) {
        val cx = (ov.xPos.toFloat().coerceIn(0.08f, 0.92f)) * bitmap.width
        val cy = (ov.yPos.toFloat().coerceIn(0.08f, 0.92f)) * bitmap.height
        val scaleFactor = bitmap.height / 720f

        if (ov.type == "light_leak") {
            val dur = (ov.end - ov.start).coerceAtLeast(0.2)
            val norm = ((timelineSec - ov.start) / dur).coerceIn(0.0, 1.0)
            val env = sin(norm * PI).toFloat()
            val sweepX = cx + ((norm - 0.5).toFloat() * 140f * scaleFactor)
            drawOpticalPurpleFlare(
                canvas = canvas,
                cx = sweepX,
                cy = cy + 28f * scaleFactor,
                intensity = (ov.opacity.toFloat() * env).coerceIn(0.15f, 1.0f),
                scaleFactor = scaleFactor
            )
            return
        }

        val label = ov.content.ifBlank { ov.name }
        val colorInt = parseAndroidColor(ov.color, (ov.opacity * 100).toInt())

        fillPaint.reset()
        fillPaint.isAntiAlias = true
        fillPaint.typeface = resolveTypeface(if (ov.type == "metallic_banner") "Cinzel" else "Vazirmatn")
        fillPaint.textSize = (bitmap.height * 0.032f * ov.scale.toFloat()).coerceIn(20f, 72f)
        fillPaint.textAlign = Paint.Align.CENTER
        fillPaint.color = if (ov.type == "persian_orange_headline") colorInt else Color.WHITE

        val textWidth = fillPaint.measureText(label)
        val padH = 24f
        val padV = 14f
        val rect = RectF(
            cx - textWidth / 2f - padH,
            cy - fillPaint.textSize - padV,
            cx + textWidth / 2f + padH,
            cy + padV
        )

        canvas.save()
        canvas.rotate(ov.rotation.toFloat(), cx, cy)
        if (ov.type != "soft_bg_text") {
            boxPaint.color = Color.argb(175, 16, 12, 28)
            canvas.drawRoundRect(rect, 18f, 18f, boxPaint)

            strokePaint.color = colorInt
            strokePaint.strokeWidth = 3.2f
            canvas.drawRoundRect(rect, 18f, 18f, strokePaint)
        } else {
            fillPaint.color = Color.argb(115, 226, 232, 240)
        }

        canvas.drawText(label, cx, cy - 4f, fillPaint)
        canvas.restore()
    }

    private fun drawSubtitleCue(canvas: Canvas, track: Track, cue: CaptionCue, timelineSec: Double) {
        val style: SubtitleStyle = cue.customStyle ?: track.style
        val tf = resolveTypeface(style.font)
        val heroTf = resolveTypeface(if (style.font.equals("Vazirmatn", ignoreCase = true)) "Lalezar" else style.font)
        val scaleFactor = bitmap.height / 720f
        val basePxSize = (style.fontSize.toFloat() * scaleFactor * 1.32f * style.scale.toFloat()).coerceIn(20f, 108f)

        val cueDur = (cue.end - cue.start).coerceAtLeast(0.25)
        val elapsed = (timelineSec - (cue.start - style.early)).coerceAtLeast(0.0)
        val remaining = (cue.end - timelineSec).coerceAtLeast(0.0)

        val enterProg = (elapsed / 0.24).coerceIn(0.0, 1.0).toFloat()
        val exitProg = (remaining / 0.18).coerceIn(0.0, 1.0).toFloat()

        val animScale = when (style.animation.lowercase()) {
            "pop" -> {
                val spring = if (enterProg < 0.72f) {
                    0.55f + (enterProg / 0.72f) * 0.55f
                } else {
                    1.10f - ((enterProg - 0.72f) / 0.28f) * 0.10f
                }
                spring * (0.85f + 0.15f * exitProg)
            }
            "bounce" -> (0.78f + 0.22f * sin(enterProg * (PI.toFloat() / 2f))) * (0.85f + 0.15f * exitProg)
            else -> 0.92f + 0.08f * enterProg
        }
        val slideOffsetY = if (style.animation.lowercase().contains("slide")) {
            (1f - enterProg) * 34f * scaleFactor
        } else {
            0f
        }

        val cx = (style.xPos.toFloat().coerceIn(0.10f, 0.90f)) * bitmap.width
        val cy = (style.yPos.toFloat().coerceIn(0.10f, 0.90f)) * bitmap.height + slideOffsetY

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(style.rotation.toFloat())
        canvas.scale(animScale, animScale)

        if (style.threeTierLayout) {
            val (t1, t2, t3) = cue.resolveThreeTiers()

            // Authentic Reference Video 1 proportions:
            // Tier 1 (Top White Kicker): 0.88x base
            // Tier 2 (Center Giant Orange-Gold 3D Hero Headline): 1.96x base
            // Tier 3 (Bottom White Supporting Line): 0.92x base
            val tier1Size = basePxSize * 0.88f
            val activeHeroWord = cue.words.firstOrNull { w ->
                (w.tier == 2 || w.text == t2) && timelineSec >= w.start && timelineSec <= w.end
            }
            val heroWordPulse = if (activeHeroWord != null) {
                val wProg = ((timelineSec - activeHeroWord.start) / (activeHeroWord.end - activeHeroWord.start).coerceAtLeast(0.15)).coerceIn(0.0, 1.0)
                1.0f + 0.10f * sin(wProg * PI).toFloat()
            } else {
                1.0f
            }
            val tier2Size = basePxSize * 1.96f * heroWordPulse
            val tier3Size = basePxSize * 0.92f

            // Optional backdrop only if explicitly enabled by user
            if (style.bgEnabled && !style.bgShape.equals("None", ignoreCase = true) && style.bgOpacity > 5) {
                fillPaint.typeface = Typeface.create(heroTf, Typeface.BOLD)
                fillPaint.textSize = tier2Size
                val maxW = max(fillPaint.measureText(t2), fillPaint.measureText(t1) * 0.6f)
                val totalH = tier1Size + tier2Size + tier3Size
                val padH = 32f * scaleFactor
                val padV = 20f * scaleFactor
                val rect = RectF(-maxW / 2f - padH, -totalH * 0.55f - padV, maxW / 2f + padH, totalH * 0.52f + padV)
                boxPaint.color = parseAndroidColor(style.bgColor, min(style.bgOpacity, style.opacity))
                val radius = if (style.bgShape.equals("Pill", ignoreCase = true)) 60f * scaleFactor else 22f * scaleFactor
                canvas.drawRoundRect(rect, radius, radius, boxPaint)
            }

            // Tight sculptural vertical stacking matching Reference Video 1
            val tier1Y = -tier2Size * 0.72f
            val tier2Y = 12f * scaleFactor
            val tier3Y = tier2Y + tier3Size * 1.04f

            // 1. Draw Tier 1 (Top White Line with crisp black stroke & drop shadow)
            if (t1.isNotBlank()) {
                val t1Enter = ((elapsed - 0.02) / 0.18).coerceIn(0.0, 1.0).toFloat()
                if (t1Enter > 0.02f) {
                    drawStyledLine(
                        canvas = canvas,
                        text = transformText(t1, style.textTransform),
                        x = 0f,
                        y = tier1Y - (1f - t1Enter) * 14f * scaleFactor,
                        tf = tf,
                        pxSize = tier1Size,
                        colorHex = style.tier1Color,
                        style = style.copy(
                            extrusion3D = 2.0,
                            outline = max(3.2, style.outline * 0.85),
                            glow = 0.0
                        ),
                        scaleFactor = scaleFactor,
                        useGradient = false,
                        isHeroTier = false
                    )
                }
            }

            // 2. Draw Tier 2 (Massive Center Orange-Gold 3D Hero Headline)
            if (t2.isNotBlank()) {
                val t2Enter = ((elapsed - 0.06) / 0.22).coerceIn(0.0, 1.0).toFloat()
                val popScale = if (t2Enter < 0.75f) {
                    0.65f + (t2Enter / 0.75f) * 0.45f
                } else {
                    1.10f - ((t2Enter - 0.75f) / 0.25f) * 0.10f
                }
                canvas.save()
                canvas.translate(0f, tier2Y)
                canvas.scale(popScale, popScale)
                drawStyledLine(
                    canvas = canvas,
                    text = transformText(t2, style.textTransform),
                    x = 0f,
                    y = 0f,
                    tf = heroTf,
                    pxSize = tier2Size,
                    colorHex = style.tier2Color,
                    style = style.copy(
                        extrusion3D = max(5.5, style.extrusion3D),
                        outline = max(4.5, style.outline * 1.15),
                        glow = max(7.0, style.glow)
                    ),
                    scaleFactor = scaleFactor,
                    useGradient = true,
                    isHeroTier = true
                )
                canvas.restore()

                // Optical purple lens flare across the lower edge of the hero word during emphasis
                val flareWindow = ((elapsed - 0.25) / (cueDur * 0.65)).coerceIn(0.0, 1.0)
                if (flareWindow > 0.0 && flareWindow < 1.0) {
                    val flareEnv = sin(flareWindow * PI).toFloat()
                    val sweepOffset = ((flareWindow - 0.5).toFloat() * 110f * scaleFactor)
                    drawOpticalPurpleFlare(
                        canvas = canvas,
                        cx = sweepOffset,
                        cy = tier2Y + 8f * scaleFactor,
                        intensity = flareEnv * 0.62f,
                        scaleFactor = scaleFactor * 0.85f
                    )
                }
            }

            // 3. Draw Tier 3 (Bottom White Supporting Line tucked cleanly under Tier 2)
            if (t3.isNotBlank()) {
                val t3Enter = ((elapsed - 0.12) / 0.20).coerceIn(0.0, 1.0).toFloat()
                if (t3Enter > 0.02f) {
                    drawStyledLine(
                        canvas = canvas,
                        text = transformText(t3, style.textTransform),
                        x = 0f,
                        y = tier3Y + (1f - t3Enter) * 14f * scaleFactor,
                        tf = tf,
                        pxSize = tier3Size,
                        colorHex = style.tier3Color,
                        style = style.copy(
                            extrusion3D = 2.0,
                            outline = max(3.2, style.outline * 0.85),
                            glow = 0.0
                        ),
                        scaleFactor = scaleFactor,
                        useGradient = false,
                        isHeroTier = false
                    )
                }
            }
        } else {
            fillPaint.typeface = Typeface.create(tf, Typeface.BOLD)
            fillPaint.textSize = basePxSize
            val rawText = if (style.animation.equals("Typewriter", ignoreCase = true)) {
                val prog = ((timelineSec - cue.start) / (cueDur * 0.7)).coerceIn(0.1, 1.0)
                val chars = (cue.text.length * prog).roundToInt().coerceAtLeast(1)
                cue.text.take(chars)
            } else {
                cue.text
            }
            val lines = rawText.split("\n").map { transformText(it, style.textTransform) }
            val lineHeight = basePxSize * style.lineSpacing.toFloat().coerceIn(1.0f, 1.8f)
            val totalHeight = lines.size * lineHeight
            val maxLineWidth = lines.maxOfOrNull { fillPaint.measureText(it) } ?: 120f

            if (style.bgEnabled && !style.bgShape.equals("None", ignoreCase = true)) {
                val padH = 28f * scaleFactor
                val padV = 16f * scaleFactor
                val bgRect = RectF(
                    -maxLineWidth / 2f - padH,
                    -basePxSize - padV,
                    maxLineWidth / 2f + padH,
                    (totalHeight - lineHeight) + padV
                )
                boxPaint.color = parseAndroidColor(style.bgColor, min(style.bgOpacity, style.opacity))
                val radius = when (style.bgShape.lowercase()) {
                    "pill" -> 60f * scaleFactor
                    "sharp" -> 0f
                    else -> 20f * scaleFactor
                }
                canvas.drawRoundRect(bgRect, radius, radius, boxPaint)
            }

            val hasPerWordOverrides = cue.words.isNotEmpty() && (
                style.karaoke || cue.words.any { !it.customColor.isNullOrBlank() || it.scaleBoost != 1.0 || it.animation != "None" }
            )

            if (hasPerWordOverrides && lines.size == 1) {
                val isRtl = rawText.any { it.code in 0x0600..0x06FF || it.code in 0x0750..0x077F || it.code in 0xFB50..0xFDFF || it.code in 0xFE70..0xFEFF }
                val spaceW = fillPaint.measureText(" ") * 1.15f
                val orderedWords = if (isRtl) cue.words.reversed() else cue.words
                val wordWidths = orderedWords.map { w ->
                    val wPx = (basePxSize * w.scaleBoost.toFloat()).coerceIn(18f, 118f)
                    fillPaint.textSize = wPx
                    Triple(w, wPx, fillPaint.measureText(transformText(w.text, style.textTransform)))
                }
                val totalRowW = wordWidths.sumOf { it.third.toDouble() }.toFloat() + max(0, wordWidths.size - 1) * spaceW
                var cursorX = -totalRowW / 2f
                wordWidths.forEach { (w, wPx, wWidth) ->
                    val wordCenterX = cursorX + wWidth / 2f
                    val isWordSpokenNow = timelineSec >= w.start && timelineSec <= w.end
                    val wordColorHex = when {
                        !w.customColor.isNullOrBlank() -> w.customColor!!
                        style.karaoke && isWordSpokenNow -> style.secondaryColor
                        else -> style.textColor
                    }
                    if (style.karaoke && isWordSpokenNow) {
                        val pillRect = RectF(
                            wordCenterX - wWidth / 2f - 8f * scaleFactor,
                            -wPx * 0.9f,
                            wordCenterX + wWidth / 2f + 8f * scaleFactor,
                            wPx * 0.22f
                        )
                        boxPaint.color = parseAndroidColor(style.secondaryColor, 42)
                        canvas.drawRoundRect(pillRect, 10f * scaleFactor, 10f * scaleFactor, boxPaint)
                    }
                    val wordAnimScale = if (isWordSpokenNow && w.animation.lowercase() in listOf("pop", "bounce")) 1.15f else 1.0f
                    drawStyledLine(
                        canvas = canvas,
                        text = transformText(w.text, style.textTransform),
                        x = wordCenterX,
                        y = 0f,
                        tf = tf,
                        pxSize = wPx * wordAnimScale,
                        colorHex = wordColorHex,
                        style = style,
                        scaleFactor = scaleFactor,
                        useGradient = style.gradient && isWordSpokenNow,
                        isHeroTier = isWordSpokenNow
                    )
                    cursorX += wWidth + spaceW
                }
            } else {
                lines.forEachIndexed { idx, line ->
                    val baselineY = idx * lineHeight
                    drawStyledLine(
                        canvas = canvas,
                        text = line,
                        x = 0f,
                        y = baselineY,
                        tf = tf,
                        pxSize = basePxSize,
                        colorHex = style.textColor,
                        style = style,
                        scaleFactor = scaleFactor,
                        useGradient = style.gradient,
                        isHeroTier = false
                    )
                }
            }
        }

        canvas.restore()
    }

    private fun drawStyledLine(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        tf: Typeface,
        pxSize: Float,
        colorHex: String,
        style: SubtitleStyle,
        scaleFactor: Float,
        useGradient: Boolean,
        isHeroTier: Boolean = false
    ) {
        val boldTf = Typeface.create(tf, Typeface.BOLD)
        val outlineCol = parseAndroidColor(style.outlineColor, style.opacity)

        // 0. Soft Ambient Drop Shadow Plate behind text for instant legibility over bright skin/clothing
        fillPaint.reset()
        fillPaint.isAntiAlias = true
        fillPaint.typeface = boldTf
        fillPaint.textSize = pxSize
        fillPaint.textAlign = Paint.Align.CENTER
        fillPaint.color = Color.argb(145, 0, 0, 0)
        fillPaint.setShadowLayer(
            (if (isHeroTier) 18f else 10f) * scaleFactor,
            0f,
            (if (isHeroTier) 7f else 4f) * scaleFactor,
            Color.argb(210, 0, 0, 0)
        )
        canvas.drawText(text, x, y + 3f * scaleFactor, fillPaint)
        fillPaint.clearShadowLayer()

        // 1. 3D Extrusion Stack (deep dark-brown/black sculptural depth)
        val depthSteps = style.extrusion3D.roundToInt().coerceIn(0, 9)
        if (depthSteps > 0) {
            fillPaint.reset()
            fillPaint.isAntiAlias = true
            fillPaint.typeface = boldTf
            fillPaint.textSize = pxSize
            fillPaint.textAlign = Paint.Align.CENTER
            for (d in depthSteps downTo 1) {
                val offsetX = d * 1.35f * scaleFactor
                val offsetY = d * 2.15f * scaleFactor
                fillPaint.color = if (isHeroTier && d <= 2) {
                    Color.argb(255, 68, 20, 4)
                } else {
                    outlineCol
                }
                canvas.drawText(text, x + offsetX, y + offsetY, fillPaint)
            }
        }

        // 2. Crisp Heavy Outer Stroke Outline
        if (style.outline > 0.1) {
            strokePaint.typeface = boldTf
            strokePaint.textSize = pxSize
            strokePaint.textAlign = Paint.Align.CENTER
            strokePaint.strokeWidth = (style.outline.toFloat() * scaleFactor * (if (isHeroTier) 2.15f else 1.75f)).coerceAtLeast(2.5f)
            strokePaint.color = outlineCol
            canvas.drawText(text, x, y, strokePaint)
        }

        // 3. Foreground Fill + Warm Glow/Shadow + Multi-Stop Vertical Orange-Gold Gradient
        fillPaint.reset()
        fillPaint.isAntiAlias = true
        fillPaint.typeface = boldTf
        fillPaint.textSize = pxSize
        fillPaint.textAlign = Paint.Align.CENTER

        if (style.glow > 0.1) {
            fillPaint.setShadowLayer(
                (style.glow.toFloat() * 2.4f * scaleFactor).coerceAtMost(24f),
                0f,
                0f,
                parseAndroidColor(style.glowColor, min(90, style.opacity))
            )
        } else if (style.shadowDepth > 0.1) {
            val sOffset = (style.shadowDepth.toFloat() * scaleFactor)
            fillPaint.setShadowLayer(
                (style.shadowBlur.toFloat() * scaleFactor).coerceAtLeast(1f),
                sOffset,
                sOffset,
                parseAndroidColor(style.shadowColor, style.opacity)
            )
        }

        if (useGradient) {
            val topColor = if (isHeroTier) {
                parseAndroidColor(style.secondaryColor, style.opacity) // Bright Gold #FFB800 top
            } else {
                parseAndroidColor(colorHex, style.opacity)
            }
            val midColor = parseAndroidColor(colorHex, style.opacity) // Vibrant Orange #FF7A00 mid
            val botColor = parseAndroidColor(style.tertiaryColor, style.opacity) // Deep Burnt Orange #D93800 bottom
            fillPaint.shader = LinearGradient(
                x,
                y - pxSize * 0.88f,
                x,
                y + pxSize * 0.15f,
                intArrayOf(topColor, midColor, botColor),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP
            )
        } else {
            fillPaint.shader = null
            fillPaint.color = parseAndroidColor(colorHex, style.opacity)
        }

        canvas.drawText(text, x, y, fillPaint)
        fillPaint.shader = null
    }

    private fun transformText(text: String, mode: String): String {
        return when (mode.lowercase()) {
            "uppercase" -> text.uppercase(Locale.getDefault())
            "lowercase" -> text.lowercase(Locale.getDefault())
            else -> text
        }
    }

    private fun parseAndroidColor(hex: String, opacityPercent: Int): Int {
        val clean = hex.trim().removePrefix("#")
        val rgb = clean.toLongOrNull(16) ?: 0xFFFFFFL
        val r = ((rgb shr 16) and 0xFF).toInt()
        val g = ((rgb shr 8) and 0xFF).toInt()
        val b = (rgb and 0xFF).toInt()
        val a = ((opacityPercent.coerceIn(0, 100) / 100.0) * 255.0).roundToInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }

    fun disposeBitmap() {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}

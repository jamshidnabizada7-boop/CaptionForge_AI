package com.captionforge.android.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
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
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Real On-Device Hardware-Accelerated MP4 Video Renderer powered by Jetpack Media3 Transformer
 * and OverlayEffect (BitmapOverlay).
 *
 * Renders all 26 bundled multi-script fonts (Latin, Persian/Dari, Pashto, Urdu Nastaliq, Arabic),
 * custom user TTF/OTF fonts, Reference Video 1's 3-Tier Orange Persian Kinetic layout,
 * 3D extrusion stacks, multi-stop gradients, outlines, glow, and VFX layers directly onto the
 * trimmed MP4 video stream.
 */
@OptIn(UnstableApi::class)
class OnDeviceMp4Renderer(private val context: Context) {

    suspend fun renderProjectToMp4(
        project: Project,
        resolution: Int,
        onProgress: (String) -> Unit
    ): Result<File> = withContext(Dispatchers.Main) {
        runCatching {
            onProgress("Preparing input video stream for on-device Media3 Transformer...")
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
            if (kotlin.math.abs(contrastVal) > 0.02f) {
                videoEffects.add(Contrast(contrastVal))
            }

            val captionOverlay = CaptionAndVfxBitmapOverlay(
                context = context,
                project = project,
                frameWidth = targetWidth,
                frameHeight = targetHeight
            )
            videoEffects.add(OverlayEffect(listOf<TextureOverlay>(captionOverlay)))

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(Effects(emptyList(), videoEffects))
                .build()

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
            transformer.start(editedMediaItem, outFile.absolutePath)

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

        // 1. Background Overlays
        project.overlays.filter {
            it.visible && (it.isBackground || it.depthLayer <= 0) &&
                timelineSec >= it.start && timelineSec <= it.end
        }.forEach { ov ->
            drawGraphicOverlay(canvas, ov)
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
            drawGraphicOverlay(canvas, ov)
        }

        return bitmap
    }

    private fun drawGraphicOverlay(canvas: Canvas, ov: GraphicOverlay) {
        val label = ov.content.ifBlank { ov.name }
        val cx = (ov.xPos.toFloat().coerceIn(0.08f, 0.92f)) * bitmap.width
        val cy = (ov.yPos.toFloat().coerceIn(0.08f, 0.92f)) * bitmap.height
        val colorInt = parseAndroidColor(ov.color, (ov.opacity * 100).toInt())

        fillPaint.reset()
        fillPaint.isAntiAlias = true
        fillPaint.typeface = resolveTypeface("Vazirmatn")
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
        boxPaint.color = Color.argb(190, 16, 12, 28)
        canvas.drawRoundRect(rect, 18f, 18f, boxPaint)

        strokePaint.color = colorInt
        strokePaint.strokeWidth = 3.5f
        canvas.drawRoundRect(rect, 18f, 18f, strokePaint)

        canvas.drawText(label, cx, cy - 4f, fillPaint)
        canvas.restore()
    }

    private fun drawSubtitleCue(canvas: Canvas, track: Track, cue: CaptionCue, timelineSec: Double) {
        val style: SubtitleStyle = cue.customStyle ?: track.style
        val tf = resolveTypeface(style.font)
        val scaleFactor = bitmap.height / 720f
        val basePxSize = (style.fontSize.toFloat() * scaleFactor * 1.32f * style.scale.toFloat()).coerceIn(20f, 96f)

        val elapsed = (timelineSec - (cue.start - style.early)).coerceAtLeast(0.0)
        val enterProg = (elapsed / 0.22).coerceIn(0.0, 1.0).toFloat()
        val animScale = when (style.animation.lowercase()) {
            "pop" -> 0.75f + 0.25f * sin(enterProg * (PI.toFloat() / 2f))
            "bounce" -> 0.82f + 0.18f * enterProg
            else -> 1.0f
        }

        val cx = (style.xPos.toFloat().coerceIn(0.10f, 0.90f)) * bitmap.width
        val cy = (style.yPos.toFloat().coerceIn(0.10f, 0.90f)) * bitmap.height

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(style.rotation.toFloat())
        canvas.scale(animScale, animScale)

        if (style.threeTierLayout) {
            val (t1, t2, t3) = cue.resolveThreeTiers()
            val tierSpecs = mutableListOf<Triple<String, Float, String>>()
            if (t1.isNotBlank()) tierSpecs.add(Triple(t1, basePxSize * 0.76f, style.tier1Color))
            if (t2.isNotBlank()) tierSpecs.add(Triple(t2, basePxSize * 1.30f, style.tier2Color))
            if (t3.isNotBlank()) tierSpecs.add(Triple(t3, basePxSize * 0.84f, style.tier3Color))

            fillPaint.typeface = Typeface.create(tf, Typeface.BOLD)
            val totalH = tierSpecs.sumOf { (it.second * 1.25f).toDouble() }.toFloat()
            val maxW = tierSpecs.maxOfOrNull { (txt, sz, _) ->
                fillPaint.textSize = sz
                fillPaint.measureText(txt)
            } ?: 180f

            if (style.bgEnabled && !style.bgShape.equals("None", ignoreCase = true)) {
                val padH = 30f * scaleFactor
                val padV = 18f * scaleFactor
                val rect = RectF(-maxW / 2f - padH, -totalH / 2f - padV, maxW / 2f + padH, totalH / 2f + padV)
                boxPaint.color = parseAndroidColor(style.bgColor, min(style.bgOpacity, style.opacity))
                val radius = if (style.bgShape.equals("Pill", ignoreCase = true)) 60f * scaleFactor else 22f * scaleFactor
                canvas.drawRoundRect(rect, radius, radius, boxPaint)
            }

            var curY = -totalH / 2f + (tierSpecs.firstOrNull()?.second ?: basePxSize) * 0.85f
            tierSpecs.forEachIndexed { idx, (lineText, lineSize, tierColorHex) ->
                val isHero = (idx == 1 || (tierSpecs.size == 1 && idx == 0))
                drawStyledLine(
                    canvas = canvas,
                    text = transformText(lineText, style.textTransform),
                    x = 0f,
                    y = curY,
                    tf = tf,
                    pxSize = lineSize,
                    colorHex = tierColorHex,
                    style = style,
                    scaleFactor = scaleFactor,
                    useGradient = isHero && style.gradient
                )
                curY += lineSize * 1.25f
            }
        } else {
            fillPaint.typeface = Typeface.create(tf, Typeface.BOLD)
            fillPaint.textSize = basePxSize
            val rawText = if (style.animation.equals("Typewriter", ignoreCase = true)) {
                val cueDur = (cue.end - cue.start).coerceAtLeast(0.2)
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
                val spaceW = fillPaint.measureText(" ")
                val wordWidths = cue.words.map { w ->
                    val wPx = (basePxSize * w.scaleBoost.toFloat()).coerceIn(18f, 110f)
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
                        boxPaint.color = parseAndroidColor(style.secondaryColor, 38)
                        canvas.drawRoundRect(pillRect, 10f * scaleFactor, 10f * scaleFactor, boxPaint)
                    }
                    val wordAnimScale = if (isWordSpokenNow && w.animation.lowercase() in listOf("pop", "bounce")) 1.12f else 1.0f
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
                        useGradient = style.gradient && isWordSpokenNow
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
                        useGradient = style.gradient
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
        useGradient: Boolean
    ) {
        val boldTf = Typeface.create(tf, Typeface.BOLD)
        val outlineCol = parseAndroidColor(style.outlineColor, style.opacity)

        // 1. 3D Extrusion Stack
        val depthSteps = style.extrusion3D.roundToInt().coerceIn(0, 6)
        if (depthSteps > 0) {
            fillPaint.reset()
            fillPaint.isAntiAlias = true
            fillPaint.typeface = boldTf
            fillPaint.textSize = pxSize
            fillPaint.textAlign = Paint.Align.CENTER
            fillPaint.color = outlineCol
            for (d in depthSteps downTo 1) {
                val offset = d * 1.8f * scaleFactor
                canvas.drawText(text, x + offset, y + offset, fillPaint)
            }
        }

        // 2. Stroke Outline
        if (style.outline > 0.1) {
            strokePaint.typeface = boldTf
            strokePaint.textSize = pxSize
            strokePaint.textAlign = Paint.Align.CENTER
            strokePaint.strokeWidth = (style.outline.toFloat() * scaleFactor * 1.7f).coerceAtLeast(2f)
            strokePaint.color = outlineCol
            canvas.drawText(text, x, y, strokePaint)
        }

        // 3. Foreground Fill + Glow/Shadow + Optional Gradient
        fillPaint.reset()
        fillPaint.isAntiAlias = true
        fillPaint.typeface = boldTf
        fillPaint.textSize = pxSize
        fillPaint.textAlign = Paint.Align.CENTER

        if (style.glow > 0.1) {
            fillPaint.setShadowLayer(
                (style.glow.toFloat() * 2.6f * scaleFactor).coerceAtMost(24f),
                0f,
                0f,
                parseAndroidColor(style.glowColor, style.opacity)
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
            fillPaint.shader = LinearGradient(
                x,
                y - pxSize,
                x,
                y + pxSize * 0.2f,
                intArrayOf(
                    parseAndroidColor(colorHex, style.opacity),
                    parseAndroidColor(style.secondaryColor, style.opacity),
                    parseAndroidColor(style.tertiaryColor, style.opacity)
                ),
                null,
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

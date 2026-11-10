package com.captionforge.android.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.CaptionTierStyle
import com.captionforge.android.model.GraphicOverlay
import com.captionforge.android.model.OverlayKeyframe
import com.captionforge.android.model.Project
import com.captionforge.android.model.RtlTextShaper
import com.captionforge.android.model.SubtitleStyle
import com.captionforge.android.model.Track
import com.captionforge.android.ui.theme.AppColors
import com.captionforge.android.ui.theme.LalezarFontFamily
import com.captionforge.android.ui.theme.isRtlScript
import com.captionforge.android.ui.theme.parseHexColor
import com.captionforge.android.ui.theme.resolveComposeFontWeight
import com.captionforge.android.ui.theme.resolveSubtitleFontFamily
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SubtitleOverlayCanvas(
    project: Project,
    currentTime: Double,
    selectedCueId: String?,
    selectedOverlayId: String?,
    sfxPulse: Boolean,
    onSelectCue: (Track, CaptionCue) -> Unit,
    onSelectOverlay: (GraphicOverlay) -> Unit,
    onDragCuePosition: ((Double, Double) -> Unit)? = null,
    onDragOverlayPosition: ((String, Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("caption_layer_canvas")
    ) {
        val canvasWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val canvasHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // 1. Background Subject-Masked Graphic Overlays (depthLayer <= 0 or isBackground)
        project.overlays.filter {
            it.visible && (it.isBackground || it.depthLayer <= 0) &&
                currentTime >= it.start && currentTime <= it.end
        }.forEach { overlay ->
            GraphicOverlayItem(
                overlay = overlay,
                currentTime = currentTime,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                isSelected = overlay.id == selectedOverlayId,
                onClick = { onSelectOverlay(overlay) },
                onDragPosition = onDragOverlayPosition
            )
        }

        // 2. Active Subtitle Tracks (captionLayer — up to 5 simultaneous tracks)
        project.tracks.filter { it.visible }.forEach { track ->
            val earlyOffset = track.style.early
            val activeCue = track.captions.firstOrNull { cue ->
                currentTime >= (cue.start - earlyOffset) && currentTime <= cue.end
            }
            if (activeCue != null) {
                ActiveTrackSubtitleItem(
                    track = track,
                    cue = activeCue,
                    currentTime = currentTime,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    isSelected = activeCue.id == selectedCueId,
                    onClick = { onSelectCue(track, activeCue) },
                    onDragPosition = onDragCuePosition
                )
            }
        }

        // 3. Foreground Graphic Overlays & VFX Templates (including Optical Purple Lens Flares)
        project.overlays.filter {
            it.visible && !it.isBackground && it.depthLayer > 0 &&
                currentTime >= it.start && currentTime <= it.end
        }.forEach { overlay ->
            GraphicOverlayItem(
                overlay = overlay,
                currentTime = currentTime,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                isSelected = overlay.id == selectedOverlayId,
                onClick = { onSelectOverlay(overlay) },
                onDragPosition = onDragOverlayPosition
            )
        }

        // 4. SFX Trigger Visual Indicator Badge
        if (sfxPulse && project.effects.sfx != "None") {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.AccentAmber.copy(alpha = 0.90f)
            ) {
                Text(
                    text = "♪ ${project.effects.sfx}",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveTrackSubtitleItem(
    track: Track,
    cue: CaptionCue,
    currentTime: Double,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDragPosition: ((Double, Double) -> Unit)?
) {
    val style = cue.customStyle ?: track.style
    val fontFamily = resolveSubtitleFontFamily(style.font)
    val fontWeight = resolveComposeFontWeight(style.fontWeight)
    val rtl = isRtlScript(cue.text) ||
        isRtlScript(cue.tier1Text) ||
        isRtlScript(cue.tier2Text) ||
        isRtlScript(cue.tier3Text) ||
        track.language in listOf("fa", "ps", "ur", "ar")

    // Interpolate cue keyframes if present
    val kfState = interpolateKeyframes(
        keyframes = cue.keyframes,
        relTime = (currentTime - cue.start).coerceAtLeast(0.0),
        defaultX = style.xPos,
        defaultY = style.yPos,
        defaultScale = style.scale,
        defaultRot = style.rotation,
        defaultOpacity = style.opacity / 100.0
    )

    val elapsed = (currentTime - (cue.start - style.early)).coerceAtLeast(0.0)
    val remaining = (cue.end - currentTime).coerceAtLeast(0.0)
    val enterProgress = (elapsed / 0.24).coerceIn(0.0, 1.0).toFloat()
    val exitProgress = (remaining / 0.18).coerceIn(0.0, 1.0).toFloat()

    val enterScale = when (style.animation.lowercase()) {
        "pop" -> {
            if (enterProgress < 0.72f) {
                0.55f + (enterProgress / 0.72f) * 0.55f
            } else {
                1.10f - ((enterProgress - 0.72f) / 0.28f) * 0.10f
            }
        }
        "bounce" -> 0.80f + 0.20f * enterProgress + 0.08f * sin(enterProgress * PI.toFloat() * 2f)
        else -> 1f
    }
    val exitScale = when (style.exitAnimation.lowercase()) {
        "scale down" -> 0.78f + 0.22f * exitProgress
        else -> 1f
    }
    val totalScale = (kfState.scale.toFloat() * enterScale * exitScale).coerceIn(0.4f, 2.6f)

    val slideOffsetY = when (style.animation.lowercase()) {
        "slide up" -> ((1f - enterProgress) * 22f).dp
        "slide down" -> (-(1f - enterProgress) * 22f).dp
        else -> 0.dp
    } + when (style.exitAnimation.lowercase()) {
        "slide down" -> ((1f - exitProgress) * 18f).dp
        else -> 0.dp
    }

    val enterAlpha = if (style.animation.equals("Fade", ignoreCase = true)) enterProgress else 1f
    val exitAlpha = if (style.exitAnimation.equals("Fade", ignoreCase = true) || style.exitAnimation.equals("Blur Out", ignoreCase = true)) exitProgress else 1f
    val totalAlpha = (enterAlpha * exitAlpha * kfState.opacity.toFloat()).coerceIn(0.05f, 1f)

    val effectiveBlur = style.blur.toFloat() +
        if (style.exitAnimation.equals("Blur Out", ignoreCase = true)) (1f - exitProgress) * 8f else 0f

    val faceMotionX = if (style.followFace) {
        (sin(currentTime * 1.8) * 14.0).dp
    } else 0.dp

    var dragAccumX by remember(cue.id, style.xPos) { mutableFloatStateOf(kfState.x.toFloat()) }
    var dragAccumY by remember(cue.id, style.yPos) { mutableFloatStateOf(kfState.y.toFloat()) }

    val xBias = ((dragAccumX.coerceIn(0.08f, 0.92f) - 0.5f) * 2f)
    val yFraction = dragAccumY.coerceIn(0.08f, 0.92f)
    val density = LocalDensity.current
    val topPaddingDp = with(density) { (canvasHeightPx * (yFraction * 0.78f)).toDp() }
    val horizontalOffsetDp = with(density) { (canvasWidthPx * xBias * 0.38f).toDp() }

    val hasVisibleBox = style.bgEnabled && !style.bgShape.equals("None", ignoreCase = true) && style.bgOpacity > 5
    val bgShape: Shape = when (style.bgShape.lowercase()) {
        "pill" -> RoundedCornerShape(50)
        "sharp" -> RectangleShape
        "banner" -> RoundedCornerShape(4.dp)
        else -> RoundedCornerShape(14.dp)
    }

    val bgColor = if (hasVisibleBox) {
        parseHexColor(style.bgColor, style.bgOpacity)
    } else {
        Color.Transparent
    }

    val scaledFontSize = (style.fontSize * 0.72).coerceIn(14.0, 38.0).sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            Box(
                modifier = Modifier
                    .padding(top = topPaddingDp)
                    .offset(x = horizontalOffsetDp + faceMotionX, y = slideOffsetY)
                    .scale(totalScale)
                    .rotate(kfState.rotation.toFloat())
                    .alpha(totalAlpha)
                    .let { mod -> if (effectiveBlur > 0.2f) mod.blur(effectiveBlur.dp) else mod }
                    .let { mod -> if (hasVisibleBox) mod.clip(bgShape).background(bgColor) else mod }
                    .border(
                        width = if (isSelected) 1.5.dp else if (style.overlay == "Glowing Border") 1.5.dp else 0.dp,
                        color = when {
                            isSelected -> AppColors.PrimaryPurpleLight.copy(alpha = 0.75f)
                            style.overlay == "Glowing Border" -> parseHexColor(style.glowColor)
                            else -> Color.Transparent
                        },
                        shape = bgShape
                    )
                    .pointerInput(cue.id, canvasWidthPx, canvasHeightPx) {
                        detectDragGestures(
                            onDragEnd = {
                                onDragPosition?.invoke(
                                    dragAccumX.toDouble().coerceIn(0.08, 0.92),
                                    dragAccumY.toDouble().coerceIn(0.08, 0.92)
                                )
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            dragAccumX = (dragAccumX + dragAmount.x / canvasWidthPx).coerceIn(0.08f, 0.92f)
                            dragAccumY = (dragAccumY + dragAmount.y / canvasHeightPx).coerceIn(0.08f, 0.92f)
                        }
                    }
                    .clickable { onClick() }
                    .padding(
                        horizontal = if (hasVisibleBox) 14.dp else 6.dp,
                        vertical = if (hasVisibleBox) 8.dp else 2.dp
                    )
            ) {
                if (style.threeTierLayout) {
                    ThreeTierKineticSubtitleContent(
                        cue = cue,
                        style = style,
                        baseFontSize = scaledFontSize,
                        currentTime = currentTime,
                        isRtl = rtl
                    )
                } else if (style.karaoke && cue.words.isNotEmpty()) {
                    val activeWords = if (style.oneLine) {
                        val currentWord = cue.words.firstOrNull { currentTime >= it.start && currentTime <= it.end }
                            ?: cue.words.lastOrNull { currentTime >= it.start }
                            ?: cue.words.first()
                        listOf(currentWord)
                    } else {
                        cue.words
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.widthIn(max = 330.dp)
                    ) {
                        activeWords.forEach { word ->
                            val isActiveWord = currentTime >= word.start && currentTime <= word.end
                            val isPastWord = currentTime > word.end
                            val wordText = RtlTextShaper.shapeLineForRtlDisplay(
                                applyTextTransform(word.text, style.textTransform),
                                forceRtl = rtl
                            )
                            val wordScale = if (isActiveWord) {
                                (word.scaleBoost.toFloat() * 1.14f).coerceIn(1.0f, 1.75f)
                            } else {
                                word.scaleBoost.toFloat().coerceIn(0.8f, 1.4f)
                            }
                            val wordColor = when {
                                word.customColor != null && (isActiveWord || isPastWord) -> parseHexColor(word.customColor)
                                isActiveWord -> parseHexColor(style.secondaryColor.ifBlank { "#FACC15" })
                                isPastWord -> parseHexColor(style.textColor)
                                else -> parseHexColor(style.textColor).copy(alpha = 0.76f)
                            }

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                                    .scale(wordScale)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isActiveWord) parseHexColor(style.secondaryColor).copy(alpha = 0.24f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            ) {
                                StyledLayeredText(
                                    text = wordText,
                                    fontFamily = fontFamily,
                                    fontWeight = if (isActiveWord) FontWeight.Black else fontWeight,
                                    fontSize = scaledFontSize,
                                    fillColor = wordColor,
                                    style = style,
                                    useGradient = isActiveWord && style.gradient,
                                    isRtl = rtl
                                )
                            }
                        }
                    }
                } else {
                    val rawText = if (style.animation.equals("Typewriter", ignoreCase = true)) {
                        val totalProgress = ((currentTime - cue.start) / cue.duration).coerceIn(0.05, 1.0)
                        val count = (cue.text.length * totalProgress).toInt().coerceAtLeast(1)
                        applyTextTransform(cue.text.take(count), style.textTransform)
                    } else {
                        applyTextTransform(cue.text, style.textTransform)
                    }
                    val displayText = RtlTextShaper.shapeLineForRtlDisplay(rawText, forceRtl = rtl)

                    StyledLayeredText(
                        text = displayText,
                        fontFamily = fontFamily,
                        fontWeight = fontWeight,
                        fontSize = scaledFontSize,
                        fillColor = parseHexColor(style.textColor),
                        style = style,
                        useGradient = style.gradient,
                        isRtl = rtl
                    )
                }
            }
        }
    }
}

/**
 * Recreates Reference Video 1's signature 3-Tier Persian Orange Kinetic Typography in `captionLayer`:
 * - Tier 1 (Top Sub-Headline): Crisp white supporting line (e.g., "ویزای تحصیلی" / "ویزه خانم")
 *   with its own distinct stroke, tight drop-shadow, and subtle inner luminance.
 * - Tier 2 (Center Hero Headline): Massive (~2.0x) orange-gold gradient Persian display headline
 *   ("فنلاند" / "نازیــــه") with deep 3D dark extrusion stack, heavy rounded stroke, warm outer bloom,
 *   golden-white inner core glow, and sweeping Optical Purple Anamorphic Lens Flare.
 * - Tier 3 (Bottom Sub-Headline): Crisp white supporting line tucked closely below Tier 2
 *   ("بدون مدرک زبان" / "است") with distinct sub-headline stroke, shadow, and inner glow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThreeTierKineticSubtitleContent(
    cue: CaptionCue,
    style: SubtitleStyle,
    baseFontSize: TextUnit,
    currentTime: Double,
    isRtl: Boolean
) {
    val (tier1Raw, tier2Raw, tier3Raw) = cue.resolveThreeTiers(normalizeRtl = true)
    val tier1Style = style.resolveTierLayerStyle(1)
    val tier2Style = style.resolveTierLayerStyle(2)
    val tier3Style = style.resolveTierLayerStyle(3)

    val tier1Size = (baseFontSize.value * tier1Style.fontScale.toFloat()).sp
    val tier2HeroSize = (baseFontSize.value * tier2Style.fontScale.toFloat()).sp
    val tier3Size = (baseFontSize.value * tier3Style.fontScale.toFloat()).sp

    val tier1FontFamily = resolveSubtitleFontFamily(tier1Style.fontFamily)
    val heroFontFamily = resolveSubtitleFontFamily(tier2Style.fontFamily)
    val tier3FontFamily = resolveSubtitleFontFamily(tier3Style.fontFamily)

    val relProgress = ((currentTime - cue.start) / cue.duration).coerceIn(0.0, 1.0).toFloat()
    val activeHeroWord = cue.words.firstOrNull { w ->
        (w.tier == 2 || RtlTextShaper.normalizePersianText(w.text) == tier2Raw) &&
            currentTime >= w.start && currentTime <= w.end
    }
    val heroPulse = if (activeHeroWord != null) {
        val wProg = ((currentTime - activeHeroWord.start) / (activeHeroWord.end - activeHeroWord.start).coerceAtLeast(0.15)).coerceIn(0.0, 1.0).toFloat()
        1.0f + 0.12f * sin(wProg * PI.toFloat())
    } else {
        1.0f + 0.05f * sin(relProgress * PI.toFloat())
    }

    val tierHorizontalAlignment = when (style.align.lowercase()) {
        "left" -> if (isRtl) Alignment.End else Alignment.Start
        "right" -> if (isRtl) Alignment.Start else Alignment.End
        else -> Alignment.CenterHorizontally
    }

    Box(contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = tierHorizontalAlignment,
            verticalArrangement = Arrangement.spacedBy((-6).dp),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            // Tier 1: Top Sub-Headline with distinct sub-headline stroke, drop-shadow, and inner glow
            if (tier1Raw.isNotBlank()) {
                val shapedT1 = RtlTextShaper.shapeLineForRtlDisplay(
                    applyTextTransform(tier1Raw, style.textTransform),
                    forceRtl = isRtl
                )
                CaptionTierLayeredText(
                    text = shapedT1,
                    tierStyle = tier1Style,
                    parentStyle = style,
                    fontFamily = tier1FontFamily,
                    fontWeight = resolveComposeFontWeight(tier1Style.fontWeight),
                    fontSize = tier1Size,
                    isRtl = isRtl
                )
            }

            // Tier 2: Massive Hero Orange-Gold 3D Persian Headline + Inner Glow + Optical Purple Lens Flare
            if (tier2Raw.isNotBlank()) {
                val shapedT2 = RtlTextShaper.shapeLineForRtlDisplay(
                    applyTextTransform(tier2Raw, style.textTransform),
                    forceRtl = isRtl
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.scale(heroPulse)
                ) {
                    CaptionTierLayeredText(
                        text = shapedT2,
                        tierStyle = tier2Style,
                        parentStyle = style,
                        fontFamily = heroFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = tier2HeroSize,
                        isRtl = isRtl
                    )

                    // Sweeping Optical Purple Anamorphic Lens Flare during hero emphasis
                    if (relProgress in 0.12f..0.82f) {
                        val flareEnv = sin(((relProgress - 0.12f) / 0.70f) * PI.toFloat()).coerceIn(0f, 1f)
                        val sweepBias = (relProgress - 0.47f) * 140f
                        OpticalPurpleLensFlareCanvas(
                            intensity = flareEnv * 0.78f,
                            sweepOffsetX = sweepBias,
                            modifier = Modifier
                                .width(240.dp)
                                .height(46.dp)
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            // Tier 3: Bottom Sub-Headline with distinct sub-headline stroke, drop-shadow, inner glow & word highlight
            if (tier3Raw.isNotBlank()) {
                val tier3Words = cue.words.filter { it.tier == 3 }
                if (tier3Words.isNotEmpty() && style.karaoke) {
                    FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.Center
                    ) {
                        tier3Words.forEach { w ->
                            val active = currentTime >= w.start && currentTime <= w.end
                            val wordHex = when {
                                active -> w.customColor ?: "#FFB800"
                                w.customColor != null -> w.customColor
                                else -> tier3Style.fillColor
                            }
                            val wordTierStyle = if (active) {
                                tier3Style.copy(
                                    fillColor = wordHex,
                                    useGradient = style.gradient,
                                    outerGlowRadius = 4.0,
                                    innerGlowRadius = max(2.4, tier3Style.innerGlowRadius * 1.35)
                                )
                            } else {
                                tier3Style.copy(fillColor = wordHex)
                            }
                            val shapedWord = RtlTextShaper.shapeLineForRtlDisplay(
                                applyTextTransform(w.text, style.textTransform),
                                forceRtl = isRtl
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .scale(if (active) 1.12f else 1.0f)
                            ) {
                                CaptionTierLayeredText(
                                    text = shapedWord,
                                    tierStyle = wordTierStyle,
                                    parentStyle = style,
                                    fontFamily = tier3FontFamily,
                                    fontWeight = if (active) FontWeight.Black else resolveComposeFontWeight(tier3Style.fontWeight),
                                    fontSize = tier3Size,
                                    isRtl = isRtl
                                )
                            }
                        }
                    }
                } else {
                    val shapedT3 = RtlTextShaper.shapeLineForRtlDisplay(
                        applyTextTransform(tier3Raw, style.textTransform),
                        forceRtl = isRtl
                    )
                    CaptionTierLayeredText(
                        text = shapedT3,
                        tierStyle = tier3Style,
                        parentStyle = style,
                        fontFamily = tier3FontFamily,
                        fontWeight = resolveComposeFontWeight(tier3Style.fontWeight),
                        fontSize = tier3Size,
                        isRtl = isRtl
                    )
                }
            }
        }
    }
}

@Composable
private fun OpticalPurpleLensFlareCanvas(
    intensity: Float,
    sweepOffsetX: Float = 0f,
    modifier: Modifier = Modifier
) {
    val alpha = intensity.coerceIn(0f, 1f)
    if (alpha <= 0.02f) return
    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f + sweepOffsetX
        val cy = size.height * 0.55f
        val radius = size.width * 0.48f

        // 1. Purple-magenta radial optical bloom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFF0ABFC).copy(alpha = alpha * 0.85f),
                    Color(0xFFD946EF).copy(alpha = alpha * 0.55f),
                    Color(0xFF7C3AED).copy(alpha = alpha * 0.25f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = radius.coerceAtLeast(10f)
            ),
            radius = radius.coerceAtLeast(10f),
            center = Offset(cx, cy)
        )

        // 2. Horizontal anamorphic violet-white light streak
        val streakW = size.width * 0.95f
        val streakH = 7.dp.toPx()
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFE879F9).copy(alpha = alpha * 0.8f),
                    Color.White.copy(alpha = (alpha * 0.95f).coerceAtMost(1f)),
                    Color(0xFFE879F9).copy(alpha = alpha * 0.8f),
                    Color.Transparent
                ),
                startX = cx - streakW * 0.5f,
                endX = cx + streakW * 0.5f
            ),
            topLeft = Offset(cx - streakW * 0.5f, cy - streakH * 0.5f),
            size = Size(streakW, streakH)
        )
    }
}

/**
 * 5-Layer Per-Component Persian Cinema Typography Renderer (`CaptionTierLayeredText`):
 * 1. Distinct Drop-Shadow Pass (`dropShadowDepth`, `dropShadowBlur`, `dropShadowColor`)
 * 2. 3D Sculptural Extrusion Stack (`extrusion3D`, `extrusionColor`)
 * 3. Distinct Round-Join Vector Stroke Pass (`strokeWidth`, `strokeColor`)
 * 4. Primary Fill / Vertical 3-Stop Gradient + Outer Bloom Pass
 * 5. Distinct Inner Glow / Specular Core Highlight Pass (`innerGlowRadius`, `innerGlowColor`, `innerGlowAlpha`)
 */
@Composable
private fun CaptionTierLayeredText(
    text: String,
    tierStyle: CaptionTierStyle,
    parentStyle: SubtitleStyle,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontSize: TextUnit,
    isRtl: Boolean
) {
    val isHeroTier = tierStyle.tierIndex == 2
    val textAlign = when (parentStyle.align.lowercase()) {
        "left" -> TextAlign.Left
        "right" -> TextAlign.Right
        else -> TextAlign.Center
    }
    val textDirection = if (isRtl || RtlTextShaper.containsRtl(text)) {
        TextDirection.Rtl
    } else {
        TextDirection.ContentOrLtr
    }

    val strokeColor = parseHexColor(tierStyle.strokeColor)
    val extrusionBaseColor = parseHexColor(tierStyle.extrusionColor)
    val dropShadowColor = parseHexColor(tierStyle.dropShadowColor)
    val dropShadowOffsetPx = (tierStyle.dropShadowDepth * 1.25).toFloat()
    val dropShadowBlurPx = (tierStyle.dropShadowBlur * 1.85).toFloat().coerceAtLeast(1f)

    Box(contentAlignment = Alignment.Center) {
        // Pass 1: Distinct Component Drop-Shadow Pass for instant separation over bright video regions
        if (tierStyle.dropShadowDepth > 0.1 || tierStyle.dropShadowBlur > 0.1) {
            Text(
                text = text,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontSize = fontSize,
                color = Color.Transparent,
                textAlign = textAlign,
                letterSpacing = parentStyle.letterSpacing.sp,
                lineHeight = (fontSize.value * parentStyle.lineSpacing).sp,
                style = TextStyle(
                    textDirection = textDirection,
                    shadow = Shadow(
                        color = dropShadowColor.copy(alpha = 0.88f),
                        offset = Offset(dropShadowOffsetPx * 0.45f, dropShadowOffsetPx),
                        blurRadius = dropShadowBlurPx
                    )
                )
            )
        }

        // Pass 2: 3D Extrusion Stack (deep sculptural dark-mahogany/obsidian steps)
        val depthSteps = tierStyle.extrusion3D.roundToInt().coerceIn(0, 8)
        for (step in depthSteps downTo 1) {
            Text(
                text = text,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontSize = fontSize,
                color = if (isHeroTier && step <= 2) extrusionBaseColor else strokeColor.copy(alpha = 0.96f),
                textAlign = textAlign,
                letterSpacing = parentStyle.letterSpacing.sp,
                lineHeight = (fontSize.value * parentStyle.lineSpacing).sp,
                style = TextStyle(textDirection = textDirection),
                modifier = Modifier.offset {
                    IntOffset((step * 1.55f).roundToInt(), (step * 2.35f).roundToInt())
                }
            )
        }

        // Pass 3: Distinct Component Vector Stroke Outline (Round Join & Cap for Persian dots/curves)
        if (tierStyle.strokeWidth > 0.2) {
            Text(
                text = text,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontSize = fontSize,
                color = strokeColor,
                textAlign = textAlign,
                letterSpacing = parentStyle.letterSpacing.sp,
                lineHeight = (fontSize.value * parentStyle.lineSpacing).sp,
                style = TextStyle(
                    textDirection = textDirection,
                    drawStyle = Stroke(
                        width = (tierStyle.strokeWidth * (if (isHeroTier) 2.55 else 2.05)).toFloat().coerceIn(2f, 18f),
                        join = StrokeJoin.Round,
                        cap = StrokeCap.Round
                    )
                )
            )
        }

        // Pass 4: Primary Fill (Vertical 3-Stop Gold-to-Orange Gradient or Crisp Solid Fill) + Outer Glow
        val fillColor = parseHexColor(tierStyle.fillColor)
        val outerGlowShadow = if (tierStyle.outerGlowRadius > 0.2) {
            Shadow(
                color = parseHexColor(tierStyle.outerGlowColor).copy(alpha = 0.82f),
                offset = Offset.Zero,
                blurRadius = (tierStyle.outerGlowRadius * 3.1).toFloat().coerceAtLeast(2f)
            )
        } else {
            Shadow(
                color = dropShadowColor.copy(alpha = 0.80f),
                offset = Offset(dropShadowOffsetPx * 0.35f, dropShadowOffsetPx * 0.75f),
                blurRadius = (dropShadowBlurPx * 0.7f).coerceAtLeast(2f)
            )
        }

        val fgTextStyle = if (tierStyle.useGradient) {
            val topCol = parseHexColor(tierStyle.secondaryColor)
            val midCol = fillColor
            val botCol = parseHexColor(tierStyle.tertiaryColor)
            TextStyle(
                textDirection = textDirection,
                brush = Brush.verticalGradient(
                    colors = listOf(topCol, midCol, botCol)
                ),
                shadow = outerGlowShadow
            )
        } else {
            TextStyle(
                textDirection = textDirection,
                color = fillColor,
                shadow = outerGlowShadow
            )
        }

        Text(
            text = text,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontSize = fontSize,
            textAlign = textAlign,
            letterSpacing = parentStyle.letterSpacing.sp,
            lineHeight = (fontSize.value * parentStyle.lineSpacing).sp,
            style = fgTextStyle
        )

        // Pass 5: Distinct Inner Glow / Specular Core Luminance Pass
        if (tierStyle.innerGlowRadius > 0.2 && tierStyle.innerGlowAlpha > 0.05) {
            val innerGlowCol = parseHexColor(tierStyle.innerGlowColor)
                .copy(alpha = tierStyle.innerGlowAlpha.toFloat().coerceIn(0.08f, 0.85f))
            Text(
                text = text,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontSize = fontSize,
                color = innerGlowCol.copy(alpha = (tierStyle.innerGlowAlpha.toFloat() * 0.42f).coerceIn(0.05f, 0.55f)),
                textAlign = textAlign,
                letterSpacing = parentStyle.letterSpacing.sp,
                lineHeight = (fontSize.value * parentStyle.lineSpacing).sp,
                style = TextStyle(
                    textDirection = textDirection,
                    drawStyle = Stroke(
                        width = (tierStyle.innerGlowRadius * 0.65).toFloat().coerceIn(0.8f, 3.8f),
                        join = StrokeJoin.Round,
                        cap = StrokeCap.Round
                    ),
                    shadow = Shadow(
                        color = innerGlowCol,
                        offset = Offset(0f, -0.8f),
                        blurRadius = (tierStyle.innerGlowRadius * 1.9).toFloat().coerceAtLeast(1.5f)
                    )
                )
            )
        }
    }
}

/**
 * General single-tier / word-chip layered text renderer delegating to `CaptionTierLayeredText`.
 */
@Composable
private fun StyledLayeredText(
    text: String,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontSize: TextUnit,
    fillColor: Color,
    style: SubtitleStyle,
    useGradient: Boolean,
    isHeroTier: Boolean = false,
    isRtl: Boolean = isRtlScript(text)
) {
    val hexFill = String.format(
        Locale.US,
        "#%02X%02X%02X",
        (fillColor.red * 255).roundToInt().coerceIn(0, 255),
        (fillColor.green * 255).roundToInt().coerceIn(0, 255),
        (fillColor.blue * 255).roundToInt().coerceIn(0, 255)
    )
    val tierStyle = CaptionTierStyle(
        tierIndex = if (isHeroTier) 2 else 1,
        fontFamily = style.font,
        fontWeight = style.fontWeight,
        fontScale = 1.0,
        fillColor = hexFill,
        secondaryColor = style.secondaryColor,
        tertiaryColor = style.tertiaryColor,
        useGradient = useGradient,
        strokeWidth = style.outline,
        strokeColor = style.outlineColor,
        dropShadowDepth = style.shadowDepth,
        dropShadowBlur = style.shadowBlur,
        dropShadowColor = style.shadowColor,
        extrusion3D = style.extrusion3D,
        extrusionColor = if (isHeroTier) "#3B1104" else "#110C1D",
        outerGlowRadius = style.glow,
        outerGlowColor = style.glowColor,
        innerGlowRadius = if (isHeroTier) style.headlineInnerGlow else style.subHeadlineInnerGlow * 0.6,
        innerGlowColor = if (isHeroTier) style.headlineInnerGlowColor else style.subHeadlineInnerGlowColor,
        innerGlowAlpha = if (isHeroTier) 0.55 else 0.28
    )
    CaptionTierLayeredText(
        text = text,
        tierStyle = tierStyle,
        parentStyle = style,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontSize = fontSize,
        isRtl = isRtl
    )
}

private data class InterpolatedTransform(
    val x: Double,
    val y: Double,
    val scale: Double,
    val rotation: Double,
    val opacity: Double
)

private fun interpolateKeyframes(
    keyframes: List<OverlayKeyframe>,
    relTime: Double,
    defaultX: Double,
    defaultY: Double,
    defaultScale: Double,
    defaultRot: Double,
    defaultOpacity: Double
): InterpolatedTransform {
    if (keyframes.isEmpty()) {
        return InterpolatedTransform(defaultX, defaultY, defaultScale, defaultRot, defaultOpacity)
    }
    val sorted = keyframes.sortedBy { it.time }
    val prev = sorted.lastOrNull { it.time <= relTime } ?: sorted.first()
    val next = sorted.firstOrNull { it.time >= relTime } ?: sorted.last()
    if (prev == next || abs(next.time - prev.time) < 0.001) {
        return InterpolatedTransform(prev.xPos, prev.yPos, prev.scale, prev.rotation, prev.opacity)
    }
    val rawT = ((relTime - prev.time) / (next.time - prev.time)).coerceIn(0.0, 1.0)
    val t = when (next.easing) {
        "easeInOut" -> rawT * rawT * (3.0 - 2.0 * rawT)
        "easeOut" -> 1.0 - (1.0 - rawT) * (1.0 - rawT)
        else -> rawT
    }
    return InterpolatedTransform(
        x = prev.xPos + (next.xPos - prev.xPos) * t,
        y = prev.yPos + (next.yPos - prev.yPos) * t,
        scale = prev.scale + (next.scale - prev.scale) * t,
        rotation = prev.rotation + (next.rotation - prev.rotation) * t,
        opacity = prev.opacity + (next.opacity - prev.opacity) * t
    )
}

private fun applyTextTransform(input: String, mode: String): String {
    return when (mode.lowercase()) {
        "uppercase" -> input.uppercase(Locale.getDefault())
        "lowercase" -> input.lowercase(Locale.getDefault())
        "title case" -> input.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        else -> input
    }
}

@Composable
private fun GraphicOverlayItem(
    overlay: GraphicOverlay,
    currentTime: Double,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDragPosition: ((String, Double, Double) -> Unit)?
) {
    val relTime = (currentTime - overlay.start).coerceAtLeast(0.0)
    val kf = interpolateKeyframes(
        keyframes = overlay.keyframes,
        relTime = relTime,
        defaultX = overlay.xPos,
        defaultY = overlay.yPos,
        defaultScale = overlay.scale,
        defaultRot = overlay.rotation,
        defaultOpacity = overlay.opacity
    )

    var dragX by remember(overlay.id, kf.x) { mutableFloatStateOf(kf.x.toFloat()) }
    var dragY by remember(overlay.id, kf.y) { mutableFloatStateOf(kf.y.toFloat()) }

    val baseColor = parseHexColor(overlay.color)
    val pulseScale = if (overlay.animation.equals("Pulse", ignoreCase = true)) {
        (kf.scale * (1.0 + 0.06 * sin(currentTime * 6.0))).toFloat()
    } else {
        kf.scale.toFloat()
    }

    val xBias = ((dragX.coerceIn(0.08f, 0.92f) - 0.5f) * 2f)
    val yBias = ((dragY.coerceIn(0.08f, 0.92f) - 0.5f) * 2f)
    val density = LocalDensity.current
    val offsetX = with(density) { (canvasWidthPx * xBias * 0.36f).toDp() }
    val offsetY = with(density) { (canvasHeightPx * yBias * 0.38f).toDp() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (overlay.type == "light_leak") {
            val dur = (overlay.end - overlay.start).coerceAtLeast(0.2)
            val norm = ((currentTime - overlay.start) / dur).coerceIn(0.0, 1.0).toFloat()
            val env = sin(norm * PI.toFloat()).coerceIn(0f, 1f)
            OpticalPurpleLensFlareCanvas(
                intensity = env * kf.opacity.toFloat(),
                sweepOffsetX = (norm - 0.5f) * 120f,
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .width(260.dp)
                    .height(64.dp)
                    .clickable { onClick() }
            )
        } else {
            Surface(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .scale(pulseScale.coerceIn(0.4f, 2.2f))
                    .rotate(kf.rotation.toFloat())
                    .alpha(kf.opacity.toFloat().coerceIn(0.15f, 1.0f))
                    .pointerInput(overlay.id, canvasWidthPx, canvasHeightPx) {
                        detectDragGestures(
                            onDragEnd = {
                                onDragPosition?.invoke(
                                    overlay.id,
                                    dragX.toDouble().coerceIn(0.08, 0.92),
                                    dragY.toDouble().coerceIn(0.08, 0.92)
                                )
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            dragX = (dragX + dragAmount.x / canvasWidthPx).coerceIn(0.08f, 0.92f)
                            dragY = (dragY + dragAmount.y / canvasHeightPx).coerceIn(0.08f, 0.92f)
                        }
                    }
                    .clickable { onClick() },
                shape = RoundedCornerShape(12.dp),
                color = when (overlay.type) {
                    "metallic_banner" -> Color(0xFF1E293B).copy(alpha = 0.92f)
                    "persian_orange_headline" -> Color(0xFF1A1025).copy(alpha = 0.92f)
                    else -> Color.Black.copy(alpha = if (overlay.isBackground) 0.38f else 0.72f)
                },
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.5.dp,
                    color = if (isSelected) Color.White else baseColor
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(baseColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = overlay.content.ifBlank { overlay.name },
                        color = if (overlay.type == "persian_orange_headline") baseColor else Color.White,
                        fontFamily = resolveSubtitleFontFamily(
                            if (isRtlScript(overlay.content)) "Vazirmatn" else "Cinzel"
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (overlay.type == "metallic_banner" || overlay.type == "persian_orange_headline") 17.sp else 13.sp,
                        style = TextStyle(
                            shadow = Shadow(
                                color = baseColor,
                                offset = Offset(1f, 1f),
                                blurRadius = (overlay.glow.toFloat() * 2f).coerceAtLeast(4f)
                            )
                        )
                    )
                }
            }
        }
    }
}

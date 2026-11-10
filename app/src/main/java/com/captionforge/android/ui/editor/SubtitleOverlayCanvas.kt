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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.GraphicOverlay
import com.captionforge.android.model.OverlayKeyframe
import com.captionforge.android.model.Project
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
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
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

        // 2. Active Subtitle Tracks (Up to 5 simultaneous tracks)
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
    val rtl = isRtlScript(cue.text) || track.language in listOf("fa", "ps", "ur", "ar")

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
                        fontFamily = fontFamily,
                        fontWeight = fontWeight,
                        baseFontSize = scaledFontSize,
                        currentTime = currentTime
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
                            val wordText = applyTextTransform(word.text, style.textTransform)
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
                                    useGradient = isActiveWord && style.gradient
                                )
                            }
                        }
                    }
                } else {
                    val displayText = if (style.animation.equals("Typewriter", ignoreCase = true)) {
                        val totalProgress = ((currentTime - cue.start) / cue.duration).coerceIn(0.05, 1.0)
                        val count = (cue.text.length * totalProgress).toInt().coerceAtLeast(1)
                        applyTextTransform(cue.text.take(count), style.textTransform)
                    } else {
                        applyTextTransform(cue.text, style.textTransform)
                    }

                    StyledLayeredText(
                        text = displayText,
                        fontFamily = fontFamily,
                        fontWeight = fontWeight,
                        fontSize = scaledFontSize,
                        fillColor = parseHexColor(style.textColor),
                        style = style,
                        useGradient = style.gradient
                    )
                }
            }
        }
    }
}

/**
 * Recreates Reference Video 1's signature 3-Tier Persian Orange Kinetic Typography:
 * - Tier 1 (Top): Crisp white supporting line (e.g., "ویزای تحصیلی" / "ویزه خانم")
 * - Tier 2 (Center Hero): Massive (2.0x) orange-gold gradient Persian display headline
 *   ("فنلاند" / "نازیــــه") with deep 3D dark extrusion stack, heavy stroke, warm bloom,
 *   and sweeping Optical Purple Anamorphic Lens Flare
 * - Tier 3 (Bottom): Crisp white supporting line tucked closely below Tier 2 ("بدون مدرک زبان" / "است")
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThreeTierKineticSubtitleContent(
    cue: CaptionCue,
    style: SubtitleStyle,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    baseFontSize: TextUnit,
    currentTime: Double
) {
    val (tier1, tier2, tier3) = cue.resolveThreeTiers()
    val tier1Size = (baseFontSize.value * 0.88f).sp
    val tier2HeroSize = (baseFontSize.value * 1.96f).sp
    val tier3Size = (baseFontSize.value * 0.92f).sp

    val heroFontFamily = if (style.font.equals("Vazirmatn", ignoreCase = true) || style.font.equals("Lalezar", ignoreCase = true)) {
        LalezarFontFamily
    } else {
        fontFamily
    }

    val relProgress = ((currentTime - cue.start) / cue.duration).coerceIn(0.0, 1.0).toFloat()
    val activeHeroWord = cue.words.firstOrNull { w ->
        (w.tier == 2 || w.text == tier2) && currentTime >= w.start && currentTime <= w.end
    }
    val heroPulse = if (activeHeroWord != null) {
        val wProg = ((currentTime - activeHeroWord.start) / (activeHeroWord.end - activeHeroWord.start).coerceAtLeast(0.15)).coerceIn(0.0, 1.0).toFloat()
        1.0f + 0.12f * sin(wProg * PI.toFloat())
    } else {
        1.0f + 0.05f * sin(relProgress * PI.toFloat())
    }

    Box(contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((-6).dp),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            // Tier 1: Top White Supporting Line
            if (tier1.isNotBlank()) {
                StyledLayeredText(
                    text = applyTextTransform(tier1, style.textTransform),
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = tier1Size,
                    fillColor = parseHexColor(style.tier1Color),
                    style = style.copy(
                        extrusion3D = 2.0,
                        outline = max(3.2, style.outline * 0.85),
                        glow = 0.0
                    ),
                    useGradient = false,
                    isHeroTier = false
                )
            }

            // Tier 2: Massive Hero Orange-Gold 3D Headline + Optical Purple Lens Flare
            if (tier2.isNotBlank()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.scale(heroPulse)
                ) {
                    StyledLayeredText(
                        text = applyTextTransform(tier2, style.textTransform),
                        fontFamily = heroFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = tier2HeroSize,
                        fillColor = parseHexColor(style.tier2Color),
                        style = style.copy(
                            textColor = style.tier2Color,
                            extrusion3D = max(5.5, style.extrusion3D),
                            outline = max(4.5, style.outline),
                            glow = max(7.0, style.glow)
                        ),
                        useGradient = true,
                        isHeroTier = true
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

            // Tier 3: Bottom Crisp White Supporting Line (with active word highlight)
            if (tier3.isNotBlank()) {
                val tier3Words = cue.words.filter { it.tier == 3 }
                if (tier3Words.isNotEmpty() && style.karaoke) {
                    FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.Center
                    ) {
                        tier3Words.forEach { w ->
                            val active = currentTime >= w.start && currentTime <= w.end
                            val c = when {
                                active -> parseHexColor(w.customColor ?: "#FFB800")
                                w.customColor != null -> parseHexColor(w.customColor)
                                else -> parseHexColor(style.tier3Color)
                            }
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .scale(if (active) 1.12f else 1.0f)
                            ) {
                                StyledLayeredText(
                                    text = applyTextTransform(w.text, style.textTransform),
                                    fontFamily = fontFamily,
                                    fontWeight = if (active) FontWeight.Black else FontWeight.ExtraBold,
                                    fontSize = tier3Size,
                                    fillColor = c,
                                    style = style.copy(
                                        extrusion3D = 2.0,
                                        outline = max(3.2, style.outline * 0.85),
                                        glow = if (active) 4.0 else 0.0
                                    ),
                                    useGradient = active && style.gradient,
                                    isHeroTier = false
                                )
                            }
                        }
                    }
                } else {
                    StyledLayeredText(
                        text = applyTextTransform(tier3, style.textTransform),
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = tier3Size,
                        fillColor = parseHexColor(style.tier3Color),
                        style = style.copy(
                            extrusion3D = 2.0,
                            outline = max(3.2, style.outline * 0.85),
                            glow = 0.0
                        ),
                        useGradient = false,
                        isHeroTier = false
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
 * Renders multi-layer studio typography with:
 * 1. Soft dark ambient drop shadow for instant legibility over bright footage
 * 2. 3D Extrusion depth stack (`extrusion3D`)
 * 3. True vector stroke outline (`outline` + `outlineColor`)
 * 4. Multi-stop vertical orange-gold gradient fill (`secondaryColor` top -> `textColor` mid -> `tertiaryColor` bottom)
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
    isHeroTier: Boolean = false
) {
    val textAlign = when (style.align.lowercase()) {
        "left" -> TextAlign.Left
        "right" -> TextAlign.Right
        else -> TextAlign.Center
    }
    val outlineColor = parseHexColor(style.outlineColor)
    val shadowColor = if (style.glow > 0.2) {
        parseHexColor(style.glowColor)
    } else {
        parseHexColor(style.shadowColor)
    }
    val shadowBlurPx = if (style.glow > 0.2) {
        (style.glow * 3.0).toFloat()
    } else {
        (style.shadowBlur * 1.8).toFloat()
    }
    val shadowOffsetPx = (style.shadowDepth * 1.2).toFloat()

    Box(contentAlignment = Alignment.Center) {
        // Layer 1: 3D Extrusion Stack (deep sculptural dark-brown/black shadow steps)
        val depthSteps = style.extrusion3D.roundToInt().coerceIn(0, 8)
        for (step in depthSteps downTo 1) {
            Text(
                text = text,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontSize = fontSize,
                color = if (isHeroTier && step <= 2) Color(0xFF441404) else outlineColor.copy(alpha = 0.96f),
                textAlign = textAlign,
                letterSpacing = style.letterSpacing.sp,
                lineHeight = (fontSize.value * style.lineSpacing).sp,
                modifier = Modifier.offset {
                    IntOffset((step * 1.6f).roundToInt(), (step * 2.4f).roundToInt())
                }
            )
        }

        // Layer 2: Crisp Vector Stroke Outline
        if (style.outline > 0.2) {
            Text(
                text = text,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontSize = fontSize,
                color = outlineColor,
                textAlign = textAlign,
                letterSpacing = style.letterSpacing.sp,
                lineHeight = (fontSize.value * style.lineSpacing).sp,
                style = TextStyle.Default.copy(
                    drawStyle = Stroke(
                        width = (style.outline * (if (isHeroTier) 2.6 else 2.1)).toFloat().coerceIn(2f, 16f)
                    )
                )
            )
        }

        // Layer 3: Foreground Vertical Orange-Gold Gradient or Solid Fill + Glow/Shadow
        val fgTextStyle = if (useGradient) {
            val topCol = if (isHeroTier) parseHexColor(style.secondaryColor) else parseHexColor(style.textColor)
            val midCol = parseHexColor(style.textColor)
            val botCol = parseHexColor(style.tertiaryColor)
            TextStyle(
                brush = Brush.verticalGradient(
                    colors = listOf(topCol, midCol, botCol)
                ),
                shadow = Shadow(
                    color = shadowColor,
                    offset = Offset(shadowOffsetPx, shadowOffsetPx),
                    blurRadius = shadowBlurPx.coerceAtLeast(1f)
                )
            )
        } else {
            TextStyle(
                color = fillColor,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(2f, 4f),
                    blurRadius = 8f
                )
            )
        }

        Text(
            text = text,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontSize = fontSize,
            textAlign = textAlign,
            letterSpacing = style.letterSpacing.sp,
            lineHeight = (fontSize.value * style.lineSpacing).sp,
            style = fgTextStyle
        )
    }
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

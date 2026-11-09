package com.captionforge.android.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.GraphicOverlay
import com.captionforge.android.model.OverlayKeyframe
import com.captionforge.android.model.Track
import com.captionforge.android.ui.theme.BebasNeueFontFamily
import com.captionforge.android.ui.theme.VazirmatnFontFamily
import com.captionforge.android.ui.theme.isRtlScript
import com.captionforge.android.ui.theme.parseHexColor
import com.captionforge.android.ui.theme.resolveSubtitleFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SubtitleOverlayCanvas(
    tracks: List<Track>,
    overlays: List<GraphicOverlay>,
    currentTime: Double,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val canvasWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val canvasHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val canvasWidthDp = maxWidth
        val canvasHeightDp = maxHeight

        // 1. Depth Layer 0: Background Overlays occluded behind speaker
        overlays.filter { ov ->
            ov.visible &&
                (ov.isBackground || ov.depthLayer == 0 || ov.type == "soft_bg_text") &&
                currentTime >= ov.start && currentTime <= ov.end
        }.forEach { ov ->
            RenderGraphicOverlayItem(
                ov = ov,
                currentTime = currentTime,
                canvasWidthDp = canvasWidthDp.value,
                canvasHeightDp = canvasHeightDp.value
            )
        }

        // 2. Visible Subtitle Tracks
        tracks.filter { it.visible && it.captions.isNotEmpty() }.forEach { track ->
            val style = track.style
            val adjustedTime = currentTime + style.early
            val activeCue = track.captions.firstOrNull { cue ->
                adjustedTime >= cue.start && adjustedTime <= cue.end
            }

            if (activeCue != null && activeCue.text.isNotBlank()) {
                val elapsed = (adjustedTime - activeCue.start).coerceAtLeast(0.0)
                val rtl = isRtlScript(activeCue.text)

                var scale = 1f
                var alpha = (style.opacity.coerceIn(0, 100) / 100f)
                var translateYDp = 0f

                when (style.animation) {
                    "Pop" -> {
                        if (elapsed < 0.12) {
                            scale = (0.75 + (elapsed / 0.12) * 0.40).toFloat()
                        } else if (elapsed < 0.22) {
                            scale = (1.15 - ((elapsed - 0.12) / 0.10) * 0.15).toFloat()
                        }
                    }
                    "Bounce" -> {
                        if (elapsed < 0.10) {
                            translateYDp = (-15.0 * (1.0 - elapsed / 0.10)).toFloat()
                            scale = (0.85 + (elapsed / 0.10) * 0.35).toFloat()
                        } else if (elapsed < 0.24) {
                            translateYDp = (5.0 * (1.0 - (elapsed - 0.10) / 0.14)).toFloat()
                            scale = (1.20 - ((elapsed - 0.10) / 0.14) * 0.20).toFloat()
                        }
                    }
                    "Slide up" -> {
                        if (elapsed < 0.18) {
                            translateYDp = (24.0 * (1.0 - elapsed / 0.18)).toFloat()
                        }
                    }
                    "Slide down" -> {
                        if (elapsed < 0.18) {
                            translateYDp = (-24.0 * (1.0 - elapsed / 0.18)).toFloat()
                        }
                    }
                    "Zoom" -> {
                        if (elapsed < 0.20) {
                            scale = (0.35 + (elapsed / 0.20) * 0.65).toFloat()
                        }
                    }
                    "Fade" -> {
                        if (elapsed < 0.15) {
                            alpha *= (elapsed / 0.15).toFloat().coerceIn(0f, 1f)
                        } else if (activeCue.end - adjustedTime < 0.15) {
                            alpha *= ((activeCue.end - adjustedTime) / 0.15).toFloat().coerceIn(0f, 1f)
                        }
                    }
                }

                val textColor = parseHexColor(style.textColor)
                val bgColor = parseHexColor(style.bgColor, 85)
                val scaledFontSp = (style.fontSize * (canvasWidthDp.value / 340.0)).coerceIn(12.0, 44.0).sp
                val fontFamily = resolveSubtitleFontFamily(style.font)

                val horizontalAlignment = when (style.align) {
                    "left" -> Alignment.CenterStart
                    "right" -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
                val textAlign = when (style.align) {
                    "left" -> TextAlign.Left
                    "right" -> TextAlign.Right
                    else -> TextAlign.Center
                }

                val containerBg = when {
                    style.overlay == "Solid" -> Color(0xEE0D0B14)
                    style.overlay == "Translucent" -> Color(0x88000000)
                    style.bgEnabled -> bgColor
                    else -> Color.Transparent
                }

                val yOffsetDp = (style.yPos.coerceIn(0.08, 0.92) * canvasHeightDp.value + translateYDp).dp
                val displayText = if (style.oneLine) activeCue.text.replace("\n", " ") else activeCue.text

                val shadowColor = if (style.glow > 0.1) {
                    parseHexColor(style.glowColor)
                } else {
                    Color.Black.copy(alpha = 0.9f)
                }
                val shadowBlur = if (style.glow > 0.1) {
                    (style.glow * 3f).toFloat()
                } else {
                    max(2f, style.outline.toFloat() * 2.5f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = yOffsetDp)
                        .padding(horizontal = 16.dp)
                        .scale(scale)
                        .alpha(alpha.coerceIn(0f, 1f)),
                    contentAlignment = horizontalAlignment
                ) {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(containerBg)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (style.karaoke && activeCue.words.isNotEmpty()) {
                                KaraokeCueText(
                                    cue = activeCue,
                                    adjustedTime = adjustedTime,
                                    baseColor = textColor,
                                    fontSizeSp = scaledFontSp.value,
                                    fontFamily = fontFamily,
                                    textAlign = textAlign,
                                    oneLine = style.oneLine,
                                    letterSpacing = style.letterSpacing,
                                    shadowColor = shadowColor,
                                    shadowBlur = shadowBlur
                                )
                            } else {
                                val textBrush = if (style.gradient) {
                                    Brush.verticalGradient(
                                        listOf(textColor, parseHexColor(style.secondaryColor))
                                    )
                                } else null

                                Text(
                                    text = displayText,
                                    textAlign = textAlign,
                                    maxLines = if (style.oneLine) 1 else 4,
                                    overflow = TextOverflow.Ellipsis,
                                    style = if (textBrush != null) {
                                        TextStyle(
                                            brush = textBrush,
                                            fontFamily = fontFamily,
                                            fontSize = scaledFontSp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = style.letterSpacing.sp,
                                            shadow = Shadow(
                                                color = shadowColor,
                                                offset = Offset(2f, 2f),
                                                blurRadius = shadowBlur
                                            )
                                        )
                                    } else {
                                        TextStyle(
                                            color = textColor,
                                            fontFamily = fontFamily,
                                            fontSize = scaledFontSp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = style.letterSpacing.sp,
                                            shadow = Shadow(
                                                color = shadowColor,
                                                offset = Offset(2f, 2f),
                                                blurRadius = shadowBlur
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Foreground Graphic Overlays (Layers 1..3)
        overlays.filter { ov ->
            ov.visible &&
                !(ov.isBackground || ov.depthLayer == 0 || ov.type == "soft_bg_text") &&
                currentTime >= ov.start && currentTime <= ov.end
        }.forEach { ov ->
            RenderGraphicOverlayItem(
                ov = ov,
                currentTime = currentTime,
                canvasWidthDp = canvasWidthDp.value,
                canvasHeightDp = canvasHeightDp.value
            )
        }
    }
}

@Composable
private fun KaraokeCueText(
    cue: CaptionCue,
    adjustedTime: Double,
    baseColor: Color,
    fontSizeSp: Float,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    textAlign: TextAlign,
    oneLine: Boolean,
    letterSpacing: Double,
    shadowColor: Color,
    shadowBlur: Float
) {
    val annotated = buildAnnotatedString {
        cue.words.forEachIndexed { idx, word ->
            val isActive = adjustedTime >= word.start && adjustedTime <= word.end
            val isPast = adjustedTime > word.end
            val wordColor = when {
                isActive -> Color(0xFFFACC15)
                isPast -> baseColor
                else -> baseColor.copy(alpha = 0.65f)
            }
            withStyle(
                SpanStyle(
                    color = wordColor,
                    fontFamily = fontFamily,
                    fontSize = (if (isActive) fontSizeSp * 1.1f else fontSizeSp).sp,
                    fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                    letterSpacing = letterSpacing.sp,
                    shadow = Shadow(
                        color = shadowColor,
                        offset = Offset(1.5f, 1.5f),
                        blurRadius = if (isActive) shadowBlur * 1.3f else shadowBlur
                    )
                )
            ) {
                append(word.text)
                if (idx < cue.words.lastIndex) append(" ")
            }
        }
    }
    Text(
        text = annotated,
        textAlign = textAlign,
        maxLines = if (oneLine) 1 else 4,
        overflow = TextOverflow.Ellipsis
    )
}

private fun evaluateEasing(rawT: Double, easing: String): Double {
    val t = rawT.coerceIn(0.0, 1.0)
    return when (easing) {
        "linear" -> t
        "easeIn" -> t * t
        "easeOut" -> t * (2.0 - t)
        "easeInOut" -> 0.5 * (1.0 - cos(PI * t))
        "bounce" -> {
            when {
                t < 1.0 / 2.75 -> 7.5625 * t * t
                t < 2.0 / 2.75 -> {
                    val u = t - 1.5 / 2.75
                    7.5625 * u * u + 0.75
                }
                t < 2.5 / 2.75 -> {
                    val u = t - 2.25 / 2.75
                    7.5625 * u * u + 0.9375
                }
                else -> {
                    val u = t - 2.625 / 2.75
                    7.5625 * u * u + 0.984375
                }
            }
        }
        else -> t
    }
}

@Composable
private fun RenderGraphicOverlayItem(
    ov: GraphicOverlay,
    currentTime: Double,
    canvasWidthDp: Float,
    canvasHeightDp: Float
) {
    val relTime = (currentTime - ov.start).coerceIn(0.0, ov.duration)
    var curX = ov.xPos
    var curY = ov.yPos
    var curScale = ov.scale
    var curOpacity = ov.opacity
    var curRotation = ov.rotation
    var curBlur = ov.blur
    val curColor = parseHexColor(ov.color)

    if (ov.keyframes.size >= 2) {
        val sorted: List<OverlayKeyframe> = ov.keyframes.sortedBy { it.time }
        when {
            relTime <= sorted.first().time -> {
                val k = sorted.first()
                curX = k.xPos; curY = k.yPos; curScale = k.scale; curOpacity = k.opacity; curRotation = k.rotation; curBlur = k.blur
            }
            relTime >= sorted.last().time -> {
                val k = sorted.last()
                curX = k.xPos; curY = k.yPos; curScale = k.scale; curOpacity = k.opacity; curRotation = k.rotation; curBlur = k.blur
            }
            else -> {
                var k1 = sorted.first()
                var k2 = sorted.last()
                for (i in 0 until sorted.size - 1) {
                    if (relTime >= sorted[i].time && relTime <= sorted[i + 1].time) {
                        k1 = sorted[i]
                        k2 = sorted[i + 1]
                        break
                    }
                }
                val span = max(0.001, k2.time - k1.time)
                val alpha = evaluateEasing((relTime - k1.time) / span, k2.easing)
                curX = k1.xPos + (k2.xPos - k1.xPos) * alpha
                curY = k1.yPos + (k2.yPos - k1.yPos) * alpha
                curScale = k1.scale + (k2.scale - k1.scale) * alpha
                curOpacity = k1.opacity + (k2.opacity - k1.opacity) * alpha
                curRotation = k1.rotation + (k2.rotation - k1.rotation) * alpha
                curBlur = k1.blur + (k2.blur - k1.blur) * alpha
            }
        }
    } else {
        when (ov.animation) {
            "Fade" -> {
                if (relTime < 0.2) curOpacity *= (relTime / 0.2)
                else if (ov.end - currentTime < 0.2) curOpacity *= ((ov.end - currentTime) / 0.2)
            }
            "Pulse" -> {
                curScale *= 1.0 + 0.07 * sin(relTime * 6.0)
            }
            "Pop" -> {
                if (relTime < 0.16) {
                    curScale *= 0.72 + (relTime / 0.16) * 0.36
                }
            }
        }
    }

    if (ov.type == "light_leak") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha((curOpacity * 0.75).toFloat().coerceIn(0f, 1f))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xD9D946EF),
                            Color(0x669333EA),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidthDp * 1.5f, canvasHeightDp * 0.4f),
                        radius = max(canvasWidthDp, canvasHeightDp) * 1.8f
                    )
                )
        )
        return
    }

    val xDp = (curX.coerceIn(0.08, 0.92) * canvasWidthDp).dp
    val yDp = (curY.coerceIn(0.08, 0.92) * canvasHeightDp).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = (xDp - 70.dp).coerceAtLeast(4.dp), y = (yDp - 24.dp).coerceAtLeast(4.dp))
                .rotate(curRotation.toFloat())
                .scale(curScale.toFloat().coerceIn(0.3f, 2.5f))
                .alpha(curOpacity.toFloat().coerceIn(0f, 1f))
                .then(if (curBlur > 0.2) Modifier.blur(curBlur.dp) else Modifier)
        ) {
            when (ov.type) {
                "persian_orange_headline", "persian_orange" -> {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xAA110D1A))
                                .border(1.5.dp, Color(0xFFFF7A00), RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "ویزای تحصیلی",
                                fontFamily = VazirmatnFontFamily,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = ov.content.ifBlank { "فنلاند" },
                                style = TextStyle(
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFFFFB800), Color(0xFFFF6A00))
                                    ),
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black,
                                    shadow = Shadow(
                                        color = Color.Black,
                                        offset = Offset(2f, 2f),
                                        blurRadius = 6f
                                    )
                                )
                            )
                        }
                    }
                }

                "glow_hologram" -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x440284C7))
                            .border(2.dp, Color(0xFF00F0FF), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationCity,
                                contentDescription = null,
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Apartment,
                                contentDescription = null,
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Text(
                            text = ov.content.ifBlank { "◈ HOLOGRAM ◈" },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                "metallic_banner" -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF0F172A), Color(0xFF334155), Color(0xFF0F172A))
                                )
                            )
                            .border(2.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                            .padding(horizontal = 22.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ov.content.ifBlank { "AUSTRALIA" }.uppercase(),
                            fontFamily = BebasNeueFontFamily,
                            color = Color(0xFFE2E8F0),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 5.sp
                        )
                    }
                }

                "flag_graphic", "waving_flag" -> {
                    val waveOffset = (sin(relTime * 5.0) * 4.0).roundToInt()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .offset { IntOffset(0, waveOffset) }
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xDD0F172A))
                            .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ov.content.ifBlank { "⚑ MALAYSIA FLAG ⚑" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                "soft_bg_text" -> {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = ov.content.ifBlank { "برج‌های دوگانه بلندترین" },
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.45f),
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = Offset(1f, 1f),
                                    blurRadius = 10f
                                )
                            )
                        )
                    }
                }

                "end_accent" -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEF4444))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = ov.content.ifBlank { "CaptionForge Studio" },
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .border(1.dp, curColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = ov.content.ifBlank { ov.name },
                            color = curColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

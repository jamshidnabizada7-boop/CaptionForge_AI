package com.captionforge.android.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.captionforge.android.model.AudioClip
import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.GraphicOverlay
import com.captionforge.android.model.Project
import com.captionforge.android.model.SrtParser
import com.captionforge.android.ui.theme.AppColors
import com.captionforge.android.ui.theme.VazirmatnFontFamily
import com.captionforge.android.ui.theme.parseHexColor
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun TimelineComposable(
    project: Project,
    currentTime: Double,
    activeTrackIndex: Int,
    selectedCueId: String?,
    selectedOverlayId: String?,
    onSeek: (Double) -> Unit,
    onSelectTrack: (Int) -> Unit,
    onSelectCue: (CaptionCue) -> Unit,
    onEditCue: (CaptionCue) -> Unit,
    onSelectOverlay: (GraphicOverlay) -> Unit,
    onAddCueAtPlayhead: () -> Unit,
    onSplitVideoClip: () -> Unit,
    onCycleClipTransition: (String) -> Unit,
    onOpenTrackManager: () -> Unit,
    onEditAudioClip: ((AudioClip) -> Unit)? = null,
    selectedWordIndex: Int = 0,
    onSelectWord: ((CaptionCue, Int) -> Unit)? = null,
    onTrimCue: ((CaptionCue, Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var dpPerSecond by remember { mutableFloatStateOf(96f) }
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()
    val wordStripScroll = rememberScrollState()
    val density = LocalDensity.current

    val totalDuration = max(1.0, project.duration)
    val timelineWidthDp = max(380f, (totalDuration * dpPerSecond).toFloat() + 120f).dp
    val pxPerSecond = with(density) { dpPerSecond.dp.toPx() }
    val hasGlobalSfx = project.effects.sfx != "None"

    val activeTrack = project.tracks.getOrNull(activeTrackIndex) ?: project.tracks.firstOrNull()
    val activeOrSelectedCue = activeTrack?.captions?.firstOrNull { it.id == selectedCueId }
        ?: activeTrack?.captions?.firstOrNull { currentTime >= it.start && currentTime <= it.end }
        ?: activeTrack?.captions?.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF08070E))
            .testTag("timeline_container")
    ) {
        // Scrollable Time Ruler + Chunky Purple Pill Tracks (Exact Magic Editor Layout)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(horizontalScroll)
        ) {
            Box(
                modifier = Modifier
                    .width(timelineWidthDp)
                    .fillMaxHeight()
                    .pointerInput(pxPerSecond, totalDuration) {
                        detectTapGestures { offset ->
                            val tappedSec = (offset.x / pxPerSecond).toDouble().coerceIn(0.0, totalDuration)
                            onSeek(tappedSec)
                        }
                    }
                    .pointerInput(pxPerSecond, totalDuration) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val draggedSec = (change.position.x / pxPerSecond).toDouble().coerceIn(0.0, totalDuration)
                            onSeek(draggedSec)
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .width(timelineWidthDp)
                        .verticalScroll(verticalScroll)
                        .padding(bottom = 6.dp)
                ) {
                    // 1. Clean Timecode Ruler (00:00   .   .   00:01   .   .   00:02)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(Color(0xFF08070E))
                    ) {
                        val totalSecs = ceil(totalDuration).toInt()
                        for (sec in 0..totalSecs) {
                            val xOffset = (sec * dpPerSecond).dp
                            val mm = sec / 60
                            val ss = sec % 60
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", mm, ss),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF7C7A92),
                                modifier = Modifier
                                    .offset(x = xOffset + 2.dp, y = 3.dp)
                            )
                        }
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val subSteps = totalSecs * 4
                            for (i in 0..subSteps) {
                                if (i % 4 != 0) {
                                    val x = (i / 4f) * pxPerSecond
                                    drawCircle(
                                        color = Color(0xFF3E3B54),
                                        radius = 1.5f,
                                        center = Offset(x, size.height * 0.48f)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Subtitle Tracks (Chunky Rounded Indigo/Purple Pills like Magic Editor)
                    val indexedTracks = project.tracks.mapIndexed { idx, tr -> idx to tr }.reversed()
                    indexedTracks.forEach { (trackIdx, track) ->
                        val isTrackSelected = trackIdx == activeTrackIndex
                        if (project.tracks.size > 1) {
                            Row(
                                modifier = Modifier
                                    .padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                                    .clickable { onSelectTrack(trackIdx) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (isTrackSelected) Color(0xFF818CF8) else Color(0xFF64708D))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${track.style.preset} - ${track.name}",
                                    fontSize = 10.sp,
                                    fontWeight = if (isTrackSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isTrackSelected) Color(0xFFD8B4FE) else Color(0xFF8E8CA6)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .padding(vertical = 2.dp)
                        ) {
                            track.captions.forEach { cue ->
                                val leftDp = (cue.start * dpPerSecond).dp
                                val widthDp = max(48.0, (cue.end - cue.start) * dpPerSecond).dp
                                val isSelected = cue.id == selectedCueId
                                val isAtPlayhead = currentTime >= cue.start && currentTime <= cue.end
                                val isHighlighted = isSelected || (isTrackSelected && isAtPlayhead)
                                val cueHasSfx = hasGlobalSfx ||
                                    cue.entranceSfx != "None" ||
                                    cue.words.any { it.sfx != "None" }

                                Box(
                                    modifier = Modifier
                                        .offset(x = leftDp)
                                        .width(widthDp)
                                        .fillMaxHeight()
                                        .padding(horizontal = 1.5.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(
                                            when {
                                                isHighlighted -> Color(0xFF4F46E5)
                                                isTrackSelected -> Color(0xFF28205C)
                                                else -> Color(0xFF1E1942)
                                            }
                                        )
                                        .border(
                                            width = if (isHighlighted) 1.5.dp else 1.dp,
                                            color = if (isHighlighted) Color(0xFFA5B4FC) else Color(0xFF3E347A),
                                            shape = RoundedCornerShape(13.dp)
                                        )
                                        .clickable {
                                            onSelectTrack(trackIdx)
                                            if (isSelected) {
                                                onEditCue(cue)
                                            } else {
                                                onSelectCue(cue)
                                            }
                                        }
                                        .testTag("timeline_cue_${cue.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Interactive Left & Right White Trim Handles when Selected/Active
                                    if (isHighlighted) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .width(12.dp)
                                                .fillMaxHeight()
                                                .background(Color(0xFF6366F1))
                                                .pointerInput(cue.id, cue.start, cue.end, pxPerSecond) {
                                                    detectHorizontalDragGestures { change, dragAmount ->
                                                        change.consume()
                                                        val deltaSec = (dragAmount / pxPerSecond).toDouble()
                                                        val newStart = ((cue.start + deltaSec).coerceIn(0.0, cue.end - 0.2) * 100.0).roundToInt() / 100.0
                                                        onTrimCue?.invoke(cue, newStart, cue.end)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(14.dp)
                                                    .clip(RoundedCornerShape(1.dp))
                                                    .background(Color.White)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .width(12.dp)
                                                .fillMaxHeight()
                                                .background(Color(0xFF6366F1))
                                                .pointerInput(cue.id, cue.start, cue.end, pxPerSecond) {
                                                    detectHorizontalDragGestures { change, dragAmount ->
                                                        change.consume()
                                                        val deltaSec = (dragAmount / pxPerSecond).toDouble()
                                                        val newEnd = ((cue.end + deltaSec).coerceIn(cue.start + 0.2, totalDuration) * 100.0).roundToInt() / 100.0
                                                        onTrimCue?.invoke(cue, cue.start, newEnd)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(14.dp)
                                                    .clip(RoundedCornerShape(1.dp))
                                                    .background(Color.White)
                                            )
                                        }
                                    }

                                    // Caption Text inside the Chunky Purple Pill
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = if (isHighlighted) 14.dp else 8.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (cueHasSfx) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF6D28D9)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VolumeUp,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = cue.text.replace("\n", " "),
                                            fontFamily = VazirmatnFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // "+ Add" pill at end of track
                            val lastEnd = track.captions.maxOfOrNull { it.end } ?: 0.0
                            Surface(
                                modifier = Modifier
                                    .offset(x = ((lastEnd + 0.15) * dpPerSecond).dp)
                                    .height(34.dp)
                                    .align(Alignment.CenterStart)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSelectTrack(trackIdx)
                                        onAddCueAtPlayhead()
                                    }
                                    .testTag("timeline_add_cue_btn"),
                                color = Color(0xFF1A1636),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Caption",
                                        tint = Color(0xFFA5B4FC),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Add",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA5B4FC)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Compact Secondary Pill Strip for VFX Overlays & Audio Clips (when present)
                    if (project.overlays.isNotEmpty() || project.audioClips.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .padding(top = 2.dp)
                        ) {
                            project.overlays.forEach { ov ->
                                if (!ov.visible) return@forEach
                                val leftDp = (ov.start * dpPerSecond).dp
                                val widthDp = max(36.0, (ov.end - ov.start) * dpPerSecond).dp
                                val isSel = ov.id == selectedOverlayId
                                Box(
                                    modifier = Modifier
                                        .offset(x = leftDp)
                                        .width(widthDp)
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) AppColors.AccentOrange else Color(0xFF3B1D5E))
                                        .border(
                                            1.dp,
                                            if (isSel) Color.White else AppColors.PrimaryPurpleLight.copy(alpha = 0.5f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onSelectOverlay(ov) }
                                        .padding(horizontal = 6.dp)
                                        .testTag("timeline_overlay_${ov.id}"),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "✦ ${ov.name}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            project.audioClips.forEach { clip ->
                                val leftDp = (clip.start * dpPerSecond).dp
                                val widthDp = max(36.0, clip.duration * dpPerSecond).dp
                                Box(
                                    modifier = Modifier
                                        .offset(x = leftDp)
                                        .width(widthDp)
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF065F46).copy(alpha = 0.85f))
                                        .border(1.dp, AppColors.AccentGreen, RoundedCornerShape(6.dp))
                                        .clickable { onEditAudioClip?.invoke(clip) }
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "♪ ${clip.name}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Crisp White Vertical Playhead Line (Exact Magic Editor Playhead)
                val playheadX = (currentTime * dpPerSecond).dp
                Box(
                    modifier = Modifier
                        .offset(x = playheadX - 1.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }
        }

        // 5. Interactive Word-Level Timeline Strip (Priority 3: Word-by-word selection, timing & styling strip)
        if (activeOrSelectedCue != null) {
            val words = remember(activeOrSelectedCue.id, activeOrSelectedCue.text, activeOrSelectedCue.words) {
                if (activeOrSelectedCue.words.isNotEmpty()) {
                    activeOrSelectedCue.words
                } else {
                    SrtParser.buildWordTimings(
                        activeOrSelectedCue.text,
                        activeOrSelectedCue.start,
                        activeOrSelectedCue.end
                    )
                }
            }
            if (words.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E0C18))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("timeline_word_strip"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WORDS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF818CF8),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(wordStripScroll),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        words.forEachIndexed { wIdx, w ->
                            val isWordSelected = activeOrSelectedCue.id == selectedCueId && wIdx == selectedWordIndex
                            val isWordSpokenNow = currentTime >= w.start && currentTime <= w.end
                            val hasCustomColor = !w.customColor.isNullOrBlank()
                            val hasWordSfx = w.sfx != "None" && w.sfx.isNotBlank()
                            val hasWordAnim = w.animation != "None" && w.animation.isNotBlank()

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelectCue(activeOrSelectedCue)
                                        onSelectWord?.invoke(activeOrSelectedCue, wIdx)
                                        onSeek(w.start)
                                    }
                                    .testTag("timeline_word_chip_$wIdx"),
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isWordSelected -> Color(0xFF635BFF)
                                    isWordSpokenNow -> Color(0xFF312E81)
                                    else -> Color(0xFF181629)
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = when {
                                        isWordSelected -> Color.White
                                        hasCustomColor -> parseHexColor(w.customColor!!)
                                        isWordSpokenNow -> Color(0xFF818CF8)
                                        else -> Color(0xFF2D2A45)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (hasCustomColor) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColor(w.customColor!!))
                                        )
                                    }
                                    Text(
                                        text = w.text,
                                        fontFamily = VazirmatnFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1fs", w.start),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    if (hasWordAnim) {
                                        Text(
                                            text = "✦",
                                            fontSize = 9.sp,
                                            color = Color(0xFFFACC15)
                                        )
                                    }
                                    if (hasWordSfx) {
                                        Text(
                                            text = "♪",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = AppColors.AccentGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectCue(activeOrSelectedCue)
                                onEditCue(activeOrSelectedCue)
                            }
                            .testTag("timeline_open_word_inspector_btn"),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF231F3A),
                        border = BorderStroke(1.dp, Color(0xFF4F46E5))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Word Inspector",
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Word",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA5B4FC)
                            )
                        }
                    }
                }
            }
        }
    }
}

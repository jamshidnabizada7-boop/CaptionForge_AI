package com.captionforge.android.ui.editor

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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.GraphicOverlay
import com.captionforge.android.model.Project
import com.captionforge.android.ui.theme.AppColors
import com.captionforge.android.ui.theme.parseHexColor
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sin

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
    modifier: Modifier = Modifier
) {
    var dpPerSecond by remember { mutableFloatStateOf(76f) }
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()
    val density = LocalDensity.current

    val totalDuration = max(2.0, project.duration)
    val totalWidthDp = (totalDuration * dpPerSecond + 160.0).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
    ) {
        // Top Timeline Header Bar (Track Tabs + Quick Add/Split + Zoom)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                project.tracks.forEachIndexed { idx, track ->
                    val isSelected = idx == activeTrackIndex
                    val trackColor = AppColors.TrackColors[idx % AppColors.TrackColors.size]
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectTrack(idx) }
                            .testTag("timeline_track_tab_$idx"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) trackColor.copy(alpha = 0.22f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) trackColor else AppColors.SurfaceBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(trackColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = track.name.ifBlank { "Track ${idx + 1}" },
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else AppColors.TextSecondary
                            )
                            if (!track.visible) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Hidden",
                                    tint = AppColors.TextMuted,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            if (track.locked) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = AppColors.AccentAmber,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenTrackManager() }
                        .testTag("manage_tracks_chip"),
                    shape = RoundedCornerShape(16.dp),
                    color = AppColors.SurfaceElevated
                ) {
                    Text(
                        text = "+ Tracks",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.PrimaryPurpleLight,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Add Caption Cue button
            Surface(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAddCueAtPlayhead() }
                    .testTag("add_cue_timeline_button"),
                color = AppColors.PrimaryPurple.copy(alpha = 0.25f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Cue",
                        tint = AppColors.PrimaryPurpleLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Cue", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Split Video Clip button
            IconButton(
                onClick = onSplitVideoClip,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("split_clip_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Split Clip at Playhead",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Zoom Out / In
            IconButton(
                onClick = { dpPerSecond = (dpPerSecond - 16f).coerceIn(40f, 220f) },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = "${dpPerSecond.toInt()}px",
                fontSize = 10.sp,
                color = AppColors.TextMuted
            )
            IconButton(
                onClick = { dpPerSecond = (dpPerSecond + 16f).coerceIn(40f, 220f) },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        HorizontalDivider(color = AppColors.SurfaceBorder)

        // Multi-Track Scrollable Scrubber Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScroll)
        ) {
            val pxPerSecond = with(density) { dpPerSecond.dp.toPx() }

            Box(
                modifier = Modifier
                    .width(totalWidthDp)
                    .fillMaxHeight()
                    .pointerInput(pxPerSecond, totalDuration) {
                        detectTapGestures { offset ->
                            val sec = (offset.x / pxPerSecond).toDouble().coerceIn(0.0, totalDuration)
                            onSeek(sec)
                        }
                    }
                    .pointerInput(pxPerSecond, totalDuration) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val sec = (change.position.x / pxPerSecond).toDouble().coerceIn(0.0, totalDuration)
                            onSeek(sec)
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(verticalScroll)
                        .padding(bottom = 12.dp)
                ) {
                    // 1. Time Ruler
                    TimeRulerRow(totalDuration = totalDuration, dpPerSecond = dpPerSecond)

                    // 2. Video Clips Track Lane
                    val clips = project.videoClips.ifEmpty {
                        listOf(
                            com.captionforge.android.model.VideoClipSegment(
                                id = "vc_default",
                                label = "Main Video",
                                sourceUri = project.source,
                                startInTimeline = project.trim.start,
                                endInTimeline = project.trim.end,
                                transition = "Cut"
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .padding(vertical = 2.dp)
                            .background(Color.White.copy(alpha = 0.03f))
                    ) {
                        clips.forEach { clip ->
                            val leftDp = (clip.startInTimeline * dpPerSecond).dp
                            val widthDp = max(28.0, clip.duration * dpPerSecond).dp
                            Row(
                                modifier = Modifier
                                    .offset(x = leftDp)
                                    .width(widthDp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF312E81).copy(alpha = 0.85f))
                                    .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(6.dp))
                                    .clickable { onCycleClipTransition(clip.id) }
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = Color(0xFFA5B4FC),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${clip.label} [${clip.transition}]",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 3. Graphic Overlays / VFX Lane (if any overlays exist)
                    if (project.overlays.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .padding(vertical = 2.dp)
                                .background(Color.White.copy(alpha = 0.04f))
                        ) {
                            project.overlays.forEach { ov ->
                                val leftDp = (ov.start * dpPerSecond).dp
                                val widthDp = max(28.0, ov.duration * dpPerSecond).dp
                                val ovColor = parseHexColor(ov.color)
                                val isSelected = ov.id == selectedOverlayId

                                Row(
                                    modifier = Modifier
                                        .offset(x = leftDp)
                                        .width(widthDp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ovColor.copy(alpha = if (ov.visible) 0.72f else 0.25f))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color.White else ovColor,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onSelectOverlay(ov) }
                                        .padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (ov.keyframes.isNotEmpty()) "${ov.name} ◆${ov.keyframes.size}" else ov.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // 4. Subtitle Tracks Lanes
                    project.tracks.forEachIndexed { trackIdx, track ->
                        val isCurrentTrack = trackIdx == activeTrackIndex
                        val trackColor = AppColors.TrackColors[trackIdx % AppColors.TrackColors.size]

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .padding(vertical = 2.dp)
                                .background(
                                    if (isCurrentTrack) AppColors.SurfaceElevated.copy(alpha = 0.7f)
                                    else Color.Transparent
                                )
                        ) {
                            track.captions.forEach { cue ->
                                val isCueSelected = cue.id == selectedCueId
                                val leftDp = (cue.start * dpPerSecond).dp
                                val widthDp = max(24.0, cue.duration * dpPerSecond).dp

                                Box(
                                    modifier = Modifier
                                        .offset(x = leftDp)
                                        .width(widthDp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(
                                            trackColor.copy(alpha = if (isCueSelected) 0.90f else 0.48f)
                                        )
                                        .border(
                                            width = if (isCueSelected) 2.dp else 1.dp,
                                            color = if (isCueSelected) Color.White else trackColor,
                                            shape = RoundedCornerShape(7.dp)
                                        )
                                        .clickable {
                                            onSelectTrack(trackIdx)
                                            if (isCueSelected) {
                                                onEditCue(cue)
                                            } else {
                                                onSelectCue(cue)
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("timeline_cue_${cue.id}"),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = cue.text,
                                        fontSize = 11.sp,
                                        fontWeight = if (isCueSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // 5. Audio / SFX & Waveform Track Lane
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .padding(vertical = 2.dp)
                            .background(AppColors.AccentGreen.copy(alpha = 0.06f))
                    ) {
                        // Procedural speech audio waveform background
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = 3.dp.toPx()
                            val gap = 3.dp.toPx()
                            val step = barWidth + gap
                            val count = (size.width / step).toInt()
                            val centerY = size.height / 2f
                            for (i in 0 until count) {
                                val sec = (i * step) / pxPerSecond
                                if (sec >= project.trim.start && sec <= project.trim.end) {
                                    val amp = (0.25f + 0.65f * kotlin.math.abs(sin(i * 0.45f))).coerceIn(0.15f, 0.9f)
                                    val halfH = (size.height * 0.42f) * amp
                                    drawLine(
                                        color = Color(0xFF10B981).copy(alpha = 0.35f),
                                        start = Offset(i * step, centerY - halfH),
                                        end = Offset(i * step, centerY + halfH),
                                        strokeWidth = barWidth
                                    )
                                }
                            }
                        }

                        project.audioClips.forEach { clip ->
                            val leftDp = (clip.start * dpPerSecond).dp
                            val widthDp = max(32.0, clip.duration * dpPerSecond).dp
                            Row(
                                modifier = Modifier
                                    .offset(x = leftDp)
                                    .width(widthDp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF059669).copy(alpha = 0.85f))
                                    .border(1.dp, Color(0xFF34D399), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = clip.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Dimmed non-active Trim Regions
                if (project.trim.start > 0.01) {
                    Box(
                        modifier = Modifier
                            .width((project.trim.start * dpPerSecond).dp)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.55f))
                    )
                }
                if (project.trim.end < totalDuration - 0.01) {
                    Box(
                        modifier = Modifier
                            .offset(x = (project.trim.end * dpPerSecond).dp)
                            .width(((totalDuration - project.trim.end) * dpPerSecond + 160.0).dp)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.55f))
                    )
                }

                // Red Playhead Indicator
                val playheadXDp = (currentTime * dpPerSecond).dp
                Box(
                    modifier = Modifier
                        .offset(x = playheadXDp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(AppColors.AccentCoral)
                )
                Box(
                    modifier = Modifier
                        .offset(x = (playheadXDp - 5.dp).coerceAtLeast(0.dp), y = 0.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(AppColors.AccentCoral)
                )
            }
        }
    }
}

@Composable
private fun TimeRulerRow(
    totalDuration: Double,
    dpPerSecond: Float
) {
    val secondCount = ceil(totalDuration).toInt() + 2
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
    ) {
        for (s in 0 until secondCount) {
            val xDp = (s * dpPerSecond).dp
            Column(
                modifier = Modifier.offset(x = xDp)
            ) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(7.dp)
                        .background(AppColors.TextMuted)
                )
                Text(
                    text = "${s}s",
                    fontSize = 9.sp,
                    color = AppColors.TextMuted
                )
            }
        }
    }
}

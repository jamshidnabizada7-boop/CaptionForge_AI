package com.captionforge.android.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.GTranslate
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.captionforge.android.model.GraphicOverlay
import com.captionforge.android.model.Project
import com.captionforge.android.model.SubtitleStyle
import com.captionforge.android.model.Track
import com.captionforge.android.model.VideoEffects
import com.captionforge.android.ui.theme.AppColors
import com.captionforge.android.ui.theme.parseHexColor
import com.captionforge.android.ui.theme.resolveSubtitleFontFamily
import com.captionforge.android.viewmodel.EditorTool
import java.util.Locale
import kotlin.math.max

@Composable
fun ToolsTraysComposable(
    project: Project,
    activeTool: EditorTool,
    currentTrack: Track,
    effects: VideoEffects,
    onSelectTool: (EditorTool) -> Unit,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onUpdateEffects: ((VideoEffects) -> VideoEffects) -> Unit,
    onApplyTemplate: (String) -> Unit,
    onAddOverlayModal: () -> Unit,
    onEditOverlay: (GraphicOverlay) -> Unit,
    onToggleOverlayVisibility: (String) -> Unit,
    onDeleteOverlay: (String) -> Unit,
    onTrackOverlayMotion: (GraphicOverlay) -> Unit,
    onOpenTranslateModal: () -> Unit,
    onImportSrt: () -> Unit,
    onExportSrt: () -> Unit,
    onImportCustomFont: () -> Unit,
    onAuditionSfx: (String) -> Unit,
    onAddSfxClipAtPlayhead: (String) -> Unit,
    onUpdateTrim: (Double, Double) -> Unit,
    onSplitClipAtPlayhead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = currentTrack.style

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
    ) {
        HorizontalDivider(color = AppColors.SurfaceBorder)

        // Active Tool Content Tray
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
        ) {
            when (activeTool) {
                EditorTool.STYLES -> StylesTray(style = style, onUpdateStyle = onUpdateStyle)
                EditorTool.TEMPLATES -> TemplatesTray(onApplyTemplate = onApplyTemplate)
                EditorTool.OVERLAYS -> OverlaysTray(
                    overlays = project.overlays,
                    onAddOverlay = onAddOverlayModal,
                    onEditOverlay = onEditOverlay,
                    onToggleVisibility = onToggleOverlayVisibility,
                    onDeleteOverlay = onDeleteOverlay,
                    onTrackMotion = onTrackOverlayMotion
                )
                EditorTool.CLIPS -> ClipsAndTrimTray(
                    project = project,
                    onUpdateTrim = onUpdateTrim,
                    onSplitClip = onSplitClipAtPlayhead
                )
                EditorTool.FORMAT -> FormatTray(style = style, onUpdateStyle = onUpdateStyle)
                EditorTool.FONTS -> FontsTray(
                    style = style,
                    customFonts = project.customFonts,
                    onUpdateStyle = onUpdateStyle,
                    onImportCustomFont = onImportCustomFont
                )
                EditorTool.COLORS -> ColorsTray(style = style, onUpdateStyle = onUpdateStyle)
                EditorTool.ANIMATIONS -> AnimationsTray(style = style, onUpdateStyle = onUpdateStyle)
                EditorTool.TRANSLATE -> TranslateAndSrtTray(
                    onOpenTranslateModal = onOpenTranslateModal,
                    onImportSrt = onImportSrt,
                    onExportSrt = onExportSrt
                )
                EditorTool.SFX -> SfxTray(
                    effects = effects,
                    onAuditionSfx = onAuditionSfx,
                    onUpdateEffects = onUpdateEffects,
                    onAddClipAtPlayhead = onAddSfxClipAtPlayhead
                )
                EditorTool.TIMING -> TimingTray(style = style, onUpdateStyle = onUpdateStyle)
                EditorTool.FILTERS -> FiltersTray(effects = effects, onUpdateEffects = onUpdateEffects)
                EditorTool.CLEAR_VOICE -> ClearVoiceTray(effects = effects, onUpdateEffects = onUpdateEffects)
                EditorTool.FOLLOW -> FollowTray(
                    style = style,
                    overlays = project.overlays,
                    onUpdateStyle = onUpdateStyle,
                    onTrackOverlayMotion = onTrackOverlayMotion
                )
            }
        }

        HorizontalDivider(color = AppColors.SurfaceBorder)

        // Bottom Scrollable Tool Category Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(EditorTool.entries) { tool ->
                val isSelected = tool == activeTool
                Column(
                    modifier = Modifier
                        .width(62.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectTool(tool) }
                        .padding(vertical = 4.dp)
                        .testTag("editor_tool_${tool.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = toolIcon(tool),
                        contentDescription = tool.label,
                        tint = if (isSelected) AppColors.PrimaryPurple else AppColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tool.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else AppColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun toolIcon(tool: EditorTool): ImageVector {
    return when (tool) {
        EditorTool.STYLES -> Icons.Default.AutoAwesome
        EditorTool.TEMPLATES -> Icons.Default.DashboardCustomize
        EditorTool.OVERLAYS -> Icons.Default.Layers
        EditorTool.CLIPS -> Icons.Default.Movie
        EditorTool.FORMAT -> Icons.Default.FormatShapes
        EditorTool.FONTS -> Icons.Default.FontDownload
        EditorTool.COLORS -> Icons.Default.Palette
        EditorTool.ANIMATIONS -> Icons.Default.Animation
        EditorTool.TRANSLATE -> Icons.Default.GTranslate
        EditorTool.SFX -> Icons.Default.VolumeUp
        EditorTool.TIMING -> Icons.Default.Tune
        EditorTool.FILTERS -> Icons.Default.FilterVintage
        EditorTool.CLEAR_VOICE -> Icons.Default.RecordVoiceOver
        EditorTool.FOLLOW -> Icons.Default.Face
    }
}

private data class StylePresetItem(
    val name: String,
    val font: String,
    val textColor: String,
    val bgColor: String,
    val animation: String,
    val karaoke: Boolean
)

@Composable
private fun StylesTray(
    style: SubtitleStyle,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit
) {
    val presets = listOf(
        StylePresetItem("Bold", "Noto Sans Arabic", "#FFFFFF", "#181124", "Pop", false),
        StylePresetItem("Karaoke", "Inter", "#FFFFFF", "#0D0B14", "Pop", true),
        StylePresetItem("Orange Headline", "Vazirmatn", "#FF7A00", "#110D1A", "Pop", true),
        StylePresetItem("Neon", "Inter", "#A78BFA", "#2E1065", "Bounce", false),
        StylePresetItem("Vazir", "Vazirmatn", "#FACC15", "#1F2937", "Slide up", false),
        StylePresetItem("Arabic Clean", "Amiri", "#FFFFFF", "#312E81", "Fade", false),
        StylePresetItem("Minimal", "Inter", "#F8FAFC", "#000000", "Fade", false)
    )

    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(presets) { p ->
            val isSelected = style.preset == p.name
            Surface(
                modifier = Modifier
                    .width(114.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onUpdateStyle {
                            it.copy(
                                preset = p.name,
                                font = p.font,
                                textColor = p.textColor,
                                bgColor = p.bgColor,
                                animation = p.animation,
                                karaoke = p.karaoke
                            )
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                color = AppColors.SurfaceElevated,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) AppColors.PrimaryPurple else AppColors.SurfaceBorder
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Aa سلام",
                        fontFamily = resolveSubtitleFontFamily(p.font),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = parseHexColor(p.textColor)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = p.name,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) AppColors.PrimaryPurpleLight else AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class TemplateItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val hexColor: String
)

@Composable
private fun TemplatesTray(onApplyTemplate: (String) -> Unit) {
    val templates = listOf(
        TemplateItem("v1_orange_headline", "Orange Headline", "Video 1 Persian Hero", "#FF7A00"),
        TemplateItem("v1_orange_white_explainer", "Orange/White Explainer", "Dual-Track Layout", "#FFFFFF"),
        TemplateItem("v1_purple_flare", "Purple Flare", "Light Leak Transition", "#D946EF"),
        TemplateItem("v2_hand_hologram", "Hand Hologram", "Video 2 Glow Object", "#38BDF8"),
        TemplateItem("v2_soft_bg_words", "Soft Persian Words", "Behind Speaker Depth", "#94A3B8"),
        TemplateItem("v2_flag_subject", "Waving Flag", "Flag Behind Subject", "#F59E0B"),
        TemplateItem("v2_metallic_australia", "Metallic Reveal", "AUSTRALIA Banner", "#E2E8F0"),
        TemplateItem("v2_end_accent", "End Studio Accent", "Bottom Red Title", "#EF4444")
    )

    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(templates) { t ->
            val accent = parseHexColor(t.hexColor)
            Surface(
                modifier = Modifier
                    .width(142.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onApplyTemplate(t.id) }
                    .testTag("template_card_${t.id}"),
                shape = RoundedCornerShape(12.dp),
                color = AppColors.SurfaceElevated,
                border = BorderStroke(1.5.dp, accent.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = t.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = t.subtitle,
                        fontSize = 10.sp,
                        color = AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlaysTray(
    overlays: List<GraphicOverlay>,
    onAddOverlay: () -> Unit,
    onEditOverlay: (GraphicOverlay) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onDeleteOverlay: (String) -> Unit,
    onTrackMotion: (GraphicOverlay) -> Unit
) {
    if (overlays.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("No Graphic Overlays", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Text(
                    "Add glowing holograms, waving flags, light flares, or keyframed titles.",
                    fontSize = 11.sp,
                    color = AppColors.TextSecondary
                )
            }
            Button(
                onClick = onAddOverlay,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryPurple)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Layer", fontSize = 12.sp)
            }
        }
        return
    }

    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            OutlinedButton(
                onClick = onAddOverlay,
                modifier = Modifier.height(80.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Layer", fontSize = 12.sp)
            }
        }

        items(overlays, key = { it.id }) { ov ->
            val ovColor = parseHexColor(ov.color)
            Surface(
                modifier = Modifier
                    .width(185.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                color = AppColors.SurfaceElevated,
                border = BorderStroke(1.dp, if (ov.visible) ovColor else AppColors.SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ov.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onToggleVisibility(ov.id) },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = if (ov.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle visibility",
                                tint = if (ov.visible) ovColor else AppColors.TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Text(
                        text = "${String.format(Locale.US, "%.1f", ov.start)}s–${String.format(Locale.US, "%.1f", ov.end)}s • ${if (ov.isBackground) "Behind Subject" else "Foreground"} • ${ov.keyframes.size} KF",
                        fontSize = 10.sp,
                        color = AppColors.TextSecondary,
                        maxLines = 1
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Track AI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.AccentCyan,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onTrackMotion(ov) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Row {
                            IconButton(
                                onClick = { onEditOverlay(ov) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Keyframes",
                                    tint = AppColors.PrimaryPurpleLight,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteOverlay(ov.id) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Layer",
                                    tint = AppColors.AccentCoral,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipsAndTrimTray(
    project: Project,
    onUpdateTrim: (Double, Double) -> Unit,
    onSplitClip: () -> Unit
) {
    val maxDur = max(1f, project.duration.toFloat())
    val startVal = project.trim.start.toFloat().coerceIn(0f, maxDur)
    val endVal = project.trim.end.toFloat().coerceIn(startVal + 0.2f, maxDur)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trim (${String.format(Locale.US, "%.1f", startVal)}s – ${String.format(Locale.US, "%.1f", endVal)}s) • ${project.videoClips.size.coerceAtLeast(1)} Clips",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            OutlinedButton(
                onClick = onSplitClip,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Split at Playhead", fontSize = 11.sp)
            }
        }

        RangeSlider(
            value = startVal..endVal,
            onValueChange = { range ->
                if (range.endInclusive - range.start >= 0.3f) {
                    onUpdateTrim(range.start.toDouble(), range.endInclusive.toDouble())
                }
            },
            valueRange = 0f..maxDur
        )
    }
}

@Composable
private fun FormatTray(
    style: SubtitleStyle,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Align:", fontSize = 11.sp, color = AppColors.TextSecondary)
                Spacer(modifier = Modifier.width(6.dp))
                listOf(
                    "left" to Icons.Default.FormatAlignLeft,
                    "center" to Icons.Default.FormatAlignCenter,
                    "right" to Icons.Default.FormatAlignRight
                ).forEach { (alignVal, icon) ->
                    val isSel = style.align == alignVal
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) AppColors.PrimaryPurple else AppColors.SurfaceElevated)
                            .clickable { onUpdateStyle { it.copy(align = alignVal) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = alignVal, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Overlay:", fontSize = 11.sp, color = AppColors.TextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                listOf("None", "Translucent", "Solid").forEach { mode ->
                    FilterChip(
                        selected = style.overlay == mode,
                        onClick = { onUpdateStyle { it.copy(overlay = mode) } },
                        label = { Text(mode, fontSize = 10.sp) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Size ${style.fontSize.toInt()}pt", fontSize = 11.sp, color = AppColors.TextSecondary)
            Slider(
                value = style.fontSize.toFloat().coerceIn(14f, 48f),
                onValueChange = { v -> onUpdateStyle { it.copy(fontSize = v.toDouble()) } },
                valueRange = 14f..48f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )
            Text("Outline ${String.format(Locale.US, "%.1f", style.outline)}", fontSize = 11.sp, color = AppColors.TextSecondary)
            Slider(
                value = style.outline.toFloat().coerceIn(0f, 6f),
                onValueChange = { v -> onUpdateStyle { it.copy(outline = v.toDouble()) } },
                valueRange = 0f..6f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )
        }
    }
}

private data class FontCardInfo(
    val family: String,
    val scriptLabel: String,
    val sample: String
)

@Composable
private fun FontsTray(
    style: SubtitleStyle,
    customFonts: List<String>,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onImportCustomFont: () -> Unit
) {
    val builtInFonts = listOf(
        FontCardInfo("Vazirmatn", "Persian / Dari / Pashto", "کپشن‌فورج متن فارسی"),
        FontCardInfo("Noto Sans Arabic", "Arabic / Persian / Pashto", "خط عربی و پشتو"),
        FontCardInfo("Noto Nastaliq Urdu", "Urdu Nastaliq", "اردو نستعلیق ڈیزائن"),
        FontCardInfo("Lalezar", "Persian Cinematic Bold", "عنوان سینمایی"),
        FontCardInfo("Amiri", "Classical Arabic", "الخط العربي الأصيل"),
        FontCardInfo("Bebas Neue", "English Title", "AUSTRALIA BANNER"),
        FontCardInfo("Inter", "Clean Modern Sans", "Modern Subtitles")
    ) + customFonts.map { FontCardInfo(it, "Imported Custom Font", "Custom: $it") }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                OutlinedButton(
                    onClick = onImportCustomFont,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(".TTF/.OTF", fontSize = 11.sp)
                }
            }
            items(builtInFonts) { f ->
                val isSel = style.font.equals(f.family, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .width(136.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onUpdateStyle { it.copy(font = f.family) } },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSel) AppColors.PrimaryPurple.copy(alpha = 0.2f) else AppColors.SurfaceElevated,
                    border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) AppColors.PrimaryPurple else AppColors.SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = f.sample,
                            fontFamily = resolveSubtitleFontFamily(f.family),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${f.family} • ${f.scriptLabel}",
                            fontSize = 9.sp,
                            color = AppColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Bottom fine typography bar: Glow + Gradient toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(Color.Black.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Glow:", fontSize = 10.sp, color = AppColors.TextSecondary)
            Slider(
                value = style.glow.toFloat().coerceIn(0f, 12f),
                onValueChange = { v -> onUpdateStyle { it.copy(glow = v.toDouble()) } },
                valueRange = 0f..12f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )
            Text("Spacing:", fontSize = 10.sp, color = AppColors.TextSecondary)
            Slider(
                value = style.letterSpacing.toFloat().coerceIn(-1f, 8f),
                onValueChange = { v -> onUpdateStyle { it.copy(letterSpacing = v.toDouble()) } },
                valueRange = -1f..8f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onUpdateStyle { it.copy(gradient = !it.gradient) } },
                color = if (style.gradient) AppColors.PrimaryPurple else AppColors.SurfaceElevated,
                border = BorderStroke(1.dp, AppColors.PrimaryPurple)
            ) {
                Text(
                    text = "Gradient",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorsTray(
    style: SubtitleStyle,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit
) {
    val textSwatches = listOf("#FFFFFF", "#FF7A00", "#FACC15", "#A78BFA", "#06B6D4", "#10B981", "#F43F5E")
    val bgSwatches = listOf("#181124", "#000000", "#2E1065", "#0F172A", "#1E1B4B", "#064E3B")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Text:", fontSize = 12.sp, color = AppColors.TextSecondary, modifier = Modifier.width(44.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                textSwatches.forEach { hex ->
                    val isSel = style.textColor.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(hex))
                            .border(
                                width = if (isSel) 3.dp else 1.dp,
                                color = if (isSel) AppColors.PrimaryPurple else AppColors.SurfaceBorder,
                                shape = CircleShape
                            )
                            .clickable { onUpdateStyle { it.copy(textColor = hex) } }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Box:", fontSize = 12.sp, color = AppColors.TextSecondary, modifier = Modifier.width(36.dp))
            Switch(
                checked = style.bgEnabled,
                onCheckedChange = { enabled -> onUpdateStyle { it.copy(bgEnabled = enabled) } },
                modifier = Modifier.padding(end = 10.dp)
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                bgSwatches.forEach { hex ->
                    val isSel = style.bgColor.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(parseHexColor(hex))
                            .border(
                                width = if (isSel) 2.5.dp else 1.dp,
                                color = if (isSel) AppColors.PrimaryPurpleLight else AppColors.SurfaceBorder,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onUpdateStyle { it.copy(bgColor = hex, bgEnabled = true) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimationsTray(
    style: SubtitleStyle,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit
) {
    val animations = listOf("Pop", "Bounce", "Fade", "Slide up", "Slide down", "Zoom", "None")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(animations) { anim ->
                val isSel = style.animation == anim
                Surface(
                    modifier = Modifier
                        .width(92.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onUpdateStyle { it.copy(animation = anim) } },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSel) AppColors.PrimaryPurple.copy(alpha = 0.22f) else AppColors.SurfaceElevated,
                    border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) AppColors.PrimaryPurple else AppColors.SurfaceBorder)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleFilled,
                            contentDescription = anim,
                            tint = if (isSel) AppColors.PrimaryPurpleLight else AppColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = anim,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Word-by-Word Active Karaoke Highlight (#FACC15)",
                fontSize = 11.sp,
                color = AppColors.TextSecondary
            )
            Switch(
                checked = style.karaoke,
                onCheckedChange = { k -> onUpdateStyle { it.copy(karaoke = k) } }
            )
        }
    }
}

@Composable
private fun TranslateAndSrtTray(
    onOpenTranslateModal: () -> Unit,
    onImportSrt: () -> Unit,
    onExportSrt: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Multilingual & RTL Subtitles",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )
            Text(
                text = "Translate to Persian/Dari, Pashto, Urdu, Arabic, or import/export .SRT files.",
                fontSize = 11.sp,
                color = AppColors.TextSecondary
            )
        }

        OutlinedButton(onClick = onImportSrt) {
            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Import SRT", fontSize = 11.sp)
        }

        OutlinedButton(onClick = onExportSrt) {
            Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save SRT", fontSize = 11.sp)
        }

        Button(
            onClick = onOpenTranslateModal,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryPurple),
            modifier = Modifier.testTag("open_translate_modal_button")
        ) {
            Icon(Icons.Default.GTranslate, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Translate", fontSize = 12.sp)
        }
    }
}

@Composable
private fun SfxTray(
    effects: VideoEffects,
    onAuditionSfx: (String) -> Unit,
    onUpdateEffects: ((VideoEffects) -> VideoEffects) -> Unit,
    onAddClipAtPlayhead: (String) -> Unit
) {
    val categories = listOf(
        "None", "Pop", "Click", "Whoosh", "Shimmer", "Impact",
        "Riser", "Bass", "Glitch", "Light Sweep", "Transition", "Ambient"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { cat ->
                val isSel = effects.sfx == cat
                FilterChip(
                    selected = isSel,
                    onClick = { onAuditionSfx(cat) },
                    label = { Text(cat, fontSize = 11.sp) },
                    leadingIcon = if (isSel && cat != "None") {
                        {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else null
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Sentence", "Word").forEach { mode ->
                FilterChip(
                    selected = effects.sfxMode == mode,
                    onClick = { onUpdateEffects { it.copy(sfxMode = mode) } },
                    label = { Text(mode, fontSize = 10.sp) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            Text("Vol ${effects.sfxVolume}%", fontSize = 10.sp, color = AppColors.TextSecondary)
            Slider(
                value = effects.sfxVolume.toFloat(),
                onValueChange = { v -> onUpdateEffects { it.copy(sfxVolume = v.toInt()) } },
                valueRange = 0f..100f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )
            OutlinedButton(
                onClick = { onAddClipAtPlayhead(effects.sfx) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Add Clip", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun TimingTray(
    style: SubtitleStyle,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Vertical Pos (${(style.yPos * 100).toInt()}%):",
                fontSize = 12.sp,
                color = AppColors.TextSecondary,
                modifier = Modifier.width(125.dp)
            )
            Slider(
                value = style.yPos.toFloat().coerceIn(0.1f, 0.9f),
                onValueChange = { v -> onUpdateStyle { it.copy(yPos = v.toDouble()) } },
                valueRange = 0.1f..0.9f,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Early Offset (${String.format(Locale.US, "%.1f", style.early)}s):",
                fontSize = 12.sp,
                color = AppColors.TextSecondary,
                modifier = Modifier.width(125.dp)
            )
            Slider(
                value = style.early.toFloat().coerceIn(0f, 1f),
                onValueChange = { v -> onUpdateStyle { it.copy(early = v.toDouble()) } },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private data class FilterPreset(
    val name: String,
    val b: Double,
    val c: Double,
    val s: Double
)

@Composable
private fun FiltersTray(
    effects: VideoEffects,
    onUpdateEffects: ((VideoEffects) -> VideoEffects) -> Unit
) {
    val presets = listOf(
        FilterPreset("Original", 0.0, 1.0, 1.0),
        FilterPreset("Vivid", 0.04, 1.2, 1.35),
        FilterPreset("Warm", 0.03, 1.08, 1.15),
        FilterPreset("Cool", -0.02, 1.10, 0.92),
        FilterPreset("Cinema", -0.03, 1.25, 1.10),
        FilterPreset("B&W", 0.0, 1.20, 0.0)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { p ->
                val isSel = kotlin.math.abs(effects.saturation - p.s) < 0.05 &&
                    kotlin.math.abs(effects.contrast - p.c) < 0.05
                FilterChip(
                    selected = isSel,
                    onClick = {
                        onUpdateEffects {
                            it.copy(brightness = p.b, contrast = p.c, saturation = p.s)
                        }
                    },
                    label = { Text(p.name, fontSize = 11.sp) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Contrast", fontSize = 10.sp, color = AppColors.TextSecondary)
            Slider(
                value = effects.contrast.toFloat().coerceIn(0.5f, 2.0f),
                onValueChange = { v -> onUpdateEffects { it.copy(contrast = v.toDouble()) } },
                valueRange = 0.5f..2.0f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )
            Text("Saturation", fontSize = 10.sp, color = AppColors.TextSecondary)
            Slider(
                value = effects.saturation.toFloat().coerceIn(0f, 2.5f),
                onValueChange = { v -> onUpdateEffects { it.copy(saturation = v.toDouble()) } },
                valueRange = 0f..2.5f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )
        }
    }
}

@Composable
private fun ClearVoiceTray(
    effects: VideoEffects,
    onUpdateEffects: ((VideoEffects) -> VideoEffects) -> Unit
) {
    val levels = listOf("Off", "Light", "Balanced", "Strong")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Clear Voice (FFmpeg Vocal Isolation & High-Pass Denoising)",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            levels.forEach { lvl ->
                FilterChip(
                    selected = effects.clearVoice == lvl,
                    onClick = { onUpdateEffects { it.copy(clearVoice = lvl) } },
                    label = { Text(lvl, fontSize = 12.sp) }
                )
            }
        }
    }
}

@Composable
private fun FollowTray(
    style: SubtitleStyle,
    overlays: List<GraphicOverlay>,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onTrackOverlayMotion: (GraphicOverlay) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Face & Object Motion Tracking",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )
            Text(
                text = "Dynamic speaker face-follow for subtitles & keyframe path generation for VFX layers.",
                fontSize = 11.sp,
                color = AppColors.TextSecondary
            )
        }
        if (overlays.isNotEmpty()) {
            OutlinedButton(
                onClick = { onTrackOverlayMotion(overlays.first()) },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Track Layer", fontSize = 11.sp)
            }
        }
        Switch(
            checked = style.followFace,
            onCheckedChange = { enabled -> onUpdateStyle { it.copy(followFace = enabled) } }
        )
    }
}

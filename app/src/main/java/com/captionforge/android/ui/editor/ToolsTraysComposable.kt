package com.captionforge.android.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.captionforge.android.audio.SfxPlayerManager
import com.captionforge.android.audio.SfxSoundPreset
import com.captionforge.android.model.AudioClip
import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.GraphicOverlay
import com.captionforge.android.model.Project
import com.captionforge.android.model.StylePresetCatalog
import com.captionforge.android.model.StylePresetItem
import com.captionforge.android.model.SubtitleStyle
import com.captionforge.android.model.Track
import com.captionforge.android.model.VideoEffects
import com.captionforge.android.ui.theme.AppColors
import com.captionforge.android.ui.theme.FontCatalog
import com.captionforge.android.ui.theme.LalezarFontFamily
import com.captionforge.android.ui.theme.VazirmatnFontFamily
import com.captionforge.android.ui.theme.parseHexColor
import com.captionforge.android.ui.theme.resolveSubtitleFontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.captionforge.android.model.SrtParser
import com.captionforge.android.viewmodel.CaptionForgeViewModel
import com.captionforge.android.viewmodel.EditorTool
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ToolsTraysComposable(
    activeTool: EditorTool,
    project: Project,
    activeTrack: Track?,
    selectedCue: CaptionCue?,
    selectedOverlay: GraphicOverlay?,
    viewModel: CaptionForgeViewModel,
    onOpenTranslateSheet: () -> Unit,
    onOpenAddOverlaySheet: () -> Unit,
    onOpenEditOverlayDialog: (GraphicOverlay) -> Unit,
    onOpenEditCueDialog: (CaptionCue) -> Unit,
    onOpenEditAudioClipDialog: (AudioClip) -> Unit,
    onPickCustomFont: () -> Unit,
    onPickSrtFile: () -> Unit,
    onPickUserAudioFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = activeTrack?.style ?: SubtitleStyle()

    // Grid-heavy drawers (Style, Font, Color, Animation) get a generous studio height like Magic Editor
    val trayHeight = when (activeTool) {
        EditorTool.STYLES,
        EditorTool.FONTS,
        EditorTool.COLORS,
        EditorTool.ANIMATIONS -> 244.dp
        else -> 196.dp
    }

    val selectedWordIndex by viewModel.selectedWordIndex.collectAsStateWithLifecycle()
    var isDrawerExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0A12))
    ) {
        // Sleek Drawer Handle Bar so user can collapse/expand the active tool panel at any time
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF110F1D))
                .border(0.5.dp, Color(0xFF232038))
                .clickable { isDrawerExpanded = !isDrawerExpanded }
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .testTag("tool_drawer_toggle_btn"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF635BFF))
                )
                Text(
                    text = activeTool.label.uppercase(Locale.US),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFA5B4FC)
                )
            }
            Text(
                text = if (isDrawerExpanded) "Hide Panel ⌄" else "Show Panel ⌃",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
        }

        if (isDrawerExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trayHeight)
                    .background(Color(0xFF0E0D17))
                    .border(0.5.dp, Color(0xFF232038))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                when (activeTool) {
                    EditorTool.STYLES -> StylesTray(
                    project = project,
                    style = style,
                    selectedCue = selectedCue,
                    onApplyPreset = { preset, toEntireTrack ->
                        viewModel.applyStylePreset(preset, toEntireTrack)
                    },
                    onToggleFavoritePreset = { viewModel.toggleFavoriteStylePreset(it) },
                    onSaveCustomPreset = { name -> viewModel.saveCurrentStyleAsCustomPreset(name) },
                    onDeleteCustomPreset = { name -> viewModel.deleteCustomStylePreset(name) },
                    onEditCueWords = { selectedCue?.let(onOpenEditCueDialog) }
                )
                EditorTool.FONTS -> FontsTray(
                    project = project,
                    style = style,
                    onSelectFont = { fontName, weight -> viewModel.selectFontForActiveTrack(fontName, weight) },
                    onToggleFavoriteFont = { viewModel.toggleFavoriteFont(it) },
                    onUpdateStyle = { viewModel.updateActiveTrackStyle(it) },
                    onPickCustomFont = onPickCustomFont
                )
                EditorTool.COLORS -> ColorsTray(
                    style = style,
                    onUpdateStyle = { viewModel.updateActiveTrackStyle(it) }
                )
                EditorTool.ANIMATIONS -> AnimationsTray(
                    style = style,
                    selectedCue = selectedCue,
                    onUpdateStyle = { viewModel.updateActiveTrackStyle(it) },
                    onOpenWordInspector = { selectedCue?.let(onOpenEditCueDialog) }
                )
                EditorTool.FOLLOW -> FollowTray(
                    style = style,
                    selectedOverlay = selectedOverlay,
                    onUpdateStyle = { viewModel.updateActiveTrackStyle(it) },
                    onTrackOverlay = { ov, type -> viewModel.trackMotionForOverlay(ov, type) }
                )
                EditorTool.TRANSLATE -> TranslateTray(
                    project = project,
                    onOpenTranslateSheet = onOpenTranslateSheet,
                    onImportSrt = onPickSrtFile
                )
                EditorTool.SFX -> SfxTray(
                    project = project,
                    selectedCue = selectedCue,
                    selectedWordIndex = selectedWordIndex,
                    onSelectWordIndex = { viewModel.selectWordIndex(it) },
                    sfxPlayer = viewModel.sfxPlayer,
                    onSelectSfx = { viewModel.auditionAndSelectSfx(it) },
                    onAddPresetClipAtPlayhead = { viewModel.addSfxPresetClipAtPlayhead(it) },
                    onImportUserAudio = onPickUserAudioFile,
                    onEditAudioClip = onOpenEditAudioClipDialog,
                    onUpdateEffects = { viewModel.updateVideoEffects(it) }
                )
                EditorTool.TIMING -> TimingAndPositionTray(
                    style = style,
                    selectedCue = selectedCue,
                    onUpdateStyle = { viewModel.updateActiveTrackStyle(it) },
                    onEditCue = onOpenEditCueDialog,
                    onSplitCue = { viewModel.splitSelectedCueAtPlayhead() },
                    onMergeCue = { viewModel.mergeSelectedCueWithNext() }
                )
                EditorTool.CLIPS -> TextFlowAndClipsTray(
                    project = project,
                    selectedCue = selectedCue,
                    onSplitVideoClip = { viewModel.splitVideoClipAtPlayhead() },
                    onCycleTransition = { viewModel.cycleClipTransition(it) },
                    onAddCue = { viewModel.addCaptionAtPlayhead() },
                    onEditCue = onOpenEditCueDialog,
                    onSplitCue = { viewModel.splitSelectedCueAtPlayhead() },
                    onMergeCue = { viewModel.mergeSelectedCueWithNext() },
                    onDeleteCue = { viewModel.deleteSelectedCue() },
                    onImportSrt = onPickSrtFile
                )
                EditorTool.OVERLAYS -> OverlaysTray(
                    project = project,
                    style = style,
                    selectedOverlay = selectedOverlay,
                    onUpdateStyle = { viewModel.updateActiveTrackStyle(it) },
                    onSelectOverlay = { viewModel.selectOverlay(it.id) },
                    onToggleVisibility = { viewModel.toggleOverlayVisibility(it) },
                    onDeleteOverlay = { viewModel.deleteOverlay(it) },
                    onEditOverlay = onOpenEditOverlayDialog,
                    onAddOverlayClick = onOpenAddOverlaySheet
                )
                EditorTool.FILTERS -> FiltersTray(
                    effects = project.effects,
                    onUpdateEffects = { viewModel.updateVideoEffects(it) }
                )
                EditorTool.CLEAR_VOICE -> ClearVoiceTray(
                    effects = project.effects,
                    onUpdateEffects = { viewModel.updateVideoEffects(it) }
                )
                EditorTool.TEMPLATES -> TemplatesTray(
                    onApplyTemplate = { viewModel.applyCinematicTemplate(it) }
                )
                EditorTool.FORMAT -> FormatTray(
                    project = project,
                    onSelectAspectRatio = { viewModel.updateAspectRatio(it) },
                    onSelectResolution = { viewModel.updateResolution(it) }
                )
            }
            }
        }

        // Bottom Scrollable Studio Navigation Bar (Icon + Label + PRO Badges)
        val orderedTools = listOf(
            EditorTool.STYLES,
            EditorTool.FONTS,
            EditorTool.COLORS,
            EditorTool.ANIMATIONS,
            EditorTool.FOLLOW,
            EditorTool.TRANSLATE,
            EditorTool.SFX,
            EditorTool.TIMING,
            EditorTool.CLIPS,
            EditorTool.OVERLAYS,
            EditorTool.FILTERS,
            EditorTool.CLEAR_VOICE,
            EditorTool.TEMPLATES,
            EditorTool.FORMAT
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF08070E))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            orderedTools.forEach { tool ->
                val isSelected = tool == activeTool
                val hasProBadge = tool in listOf(
                    EditorTool.FOLLOW,
                    EditorTool.OVERLAYS,
                    EditorTool.FILTERS,
                    EditorTool.CLEAR_VOICE
                )
                val (icon, displayLabel) = when (tool) {
                    EditorTool.STYLES -> Icons.Default.AutoAwesome to "Style"
                    EditorTool.FONTS -> Icons.Default.TextFields to "Font"
                    EditorTool.COLORS -> Icons.Default.Palette to "Color"
                    EditorTool.ANIMATIONS -> Icons.Default.AutoAwesome to "Animation"
                    EditorTool.FOLLOW -> Icons.Default.PersonPin to "Follow"
                    EditorTool.TRANSLATE -> Icons.Default.Translate to "Translate"
                    EditorTool.SFX -> Icons.Default.GraphicEq to "SFX"
                    EditorTool.TIMING -> Icons.Default.Schedule to "Timing"
                    EditorTool.CLIPS -> Icons.Default.Tune to "Text flow"
                    EditorTool.OVERLAYS -> Icons.Default.Layers to "Overlay"
                    EditorTool.FILTERS -> Icons.Default.Palette to "Video Color"
                    EditorTool.CLEAR_VOICE -> Icons.Default.VolumeUp to "Clear voice"
                    EditorTool.TEMPLATES -> Icons.Default.AutoAwesome to "Templates"
                    EditorTool.FORMAT -> Icons.Default.Tune to "Format"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected && isDrawerExpanded) Color(0xFF1F1B38) else Color.Transparent)
                        .clickable {
                            if (isSelected) {
                                isDrawerExpanded = !isDrawerExpanded
                            } else {
                                viewModel.selectTool(tool)
                                isDrawerExpanded = true
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("tool_${tool.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.TopCenter) {
                            Icon(
                                imageVector = icon,
                                contentDescription = displayLabel,
                                tint = if (isSelected) Color.White else Color(0xFF7C7A92),
                                modifier = Modifier
                                    .padding(top = if (hasProBadge) 4.dp else 0.dp)
                                    .size(20.dp)
                            )
                            if (hasProBadge) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF4F46E5)
                                ) {
                                    Text(
                                        text = "PRO",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = displayLabel,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF7C7A92)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 1. CAPTION STYLE DRAWER (Priority 1: All 11+ Required Families + Live Specimen Previews + Favorites + Custom Saved Styles)
 */
@Composable
private fun StylesTray(
    project: Project,
    style: SubtitleStyle,
    selectedCue: CaptionCue?,
    onApplyPreset: (StylePresetItem, Boolean) -> Unit,
    onToggleFavoritePreset: (String) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onEditCueWords: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var applyToEntireTrack by remember { mutableStateOf(true) }

    val categories = listOf(
        "All",
        "Favorites ★",
        "3-Tier RTL",
        "Documentary",
        "Social Reels",
        "Karaoke",
        "Minimal",
        "Neon & Glitch",
        "Typewriter & Pop",
        "Metallic & Luxury",
        "Nastaliq",
        "Custom"
    )
    val builtInPresets = StylePresetCatalog.presets
    val customPresetItems = remember(project.customStylePresets) {
        project.customStylePresets.mapIndexed { idx, customStyle ->
            StylePresetItem(
                id = "custom_${idx}_${customStyle.preset.lowercase().replace(" ", "_")}",
                name = customStyle.preset,
                category = "Custom",
                badgeText = "CUSTOM",
                previewSample = "اگه تو Chat",
                description = "Saved Custom Style • ${customStyle.font}",
                style = customStyle,
                defaultSfx = "None"
            )
        }
    }
    val allPresets = builtInPresets + customPresetItems

    val filteredPresets = remember(selectedCategory, allPresets, project.favoriteStylePresets) {
        when (selectedCategory) {
            "All" -> allPresets
            "Favorites ★" -> allPresets.filter { p ->
                project.favoriteStylePresets.any { it.equals(p.id, ignoreCase = true) || it.equals(p.name, ignoreCase = true) }
            }
            "3-Tier RTL" -> allPresets.filter {
                it.style.threeTierLayout || it.id in listOf("orange_persian_kinetic", "white_bold_colored_emphasis")
            }
            "Documentary" -> allPresets.filter {
                it.id in listOf("bold_documentary", "cinematic_film", "white_bold_colored_emphasis")
            }
            "Social Reels" -> allPresets.filter {
                it.id in listOf("social_media_reels", "youtube_creator", "word_by_word_pop")
            }
            "Karaoke" -> allPresets.filter {
                it.style.karaoke || it.id == "color_highlight_karaoke"
            }
            "Minimal" -> allPresets.filter {
                it.id in listOf("modern_minimal", "luxury_editorial", "bold_documentary")
            }
            "Neon & Glitch" -> allPresets.filter {
                it.id in listOf("neon_glow", "glitch_digital")
            }
            "Typewriter & Pop" -> allPresets.filter {
                it.id in listOf("typewriter_reveal", "pop_and_bounce", "word_by_word_pop")
            }
            "Metallic & Luxury" -> allPresets.filter {
                it.id in listOf("metallic_cinematic_title", "luxury_editorial", "cinematic_film")
            }
            "Nastaliq" -> allPresets.filter {
                it.id in listOf("elegant_nastaliq", "orange_persian_kinetic")
            }
            "Custom" -> customPresetItems
            else -> allPresets
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Row: "Caption style" + Track/Cue Toggle + Save Custom Style + Word Inspector button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Caption style",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF1B192B),
                    modifier = Modifier
                        .clickable { applyToEntireTrack = !applyToEntireTrack }
                        .testTag("style_scope_toggle_btn")
                ) {
                    Text(
                        text = if (applyToEntireTrack) "Scope: Track" else "Scope: Cue",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5B4FC),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF231E3C),
                    border = BorderStroke(1.dp, Color(0xFF4F46E5)),
                    modifier = Modifier
                        .clickable {
                            onSaveCustomPreset("My ${style.font} Style ${project.customStylePresets.size + 1}")
                        }
                        .testTag("save_custom_style_btn")
                ) {
                    Text(
                        text = "+ Save Style",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (selectedCue != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AppColors.AccentOrange.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, AppColors.AccentOrange),
                        modifier = Modifier.clickable { onEditCueWords() }
                    ) {
                        Text(
                            text = "Words ✎",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.AccentOrange,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Category Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            categories.forEach { cat ->
                val isSel = selectedCategory == cat
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSel) Color.White else Color(0xFF1C1B29),
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2-Column Visual Grid of Caption Style Cards
        val rows = filteredPresets.chunked(2)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rows.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { preset ->
                        val isSelected = style.preset.equals(preset.name, ignoreCase = true) ||
                            style.preset.equals(preset.style.preset, ignoreCase = true)
                        val isFav = project.favoriteStylePresets.any {
                            it.equals(preset.id, ignoreCase = true) || it.equals(preset.name, ignoreCase = true)
                        }
                        val isCustom = preset.id.startsWith("custom_")
                        CaptionStyleGridCard(
                            preset = preset,
                            isSelected = isSelected,
                            isFavorite = isFav,
                            isCustom = isCustom,
                            onClick = { onApplyPreset(preset, applyToEntireTrack) },
                            onToggleFavorite = { onToggleFavoritePreset(preset.id) },
                            onDeleteCustom = if (isCustom) ({ onDeleteCustomPreset(preset.name) }) else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptionStyleGridCard(
    preset: StylePresetItem,
    isSelected: Boolean,
    isFavorite: Boolean,
    isCustom: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteCustom: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val primaryCol = parseHexColor(preset.style.textColor)
    val secondaryCol = parseHexColor(preset.style.secondaryColor)
    val bgCardCol = parseHexColor(preset.style.bgColor, preset.style.bgOpacity.coerceIn(35, 100))
    val fontFam = resolveSubtitleFontFamily(preset.style.font)

    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF232232), Color(0xFF151420))
                )
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF635BFF) else Color(0xFF2D2B42),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(7.dp)
            .testTag("style_card_${preset.id}")
    ) {
        // Top-Left Animation / Layout Badge
        Surface(
            shape = RoundedCornerShape(5.dp),
            color = if (preset.style.threeTierLayout) Color(0xFFFF6B00) else Color(0xFF4F46E5),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text(
                text = when {
                    preset.style.threeTierLayout -> "3-TIER"
                    preset.style.animation != "None" -> preset.style.animation.uppercase()
                    else -> "PRO"
                },
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        // Top-Right Favorite Bookmark / Custom Delete
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isCustom && onDeleteCustom != null) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF3A1A24))
                        .clickable { onDeleteCustom() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Custom Preset",
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isFavorite) Color(0xFFF59E0B) else Color.Black.copy(alpha = 0.45f))
                    .clickable { onToggleFavorite() }
                    .testTag("style_fav_${preset.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = "Favorite Style",
                    tint = if (isFavorite) Color.Black else Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        // Center Live Styled Specimen Preview
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                preset.style.threeTierLayout -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ویزای تحصیلی",
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "فنلاند",
                            fontFamily = LalezarFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = secondaryCol
                        )
                    }
                }
                preset.style.bgEnabled -> {
                    Surface(
                        shape = RoundedCornerShape(if (preset.style.bgShape == "Pill") 50 else 7),
                        color = bgCardCol,
                        border = if (preset.style.outline > 0) {
                            BorderStroke(1.dp, secondaryCol.copy(alpha = 0.7f))
                        } else null
                    ) {
                        Text(
                            text = preset.previewSample.replace("\n", " "),
                            fontFamily = fontFam,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryCol,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                preset.style.karaoke -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = secondaryCol
                        ) {
                            Text(
                                text = preset.previewSample.substringBefore(" "),
                                fontFamily = fontFam,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = preset.previewSample.substringAfter(" ", "LIVE"),
                            fontFamily = fontFam,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryCol,
                            maxLines = 1
                        )
                    }
                }
                else -> {
                    Text(
                        text = preset.previewSample.replace("\n", " "),
                        fontFamily = fontFam,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (preset.style.gradient) secondaryCol else primaryCol,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Bottom Row: Preset Name + SFX / Selected Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = preset.name,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF635BFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            } else if (preset.defaultSfx != "None") {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = preset.defaultSfx,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

/**
 * 2. PROFESSIONAL MULTILINGUAL FONT BROWSER & TYPOGRAPHY DESIGNER (Priority 2: Farsi/Dari, Pashto, Urdu, Arabic, English + Weights, Outline, Shadow, 3D, Background, Gradient)
 */
@Composable
private fun FontsTray(
    project: Project,
    style: SubtitleStyle,
    onSelectFont: (String, Int) -> Unit,
    onToggleFavoriteFont: (String) -> Unit,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onPickCustomFont: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Farsi") }
    var subMode by remember { mutableIntStateOf(0) } // 0 = Font Gallery, 1 = Weight / Stroke / Shadow / Card

    val persianDisplayLabels = mapOf(
        "Vazirmatn" to "فونت وزیرمتن",
        "Lalezar" to "لاله زار",
        "Noto Sans Arabic" to "فونت ملی",
        "Noto Naskh Arabic" to "فونت نسخ",
        "Noto Nastaliq Urdu" to "فونت نستعلیق",
        "Amiri" to "فونت امیری",
        "Cairo" to "فونت قاهره",
        "Tajawal" to "فونت تجول",
        "IBM Plex Sans Arabic" to "فونت کولاک",
        "Reem Kufi" to "فونت کوفی",
        "Markazi Text" to "فونت مرکزی",
        "Lateef" to "فونت لطیف",
        "Mirza" to "فونت میرزا",
        "Scheherazade New" to "فونت شهرزاد"
    )

    val allFonts = FontCatalog.allFonts(project.customFonts)
    val visibleFonts = remember(selectedTab, allFonts, project.favoriteFonts) {
        when (selectedTab) {
            "Farsi" -> allFonts.filter { it.matchesScript("fa") }
            "Pashto" -> allFonts.filter { it.matchesScript("ps") }
            "Urdu" -> allFonts.filter { it.matchesScript("ur") }
            "Arabic" -> allFonts.filter { it.matchesScript("ar") }
            "English" -> allFonts.filter { !it.isRtlOptimized }
            "Favorites ★" -> allFonts.filter { f -> project.favoriteFonts.any { it.equals(f.name, ignoreCase = true) } }
            "Custom" -> allFonts.filter { it.category == "Custom" }
            else -> allFonts
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Row: "Font" + Mode Switch [Gallery | Stroke & Card] + Script Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Font",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (subMode == 1) Color(0xFF635BFF) else Color(0xFF1C1B29),
                    modifier = Modifier
                        .clickable { subMode = if (subMode == 0) 1 else 0 }
                        .testTag("font_typography_controls_toggle")
                ) {
                    Text(
                        text = if (subMode == 0) "Weight & Stroke ⚙" else "← Font Cards",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            if (subMode == 0) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf("All", "Farsi", "Pashto", "Urdu", "Arabic", "English", "Favorites ★").forEach { tab ->
                        val isSel = selectedTab == tab
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSel) Color.White else Color(0xFF1C1B29),
                            modifier = Modifier.clickable { selectedTab = tab }
                        ) {
                            Text(
                                text = tab,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (subMode == 0) {
            // 3-Column Grid of Rounded Dark Font Cards + "☁ Add more fonts" button at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleFonts.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { fontItem ->
                            val isSel = style.font.equals(fontItem.name, ignoreCase = true)
                            val isFav = project.favoriteFonts.any { it.equals(fontItem.name, ignoreCase = true) }
                            val displayTitle = if (fontItem.isRtlOptimized) {
                                persianDisplayLabels[fontItem.name] ?: fontItem.sampleText
                            } else {
                                fontItem.name
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF191826))
                                    .border(
                                        width = if (isSel) 2.dp else 1.dp,
                                        color = if (isSel) Color(0xFF635BFF) else Color(0xFF28263B),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectFont(fontItem.name, style.fontWeight) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("font_card_${fontItem.name}"),
                                contentAlignment = Alignment.Center
                            ) {
                                // Top-Left Favorite Star
                                Text(
                                    text = if (isFav) "★" else "☆",
                                    fontSize = 10.sp,
                                    color = if (isFav) Color(0xFFFACC15) else Color(0xFF64748B),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .clickable { onToggleFavoriteFont(fontItem.name) }
                                        .testTag("font_fav_${fontItem.name}")
                                )
                                // Top-Right Script Badge
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (fontItem.isRtlOptimized) Color(0xFF4F46E5) else Color(0xFF334155),
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Text(
                                        text = if (fontItem.isRtlOptimized) "RTL" else "LATIN",
                                        fontSize = 6.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    Text(
                                        text = displayTitle,
                                        fontFamily = resolveSubtitleFontFamily(fontItem.name),
                                        fontSize = if (fontItem.isRtlOptimized) 14.sp else 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = fontItem.name,
                                        fontSize = 8.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Full-width "☁ Add more fonts" button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPickCustomFont() }
                        .testTag("import_custom_font_btn"),
                    color = Color(0xFF191826),
                    border = BorderStroke(1.dp, Color(0xFF2E2B46))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Add more fonts",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add more fonts (.TTF / .OTF)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Typography Weight, Outline, Shadow, 3D, Background Card & Gradient Inspector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Font Weight Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Weight", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(400 to "Regular", 600 to "Semi", 700 to "Bold", 800 to "Extra", 900 to "Black").forEach { (w, label) ->
                            val isSel = style.fontWeight == w
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF635BFF) else Color(0xFF1B192B),
                                modifier = Modifier.clickable { onUpdateStyle { it.copy(fontWeight = w) } }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Outline, Shadow & 3D Extrusion Quick Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Outline: ${style.outline.roundToInt()}px", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Slider(
                            value = style.outline.toFloat(),
                            onValueChange = { v -> onUpdateStyle { it.copy(outline = v.roundToInt().toDouble()) } },
                            valueRange = 0f..8f
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Shadow: ${style.shadowDepth.roundToInt()}px", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Slider(
                            value = style.shadowDepth.toFloat(),
                            onValueChange = { v -> onUpdateStyle { it.copy(shadowDepth = v.roundToInt().toDouble()) } },
                            valueRange = 0f..12f
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("3D Depth: ${style.extrusion3D.roundToInt()}px", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Slider(
                            value = style.extrusion3D.toFloat(),
                            onValueChange = { v -> onUpdateStyle { it.copy(extrusion3D = v.roundToInt().toDouble()) } },
                            valueRange = 0f..8f
                        )
                    }
                }

                // Background Card Shape & Gradient Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("None", "Rounded", "Pill", "Box").forEach { shapeOpt ->
                        val isActive = if (shapeOpt == "None") !style.bgEnabled else (style.bgEnabled && style.bgShape == shapeOpt)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isActive) Color(0xFF635BFF) else Color(0xFF1B192B),
                            modifier = Modifier.clickable {
                                if (shapeOpt == "None") {
                                    onUpdateStyle { it.copy(bgEnabled = false) }
                                } else {
                                    onUpdateStyle { it.copy(bgEnabled = true, bgShape = shapeOpt) }
                                }
                            }
                        ) {
                            Text(
                                text = "Card: $shapeOpt",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (style.gradient) AppColors.AccentOrange else Color(0xFF1B192B),
                        modifier = Modifier.clickable { onUpdateStyle { it.copy(gradient = !it.gradient) } }
                    ) {
                        Text(
                            text = if (style.gradient) "Gradient: ON" else "Gradient: OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. COLOR DRAWER (Matches Video 1 00:01: Text color / Highlight colour pill tabs + 6-Column Circular Swatches)
 */
@Composable
private fun ColorsTray(
    style: SubtitleStyle,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit
) {
    // 0 = Text color, 1 = Highlight colour, 2 = Background / 3D Outline
    var colorMode by remember { mutableIntStateOf(0) }

    val studioSwatches = listOf(
        "#FFFFFF", "#E2E8F0", "#94A3B8", "#475569", "#1E293B", "#09090D",
        "#FF6B00", "#FF9500", "#FACC15", "#10B981", "#00E5FF", "#3B82F6",
        "#4F46E5", "#8B5CF6", "#A855F7", "#D946EF", "#EC4899", "#F43F5E",
        "#FEF08A", "#BBF7D0", "#BAE6FD", "#C7D2FE", "#E9D5FF", "#FBCFE8"
    )

    val activeHex = when (colorMode) {
        0 -> style.textColor
        1 -> style.secondaryColor
        else -> style.bgColor
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Row: "Color" + Reset button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Color",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            IconButton(
                onClick = {
                    onUpdateStyle {
                        it.copy(
                            textColor = "#FFFFFF",
                            secondaryColor = "#FF6B00",
                            bgColor = "#181124"
                        )
                    }
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Color",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Pill Tabs: [▬ Text color]  [● Highlight colour]  [▣ Box / 3D]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Text color pill
            Surface(
                shape = RoundedCornerShape(50),
                color = if (colorMode == 0) Color.White else Color(0xFF1C1B29),
                modifier = Modifier.clickable { colorMode = 0 }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(parseHexColor(style.textColor))
                            .border(0.5.dp, Color.Gray, RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Text color",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (colorMode == 0) Color.Black else Color.White
                    )
                }
            }

            // Highlight colour pill
            Surface(
                shape = RoundedCornerShape(50),
                color = if (colorMode == 1) Color.White else Color(0xFF1C1B29),
                modifier = Modifier.clickable { colorMode = 1 }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(style.secondaryColor))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Highlight colour",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (colorMode == 1) Color.Black else Color.White
                    )
                }
            }

            // Card / 3D pill
            Surface(
                shape = RoundedCornerShape(50),
                color = if (colorMode == 2) Color.White else Color(0xFF1C1B29),
                modifier = Modifier.clickable { colorMode = 2 }
            ) {
                Text(
                    text = "Card & 3D",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (colorMode == 2) Color.Black else Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 6-Column Grid of Large Circular Color Swatches (Exact Magic Editor Layout)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            studioSwatches.chunked(6).forEach { rowHexes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowHexes.forEach { hex ->
                        val isSel = activeHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSel) 2.5.dp else 1.dp,
                                    color = if (isSel) Color.White else Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .padding(if (isSel) 3.dp else 0.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .clickable {
                                    when (colorMode) {
                                        0 -> onUpdateStyle { it.copy(textColor = hex) }
                                        1 -> onUpdateStyle { it.copy(secondaryColor = hex, karaoke = true) }
                                        else -> onUpdateStyle { it.copy(bgColor = hex, bgEnabled = true) }
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4. ANIMATION DRAWER (Matches Video 1 00:02–00:05: Speed Slider + 3-Column Rounded Animation Cards)
 */
@Composable
private fun AnimationsTray(
    style: SubtitleStyle,
    selectedCue: CaptionCue?,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onOpenWordInspector: () -> Unit
) {
    var animSpeed by remember { mutableFloatStateOf(1.0f) }

    val animCards = listOf(
        Triple("None", "None", false),
        Triple("Fade", "Fade In", false),
        Triple("SlideUp", "Slide Up", false),
        Triple("SlideDown", "Slide Down", false),
        Triple("Pop", "Pop", true),
        Triple("Bounce", "Bounce", true),
        Triple("ZoomIn", "Zoom In", true),
        Triple("Blur", "Defocus", true),
        Triple("Typewriter", "Typewriter", true)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Row: "Animation" + Word Inspector / Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Animation",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedCue != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF28205C),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { onOpenWordInspector() }
                    ) {
                        Text(
                            text = "Per-Word Anim ✎",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Animation",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onUpdateStyle { it.copy(animation = "None") } }
                )
            }
        }

        // Speed Slider Row ("Speed   ————●———— 1.0x")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Speed",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8),
                modifier = Modifier.width(48.dp)
            )
            Slider(
                value = animSpeed,
                onValueChange = { animSpeed = it },
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF4F46E5),
                    inactiveTrackColor = Color(0xFF262438)
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format(Locale.US, "%.1fx", animSpeed),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }

        // 3-Column Grid of Rounded Animation Cards (Exact Magic Editor Grid)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            animCards.chunked(3).forEach { rowTriples ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTriples.forEach { (animKey, label, isPro) ->
                        val isSel = style.animation.equals(animKey, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF191826))
                                .border(
                                    width = if (isSel) 1.8.dp else 1.dp,
                                    color = if (isSel) Color.White else Color(0xFF28263B),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onUpdateStyle { it.copy(animation = animKey) } }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPro) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF4F46E5),
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Text(
                                        text = "PRO",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                                    )
                                }
                            }
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. FOLLOW FACE TRAY (Matches Video 1 00:06–00:21: 4 Visual Mode Cards [Locked, Smooth, Float, Depth] + Stop following button)
 */
@Composable
private fun FollowTray(
    style: SubtitleStyle,
    selectedOverlay: GraphicOverlay?,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onTrackOverlay: (GraphicOverlay, String) -> Unit
) {
    var followMode by remember { mutableStateOf("Smooth") }
    val modes = listOf("Locked", "Smooth", "Float", "Depth")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Follow face",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = Color(0xFF4F46E5)
                ) {
                    Text(
                        text = "PRO",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                    )
                }
            }
            Text(
                text = "The caption stays with the face as it moves.",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }

        // 4 Visual Mode Cards (Locked, Smooth, Float, Depth)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEach { mode ->
                val isSel = style.followFace && followMode == mode
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            followMode = mode
                            onUpdateStyle { it.copy(followFace = true) }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF191826))
                            .border(
                                width = if (isSel) 2.dp else 1.dp,
                                color = if (isSel) Color(0xFF4F46E5) else Color(0xFF28263B),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(34.dp)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (mode == "Depth") Color.White.copy(alpha = 0.55f) else Color.White
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mode,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Color.White else Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Bottom Action Button ("Stop following" in subtle coral or "Start following" in indigo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onUpdateStyle { it.copy(followFace = !it.followFace) }
                    },
                color = if (style.followFace) Color(0xFF3A1A28) else Color(0xFF4F46E5)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonPin,
                        contentDescription = null,
                        tint = if (style.followFace) Color(0xFFF87171) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (style.followFace) "Stop following" else "Enable Follow Face",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (style.followFace) Color(0xFFF87171) else Color.White
                    )
                }
            }

            if (selectedOverlay != null) {
                OutlinedButton(
                    onClick = { onTrackOverlay(selectedOverlay, "hand") },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Track Hand", fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * 6. SFX TRAY (Matches Video 1 00:42–01:10: [Word • | Sentence] Segmented Pill + Chunky Colored Waveform Cards + Volume Slider)
 */
@Composable
private fun SfxTray(
    project: Project,
    selectedCue: CaptionCue?,
    selectedWordIndex: Int,
    onSelectWordIndex: (Int) -> Unit,
    sfxPlayer: SfxPlayerManager,
    onSelectSfx: (String) -> Unit,
    onAddPresetClipAtPlayhead: (SfxSoundPreset) -> Unit,
    onImportUserAudio: () -> Unit,
    onEditAudioClip: (AudioClip) -> Unit,
    onUpdateEffects: ((VideoEffects) -> VideoEffects) -> Unit
) {
    val effects = project.effects
    val isWordMode = effects.sfxMode == "Word" || effects.sfxMode == "Both"

    val activeWords = remember(selectedCue?.id, selectedCue?.text, selectedCue?.words) {
        if (selectedCue == null) emptyList()
        else if (selectedCue.words.isNotEmpty()) selectedCue.words
        else SrtParser.buildWordTimings(selectedCue.text, selectedCue.start, selectedCue.end)
    }
    val activeWord = activeWords.getOrNull(selectedWordIndex.coerceIn(0, maxOf(0, activeWords.lastIndex)))

    val quickStripPresets = listOf(
        Triple("None", "None", Color(0xFF64748B)),
        Triple("UI Click", "Detent", Color(0xFF6366F1)),
        Triple("Punchy Pop", "Pop", Color(0xFFEC4899)),
        Triple("Text Click", "Click", Color(0xFF3B82F6)),
        Triple("Soft Pop", "Tap", Color(0xFFF59E0B)),
        Triple("Soft Transition", "Soft", Color(0xFF10B981)),
        Triple("Heavy Impact", "Stamp", Color(0xFFF97316)),
        Triple("Transition Hit", "Punch", Color(0xFF6366F1)),
        Triple("Cinematic Boom", "Slam", Color(0xFF8B5CF6)),
        Triple("Text Click", "Typewriter", Color(0xFF6366F1)),
        Triple("Soft Pop", "Bubble", Color(0xFFEC4899)),
        Triple("Magical Shimmer", "Message", Color(0xFF3B82F6)),
        Triple("Cinematic Swoosh", "Swish", Color(0xFF6366F1)),
        Triple("Electric Pulse", "Neon", Color(0xFF8B5CF6)),
        Triple("Fast Whoosh", "Whoosh", Color(0xFF10B981)),
        Triple("Light Sweep", "Glide", Color(0xFFEC4899)),
        Triple("Bass Drop", "808 Drop", Color(0xFFF43F5E)),
        Triple("Riser", "Riser", Color(0xFF00E5FF))
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header: "SFX" + Centered [ Word • | Sentence ] Segmented Pill + Import Audio / Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SFX",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                if (isWordMode && activeWord != null) {
                    Text(
                        text = "Word: \"${activeWord.text}\" (${activeWord.sfx})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.AccentGreen,
                        maxLines = 1
                    )
                } else if (!isWordMode && selectedCue != null) {
                    Text(
                        text = "Cue Entrance: ${selectedCue.entranceSfx}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5B4FC),
                        maxLines = 1
                    )
                }
            }

            // Segmented [ Word • | Sentence ] Pill
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF1A1926),
                border = BorderStroke(1.dp, Color(0xFF2B293F))
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isWordMode) Color(0xFF2E2C44) else Color.Transparent,
                        modifier = Modifier
                            .clickable { onUpdateEffects { it.copy(sfxMode = "Word") } }
                            .testTag("sfx_mode_word_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Word",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6366F1))
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (!isWordMode) Color(0xFF2E2C44) else Color.Transparent,
                        modifier = Modifier
                            .clickable { onUpdateEffects { it.copy(sfxMode = "Sentence") } }
                            .testTag("sfx_mode_sentence_btn")
                    ) {
                        Text(
                            text = "Sentence",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onImportUserAudio,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "Import Audio",
                        tint = Color(0xFFA5B4FC),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { onSelectSfx("None") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset SFX",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Quick Word Selector Strip when in Word mode
        if (isWordMode && activeWords.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                activeWords.forEachIndexed { idx, w ->
                    val isWSelected = idx == selectedWordIndex.coerceIn(0, activeWords.lastIndex)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isWSelected) Color(0xFF635BFF) else Color(0xFF191826),
                        border = BorderStroke(
                            1.dp,
                            if (w.sfx != "None") AppColors.AccentGreen else Color(0xFF28263B)
                        ),
                        modifier = Modifier
                            .clickable { onSelectWordIndex(idx) }
                            .testTag("sfx_word_chip_$idx")
                    ) {
                        Text(
                            text = if (w.sfx != "None") "${w.text} ♪" else w.text,
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Horizontal Row of Chunky Colored Waveform Pill Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickStripPresets.forEach { (categoryKey, displayLabel, waveColor) ->
                val isSel = if (isWordMode && activeWord != null) {
                    activeWord.sfx.equals(categoryKey, ignoreCase = true) ||
                        activeWord.sfx.equals(displayLabel, ignoreCase = true)
                } else {
                    effects.sfx.equals(categoryKey, ignoreCase = true) ||
                        effects.sfx.equals(displayLabel, ignoreCase = true)
                }
                val matchingPreset = sfxPlayer.presets.firstOrNull {
                    it.category.equals(categoryKey, ignoreCase = true)
                } ?: sfxPlayer.presets.first()

                Surface(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onSelectSfx(categoryKey)
                            if (categoryKey != "None") {
                                onAddPresetClipAtPlayhead(matchingPreset)
                            }
                        }
                        .testTag("sfx_card_${displayLabel.lowercase()}"),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF191826),
                    border = BorderStroke(
                        width = if (isSel) 2.dp else 1.dp,
                        color = if (isSel) Color(0xFF6366F1) else Color(0xFF28263B)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (categoryKey == "None") {
                            Icon(
                                imageVector = Icons.Default.VolumeOff,
                                contentDescription = "None",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "None",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = displayLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            val bars = matchingPreset.waveformBars.take(9)
                            Canvas(
                                modifier = Modifier
                                    .width(34.dp)
                                    .height(22.dp)
                            ) {
                                val step = size.width / bars.size.coerceAtLeast(1)
                                val centerY = size.height / 2f
                                bars.forEachIndexed { idx, amp ->
                                    val h = (amp.coerceIn(0.2f, 1f) * size.height * 0.9f)
                                    drawLine(
                                        color = waveColor,
                                        start = Offset(idx * step + step / 2f, centerY - h / 2f),
                                        end = Offset(idx * step + step / 2f, centerY + h / 2f),
                                        strokeWidth = 4.5f
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Volume Slider Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1F1D30)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "SFX Volume",
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = effects.sfxVolume.toFloat(),
                onValueChange = { vol -> onUpdateEffects { it.copy(sfxVolume = vol.roundToInt()) } },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF4F46E5),
                    inactiveTrackColor = Color(0xFF262438)
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${effects.sfxVolume}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

/**
 * 7. POSITION & TIMING TRAY (Matches Video 1 01:12–01:21: "Always one line" + Alignment + "Show captions a little early")
 */
@Composable
private fun TimingAndPositionTray(
    style: SubtitleStyle,
    selectedCue: CaptionCue?,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onEditCue: (CaptionCue) -> Unit,
    onSplitCue: () -> Unit,
    onMergeCue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Changes apply to this caption track",
            fontSize = 11.sp,
            color = Color(0xFF7C7A92)
        )

        // Card 1: "Always one line" + Alignment (Video 1 01:12)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF191826),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Always one line",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Never wrap. Long lines shrink to fit instead.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Switch(
                    checked = style.oneLine,
                    onCheckedChange = { enabled -> onUpdateStyle { it.copy(oneLine = enabled) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4F46E5))
                )
            }
        }

        // Card 2: "Show captions a little early" (Video 1 01:17)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF191826),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show captions a little early",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Easier to read: each word shows 0.3 s before it's spoken.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Switch(
                    checked = style.early >= 0.2,
                    onCheckedChange = { enabled ->
                        onUpdateStyle { it.copy(early = if (enabled) 0.3 else 0.0) }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4F46E5))
                )
            }
        }

        // 3-Segment Alignment Selector (Left | Center | Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Text", fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(36.dp))
            listOf(
                "left" to Icons.Default.FormatAlignLeft,
                "center" to Icons.Default.FormatAlignCenter,
                "right" to Icons.Default.FormatAlignRight
            ).forEach { (alignVal, alignIcon) ->
                val isSel = style.align == alignVal
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onUpdateStyle { it.copy(align = alignVal) } },
                    color = if (isSel) Color(0xFF2D2B42) else Color(0xFF191826),
                    border = BorderStroke(1.dp, if (isSel) Color.White else Color(0xFF28263B))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = alignIcon,
                            contentDescription = alignVal,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 8. TEXT FLOW & CLIPS TRAY (Matches Video 1 01:22–01:26: Words per line chunk slider + Split/Merge/SRT)
 */
@Composable
private fun TextFlowAndClipsTray(
    project: Project,
    selectedCue: CaptionCue?,
    onSplitVideoClip: () -> Unit,
    onCycleTransition: (String) -> Unit,
    onAddCue: () -> Unit,
    onEditCue: (CaptionCue) -> Unit,
    onSplitCue: () -> Unit,
    onMergeCue: () -> Unit,
    onDeleteCue: () -> Unit,
    onImportSrt: () -> Unit
) {
    var wordsChunk by remember { mutableFloatStateOf(2f) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Text flow & Clips",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF191826),
                border = BorderStroke(1.dp, Color(0xFF2E2B46)),
                modifier = Modifier.clickable { onImportSrt() }
            ) {
                Text(
                    text = "Import .SRT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA5B4FC),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Words per chunk slider ("1 — 2 — 3 — 4 — Whole" like Video 1 01:23)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text("2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("3", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text("4", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text("Whole", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
            Slider(
                value = wordsChunk,
                onValueChange = { wordsChunk = it },
                valueRange = 1f..5f,
                steps = 3,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF4F46E5),
                    inactiveTrackColor = Color(0xFF262438)
                )
            )
        }

        // Quick Clip & Cue Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onAddCue, modifier = Modifier.testTag("tray_add_cue_btn")) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Cue", fontSize = 11.sp)
            }
            OutlinedButton(onClick = onSplitVideoClip, modifier = Modifier.testTag("tray_split_video_btn")) {
                Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Split Video", fontSize = 11.sp)
            }
            if (selectedCue != null) {
                OutlinedButton(onClick = { onEditCue(selectedCue) }) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Words", fontSize = 11.sp)
                }
                OutlinedButton(onClick = onSplitCue) {
                    Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Split Cue", fontSize = 11.sp)
                }
                OutlinedButton(onClick = onMergeCue) {
                    Icon(Icons.AutoMirrored.Filled.MergeType, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Merge", fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * 9. OVERLAY TRAY (Matches Video 1 01:27–01:34: None / Smart / Bottom / Deep / Full / Top Cards + VFX Layers)
 */
@Composable
private fun OverlaysTray(
    project: Project,
    style: SubtitleStyle,
    selectedOverlay: GraphicOverlay?,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onSelectOverlay: (GraphicOverlay) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onDeleteOverlay: (String) -> Unit,
    onEditOverlay: (GraphicOverlay) -> Unit,
    onAddOverlayClick: () -> Unit
) {
    val scrimModes = listOf("None", "Smart", "Bottom", "Deep", "Full", "Top")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Changes apply to this caption track",
                fontSize = 11.sp,
                color = Color(0xFF7C7A92)
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF4F46E5),
                modifier = Modifier
                    .clickable { onAddOverlayClick() }
                    .testTag("add_overlay_layer_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add VFX Layer (${project.overlays.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Scrim Thumbnail Cards (None, Smart, Bottom, Deep, Full, Top — exact Video 1 01:28 look)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            scrimModes.forEach { mode ->
                val isSel = style.overlay.equals(mode, ignoreCase = true)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        onUpdateStyle { it.copy(overlay = mode, bgEnabled = mode != "None") }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 62.dp, height = 74.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    when (mode) {
                                        "None" -> listOf(Color(0xFF1F1D2E), Color(0xFF1F1D2E))
                                        "Top" -> listOf(Color(0xFF090812), Color(0xFFD8D6E8))
                                        "Full" -> listOf(Color(0xFF3E3B5B), Color(0xFF2B2845))
                                        else -> listOf(Color(0xFFD8D6E8), Color(0xFF1E1B36))
                                    }
                                )
                            )
                            .border(
                                width = if (isSel) 2.dp else 1.dp,
                                color = if (isSel) Color(0xFF4F46E5) else Color(0xFF2E2B46),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .width(28.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF4F46E5))
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mode,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Color.White else Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

/**
 * 10. TRANSLATE TRAY (Matches Video 1 00:22–00:34: Simultaneous vs Replace + Multi-language chips)
 */
@Composable
private fun TranslateTray(
    project: Project,
    onOpenTranslateSheet: () -> Unit,
    onImportSrt: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Translate captions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Replace active track or add a Simultaneous bilingual row above.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenTranslateSheet,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("open_translate_modal_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
            ) {
                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Translate Captions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onImportSrt,
                modifier = Modifier.height(46.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Import .SRT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

/**
 * 11. VIDEO COLOR GRADING TRAY (Matches Video 1 01:35–01:42: Original, Auto, Vivid, Warm, Cool, Matte, Punch, Bright, Mono + Slider)
 */
@Composable
private fun FiltersTray(
    effects: VideoEffects,
    onUpdateEffects: ((VideoEffects) -> VideoEffects) -> Unit
) {
    var selectedLook by remember { mutableStateOf("Original") }
    val looks = listOf(
        Triple("Original", 0.0 to 1.0, 1.0),
        Triple("Auto", 0.03 to 1.08, 1.12),
        Triple("Vivid", 0.02 to 1.15, 1.35),
        Triple("Warm", 0.04 to 1.06, 1.22),
        Triple("Cool", 0.0 to 1.08, 0.88),
        Triple("Matte", 0.06 to 0.92, 0.85),
        Triple("Punch", 0.0 to 1.28, 1.18),
        Triple("Bright", 0.12 to 1.05, 1.08),
        Triple("Mono", 0.0 to 1.15, 0.0)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Color", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF4F46E5)) {
                        Text(
                            text = "PRO",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text("As shot. Pick a look or fine-tune it.", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
            IconButton(
                onClick = {
                    selectedLook = "Original"
                    onUpdateEffects { it.copy(brightness = 0.0, contrast = 1.0, saturation = 1.0) }
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Color", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            }
        }

        // Look Thumbnail Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            looks.forEach { (name, bc, sat) ->
                val isSel = selectedLook == name
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        selectedLook = name
                        onUpdateEffects {
                            it.copy(brightness = bc.first, contrast = bc.second, saturation = sat)
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when (name) {
                                    "Mono" -> Brush.linearGradient(listOf(Color.DarkGray, Color.LightGray))
                                    "Vivid" -> AppColors.MagicBrandGradient
                                    "Warm" -> AppColors.SolarKineticGradient
                                    "Cool" -> AppColors.CyberHologramGradient
                                    else -> Brush.linearGradient(listOf(Color(0xFF28243E), Color(0xFF4F46E5)))
                                }
                            )
                            .border(
                                width = if (isSel) 2.dp else 1.dp,
                                color = if (isSel) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = name,
                        fontSize = 10.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Color.White else Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Intensity / Contrast Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Intensity", fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(56.dp))
            Slider(
                value = (effects.contrast * 100.0).toFloat().coerceIn(50f, 150f),
                onValueChange = { v -> onUpdateEffects { it.copy(contrast = v / 100.0) } },
                valueRange = 50f..150f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF4F46E5),
                    inactiveTrackColor = Color(0xFF262438)
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(effects.contrast * 100).roundToInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.width(38.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * 12. CLEAR VOICE TRAY (Matches Video 1 01:43–02:10: [Off | Light | Balanced ✦ | Strong] + Suggested Badge + Hold to compare)
 */
@Composable
private fun ClearVoiceTray(
    effects: VideoEffects,
    onUpdateEffects: ((VideoEffects) -> VideoEffects) -> Unit
) {
    val levels = listOf("Off", "Light", "Balanced", "Strong")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Clear voice",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF4F46E5)) {
                    Text(
                        text = "PRO",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = "Some background noise • Cleans room noise and keeps your voice natural.",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }

        // 4-Segment Selector: Off | Light | Balanced ✦ | Strong
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF191826),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                levels.forEach { lvl ->
                    val isSel = effects.clearVoice.equals(lvl, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { onUpdateEffects { it.copy(clearVoice = lvl) } },
                        color = if (isSel) Color(0xFF4F46E5) else Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (lvl == "Balanced") "Balanced ✦" else lvl,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Bottom Row: "✦ Suggested: Balanced" + "Hold to compare" pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (effects.clearVoice) {
                    "Strong" -> "For street, wind and crowded rooms."
                    "Balanced" -> "Clears room noise and keeps your voice natural."
                    "Light" -> "Gentle noise reduction for indoor recordings."
                    else -> "Your sound stays as recorded."
                },
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.weight(1f)
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF1D1B30),
                border = BorderStroke(1.dp, Color(0xFF3E347A))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Suggested: Balanced",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5B4FC)
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplatesTray(
    onApplyTemplate: (String) -> Unit
) {
    val templates = listOf(
        Triple("video1_persian_orange", "Ref Video 1 • Persian 3-Tier Kinetic", "3D Orange Headline + Word SFX"),
        Triple("video2_vfx_showcase", "Ref Video 2 • Subject Depth & VFX", "Behind-Subject Text + Hologram"),
        Triple("hormozi_reel", "Creator Viral Pop", "High-contrast Karaoke + Pop SFX"),
        Triple("neon_cyber", "Cyberpunk Neon Glow", "Orbitron + Electric Pulse")
    )
    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        templates.forEach { (id, title, desc) ->
            Surface(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onApplyTemplate(id) },
                color = Color(0xFF191826),
                border = BorderStroke(1.dp, Color(0xFF3E347A))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(shape = RoundedCornerShape(5.dp), color = Color(0xFF4F46E5)) {
                        Text(
                            text = "STUDIO TEMPLATE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Column {
                        Text(title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(desc, fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatTray(
    project: Project,
    onSelectAspectRatio: (String) -> Unit,
    onSelectResolution: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Aspect Ratio:", fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(90.dp))
            listOf("9:16", "1:1", "16:9").forEach { ratio ->
                FilterChip(
                    selected = project.aspectRatio == ratio,
                    onClick = { onSelectAspectRatio(ratio) },
                    label = { Text(ratio, fontWeight = FontWeight.Bold) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Resolution:", fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.width(90.dp))
            listOf(480, 720, 1080).forEach { res ->
                FilterChip(
                    selected = project.resolution == res,
                    onClick = { onSelectResolution(res) },
                    label = { Text("${res}p", fontWeight = FontWeight.Bold) }
                )
            }
        }
    }
}

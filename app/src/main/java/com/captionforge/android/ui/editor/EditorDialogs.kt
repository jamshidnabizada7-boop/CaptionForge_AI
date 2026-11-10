package com.captionforge.android.ui.editor

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.captionforge.android.model.AudioClip
import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.GraphicOverlay
import com.captionforge.android.model.OverlayKeyframe
import com.captionforge.android.model.Project
import com.captionforge.android.model.SrtParser
import com.captionforge.android.model.WordTiming
import com.captionforge.android.ui.theme.AppColors
import com.captionforge.android.ui.theme.parseHexColor
import com.captionforge.android.viewmodel.ExportUiState
import java.util.Locale

@Composable
fun EditCueDialog(
    cue: CaptionCue,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double) -> Unit,
    onSaveFullCue: ((CaptionCue) -> Unit)? = null
) {
    var text by remember(cue) { mutableStateOf(cue.text) }
    var startStr by remember(cue) { mutableStateOf(String.format(Locale.US, "%.2f", cue.start)) }
    var endStr by remember(cue) { mutableStateOf(String.format(Locale.US, "%.2f", cue.end)) }
    var tier1Text by remember(cue) { mutableStateOf(cue.tier1Text) }
    var tier2Text by remember(cue) { mutableStateOf(cue.tier2Text) }
    var tier3Text by remember(cue) { mutableStateOf(cue.tier3Text) }
    var entranceSfx by remember(cue) { mutableStateOf(cue.entranceSfx) }

    val wordsList = remember(cue) {
        val initial = if (cue.words.isNotEmpty()) cue.words else SrtParser.buildWordTimings(cue.text, cue.start, cue.end)
        mutableStateListOf(*initial.toTypedArray())
    }
    var selectedWordIndex by remember(cue) { mutableIntStateOf(0) }

    val wordColors = listOf(null, "#FFFFFF", "#FF7A00", "#FACC15", "#38BDF8", "#10B981", "#F43F5E")
    val wordSfxOptions = listOf("None", "Punchy Pop", "Soft Pop", "Text Click", "Fast Whoosh", "Heavy Impact", "Bass Drop", "Sparkle")
    val wordAnimOptions = listOf("None", "Pop", "Bounce", "Glow", "Slide")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.SurfaceElevated,
        title = { Text("Edit Caption, 3-Tier & Word Styling", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { newText ->
                        text = newText
                        val s = startStr.toDoubleOrNull() ?: cue.start
                        val e = endStr.toDoubleOrNull() ?: cue.end
                        val rebuilt = SrtParser.buildWordTimings(newText, s, e, wordsList.toList())
                        wordsList.clear()
                        wordsList.addAll(rebuilt)
                        if (selectedWordIndex >= wordsList.size) selectedWordIndex = 0
                    },
                    label = { Text("Subtitle Text (RTL & LTR supported)") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_cue_text_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startStr,
                        onValueChange = { startStr = it },
                        label = { Text("Start (s)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endStr,
                        onValueChange = { endStr = it },
                        label = { Text("End (s)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3-Tier Reference Video 1 Override Fields
                Text(
                    text = "3-Tier Reference Video 1 Lines (Optional explicit split):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.AccentOrange
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = tier1Text,
                        onValueChange = { tier1Text = it },
                        label = { Text("Tier 1 (Top)", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tier2Text,
                        onValueChange = { tier2Text = it },
                        label = { Text("Tier 2 (Orange Hero)", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tier3Text,
                        onValueChange = { tier3Text = it },
                        label = { Text("Tier 3 (Bottom)", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Entrance SFX
                Text("Cue Entrance SFX:", fontSize = 11.sp, color = AppColors.TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    wordSfxOptions.forEach { sfxOpt ->
                        FilterChip(
                            selected = entranceSfx.equals(sfxOpt, ignoreCase = true),
                            onClick = { entranceSfx = sfxOpt },
                            label = { Text(sfxOpt, fontSize = 10.sp) }
                        )
                    }
                }

                HorizontalDivider(color = AppColors.SurfaceBorder)

                // Word-by-Word Inspector
                if (wordsList.isNotEmpty()) {
                    Text(
                        text = "Tap a Word to Customize Color, Scale, Animation & SFX:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        wordsList.forEachIndexed { idx, w ->
                            val isSel = idx == selectedWordIndex
                            val chipCol = w.customColor?.let { parseHexColor(it) } ?: AppColors.PrimaryPurpleLight
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedWordIndex = idx }
                                    .testTag("word_chip_$idx"),
                                color = if (isSel) chipCol.copy(alpha = 0.28f) else AppColors.Surface,
                                border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) Color.White else chipCol)
                            ) {
                                Text(
                                    text = "${w.text}${if (w.sfx != "None") " ♪" else ""}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    val activeWord = wordsList.getOrNull(selectedWordIndex)
                    if (activeWord != null) {
                        var wordTextStr by remember(selectedWordIndex, activeWord.text) {
                            mutableStateOf(activeWord.text)
                        }
                        var wordStartStr by remember(selectedWordIndex, activeWord.start) {
                            mutableStateOf(String.format(Locale.US, "%.2f", activeWord.start))
                        }
                        var wordEndStr by remember(selectedWordIndex, activeWord.end) {
                            mutableStateOf(String.format(Locale.US, "%.2f", activeWord.end))
                        }

                        Text(
                            text = "Word \"${activeWord.text}\" (${String.format(Locale.US, "%.2f", activeWord.start)}s–${String.format(Locale.US, "%.2f", activeWord.end)}s):",
                            fontSize = 11.sp,
                            color = AppColors.AccentCyan
                        )

                        // Editable Word Text + Word Start (s) + Word End (s)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = wordTextStr,
                                onValueChange = { newWText ->
                                    wordTextStr = newWText
                                    if (newWText.isNotBlank()) {
                                        wordsList[selectedWordIndex] = activeWord.copy(text = newWText.trim())
                                        text = wordsList.joinToString(" ") { it.text }
                                    }
                                },
                                label = { Text("Word", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("edit_word_text_input")
                            )
                            OutlinedTextField(
                                value = wordStartStr,
                                onValueChange = { newWStart ->
                                    wordStartStr = newWStart
                                    newWStart.toDoubleOrNull()?.let { parsed ->
                                        wordsList[selectedWordIndex] = activeWord.copy(start = parsed.coerceAtLeast(0.0))
                                    }
                                },
                                label = { Text("W.Start", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("edit_word_start_input")
                            )
                            OutlinedTextField(
                                value = wordEndStr,
                                onValueChange = { newWEnd ->
                                    wordEndStr = newWEnd
                                    newWEnd.toDoubleOrNull()?.let { parsed ->
                                        wordsList[selectedWordIndex] = activeWord.copy(
                                            end = parsed.coerceAtLeast(activeWord.start + 0.05)
                                        )
                                    }
                                },
                                label = { Text("W.End", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("edit_word_end_input")
                            )
                        }

                        // Per-word Custom Color Swatches
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Word Color:", fontSize = 11.sp, color = AppColors.TextSecondary)
                            wordColors.forEach { hex ->
                                val isSel = activeWord.customColor == hex
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (hex == null) AppColors.Surface else parseHexColor(hex))
                                        .border(
                                            width = if (isSel) 2.5.dp else 1.dp,
                                            color = if (isSel) Color.White else AppColors.SurfaceBorder,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            wordsList[selectedWordIndex] = activeWord.copy(customColor = hex)
                                        }
                                        .testTag("word_color_${hex ?: "default"}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hex == null) {
                                        Text("A", fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        }

                        // Per-word Scale Boost
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Scale Boost: ${String.format(Locale.US, "%.2f", activeWord.scaleBoost)}x",
                                fontSize = 11.sp,
                                color = AppColors.TextSecondary,
                                modifier = Modifier.width(115.dp)
                            )
                            Slider(
                                value = activeWord.scaleBoost.toFloat(),
                                onValueChange = { sc ->
                                    wordsList[selectedWordIndex] = activeWord.copy(scaleBoost = sc.toDouble())
                                },
                                valueRange = 0.8f..1.6f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Per-word Animation
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Anim:", fontSize = 10.sp, color = AppColors.TextSecondary)
                            wordAnimOptions.forEach { animOpt ->
                                FilterChip(
                                    selected = activeWord.animation.equals(animOpt, ignoreCase = true),
                                    onClick = {
                                        wordsList[selectedWordIndex] = activeWord.copy(animation = animOpt)
                                    },
                                    label = { Text(animOpt, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("word_anim_${animOpt.lowercase()}")
                                )
                            }
                        }

                        // Per-word Synchronized SFX
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Word SFX:", fontSize = 10.sp, color = AppColors.TextSecondary)
                            wordSfxOptions.forEach { sfxOpt ->
                                FilterChip(
                                    selected = activeWord.sfx.equals(sfxOpt, ignoreCase = true),
                                    onClick = {
                                        wordsList[selectedWordIndex] = activeWord.copy(sfx = sfxOpt)
                                    },
                                    label = { Text(sfxOpt, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("word_sfx_${sfxOpt.lowercase().replace(" ", "_")}")
                                )
                            }
                        }

                        // Per-word Tier Assignment
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("3-Tier Line:", fontSize = 10.sp, color = AppColors.TextSecondary)
                            listOf(0 to "Auto", 1 to "Tier 1 Top", 2 to "Tier 2 Hero", 3 to "Tier 3 Bottom").forEach { (tCode, lbl) ->
                                FilterChip(
                                    selected = activeWord.tier == tCode,
                                    onClick = {
                                        wordsList[selectedWordIndex] = activeWord.copy(tier = tCode)
                                    },
                                    label = { Text(lbl, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = startStr.toDoubleOrNull() ?: cue.start
                    val e = endStr.toDoubleOrNull() ?: cue.end
                    if (text.isNotBlank() && e > s) {
                        if (onSaveFullCue != null) {
                            onSaveFullCue(
                                cue.copy(
                                    text = text.trim(),
                                    start = s,
                                    end = e,
                                    tier1Text = tier1Text.trim(),
                                    tier2Text = tier2Text.trim(),
                                    tier3Text = tier3Text.trim(),
                                    entranceSfx = entranceSfx,
                                    words = wordsList.toList()
                                )
                            )
                        } else {
                            onSave(text, s, e)
                        }
                    }
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditAudioClipDialog(
    clip: AudioClip,
    onDismiss: () -> Unit,
    onSave: (AudioClip) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember(clip) { mutableStateOf(clip.name) }
    var startStr by remember(clip) { mutableStateOf(String.format(Locale.US, "%.2f", clip.start)) }
    var durStr by remember(clip) { mutableStateOf(String.format(Locale.US, "%.2f", clip.duration)) }
    var volume by remember(clip) { mutableFloatStateOf(clip.volume.toFloat()) }
    var fadeIn by remember(clip) { mutableFloatStateOf(clip.fadeIn.toFloat()) }
    var fadeOut by remember(clip) { mutableFloatStateOf(clip.fadeOut.toFloat()) }
    var loop by remember(clip) { mutableStateOf(clip.loop) }
    var ducking by remember(clip) { mutableStateOf(clip.ducking) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.SurfaceElevated,
        title = { Text("Edit Sound / Music Clip", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Clip Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startStr,
                        onValueChange = { startStr = it },
                        label = { Text("Start Time (s)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durStr,
                        onValueChange = { durStr = it },
                        label = { Text("Duration (s)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Volume (${(volume * 100).toInt()}%)", fontSize = 11.sp, color = AppColors.TextSecondary)
                Slider(value = volume, onValueChange = { volume = it }, valueRange = 0.05f..1.5f)

                Text("Fade-In (${String.format(Locale.US, "%.2f", fadeIn)}s) & Fade-Out (${String.format(Locale.US, "%.2f", fadeOut)}s)", fontSize = 11.sp, color = AppColors.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Slider(value = fadeIn, onValueChange = { fadeIn = it }, valueRange = 0f..1.0f, modifier = Modifier.weight(1f))
                    Slider(value = fadeOut, onValueChange = { fadeOut = it }, valueRange = 0f..1.0f, modifier = Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Loop Clip Across Duration", fontSize = 12.sp, color = Color.White)
                    Switch(checked = loop, onCheckedChange = { loop = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto Voice Ducking (-12 dB during speech)", fontSize = 12.sp, color = Color.White)
                    Switch(checked = ducking, onCheckedChange = { ducking = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = startStr.toDoubleOrNull() ?: clip.start
                    val d = (durStr.toDoubleOrNull() ?: clip.duration).coerceAtLeast(0.1)
                    onSave(
                        clip.copy(
                            name = name.ifBlank { clip.name },
                            start = s.coerceAtLeast(0.0),
                            duration = d,
                            volume = volume.toDouble(),
                            fadeIn = fadeIn.toDouble(),
                            fadeOut = fadeOut.toDouble(),
                            loop = loop,
                            ducking = ducking
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onDelete(clip.id)
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = AppColors.AccentCoral)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun EditOverlayAndKeyframesDialog(
    overlay: GraphicOverlay,
    currentTime: Double,
    onDismiss: () -> Unit,
    onSave: (GraphicOverlay) -> Unit
) {
    var name by remember(overlay) { mutableStateOf(overlay.name) }
    var content by remember(overlay) { mutableStateOf(overlay.content) }
    var startStr by remember(overlay) { mutableStateOf(String.format(Locale.US, "%.2f", overlay.start)) }
    var endStr by remember(overlay) { mutableStateOf(String.format(Locale.US, "%.2f", overlay.end)) }
    var xPos by remember(overlay) { mutableFloatStateOf(overlay.xPos.toFloat()) }
    var yPos by remember(overlay) { mutableFloatStateOf(overlay.yPos.toFloat()) }
    var scale by remember(overlay) { mutableFloatStateOf(overlay.scale.toFloat()) }
    var isBackground by remember(overlay) { mutableStateOf(overlay.isBackground) }
    val keyframes = remember(overlay) { mutableStateListOf(*overlay.keyframes.toTypedArray()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.SurfaceElevated,
        title = { Text("Edit Layer & Keyframes", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Layer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Display Text / Content") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startStr,
                        onValueChange = { startStr = it },
                        label = { Text("Start (s)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endStr,
                        onValueChange = { endStr = it },
                        label = { Text("End (s)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Position X (${(xPos * 100).toInt()}%) & Y (${(yPos * 100).toInt()}%)", fontSize = 11.sp, color = AppColors.TextSecondary)
                Slider(value = xPos, onValueChange = { xPos = it }, valueRange = 0.1f..0.9f)
                Slider(value = yPos, onValueChange = { yPos = it }, valueRange = 0.1f..0.9f)

                Text("Scale (${String.format(Locale.US, "%.2f", scale)}x)", fontSize = 11.sp, color = AppColors.TextSecondary)
                Slider(value = scale, onValueChange = { scale = it }, valueRange = 0.5f..2.0f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Depth Mask: Behind Subject", fontSize = 12.sp, color = Color.White)
                    Switch(checked = isBackground, onCheckedChange = { isBackground = it })
                }

                HorizontalDivider(color = AppColors.SurfaceBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Keyframes (${keyframes.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(
                        onClick = {
                            val relT = (currentTime - overlay.start).coerceIn(0.0, overlay.duration)
                            keyframes.add(
                                OverlayKeyframe(
                                    time = relT,
                                    xPos = xPos.toDouble(),
                                    yPos = yPos.toDouble(),
                                    scale = scale.toDouble(),
                                    easing = "easeInOut",
                                    color = overlay.color
                                )
                            )
                            keyframes.sortBy { it.time }
                        }
                    ) {
                        Text("+ Add at Playhead", fontSize = 11.sp)
                    }
                }

                keyframes.forEachIndexed { idx, kf ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", kf.time)}s • (${String.format(Locale.US, "%.2f", kf.xPos)}, ${String.format(Locale.US, "%.2f", kf.yPos)}) • ${String.format(Locale.US, "%.1f", kf.scale)}x",
                            fontSize = 11.sp,
                            color = AppColors.TextSecondary
                        )
                        IconButton(
                            onClick = { keyframes.removeAt(idx) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove keyframe",
                                tint = AppColors.AccentCoral,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = startStr.toDoubleOrNull() ?: overlay.start
                    val e = endStr.toDoubleOrNull() ?: overlay.end
                    onSave(
                        overlay.copy(
                            name = name.ifBlank { overlay.name },
                            content = content,
                            start = s,
                            end = maxOf(s + 0.2, e),
                            xPos = xPos.toDouble(),
                            yPos = yPos.toDouble(),
                            scale = scale.toDouble(),
                            isBackground = isBackground,
                            depthLayer = if (isBackground) 0 else 2,
                            keyframes = keyframes.toList()
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOverlayBottomSheet(
    onDismiss: () -> Unit,
    onAddLayer: (type: String, name: String, content: String, color: String) -> Unit
) {
    val presets = listOf(
        listOf("glow_hologram", "Glow Hologram", "◈ HOLOGRAM ◈", "#38BDF8"),
        listOf("metallic_banner", "Metallic Australia Reveal", "AUSTRALIA", "#E2E8F0"),
        listOf("persian_orange_headline", "Persian Orange Headline", "فنلاند", "#FF7A00"),
        listOf("light_leak", "Purple Light Flare", "✦ LIGHT FLARE ✦", "#D946EF"),
        listOf("flag_graphic", "Waving Flag Behind Subject", "⚑ MALAYSIA FLAG ⚑", "#F59E0B"),
        listOf("soft_bg_text", "Soft Persian Background Words", "کپشن فورج شناور", "#94A3B8"),
        listOf("end_accent", "End Studio Accent", "CaptionForge Studio", "#EF4444")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.SurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Add Cinematic VFX / Graphic Layer",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            presets.forEach { (type, name, content, color) ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onAddLayer(type, name, content, color)
                            onDismiss()
                        },
                    color = AppColors.Surface,
                    border = BorderStroke(1.dp, parseHexColor(color).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text(text = content, color = AppColors.TextSecondary, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(color))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackManagerBottomSheet(
    project: Project,
    activeTrackIndex: Int,
    onSelectTrack: (Int) -> Unit,
    onAddTrack: (String, String) -> Unit,
    onToggleVisibility: (Int) -> Unit,
    onToggleLock: (Int) -> Unit,
    onDeleteTrack: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.SurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subtitle Tracks Manager", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Button(
                    onClick = { onAddTrack("Track ${project.tracks.size + 1}", "fa") },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryPurple)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Track", fontSize = 12.sp)
                }
            }

            project.tracks.forEachIndexed { idx, track ->
                val isSelected = idx == activeTrackIndex
                val trackColor = AppColors.TrackColors[idx % AppColors.TrackColors.size]

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onSelectTrack(idx)
                            onDismiss()
                        },
                    color = if (isSelected) AppColors.Surface else AppColors.SurfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) trackColor else AppColors.SurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(trackColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${track.name} (${track.language.uppercase()})",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${track.captions.size} cues • Font: ${track.style.font}",
                                fontSize = 11.sp,
                                color = AppColors.TextSecondary
                            )
                        }
                        IconButton(onClick = { onToggleVisibility(idx) }) {
                            Icon(
                                imageVector = if (track.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Visibility",
                                tint = if (track.visible) Color.White else AppColors.TextMuted
                            )
                        }
                        IconButton(onClick = { onToggleLock(idx) }) {
                            Icon(
                                imageVector = if (track.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock",
                                tint = if (track.locked) AppColors.AccentAmber else AppColors.TextSecondary
                            )
                        }
                        if (project.tracks.size > 1) {
                            IconButton(onClick = { onDeleteTrack(idx) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Track",
                                    tint = AppColors.AccentCoral
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun AutoAiTranscriptionDialog(
    isBackendOnline: Boolean,
    onDismiss: () -> Unit,
    onConfirmTranscribe: (language: String?, model: String) -> Unit
) {
    var selectedLang by remember { mutableStateOf("auto") }
    var selectedModel by remember { mutableStateOf("small") }

    val langs = listOf(
        "auto" to "Auto-Detect",
        "en" to "English",
        "fa" to "Persian / Dari",
        "ps" to "Pashto",
        "ur" to "Urdu",
        "ar" to "Arabic"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.SurfaceElevated,
        title = { Text("Auto AI Speech Subtitles", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isBackendOnline) {
                        "Faster-Whisper on your FastAPI backend will extract audio, detect spoken language, and generate word-level synchronized captions."
                    } else {
                        "Note: Your FastAPI server is currently unreachable. Connect to your FastAPI backend in Settings for live Faster-Whisper ASR, or import an .SRT file."
                    },
                    fontSize = 12.sp,
                    color = if (isBackendOnline) AppColors.TextSecondary else AppColors.AccentAmber
                )
                Text("Spoken Language:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    langs.take(3).forEach { (code, name) ->
                        FilterChip(
                            selected = selectedLang == code,
                            onClick = { selectedLang = code },
                            label = { Text(name, fontSize = 11.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    langs.drop(3).forEach { (code, name) ->
                        FilterChip(
                            selected = selectedLang == code,
                            onClick = { selectedLang = code },
                            label = { Text(name, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmTranscribe(if (selectedLang == "auto") null else selectedLang, selectedModel)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryPurple)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Transcribe Video")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateBottomSheet(
    isBackendOnline: Boolean,
    onDismiss: () -> Unit,
    onTranslate: (targetLang: String, mode: String, provider: String) -> Unit
) {
    val languages = listOf(
        "en" to "English",
        "fa" to "Persian (Farsi / Dari)",
        "ps" to "Pashto (پښتو)",
        "ur" to "Urdu (اردو)",
        "ar" to "Arabic (العربية)",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "tr" to "Turkish"
    )
    var selectedLang by remember { mutableStateOf("en") }
    var selectedMode by remember { mutableStateOf("Simultaneous") }
    var selectedProvider by remember { mutableStateOf("auto") }

    val selectedLangLabel = languages.firstOrNull { it.first == selectedLang }?.second?.substringBefore(" (") ?: "English"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111119)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF635BFF).copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("文A", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF818CF8))
                        }
                    }
                    Column {
                        Text("Translate captions", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Generate captions in another language", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                }
            }

            // Step 1: How should the translation show?
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF635BFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("How should the translation show?", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Replace
                val isReplace = selectedMode == "Replace"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedMode = "Replace" },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isReplace) Color(0xFF1E1B3A) else Color(0xFF171722),
                    border = BorderStroke(if (isReplace) 2.dp else 1.dp, if (isReplace) Color(0xFF635BFF) else Color(0xFF262636))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0D0D14)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF635BFF)
                                ) {
                                    Text(
                                        text = "Translated text",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Replace", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Only translated captions", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }

                // Card 2: Simultaneous
                val isSimultaneous = selectedMode == "Simultaneous"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedMode = "Simultaneous" },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSimultaneous) Color(0xFF1E1B3A) else Color(0xFF171722),
                    border = BorderStroke(if (isSimultaneous) 2.dp else 1.dp, if (isSimultaneous) Color(0xFF635BFF) else Color(0xFF262636))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0D0D14)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Original text", fontSize = 9.sp, color = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF635BFF)
                                ) {
                                    Text(
                                        text = "Translated text",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Simultaneous", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Show both languages", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }
            }

            // Step 2: Into which language?
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF635BFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("Into which language?", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (code, label) ->
                            val isSel = selectedLang == code
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedLang = code },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) Color(0xFF635BFF) else Color(0xFF171722),
                                border = BorderStroke(1.dp, if (isSel) Color.White.copy(alpha = 0.5f) else Color(0xFF262636))
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label.substringBefore(" ("),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isBackendOnline) {
                Text(
                    text = "Offline mode active: Generates dual-track RTL/LTR translated preview cues immediately.",
                    fontSize = 10.sp,
                    color = AppColors.AccentAmber
                )
            }

            Button(
                onClick = {
                    onTranslate(selectedLang, selectedMode, selectedProvider)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_translate_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF635BFF))
            ) {
                Text(
                    text = "Translate to $selectedLangLabel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    project: Project,
    exportState: ExportUiState,
    onSelectResolution: (Int) -> Unit,
    onExportSrt: () -> Unit,
    onStartMp4Export: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRes by remember(project.resolution) { mutableIntStateOf(project.resolution) }
    var selectedFps by remember { mutableIntStateOf(30) }
    val context = LocalContext.current

    val dimensionsLabel = when (selectedRes) {
        480 -> if (project.aspectRatio == "16:9") "854 × 480" else "480 × 854"
        1080 -> if (project.aspectRatio == "16:9") "1920 × 1080" else "1080 × 1920"
        else -> if (project.aspectRatio == "16:9") "1280 × 720" else "720 × 1280"
    }
    val estimatedMb = ((project.duration.coerceAtLeast(5.0) * (selectedRes / 720.0) * (selectedFps / 30.0) * 0.95).toInt()).coerceAtLeast(4)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111119)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Export settings", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                }
            }

            // Resolution Row + Segmented Pills (matching Video 1 02:15-02:19)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Resolution", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
                Text(dimensionsLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF171722),
                border = BorderStroke(1.dp, Color(0xFF262636))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(480, 720, 1080).forEach { res ->
                        val isSel = selectedRes == res
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable {
                                    selectedRes = res
                                    onSelectResolution(res)
                                },
                            shape = RoundedCornerShape(9.dp),
                            color = if (isSel) Color(0xFF635BFF) else Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${res}p",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSel) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            // Frame Rate Row + Segmented Pills (24 / 25 / 30 fps)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Frame rate", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
                Text("${selectedFps} fps", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF171722),
                border = BorderStroke(1.dp, Color(0xFF262636))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(24, 25, 30).forEach { fps ->
                        val isSel = selectedFps == fps
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { selectedFps = fps },
                            shape = RoundedCornerShape(9.dp),
                            color = if (isSel) Color(0xFF635BFF) else Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$fps",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSel) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            // Estimated file size card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF171722)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Estimated file size", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Text("≈ $estimatedMb MB", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }

            if (exportState.isExporting) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = AppColors.Surface,
                    border = BorderStroke(1.dp, AppColors.SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF635BFF)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = exportState.progressMessage ?: "Rendering MP4...",
                            fontSize = 12.sp,
                            color = AppColors.PrimaryPurpleLight
                        )
                    }
                }
            }

            exportState.error?.let { err ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = AppColors.AccentCoral.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AppColors.AccentCoral)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = AppColors.AccentCoral
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = err,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }

            exportState.downloadUrl?.let { url ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = AppColors.AccentGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AppColors.AccentGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppColors.AccentGreen
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Backend FFmpeg MP4 Ready!", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Tap to download burned-in MP4 from $url", fontSize = 11.sp, color = AppColors.TextSecondary)
                        }
                    }
                }
            }

            exportState.exportedMp4File?.let { mp4File ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exported_mp4_card"),
                    shape = RoundedCornerShape(10.dp),
                    color = AppColors.AccentGreen.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, AppColors.AccentGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppColors.AccentGreen
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "On-Device Media3 MP4 Rendered!",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${mp4File.name} (${mp4File.length() / 1024} KB)\nSaved to: ${mp4File.absolutePath}",
                                fontSize = 11.sp,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }
            }

            exportState.exportedSrtFile?.let { srtFile ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = AppColors.AccentCyan.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, AppColors.AccentCyan)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppColors.AccentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Local SRT saved: ${srtFile.name} (${srtFile.length()} bytes)",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onExportSrt,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("export_srt_button")
                ) {
                    Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export SRT", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onStartMp4Export(selectedRes) },
                    enabled = !exportState.isExporting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.7f)
                        .height(50.dp)
                        .testTag("render_mp4_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF635BFF))
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (exportState.isExporting) "Exporting..." else "Export Video",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

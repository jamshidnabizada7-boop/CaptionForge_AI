package com.captionforge.android.ui.editor

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.GraphicOverlay
import com.captionforge.android.model.OverlayKeyframe
import com.captionforge.android.model.Project
import com.captionforge.android.ui.theme.AppColors
import com.captionforge.android.ui.theme.parseHexColor
import com.captionforge.android.viewmodel.ExportUiState
import java.util.Locale

@Composable
fun EditCueDialog(
    cue: CaptionCue,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double) -> Unit
) {
    var text by remember(cue) { mutableStateOf(cue.text) }
    var startStr by remember(cue) { mutableStateOf(String.format(Locale.US, "%.2f", cue.start)) }
    var endStr by remember(cue) { mutableStateOf(String.format(Locale.US, "%.2f", cue.end)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.SurfaceElevated,
        title = { Text("Edit Caption Cue", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Subtitle Text (RTL & LTR supported)") },
                    maxLines = 4,
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = startStr.toDoubleOrNull() ?: cue.start
                    val e = endStr.toDoubleOrNull() ?: cue.end
                    if (text.isNotBlank() && e > s) {
                        onSave(text, s, e)
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
    var xPos by remember(overlay) { mutableStateOf(overlay.xPos.toFloat()) }
    var yPos by remember(overlay) { mutableStateOf(overlay.yPos.toFloat()) }
    var scale by remember(overlay) { mutableStateOf(overlay.scale.toFloat()) }
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
        "fa" to "Persian (Dari / Farsi)",
        "ps" to "Pashto (پښتو)",
        "ur" to "Urdu (اردو)",
        "ar" to "Arabic (العربية)",
        "en" to "English",
        "es" to "Spanish",
        "fr" to "French"
    )
    var selectedLang by remember { mutableStateOf("fa") }
    var selectedMode by remember { mutableStateOf("Simultaneous") }
    var selectedProvider by remember { mutableStateOf("auto") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.SurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("AI Subtitle Translation & Bilingual RTL", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (!isBackendOnline) {
                Text(
                    text = "Backend is offline: Translating now will generate a clearly labeled Offline Preview RTL track so you can test dual-track Persian/Pashto/Urdu/Arabic styling immediately.",
                    fontSize = 11.sp,
                    color = AppColors.AccentAmber
                )
            }

            Text("Target Language:", fontSize = 12.sp, color = AppColors.TextSecondary)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                languages.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { (code, label) ->
                            FilterChip(
                                selected = selectedLang == code,
                                onClick = { selectedLang = code },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            Text("Track Mode:", fontSize = 12.sp, color = AppColors.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = selectedMode == "Simultaneous",
                    onClick = { selectedMode = "Simultaneous" },
                    label = { Text("Simultaneous (Dual Bilingual Track)", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedMode == "Replace",
                    onClick = { selectedMode = "Replace" },
                    label = { Text("Replace Current Track", fontSize = 11.sp) }
                )
            }

            Button(
                onClick = {
                    onTranslate(selectedLang, selectedMode, selectedProvider)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_translate_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryPurple)
            ) {
                Text("Generate Translated Track", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
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
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.SurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Export Captioned Video", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Text(
                text = "Select Resolution (${project.aspectRatio}):",
                fontSize = 13.sp,
                color = AppColors.TextSecondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(480, 720, 1080).forEach { res ->
                    FilterChip(
                        selected = selectedRes == res,
                        onClick = {
                            selectedRes = res
                            onSelectResolution(res)
                        },
                        label = { Text("${res}p", fontWeight = FontWeight.Bold) }
                    )
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
                            color = AppColors.PrimaryPurple
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
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_srt_button")
                ) {
                    Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export SRT")
                }

                Button(
                    onClick = { onStartMp4Export(selectedRes) },
                    enabled = !exportState.isExporting,
                    modifier = Modifier
                        .weight(1.6f)
                        .height(48.dp)
                        .testTag("render_mp4_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryPurple)
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (exportState.isExporting) "Rendering..." else "Render MP4",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

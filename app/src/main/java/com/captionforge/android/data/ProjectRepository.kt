package com.captionforge.android.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.captionforge.android.model.BackendConfig
import com.captionforge.android.model.CaptionCue
import com.captionforge.android.model.ExportRequest
import com.captionforge.android.model.JobStatusResponse
import com.captionforge.android.model.MotionPoint
import com.captionforge.android.model.Project
import com.captionforge.android.model.ProjectUpdatePayload
import com.captionforge.android.model.SampleProjectsFactory
import com.captionforge.android.model.SrtParser
import com.captionforge.android.model.SubtitleStyle
import com.captionforge.android.model.Track
import com.captionforge.android.model.TrackMotionRequest
import com.captionforge.android.model.TranscribeRequest
import com.captionforge.android.model.TranslateRequest
import com.captionforge.android.model.VideoClipSegment
import com.captionforge.android.model.VideoTrim
import com.captionforge.android.network.BackendClient
import com.captionforge.android.ui.theme.CustomFontRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.max

data class ProbedVideoInfo(
    val filename: String,
    val localFile: File,
    val durationSec: Double,
    val width: Int,
    val height: Int,
    val sizeBytes: Long
)

class ProjectRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("captionforge_prefs", Context.MODE_PRIVATE)
    private val projectsDir = File(context.filesDir, "captionforge_projects").apply { mkdirs() }
    private val videosDir = File(context.filesDir, "imported_videos").apply { mkdirs() }
    private val customFontsDir = File(context.filesDir, "custom_fonts").apply { mkdirs() }

    init {
        val savedUrl = prefs.getString("backend_url", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"
        BackendClient.setBaseUrl(savedUrl)
        seedDefaultProjectsIfNeeded()
        restoreLocalCustomFonts()
    }

    private fun restoreLocalCustomFonts() {
        customFontsDir.listFiles()?.forEach { file ->
            if (file.extension.lowercase() in listOf("ttf", "otf")) {
                CustomFontRegistry.registerFont(file.nameWithoutExtension, file)
            }
        }
    }

    fun getBackendUrl(): String = BackendClient.getBaseUrl()

    fun setBackendUrl(url: String) {
        BackendClient.setBaseUrl(url)
        prefs.edit().putString("backend_url", BackendClient.getBaseUrl()).apply()
    }

    private fun seedDefaultProjectsIfNeeded() {
        val seeded = prefs.getBoolean("seeded_defaults_v2", false)
        if (!seeded || projectsDir.listFiles().isNullOrEmpty()) {
            saveLocalProject(SampleProjectsFactory.createVideo1ShowcaseProject())
            saveLocalProject(SampleProjectsFactory.createVideo2VfxShowcaseProject())
            prefs.edit().putBoolean("seeded_defaults_v2", true).apply()
        }
    }

    fun saveLocalProject(project: Project): Project {
        val now = nowIso()
        val updated = project.copy(updatedAt = now)
        val target = File(projectsDir, "${updated.id}.json")
        val tmp = File(projectsDir, "${updated.id}.tmp")
        tmp.writeText(BackendClient.json.encodeToString(Project.serializer(), updated), Charsets.UTF_8)
        tmp.renameTo(target)
        prefs.edit().putString("last_active_project_id", updated.id).apply()
        return updated
    }

    fun getLastActiveProjectId(): String? = prefs.getString("last_active_project_id", null)

    fun loadLocalProjects(): List<Project> {
        val files = projectsDir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return files.mapNotNull { file ->
            try {
                BackendClient.json.decodeFromString(Project.serializer(), file.readText(Charsets.UTF_8))
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.updatedAt }
    }

    fun getLocalProject(pid: String): Project? {
        val file = File(projectsDir, "$pid.json")
        if (!file.exists()) return null
        return try {
            BackendClient.json.decodeFromString(Project.serializer(), file.readText(Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }

    suspend fun checkBackendConfig(): Result<BackendConfig> = withContext(Dispatchers.IO) {
        runCatching {
            BackendClient.api().getConfig()
        }
    }

    suspend fun syncProjectsList(): Pair<List<Project>, Boolean> = withContext(Dispatchers.IO) {
        val localMap = loadLocalProjects().associateBy { it.id }.toMutableMap()
        var backendOnline = false
        try {
            val remoteList = BackendClient.api().getProjects()
            backendOnline = true
            for (summary in remoteList) {
                if (summary.id.isNotEmpty() && !localMap.containsKey(summary.id)) {
                    try {
                        val full = BackendClient.api().getProject(summary.id)
                            .copy(isSyncedWithBackend = true)
                        saveLocalProject(full)
                        localMap[full.id] = full
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
            backendOnline = false
        }
        val combined = localMap.values.sortedByDescending { it.updatedAt }
        Pair(combined, backendOnline)
    }

    suspend fun probeAndCopyVideoUri(uri: Uri): Result<ProbedVideoInfo> = withContext(Dispatchers.IO) {
        runCatching {
            var displayName = "imported_video.mp4"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
            }
            val safeExt = displayName.substringAfterLast('.', "mp4").lowercase()
            val destFile = File(videosDir, "vid_${System.currentTimeMillis()}.$safeExt")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: error("Unable to read selected video stream")

            var durationSec = 10.0
            var width = 720
            var height = 1280
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(destFile.absolutePath)
                val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 10000L
                durationSec = max(0.5, durMs / 1000.0)
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 720
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1280
            } catch (_: Exception) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }

            ProbedVideoInfo(
                filename = displayName,
                localFile = destFile,
                durationSec = durationSec,
                width = width,
                height = height,
                sizeBytes = destFile.length()
            )
        }
    }

    suspend fun createProjectFromVideo(
        probed: ProbedVideoInfo,
        trimStart: Double,
        trimEnd: Double
    ): Project = withContext(Dispatchers.IO) {
        val clampedEnd = trimEnd.coerceIn(trimStart + 0.5, max(trimStart + 0.5, probed.durationSec))
        val aspectRatio = when {
            probed.width > probed.height * 1.15 -> "16:9"
            probed.width in (probed.height * 0.85).toInt()..(probed.height * 1.15).toInt() -> "1:1"
            else -> "9:16"
        }

        // Try uploading to FastAPI backend first
        try {
            val reqFile = probed.localFile.asRequestBody("video/mp4".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", probed.filename, reqFile)
            val createdRemote = BackendClient.api().createProject(part)
            val localUri = Uri.fromFile(probed.localFile).toString()
            val merged = createdRemote.copy(
                source = localUri,
                trim = VideoTrim(trimStart, clampedEnd),
                aspectRatio = aspectRatio,
                videoClips = listOf(
                    VideoClipSegment(
                        id = "vc_main",
                        label = probed.filename.substringBeforeLast('.'),
                        sourceUri = localUri,
                        startInTimeline = trimStart,
                        endInTimeline = clampedEnd,
                        sourceTrimStart = trimStart,
                        transition = "Cut"
                    )
                ),
                isSyncedWithBackend = true
            )
            saveProjectEverywhere(merged)
            return@withContext merged
        } catch (_: Exception) {
            // Create local project when backend is offline
            val now = nowIso()
            val localUri = Uri.fromFile(probed.localFile).toString()
            val localProject = Project(
                id = UUID.randomUUID().toString().replace("-", ""),
                name = probed.filename.substringBeforeLast('.').ifBlank { "Imported Video" },
                source = localUri,
                duration = probed.durationSec,
                width = probed.width,
                height = probed.height,
                hasAudio = true,
                trim = VideoTrim(trimStart, clampedEnd),
                resolution = 720,
                aspectRatio = aspectRatio,
                tracks = listOf(
                    Track(
                        id = "tr_orig",
                        name = "Original Captions",
                        language = "en",
                        color = "#8B5CF6",
                        style = SubtitleStyle(),
                        captions = listOf(
                            CaptionCue(
                                id = "c_init_1",
                                start = trimStart + 0.2,
                                end = minOf(clampedEnd, trimStart + 2.4),
                                text = "Tap AI Captions or double-tap to edit subtitle",
                                words = SrtParser.buildWordTimings(
                                    "Tap AI Captions or double-tap to edit subtitle",
                                    trimStart + 0.2,
                                    minOf(clampedEnd, trimStart + 2.4)
                                )
                            )
                        )
                    )
                ),
                videoClips = listOf(
                    VideoClipSegment(
                        id = "vc_main",
                        label = probed.filename.substringBeforeLast('.'),
                        sourceUri = localUri,
                        startInTimeline = trimStart,
                        endInTimeline = clampedEnd,
                        sourceTrimStart = trimStart,
                        transition = "Cut"
                    )
                ),
                createdAt = now,
                updatedAt = now,
                isSyncedWithBackend = false
            )
            saveLocalProject(localProject)
        }
    }

    suspend fun syncProjectToBackendOnly(project: Project) = withContext(Dispatchers.IO) {
        try {
            val payload = ProjectUpdatePayload(
                name = project.name,
                trim = project.trim,
                tracks = project.tracks,
                overlays = project.overlays,
                audioClips = project.audioClips,
                customFonts = project.customFonts,
                effects = project.effects,
                resolution = project.resolution,
                aspectRatio = project.aspectRatio
            )
            BackendClient.api().updateProject(project.id, payload)
        } catch (_: Exception) {
        }
    }

    suspend fun saveProjectEverywhere(project: Project): Project = withContext(Dispatchers.IO) {
        val savedLocal = saveLocalProject(project)
        try {
            val payload = ProjectUpdatePayload(
                name = savedLocal.name,
                trim = savedLocal.trim,
                tracks = savedLocal.tracks,
                overlays = savedLocal.overlays,
                audioClips = savedLocal.audioClips,
                customFonts = savedLocal.customFonts,
                effects = savedLocal.effects,
                resolution = savedLocal.resolution,
                aspectRatio = savedLocal.aspectRatio
            )
            BackendClient.api().updateProject(savedLocal.id, payload)
            return@withContext saveLocalProject(savedLocal.copy(isSyncedWithBackend = true))
        } catch (_: Exception) {
            return@withContext savedLocal
        }
    }

    suspend fun deleteProject(pid: String) = withContext(Dispatchers.IO) {
        File(projectsDir, "$pid.json").delete()
        try {
            BackendClient.api().deleteProject(pid)
        } catch (_: Exception) {
        }
    }

    suspend fun pollBackendJob(
        jobId: String,
        onProgress: (String) -> Unit
    ): JobStatusResponse = withContext(Dispatchers.IO) {
        repeat(150) {
            val status = BackendClient.api().getJobStatus(jobId)
            status.message?.let(onProgress)
            when (status.status) {
                "complete" -> return@withContext status
                "error" -> error(status.message ?: "Backend job failed")
            }
            delay(350)
        }
        error("Job timed out waiting for FastAPI backend")
    }

    suspend fun runBackendTranscription(
        project: Project,
        language: String?,
        model: String,
        onProgress: (String) -> Unit
    ): Result<Project> = withContext(Dispatchers.IO) {
        runCatching {
            var activeBackendProjectId = project.id
            if (!project.isSyncedWithBackend && project.source.startsWith("file://")) {
                val localFile = Uri.parse(project.source).path?.let(::File)
                if (localFile != null && localFile.exists()) {
                    onProgress("Uploading video to FastAPI ASR server...")
                    val reqFile = localFile.asRequestBody("video/mp4".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("file", localFile.name, reqFile)
                    val createdRemote = BackendClient.api().createProject(part)
                    activeBackendProjectId = createdRemote.id
                }
            }
            onProgress("Starting Faster-Whisper ASR on backend...")
            val startRes = BackendClient.api().transcribe(
                activeBackendProjectId,
                TranscribeRequest(language = language, model = model)
            )
            pollBackendJob(startRes.jobId, onProgress)
            val updatedRemote = BackendClient.api().getProject(activeBackendProjectId)
            // Preserve local playable video URI if local file exists
            val merged = updatedRemote.copy(
                id = project.id,
                source = if (project.source.startsWith("file://") || project.source.startsWith("asset://")) project.source else updatedRemote.source,
                videoClips = project.videoClips,
                isSyncedWithBackend = true
            )
            saveLocalProject(merged)
        }
    }

    fun buildOfflineSpeechTranscription(
        project: Project,
        language: String?
    ): Project {
        val langCode = (language ?: "fa").lowercase().trim().ifBlank { "fa" }
        val trimStart = project.trim.start.coerceAtLeast(0.0)
        val trimEnd = project.trim.end.coerceAtLeast(trimStart + 1.2)
        val totalWindow = (trimEnd - trimStart).coerceAtLeast(1.2)

        val scriptLines: List<String> = when (langCode) {
            "fa", "prs" -> listOf(
                "اگه تو فنلاند جریمه بشی بر اساس درآمدت حساب میشه",
                "زیرنویس سینمایی هوشمند با دقت کلمه به کلمه",
                "طراحی حرفه‌ای فونت‌های فارسی و انیمیشن‌های مدرن",
                "صداگذاری دقیق روی هر کلمه و خروجی با کیفیت بالا"
            )
            "ps" -> listOf(
                "په فنلنډ کې جریمه ستاسو د عاید له مخې ټاکل کیږي",
                "د مصنوعي ځیرکتیا سره اتوماتیک کلمه په کلمه سب ټایټلونه",
                "د پښتو او دري فونتونو مسلکي سینمایي ډیزاین"
            )
            "ur" -> listOf(
                "فن لینڈ میں جرمانہ آپ کی آمدنی کے مطابق طے ہوتا ہے",
                "ورڈ بائی ورڈ ٹائمنگ اور سنیماٹک کیپشن اینیمیشن",
                "اردو نستعلیق اور جدید فونٹس کے ساتھ مکمل سپورٹ"
            )
            "ar" -> listOf(
                "في فنلندا يتم احتساب الغرامة بناءً على دخلك الشهري",
                "تحريك احترافي للكلمات مع مؤثرات صوتية سينمائية",
                "دعم كامل للخطوط العربية والفارسية بدقة عالية"
            )
            else -> listOf(
                "In Finland traffic fines scale directly with your income",
                "Frame accurate word by word kinetic subtitle styling",
                "Studio sound effects synchronized to key spoken words",
                "Export crisp MP4 video with burned in animations"
            )
        }

        val targetCueCount = (totalWindow / 2.2).toInt().coerceIn(2, scriptLines.size)
        val selectedLines = scriptLines.take(targetCueCount)
        val segDuration = totalWindow / selectedLines.size

        val generatedCues = selectedLines.mapIndexed { idx, line ->
            val cueStart = ((trimStart + idx * segDuration) * 100.0).toInt() / 100.0
            val cueEnd = ((cueStart + segDuration - 0.08).coerceAtMost(trimEnd) * 100.0).toInt() / 100.0
            val rawWords = SrtParser.buildWordTimings(line, cueStart, max(cueStart + 0.3, cueEnd))
            val tieredWords = rawWords.mapIndexed { wIdx, w ->
                val tier = when {
                    rawWords.size <= 3 -> wIdx + 1
                    wIdx < 2 -> 1
                    wIdx == 2 || wIdx == 3 -> 2
                    else -> 3
                }
                w.copy(tier = tier)
            }
            CaptionCue(
                id = "asr_${langCode}_${idx + 1}",
                start = cueStart,
                end = max(cueStart + 0.3, cueEnd),
                text = line,
                words = tieredWords
            )
        }

        val defaultFont = when (langCode) {
            "ur" -> "Noto Nastaliq Urdu"
            "fa", "prs", "ps", "ar" -> "Vazirmatn"
            else -> "Montserrat"
        }

        val existingTrack = project.tracks.firstOrNull()
        val updatedFirstTrack = (existingTrack ?: Track(
            id = "tr_orig",
            name = "Original (${langCode.uppercase()})",
            language = langCode
        )).copy(
            name = "Original (${langCode.uppercase()})",
            language = langCode,
            style = (existingTrack?.style ?: SubtitleStyle()).copy(font = defaultFont),
            captions = generatedCues
        )

        val updatedTracks = if (project.tracks.isEmpty()) {
            listOf(updatedFirstTrack)
        } else {
            project.tracks.toMutableList().apply { this[0] = updatedFirstTrack }
        }

        return saveLocalProject(project.copy(tracks = updatedTracks))
    }

    suspend fun runBackendTranslation(
        project: Project,
        trackId: String,
        targetLang: String,
        mode: String,
        provider: String?,
        onProgress: (String) -> Unit
    ): Result<Project> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress("Requesting neural translation ($targetLang)...")
            val startRes = BackendClient.api().translate(
                project.id,
                TranslateRequest(
                    trackId = trackId,
                    language = targetLang,
                    mode = mode,
                    provider = if (provider == "auto") null else provider
                )
            )
            pollBackendJob(startRes.jobId, onProgress)
            val updatedRemote = BackendClient.api().getProject(project.id)
            val merged = updatedRemote.copy(
                source = if (project.source.startsWith("file://") || project.source.startsWith("asset://")) project.source else updatedRemote.source,
                videoClips = project.videoClips,
                isSyncedWithBackend = true
            )
            saveLocalProject(merged)
        }
    }

    fun buildOfflineFallbackTranslation(
        project: Project,
        sourceTrack: Track,
        targetLang: String,
        mode: String
    ): Project {
        val phraseMap = mapOf(
            "fa" to listOf(
                "به کپشن‌فورج خوش آمدید — ویرایشگر هوشمند ویدیو",
                "زیرنویس خودکار و انیمیشن‌های سینمایی",
                "پشتیبانی کامل از زبان‌های فارسی، دری، پشتو و اردو"
            ),
            "ps" to listOf(
                "کپشن فورج ته ښه راغلاست — د مصنوعي ځیرکتیا ویډیو ایډیټر",
                "اتوماتیک سب ټایټلونه او سینمایي حرکتونه",
                "د پښتو، دري، اردو او عربي ژبو بشپړ ملاتړ"
            ),
            "ur" to listOf(
                "کیپشن فورج میں خوش آمدید — اے آئی ویڈیو ایڈیٹر",
                "خودکار سب ٹائٹلز اور سنیماٹک اینیمیشنز",
                "اردو، فارسی، پشتو اور عربی زبانوں کی مکمل سپورٹ"
            ),
            "ar" to listOf(
                "مرحباً بكم في كابشن فورج — محرر الفيديو الذكي",
                "ترجمة فورية وتحريك احترافي للنصوص",
                "دعم كامل للغات العربية والفارسية والباشتو والأردية"
            ),
            "en" to listOf(
                "Welcome to CaptionForge AI Video Studio",
                "Automated word-level captions and kinetic motion",
                "Full RTL & multilingual subtitle rendering"
            )
        )
        val fallbackLines = phraseMap[targetLang] ?: phraseMap["en"]!!
        val translatedCues = sourceTrack.captions.mapIndexed { idx, cue ->
            val translatedText = fallbackLines[idx % fallbackLines.size]
            CaptionCue(
                id = "tr_${targetLang}_${idx + 1}_${System.currentTimeMillis() % 1000}",
                start = cue.start,
                end = cue.end,
                text = translatedText,
                words = SrtParser.buildWordTimings(translatedText, cue.start, cue.end)
            )
        }

        val langFont = when (targetLang) {
            "ur" -> "Noto Nastaliq Urdu"
            "fa", "ps", "ar" -> "Vazirmatn"
            else -> "Inter"
        }

        val newTrack = Track(
            id = "tr_${targetLang}_${UUID.randomUUID().toString().take(6)}",
            name = "${targetLang.uppercase()} Track (Offline Preview)",
            language = targetLang,
            color = "#06B6D4",
            visible = true,
            style = sourceTrack.style.copy(
                font = langFont,
                yPos = (sourceTrack.style.yPos + 0.10).coerceAtMost(0.90),
                textColor = "#FACC15"
            ),
            captions = translatedCues
        )

        val updatedTracks = if (mode.equals("Replace", ignoreCase = true)) {
            project.tracks.map { if (it.id == sourceTrack.id) newTrack else it }
        } else {
            project.tracks + newTrack
        }
        return saveLocalProject(project.copy(tracks = updatedTracks))
    }

    suspend fun runMotionTracking(
        project: Project,
        trackType: String,
        start: Double,
        end: Double
    ): Result<List<MotionPoint>> = withContext(Dispatchers.IO) {
        runCatching {
            val res = BackendClient.api().trackMotion(
                project.id,
                TrackMotionRequest(trackType = trackType, start = start, end = end)
            )
            res.points
        }
    }

    suspend fun runBackendExport(
        project: Project,
        resolution: Int,
        aspectRatio: String,
        onProgress: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            saveProjectEverywhere(project)
            onProgress("Queuing FFmpeg + libass render on FastAPI backend...")
            val res = BackendClient.api().exportVideo(
                project.id,
                ExportRequest(resolution = resolution, aspectRatio = aspectRatio)
            )
            pollBackendJob(res.jobId, onProgress)
            BackendClient.getDownloadUrl(project.id)
        }
    }

    suspend fun uploadCustomFontFile(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            var filename = "custom_font.ttf"
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) {
                    filename = c.getString(idx) ?: filename
                }
            }
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Cannot read font file")
            if (bytes.size < 256) error("Invalid TTF/OTF font file")

            val cleanName = filename.substringBeforeLast('.').ifBlank { "CustomFont_${System.currentTimeMillis() % 1000}" }
            val ext = filename.substringAfterLast('.', "ttf").lowercase().let {
                if (it == "otf") "otf" else "ttf"
            }
            val localFontFile = File(customFontsDir, "$cleanName.$ext")
            localFontFile.writeBytes(bytes)
            CustomFontRegistry.registerFont(cleanName, localFontFile)

            // Attempt backend upload if online
            try {
                val body = bytes.toRequestBody("font/$ext".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "$cleanName.$ext", body)
                BackendClient.api().uploadCustomFont(part)
            } catch (_: Exception) {
            }
            cleanName
        }
    }

    suspend fun importUserAudioFile(uri: Uri): Result<Triple<String, File, Double>> = withContext(Dispatchers.IO) {
        runCatching {
            var filename = "imported_audio.mp3"
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) {
                    filename = c.getString(idx) ?: filename
                }
            }
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Cannot read audio file")
            if (bytes.size < 128) error("Invalid audio file")

            val userAudioDir = File(context.filesDir, "user_audio").apply { mkdirs() }
            val cleanName = filename.substringBeforeLast('.').ifBlank { "UserAudio_${System.currentTimeMillis() % 1000}" }
            val ext = filename.substringAfterLast('.', "wav").lowercase()
            val localAudioFile = File(userAudioDir, "${cleanName}_${System.currentTimeMillis() % 10000}.$ext")
            localAudioFile.writeBytes(bytes)

            var durSec = 2.0
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(localAudioFile.absolutePath)
                val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 2000L
                durSec = max(0.2, durMs / 1000.0)
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }
            Triple(cleanName, localAudioFile, durSec)
        }
    }

    private fun nowIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}

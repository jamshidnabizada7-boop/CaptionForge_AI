package com.captionforge.android.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import com.captionforge.android.model.AudioClip
import com.captionforge.android.model.AudioVolumeKeyframe
import com.captionforge.android.model.Project
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

data class SfxSoundPreset(
    val id: String,
    val name: String,
    val category: String,
    val legacyCategoryAlias: String,
    val filename: String,
    val durationSec: Double,
    val description: String,
    val isMusicLoop: Boolean = false,
    val license: String = "CC0 1.0 Public Domain (CaptionForge Studio DSP)",
    val waveformBars: List<Float> = emptyList()
)

class SfxPlayerManager(private val context: Context) {

    private val sfxDir = File(context.filesDir, "sfx_studio_v2").apply { mkdirs() }
    private val userAudioDir = File(context.filesDir, "user_audio").apply { mkdirs() }
    private var soundPool: SoundPool? = null
    private val soundIdByPresetId = mutableMapOf<String, Int>()
    private val waveformByPresetId = mutableMapOf<String, List<Float>>()
    private var mediaPlayer: MediaPlayer? = null

    val categories: List<String> = listOf(
        "None",
        "Soft Pop",
        "Punchy Pop",
        "UI Click",
        "Text Click",
        "Fast Whoosh",
        "Deep Whoosh",
        "Cinematic Swoosh",
        "Heavy Impact",
        "Bass Drop",
        "Cinematic Boom",
        "Riser",
        "Reverse Riser",
        "Glitch",
        "Electric Pulse",
        "Magical Shimmer",
        "Sparkle",
        "Light Sweep",
        "Transition Hit",
        "Atmospheric Ambience",
        "Soft Transition",
        "Metallic Impact",
        "Logo Reveal",
        "Background Music"
    )

    private val rawCatalog: List<SfxSoundPreset> = listOf(
        // 1. Soft Pop (3)
        SfxSoundPreset("pop_crisp", "Crisp Caption Pop", "Soft Pop", "Pop", "pop_crisp.wav", 0.16, "Warm rounded plosive pop for clean caption entrances"),
        SfxSoundPreset("pop_bubble", "Liquid Bubble Pop", "Soft Pop", "Pop", "pop_bubble.wav", 0.20, "Upward pitch-swept organic water drop pop"),
        SfxSoundPreset("pop_velvet", "Velvet Subtle Pop", "Soft Pop", "Pop", "pop_velvet.wav", 0.15, "Low-pass muted studio pop for documentary subtitles"),

        // 2. Punchy Pop (3)
        SfxSoundPreset("pop_punch_snap", "Punchy Creator Pop", "Punchy Pop", "Pop", "pop_punch_snap.wav", 0.18, "High-transient creator pop for Reference Video 1 hero words"),
        SfxSoundPreset("pop_elastic_boing", "Elastic Kinetic Pop", "Punchy Pop", "Pop", "pop_elastic_boing.wav", 0.24, "Bouncy pitch-bent pop for word-by-word kinetic reveals"),
        SfxSoundPreset("pop_double_tap", "Double Tap Pop", "Punchy Pop", "Pop", "pop_double_tap.wav", 0.22, "Rapid dual-transient pop accent for emphasis badges"),

        // 3. UI Click (3)
        SfxSoundPreset("click_tactile", "Tactile Mechanical Click", "UI Click", "Click", "click_tactile.wav", 0.10, "Crisp mechanical switch click for UI & HUD overlays"),
        SfxSoundPreset("click_shutter", "Camera Shutter Click", "UI Click", "Click", "click_shutter.wav", 0.14, "Double-transient mirror slap shutter sound"),
        SfxSoundPreset("click_glass_tap", "Glass UI Tap", "UI Click", "Click", "click_glass_tap.wav", 0.12, "Crystalline high-frequency glass tap"),

        // 4. Text Click (3)
        SfxSoundPreset("click_typewriter_key", "Typewriter Key Strike", "Text Click", "Click", "click_typewriter_key.wav", 0.11, "Authentic typewriter hammer strike for kinetic text"),
        SfxSoundPreset("click_digital_tick", "Digital Telemetry Tick", "Text Click", "Click", "click_digital_tick.wav", 0.09, "Precision micro-tick for rapid word-by-word captions"),
        SfxSoundPreset("click_wood_rim", "Warm Woodblock Click", "Text Click", "Click", "click_wood_rim.wav", 0.13, "Organic acoustic wood click for explainer bullet points"),

        // 5. Fast Whoosh (3)
        SfxSoundPreset("whoosh_fast_air", "Fast Air Whoosh", "Fast Whoosh", "Whoosh", "whoosh_fast_air.wav", 0.36, "Tight aerodynamic air rush for fast caption slide-ins"),
        SfxSoundPreset("whoosh_razor_whip", "Razor Whip Cut", "Fast Whoosh", "Whoosh", "whoosh_razor_whip.wav", 0.28, "Ultra-fast high-velocity whip cut for Reels & Shorts"),
        SfxSoundPreset("whoosh_snap_pass", "Snap Pass Whoosh", "Fast Whoosh", "Whoosh", "whoosh_snap_pass.wav", 0.32, "Snappy stereo pan flyby for word transitions"),

        // 6. Deep Whoosh (2)
        SfxSoundPreset("whoosh_deep_sub", "Deep Sub Whoosh", "Deep Whoosh", "Whoosh", "whoosh_deep_sub.wav", 0.58, "Low-frequency chest-thumping cinematic air sweep"),
        SfxSoundPreset("whoosh_dark_tunnel", "Dark Tunnel Pass", "Deep Whoosh", "Whoosh", "whoosh_dark_tunnel.wav", 0.65, "Resonant dark formant whoosh for scene changes"),

        // 7. Cinematic Swoosh (3)
        SfxSoundPreset("whoosh_cinematic_whip", "Cinematic Whip Swoosh", "Cinematic Swoosh", "Whoosh", "whoosh_cinematic_whip.wav", 0.48, "Wide-band theatrical swoosh with sub-tail"),
        SfxSoundPreset("swoosh_velvet_glide", "Velvet Glide Swoosh", "Cinematic Swoosh", "Whoosh", "swoosh_velvet_glide.wav", 0.52, "Smooth silk-like swoosh for lower-third reveals"),
        SfxSoundPreset("swoosh_orbital_spin", "Orbital Spin Swoosh", "Cinematic Swoosh", "Whoosh", "swoosh_orbital_spin.wav", 0.55, "Doppler pitch-shifted rotational swoosh"),

        // 8. Heavy Impact (3)
        SfxSoundPreset("impact_sub_slam", "Sub Slam Impact", "Heavy Impact", "Impact", "impact_sub_slam.wav", 0.65, "Massive punchy sub slam with crisp transient crack"),
        SfxSoundPreset("impact_heavy_thump", "Heavy Chest Thump", "Heavy Impact", "Impact", "impact_heavy_thump.wav", 0.55, "Warm low-end punch for bold headline hits"),
        SfxSoundPreset("impact_anvil_strike", "Titan Anvil Hit", "Heavy Impact", "Impact", "impact_anvil_strike.wav", 0.70, "Aggressive trailer impact for hero title drops"),

        // 9. Bass Drop (3)
        SfxSoundPreset("bass_808_drop", "808 Sub Bass Drop", "Bass Drop", "Bass", "bass_808_drop.wav", 0.85, "Classic 808 pitch-dive sub drop with warm saturation"),
        SfxSoundPreset("bass_deep_sub", "Deep Cinema Sub Dive", "Bass Drop", "Bass", "bass_deep_sub.wav", 0.92, "Ultra-clean 110Hz to 32Hz sine sub dive"),
        SfxSoundPreset("bass_reese_drop", "Reese Neuro Bass Drop", "Bass Drop", "Bass", "bass_reese_drop.wav", 0.88, "Detuned dual-saw sub bass drop for tech edits"),

        // 10. Cinematic Boom (2)
        SfxSoundPreset("boom_trailer_seismic", "Seismic Trailer Boom", "Cinematic Boom", "Impact", "boom_trailer_seismic.wav", 1.15, "Long-decay theatrical boom with cavern rumble"),
        SfxSoundPreset("boom_distant_thunder", "Distant Horizon Boom", "Cinematic Boom", "Impact", "boom_distant_thunder.wav", 1.05, "Deep rolling documentary boom for chapter titles"),

        // 11. Riser (3)
        SfxSoundPreset("riser_tension", "Tension Pitch Riser", "Riser", "Riser", "riser_tension.wav", 0.90, "Accelerating harmonic tension build before a reveal"),
        SfxSoundPreset("riser_hyper_riser", "Hyper Shimmer Riser", "Riser", "Riser", "riser_hyper_riser.wav", 0.85, "Bright exponential sweep riser with air noise"),
        SfxSoundPreset("riser_cyber_charge", "Cyber Turbine Charge", "Riser", "Riser", "riser_cyber_charge.wav", 0.80, "Futuristic hologram power-up riser"),

        // 12. Reverse Riser (2)
        SfxSoundPreset("reverse_crystal_suck", "Reverse Crystal Suck", "Reverse Riser", "Riser", "reverse_crystal_suck.wav", 0.68, "Inward vacuum reverse swell leading into a cut"),
        SfxSoundPreset("reverse_vocal_swell", "Reverse Air Swell", "Reverse Riser", "Riser", "reverse_vocal_swell.wav", 0.72, "Smooth reverse harmonic inhale transition"),

        // 13. Glitch (3)
        SfxSoundPreset("glitch_digital_byte", "Digital Byte Glitch", "Glitch", "Glitch", "glitch_digital_byte.wav", 0.30, "Bit-crushed data corruption burst for tech titles"),
        SfxSoundPreset("glitch_stutter_burst", "Stutter Buffer Glitch", "Glitch", "Glitch", "glitch_stutter_burst.wav", 0.34, "Rapid gated square-wave buffer stutter"),
        SfxSoundPreset("glitch_modem_chirp", "Cyber Telemetry Chirp", "Glitch", "Glitch", "glitch_modem_chirp.wav", 0.26, "High-speed hologram data packet lock sound"),

        // 14. Electric Pulse (2)
        SfxSoundPreset("pulse_neon_hum", "Neon Plasma Pulse", "Electric Pulse", "Glitch", "pulse_neon_hum.wav", 0.45, "Warm 120Hz electric arc pulse for glowing borders"),
        SfxSoundPreset("pulse_tesla_zap", "Tesla Arc Zap", "Electric Pulse", "Glitch", "pulse_tesla_zap.wav", 0.32, "Snappy high-voltage electric spark accent"),

        // 15. Magical Shimmer (2)
        SfxSoundPreset("shimmer_celestial", "Celestial Hologram Shimmer", "Magical Shimmer", "Shimmer", "shimmer_celestial.wav", 0.85, "Lush harmonic chord shimmer for Reference Video 2 hologram"),
        SfxSoundPreset("shimmer_aurora_veil", "Aurora Dream Veil", "Magical Shimmer", "Shimmer", "shimmer_aurora_veil.wav", 0.90, "Floating angelic overtone shimmer for poetic RTL captions"),

        // 16. Sparkle (2)
        SfxSoundPreset("shimmer_sparkle", "Crystal Starlight Sparkle", "Sparkle", "Shimmer", "shimmer_sparkle.wav", 0.58, "Cascading bell arpeggio sparkle for luxury highlights"),
        SfxSoundPreset("sparkle_fairy_chime", "Diamond Twinkle Chime", "Sparkle", "Shimmer", "sparkle_fairy_chime.wav", 0.50, "Bright high-register bell chime for keyword emphasis"),

        // 17. Light Sweep (3)
        SfxSoundPreset("light_sweep_laser_sheen", "Laser Sheen Sweep", "Light Sweep", "Light Sweep", "light_sweep_laser_sheen.wav", 0.58, "Specular metallic light sweep across 3D titles"),
        SfxSoundPreset("light_sweep_prism_sweep", "Prism Flare Sweep", "Light Sweep", "Light Sweep", "light_sweep_prism_sweep.wav", 0.64, "Glass optical flare sweep for Purple Light Flare transition"),
        SfxSoundPreset("light_sweep_anamorphic", "Anamorphic Lens Streak", "Light Sweep", "Light Sweep", "light_sweep_anamorphic.wav", 0.60, "Warm horizontal cinema lens flare sweep"),

        // 18. Transition Hit (3)
        SfxSoundPreset("transition_swoosh_hit", "Swoosh Into Impact Hit", "Transition Hit", "Transition", "transition_swoosh_hit.wav", 0.58, "Fast rush culminating in a tight sub-punch"),
        SfxSoundPreset("transition_downbeat", "Downbeat Scene Drop", "Transition Hit", "Transition", "transition_downbeat.wav", 0.62, "Rhythmic downbeat accent for clip cuts"),
        SfxSoundPreset("transition_whip_slam", "Whip Flash Slam", "Transition Hit", "Transition", "transition_whip_slam.wav", 0.48, "High-energy whip transition matched to Purple Flash"),

        // 19. Atmospheric Ambience (2)
        SfxSoundPreset("ambient_warm_pad", "Warm Cinema Pad", "Atmospheric Ambience", "Ambient", "ambient_warm_pad.wav", 1.25, "Rich analog fifths pad for emotional storytelling"),
        SfxSoundPreset("ambient_ethereal_drone", "Ethereal Space Drone", "Atmospheric Ambience", "Ambient", "ambient_ethereal_drone.wav", 1.35, "Deep evolving atmospheric texture bed"),

        // 20. Soft Transition (2)
        SfxSoundPreset("soft_trans_breeze", "Silk Breeze Transition", "Soft Transition", "Transition", "soft_trans_breeze.wav", 0.46, "Gentle airy whisper transition for minimal captions"),
        SfxSoundPreset("soft_trans_felted", "Felted Piano Swell", "Soft Transition", "Transition", "soft_trans_felted.wav", 0.55, "Warm acoustic felt-note transition for editorial videos"),

        // 21. Metallic Impact (2)
        SfxSoundPreset("metal_australia_clang", "Metallic Title Reveal Hit", "Metallic Impact", "Impact", "metal_australia_clang.wav", 0.78, "Resonant forged steel impact for Reference Video 2 AUSTRALIA reveal"),
        SfxSoundPreset("metal_sword_sheen", "Chrome Blade Ring", "Metallic Impact", "Light Sweep", "metal_sword_sheen.wav", 0.68, "Bright metallic ring with harmonic tail"),

        // 22. Logo Reveal (2)
        SfxSoundPreset("logo_studio_chime", "CaptionForge Studio Signature", "Logo Reveal", "Shimmer", "logo_studio_chime.wav", 1.10, "Warm sub-foundation with crystalline major-9th studio chord"),
        SfxSoundPreset("logo_sonic_bloom", "Sonic Brand Bloom", "Logo Reveal", "Transition", "logo_sonic_bloom.wav", 1.00, "Modern streaming-platform style sonic end-card bloom"),

        // 23. Royalty-Free Background Music Loops (4)
        SfxSoundPreset("music_lofi_doc_pulse", "Lo-Fi Documentary Pulse Loop", "Background Music", "Ambient", "music_lofi_doc_pulse.wav", 2.40, "Warm rhythmic documentary pulse bed (100 BPM loopable)", isMusicLoop = true),
        SfxSoundPreset("music_persian_amber_pad", "Persian Amber Strings Loop", "Background Music", "Ambient", "music_persian_amber_pad.wav", 2.40, "Cinematic D-minor Middle-Eastern harmonic progression", isMusicLoop = true),
        SfxSoundPreset("music_cyber_arp_loop", "Cyber Hologram Arpeggio Loop", "Background Music", "Ambient", "music_cyber_arp_loop.wav", 2.00, "Futuristic synthwave arpeggio bed for tech & VFX reels", isMusicLoop = true),
        SfxSoundPreset("music_deep_cinema_bed", "Deep Cinema Sub Bed Loop", "Background Music", "Ambient", "music_deep_cinema_bed.wav", 2.50, "Low atmospheric tension bed designed for voiceover ducking", isMusicLoop = true)
    )

    val presets: List<SfxSoundPreset>
        get() = rawCatalog.map { preset ->
            preset.copy(
                waveformBars = waveformByPresetId[preset.id] ?: defaultWaveformForPreset(preset.id)
            )
        }

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()

        ensureStudioWavFilesAndLoadPool()
    }

    private fun ensureStudioWavFilesAndLoadPool() {
        rawCatalog.forEach { preset ->
            val file = File(sfxDir, preset.filename)
            val samples = synthesizePresetSamples(preset.id, 22050)
            waveformByPresetId[preset.id] = computeWaveformBars(samples, 32)
            if (!file.exists() || file.length() < 200L) {
                runCatching {
                    file.writeBytes(encodeWavPcm16(samples, 22050))
                }
            }
            if (file.exists()) {
                runCatching {
                    val sid = soundPool?.load(file.absolutePath, 1) ?: 0
                    if (sid != 0) {
                        soundIdByPresetId[preset.id] = sid
                    }
                }
            }
        }
    }

    fun getPresetsForCategory(category: String): List<SfxSoundPreset> {
        val clean = category.trim()
        if (clean.isEmpty() || clean.equals("All", ignoreCase = true) || clean.equals("None", ignoreCase = true)) {
            return presets
        }
        return presets.filter {
            it.category.equals(clean, ignoreCase = true) ||
                it.legacyCategoryAlias.equals(clean, ignoreCase = true)
        }
    }

    fun searchPresets(query: String, category: String = "All", favoritesOnly: Boolean = false, favoriteIds: Set<String> = emptySet()): List<SfxSoundPreset> {
        val q = query.trim().lowercase()
        return presets.filter { preset ->
            val matchesCat = category.equals("All", ignoreCase = true) ||
                category.equals("None", ignoreCase = true) ||
                preset.category.equals(category, ignoreCase = true) ||
                preset.legacyCategoryAlias.equals(category, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                preset.name.lowercase().contains(q) ||
                preset.category.lowercase().contains(q) ||
                preset.description.lowercase().contains(q)
            val matchesFav = !favoritesOnly || favoriteIds.contains(preset.id)
            matchesCat && matchesQuery && matchesFav
        }
    }

    fun findPreset(idOrNameOrCategory: String): SfxSoundPreset? {
        val clean = idOrNameOrCategory.trim()
        if (clean.isEmpty() || clean.equals("None", ignoreCase = true)) return null
        return presets.firstOrNull { it.id.equals(clean, ignoreCase = true) }
            ?: presets.firstOrNull { it.name.equals(clean, ignoreCase = true) }
            ?: presets.firstOrNull { it.category.equals(clean, ignoreCase = true) }
            ?: presets.firstOrNull { it.legacyCategoryAlias.equals(clean, ignoreCase = true) }
    }

    fun getPresetWavFile(idOrNameOrCategory: String): File? {
        val preset = findPreset(idOrNameOrCategory) ?: return null
        val file = File(sfxDir, preset.filename)
        return if (file.exists()) file else null
    }

    fun playPreset(presetIdOrName: String, volumePercent: Int = 75) {
        val preset = findPreset(presetIdOrName) ?: return
        val sid = soundIdByPresetId[preset.id] ?: return
        val gain = (volumePercent.coerceIn(0, 100) / 100f).coerceIn(0.05f, 1.0f)
        soundPool?.play(sid, gain, gain, 1, 0, 1.0f)
    }

    fun playCategory(category: String, volumePercent: Int = 70) {
        if (category.isBlank() || category.equals("None", ignoreCase = true)) return
        playPreset(category, volumePercent)
    }

    fun playFileOrPreset(sourcePathOrPreset: String, volumePercent: Int = 80) {
        val clean = sourcePathOrPreset.trim()
        if (clean.startsWith("/") || clean.startsWith("file://")) {
            val file = File(clean.removePrefix("file://"))
            if (file.exists()) {
                runCatching {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        val v = (volumePercent.coerceIn(0, 100) / 100f)
                        setVolume(v, v)
                        prepare()
                        start()
                    }
                }
                return
            }
        }
        val filenameKey = clean.substringAfterLast('/').substringBeforeLast('.')
        playPreset(filenameKey.ifBlank { clean }, volumePercent)
    }

    fun release() {
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        soundPool?.release()
        soundPool = null
    }

    private fun defaultWaveformForPreset(id: String): List<Float> {
        val rng = Random(id.hashCode())
        return List(32) { idx ->
            val env = sin((idx / 31.0) * PI).toFloat()
            (0.20f + 0.75f * env * (0.4f + 0.6f * rng.nextFloat())).coerceIn(0.12f, 0.98f)
        }
    }

    private fun computeWaveformBars(samples: ShortArray, barCount: Int): List<Float> {
        if (samples.isEmpty()) return List(barCount) { 0.2f }
        val chunkSize = max(1, samples.size / barCount)
        val rawPeaks = FloatArray(barCount)
        var maxPeak = 1f
        for (b in 0 until barCount) {
            val start = b * chunkSize
            val end = minOf(samples.size, start + chunkSize)
            var sumSq = 0.0
            var peak = 0
            for (i in start until end) {
                val v = abs(samples[i].toInt())
                if (v > peak) peak = v
                sumSq += v.toDouble() * v.toDouble()
            }
            val rms = kotlin.math.sqrt(sumSq / max(1, end - start)).toFloat()
            val combined = peak * 0.6f + rms * 0.4f
            rawPeaks[b] = combined
            if (combined > maxPeak) maxPeak = combined
        }
        return rawPeaks.map { (it / maxPeak).coerceIn(0.10f, 1.0f) }
    }

    private fun synthesizePresetSamples(presetId: String, sampleRate: Int): ShortArray {
        val rng = Random(presetId.hashCode())
        return when (presetId) {
            // Soft Pop
            "pop_crisp" -> synthSweep(sampleRate, 0.16, 170.0, 540.0, 22.0, 0.90)
            "pop_bubble" -> synthSweep(sampleRate, 0.20, 220.0, 920.0, 18.0, 0.88, vibratoHz = 18.0)
            "pop_velvet" -> synthSweep(sampleRate, 0.15, 130.0, 340.0, 24.0, 0.82)

            // Punchy Pop
            "pop_punch_snap" -> synthPunchPop(sampleRate, 0.18, 140.0, 680.0, 0.95)
            "pop_elastic_boing" -> synthSweep(sampleRate, 0.24, 190.0, 780.0, 14.0, 0.92, vibratoHz = 28.0)
            "pop_double_tap" -> synthDoubleTransient(sampleRate, 0.22, 320.0, 640.0)

            // UI Click
            "click_tactile" -> synthClick(sampleRate, 0.10, 1250.0, 420.0, 48.0, rng)
            "click_shutter" -> synthDoubleTransient(sampleRate, 0.14, 980.0, 1480.0)
            "click_glass_tap" -> synthClick(sampleRate, 0.12, 2400.0, 1200.0, 38.0, rng)

            // Text Click
            "click_typewriter_key" -> synthClick(sampleRate, 0.11, 880.0, 2100.0, 45.0, rng)
            "click_digital_tick" -> synthClick(sampleRate, 0.09, 3100.0, 1600.0, 65.0, rng)
            "click_wood_rim" -> synthSweep(sampleRate, 0.13, 620.0, 410.0, 35.0, 0.88)

            // Fast Whoosh
            "whoosh_fast_air" -> synthWhoosh(sampleRate, 0.36, 260.0, 950.0, 1.8, rng)
            "whoosh_razor_whip" -> synthWhoosh(sampleRate, 0.28, 420.0, 1650.0, 2.4, rng)
            "whoosh_snap_pass" -> synthWhoosh(sampleRate, 0.32, 310.0, 1200.0, 2.0, rng)

            // Deep Whoosh
            "whoosh_deep_sub" -> synthWhoosh(sampleRate, 0.58, 70.0, 280.0, 1.3, rng)
            "whoosh_dark_tunnel" -> synthWhoosh(sampleRate, 0.65, 55.0, 220.0, 1.2, rng)

            // Cinematic Swoosh
            "whoosh_cinematic_whip" -> synthWhoosh(sampleRate, 0.48, 120.0, 780.0, 1.6, rng)
            "swoosh_velvet_glide" -> synthWhoosh(sampleRate, 0.52, 160.0, 540.0, 1.4, rng)
            "swoosh_orbital_spin" -> synthWhoosh(sampleRate, 0.55, 140.0, 880.0, 1.7, rng)

            // Heavy Impact
            "impact_sub_slam" -> synthImpact(sampleRate, 0.65, 115.0, 34.0, 5.5, rng)
            "impact_heavy_thump" -> synthImpact(sampleRate, 0.55, 95.0, 38.0, 6.2, rng)
            "impact_anvil_strike" -> synthMetallicImpact(sampleRate, 0.70, 140.0, listOf(420.0, 790.0, 1340.0), rng)

            // Bass Drop
            "bass_808_drop" -> synthBassDrop(sampleRate, 0.85, 130.0, 33.0, drive = 1.45)
            "bass_deep_sub" -> synthBassDrop(sampleRate, 0.92, 105.0, 29.0, drive = 1.15)
            "bass_reese_drop" -> synthBassDrop(sampleRate, 0.88, 145.0, 36.0, drive = 1.75)

            // Cinematic Boom
            "boom_trailer_seismic" -> synthImpact(sampleRate, 1.15, 82.0, 26.0, 2.8, rng)
            "boom_distant_thunder" -> synthImpact(sampleRate, 1.05, 68.0, 24.0, 3.1, rng)

            // Riser
            "riser_tension" -> synthRiser(sampleRate, 0.90, 150.0, 1180.0, reverse = false, rng = rng)
            "riser_hyper_riser" -> synthRiser(sampleRate, 0.85, 220.0, 1760.0, reverse = false, rng = rng)
            "riser_cyber_charge" -> synthRiser(sampleRate, 0.80, 110.0, 1420.0, reverse = false, rng = rng)

            // Reverse Riser
            "reverse_crystal_suck" -> synthRiser(sampleRate, 0.68, 920.0, 260.0, reverse = true, rng = rng)
            "reverse_vocal_swell" -> synthRiser(sampleRate, 0.72, 640.0, 180.0, reverse = true, rng = rng)

            // Glitch
            "glitch_digital_byte" -> synthGlitch(sampleRate, 0.30, 26, rng)
            "glitch_stutter_burst" -> synthGlitch(sampleRate, 0.34, 38, rng)
            "glitch_modem_chirp" -> synthGlitch(sampleRate, 0.26, 44, rng)

            // Electric Pulse
            "pulse_neon_hum" -> synthElectricPulse(sampleRate, 0.45, 120.0, rng)
            "pulse_tesla_zap" -> synthElectricPulse(sampleRate, 0.32, 240.0, rng)

            // Magical Shimmer
            "shimmer_celestial" -> synthChordShimmer(sampleRate, 0.85, listOf(523.25, 659.25, 783.99, 1046.5, 1318.5))
            "shimmer_aurora_veil" -> synthChordShimmer(sampleRate, 0.90, listOf(440.0, 554.37, 659.25, 880.0, 1108.73))

            // Sparkle
            "shimmer_sparkle" -> synthSparkleArp(sampleRate, 0.58, listOf(1046.5, 1318.5, 1567.98, 2093.0))
            "sparkle_fairy_chime" -> synthSparkleArp(sampleRate, 0.50, listOf(1174.66, 1479.98, 1760.0, 2349.32))

            // Light Sweep
            "light_sweep_laser_sheen" -> synthLightSweep(sampleRate, 0.58, 1900.0, 380.0)
            "light_sweep_prism_sweep" -> synthLightSweep(sampleRate, 0.64, 2400.0, 520.0)
            "light_sweep_anamorphic" -> synthLightSweep(sampleRate, 0.60, 1500.0, 310.0)

            // Transition Hit
            "transition_swoosh_hit" -> synthTransitionHit(sampleRate, 0.58, rng)
            "transition_downbeat" -> synthImpact(sampleRate, 0.62, 125.0, 42.0, 4.8, rng)
            "transition_whip_slam" -> synthTransitionHit(sampleRate, 0.48, rng)

            // Atmospheric Ambience
            "ambient_warm_pad" -> synthPad(sampleRate, 1.25, listOf(146.83, 220.0, 293.66, 349.23))
            "ambient_ethereal_drone" -> synthPad(sampleRate, 1.35, listOf(110.0, 164.81, 220.0, 329.63))

            // Soft Transition
            "soft_trans_breeze" -> synthWhoosh(sampleRate, 0.46, 180.0, 420.0, 1.1, rng)
            "soft_trans_felted" -> synthPad(sampleRate, 0.55, listOf(261.63, 329.63, 392.0))

            // Metallic Impact
            "metal_australia_clang" -> synthMetallicImpact(sampleRate, 0.78, 110.0, listOf(380.0, 712.0, 1185.0, 1840.0), rng)
            "metal_sword_sheen" -> synthMetallicImpact(sampleRate, 0.68, 220.0, listOf(880.0, 1420.0, 2190.0), rng)

            // Logo Reveal
            "logo_studio_chime" -> synthChordShimmer(sampleRate, 1.10, listOf(130.81, 261.63, 392.0, 493.88, 587.33))
            "logo_sonic_bloom" -> synthChordShimmer(sampleRate, 1.00, listOf(146.83, 293.66, 440.0, 554.37, 659.25))

            // Background Music Loops
            "music_lofi_doc_pulse" -> synthMusicLoop(sampleRate, 2.40, listOf(146.83, 174.61, 220.0, 261.63), bpm = 100.0)
            "music_persian_amber_pad" -> synthMusicLoop(sampleRate, 2.40, listOf(146.83, 155.56, 196.00, 220.00), bpm = 92.0)
            "music_cyber_arp_loop" -> synthMusicLoop(sampleRate, 2.00, listOf(164.81, 196.00, 246.94, 329.63), bpm = 120.0)
            "music_deep_cinema_bed" -> synthPad(sampleRate, 2.50, listOf(73.42, 110.0, 146.83, 220.0))

            else -> synthSweep(sampleRate, 0.25, 220.0, 520.0, 16.0, 0.85)
        }
    }

    private fun synthSweep(
        sr: Int,
        dur: Double,
        f0: Double,
        f1: Double,
        decay: Double,
        gain: Double,
        vibratoHz: Double = 0.0
    ): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val p = (t / dur).coerceIn(0.0, 1.0)
            val vib = if (vibratoHz > 0.0) 1.0 + 0.06 * sin(2.0 * PI * vibratoHz * t) else 1.0
            val freq = (f0 + (f1 - f0) * p.pow(0.75)) * vib
            phase += 2.0 * PI * freq / sr
            val env = exp(-t * decay) * sin(minOf(1.0, t / 0.005) * (PI / 2.0))
            val sample = (sin(phase) * env * gain).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthPunchPop(sr: Int, dur: Double, f0: Double, f1: Double, gain: Double): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val p = (t / dur).coerceIn(0.0, 1.0)
            val freq = f0 + (f1 - f0) * p.pow(0.55)
            phase += 2.0 * PI * freq / sr
            val env = exp(-t * 17.0)
            val raw = sin(phase) + 0.35 * sin(phase * 2.0)
            val sample = (kotlin.math.tanh(raw * 1.4) * env * gain).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthDoubleTransient(sr: Int, dur: Double, f1: Double, f2: Double): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val hit1 = exp(-t * 42.0) * sin(2.0 * PI * f1 * t)
            val t2 = (t - dur * 0.38).coerceAtLeast(0.0)
            val hit2 = if (t >= dur * 0.38) exp(-t2 * 38.0) * sin(2.0 * PI * f2 * t2) else 0.0
            val sample = ((hit1 + hit2 * 0.9) * 0.85).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthClick(sr: Int, dur: Double, f1: Double, f2: Double, decay: Double, rng: Random): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val env = exp(-t * decay)
            val tone = 0.65 * sin(2.0 * PI * f1 * t) + 0.35 * sin(2.0 * PI * f2 * t)
            val clickNoise = (rng.nextDouble(-1.0, 1.0)) * exp(-t * (decay * 1.6)) * 0.25
            val sample = ((tone + clickNoise) * env * 0.90).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthWhoosh(sr: Int, dur: Double, fLow: Double, fHigh: Double, sharpness: Double, rng: Random): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        var lp = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val p = (t / dur).coerceIn(0.0, 1.0)
            val bell = sin(p * PI).pow(sharpness)
            val sweepFreq = fLow + (fHigh - fLow) * sin(p * PI)
            phase += 2.0 * PI * sweepFreq / sr
            val white = rng.nextDouble(-1.0, 1.0)
            lp = 0.78 * lp + 0.22 * white
            val sample = ((0.58 * sin(phase) + 0.42 * lp) * bell * 0.90).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthImpact(sr: Int, dur: Double, fStart: Double, fEnd: Double, decay: Double, rng: Random): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val freq = fEnd + (fStart - fEnd) * exp(-t * 14.0)
            phase += 2.0 * PI * freq / sr
            val sub = sin(phase) + 0.30 * sin(phase * 2.0)
            val punchTransient = rng.nextDouble(-1.0, 1.0) * exp(-t * 38.0) * 0.35
            val env = exp(-t * decay)
            val sample = ((sub * env + punchTransient) * 0.92).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthBassDrop(sr: Int, dur: Double, fStart: Double, fEnd: Double, drive: Double): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val p = (t / dur).coerceIn(0.0, 1.0)
            val freq = fStart * (fEnd / fStart).pow(p.pow(0.7))
            phase += 2.0 * PI * freq / sr
            val env = (1.0 - p).pow(0.45)
            val raw = sin(phase) + 0.25 * sin(phase * 1.5)
            val sample = (kotlin.math.tanh(raw * drive) * env * 0.92).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthRiser(sr: Int, dur: Double, f0: Double, f1: Double, reverse: Boolean, rng: Random): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val p = (t / dur).coerceIn(0.0, 1.0)
            val curve = if (reverse) (1.0 - p).pow(1.6) else p.pow(1.8)
            val freq = f0 + (f1 - f0) * curve
            phase += 2.0 * PI * freq / sr
            val env = if (reverse) sin(p * PI) else p.pow(1.2) * (1.0 - 0.15 * (p > 0.95).compareTo(false))
            val air = rng.nextDouble(-1.0, 1.0) * 0.18 * env
            val sample = ((sin(phase) * 0.78 + air) * env * 0.88).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthGlitch(sr: Int, dur: Double, slices: Int, rng: Random): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        val sliceLen = max(1, n / slices)
        val freqs = listOf(240.0, 480.0, 960.0, 1440.0, 1920.0, 2880.0)
        for (i in 0 until n) {
            val sliceIdx = i / sliceLen
            val f = freqs[(sliceIdx * 7 + presetHash(sliceIdx)) % freqs.size]
            val gate = if (sliceIdx % 4 == 2) 0.12 else 0.90
            val t = i.toDouble() / sr
            val sq = if (sin(2.0 * PI * f * t) >= 0.0) 0.75 else -0.75
            val noise = rng.nextDouble(-0.25, 0.25)
            val sample = ((sq + noise) * gate * 0.78).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun presetHash(v: Int): Int = abs((v * 1103515245 + 12345))

    private fun synthElectricPulse(sr: Int, dur: Double, baseHz: Double, rng: Random): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val p = (t / dur).coerceIn(0.0, 1.0)
            val env = sin(p * PI)
            val buzz = sin(2.0 * PI * baseHz * t) +
                0.5 * sin(2.0 * PI * (baseHz * 3.0) * t) +
                0.3 * sin(2.0 * PI * (baseHz * 7.0) * t)
            val spark = rng.nextDouble(-0.3, 0.3) * (0.5 + 0.5 * sin(2.0 * PI * 28.0 * t))
            val sample = ((buzz * 0.55 + spark) * env * 0.85).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthChordShimmer(sr: Int, dur: Double, chordHz: List<Double>): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val p = (t / dur).coerceIn(0.0, 1.0)
            val env = sin(p * PI).pow(0.65) * exp(-t * 1.4)
            var sum = 0.0
            chordHz.forEachIndexed { idx, hz ->
                val detune = 1.0 + (idx - 2) * 0.0025 * sin(2.0 * PI * 4.5 * t)
                sum += sin(2.0 * PI * hz * detune * t) / chordHz.size
            }
            val sample = (sum * env * 0.92).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthSparkleArp(sr: Int, dur: Double, notesHz: List<Double>): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        val stepSec = dur / max(1, notesHz.size)
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            var sum = 0.0
            notesHz.forEachIndexed { idx, hz ->
                val noteStart = idx * stepSec * 0.65
                if (t >= noteStart) {
                    val localT = t - noteStart
                    val env = exp(-localT * 9.5)
                    sum += sin(2.0 * PI * hz * localT) * env * 0.55
                }
            }
            val sample = sum.coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthLightSweep(sr: Int, dur: Double, fStart: Double, fEnd: Double): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val p = (t / dur).coerceIn(0.0, 1.0)
            val freq = fStart + (fEnd - fStart) * p
            phase += 2.0 * PI * freq / sr
            val env = sin(p * PI)
            val shimmer = sin(phase) * 0.65 + sin(phase * 1.5) * 0.35
            val sample = (shimmer * env * 0.85).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthTransitionHit(sr: Int, dur: Double, rng: Random): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        val hitPoint = dur * 0.42
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val sample = if (t < hitPoint) {
                val p = (t / hitPoint).coerceIn(0.0, 1.0)
                val f = 180.0 + 720.0 * p.pow(1.6)
                phase += 2.0 * PI * f / sr
                (sin(phase) * 0.6 + rng.nextDouble(-0.3, 0.3)) * p
            } else {
                val lt = t - hitPoint
                val f = 42.0 + 95.0 * exp(-lt * 18.0)
                phase += 2.0 * PI * f / sr
                sin(phase) * exp(-lt * 6.5) * 0.95
            }
            out[i] = (sample.coerceIn(-0.98, 0.98) * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthMetallicImpact(
        sr: Int,
        dur: Double,
        subHz: Double,
        partialsHz: List<Double>,
        rng: Random
    ): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val sub = sin(2.0 * PI * subHz * exp(-t * 4.0) * t) * exp(-t * 5.0) * 0.55
            var metal = 0.0
            partialsHz.forEachIndexed { idx, hz ->
                metal += sin(2.0 * PI * hz * t) * exp(-t * (4.5 + idx * 1.8)) / partialsHz.size
            }
            val transient = rng.nextDouble(-1.0, 1.0) * exp(-t * 45.0) * 0.25
            val sample = ((sub + metal * 0.65 + transient) * 0.92).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthPad(sr: Int, dur: Double, chordHz: List<Double>): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val p = (t / dur).coerceIn(0.0, 1.0)
            val env = sin(p * PI).pow(0.5)
            var sum = 0.0
            chordHz.forEach { hz ->
                sum += (sin(2.0 * PI * hz * t) + 0.25 * cos(2.0 * PI * (hz * 2.0) * t)) / chordHz.size
            }
            val sample = (sum * env * 0.78).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun synthMusicLoop(sr: Int, dur: Double, notesHz: List<Double>, bpm: Double): ShortArray {
        val n = (sr * dur).toInt()
        val out = ShortArray(n)
        val beatSec = 60.0 / bpm
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val stepIdx = ((t / (beatSec * 0.5)).toInt()) % notesHz.size
            val localBeat = (t % (beatSec * 0.5))
            val noteHz = notesHz[stepIdx]
            val bassEnv = exp(-localBeat * 5.5)
            val pulseEnv = exp(-(t % beatSec) * 9.0)
            val pad = sin(2.0 * PI * notesHz[0] * t) * 0.32 + sin(2.0 * PI * notesHz[2 % notesHz.size] * t) * 0.24
            val arp = sin(2.0 * PI * (noteHz * 2.0) * t) * bassEnv * 0.35
            val kick = sin(2.0 * PI * (58.0 + 60.0 * exp(-(t % beatSec) * 28.0)) * (t % beatSec)) * pulseEnv * 0.32
            val fadeEdge = when {
                t < 0.04 -> t / 0.04
                t > dur - 0.04 -> (dur - t) / 0.04
                else -> 1.0
            }
            val sample = ((pad + arp + kick) * fadeEdge * 0.85).coerceIn(-0.98, 0.98)
            out[i] = (sample * 32767.0).toInt().toShort()
        }
        return out
    }

    private fun encodeWavPcm16(samples: ShortArray, sampleRate: Int): ByteArray {
        val pcmBytes = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { pcmBytes.putShort(it) }
        val data = pcmBytes.array()

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + data.size)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1) // PCM
        header.putShort(1) // Mono
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(data.size)

        val bos = ByteArrayOutputStream(44 + data.size)
        bos.write(header.array())
        bos.write(data)
        return bos.toByteArray()
    }

    /**
     * Resolves an audio clip source string (absolute path, `file://`, `content://`, `asset:///`,
     * or relative `user_audio/` / `sfx/` path) to a local readable File if it exists on disk.
     */
    private fun resolveAudioClipFile(source: String): File? {
        val clean = source.trim()
        if (clean.isEmpty()) return null

        if (clean.startsWith("content://")) {
            val uri = runCatching { Uri.parse(clean) }.getOrNull() ?: return null
            val cached = File(context.cacheDir, "decoded_uri_${abs(clean.hashCode())}.audio")
            if (!cached.exists() || cached.length() < 64L) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        cached.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
            return if (cached.exists() && cached.length() > 44L) cached else null
        }

        if (clean.startsWith("asset:///")) {
            val assetPath = clean.removePrefix("asset:///")
            val cached = File(context.cacheDir, "decoded_asset_${assetPath.replace('/', '_')}")
            if (!cached.exists() || cached.length() < 64L) {
                runCatching {
                    context.assets.open(assetPath).use { input ->
                        cached.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
            return if (cached.exists() && cached.length() > 44L) cached else null
        }

        val rawPath = if (clean.startsWith("file://")) {
            runCatching { Uri.parse(clean).path }.getOrNull() ?: clean.removePrefix("file://")
        } else {
            clean
        }

        val candidates = listOf(
            File(rawPath),
            File(context.filesDir, rawPath),
            File(userAudioDir, rawPath.substringAfterLast('/')),
            File(sfxDir, rawPath.substringAfterLast('/'))
        )
        return candidates.firstOrNull { it.exists() && it.isFile && it.length() > 44L }
    }

    /**
     * Decodes a RIFF/WAVE byte array (8-bit, 16-bit, 24-bit, 32-bit PCM or 32-bit IEEE float,
     * mono or multi-channel stereo) into normalized [-1.0f .. 1.0f] mono float samples at `targetSampleRate`.
     */
    fun decodeRiffWavBytes(bytes: ByteArray, targetSampleRate: Int): FloatArray? {
        if (bytes.size < 44) return null
        val headerTag = String(bytes, 0, 4, Charsets.US_ASCII)
        val waveTag = String(bytes, 8, 4, Charsets.US_ASCII)
        if (headerTag != "RIFF" || waveTag != "WAVE") return null

        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var offset = 12
        var audioFormat = 1
        var numChannels = 1
        var sampleRate = targetSampleRate
        var bitsPerSample = 16
        var dataOffset = -1
        var dataSize = 0

        while (offset + 8 <= bytes.size) {
            val chunkId = String(bytes, offset, 4, Charsets.US_ASCII)
            val chunkSize = buf.getInt(offset + 4).coerceAtLeast(0)
            val chunkDataStart = offset + 8
            if (chunkId == "fmt " && chunkDataStart + 16 <= bytes.size) {
                audioFormat = buf.getShort(chunkDataStart).toInt() and 0xFFFF
                numChannels = (buf.getShort(chunkDataStart + 2).toInt() and 0xFFFF).coerceAtLeast(1)
                sampleRate = buf.getInt(chunkDataStart + 4).coerceAtLeast(8000)
                bitsPerSample = (buf.getShort(chunkDataStart + 14).toInt() and 0xFFFF).coerceAtLeast(8)
            } else if (chunkId == "data") {
                dataOffset = chunkDataStart
                dataSize = min(chunkSize, bytes.size - chunkDataStart)
                break
            }
            val paddedChunkSize = chunkSize + (chunkSize and 1)
            if (paddedChunkSize < 0 || chunkDataStart + paddedChunkSize <= offset) break
            offset = chunkDataStart + paddedChunkSize
        }

        if (dataOffset < 0 || dataSize <= 0) return null
        val bytesPerSample = (bitsPerSample / 8).coerceAtLeast(1)
        val frameSize = bytesPerSample * numChannels
        val totalFrames = dataSize / frameSize
        if (totalFrames <= 0) return null

        val monoNative = FloatArray(totalFrames)
        for (f in 0 until totalFrames) {
            val framePos = dataOffset + f * frameSize
            var channelSum = 0f
            for (ch in 0 until numChannels) {
                val samplePos = framePos + ch * bytesPerSample
                val sampleVal = when {
                    audioFormat == 3 && bitsPerSample == 32 && samplePos + 4 <= bytes.size -> {
                        buf.getFloat(samplePos).coerceIn(-1f, 1f)
                    }
                    bitsPerSample == 8 && samplePos + 1 <= bytes.size -> {
                        val u = bytes[samplePos].toInt() and 0xFF
                        ((u - 128) / 128f).coerceIn(-1f, 1f)
                    }
                    bitsPerSample == 16 && samplePos + 2 <= bytes.size -> {
                        (buf.getShort(samplePos) / 32768f).coerceIn(-1f, 1f)
                    }
                    bitsPerSample == 24 && samplePos + 3 <= bytes.size -> {
                        val b0 = bytes[samplePos].toInt() and 0xFF
                        val b1 = bytes[samplePos + 1].toInt() and 0xFF
                        val b2 = bytes[samplePos + 2].toInt() // signed MSB
                        val s24 = (b2 shl 16) or (b1 shl 8) or b0
                        (s24 / 8388608f).coerceIn(-1f, 1f)
                    }
                    bitsPerSample == 32 && samplePos + 4 <= bytes.size -> {
                        (buf.getInt(samplePos) / 2147483648f).coerceIn(-1f, 1f)
                    }
                    else -> 0f
                }
                channelSum += sampleVal
            }
            monoNative[f] = (channelSum / numChannels).coerceIn(-1f, 1f)
        }

        return resampleMonoFloat(monoNative, sampleRate, targetSampleRate)
    }

    private fun resampleMonoFloat(src: FloatArray, srcRate: Int, dstRate: Int): FloatArray {
        if (src.isEmpty() || srcRate == dstRate || srcRate <= 0 || dstRate <= 0) return src
        val outLen = ((src.size.toLong() * dstRate) / srcRate).toInt().coerceAtLeast(1)
        val out = FloatArray(outLen)
        val ratio = srcRate.toDouble() / dstRate.toDouble()
        for (i in 0 until outLen) {
            val srcPos = i * ratio
            val idx0 = srcPos.toInt().coerceIn(0, src.lastIndex)
            val idx1 = (idx0 + 1).coerceAtMost(src.lastIndex)
            val frac = (srcPos - idx0).toFloat()
            out[i] = src[idx0] * (1f - frac) + src[idx1] * frac
        }
        return out
    }

    /**
     * Decodes compressed audio files (MP3, M4A, AAC, OGG, FLAC, MP4) using Android's
     * `MediaExtractor` + `MediaCodec`, with a host `ffmpeg` fallback when running in JVM unit tests.
     */
    private fun decodeCompressedAudioFile(file: File, targetSampleRate: Int): FloatArray? {
        // 1. Try Android MediaExtractor + MediaCodec
        val codecResult = runCatching {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(file.absolutePath)
                var audioTrackIdx = -1
                var format: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val f = extractor.getTrackFormat(i)
                    val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) {
                        audioTrackIdx = i
                        format = f
                        break
                    }
                }
                if (audioTrackIdx < 0 || format == null) return@runCatching null
                extractor.selectTrack(audioTrackIdx)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: return@runCatching null
                var srcSampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                } else targetSampleRate
                var channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                } else 1

                val codec = MediaCodec.createDecoderByType(mime)
                codec.configure(format, null, null, 0)
                codec.start()

                val pcmOut = ByteArrayOutputStream()
                val info = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false
                var safetyLoops = 0

                while (!outputDone && safetyLoops < 20000) {
                    safetyLoops++
                    if (!inputDone) {
                        val inIdx = codec.dequeueInputBuffer(5000)
                        if (inIdx >= 0) {
                            val inBuf = codec.getInputBuffer(inIdx)
                            if (inBuf != null) {
                                val read = extractor.readSampleData(inBuf, 0)
                                if (read < 0) {
                                    codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    inputDone = true
                                } else {
                                    val pts = extractor.sampleTime
                                    codec.queueInputBuffer(inIdx, 0, read, pts, 0)
                                    extractor.advance()
                                }
                            }
                        }
                    }

                    val outIdx = codec.dequeueOutputBuffer(info, 5000)
                    when {
                        outIdx >= 0 -> {
                            val outBuf = codec.getOutputBuffer(outIdx)
                            if (outBuf != null && info.size > 0) {
                                outBuf.position(info.offset)
                                outBuf.limit(info.offset + info.size)
                                val chunk = ByteArray(info.size)
                                outBuf.get(chunk)
                                pcmOut.write(chunk)
                            }
                            codec.releaseOutputBuffer(outIdx, false)
                            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                outputDone = true
                            }
                        }
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val newFmt = codec.outputFormat
                            if (newFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                                srcSampleRate = newFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            }
                            if (newFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                                channels = newFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                            }
                        }
                    }
                }
                codec.stop()
                codec.release()

                val rawBytes = pcmOut.toByteArray()
                if (rawBytes.size < 4) return@runCatching null
                val shortBuf = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                val totalShorts = shortBuf.remaining()
                val totalFrames = totalShorts / channels
                if (totalFrames <= 0) return@runCatching null
                val mono = FloatArray(totalFrames)
                for (f in 0 until totalFrames) {
                    var sum = 0f
                    for (c in 0 until channels) {
                        sum += shortBuf.get(f * channels + c) / 32768f
                    }
                    mono[f] = (sum / channels).coerceIn(-1f, 1f)
                }
                resampleMonoFloat(mono, srcSampleRate, targetSampleRate)
            } finally {
                runCatching { extractor.release() }
            }
        }.getOrNull()

        if (codecResult != null && codecResult.isNotEmpty()) {
            return codecResult
        }

        // 2. Host FFmpeg CLI fallback (for JVM / container test environments)
        return runCatching {
            val proc = ProcessBuilder(
                "ffmpeg", "-v", "error",
                "-i", file.absolutePath,
                "-f", "s16le", "-ac", "1", "-ar", targetSampleRate.toString(),
                "-"
            ).start()
            val rawBytes = proc.inputStream.readBytes()
            proc.waitFor()
            if (rawBytes.size < 4) return@runCatching null
            val shortBuf = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val out = FloatArray(shortBuf.remaining())
            for (i in out.indices) {
                out[i] = (shortBuf.get(i) / 32768f).coerceIn(-1f, 1f)
            }
            out
        }.getOrNull()
    }

    /**
     * Decodes any user-imported or bundled audio file (`clip.source`) to mono float samples at `targetSampleRate`.
     */
    fun decodeAudioClipSamples(source: String, targetSampleRate: Int = 22050): FloatArray? {
        val file = resolveAudioClipFile(source) ?: return null
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        decodeRiffWavBytes(bytes, targetSampleRate)?.let { return it }
        return decodeCompressedAudioFile(file, targetSampleRate)
    }

    private fun interpolateVolumeKeyframes(
        keyframes: List<AudioVolumeKeyframe>,
        timeInClipSec: Double
    ): Float {
        if (keyframes.isEmpty()) return 1f
        val sorted = keyframes.sortedBy { it.time }
        val prev = sorted.lastOrNull { it.time <= timeInClipSec } ?: sorted.first()
        val next = sorted.firstOrNull { it.time >= timeInClipSec } ?: sorted.last()
        if (prev == next || abs(next.time - prev.time) < 0.001) {
            return prev.volume.toFloat().coerceIn(0f, 2f)
        }
        val t = ((timeInClipSec - prev.time) / (next.time - prev.time)).coerceIn(0.0, 1.0)
        return (prev.volume + (next.volume - prev.volume) * t).toFloat().coerceIn(0f, 2f)
    }

    /**
     * Synthesizes and mixes a continuous WAV stem spanning [project.trim.start .. project.trim.end]
     * containing:
     * 1. Actual decoded user-imported audio files (or studio SFX presets) for all timeline `AudioClip` items,
     *    strictly respecting `clip.source`, `clip.start`, `clip.trimStart`, `clip.volume`, `clip.fadeIn`,
     *    `clip.fadeOut`, `clip.duration`, `clip.loop`, `clip.keyframes`, and `clip.ducking`.
     * 2. Cue entrance SFX and word-level SFX triggers.
     */
    fun renderProjectSfxStemWav(project: Project, outFile: File): File {
        val sr = 22050
        val trimStart = project.trim.start.coerceAtLeast(0.0)
        val trimEnd = project.trim.end.coerceAtLeast(trimStart + 0.5)
        val durationSec = trimEnd - trimStart
        val totalSamples = (durationSec * sr).toInt().coerceAtLeast(sr / 2)
        val mixBuffer = FloatArray(totalSamples)

        val masterSfxGain = (project.effects.sfxVolume.coerceIn(0, 100) / 100f)
        val masterMusicGain = (project.effects.musicVolume.coerceIn(0, 100) / 100f)
        val mode = project.effects.sfxMode

        val activeSpeechWindows: List<Pair<Double, Double>> = project.tracks
            .filter { it.visible }
            .flatMap { tr -> tr.captions.map { it.start to it.end } }

        fun mixPresetAt(presetQuery: String, timelineTimeSec: Double, clipVolume: Float, maxDurSec: Double = 3.0) {
            val preset = findPreset(presetQuery) ?: return
            val relSec = timelineTimeSec - trimStart
            if (relSec < -1.0 || relSec > durationSec) return
            val samples = synthesizePresetSamples(preset.id, sr)
            val maxSamples = minOf(samples.size, (maxDurSec * sr).toInt())
            val startIdx = (relSec * sr).toInt()
            for (i in 0 until maxSamples) {
                val dstIdx = startIdx + i
                if (dstIdx in 0 until totalSamples) {
                    mixBuffer[dstIdx] = (mixBuffer[dstIdx] + (samples[i] / 32768f) * clipVolume).coerceIn(-0.98f, 0.98f)
                }
            }
        }

        fun mixTimelineAudioClip(clip: AudioClip) {
            val masterGain = if (clip.isMusic) masterMusicGain else masterSfxGain
            // Decode actual user-imported audio file from clip.source first; only fall back to preset if no file exists
            val decodedFileSamples: FloatArray? = decodeAudioClipSamples(clip.source, sr)
            val rawSamples: FloatArray = if (decodedFileSamples != null && decodedFileSamples.isNotEmpty()) {
                decodedFileSamples
            } else {
                val presetKey = clip.source.substringAfterLast('/').substringBeforeLast('.')
                    .ifBlank { clip.category.ifBlank { clip.name } }
                val preset = findPreset(presetKey) ?: findPreset(clip.category) ?: findPreset(clip.name) ?: return
                val shorts = synthesizePresetSamples(preset.id, sr)
                FloatArray(shorts.size) { idx -> shorts[idx] / 32768f }
            }

            if (rawSamples.isEmpty()) return

            // Respect clip.trimStart offset into the decoded source audio
            val sourceTrimOffsetSamples = (clip.trimStart.coerceAtLeast(0.0) * sr).toInt()
            if (!clip.loop && sourceTrimOffsetSamples >= rawSamples.size) return

            val availableSourceSamples = if (clip.loop) {
                rawSamples.size
            } else {
                (rawSamples.size - sourceTrimOffsetSamples).coerceAtLeast(0)
            }
            if (availableSourceSamples <= 0) return

            // Respect clip.duration (clamped to available source audio unless loop == true)
            val requestedClipDurSec = clip.duration.coerceAtLeast(0.05)
            val maxAvailableDurSec = availableSourceSamples.toDouble() / sr
            val effectiveClipDurSec = if (clip.loop) {
                requestedClipDurSec
            } else {
                min(requestedClipDurSec, maxAvailableDurSec)
            }
            val clipOutSampleCount = (effectiveClipDurSec * sr).toInt()
            if (clipOutSampleCount <= 0) return

            val relStartSec = clip.start - trimStart
            val dstStartSample = (relStartSec * sr).toInt()
            val fadeInSec = clip.fadeIn.coerceAtLeast(0.0)
            val fadeOutSec = clip.fadeOut.coerceAtLeast(0.0)
            val fadeOutStartSec = (effectiveClipDurSec - fadeOutSec).coerceAtLeast(0.0)
            val baseClipGain = (clip.volume.toFloat() * masterGain).coerceIn(0f, 2.0f)

            for (i in 0 until clipOutSampleCount) {
                val dstIdx = dstStartSample + i
                if (dstIdx !in 0 until totalSamples) continue

                val srcIdx = if (clip.loop) {
                    (sourceTrimOffsetSamples + i) % rawSamples.size
                } else {
                    sourceTrimOffsetSamples + i
                }
                if (srcIdx !in rawSamples.indices) break

                val tInClipSec = i.toDouble() / sr
                val absTimelineSec = clip.start + tInClipSec

                val fadeInFactor = if (fadeInSec > 0.001 && tInClipSec < fadeInSec) {
                    (tInClipSec / fadeInSec).toFloat().coerceIn(0f, 1f)
                } else 1f

                val fadeOutFactor = if (fadeOutSec > 0.001 && tInClipSec > fadeOutStartSec) {
                    ((effectiveClipDurSec - tInClipSec) / fadeOutSec).toFloat().coerceIn(0f, 1f)
                } else 1f

                val kfFactor = interpolateVolumeKeyframes(clip.keyframes, tInClipSec)
                val duckFactor = if (clip.isMusic && clip.ducking &&
                    activeSpeechWindows.any { absTimelineSec >= it.first && absTimelineSec <= it.second }
                ) {
                    0.42f
                } else {
                    1.0f
                }

                val sampleGain = baseClipGain * fadeInFactor * fadeOutFactor * kfFactor * duckFactor
                mixBuffer[dstIdx] = (mixBuffer[dstIdx] + rawSamples[srcIdx] * sampleGain).coerceIn(-0.98f, 0.98f)
            }
        }

        // 1. Mix all Timeline AudioClips (user-imported audio files + SFX/music clips)
        project.audioClips.forEach { clip ->
            mixTimelineAudioClip(clip)
        }

        // 2. Mix Cue entrance SFX & word-level SFX triggers
        project.tracks.filter { it.visible }.forEach { track ->
            track.captions.forEach { cue ->
                if (mode.equals("Sentence", ignoreCase = true) || mode.equals("Both", ignoreCase = true)) {
                    val cueSfx = cue.entranceSfx.takeIf { !it.equals("None", ignoreCase = true) && it.isNotBlank() }
                        ?: project.effects.sfx.takeIf { !it.equals("None", ignoreCase = true) && it.isNotBlank() }
                    if (cueSfx != null) {
                        mixPresetAt(cueSfx, cue.start, masterSfxGain * 0.85f)
                    }
                }
                if (mode.equals("Word", ignoreCase = true) || mode.equals("Both", ignoreCase = true)) {
                    cue.words.forEach { w ->
                        if (w.sfx.isNotBlank() && !w.sfx.equals("None", ignoreCase = true)) {
                            mixPresetAt(w.sfx, w.start, masterSfxGain * 0.92f)
                        }
                    }
                }
            }
        }

        val pcmShorts = ShortArray(totalSamples) { idx ->
            (mixBuffer[idx].coerceIn(-0.98f, 0.98f) * 32767f).toInt().toShort()
        }
        outFile.parentFile?.mkdirs()
        outFile.writeBytes(encodeWavPcm16(pcmShorts, sr))
        return outFile
    }
}

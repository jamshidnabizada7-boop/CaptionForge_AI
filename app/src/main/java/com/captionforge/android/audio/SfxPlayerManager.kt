package com.captionforge.android.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

class SfxPlayerManager(private val context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val loadedSoundIds = mutableMapOf<String, Int>()

    private val categoryToAsset = mapOf(
        "Pop" to "sfx/pop_crisp.wav",
        "Click" to "sfx/click_tactile.wav",
        "Whoosh" to "sfx/whoosh_cinematic_whip.wav",
        "Shimmer" to "sfx/shimmer_celestial.wav",
        "Impact" to "sfx/impact_heavy_thump.wav",
        "Riser" to "sfx/riser_tension.wav",
        "Bass" to "sfx/bass_808_drop.wav",
        "Glitch" to "sfx/glitch_digital_byte.wav",
        "Light Sweep" to "sfx/light_sweep_laser_sheen.wav",
        "Transition" to "sfx/transition_swoosh_hit.wav",
        "Ambient" to "sfx/ambient_warm_pad.wav"
    )

    init {
        preloadAllCategories()
    }

    private fun preloadAllCategories() {
        for ((category, assetPath) in categoryToAsset) {
            try {
                context.assets.openFd(assetPath).use { afd ->
                    val id = soundPool.load(afd, 1)
                    loadedSoundIds[category] = id
                }
            } catch (_: Exception) {
                // Fallback: synthesize WAV file into cache and load into SoundPool
                try {
                    val wavFile = synthesizeCategoryWav(category)
                    val id = soundPool.load(wavFile.absolutePath, 1)
                    loadedSoundIds[category] = id
                } catch (_: Exception) {
                }
            }
        }
    }

    fun playCategory(category: String, volumePercent: Int = 70) {
        if (category == "None" || category.isBlank()) return
        val vol = (volumePercent.coerceIn(0, 100) / 100f)
        val soundId = loadedSoundIds[category]
        if (soundId != null && soundId != 0) {
            soundPool.play(soundId, vol, vol, 1, 0, 1f)
        } else {
            try {
                val wavFile = synthesizeCategoryWav(category)
                MediaPlayer().apply {
                    setDataSource(wavFile.absolutePath)
                    setVolume(vol, vol)
                    setOnCompletionListener { it.release() }
                    prepare()
                    start()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun synthesizeCategoryWav(category: String): File {
        val outFile = File(context.cacheDir, "synth_${category.lowercase().replace(" ", "_")}.wav")
        if (outFile.exists() && outFile.length() > 100) return outFile

        val sampleRate = 44100
        val durationSec = when (category) {
            "Click" -> 0.04
            "Pop" -> 0.09
            "Whoosh", "Transition", "Light Sweep" -> 0.38
            "Riser", "Bass", "Impact", "Ambient" -> 0.50
            else -> 0.30
        }
        val numSamples = (sampleRate * durationSec).toInt()
        val pcmBytes = ByteBuffer.allocate(numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val p = i.toDouble() / numSamples
            val raw = when (category) {
                "Pop" -> {
                    val freq = 950.0 * (280.0 / 950.0).pow(p)
                    val env = (1.0 - p).pow(2.8)
                    sin(2.0 * PI * freq * t) * env * 0.85
                }
                "Click" -> {
                    val env = (1.0 - p).pow(5.0)
                    sin(2.0 * PI * 1600.0 * t) * env * 0.9
                }
                "Shimmer", "Light Sweep" -> {
                    val env = (1.0 - p).pow(1.4)
                    (sin(2.0 * PI * 1150.0 * t) * 0.4 +
                        sin(2.0 * PI * 1720.0 * t) * 0.35 +
                        sin(2.0 * PI * 2300.0 * t) * 0.25) * env * 0.8
                }
                "Impact", "Bass" -> {
                    val env = exp(-5.0 * p)
                    val freq = 65.0 + 30.0 * (1.0 - p)
                    sin(2.0 * PI * freq * t) * env * 0.9
                }
                "Riser" -> {
                    val freq = 160.0 + 900.0 * p.pow(2.0)
                    sin(2.0 * PI * freq * t) * p * 0.8
                }
                else -> {
                    val env = sin(PI * p).pow(1.8)
                    val freq = 520.0 + 380.0 * sin(PI * p)
                    sin(2.0 * PI * freq * t) * env * 0.75
                }
            }
            val clamped = max(-1.0, min(1.0, raw))
            pcmBytes.putShort((clamped * 32767.0).toInt().toShort())
        }

        val dataSize = numSamples * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1) // PCM
            putShort(1) // Mono
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }

        FileOutputStream(outFile).use { fos ->
            fos.write(header.array())
            fos.write(pcmBytes.array())
        }
        return outFile
    }

    fun release() {
        try {
            soundPool.release()
        } catch (_: Exception) {
        }
    }
}

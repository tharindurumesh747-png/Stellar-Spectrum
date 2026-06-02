package com.example.core

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundSynth {
    private const val SAMPLE_RATE = 22050
    private var enabled = true
    private val scope = CoroutineScope(Dispatchers.Default)

    fun setEnabled(state: Boolean) {
        enabled = state
    }

    fun playShootRed() {
        if (!enabled) return
        scope.launch {
            val duration = 0.15f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                // downward sweep from 800Hz to 200Hz
                val freq = 800f - (t / duration) * 600f
                val phase = 2.0 * Math.PI * freq * t
                buffer[i] = (sin(phase) * 16000 * (1.0f - t / duration)).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playShootBlue() {
        if (!enabled) return
        scope.launch {
            val duration = 0.25f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                // slow deep laser zoom 400Hz to 100Hz
                val freq = 400f - (t / duration) * 300f
                val phase = 2.0 * Math.PI * freq * t
                buffer[i] = (sin(phase) * 18000 * (1.0f - t / duration)).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playShootGreen() {
        if (!enabled) return
        scope.launch {
            val duration = 0.12f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                // sharp rapid chirp sweep 1200Hz to 600Hz
                val freq = 1200f - (t / duration) * 600f
                val phase = 2.0 * Math.PI * freq * t
                buffer[i] = (sin(phase) * 14000 * (1.0f - t/duration)).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playShootPurple() {
        if (!enabled) return
        scope.launch {
            val duration = 0.22f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                // vibrato laser, freq wobbles
                val freq = 600f + sin(2.0 * Math.PI * 40.0 * t).toFloat() * 100f - (t / duration) * 200f
                val phase = 2.0 * Math.PI * freq * t
                buffer[i] = (sin(phase) * 15000 * (1.0f - t/duration)).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playHitCorrect() {
        if (!enabled) return
        scope.launch {
            val duration = 0.2f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                // white noise explosion mixed with pitch sweep
                val noise = (Math.random() * 2.0 - 1.0).toFloat()
                val signalfreq = 300f * (1.0f - t/duration)
                val phase = 2.0 * Math.PI * signalfreq * t
                val sampleValue = (sin(phase) * 0.4f + noise * 0.6f) * 18000 * (1.0f - t/duration)
                buffer[i] = sampleValue.toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playHitWrong() {
        if (!enabled) return
        scope.launch {
            val duration = 0.4f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                // low discordant flat sound
                val soundVal1 = sin(2.0 * Math.PI * 110.0 * t)
                val soundVal2 = sin(2.0 * Math.PI * 117.0 * t) // detuned dissonance
                buffer[i] = ((soundVal1 + soundVal2) * 8000 * (1.0f - t / duration)).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playBossDie() {
        if (!enabled) return
        scope.launch {
            val duration = 0.8f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val noise = (Math.random() * 2.0 - 1.0).toFloat()
                val pitch = 150f * (1.0f - t / duration) + 40f
                val phase = 2.0 * Math.PI * pitch * t
                buffer[i] = ((sin(phase) * 0.2f + noise * 0.8f) * 22000 * (1.0f - t / duration)).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playPowerup() {
        if (!enabled) return
        scope.launch {
            val duration = 0.4f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                // upward chime arpeggio: C (261.6Hz), E (329.6Hz), G (392Hz), C(523Hz)
                val note = when {
                    t < 0.1f -> 261.6
                    t < 0.2f -> 329.6
                    t < 0.3f -> 392.0
                    else -> 523.2
                }
                val phase = 2.0 * Math.PI * note * t
                buffer[i] = (sin(phase) * 15000 * (1.0f - t/duration)).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playUiClick() {
        if (!enabled) return
        scope.launch {
            val duration = 0.05f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val phase = 2.0 * Math.PI * 1000.0 * t
                buffer[i] = (sin(phase) * 12000 * (1.0f - t/duration)).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    private fun playPcm(buffer: ShortArray) {
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBufSize, buffer.size * 2),
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            // Wait for duration to release tracking resource
            val sleepTimeMs = (buffer.size.toFloat() / SAMPLE_RATE * 1000).toLong()
            Thread.sleep(sleepTimeMs + 50)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

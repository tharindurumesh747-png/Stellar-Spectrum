package com.example.core

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.sin

object BgmEngine {
    private const val SAMPLE_RATE = 22050
    private var enabled = true
    private var currentWorld = 1
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start(worldIndex: Int) {
        currentWorld = worldIndex
        if (!enabled) return
        stop()
        job = scope.launch {
            runBgmLoop()
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun setEnabled(state: Boolean) {
        enabled = state
        if (!enabled) {
            stop()
        } else {
            start(currentWorld)
        }
    }

    private suspend fun runBgmLoop() = withContext(Dispatchers.Default) {
        var audioTrack: AudioTrack? = null
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            // We want to generate small 0.5-second loop chunks dynamically and write them continuously
            val loopDuration = 0.5f // seconds
            val numSamples = (loopDuration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBufSize, numSamples * 2),
                AudioTrack.MODE_STREAM
            )
            
            audioTrack.play()
            
            var cycleIndex = 0
            while (isActive) {
                generateWaveform(currentWorld, buffer, cycleIndex, numSamples)
                audioTrack.write(buffer, 0, buffer.size)
                cycleIndex++
                yield() // Cooperate with cancellation
            }
        } catch (e: CancellationException) {
            // normal termination
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {
                // Ignore secondary errors on release
            }
        }
    }

    private fun generateWaveform(world: Int, buffer: ShortArray, cycleIndex: Int, numSamples: Int) {
        val secondsOfPrevious = cycleIndex * 0.5
        
        // Define synth parameters based on world vibe
        val tempoBpm = when (world) {
            1 -> 120.0  // Neon Orbit: classic synthwave
            2 -> 130.0  // Cyber Nebula: faster, heavy cyberpunk
            3 -> 110.0  // Quantum Gateway: pulsing space
            4 -> 140.0  // Crystal Galaxy: hyper-techno
            5 -> 95.0   // Dark Matter: tense cinematic slow bass
            else -> 150.0 // Singularity Core: epic final boss arena
        }
        
        val crotchetSec = 60.0 / tempoBpm
        val stepSec = crotchetSec / 2.0 // eight note steps
        
        // Progression frequencies (I - V - vi - IV chord changes)
        // Set bass frequencies based on progression index (1 step = 0.5sec, 8 steps per progression loop)
        val stepIndex = (secondsOfPrevious / stepSec).toInt() % 8
        val baseFreq = when (world) {
            1 -> { // C2, G2, A2, F2
                when (stepIndex / 2) {
                    0 -> 65.4  // C2
                    1 -> 98.0  // G2
                    2 -> 110.0 // A2
                    else -> 87.3 // F2
                }
            }
            2 -> { // Eb2, Bb2, C2, Ab2 (Cyber / minor)
                when (stepIndex / 2) {
                    0 -> 77.8  // Eb2
                    1 -> 116.5 // Bb2
                    2 -> 65.4  // C2
                    else -> 103.8 // Ab2
                }
            }
            3 -> { // D2, F#2, G2, A2 (Warp Gateway / Major-ish Lift)
                when (stepIndex / 2) {
                    0 -> 73.4  // D2
                    1 -> 92.5  // F#2
                    2 -> 98.0  // G2
                    else -> 110.0 // A2
                }
            }
            4 -> { // E2, G2, A2, B2 (High crystal sparkle)
                when (stepIndex / 2) {
                    0 -> 82.4  // E2
                    1 -> 98.0  // G2
                    2 -> 110.0 // A2
                    else -> 123.5 // B2
                }
            }
            5 -> { // F2, E2, Db2, C2 (Dark matter tension descending chrom)
                when (stepIndex / 2) {
                    0 -> 87.3  // F2
                    1 -> 82.4  // E2
                    2 -> 69.3  // Db2
                    else -> 65.4  // C2
                }
            }
            else -> { // G2, Ab2, F2, F#2 (OMEGA SINGULARITY chromatic tense)
                when (stepIndex / 2) {
                    0 -> 98.0  // G2
                    1 -> 103.9 // Ab2
                    2 -> 87.3  // F2
                    else -> 92.5  // F#2
                }
            }
        }

        // Fill buffer
        for (i in 0 until numSamples) {
            val t = secondsOfPrevious + (i.toDouble() / SAMPLE_RATE)
            
            // Bass: arpeggiator or straight bassline pulsing with the beat
            val pulseStep = (t / stepSec).toInt() % 2
            val pulseFreq = if (pulseStep == 0) baseFreq else baseFreq * 1.5 // fifths arpeggio
            
            // Simple triangle-like wave for bass (rough synth feel)
            val bassVal = sin(2.0 * Math.PI * pulseFreq * t) * 0.4
            
            // Melodic theme / chord overlay (ambient sine swells or high sparks)
            val leadFreq1 = baseFreq * 2.0 // Tonic
            val leadFreq2 = baseFreq * 3.0 // Dominant / fifth
            val leadFreq3 = baseFreq * 3.75 // Seventh or major third helper
            
            // Pulsing envelope
            val env = 0.12 * (1.0 + sin(2.0 * Math.PI * (1.0 / crotchetSec) * t))
            
            val leadVal = (sin(2.0 * Math.PI * leadFreq1 * t) + 
                           sin(2.0 * Math.PI * leadFreq2 * t) + 
                           sin(2.0 * Math.PI * leadFreq3 * t)) * env
            
            // Synthesize final sample
            val finalSample = (bassVal + leadVal) * 12000
            
            // Clamp and convert to short sample
            buffer[i] = maxOf(-32768, minOf(32767, finalSample.toInt())).toShort()
        }
    }
}

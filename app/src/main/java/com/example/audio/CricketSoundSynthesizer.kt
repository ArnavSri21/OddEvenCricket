package com.example.audio

import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Procedural 44.1 kHz 16-bit PCM WAV synthesizer for realistic cricket match acoustics:
 * leather-on-willow bat strikes, hardwood stump/bail clatter, layered stadium crowd roars
 * and clapping, umpire pea-whistle/boundary/six signals, coin spin, pavilion bell, and fanfares.
 */
internal object CricketSoundSynthesizer {
    private const val SAMPLE_RATE = 44_100
    private const val TWO_PI = 2.0 * PI

    fun generateWavToFile(effect: CricketSoundEffect, outFile: File) {
        val samples = when (effect) {
            CricketSoundEffect.BAT_HIT_SINGLE -> synthBatHitSingle()
            CricketSoundEffect.BAT_HIT_FOUR -> synthBatHitFour()
            CricketSoundEffect.BAT_HIT_SIX -> synthBatHitSix()
            CricketSoundEffect.WICKET_OUT -> synthWicketOut()
            CricketSoundEffect.UMPIRE_SIGNAL -> synthUmpireSignal()
            CricketSoundEffect.TOSS_FLIP -> synthTossFlip()
            CricketSoundEffect.CROWD_APPLAUSE -> synthCrowdApplause()
            CricketSoundEffect.INNINGS_BREAK_HORN -> synthInningsBreakBell()
            CricketSoundEffect.VICTORY_FANFARE -> synthVictoryFanfare()
            CricketSoundEffect.DEFEAT_STING -> synthDefeatSting()
        }
        outFile.writeBytes(encodePcm16Wav(samples))
    }

    /**
     * Crisp leather-on-willow knock for 1-3 runs + light appreciative applause.
     */
    private fun synthBatHitSingle(): DoubleArray {
        val total = (SAMPLE_RATE * 0.52).toInt()
        val buf = DoubleArray(total)
        val rng = Random(101)

        // Willow knock at t=0
        addWillowStrike(buf, startSec = 0.0, intensity = 0.88, pitchScale = 1.06, rng = rng)

        // Subtle appreciative crowd claps starting at 110ms
        addCrowdClaps(buf, startSec = 0.11, durationSec = 0.36, density = 14, gain = 0.22, rng = rng)
        return softLimitNormalize(buf, peak = 0.88)
    }

    /**
     * Booming middle-of-the-bat crack + ball turf zip + rising crowd cheer + umpire 4-signal.
     */
    private fun synthBatHitFour(): DoubleArray {
        val total = (SAMPLE_RATE * 1.45).toInt()
        val buf = DoubleArray(total)
        val rng = Random(202)

        // Powerful sweet-spot willow crack
        addWillowStrike(buf, startSec = 0.0, intensity = 1.12, pitchScale = 0.96, rng = rng)

        // Crisp ball racing across outfield whoosh (50ms - 320ms)
        addFilteredNoiseBurst(
            buf = buf,
            startSec = 0.045,
            durationSec = 0.28,
            centerHz = 950.0,
            bandwidthHz = 620.0,
            attackSec = 0.03,
            releaseSec = 0.20,
            gain = 0.18,
            rng = rng,
        )

        // Swelling stadium crowd cheer + hand claps
        addStadiumCheer(buf, startSec = 0.12, durationSec = 1.25, peakSec = 0.42, gain = 0.62, rng = rng)
        addCrowdClaps(buf, startSec = 0.18, durationSec = 1.05, density = 34, gain = 0.30, rng = rng)

        // Umpire Four Signal: brisk two-sweep chime (A5 -> C#6)
        addUmpireChime(buf, startSec = 0.34, freqHz = 880.0, durationSec = 0.14, gain = 0.24)
        addUmpireChime(buf, startSec = 0.49, freqHz = 1108.7, durationSec = 0.22, gain = 0.28)

        return softLimitNormalize(buf, peak = 0.94)
    }

    /**
     * Thunderous maximum sweet-spot crack + huge stadium roar + stadium horn + umpire 6-signal.
     */
    private fun synthBatHitSix(): DoubleArray {
        val total = (SAMPLE_RATE * 1.95).toInt()
        val buf = DoubleArray(total)
        val rng = Random(303)

        // Deep explosive middle-of-the-bat thwack
        addWillowStrike(buf, startSec = 0.0, intensity = 1.30, pitchScale = 0.86, rng = rng)

        // High-energy ascending stadium roar
        addStadiumCheer(buf, startSec = 0.10, durationSec = 1.78, peakSec = 0.55, gain = 0.82, rng = rng)
        addCrowdClaps(buf, startSec = 0.20, durationSec = 1.45, density = 46, gain = 0.34, rng = rng)

        // Umpire Six Signal (both arms raised): ascending bright triad sting (C6 -> E6 -> G6)
        addUmpireChime(buf, startSec = 0.36, freqHz = 1046.5, durationSec = 0.13, gain = 0.24)
        addUmpireChime(buf, startSec = 0.48, freqHz = 1318.5, durationSec = 0.14, gain = 0.26)
        addUmpireChime(buf, startSec = 0.61, freqHz = 1568.0, durationSec = 0.34, gain = 0.32)

        // Brief warm stadium horn layer beneath the roar
        addBrassNote(buf, startSec = 0.60, freqHz = 392.0, durationSec = 0.45, gain = 0.20)
        addBrassNote(buf, startSec = 0.60, freqHz = 523.25, durationSec = 0.45, gain = 0.18)

        return softLimitNormalize(buf, peak = 0.96)
    }

    /**
     * Authentic wooden stumps & bails clatter + crowd gasp/roar + unmistakable umpire OUT whistle/signal.
     */
    private fun synthWicketOut(): DoubleArray {
        val total = (SAMPLE_RATE * 1.65).toInt()
        val buf = DoubleArray(total)
        val rng = Random(404)

        // Staggered ash-wood stump + bouncing bail impacts
        addTimberClatter(buf, startSec = 0.0, baseHz = 760.0, gain = 1.05, rng = rng)
        addTimberClatter(buf, startSec = 0.042, baseHz = 1140.0, gain = 0.82, rng = rng)
        addTimberClatter(buf, startSec = 0.095, baseHz = 1520.0, gain = 0.64, rng = rng)
        addTimberClatter(buf, startSec = 0.155, baseHz = 1860.0, gain = 0.42, rng = rng)
        addTimberClatter(buf, startSec = 0.225, baseHz = 1690.0, gain = 0.26, rng = rng)

        // Sudden crowd gasp + roar
        addStadiumCheer(buf, startSec = 0.09, durationSec = 1.45, peakSec = 0.36, gain = 0.70, rng = rng)

        // Umpire OUT signal: crisp trilled whistle + decisive raised-finger sting
        addPeaWhistle(buf, startSec = 0.24, freqHz = 2780.0, durationSec = 0.28, gain = 0.26)
        addBrassNote(buf, startSec = 0.54, freqHz = 329.63, durationSec = 0.18, gain = 0.22)
        addBrassNote(buf, startSec = 0.72, freqHz = 261.63, durationSec = 0.36, gain = 0.26)

        return softLimitNormalize(buf, peak = 0.95)
    }

    /**
     * Crisp Umpire match-official pea-whistle ("Play!" / "Over!" / Toss start).
     */
    private fun synthUmpireSignal(): DoubleArray {
        val total = (SAMPLE_RATE * 0.58).toInt()
        val buf = DoubleArray(total)

        addPeaWhistle(buf, startSec = 0.02, freqHz = 2620.0, durationSec = 0.24, gain = 0.45)
        addUmpireChime(buf, startSec = 0.24, freqHz = 1174.7, durationSec = 0.24, gain = 0.22)

        return softLimitNormalize(buf, peak = 0.85)
    }

    /**
     * Realistic metallic coin flick & aerial spin shimmer for the Odd-Even Toss.
     */
    private fun synthTossFlip(): DoubleArray {
        val total = (SAMPLE_RATE * 0.68).toInt()
        val buf = DoubleArray(total)
        val rng = Random(505)

        // Thumb-flick transient
        addFilteredNoiseBurst(
            buf = buf,
            startSec = 0.0,
            durationSec = 0.015,
            centerHz = 3600.0,
            bandwidthHz = 2200.0,
            attackSec = 0.001,
            releaseSec = 0.012,
            gain = 0.42,
            rng = rng,
        )

        // Inharmonic silver coin partials with spinning amplitude flutter (~19 Hz)
        val modes = doubleArrayOf(2890.0, 4340.0, 6210.0, 7950.0)
        val amps = doubleArrayOf(0.55, 0.38, 0.26, 0.14)
        val decays = doubleArrayOf(5.8, 7.2, 9.5, 12.0)

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val flutter = 0.72 + 0.28 * cos(TWO_PI * 19.0 * t)
            var sum = 0.0
            for (m in modes.indices) {
                sum += amps[m] * exp(-decays[m] * t) * sin(TWO_PI * modes[m] * t)
            }
            buf[i] += sum * flutter
        }
        return softLimitNormalize(buf, peak = 0.86)
    }

    /**
     * Warm rhythmic stadium applause swell for Captain Reveal, Draft Picks, and Toss Win.
     */
    private fun synthCrowdApplause(): DoubleArray {
        val total = (SAMPLE_RATE * 1.15).toInt()
        val buf = DoubleArray(total)
        val rng = Random(606)

        addStadiumCheer(buf, startSec = 0.0, durationSec = 1.05, peakSec = 0.30, gain = 0.42, rng = rng)
        addCrowdClaps(buf, startSec = 0.02, durationSec = 1.02, density = 42, gain = 0.48, rng = rng)
        return softLimitNormalize(buf, peak = 0.88)
    }

    /**
     * Classic pavilion bell + stadium buzz signaling Innings Break and Chase Target.
     */
    private fun synthInningsBreakBell(): DoubleArray {
        val total = (SAMPLE_RATE * 1.55).toInt()
        val buf = DoubleArray(total)
        val rng = Random(707)

        // Two resonant brass pavilion bell strikes
        addPavilionBell(buf, startSec = 0.02, fundamentalHz = 660.0, gain = 0.65)
        addPavilionBell(buf, startSec = 0.46, fundamentalHz = 660.0, gain = 0.60)
        addStadiumCheer(buf, startSec = 0.08, durationSec = 1.35, peakSec = 0.50, gain = 0.32, rng = rng)
        return softLimitNormalize(buf, peak = 0.90)
    }

    /**
     * Championship victory stadium roar + triumphant rising brass fanfare.
     */
    private fun synthVictoryFanfare(): DoubleArray {
        val total = (SAMPLE_RATE * 2.25).toInt()
        val buf = DoubleArray(total)
        val rng = Random(808)

        // Triumphant brass fanfare: C5 -> E5 -> G5 -> C6 chord
        addBrassNote(buf, startSec = 0.04, freqHz = 523.25, durationSec = 0.18, gain = 0.34)
        addBrassNote(buf, startSec = 0.20, freqHz = 659.25, durationSec = 0.18, gain = 0.36)
        addBrassNote(buf, startSec = 0.36, freqHz = 783.99, durationSec = 0.22, gain = 0.38)
        // Final triumphant chord
        addBrassNote(buf, startSec = 0.56, freqHz = 523.25, durationSec = 0.92, gain = 0.28)
        addBrassNote(buf, startSec = 0.56, freqHz = 659.25, durationSec = 0.92, gain = 0.26)
        addBrassNote(buf, startSec = 0.56, freqHz = 783.99, durationSec = 0.92, gain = 0.26)
        addBrassNote(buf, startSec = 0.56, freqHz = 1046.50, durationSec = 0.98, gain = 0.32)

        // Huge celebratory stadium roar + applause
        addStadiumCheer(buf, startSec = 0.05, durationSec = 2.15, peakSec = 0.65, gain = 0.72, rng = rng)
        addCrowdClaps(buf, startSec = 0.15, durationSec = 1.95, density = 52, gain = 0.36, rng = rng)

        return softLimitNormalize(buf, peak = 0.95)
    }

    /**
     * Respectful match-loss descending cadence + subdued crowd exhale.
     */
    private fun synthDefeatSting(): DoubleArray {
        val total = (SAMPLE_RATE * 1.45).toInt()
        val buf = DoubleArray(total)
        val rng = Random(909)

        addBrassNote(buf, startSec = 0.04, freqHz = 440.0, durationSec = 0.28, gain = 0.26)
        addBrassNote(buf, startSec = 0.34, freqHz = 392.0, durationSec = 0.28, gain = 0.24)
        addBrassNote(buf, startSec = 0.64, freqHz = 329.63, durationSec = 0.62, gain = 0.28)
        addFilteredNoiseBurst(
            buf = buf,
            startSec = 0.08,
            durationSec = 1.15,
            centerHz = 480.0,
            bandwidthHz = 360.0,
            attackSec = 0.14,
            releaseSec = 0.85,
            gain = 0.26,
            rng = rng,
        )
        return softLimitNormalize(buf, peak = 0.84)
    }

    // --- ACOUSTIC BUILDING BLOCKS ---

    private fun addWillowStrike(
        buf: DoubleArray,
        startSec: Double,
        intensity: Double,
        pitchScale: Double,
        rng: Random,
    ) {
        val startIdx = (startSec * SAMPLE_RATE).toInt()
        val len = (0.14 * SAMPLE_RATE).toInt()
        val endIdx = min(buf.size, startIdx + len)

        // English willow bat modal resonances (hollow wood thump + sharp sweet-spot knock)
        val f1 = 235.0 * pitchScale
        val f2 = 585.0 * pitchScale
        val f3 = 1260.0 * pitchScale
        val f4 = 2480.0 * pitchScale

        for (i in startIdx until endIdx) {
            val t = (i - startIdx).toDouble() / SAMPLE_RATE
            // Crisp leather contact crack in first 8ms
            val crackEnv = exp(-t * 280.0)
            val crackNoise = (rng.nextDouble() * 2.0 - 1.0) * crackEnv * 0.72

            // Damped wood body resonance
            val wood = (
                0.68 * exp(-t * 42.0) * sin(TWO_PI * f1 * t) +
                0.56 * exp(-t * 68.0) * sin(TWO_PI * f2 * t) +
                0.38 * exp(-t * 110.0) * sin(TWO_PI * f3 * t) +
                0.22 * exp(-t * 175.0) * sin(TWO_PI * f4 * t)
            )
            // Fast punch envelope
            val attack = min(1.0, t / 0.0012)
            buf[i] += (crackNoise + wood) * attack * intensity
        }
    }

    private fun addTimberClatter(
        buf: DoubleArray,
        startSec: Double,
        baseHz: Double,
        gain: Double,
        rng: Random,
    ) {
        val startIdx = (startSec * SAMPLE_RATE).toInt()
        val len = (0.12 * SAMPLE_RATE).toInt()
        val endIdx = min(buf.size, startIdx + len)

        val f1 = baseHz
        val f2 = baseHz * 1.68
        val f3 = baseHz * 2.74

        for (i in startIdx until endIdx) {
            val t = (i - startIdx).toDouble() / SAMPLE_RATE
            val snap = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 340.0) * 0.65
            val wood = (
                0.62 * exp(-t * 58.0) * sin(TWO_PI * f1 * t) +
                0.44 * exp(-t * 92.0) * sin(TWO_PI * f2 * t) +
                0.28 * exp(-t * 140.0) * sin(TWO_PI * f3 * t)
            )
            buf[i] += (snap + wood) * gain
        }
    }

    private fun addStadiumCheer(
        buf: DoubleArray,
        startSec: Double,
        durationSec: Double,
        peakSec: Double,
        gain: Double,
        rng: Random,
    ) {
        // Layer 3 formant-shaped noise bands to emulate thousands of cheering voices
        addFilteredNoiseBurst(
            buf = buf,
            startSec = startSec,
            durationSec = durationSec,
            centerHz = 620.0,
            bandwidthHz = 380.0,
            attackSec = peakSec * 0.7,
            releaseSec = (durationSec - peakSec).coerceAtLeast(0.25),
            gain = gain * 0.72,
            rng = rng,
        )
        addFilteredNoiseBurst(
            buf = buf,
            startSec = startSec + 0.03,
            durationSec = durationSec * 0.94,
            centerHz = 1320.0,
            bandwidthHz = 720.0,
            attackSec = peakSec * 0.8,
            releaseSec = (durationSec - peakSec).coerceAtLeast(0.25),
            gain = gain * 0.54,
            rng = rng,
        )
        addFilteredNoiseBurst(
            buf = buf,
            startSec = startSec + 0.05,
            durationSec = durationSec * 0.88,
            centerHz = 2550.0,
            bandwidthHz = 1100.0,
            attackSec = peakSec * 0.9,
            releaseSec = (durationSec - peakSec).coerceAtLeast(0.22),
            gain = gain * 0.28,
            rng = rng,
        )
    }

    private fun addCrowdClaps(
        buf: DoubleArray,
        startSec: Double,
        durationSec: Double,
        density: Int,
        gain: Double,
        rng: Random,
    ) {
        repeat(density) {
            val offset = startSec + rng.nextDouble() * durationSec
            val relPos = (offset - startSec) / durationSec.coerceAtLeast(0.05)
            // Natural swell & fade envelope across the applause window
            val macroEnv = sin(PI * relPos.coerceIn(0.0, 1.0))
            val clapStart = (offset * SAMPLE_RATE).toInt()
            val clapLen = (0.028 * SAMPLE_RATE).toInt()
            val end = min(buf.size, clapStart + clapLen)
            val freq = 1050.0 + rng.nextDouble() * 950.0
            val amp = gain * (0.45 + rng.nextDouble() * 0.55) * macroEnv

            var lp = 0.0
            for (i in clapStart until end) {
                val t = (i - clapStart).toDouble() / SAMPLE_RATE
                val noise = rng.nextDouble() * 2.0 - 1.0
                lp = lp * 0.65 + noise * 0.35
                val env = exp(-t * 145.0)
                val body = 0.65 * lp + 0.35 * sin(TWO_PI * freq * t)
                buf[i] += body * env * amp
            }
        }
    }

    private fun addPeaWhistle(
        buf: DoubleArray,
        startSec: Double,
        freqHz: Double,
        durationSec: Double,
        gain: Double,
    ) {
        val startIdx = (startSec * SAMPLE_RATE).toInt()
        val len = (durationSec * SAMPLE_RATE).toInt()
        val endIdx = min(buf.size, startIdx + len)
        var phase = 0.0

        for (i in startIdx until endIdx) {
            val t = (i - startIdx).toDouble() / SAMPLE_RATE
            val norm = t / durationSec.coerceAtLeast(0.01)
            val attack = min(1.0, t / 0.018)
            val release = min(1.0, (durationSec - t).coerceAtLeast(0.0) / 0.04)
            // Classic umpire pea-whistle 36 Hz warble + amplitude flutter
            val trill = sin(TWO_PI * 36.0 * t)
            val instFreq = freqHz + 105.0 * trill + 60.0 * norm
            phase += TWO_PI * instFreq / SAMPLE_RATE
            val ampMod = 0.78 + 0.22 * trill
            val tone = 0.78 * sin(phase) + 0.22 * sin(phase * 2.0)
            buf[i] += tone * ampMod * attack * release * gain
        }
    }

    private fun addUmpireChime(
        buf: DoubleArray,
        startSec: Double,
        freqHz: Double,
        durationSec: Double,
        gain: Double,
    ) {
        val startIdx = (startSec * SAMPLE_RATE).toInt()
        val len = (durationSec * SAMPLE_RATE).toInt()
        val endIdx = min(buf.size, startIdx + len)
        for (i in startIdx until endIdx) {
            val t = (i - startIdx).toDouble() / SAMPLE_RATE
            val attack = min(1.0, t / 0.008)
            val decay = exp(-t * (4.2 / durationSec.coerceAtLeast(0.08)))
            val sig = 0.72 * sin(TWO_PI * freqHz * t) +
                0.22 * sin(TWO_PI * freqHz * 2.0 * t) +
                0.08 * sin(TWO_PI * freqHz * 3.0 * t)
            buf[i] += sig * attack * decay * gain
        }
    }

    private fun addPavilionBell(
        buf: DoubleArray,
        startSec: Double,
        fundamentalHz: Double,
        gain: Double,
    ) {
        val startIdx = (startSec * SAMPLE_RATE).toInt()
        val len = (0.95 * SAMPLE_RATE).toInt()
        val endIdx = min(buf.size, startIdx + len)
        val ratios = doubleArrayOf(1.0, 2.0, 2.756, 4.07, 5.404)
        val amps = doubleArrayOf(0.58, 0.34, 0.24, 0.14, 0.08)
        val decays = doubleArrayOf(3.2, 4.6, 6.2, 8.8, 12.0)

        for (i in startIdx until endIdx) {
            val t = (i - startIdx).toDouble() / SAMPLE_RATE
            val attack = min(1.0, t / 0.003)
            var sum = 0.0
            for (k in ratios.indices) {
                sum += amps[k] * exp(-decays[k] * t) * sin(TWO_PI * fundamentalHz * ratios[k] * t)
            }
            buf[i] += sum * attack * gain
        }
    }

    private fun addBrassNote(
        buf: DoubleArray,
        startSec: Double,
        freqHz: Double,
        durationSec: Double,
        gain: Double,
    ) {
        val startIdx = (startSec * SAMPLE_RATE).toInt()
        val len = (durationSec * SAMPLE_RATE).toInt()
        val endIdx = min(buf.size, startIdx + len)
        for (i in startIdx until endIdx) {
            val t = (i - startIdx).toDouble() / SAMPLE_RATE
            val attack = min(1.0, t / 0.025)
            val release = min(1.0, (durationSec - t).coerceAtLeast(0.0) / 0.12)
            val env = attack * release
            val bright = 0.55 * sin(TWO_PI * freqHz * t) +
                0.28 * sin(TWO_PI * freqHz * 2.0 * t) +
                0.15 * sin(TWO_PI * freqHz * 3.0 * t) +
                0.08 * sin(TWO_PI * freqHz * 4.0 * t)
            buf[i] += bright * env * gain
        }
    }

    /**
     * 2-pole resonant bandpass filtered white noise generator for realistic crowd formants & whooshes.
     */
    private fun addFilteredNoiseBurst(
        buf: DoubleArray,
        startSec: Double,
        durationSec: Double,
        centerHz: Double,
        bandwidthHz: Double,
        attackSec: Double,
        releaseSec: Double,
        gain: Double,
        rng: Random,
    ) {
        val startIdx = (startSec * SAMPLE_RATE).toInt()
        val len = (durationSec * SAMPLE_RATE).toInt()
        val endIdx = min(buf.size, startIdx + len)

        // Biquad constant-skirt bandpass filter coefficients
        val omega = TWO_PI * centerHz / SAMPLE_RATE
        val q = (centerHz / bandwidthHz.coerceAtLeast(40.0)).coerceIn(0.4, 8.0)
        val alpha = sin(omega) / (2.0 * q)
        val cosW = cos(omega)
        val a0 = 1.0 + alpha
        val b0 = (alpha * q) / a0
        val b2 = -(alpha * q) / a0
        val a1 = (-2.0 * cosW) / a0
        val a2 = (1.0 - alpha) / a0

        var x1 = 0.0
        var x2 = 0.0
        var y1 = 0.0
        var y2 = 0.0

        val safeAtk = attackSec.coerceAtLeast(0.005)
        val safeRel = releaseSec.coerceAtLeast(0.01)

        for (i in startIdx until endIdx) {
            val t = (i - startIdx).toDouble() / SAMPLE_RATE
            val rem = (durationSec - t).coerceAtLeast(0.0)
            val env = min(1.0, t / safeAtk) * min(1.0, rem / safeRel)

            val x0 = rng.nextDouble() * 2.0 - 1.0
            val y0 = b0 * x0 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x0
            y2 = y1
            y1 = y0

            buf[i] += (y0 * 0.25) * env * gain
        }
    }

    private fun softLimitNormalize(samples: DoubleArray, peak: Double): DoubleArray {
        var maxAbs = 0.0001
        for (s in samples) {
            val a = kotlin.math.abs(s)
            if (a > maxAbs) maxAbs = a
        }
        val scale = if (maxAbs > peak) peak / maxAbs else min(1.35, peak / maxAbs)
        val out = DoubleArray(samples.size)
        for (i in samples.indices) {
            out[i] = tanh(samples[i] * scale * 1.08) * peak
        }
        return out
    }

    private fun encodePcm16Wav(samples: DoubleArray): ByteArray {
        val dataSize = samples.size * 2
        val totalFileSize = 36 + dataSize
        val baos = ByteArrayOutputStream(44 + dataSize)

        fun writeAscii(text: String) = baos.write(text.toByteArray(Charsets.US_ASCII))
        fun writeIntLE(v: Int) {
            baos.write(v and 0xFF)
            baos.write((v ushr 8) and 0xFF)
            baos.write((v ushr 16) and 0xFF)
            baos.write((v ushr 24) and 0xFF)
        }
        fun writeShortLE(v: Int) {
            baos.write(v and 0xFF)
            baos.write((v ushr 8) and 0xFF)
        }

        // RIFF WAVE Header
        writeAscii("RIFF")
        writeIntLE(totalFileSize)
        writeAscii("WAVE")

        // fmt chunk
        writeAscii("fmt ")
        writeIntLE(16)            // PCM chunk size
        writeShortLE(1)           // PCM format
        writeShortLE(1)           // Mono
        writeIntLE(SAMPLE_RATE)
        writeIntLE(SAMPLE_RATE * 2)
        writeShortLE(2)           // Block align
        writeShortLE(16)          // Bits per sample

        // data chunk
        writeAscii("data")
        writeIntLE(dataSize)
        for (s in samples) {
            val clamped = max(-1.0, min(1.0, s))
            val pcm = (clamped * 32767.0).toInt().coerceIn(-32768, 32767)
            writeShortLE(pcm)
        }

        return baos.toByteArray()
    }
}

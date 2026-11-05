package dev.taskforge.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Procedural, offline lo-fi drum loop. No recordings, network access, or audio assets. */
class LofiPlayer {
    @Volatile private var running = false
    private var worker: Thread? = null
    private var track: AudioTrack? = null
    fun toggle(): Boolean {
        if (running) { stop(); return false }
        running = true
        worker = Thread({ playLoop() }, "TaskForge-Lofi").apply { start() }
        return true
    }
    fun stop() {
        running = false
        runCatching { track?.pause(); track?.flush(); track?.release() }
        track = null
    }
    private fun playLoop() {
        val rate = 22050
        val samplesPerBeat = rate * 60 / 76
        val beat = ShortArray(samplesPerBeat * 4)
        val rng = Random(31)
        for (i in beat.indices) {
            val pos = i % samplesPerBeat
            val time = pos.toDouble() / rate
            var sample = 0.0
            if (i % (samplesPerBeat * 2) == 0) sample += sin(2 * Math.PI * (48 + 30 * exp(-time * 18)) * time) * exp(-time * 9) * 0.55
            if (i % samplesPerBeat == samplesPerBeat / 2) sample += (rng.nextDouble() * 2 - 1) * exp(-time * 22) * 0.14
            if (i % (samplesPerBeat / 2) == samplesPerBeat / 4) sample += (rng.nextDouble() * 2 - 1) * exp(-time * 95) * 0.07
            // Soft, quiet chord tones for a mellow background bed.
            val chord = intArrayOf(220, 261, 330, 392)[(i / (samplesPerBeat * 2)) % 4]
            sample += sin(2 * Math.PI * chord * i / rate) * 0.025
            beat[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        try {
            val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val audio = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(min, beat.size * 2)).setTransferMode(AudioTrack.MODE_STREAM).build()
            track = audio
            audio.play()
            while (running) audio.write(beat, 0, beat.size)
            audio.release(); track = null
        } catch (_: Exception) { running = false }
    }
}

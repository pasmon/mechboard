package com.example.mechboard

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * The small slice of [SoundPool] that [SoundManager] needs.
 *
 * Exists so that [SoundManager]'s sample-selection logic can be unit-tested on
 * the plain JVM with a fake player; [SoundPool] itself is a native component
 * that cannot be instantiated or reliably mocked off-device.
 */
interface SamplePlayer {
    /** Starts loading raw resource [resId]; returns its sample ID, or 0 if the load failed. */
    fun load(resId: Int): Int

    /** Plays [sampleId] once at [volume] (0–1, both channels) and playback [rate] (1.0 = normal). */
    fun play(sampleId: Int, volume: Float, rate: Float)

    /** Frees all native resources. The player must not be used afterwards. */
    fun release()
}

/** [SamplePlayer] backed by a [SoundPool] configured for key-click playback. */
class SoundPoolSamplePlayer(private val context: Context) : SamplePlayer {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    override fun load(resId: Int): Int = soundPool.load(context, resId, 1)

    override fun play(sampleId: Int, volume: Float, rate: Float) {
        soundPool.play(sampleId, volume, volume, 1, 0, rate)
    }

    override fun release() = soundPool.release()

    private companion object {
        const val MAX_STREAMS = 5
    }
}

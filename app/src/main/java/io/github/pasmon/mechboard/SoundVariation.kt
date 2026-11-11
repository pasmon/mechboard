package io.github.pasmon.mechboard

import kotlin.random.Random

/**
 * Produces the small per-keystroke pitch and loudness differences that keep
 * repeated playback of one sample from sounding like a loop.
 *
 * No two presses of a real switch sound exactly alike; a few percent of
 * variation is enough to hide the repetition without being audible as an effect.
 *
 * Has no Android dependency; [random] is injectable so tests are deterministic.
 */
class SoundVariation(
    private val random: Random = Random.Default,
    /** Maximum playback-rate deviation either side of 1.0 (0.04 = ±4 %). */
    private val pitchSpread: Float = DEFAULT_PITCH_SPREAD,
    /** Maximum fraction by which a keystroke may be quieter than the set volume. */
    private val volumeSpread: Float = DEFAULT_VOLUME_SPREAD
) {
    init {
        require(pitchSpread in 0f..MAX_SPREAD) { "pitchSpread must be within 0..$MAX_SPREAD" }
        require(volumeSpread in 0f..MAX_SPREAD) { "volumeSpread must be within 0..$MAX_SPREAD" }
    }

    /** Returns a playback rate in `[1 - pitchSpread, 1 + pitchSpread]`. */
    fun nextRate(): Float = 1f + (random.nextFloat() * 2f - 1f) * pitchSpread

    /**
     * Returns a volume in `[baseVolume * (1 - volumeSpread), baseVolume]`.
     *
     * Variation only ever attenuates, so the user's chosen volume is a ceiling and
     * a setting of 0 stays silent.
     */
    fun nextVolume(baseVolume: Float): Float =
        baseVolume.coerceIn(0f, 1f) * (1f - random.nextFloat() * volumeSpread)

    companion object {
        const val DEFAULT_PITCH_SPREAD = 0.04f
        const val DEFAULT_VOLUME_SPREAD = 0.10f
        private const val MAX_SPREAD = 0.5f
    }
}

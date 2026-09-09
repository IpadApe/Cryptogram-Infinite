package dev.milan.cryptogram.engine

/**
 * Maps a 1-based level number within a band to a stable quote id (design doc section 4.4).
 *
 * Level N of a band must resolve to the same quote on every device and must never
 * change when the corpus grows. The ids present at first run are frozen and shuffled
 * with a per-band seed; ids added later are appended in ascending id order, so a
 * bigger corpus only extends the sequence.
 */
class LevelIndex(
    quotesByBand: Map<Difficulty, List<Int>>,
    frozenMaxId: Map<Difficulty, Int>,
) {
    private val frozen: Map<Difficulty, List<Int>> = quotesByBand.mapValues { (d, ids) ->
        ids.filter { it <= frozenMaxId.getValue(d) }
            .sortedBy { splitmix64(BAND_SEED.getValue(d) * 31L + it) }
    }

    private val appended: Map<Difficulty, List<Int>> = quotesByBand.mapValues { (d, ids) ->
        ids.filter { it > frozenMaxId.getValue(d) }.sorted()
    }

    fun bandSize(d: Difficulty): Int = frozen.getValue(d).size + appended.getValue(d).size

    /** [level] is 1-based. Cycles back to the start once the band is exhausted. */
    fun quoteIdFor(d: Difficulty, level: Int): Int {
        val f = frozen.getValue(d)
        val a = appended.getValue(d)
        val size = f.size + a.size
        require(size > 0) { "Band $d has no quotes" }
        val idx = (level - 1).mod(size)
        return if (idx < f.size) f[idx] else a[idx - f.size]
    }

    fun cycleFor(d: Difficulty, level: Int): Int = (level - 1) / bandSize(d)

    companion object {
        val BAND_SEED = mapOf(
            Difficulty.EASY to 11L,
            Difficulty.MEDIUM to 22L,
            Difficulty.HARD to 33L,
            Difficulty.EXTREME to 44L,
        )
    }
}

/** Standard SplitMix64 finalizer. */
fun splitmix64(x: Long): Long {
    var z = x + -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
    z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L // 0xBF58476D1CE4E5B9
    z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L // 0x94D049BB133111EB
    return z xor (z ushr 31)
}

package dev.milan.cryptogram.engine

/**
 * Monoalphabetic substitution cipher over A..Z. Deterministic for a given seed
 * (design doc section 4.2). `key[i]` is the cipher letter that plaintext letter
 * `'A' + i` maps to; the key is always a derangement (no letter maps to itself).
 */
object Cipher {

    /** A derangement of A..Z, deterministic for [seed]. */
    fun key(seed: Long): CharArray {
        val rng = kotlin.random.Random(seed)
        val letters = ('A'..'Z').toMutableList()
        while (true) {
            letters.shuffle(rng)
            if (letters.indices.none { letters[it] == 'A' + it }) return letters.toCharArray()
        }
    }

    /** cipher letter -> plaintext letter, i.e. the inverse of [key]. */
    fun invert(key: CharArray): CharArray {
        val inv = CharArray(26)
        for (i in 0 until 26) inv[key[i] - 'A'] = 'A' + i
        return inv
    }

    fun encrypt(plain: String, key: CharArray): String =
        plain.uppercase().map { c -> if (c in 'A'..'Z') key[c - 'A'] else c }.joinToString("")

    fun decrypt(cipher: String, key: CharArray): String {
        val inv = invert(key)
        return cipher.uppercase().map { c -> if (c in 'A'..'Z') inv[c - 'A'] else c }.joinToString("")
    }
}

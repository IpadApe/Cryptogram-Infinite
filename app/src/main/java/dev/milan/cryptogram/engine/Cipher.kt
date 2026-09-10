package dev.milan.cryptogram.engine

/**
 * Number-substitution cipher. Each distinct plaintext letter is replaced by a
 * number 1..26 (design doc section 4.2). Deterministic for a given seed.
 * `key[i]` is the cipher number that plaintext letter `'A' + i` maps to.
 */
object Cipher {

    /** A permutation of 1..26, deterministic for [seed]. */
    fun key(seed: Long): IntArray {
        val rng = kotlin.random.Random(seed)
        return (1..26).toMutableList().also { it.shuffle(rng) }.toIntArray()
    }

    /** cipher number (1..26) -> plaintext letter. Index 0 is unused. */
    fun invert(key: IntArray): CharArray {
        val inv = CharArray(27)
        for (i in 0 until 26) inv[key[i]] = 'A' + i
        return inv
    }

    /** Tokenises plaintext: a number per letter, the literal char for anything else. */
    fun encrypt(plain: String, key: IntArray): List<CipherToken> =
        plain.uppercase().map { c ->
            if (c in 'A'..'Z') CipherToken.Num(key[c - 'A']) else CipherToken.Sym(c)
        }
}

/** One position of a ciphertext: an encoded letter, or a passed-through symbol/space. */
sealed interface CipherToken {
    data class Num(val n: Int) : CipherToken
    data class Sym(val c: Char) : CipherToken
}

package dev.milan.cryptogram.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CipherTest {

    @Test
    fun `key is a permutation of 1 to 26 for 1000 seeds`() {
        for (seed in 0L until 1000L) {
            val key = Cipher.key(seed)
            assertEquals("seed $seed: wrong length", 26, key.size)
            assertEquals("seed $seed: not a permutation of 1..26", (1..26).toSet(), key.toSet())
        }
    }

    @Test
    fun `key is deterministic for a seed`() {
        assertTrue(Cipher.key(42L).contentEquals(Cipher.key(42L)))
    }

    @Test
    fun `invert maps every cipher number back to its plaintext letter`() {
        val key = Cipher.key(7L)
        val inv = Cipher.invert(key)
        for (i in 0 until 26) {
            assertEquals('A' + i, inv[key[i]])
        }
    }

    @Test
    fun `encrypt yields one number per letter and passes punctuation through`() {
        val key = Cipher.key(7L)
        val tokens = Cipher.encrypt("THE QUICK, FOX!", key)
        val letters = tokens.count { it is CipherToken.Num }
        assertEquals(11, letters) // T H E Q U I C K F O X
        assertTrue(tokens.any { it is CipherToken.Sym && it.c == ',' })
        assertTrue(tokens.any { it is CipherToken.Sym && it.c == '!' })
        assertTrue(tokens.any { it is CipherToken.Sym && it.c == ' ' })
        tokens.filterIsInstance<CipherToken.Num>().forEach {
            assertTrue("number ${it.n} out of range", it.n in 1..26)
        }
    }
}

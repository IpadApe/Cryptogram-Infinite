package dev.milan.cryptogram.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CipherTest {

    @Test
    fun `key is a derangement of A to Z for 1000 seeds`() {
        for (seed in 0L until 1000L) {
            val key = Cipher.key(seed)
            assertEquals("seed $seed: wrong length", 26, key.size)
            assertEquals("seed $seed: not a permutation", ('A'..'Z').toSet(), key.toSet())
            for (i in 0 until 26) {
                assertTrue("seed $seed: letter ${'A' + i} maps to itself", key[i] != 'A' + i)
            }
        }
    }

    @Test
    fun `key is deterministic for a seed`() {
        assertTrue(Cipher.key(42L).contentEquals(Cipher.key(42L)))
    }

    @Test
    fun `encrypt then decrypt round-trips and keeps punctuation`() {
        val key = Cipher.key(7L)
        val plain = "THE QUICK, BROWN FOX!"
        val cipher = Cipher.encrypt(plain, key)
        assertEquals(plain, Cipher.decrypt(cipher, key))
        assertEquals("punctuation is preserved", ",!", cipher.filterNot { it in 'A'..'Z' || it == ' ' })
    }
}

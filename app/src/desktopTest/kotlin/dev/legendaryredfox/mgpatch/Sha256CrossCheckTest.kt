package dev.legendaryredfox.mgpatch

import dev.legendaryredfox.mgpatch.core.Sha256
import dev.legendaryredfox.mgpatch.core.toHex
import java.security.MessageDigest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/** The pure-Kotlin SHA-256 must agree with the JDK on every padding boundary and on ROM-sized data. */
class Sha256CrossCheckTest {
    private fun jdk(data: ByteArray) = MessageDigest.getInstance("SHA-256").digest(data).toHex()

    @Test
    fun matchesTheJdkForAllSmallLengths() {
        val random = Random(42)
        for (n in 0..300) {
            val data = random.nextBytes(n)
            assertEquals(jdk(data), Sha256.hex(data), "length $n")
        }
    }

    @Test
    fun matchesTheJdkOnTwoMegabytes() {
        val data = Random(7).nextBytes(0x200000)
        assertEquals(jdk(data), Sha256.hex(data))
    }
}

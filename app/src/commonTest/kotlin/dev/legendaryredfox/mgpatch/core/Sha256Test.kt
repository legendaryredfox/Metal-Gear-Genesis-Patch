package dev.legendaryredfox.mgpatch.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class Sha256Test {
    @Test
    fun emptyInput() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Sha256.hex(ByteArray(0)))
    }

    @Test
    fun abc() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Sha256.hex("abc".encodeToByteArray()))
    }

    @Test
    fun twoBlockMessage() {
        val msg = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray()
        assertEquals("248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1", Sha256.hex(msg))
    }

    @Test
    fun millionA() {
        val data = ByteArray(1_000_000) { 'a'.code.toByte() }
        assertEquals("cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0", Sha256.hex(data))
    }

    @Test
    fun hexRoundTrip() {
        val bytes = byteArrayOf(0, 1, 0x7F, -128, -1)
        assertEquals("00017f80ff", bytes.toHex())
        assertContentEquals(bytes, "00017f80ff".hexToBytes())
    }
}

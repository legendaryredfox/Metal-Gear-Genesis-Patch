package dev.legendaryredfox.mgpatch.core

import dev.legendaryredfox.mgpatch.core.PatchGroup.RAM_CLEAR
import dev.legendaryredfox.mgpatch.core.PatchGroup.ROM_CHECK_BYPASS
import dev.legendaryredfox.mgpatch.core.PatchGroup.SAVE_SYNC
import dev.legendaryredfox.mgpatch.core.PatchGroup.Z80_TIMEOUTS
import dev.legendaryredfox.mgpatch.core.RomProfiles.V11
import dev.legendaryredfox.mgpatch.core.RomProfiles.V12
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Structural invariants of the patch tables; they hold for any ROM, so no dump is needed. */
class RomProfilesTest {
    private fun ByteArray.u32(at: Int) =
        ((this[at].toInt() and 0xFF) shl 24) or ((this[at + 1].toInt() and 0xFF) shl 16) or
            ((this[at + 2].toInt() and 0xFF) shl 8) or (this[at + 3].toInt() and 0xFF)

    @Test
    fun hashesAndNamesAreUnique() {
        assertEquals(RomProfiles.all.size, RomProfiles.all.map { it.sha256 }.toSet().size)
        assertEquals(RomProfiles.all.size, RomProfiles.all.map { it.id }.toSet().size)
        assertEquals(RomProfiles.all.size, RomProfiles.all.map { it.outputName }.toSet().size)
        RomProfiles.all.forEach { assertEquals(64, it.sha256.length) }
    }

    @Test
    fun editsStayInsideTheRomAndDoNotOverlap() {
        for (p in RomProfiles.all) {
            val ranges = p.edits.map { it.address until it.address + it.expected.size }
            ranges.forEach { assertTrue(it.first >= 0x200 && it.last < p.areaStart, "${p.id} edit ${it.first.hex6()} out of range") }
            val sorted = ranges.sortedBy { it.first }
            for (i in 1 until sorted.size) assertTrue(sorted[i - 1].last < sorted[i].first, "${p.id} overlapping edits at ${sorted[i].first.hex6()}")
            p.edits.forEach { assertEquals(it.expected.size, it.replacement.size) }
            p.edits.forEach { assertTrue(it.address % 2 == 0, "${p.id} odd address ${it.address.hex6()}") }
        }
    }

    @Test
    fun blobsFitTheFreeAreaAndDoNotOverlap() {
        for (p in RomProfiles.all) {
            assertTrue(p.areaEnd < p.size && p.areaStart <= p.areaEnd)
            assertTrue(p.areaEnd % 2 == 1, "ROM end is the last byte, so it must be odd")
            val sorted = p.blobs.sortedBy { it.address }
            sorted.forEach {
                assertTrue(it.address >= p.areaStart && it.address + it.bytes.size - 1 <= p.areaEnd, "${p.id} blob at ${it.address.hex6()} outside area")
                assertTrue(it.address % 2 == 0)
            }
            for (i in 1 until sorted.size) assertTrue(sorted[i - 1].address + sorted[i - 1].bytes.size <= sorted[i].address)
        }
    }

    @Test
    fun callsAndJumpsLandInsideTheRoutinesOfTheirOwnGroup() {
        for (p in RomProfiles.all) {
            for (e in p.edits) {
                val op = e.replacement.take(2).toByteArray().toHex()
                if (op != "4eb9" && op != "4ef9") continue
                val target = e.replacement.u32(2)
                val blob = p.blobs.firstOrNull { target >= it.address && target < it.address + it.bytes.size }
                assertTrue(blob != null, "${p.id} edit ${e.address.hex6()} jumps to ${target.hex6()} which is not in any routine")
                assertEquals(e.group, blob.group, "${p.id} edit ${e.address.hex6()} calls a routine of another group")
            }
        }
    }

    @Test
    fun linkAddressesMatchTheProfiles() {
        // patches/build_patches.sh links the Z80 routines at these bases
        assertEquals(V12.blobs.single { it.group == Z80_TIMEOUTS }.address, PatchBlobs.V12_WAIT_BUSACK)
        assertEquals(V11.blobs.single { it.group == Z80_TIMEOUTS }.address, PatchBlobs.V11_WAIT_BUSACK)
    }

    @Test
    fun everyGroupHasWorkToDo() {
        for (p in RomProfiles.all) {
            assertTrue(p.mandatory.all { it in p.groups })
            for (g in p.groups) assertTrue(p.edits.any { it.group == g }, "${p.id}: group $g has no edit")
            assertTrue(p.edits.all { it.group in p.groups } && p.blobs.all { it.group in p.groups })
        }
    }

    @Test
    fun ramClearEndsBelowTheStackOfEachVersion() {
        // movew #n,d7 ; clrl a0@+ ; dbf  -> clears (n+1)*4 bytes from 0xFF0000
        fun clearedEnd(p: RomProfile): Int {
            val e = p.edits.single { it.group == RAM_CLEAR }
            assertEquals("3e3c", e.replacement.take(2).toByteArray().toHex())
            val n = ((e.replacement[2].toInt() and 0xFF) shl 8) or (e.replacement[3].toInt() and 0xFF)
            return 0xFF0000 + (n + 1) * 4
        }
        // v1.2 returns at 0xFFFFFC (stack top 0xFFFFF0); v1.1 starts its stack at 0xFFE000
        assertEquals(0xFFFFF0, clearedEnd(V12))
        assertEquals(0xFFDFF0, clearedEnd(V11))
    }

    @Test
    fun saveSyncAndBypassAreWiredForV11() {
        assertTrue(ROM_CHECK_BYPASS in V11.mandatory)
        assertTrue(V12.groups.none { it == ROM_CHECK_BYPASS })
        assertTrue(V11.edits.any { it.group == SAVE_SYNC })
    }
}

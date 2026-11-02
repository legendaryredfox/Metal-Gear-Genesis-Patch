package dev.legendaryredfox.mgpatch.core

import dev.legendaryredfox.mgpatch.core.Identification.Reason.NOT_MEGA_DRIVE
import dev.legendaryredfox.mgpatch.core.Identification.Reason.SMD_FORMAT
import dev.legendaryredfox.mgpatch.core.Identification.Reason.TOO_SMALL
import dev.legendaryredfox.mgpatch.core.Identification.Reason.UNKNOWN_DUMP
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.BYTES_MISMATCH
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.FREE_AREA_NOT_EMPTY
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.WRONG_SIZE
import dev.legendaryredfox.mgpatch.core.PatchGroup.RAM_CLEAR
import dev.legendaryredfox.mgpatch.core.PatchGroup.ROM_CHECK_BYPASS
import dev.legendaryredfox.mgpatch.core.PatchGroup.SAVE_SYNC
import dev.legendaryredfox.mgpatch.core.PatchGroup.Z80_TIMEOUTS
import dev.legendaryredfox.mgpatch.core.RomProfiles.V11
import dev.legendaryredfox.mgpatch.core.RomProfiles.V12
import dev.legendaryredfox.mgpatch.syntheticRom
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RomPatcherTest {
    private val profiles = listOf(V12, V11)

    private fun ByteArray.u32(at: Int) =
        ((this[at].toInt() and 0xFF) shl 24) or ((this[at + 1].toInt() and 0xFF) shl 16) or
            ((this[at + 2].toInt() and 0xFF) shl 8) or (this[at + 3].toInt() and 0xFF)

    private fun ByteArray.slice(at: Int, len: Int) = copyOfRange(at, at + len)

    // ---- identification

    @Test
    fun identifiesEachVersionBySizeAndOriginalBytes() {
        for (p in profiles) {
            val id = RomPatcher.identify(syntheticRom(p))
            assertIs<Identification.Compatible>(id)
            assertEquals(p, id.profile)
        }
    }

    @Test
    fun rejectsThingsThatAreNotSupportedDumps() {
        assertEquals(TOO_SMALL, assertIs<Identification.Unrecognized>(RomPatcher.identify(ByteArray(10))).reason)
        assertEquals(NOT_MEGA_DRIVE, assertIs<Identification.Unrecognized>(RomPatcher.identify(ByteArray(0x1000))).reason)
        assertEquals(SMD_FORMAT, assertIs<Identification.Unrecognized>(RomPatcher.identify(ByteArray(0x200 + 0x4000 * 3))).reason)
        val sega = ByteArray(0x8000).also { "SEGA".encodeToByteArray().copyInto(it, 0x100) }
        assertEquals(UNKNOWN_DUMP, assertIs<Identification.Unrecognized>(RomPatcher.identify(sega)).reason)
    }

    @Test
    fun aModifiedCodeByteIsNotCompatible() {
        val rom = syntheticRom(V12)
        rom[V12.edits.first().address] = 0x55
        assertIs<Identification.Unrecognized>(RomPatcher.identify(rom))
    }

    @Test
    fun recognizesItsOwnOutput() {
        for (p in profiles) {
            val built = RomPatcher.patch(syntheticRom(p), p, p.groups.toSet()).rom
            val id = RomPatcher.identify(built)
            assertIs<Identification.AlreadyPatched>(id)
            assertEquals(p, id.profile)
        }
    }

    // ---- building

    @Test
    fun fullBuildAppliesEveryEditAndRoutine() {
        for (p in profiles) {
            val source = syntheticRom(p)
            val copy = source.copyOf()
            val result = RomPatcher.patch(source, p, p.groups.toSet())
            assertContentEquals(copy, source, "the input must not be modified")
            assertEquals(p.groups.toSet(), result.groups)
            for (e in p.edits) assertContentEquals(e.replacement, result.rom.slice(e.address, e.expected.size))
            for (b in p.blobs) assertContentEquals(b.bytes, result.rom.slice(b.address, b.bytes.size))
            assertEquals(p.areaEnd, result.rom.u32(0x1A4))
            assertEquals(result.checksum, RomPatcher.headerChecksum(result.rom, p.areaEnd))
            assertEquals(result.checksum, ((result.rom[0x18E].toInt() and 0xFF) shl 8) or (result.rom[0x18F].toInt() and 0xFF))
            assertEquals(Sha256.hex(result.rom), result.sha256)
        }
    }

    @Test
    fun onlyDeclaredBytesChange() {
        for (p in profiles) {
            val source = syntheticRom(p)
            val result = RomPatcher.patch(source, p, p.groups.toSet())
            val declared = p.edits.flatMap { (it.address until it.address + it.expected.size).toList() } +
                p.blobs.flatMap { (it.address until it.address + it.bytes.size).toList() } +
                (0x1A4..0x1A7).toList() + listOf(0x18E, 0x18F)
            val allowed = declared.toSet()
            for (i in source.indices) if (source[i] != result.rom[i]) assertTrue(i in allowed, "unexpected change at ${i.hex6()}")
        }
    }

    @Test
    fun buildIsDeterministic() {
        for (p in profiles) {
            val a = RomPatcher.patch(syntheticRom(p), p, p.groups.toSet())
            val b = RomPatcher.patch(syntheticRom(p), p, p.groups.toSet())
            assertContentEquals(a.rom, b.rom)
        }
    }

    @Test
    fun z80TimeoutsPullInTheRamClear() {
        for (p in profiles) {
            val result = RomPatcher.patch(syntheticRom(p), p, setOf(Z80_TIMEOUTS))
            assertTrue(RAM_CLEAR in result.groups)
            val clear = p.edits.single { it.group == RAM_CLEAR }
            assertContentEquals(clear.replacement, result.rom.slice(clear.address, clear.expected.size))
        }
    }

    @Test
    fun subsetLeavesTheOtherAreasAlone() {
        val source = syntheticRom(V12)
        val result = RomPatcher.patch(source, V12, setOf(SAVE_SYNC))
        assertEquals(setOf(SAVE_SYNC), result.groups)
        val z80 = V12.blobs.single { it.group == Z80_TIMEOUTS }
        assertTrue(result.rom.slice(z80.address, z80.bytes.size).all { it.toInt() == 0 })
        for (e in V12.edits.filter { it.group != SAVE_SYNC }) assertContentEquals(e.expected, result.rom.slice(e.address, e.expected.size))
    }

    @Test
    fun mandatoryGroupIsAlwaysApplied() {
        val result = RomPatcher.patch(syntheticRom(V11), V11, setOf(SAVE_SYNC))
        assertTrue(ROM_CHECK_BYPASS in result.groups)
        for (e in V11.edits.filter { it.group == ROM_CHECK_BYPASS }) {
            assertContentEquals(e.replacement, result.rom.slice(e.address, e.expected.size))
            assertTrue(e.replacement.toList().chunked(2).all { it == NOP.toList() }, "the bypass must be pure NOPs")
        }
    }

    @Test
    fun groupsOfAnotherVersionAreIgnored() {
        val result = RomPatcher.patch(syntheticRom(V12), V12, setOf(SAVE_SYNC, ROM_CHECK_BYPASS))
        assertEquals(setOf(SAVE_SYNC), result.groups)
    }

    @Test
    fun editOnlyBuildKeepsRomEndAndFixesTheChecksum() {
        val source = syntheticRom(V12)
        val result = RomPatcher.patch(source, V12, setOf(RAM_CLEAR))
        assertEquals(source.u32(0x1A4), result.rom.u32(0x1A4))
        assertEquals(result.checksum, RomPatcher.headerChecksum(result.rom, result.rom.u32(0x1A4)))
    }

    // ---- refusals

    @Test
    fun refusesWhenTheOriginalBytesAreNotThere() {
        val rom = syntheticRom(V11)
        val bad = V11.edits.first { it.group == SAVE_SYNC }
        rom[bad.address + 1] = (rom[bad.address + 1] + 1).toByte()
        val e = assertFailsWith<PatchException> { RomPatcher.patch(rom, V11, V11.groups.toSet()) }
        assertEquals(BYTES_MISMATCH, e.kind)
        assertEquals(bad.address.toLong(), e.detail)
    }

    @Test
    fun refusesAWrongSizeAndADirtyFreeArea() {
        assertEquals(WRONG_SIZE, assertFailsWith<PatchException> { RomPatcher.patch(ByteArray(1234), V12, V12.groups.toSet()) }.kind)
        val rom = syntheticRom(V12)
        rom[V12.areaEnd] = 1
        assertEquals(FREE_AREA_NOT_EMPTY, assertFailsWith<PatchException> { RomPatcher.patch(rom, V12, V12.groups.toSet()) }.kind)
    }

    @Test
    fun refusesToPatchAnAlreadyPatchedRom() {
        val once = RomPatcher.patch(syntheticRom(V12), V12, V12.groups.toSet()).rom
        assertFailsWith<PatchException> { RomPatcher.patch(once, V12, V12.groups.toSet()) }
    }

    // ---- header checksum

    @Test
    fun headerChecksumSumsBigEndianWordsFrom0x200() {
        val rom = ByteArray(0x210)
        rom[0x200] = 0x12; rom[0x201] = 0x34
        rom[0x20E] = 0xFF.toByte(); rom[0x20F] = 0xFF.toByte()
        assertEquals(0x1233, RomPatcher.headerChecksum(rom, 0x210))
        assertNotEquals(0x1233, RomPatcher.headerChecksum(rom, 0x20E))
    }
}

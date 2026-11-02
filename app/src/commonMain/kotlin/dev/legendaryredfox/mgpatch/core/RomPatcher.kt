package dev.legendaryredfox.mgpatch.core

import dev.legendaryredfox.mgpatch.core.Identification.AlreadyPatched
import dev.legendaryredfox.mgpatch.core.Identification.Compatible
import dev.legendaryredfox.mgpatch.core.Identification.Known
import dev.legendaryredfox.mgpatch.core.Identification.Reason.NOT_MEGA_DRIVE
import dev.legendaryredfox.mgpatch.core.Identification.Reason.SMD_FORMAT
import dev.legendaryredfox.mgpatch.core.Identification.Reason.TOO_SMALL
import dev.legendaryredfox.mgpatch.core.Identification.Reason.UNKNOWN_DUMP
import dev.legendaryredfox.mgpatch.core.Identification.Unrecognized
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.AREA_TOO_SMALL
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.BAD_CHECKSUM
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.BYTES_MISMATCH
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.FREE_AREA_NOT_EMPTY
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.UNDECLARED_CHANGE
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.WRONG_SIZE

/** Identifies supported dumps and builds the fixed ROM. Pure functions on byte arrays. */
object RomPatcher {
    private const val HEADER_ROM_END = 0x1A4
    private const val HEADER_CHECKSUM = 0x18E
    private const val CHECKSUM_START = 0x200

    /** Genesis header checksum: 16-bit sum of the big-endian words from 0x200 up to ROM end. */
    fun headerChecksum(rom: ByteArray, romEnd: Int): Int {
        var sum = 0
        var i = CHECKSUM_START
        while (i < romEnd && i + 1 < rom.size) {
            sum += ((rom[i].toInt() and 0xFF) shl 8) or (rom[i + 1].toInt() and 0xFF)
            i += 2
        }
        return sum and 0xFFFF
    }

    private fun ByteArray.u32(at: Int): Int =
        ((this[at].toInt() and 0xFF) shl 24) or ((this[at + 1].toInt() and 0xFF) shl 16) or
            ((this[at + 2].toInt() and 0xFF) shl 8) or (this[at + 3].toInt() and 0xFF)

    private fun ByteArray.matches(at: Int, other: ByteArray): Boolean {
        if (at < 0 || at + other.size > size) return false
        for (i in other.indices) if (this[at + i] != other[i]) return false
        return true
    }

    private fun ByteArray.isZero(from: Int, toInclusive: Int): Boolean {
        for (i in from..toInclusive) if (this[i].toInt() != 0) return false
        return true
    }

    /** Decide which supported dump [rom] is (or why it is not one). */
    fun identify(rom: ByteArray): Identification {
        val sha = Sha256.hex(rom)
        if (rom.size < 0x200) return Unrecognized(TOO_SMALL, rom.size, sha)
        RomProfiles.all.firstOrNull { it.sha256 == sha }?.let { return Known(it, rom.size, sha) }
        val segaAt = "SEGA".encodeToByteArray()
        if (!rom.matches(0x100, segaAt)) {
            // .smd copier dumps have a 512-byte header and 16 KB interleaved blocks
            val smd = rom.size > 0x200 && (rom.size - 0x200) % 0x4000 == 0
            val reason = if (smd) SMD_FORMAT else NOT_MEGA_DRIVE
            return Unrecognized(reason, rom.size, sha)
        }
        for (p in RomProfiles.all) {
            if (rom.size != p.size) continue
            if (p.blobs.first().let { rom.matches(it.address, it.bytes) }) {
                return AlreadyPatched(p, rom.size, sha)
            }
            val originalBytes = p.edits.all { rom.matches(it.address, it.expected) }
            val areaFree = rom.isZero(p.areaStart, p.areaEnd)
            if (originalBytes && areaFree) return Compatible(p, rom.size, sha)
        }
        return Unrecognized(UNKNOWN_DUMP, rom.size, sha)
    }

    /**
     * Build the fixed ROM from [source] for [profile] with [groups] (plus the groups the profile
     * always applies and those they require). Verifies the result and throws [PatchException] if
     * anything is off, so a returned ROM is always consistent.
     */
    fun patch(source: ByteArray, profile: RomProfile, groups: Set<PatchGroup>): PatchResult {
        if (source.size != profile.size) throw PatchException(WRONG_SIZE, source.size.toLong())
        val effective = profile.effectiveGroups(groups)
        val rom = source.copyOf()
        val touched = ArrayList<IntRange>()

        val blobs = profile.blobs.filter { it.group in effective }
        for (b in blobs) {
            val end = b.address + b.bytes.size - 1
            if (b.address < profile.areaStart || end > profile.areaEnd) {
                throw PatchException(AREA_TOO_SMALL, b.address.toLong())
            }
        }
        // the routines must not overlap each other either
        for (i in blobs.indices) for (j in i + 1 until blobs.size) {
            val a = blobs[i]; val b = blobs[j]
            if (a.address < b.address + b.bytes.size && b.address < a.address + a.bytes.size) {
                throw PatchException(AREA_TOO_SMALL, b.address.toLong())
            }
        }
        if (blobs.isNotEmpty() && !rom.isZero(profile.areaStart, profile.areaEnd)) {
            throw PatchException(FREE_AREA_NOT_EMPTY, profile.areaStart.toLong())
        }
        for (b in blobs) {
            b.bytes.copyInto(rom, b.address)
            touched += b.address until b.address + b.bytes.size
        }
        for (e in profile.edits) {
            if (e.group !in effective) continue
            if (!source.matches(e.address, e.expected)) {
                throw PatchException(BYTES_MISMATCH, e.address.toLong())
            }
            e.replacement.copyInto(rom, e.address)
            touched += e.address until e.address + e.expected.size
        }

        val romEnd = if (blobs.isNotEmpty()) profile.areaEnd else source.u32(HEADER_ROM_END)
        if (blobs.isNotEmpty()) {
            for (i in 0 until 4) rom[HEADER_ROM_END + i] = (romEnd ushr (24 - 8 * i)).toByte()
            touched += HEADER_ROM_END until HEADER_ROM_END + 4
        }
        val checksum = headerChecksum(rom, romEnd)
        rom[HEADER_CHECKSUM] = (checksum ushr 8).toByte()
        rom[HEADER_CHECKSUM + 1] = checksum.toByte()
        touched += HEADER_CHECKSUM until HEADER_CHECKSUM + 2

        // verification: every changed byte was declared, and the checksum is right
        var changed = 0
        for (i in source.indices) {
            if (source[i] != rom[i]) {
                changed++
                if (touched.none { i in it }) throw PatchException(UNDECLARED_CHANGE, i.toLong())
            }
        }
        val stored = ((rom[HEADER_CHECKSUM].toInt() and 0xFF) shl 8) or (rom[HEADER_CHECKSUM + 1].toInt() and 0xFF)
        if (stored != headerChecksum(rom, rom.u32(HEADER_ROM_END))) throw PatchException(BAD_CHECKSUM)
        return PatchResult(rom, profile, effective, changed, checksum, romEnd, Sha256.hex(rom))
    }
}

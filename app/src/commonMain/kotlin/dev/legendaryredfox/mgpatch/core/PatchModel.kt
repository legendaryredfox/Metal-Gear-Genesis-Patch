package dev.legendaryredfox.mgpatch.core

import dev.legendaryredfox.mgpatch.core.PatchGroup.RAM_CLEAR
import dev.legendaryredfox.mgpatch.core.PatchGroup.Z80_TIMEOUTS

/** A user-visible group of related edits. */
enum class PatchGroup {
    /** Clear all work RAM at boot (the original leaves the top of RAM random on real hardware). */
    RAM_CLEAR,

    /** SAVE stores the live rank, ammo, equipment and weapons instead of the last checkpoint. */
    SAVE_SYNC,

    /** Every wait on the Z80 sound CPU gives up after a bounded time instead of hanging forever. */
    Z80_TIMEOUTS,

    /** v1.1 only: remove the ROM self-checksum that would corrupt the save signature of a patched ROM. */
    ROM_CHECK_BYPASS,
}

/** The m68k NOP opcode. */
internal val NOP = byteArrayOf(0x4E, 0x71)

/**
 * Replace [expected] (the instruction bytes of the original ROM) by [replacement], padded with NOPs.
 * The edit is refused when the ROM does not contain [expected], so it can only hit the intended dump.
 */
class Edit(
    val group: PatchGroup,
    val address: Int,
    val expected: ByteArray,
    replacement: ByteArray,
) {
    val replacement: ByteArray

    init {
        require(replacement.size <= expected.size && (expected.size - replacement.size) % 2 == 0) {
            "replacement for ${address.hex6()} does not fit the original instructions"
        }
        val pad = (expected.size - replacement.size) / 2
        this.replacement = replacement + ByteArray(pad * 2) { NOP[it % 2] }
    }
}

/** Machine code written into the free area at the end of the ROM. */
class Blob(val group: PatchGroup, val address: Int, val bytes: ByteArray)

/** Static description of one supported ROM dump and the edits that fix it. */
class RomProfile(
    val id: String,
    /** Human label of the dump, e.g. "Metal Gear v1.2 (World) (Aftermarket)". */
    val label: String,
    /** File name the unpatched dump usually has (without any folder). */
    val sourceName: String,
    /** Output file name without extension; ".gen" and ".bin" copies are written. */
    val outputName: String,
    val size: Int,
    val sha256: String,
    /** First and last byte of the free area; [areaEnd] also becomes the header "ROM end". */
    val areaStart: Int,
    val areaEnd: Int,
    /** Groups available for this dump, in display order. */
    val groups: List<PatchGroup>,
    /** Groups that are always applied for this dump. */
    val mandatory: Set<PatchGroup>,
    val edits: List<Edit>,
    val blobs: List<Blob>,
)

/** What a ROM file turned out to be. */
sealed interface Identification {
    val size: Int
    val sha256: String

    /** The SHA-256 is exactly the known dump. */
    data class Known(val profile: RomProfile, override val size: Int, override val sha256: String) : Identification

    /** Different hash but the size matches and every original byte the patches check is present. */
    data class Compatible(val profile: RomProfile, override val size: Int, override val sha256: String) : Identification

    /** The free area already holds this tool's routines. */
    data class AlreadyPatched(val profile: RomProfile, override val size: Int, override val sha256: String) : Identification

    data class Unrecognized(val reason: Reason, override val size: Int, override val sha256: String) : Identification

    enum class Reason {
        /** Too small to hold a Mega Drive header. */
        TOO_SMALL,

        /** Interleaved copier format (.smd) rather than a plain dump. */
        SMD_FORMAT,

        /** No "SEGA" header text. */
        NOT_MEGA_DRIVE,

        /** A Mega Drive ROM, but not one of the supported Metal Gear dumps (or modified in an unsupported way). */
        UNKNOWN_DUMP,
    }
}

/** Result of a successful [RomPatcher.patch]. */
class PatchResult(
    val rom: ByteArray,
    val profile: RomProfile,
    val groups: Set<PatchGroup>,
    val changedBytes: Int,
    val checksum: Int,
    val romEnd: Int,
    val sha256: String,
)

/** Why a build was refused; [detail] carries the offending address or size for the message. */
class PatchException(val kind: Kind, val detail: Long = 0) : Exception("$kind ${detail.toString(16)}") {
    enum class Kind {
        WRONG_SIZE,
        BYTES_MISMATCH,
        FREE_AREA_NOT_EMPTY,
        AREA_TOO_SMALL,
        UNDECLARED_CHANGE,
        BAD_CHECKSUM,
    }
}

internal fun Int.hex6(): String = "0x" + toString(16).uppercase().padStart(6, '0')

/**
 * Groups that must be applied together with this one. The Z80 timeouts keep their "Z80 is dead"
 * flag in work RAM and rely on the boot RAM clear to start it at zero.
 */
fun PatchGroup.requires(): Set<PatchGroup> = when (this) {
    Z80_TIMEOUTS -> setOf(RAM_CLEAR)
    else -> emptySet()
}

/** [selected] plus what the profile always applies plus everything those groups require. */
fun RomProfile.effectiveGroups(selected: Set<PatchGroup>): Set<PatchGroup> {
    val out = LinkedHashSet<PatchGroup>()
    val queue = ArrayDeque<PatchGroup>()
    queue.addAll(selected.filter { it in groups })
    queue.addAll(mandatory)
    while (queue.isNotEmpty()) {
        val g = queue.removeFirst()
        if (out.add(g)) queue.addAll(g.requires())
    }
    return out
}

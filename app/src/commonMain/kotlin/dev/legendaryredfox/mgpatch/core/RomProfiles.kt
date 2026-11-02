package dev.legendaryredfox.mgpatch.core

import dev.legendaryredfox.mgpatch.core.PatchGroup.RAM_CLEAR
import dev.legendaryredfox.mgpatch.core.PatchGroup.ROM_CHECK_BYPASS
import dev.legendaryredfox.mgpatch.core.PatchGroup.SAVE_SYNC
import dev.legendaryredfox.mgpatch.core.PatchGroup.Z80_TIMEOUTS

/** The supported dumps and their patches. Addresses were found by reverse engineering (see README). */
object RomProfiles {
    private const val BUSACK_ABS = "303900a11100" + "08000008"          // movew $A11100,d0 ; btst #8,d0
    private const val BUSACK_A2 = "3012" + "08000008"                    // movew a2@,d0 ; btst #8,d0
    private const val BUSACK_A0 = "3010" + "08000008"                    // movew a0@,d0 ; btst #8,d0
    private const val FLAG_LOOP = "4e92" + "4a00" + "660e" + "48780001" + "4e93" + "588f" + "4e92" + "4a00" + "67f2"

    private fun jsr(address: Int) = "4eb9".hexToBytes() + address.be32()
    private fun jmp(address: Int) = "4ef9".hexToBytes() + address.be32()
    private fun Int.be32() = byteArrayOf((this ushr 24).toByte(), (this ushr 16).toByte(), (this ushr 8).toByte(), toByte())

    /** Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen, 2 MB. */
    val V12: RomProfile = run {
        val sync = 0x1BD880
        val z80 = 0x1BD900
        val busack = PatchBlobs.V12_WAIT_BUSACK
        val edits = buildList {
            add(Edit(RAM_CLEAR, 0x49C, "3e3c1f14".hexToBytes(), "3e3c3ffb".hexToBytes()))
            add(Edit(SAVE_SYNC, 0x1476, "41fa02197200".hexToBytes(), jsr(sync)))
            for (a in intArrayOf(0x21DAA, 0x21DDA, 0x21E90, 0x21F10, 0x21FBA, 0x2237E)) {
                add(Edit(Z80_TIMEOUTS, a, (BUSACK_ABS + "66f4").hexToBytes(), jsr(busack)))
            }
            add(Edit(Z80_TIMEOUTS, 0x220EC, (BUSACK_A2 + "66f2").hexToBytes(), jsr(busack)))
            for (a in intArrayOf(0x223FE, 0x2248A, 0x224F2, 0x2255A)) {
                add(Edit(Z80_TIMEOUTS, a, (BUSACK_A0 + "66f2").hexToBytes(), jsr(busack)))
            }
            add(Edit(Z80_TIMEOUTS, 0x22AB6, (BUSACK_A0 + "66f8").hexToBytes(), jsr(busack)))
            add(Edit(Z80_TIMEOUTS, 0xFD14, FLAG_LOOP.hexToBytes(), jmp(PatchBlobs.V12_FLAGWAIT_FD14)))
            add(Edit(Z80_TIMEOUTS, 0x23980, FLAG_LOOP.hexToBytes(), jmp(PatchBlobs.V12_FLAGWAIT_23980)))
        }
        RomProfile(
            id = "v1.2",
            label = "Metal Gear v1.2 (World) (Aftermarket)",
            sourceName = "Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen",
            outputName = "Metal Gear (World) (v1.2) (Aftermarket) (Unl) [fixed]",
            size = 0x200000,
            sha256 = "1707f5534028cd4a3d049a552b1567fc7a3100598b450a976ed0690754be8f6a",
            areaStart = sync,
            areaEnd = 0x1BD9FF,
            groups = listOf(SAVE_SYNC, Z80_TIMEOUTS, RAM_CLEAR),
            mandatory = emptySet(),
            edits = edits,
            blobs = listOf(
                Blob(SAVE_SYNC, sync, PatchBlobs.SYNC_INVENTORY.hexToBytes()),
                Blob(Z80_TIMEOUTS, z80, PatchBlobs.V12_Z80.hexToBytes()),
            ),
        )
    }

    /** Metal Gear (v1.1).md, 1.5 MB (the first public release of the Mega Drive port). */
    val V11: RomProfile = run {
        val sync = 0x13FE80
        val z80 = 0x13FF00
        val busack = PatchBlobs.V11_WAIT_BUSACK
        val flagwait = PatchBlobs.V11_FLAGWAIT
        val edits = buildList {
            // clear 0xFF0000-0xFFDFEF: the stack starts at 0xFFE000 here (v1.2 moved it to the top of RAM)
            add(Edit(RAM_CLEAR, 0x4A0, "3e3c1f14".hexToBytes(), "3e3c37fb".hexToBytes()))
            // ROM self-checksum: jsr $93FC at boot, and lea/addqw/jsr a0@ (-> $798) inside the SRAM write.
            // Any patched byte below 0xF96C would otherwise change the "MGSV" save signature.
            add(Edit(ROM_CHECK_BYPASS, 0x486, "4eb9000093fc".hexToBytes(), ByteArray(0)))
            add(Edit(ROM_CHECK_BYPASS, 0x1630, "41faf16258484e90".hexToBytes(), ByteArray(0)))
            add(Edit(SAVE_SYNC, 0x14AC, "41fa02217200".hexToBytes(), jsr(sync)))
            for (a in intArrayOf(0xF9F8, 0xFA28, 0xFA7C, 0xFAFC)) {
                add(Edit(Z80_TIMEOUTS, a, (BUSACK_ABS + "66f4").hexToBytes(), jsr(busack)))
            }
            add(Edit(Z80_TIMEOUTS, 0xFBA2, (BUSACK_A2 + "66f2").hexToBytes(), jsr(busack)))
            for (a in intArrayOf(0xFD38, 0xFDB6, 0xFE1E)) {
                add(Edit(Z80_TIMEOUTS, a, (BUSACK_A0 + "66f2").hexToBytes(), jsr(busack)))
            }
            for (a in intArrayOf(0xFF18, 0x10548, 0x10E80, 0x10F0C, 0x10FAC)) {
                add(Edit(Z80_TIMEOUTS, a, FLAG_LOOP.hexToBytes(), jsr(flagwait)))
            }
        }
        RomProfile(
            id = "v1.1",
            label = "Metal Gear v1.1 (Mega Drive port)",
            sourceName = "Metal Gear (v1.1).md",
            outputName = "Metal Gear (v1.1) [fixed]",
            size = 0x180000,
            sha256 = "8073e3f1e966d8f6ab5b97f4d7e96208dd32dbea01997c3a4b5e2c5195b06f8b",
            areaStart = sync,
            areaEnd = 0x13FFFF,
            groups = listOf(SAVE_SYNC, Z80_TIMEOUTS, RAM_CLEAR, ROM_CHECK_BYPASS),
            mandatory = setOf(ROM_CHECK_BYPASS),
            edits = edits,
            blobs = listOf(
                Blob(SAVE_SYNC, sync, PatchBlobs.SYNC_INVENTORY.hexToBytes()),
                Blob(Z80_TIMEOUTS, z80, PatchBlobs.V11_Z80.hexToBytes()),
            ),
        )
    }

    val all: List<RomProfile> = listOf(V12, V11)
}

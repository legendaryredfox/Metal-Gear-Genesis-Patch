package dev.legendaryredfox.mgpatch.ui

import dev.legendaryredfox.mgpatch.core.Identification.Reason
import dev.legendaryredfox.mgpatch.core.Identification.Reason.NOT_MEGA_DRIVE
import dev.legendaryredfox.mgpatch.core.Identification.Reason.SMD_FORMAT
import dev.legendaryredfox.mgpatch.core.Identification.Reason.TOO_SMALL
import dev.legendaryredfox.mgpatch.core.Identification.Reason.UNKNOWN_DUMP
import dev.legendaryredfox.mgpatch.core.PatchException.Kind
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.AREA_TOO_SMALL
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.BAD_CHECKSUM
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.BYTES_MISMATCH
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.FREE_AREA_NOT_EMPTY
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.UNDECLARED_CHANGE
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.WRONG_SIZE
import dev.legendaryredfox.mgpatch.core.PatchGroup
import dev.legendaryredfox.mgpatch.core.PatchGroup.RAM_CLEAR
import dev.legendaryredfox.mgpatch.core.PatchGroup.ROM_CHECK_BYPASS
import dev.legendaryredfox.mgpatch.core.PatchGroup.SAVE_SYNC
import dev.legendaryredfox.mgpatch.core.PatchGroup.Z80_TIMEOUTS

object MessagesEn : Messages {
    override val appTitle = "Metal Gear Genesis Patcher"
    override val appSubtitle = "Real-hardware fixes for the Metal Gear Mega Drive port (v1.1 and v1.2)"
    override val language = "Language"
    override val stepRom = "1. Your ROM"
    override val chooseRom = "Choose ROM..."
    override val dropHint = "or drop the file here"
    override val noRomChosen = "No ROM chosen yet. This program contains no ROMs: use your own dump."
    override val fileLabel = "File"
    override val sizeLabel = "Size"
    override val hashLabel = "SHA-256"
    override val usingProfile = { p: String -> "Patch set: $p" }
    override val statusKnown = { p: String -> "Recognized: $p" }
    override val statusCompatible = { p: String -> "Looks like $p, but the file is not the known dump (different hash)." }
    override val statusAlreadyPatched = "This ROM already contains the fixes. Use the original dump instead."
    override val statusUnrecognized = { r: Reason ->
        when (r) {
            TOO_SMALL -> "The file is too small to be a Mega Drive ROM."
            SMD_FORMAT -> "This looks like a .smd (interleaved) dump. Convert it to a plain .bin/.gen/.md first."
            NOT_MEGA_DRIVE -> "This is not a Mega Drive ROM (no SEGA header)."
            UNKNOWN_DUMP -> "Not a supported Metal Gear dump. Supported: v1.1 (.md) and v1.2 (.gen)."
        }
    }
    override val allowCompatible = "Try anyway (every patch still checks the original bytes)"
    override val readError = { d: String -> "Could not read the file: $d" }
    override val stepFixes = "2. Fixes to apply"
    override val fixTitle = { g: PatchGroup ->
        when (g) {
            SAVE_SYNC -> "SAVE keeps your current items"
            Z80_TIMEOUTS -> "Sound CPU timeouts"
            RAM_CLEAR -> "Full RAM clear at boot"
            ROM_CHECK_BYPASS -> "Disable the ROM self-checksum"
        }
    }
    override val fixDescription = { g: PatchGroup, profileId: String ->
        when (g) {
            SAVE_SYNC ->
                "SAVE stores your current rank, ammo, equipment and weapons. Before, only the last checkpoint snapshot was saved, so items picked up since were lost. Existing saves keep loading."
            Z80_TIMEOUTS ->
                "Every wait on the Z80 sound CPU gives up after a short time instead of hanging forever on a black screen. If the Z80 never answers, the game runs without sound."
            RAM_CLEAR ->
                if (profileId == "v1.1") "Clears all work RAM below the stack at boot (the original leaves the sound driver variables random on real hardware)."
                else "Clears all work RAM at boot (the original only cleared the first 31 KB; the sound driver read random values above it)."
            ROM_CHECK_BYPASS ->
                "v1.1 checks a checksum of its own code and silently corrupts the save signature if a single byte changed. This switches that check off (v1.2 removed it too)."
        }
    }
    override val tagRequired = "required"
    override val tagNeededBy = { n: String -> "needed by \"$n\"" }
    override val stepOutput = "3. Output"
    override val outputFolder = "Output folder"
    override val change = "Change..."
    override val willWrite = "Two identical files will be written (use the extension your flashcart menu lists):"
    override val willOverwrite = "A file with this name exists and will be replaced."
    override val apply = "Apply fixes"
    override val successTitle = "Done"
    override val successDetail = { changed: Int, checksum: String -> "$changed bytes changed, header checksum $checksum. The result was verified (every change belongs to a fix, checksum valid)." }
    override val writtenFiles = "Written files"
    override val sha256Output = "SHA-256 of the fixed ROM"
    override val failureTitle = "Nothing was written"
    override val failure = { k: Kind, d: String ->
        when (k) {
            WRONG_SIZE -> "Unexpected ROM size ($d bytes)."
            BYTES_MISMATCH -> "The original code at $d is not what the patch expects. This is not an untouched dump of the supported version."
            FREE_AREA_NOT_EMPTY -> "The free area at $d is not empty, so the ROM seems already modified."
            AREA_TOO_SMALL -> "A patch routine does not fit the free area (at $d)."
            UNDECLARED_CHANGE -> "Internal check failed: byte $d changed outside the patched ranges."
            BAD_CHECKSUM -> "Internal check failed: the header checksum is wrong."
        }
    }
    override val writeFailure = { d: String -> "Could not write the output: $d" }
    override val nothingSelected = "Select at least one fix."
    override val footer = "Unofficial project, not affiliated with Konami or the authors of the port. No ROMs are included or downloaded."
    override val testedNote = "Fixed v1.2 ROM tested on a flashcart; v1.1 checked in the simulator and emulators. Not tested on a real cartridge with battery."
    override val help = "Help"
    override val helpTitle = "Help"
    override val helpClose = "Close"
    override val helpSections = listOf(
        HelpSection(
            "How to use",
            listOf(
                "Get your own dump of Metal Gear for Mega Drive (v1.1 as .md or v1.2 as .gen). No ROM is included or downloaded.",
                "Press \"Choose ROM...\" or drop the file on the window. The program shows the version it recognized and the SHA-256.",
                "Leave the fixes ticked (recommended). Required fixes cannot be unticked, and the RAM clear stays on while the sound CPU timeouts need it.",
                "Pick the output folder (the ROM's folder by default) and press \"Apply fixes\". Two identical files are written, [fixed].gen and [fixed].bin. Your original file is never changed.",
                "Copy the .bin or .gen to your flashcart, or flash it to your repro cartridge. Saves made with the original ROM keep loading.",
                "From a terminal: java -jar MetalGearPatcher-*.jar --cli ROM [-o OUTPUT] [--only save,z80,ram] [--force] [--check]",
            ),
        ),
        HelpSection(
            "Requirements",
            listOf(
                "To run the program: Java 21 or newer (the .jar), or the native installer, which carries its own Java. Nothing else is needed to patch a ROM.",
                "Linux: a desktop session (X11 or Wayland) with the usual AWT libraries (libxtst6, libxrender1, libfreetype6). For the Chinese interface, a CJK font such as Noto Sans CJK.",
                "To build from source: JDK 21+ and the included Gradle wrapper (it downloads Kotlin 2.4.21, Compose Multiplatform 1.12.2, Material 3 1.9.0 and kotlinx-coroutines 1.11.0).",
                "To re-assemble the 68000 routines: binutils-m68k-linux-gnu (m68k-linux-gnu-as, ld, objcopy, nm) and xxd.",
                "For the ROM behaviour tests: gcc, a Musashi checkout and Python 3 (standard library only).",
                "For native installers: jpackage (part of the JDK), plus dpkg and fakeroot for .deb, WiX for .msi, Xcode command line tools for .dmg. Each one can only be built on its own system.",
            ),
        ),
        HelpSection(
            "Files",
            listOf(
                "Input: Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen (2 MB) or Metal Gear (v1.1).md (1.5 MB).",
                "Output: \"... [fixed].gen\" and \"... [fixed].bin\" with the same contents, named after the version.",
                "app/: the Kotlin Multiplatform program (core = patch engine and tables, ui = screen and texts, desktopMain = window and files).",
                "patches/: sync_inventory.s, z80_timeouts.s and z80_timeouts_v11.s (68000 sources), their .bin/.sym files and build_patches.sh.",
                "tests/: run_tests.py and sim/mgsim.c (ROM behaviour tests). Kotlin tests are in app/src/*Test/.",
                ".github/workflows/ci.yml: build and test on Linux, macOS and Windows.",
                "README.md: full documentation in Portuguese, Russian and English.",
            ),
        ),
    )
}

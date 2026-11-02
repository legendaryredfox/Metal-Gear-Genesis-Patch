package dev.legendaryredfox.mgpatch

import dev.legendaryredfox.mgpatch.core.Identification.AlreadyPatched
import dev.legendaryredfox.mgpatch.core.Identification.Compatible
import dev.legendaryredfox.mgpatch.core.Identification.Known
import dev.legendaryredfox.mgpatch.core.Identification.Unrecognized
import dev.legendaryredfox.mgpatch.core.PatchException
import dev.legendaryredfox.mgpatch.core.PatchGroup
import dev.legendaryredfox.mgpatch.core.PatchGroup.RAM_CLEAR
import dev.legendaryredfox.mgpatch.core.PatchGroup.SAVE_SYNC
import dev.legendaryredfox.mgpatch.core.PatchGroup.Z80_TIMEOUTS
import dev.legendaryredfox.mgpatch.core.RomPatcher
import dev.legendaryredfox.mgpatch.core.RomProfile
import dev.legendaryredfox.mgpatch.core.effectiveGroups

/** Headless mode for scripts and CI. Same engine as the GUI. */
object Cli {
    const val USAGE = """Metal Gear Genesis Patcher (command line)
Usage: mgpatcher --cli ROM [-o OUTPUT] [--only save,z80,ram] [--force] [--check]
  ROM        your Metal Gear v1.1 (.md) or v1.2 (.gen) dump
  -o OUTPUT  output file (default: next to the ROM, '<name> [fixed].gen'); a twin with the other extension is written too
  --only     comma list of fixes to apply: save, z80, ram (default: all)
  --force    accept a modified dump (different hash) when every patched byte still matches
  --check    only identify the ROM and print what would be done
Exit status: 0 ok, 1 error, 2 bad usage"""

    private val NAMES = mapOf(
        "save" to SAVE_SYNC,
        "z80" to Z80_TIMEOUTS,
        "ram" to RAM_CLEAR,
    )

    fun run(args: List<String>, io: RomIo, out: (String) -> Unit, err: (String) -> Unit): Int {
        var rom: String? = null
        var output: String? = null
        var only: Set<PatchGroup>? = null
        var force = false
        var check = false
        var i = 0
        while (i < args.size) {
            when (val a = args[i]) {
                "--cli" -> {}
                "-h", "--help" -> { out(USAGE); return 0 }
                "-o", "--output" -> output = args.getOrNull(++i) ?: return usage(err, "missing value for $a")
                "--only" -> {
                    val parts = (args.getOrNull(++i) ?: return usage(err, "missing value for $a")).split(',').map { it.trim() }
                    val groups = parts.map { NAMES[it] ?: return usage(err, "unknown fix '$it' (use save, z80, ram)") }
                    only = groups.toSet()
                }
                "--force" -> force = true
                "--check" -> check = true
                else -> if (a.startsWith("-")) return usage(err, "unknown option $a") else if (rom == null) rom = a else return usage(err, "unexpected argument $a")
            }
            i++
        }
        if (rom == null) return usage(err, "no ROM given")

        val bytes = try { io.read(rom) } catch (e: Exception) { err("error: cannot read $rom: ${e.message}"); return 1 }
        val id = RomPatcher.identify(bytes)
        out("file:   $rom")
        out("size:   ${id.size} bytes")
        out("sha256: ${id.sha256}")
        val profile: RomProfile = when (id) {
            is Known -> { out("rom:    ${id.profile.label} (known dump)"); id.profile }
            is Compatible -> {
                out("rom:    ${id.profile.label} (modified dump, hash differs)")
                if (!force) { err("error: not the known dump; use --force to patch it anyway"); return 1 }
                id.profile
            }
            is AlreadyPatched -> { err("error: this ROM already contains the fixes"); return 1 }
            is Unrecognized -> { err("error: not a supported Metal Gear dump (${id.reason})"); return 1 }
        }
        val selected = only ?: profile.groups.toSet()
        val effective = profile.effectiveGroups(selected)
        out("fixes:  " + effective.joinToString(", ") { it.name })
        if (check) return 0

        val target = output ?: io.join(io.parentOf(rom), profile.outputName + ".gen")
        val twinExt = when {
            target.endsWith(".gen", true) -> ".bin"
            target.endsWith(".bin", true) -> ".gen"
            else -> null
        }
        val targets = listOfNotNull(target, twinExt?.let { target.dropLast(4) + it })
        if (targets.any { it == rom }) { err("error: output would overwrite the source ROM"); return 1 }
        return try {
            val result = RomPatcher.patch(bytes, profile, selected)
            targets.forEach { io.write(it, result.rom); out("wrote   $it") }
            out("${result.changedBytes} bytes changed, header checksum 0x${result.checksum.toString(16).padStart(4, '0')}")
            out("sha256 ${result.sha256}")
            0
        } catch (e: PatchException) {
            err("error: ${e.kind} ${e.detail.toString(16)}"); 1
        } catch (e: Exception) {
            err("error: ${e.message}"); 1
        }
    }

    private fun usage(err: (String) -> Unit, msg: String): Int {
        err("error: $msg"); err(USAGE); return 2
    }
}

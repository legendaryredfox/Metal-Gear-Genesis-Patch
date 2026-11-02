package dev.legendaryredfox.mgpatch.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.legendaryredfox.mgpatch.RomIo
import dev.legendaryredfox.mgpatch.core.Identification
import dev.legendaryredfox.mgpatch.core.Identification.Compatible
import dev.legendaryredfox.mgpatch.core.Identification.Known
import dev.legendaryredfox.mgpatch.core.PatchException
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.WRONG_SIZE
import dev.legendaryredfox.mgpatch.core.PatchGroup
import dev.legendaryredfox.mgpatch.core.PatchResult
import dev.legendaryredfox.mgpatch.core.RomPatcher
import dev.legendaryredfox.mgpatch.core.RomProfile
import dev.legendaryredfox.mgpatch.core.effectiveGroups
import dev.legendaryredfox.mgpatch.core.requires
import dev.legendaryredfox.mgpatch.ui.Outcome.Done
import dev.legendaryredfox.mgpatch.ui.Outcome.Failed

sealed interface Outcome {
    class Done(val files: List<String>, val result: PatchResult) : Outcome
    class Failed(val message: (Messages) -> String) : Outcome
}

/** Everything the screen shows and does, kept free of UI code so tests can drive it. */
class PatcherState(private val io: RomIo, initialLang: Lang = Lang.EN) {
    var lang by mutableStateOf(initialLang)
    var romPath by mutableStateOf<String?>(null)
        private set
    var identification by mutableStateOf<Identification?>(null)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var selected by mutableStateOf<Set<PatchGroup>>(emptySet())
        private set
    var outputDir by mutableStateOf("")
    var allowCompatible by mutableStateOf(false)
    var showHelp by mutableStateOf(false)
    var outcome by mutableStateOf<Outcome?>(null)
        private set

    private var romBytes: ByteArray? = null

    /** The patch set that applies to the loaded ROM, if it is usable. */
    val profile: RomProfile?
        get() = when (val id = identification) {
            is Known -> id.profile
            is Compatible -> if (allowCompatible) id.profile else null
            else -> null
        }

    /** Groups that will really be applied (selection + required + dependencies). */
    val effective: Set<PatchGroup>
        get() = profile?.effectiveGroups(selected) ?: emptySet()

    val userChoices: Set<PatchGroup>
        get() = effective - (profile?.mandatory ?: emptySet())

    val canApply: Boolean get() = profile != null && userChoices.isNotEmpty()

    /** Output file names for the current ROM, [.gen, .bin]. */
    val outputNames: List<String>
        get() = profile?.let { listOf(it.outputName + ".gen", it.outputName + ".bin") } ?: emptyList()

    fun outputPaths(): List<String> = outputNames.map { io.join(outputDir, it) }

    fun existingOutputs(): List<String> = outputPaths().filter { io.exists(it) }

    fun loadRom(path: String) {
        outcome = null
        loadError = null
        allowCompatible = false
        try {
            val bytes = io.read(path)
            romBytes = bytes
            romPath = path
            identification = RomPatcher.identify(bytes)
            outputDir = io.parentOf(path)
            selected = identification.defaultSelection()
        } catch (e: Exception) {
            romBytes = null
            romPath = path
            identification = null
            loadError = e.message ?: e::class.simpleName ?: "error"
        }
    }

    private fun Identification?.defaultSelection(): Set<PatchGroup> = when (this) {
        is Known -> profile.groups.toSet()
        is Compatible -> profile.groups.toSet()
        else -> emptySet()
    }

    fun setGroup(group: PatchGroup, on: Boolean) {
        val p = profile ?: return
        if (group in p.mandatory) return
        selected = if (on) {
            selected + group + group.requires()
        } else {
            // switching a group off also switches off what depends on it
            val dependents = p.groups.filter { group in it.requires() }
            selected - group - dependents.toSet()
        }
    }

    /** True when [group] cannot be switched off right now because another selected group needs it. */
    fun isLocked(group: PatchGroup): Boolean {
        val p = profile ?: return false
        return group in p.mandatory || p.groups.any { it in effective && group in it.requires() }
    }

    fun lockedBy(group: PatchGroup): PatchGroup? =
        profile?.groups?.firstOrNull { it in effective && group in it.requires() }

    fun apply() {
        val p = profile ?: return
        val bytes = romBytes ?: return
        if (!canApply) return
        outcome = try {
            val result = RomPatcher.patch(bytes, p, selected)
            val files = outputPaths()
            if (files.any { it == romPath }) throw IllegalStateException("output would overwrite the source ROM")
            files.forEach { io.write(it, result.rom) }
            Done(files, result)
        } catch (e: PatchException) {
            val detail = if (e.kind == WRONG_SIZE) e.detail.toString() else "0x" + e.detail.toString(16).uppercase()
            Failed { it.failure(e.kind, detail) }
        } catch (e: Exception) {
            val text = e.message ?: e::class.simpleName ?: "error"
            Failed { it.writeFailure(text) }
        }
    }
}

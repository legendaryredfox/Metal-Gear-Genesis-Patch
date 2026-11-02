package dev.legendaryredfox.mgpatch.ui

import dev.legendaryredfox.mgpatch.FakeRomIo
import dev.legendaryredfox.mgpatch.core.Identification
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PatcherStateTest {
    private val path = "/roms/mine.gen"

    private fun stateWith(rom: ByteArray, io: FakeRomIo = FakeRomIo(mapOf(path to rom))) = PatcherState(io, Lang.EN) to io

    /** The synthetic ROM has a different hash, so tests go through the "compatible" path. */
    private fun PatcherState.loadAndAllow(p: String = path) {
        loadRom(p)
        allowCompatible = true
    }

    @Test
    fun startsEmpty() {
        val (state, _) = stateWith(ByteArray(0))
        assertNull(state.romPath)
        assertNull(state.profile)
        assertFalse(state.canApply)
    }

    @Test
    fun modifiedDumpNeedsConfirmation() {
        val (state, _) = stateWith(syntheticRom(V12))
        state.loadRom(path)
        assertIs<Identification.Compatible>(state.identification)
        assertNull(state.profile)
        assertFalse(state.canApply)
        state.allowCompatible = true
        assertEquals(V12, state.profile)
        assertTrue(state.canApply)
        assertEquals("/roms", state.outputDir)
    }

    @Test
    fun everyFixStartsSelected() {
        val (state, _) = stateWith(syntheticRom(V11))
        state.loadAndAllow()
        assertEquals(V11.groups.toSet(), state.effective)
    }

    @Test
    fun switchingTheRamClearOffAlsoSwitchesOffTheZ80Timeouts() {
        val (state, _) = stateWith(syntheticRom(V12))
        state.loadAndAllow()
        assertTrue(state.isLocked(RAM_CLEAR))
        assertEquals(Z80_TIMEOUTS, state.lockedBy(RAM_CLEAR))
        state.setGroup(Z80_TIMEOUTS, false)
        assertFalse(state.isLocked(RAM_CLEAR))
        state.setGroup(RAM_CLEAR, false)
        assertEquals(setOf(SAVE_SYNC), state.effective)
        state.setGroup(Z80_TIMEOUTS, true)
        assertTrue(RAM_CLEAR in state.effective)
    }

    @Test
    fun theMandatoryFixCannotBeSwitchedOff() {
        val (state, _) = stateWith(syntheticRom(V11))
        state.loadAndAllow()
        state.setGroup(ROM_CHECK_BYPASS, false)
        assertTrue(ROM_CHECK_BYPASS in state.effective)
        assertTrue(state.isLocked(ROM_CHECK_BYPASS))
    }

    @Test
    fun cannotApplyWithOnlyTheMandatoryFix() {
        val (state, _) = stateWith(syntheticRom(V11))
        state.loadAndAllow()
        listOf(Z80_TIMEOUTS, SAVE_SYNC, RAM_CLEAR).forEach { state.setGroup(it, false) }
        assertFalse(state.canApply)
        state.apply()
        assertNull(state.outcome)
    }

    @Test
    fun applyWritesBothExtensionsWithTheSameBytes() {
        val (state, io) = stateWith(syntheticRom(V12))
        state.loadAndAllow()
        state.apply()
        val done = assertIs<Outcome.Done>(state.outcome)
        assertEquals(listOf("/roms/${V12.outputName}.gen", "/roms/${V12.outputName}.bin"), done.files)
        done.files.forEach { assertContentEquals(done.result.rom, io.files[it]) }
    }

    @Test
    fun applyHonoursTheChosenFolder() {
        val (state, io) = stateWith(syntheticRom(V11))
        state.loadAndAllow()
        state.outputDir = "/out"
        state.apply()
        assertTrue(io.exists("/out/${V11.outputName}.bin"))
        assertFalse(io.exists("/roms/${V11.outputName}.bin"))
    }

    @Test
    fun reportsExistingOutputs() {
        val (state, io) = stateWith(syntheticRom(V12))
        state.loadAndAllow()
        assertTrue(state.existingOutputs().isEmpty())
        io.files["/roms/${V12.outputName}.gen"] = ByteArray(1)
        assertEquals(1, state.existingOutputs().size)
    }

    @Test
    fun readFailureIsReportedNotThrown() {
        val (state, _) = stateWith(ByteArray(0))
        state.loadRom("/missing.gen")
        assertNotNull(state.loadError)
        assertNull(state.identification)
        assertFalse(state.canApply)
    }

    @Test
    fun writeFailureLeavesAnErrorOutcome() {
        val (state, io) = stateWith(syntheticRom(V12))
        state.loadAndAllow()
        io.failWrites = true
        state.apply()
        val failed = assertIs<Outcome.Failed>(state.outcome)
        assertTrue("disk full" in failed.message(MessagesEn))
    }

    @Test
    fun neverOverwritesTheSourceRom() {
        val source = "/roms/${V12.outputName}.gen"
        val io = FakeRomIo(mapOf(source to syntheticRom(V12)))
        val state = PatcherState(io, Lang.EN)
        state.loadAndAllow(source)
        state.apply()
        assertIs<Outcome.Failed>(state.outcome)
        assertTrue(io.writes.isEmpty())
    }

    @Test
    fun failureTextsFollowTheLanguage() {
        val (state, io) = stateWith(syntheticRom(V12))
        state.loadAndAllow()
        io.failWrites = true
        state.apply()
        val failed = assertIs<Outcome.Failed>(state.outcome)
        assertTrue("Could not write" in failed.message(MessagesEn))
        assertTrue("Não foi possível" in failed.message(MessagesPt))
    }

    @Test
    fun loadingAnotherRomClearsTheOldResult() {
        val (state, _) = stateWith(syntheticRom(V12))
        state.loadAndAllow()
        state.apply()
        assertNotNull(state.outcome)
        state.loadRom(path)
        assertNull(state.outcome)
    }
}

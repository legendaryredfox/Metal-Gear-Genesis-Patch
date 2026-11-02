package dev.legendaryredfox.mgpatch

import dev.legendaryredfox.mgpatch.core.RomProfiles.V11
import dev.legendaryredfox.mgpatch.core.RomProfiles.V12
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CliTest {
    private class Run(val code: Int, val out: List<String>, val err: List<String>)

    private fun run(io: RomIo, vararg args: String): Run {
        val out = mutableListOf<String>()
        val err = mutableListOf<String>()
        val code = Cli.run(args.toList(), io, { out += it }, { err += it })
        return Run(code, out, err)
    }

    @Test
    fun helpAndUsageErrors() {
        val io = FakeRomIo()
        assertEquals(0, run(io, "--help").code)
        assertEquals(2, run(io).code)
        assertEquals(2, run(io, "--bogus").code)
        assertEquals(2, run(io, "a", "b").code)
        assertEquals(2, run(io, "rom.gen", "--only", "nope").code)
        assertEquals(2, run(io, "rom.gen", "-o").code)
    }

    @Test
    fun missingFile() {
        val r = run(FakeRomIo(), "--cli", "/nope.gen")
        assertEquals(1, r.code)
        assertTrue(r.err.any { "cannot read" in it })
    }

    @Test
    fun aModifiedDumpNeedsForce() {
        val io = FakeRomIo(mapOf("/r/mg.gen" to syntheticRom(V12)))
        val refused = run(io, "/r/mg.gen")
        assertEquals(1, refused.code)
        assertTrue(io.writes.isEmpty())
        val ok = run(io, "/r/mg.gen", "--force")
        assertEquals(0, ok.code)
        assertEquals(listOf("/r/${V12.outputName}.gen", "/r/${V12.outputName}.bin"), io.writes)
        assertContentEquals(io.files[io.writes[0]], io.files[io.writes[1]])
    }

    @Test
    fun checkOnlyIdentifies() {
        val io = FakeRomIo(mapOf("/r/mg.md" to syntheticRom(V11)))
        val r = run(io, "/r/mg.md", "--check", "--force")
        assertEquals(0, r.code)
        assertTrue(io.writes.isEmpty())
        assertTrue(r.out.any { "ROM_CHECK_BYPASS" in it })
    }

    @Test
    fun onlySelectsFixesAndKeepsDependencies() {
        val io = FakeRomIo(mapOf("/r/mg.gen" to syntheticRom(V12)))
        val r = run(io, "/r/mg.gen", "--force", "--only", "z80", "-o", "/o/out.bin")
        assertEquals(0, r.code)
        assertEquals(listOf("/o/out.bin", "/o/out.gen"), io.writes)
        assertTrue(r.out.any { it.startsWith("fixes:") && "RAM_CLEAR" in it && "Z80_TIMEOUTS" in it && "SAVE_SYNC" !in it })
    }

    @Test
    fun refusesToOverwriteTheSource() {
        val io = FakeRomIo(mapOf("/r/mg.gen" to syntheticRom(V12)))
        val r = run(io, "/r/mg.gen", "--force", "-o", "/r/mg.gen")
        assertEquals(1, r.code)
        assertFalse(io.writes.any { it == "/r/mg.gen" })
    }

    @Test
    fun garbageAndAlreadyPatchedRomsAreRejected() {
        val io = FakeRomIo(mapOf("/r/junk.bin" to ByteArray(100), "/r/mg.gen" to syntheticRom(V12)))
        assertEquals(1, run(io, "/r/junk.bin").code)
        assertEquals(0, run(io, "/r/mg.gen", "--force").code)
        val again = run(io, "/r/${V12.outputName}.gen")
        assertEquals(1, again.code)
        assertTrue(again.err.any { "already" in it })
    }
}

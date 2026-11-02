package dev.legendaryredfox.mgpatch

import dev.legendaryredfox.mgpatch.core.Identification
import dev.legendaryredfox.mgpatch.core.PatchGroup.RAM_CLEAR
import dev.legendaryredfox.mgpatch.core.PatchGroup.SAVE_SYNC
import dev.legendaryredfox.mgpatch.core.RomPatcher
import dev.legendaryredfox.mgpatch.core.RomProfiles.V11
import dev.legendaryredfox.mgpatch.core.RomProfiles.V12
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests against the real dumps. ROMs are never committed: set MG_ROM_DIR to a folder holding
 * "Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen" and/or "Metal Gear (v1.1).md".
 * Without them these tests are skipped.
 */
class RealRomTest {
    private fun rom(name: String): ByteArray {
        val dir = System.getenv("MG_ROM_DIR").orEmpty()
        val file = File(dir, name)
        assumeTrue("$name not found in MG_ROM_DIR", dir.isNotEmpty() && file.isFile)
        return file.readBytes()
    }

    @Test
    fun v12IsRecognizedAndMatchesTheFlashcartTestedOutput() {
        val source = rom(V12.sourceName)
        assertIs<Identification.Known>(RomPatcher.identify(source))
        val result = RomPatcher.patch(source, V12, V12.groups.toSet())
        // the same bytes the Python script produced and the owner ran on a flashcart
        assertEquals("0c52b1e269bcd97b92102739336791385a06dfdcea0371d205390821197dc646", result.sha256)
        assertEquals(336, result.changedBytes)
    }

    @Test
    fun v11IsRecognizedAndBuildsTheVerifiedOutput() {
        val source = rom(V11.sourceName)
        assertIs<Identification.Known>(RomPatcher.identify(source))
        val result = RomPatcher.patch(source, V11, V11.groups.toSet())
        assertEquals(V11_FIXED_SHA256, result.sha256)
        assertIs<Identification.AlreadyPatched>(RomPatcher.identify(result.rom))
    }

    @Test
    fun cliWritesIdenticalGenAndBinForEveryVersion() {
        for (p in listOf(V12, V11)) {
            val source = rom(p.sourceName)
            val dir = createTempDirectory()
            val input = File(dir, p.sourceName).also { it.writeBytes(source) }
            val out = mutableListOf<String>()
            val code = Cli.run(listOf(input.path), DesktopRomIo, { out += it }, { out += it })
            assertEquals(0, code, out.joinToString("\n"))
            val gen = File(dir, p.outputName + ".gen").readBytes()
            val bin = File(dir, p.outputName + ".bin").readBytes()
            assertContentEquals(gen, bin)
            assertContentEquals(RomPatcher.patch(source, p, p.groups.toSet()).rom, gen)
            dir.deleteRecursively()
        }
    }

    @Test
    fun v11SubsetsAreStillVerified() {
        val source = rom(V11.sourceName)
        for (groups in listOf(setOf(SAVE_SYNC), setOf(RAM_CLEAR), V11.groups.toSet())) {
            val r = RomPatcher.patch(source, V11, groups)
            assertEquals(source.size, r.rom.size)
        }
    }

    private fun createTempDirectory(): File = kotlin.io.path.createTempDirectory("mgpatch").toFile()

    private companion object {
        /** Verified in the Musashi simulator (tests/run_tests.py, 11/11) and in BlastEm (boots to gameplay). */
        const val V11_FIXED_SHA256 = "be4ea50dc72d10e18b70b6020a9ea8b086ac8f21fef9c105c14de1a7a03707e5"
    }
}

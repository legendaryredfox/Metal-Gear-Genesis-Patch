package dev.legendaryredfox.mgpatch

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.legendaryredfox.mgpatch.core.PatchGroup.RAM_CLEAR
import dev.legendaryredfox.mgpatch.core.PatchGroup.Z80_TIMEOUTS
import dev.legendaryredfox.mgpatch.core.RomProfiles.V11
import dev.legendaryredfox.mgpatch.ui.App
import dev.legendaryredfox.mgpatch.ui.Lang
import dev.legendaryredfox.mgpatch.ui.PatcherState
import dev.legendaryredfox.mgpatch.ui.Platform
import kotlin.test.Test
import kotlin.test.assertTrue

/** Drives the real screen headlessly: choose a ROM, review the fixes, apply, switch language. */
@OptIn(ExperimentalTestApi::class)
class AppUiTest {
    private class FakePlatform(private val rom: String?) : Platform {
        override fun chooseRom() = rom
        override fun chooseFolder(initial: String?): String? = null
        override fun dropTarget(onFile: (String) -> Unit): Modifier = Modifier
    }

    @Test
    fun chooseReviewApplyAndSwitchLanguage() = runComposeUiTest {
        val io = FakeRomIo(mapOf("/roms/mg.md" to syntheticRom(V11)))
        val state = PatcherState(io, Lang.EN)
        setContent { App(state, FakePlatform("/roms/mg.md"), dark = false) }

        onNodeWithTag("chooseRom").assertIsEnabled()
        onNodeWithText("No ROM chosen yet.", substring = true).assertExists()

        onNodeWithTag("chooseRom").performClick()
        onNodeWithTag("romStatus").assertTextContains("Looks like", substring = true)
        onNodeWithTag("apply").assertIsNotEnabled()

        onNodeWithTag("allowCompatible").performClick()
        onNodeWithTag("fix-${RAM_CLEAR.name}").performScrollTo().assertIsOn()
        onNodeWithTag("fix-${Z80_TIMEOUTS.name}").assertIsOn()
        onNodeWithTag("apply").performScrollTo().assertIsEnabled().performClick()

        onNodeWithTag("success").performScrollTo().assertTextContains("bytes changed", substring = true)
        assertTrue(io.exists("/roms/${V11.outputName}.gen") && io.exists("/roms/${V11.outputName}.bin"))

        onNodeWithTag("language").performScrollTo().performClick()
        onNode(hasText("Português")).performClick()
        onNodeWithText("Aplicar correções").assertExists()
    }

    @Test
    fun helpOpensInEveryLanguageAndCloses() = runComposeUiTest {
        val state = PatcherState(FakeRomIo(), Lang.EN)
        setContent { App(state, FakePlatform(null), dark = false) }
        onNodeWithTag("helpCard").assertDoesNotExist()
        for (lang in Lang.entries) {
            state.lang = lang
            waitForIdle()
            onNodeWithTag("help").performClick()
            onNodeWithTag("helpCard").assertExists()
            onNodeWithText(lang.messages.helpSections[1].title).assertExists()
            onNodeWithTag("helpClose").performScrollTo().performClick()
            onNodeWithTag("helpCard").assertDoesNotExist()
        }
    }

    @Test
    fun anUnsupportedFileShowsAnErrorAndNoFixes() = runComposeUiTest {
        val io = FakeRomIo(mapOf("/roms/junk.bin" to ByteArray(0x1000)))
        val state = PatcherState(io, Lang.EN)
        setContent { App(state, FakePlatform("/roms/junk.bin"), dark = true) }
        onNodeWithTag("chooseRom").performClick()
        onNodeWithTag("romStatus").assertTextContains("not a Mega Drive ROM", substring = true, ignoreCase = true)
        onNodeWithTag("apply").assertIsNotEnabled()
        onNodeWithText("2. Fixes to apply").assertDoesNotExist()
    }
}

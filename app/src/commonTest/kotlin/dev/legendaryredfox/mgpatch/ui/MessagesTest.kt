package dev.legendaryredfox.mgpatch.ui

import dev.legendaryredfox.mgpatch.core.Identification.Reason
import dev.legendaryredfox.mgpatch.core.PatchException.Kind
import dev.legendaryredfox.mgpatch.core.PatchGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessagesTest {
    private fun allTexts(m: Messages): List<String> = buildList {
        add(m.appTitle); add(m.appSubtitle); add(m.language); add(m.stepRom); add(m.chooseRom); add(m.dropHint)
        add(m.noRomChosen); add(m.fileLabel); add(m.sizeLabel); add(m.hashLabel); add(m.usingProfile("x"))
        add(m.statusKnown("x")); add(m.statusCompatible("x")); add(m.statusAlreadyPatched)
        Reason.entries.forEach { add(m.statusUnrecognized(it)) }
        add(m.allowCompatible); add(m.readError("x")); add(m.stepFixes)
        PatchGroup.entries.forEach { g ->
            add(m.fixTitle(g)); add(m.fixDescription(g, "v1.1")); add(m.fixDescription(g, "v1.2"))
        }
        add(m.tagRequired); add(m.tagNeededBy("x")); add(m.stepOutput); add(m.outputFolder); add(m.change)
        add(m.willWrite); add(m.willOverwrite); add(m.apply); add(m.successTitle); add(m.successDetail(1, "0x0001"))
        add(m.writtenFiles); add(m.sha256Output); add(m.failureTitle)
        Kind.entries.forEach { add(m.failure(it, "0x10")) }
        add(m.writeFailure("x")); add(m.nothingSelected); add(m.footer); add(m.testedNote)
        add(m.help); add(m.helpTitle); add(m.helpClose)
        m.helpSections.forEach { sec -> add(sec.title); addAll(sec.items) }
    }

    @Test
    fun everyLanguageHasEveryText() {
        for (lang in Lang.entries) {
            val texts = allTexts(lang.messages)
            assertTrue(texts.all { it.isNotBlank() }, "${lang.code} has a blank text")
        }
    }

    @Test
    fun noEmDashesAnywhere() {
        for (lang in Lang.entries) assertFalse(allTexts(lang.messages).any { '\u2014' in it }, "${lang.code} uses an em dash")
    }

    @Test
    fun translationsDifferFromEnglish() {
        val en = allTexts(MessagesEn)
        for (lang in listOf(Lang.PT, Lang.RU, Lang.ZH)) {
            val other = allTexts(lang.messages)
            assertEquals(en.size, other.size)
            assertTrue(other.zip(en).count { (a, b) -> a != b } > en.size / 2, "${lang.code} looks untranslated")
        }
    }

    @Test
    fun helpHasTheSameStructureInEveryLanguage() {
        val reference = MessagesEn.helpSections.map { it.items.size }
        assertEquals(3, reference.size)
        for (lang in Lang.entries) {
            assertEquals(reference, lang.messages.helpSections.map { it.items.size }, "${lang.code} help differs in shape")
        }
    }

    @Test
    fun helpNamesTheRequiredToolsInEveryLanguage() {
        for (lang in Lang.entries) {
            val text = lang.messages.helpSections.flatMap { it.items }.joinToString("\n")
            for (needle in listOf("Java 21", "JDK 21", "binutils-m68k-linux-gnu", "gcc", "Musashi", "Gradle", "jpackage", "build_patches.sh", "run_tests.py", "[fixed].bin")) {
                assertTrue(needle in text, "${lang.code} help does not mention $needle")
            }
        }
    }

    @Test
    fun languageIsPickedFromTheLocale() {
        assertEquals(Lang.PT, Lang.fromLocale("pt"))
        assertEquals(Lang.PT, Lang.fromLocale("pt-BR"))
        assertEquals(Lang.RU, Lang.fromLocale("ru_RU"))
        assertEquals(Lang.ZH, Lang.fromLocale("zh-CN"))
        assertEquals(Lang.ZH, Lang.fromLocale("zh_TW"))
        assertEquals(Lang.EN, Lang.fromLocale("fr"))
        assertEquals(Lang.EN, Lang.fromLocale(null))
    }
}

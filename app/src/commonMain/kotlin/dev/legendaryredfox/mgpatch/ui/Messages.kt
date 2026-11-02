package dev.legendaryredfox.mgpatch.ui

import dev.legendaryredfox.mgpatch.core.Identification
import dev.legendaryredfox.mgpatch.core.PatchException
import dev.legendaryredfox.mgpatch.core.PatchGroup

enum class Lang(val code: String, val nativeName: String, val messages: Messages) {
    PT("pt", "Português", MessagesPt),
    EN("en", "English", MessagesEn),
    RU("ru", "Русский", MessagesRu),
    ZH("zh", "中文", MessagesZh);

    companion object {
        /** Pick the UI language from a locale language code such as "pt", "ru" or "en-US". */
        fun fromLocale(code: String?): Lang {
            val base = code?.lowercase()?.substringBefore('-')?.substringBefore('_')
            return entries.firstOrNull { it.code == base } ?: EN
        }
    }
}

/** Every user-visible string. One implementation per language; the compiler enforces completeness. */
interface Messages {
    val appTitle: String
    val appSubtitle: String
    val language: String
    val stepRom: String
    val chooseRom: String
    val dropHint: String
    val noRomChosen: String
    val fileLabel: String
    val sizeLabel: String
    val hashLabel: String
    val usingProfile: (String) -> String
    val statusKnown: (String) -> String
    val statusCompatible: (String) -> String
    val statusAlreadyPatched: String
    val statusUnrecognized: (Identification.Reason) -> String
    val allowCompatible: String
    val readError: (String) -> String
    val stepFixes: String
    val fixTitle: (PatchGroup) -> String
    val fixDescription: (PatchGroup, String) -> String
    val tagRequired: String
    val tagNeededBy: (String) -> String
    val stepOutput: String
    val outputFolder: String
    val change: String
    val willWrite: String
    val willOverwrite: String
    val apply: String
    val successTitle: String
    val successDetail: (changed: Int, checksum: String) -> String
    val writtenFiles: String
    val sha256Output: String
    val failureTitle: String
    val failure: (PatchException.Kind, String) -> String
    val writeFailure: (String) -> String
    val nothingSelected: String
    val footer: String
    val testedNote: String
    val help: String
    val helpTitle: String
    val helpClose: String
    val helpSections: List<HelpSection>
}

/** One block of the Help screen: a title and its bullet points. */
class HelpSection(val title: String, val items: List<String>)


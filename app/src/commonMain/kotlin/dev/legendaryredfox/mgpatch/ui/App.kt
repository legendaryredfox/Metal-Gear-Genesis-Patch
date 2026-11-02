package dev.legendaryredfox.mgpatch.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.legendaryredfox.mgpatch.core.Identification.AlreadyPatched
import dev.legendaryredfox.mgpatch.core.Identification.Compatible
import dev.legendaryredfox.mgpatch.core.Identification.Known
import dev.legendaryredfox.mgpatch.core.Identification.Unrecognized
import dev.legendaryredfox.mgpatch.ui.Outcome.Done
import dev.legendaryredfox.mgpatch.ui.Outcome.Failed

/** What the screen needs from the operating system. */
interface Platform {
    /** Show a file chooser; null when cancelled. */
    fun chooseRom(): String?

    /** Show a folder chooser starting at [initial]; null when cancelled. */
    fun chooseFolder(initial: String?): String?

    /** Modifier that calls [onFile] when a file is dropped on the element. */
    fun dropTarget(onFile: (String) -> Unit): Modifier
}

private val Green = Color(0xFF3F7D4A)
private val Amber = Color(0xFFB7791F)
private val Red = Color(0xFFB3261E)

private val LightColors = lightColorScheme(primary = Color(0xFF2F6B3B), secondary = Color(0xFF55624F))
private val DarkColors = darkColorScheme(primary = Color(0xFF8FD39A), secondary = Color(0xFFBBCBB2))

@Composable
fun MetalGearTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}

@Composable
fun App(state: PatcherState, platform: Platform, dark: Boolean) {
    val m = state.lang.messages
    MetalGearTheme(dark) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 820.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Header(state)
                    if (state.showHelp) HelpCard(state)
                    RomCard(state, platform)
                    if (state.profile != null) {
                        FixesCard(state)
                        OutputCard(state, platform)
                    }
                    ApplyBar(state)
                    OutcomeCard(state)
                    Text(m.testedNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(m.footer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Header(state: PatcherState) {
    val m = state.lang.messages
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(m.appTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(m.appSubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = { state.showHelp = !state.showHelp }, modifier = Modifier.testTag("help")) {
            Text(m.help)
        }
        Spacer(Modifier.width(8.dp))
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.testTag("language")) {
                Text(state.lang.nativeName)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                Lang.entries.forEach { l ->
                    DropdownMenuItem(text = { Text(l.nativeName) }, onClick = { state.lang = l; open = false })
                }
            }
        }
    }
}

@Composable
private fun HelpCard(state: PatcherState) {
    val m = state.lang.messages
    SectionCard(m.helpTitle) {
        Column(Modifier.testTag("helpCard"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            m.helpSections.forEach { section ->
                Text(section.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                section.items.forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("\u2022")
                        SelectionContainer { Text(item, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
        TextButton(onClick = { state.showHelp = false }, modifier = Modifier.testTag("helpClose")) { Text(m.helpClose) }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun RomCard(state: PatcherState, platform: Platform) {
    val m = state.lang.messages
    Box(platform.dropTarget { state.loadRom(it) }) {
        SectionCard(m.stepRom) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { platform.chooseRom()?.let(state::loadRom) }, modifier = Modifier.testTag("chooseRom")) {
                    Text(m.chooseRom)
                }
                Text(m.dropHint, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val path = state.romPath
            if (path == null) {
                Text(m.noRomChosen, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@SectionCard
            }
            val err = state.loadError
            if (err != null) {
                Text(m.readError(err), color = Red, modifier = Modifier.testTag("romStatus"))
                return@SectionCard
            }
            val id = state.identification ?: return@SectionCard
            Field(m.fileLabel, path)
            Field(m.sizeLabel, "${id.size} bytes (${id.size / 1024} KB)")
            Field(m.hashLabel, id.sha256, mono = true)
            when (id) {
                is Known ->
                    Status(m.statusKnown(id.profile.label), Green)
                is Compatible -> {
                    Status(m.statusCompatible(id.profile.label), Amber)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(state.allowCompatible, { state.allowCompatible = it }, modifier = Modifier.testTag("allowCompatible"))
                        Text(m.allowCompatible)
                    }
                }
                is AlreadyPatched -> Status(m.statusAlreadyPatched, Amber)
                is Unrecognized -> Status(m.statusUnrecognized(id.reason), Red)
            }
        }
    }
}

@Composable
private fun Status(text: String, color: Color) {
    Text(text, color = color, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("romStatus"))
}

@Composable
private fun Field(label: String, value: String, mono: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$label:", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(min = 64.dp))
        SelectionContainer {
            Text(value, fontFamily = if (mono) FontFamily.Monospace else null, fontSize = if (mono) 12.sp else 14.sp)
        }
    }
}

@Composable
private fun FixesCard(state: PatcherState) {
    val m = state.lang.messages
    val profile = state.profile ?: return
    SectionCard(m.stepFixes) {
        profile.groups.forEachIndexed { index, g ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val mandatory = g in profile.mandatory
            val lockedBy = state.lockedBy(g)
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = g in state.effective,
                    onCheckedChange = { state.setGroup(g, it) },
                    enabled = !state.isLocked(g),
                    modifier = Modifier.testTag("fix-${g.name}"),
                )
                Column(Modifier.padding(top = 12.dp)) {
                    Text(m.fixTitle(g), fontWeight = FontWeight.SemiBold)
                    val tag = when {
                        mandatory -> m.tagRequired
                        lockedBy != null -> m.tagNeededBy(m.fixTitle(lockedBy))
                        else -> null
                    }
                    if (tag != null) Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        m.fixDescription(g, profile.id),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun OutputCard(state: PatcherState, platform: Platform) {
    val m = state.lang.messages
    SectionCard(m.stepOutput) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(m.outputFolder, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelectionContainer { Text(state.outputDir) }
            }
            OutlinedButton(onClick = { platform.chooseFolder(state.outputDir)?.let { state.outputDir = it } }) { Text(m.change) }
        }
        Text(m.willWrite, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.outputNames.forEach { SelectionContainer { Text(it, fontFamily = FontFamily.Monospace, fontSize = 13.sp) } }
        if (state.existingOutputs().isNotEmpty()) Text(m.willOverwrite, color = Amber, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ApplyBar(state: PatcherState) {
    val m = state.lang.messages
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(onClick = { state.apply() }, enabled = state.canApply, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("apply")) {
            Text(m.apply, fontSize = 16.sp)
        }
        if (state.profile != null && state.userChoices.isEmpty()) {
            Text(m.nothingSelected, color = Amber, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OutcomeCard(state: PatcherState) {
    val m = state.lang.messages
    when (val o = state.outcome) {
        null -> Spacer(Modifier.height(0.dp))
        is Failed -> SectionCard(m.failureTitle) {
            Text(o.message(m), color = Red, modifier = Modifier.testTag("failure"))
        }
        is Done -> SectionCard(m.successTitle) {
            Text(
                m.successDetail(o.result.changedBytes, "0x" + o.result.checksum.toString(16).padStart(4, '0').uppercase()),
                modifier = Modifier.testTag("success"),
            )
            Text(m.writtenFiles, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            o.files.forEach { SelectionContainer { Text(it, fontFamily = FontFamily.Monospace, fontSize = 13.sp) } }
            Text(m.sha256Output, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SelectionContainer { Text(o.result.sha256, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
        }
    }
}

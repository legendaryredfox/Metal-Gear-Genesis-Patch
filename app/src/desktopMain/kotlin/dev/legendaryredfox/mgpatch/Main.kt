package dev.legendaryredfox.mgpatch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.legendaryredfox.mgpatch.ui.App
import dev.legendaryredfox.mgpatch.ui.Lang
import dev.legendaryredfox.mgpatch.ui.PatcherState
import java.awt.Frame
import java.awt.Taskbar
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.system.exitProcess

/** The program icon (app/icons/icon.png, copied to the resources), or null if it cannot be read. */
private fun loadIcon() = object {}.javaClass.getResourceAsStream("/icon.png")?.use { runCatching { ImageIO.read(it) }.getOrNull() }

fun main(args: Array<String>) {
    if (args.isNotEmpty() && args.none { it == "--gui" }) {
        exitProcess(Cli.run(args.toList(), DesktopRomIo, ::println, { System.err.println(it) }))
    }
    val state = PatcherState(DesktopRomIo, Lang.fromLocale(Locale.getDefault().language))
    args.firstOrNull { !it.startsWith("--") }?.let(state::loadRom)
    val icon = loadIcon()
    // macOS Dock and Windows taskbar icon when started from the jar (the installers carry their own)
    if (icon != null && Taskbar.isTaskbarSupported()) {
        val taskbar = Taskbar.getTaskbar()
        if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) runCatching { taskbar.iconImage = icon }
    }
    application {
        val window = rememberWindowState(size = DpSize(860.dp, 820.dp))
        val painter = remember { icon?.let { BitmapPainter(it.toComposeImageBitmap()) } }
        Window(onCloseRequest = ::exitApplication, title = state.lang.messages.appTitle, state = window, icon = painter) {
            val platform = DesktopPlatform { Frame.getFrames().firstOrNull { it.isActive } }
            App(state, platform, isSystemInDarkTheme())
        }
    }
}

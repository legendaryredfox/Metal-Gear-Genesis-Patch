package dev.legendaryredfox.mgpatch

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import dev.legendaryredfox.mgpatch.ui.Platform
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import javax.swing.UIManager

/** Native file choosers (AWT FileDialog on macOS and Windows, Swing for folders) and file drop. */
class DesktopPlatform(private val parent: () -> Frame?) : Platform {
    override fun chooseRom(): String? {
        val dialog = FileDialog(parent(), "Metal Gear ROM", FileDialog.LOAD)
        dialog.isVisible = true
        val name = dialog.file ?: return null
        return File(dialog.directory, name).path
    }

    override fun chooseFolder(initial: String?): String? {
        val isMac = System.getProperty("os.name").lowercase().contains("mac")
        if (isMac) {
            System.setProperty("apple.awt.fileDialogForDirectories", "true")
            try {
                val dialog = FileDialog(parent(), "Output folder", FileDialog.LOAD)
                if (initial != null) dialog.directory = initial
                dialog.isVisible = true
                val name = dialog.file ?: return null
                return File(dialog.directory, name).path
            } finally {
                System.setProperty("apple.awt.fileDialogForDirectories", "false")
            }
        }
        runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
        val chooser = JFileChooser(initial?.let { File(it) })
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        return if (chooser.showOpenDialog(parent()) == JFileChooser.APPROVE_OPTION) chooser.selectedFile.path else null
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun dropTarget(onFile: (String) -> Unit): Modifier = Modifier.dragAndDropTarget(
        shouldStartDragAndDrop = { event -> event.dragData() is DragData.FilesList },
        target = object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val data = event.dragData() as? DragData.FilesList ?: return false
                val first = data.readFiles().firstOrNull() ?: return false
                val path = runCatching { File(URI(first)).path }.getOrElse { first }
                onFile(path)
                return true
            }
        },
    )
}

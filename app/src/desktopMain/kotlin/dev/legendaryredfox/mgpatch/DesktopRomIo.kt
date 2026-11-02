package dev.legendaryredfox.mgpatch

import java.io.File

object DesktopRomIo : RomIo {
    override fun read(path: String): ByteArray = File(path).readBytes()

    override fun write(path: String, data: ByteArray) {
        val f = File(path)
        f.absoluteFile.parentFile?.mkdirs()
        f.writeBytes(data)
    }

    override fun exists(path: String): Boolean = File(path).exists()
    override fun parentOf(path: String): String = File(path).absoluteFile.parent ?: "."
    override fun fileName(path: String): String = File(path).name
    override fun join(dir: String, name: String): String = File(dir, name).path
}

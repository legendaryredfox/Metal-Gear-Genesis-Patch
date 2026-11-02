package dev.legendaryredfox.mgpatch

/** File access the app needs. The desktop implementation uses java.io; tests use an in-memory fake. */
interface RomIo {
    fun read(path: String): ByteArray
    fun write(path: String, data: ByteArray)
    fun exists(path: String): Boolean
    fun parentOf(path: String): String
    fun fileName(path: String): String
    fun join(dir: String, name: String): String
}
